package com.theis.agrinovis.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.theis.agrinovis.data.local.dao.PlantDao
import com.theis.agrinovis.data.local.dao.UserDao
import com.theis.agrinovis.data.local.database.AgrinovisDatabase
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.data.remote.SupabaseClient
import com.theis.agrinovis.data.remote.model.PlantDto
import com.theis.agrinovis.data.remote.model.toDto
import com.theis.agrinovis.data.remote.model.toEntity
import com.theis.agrinovis.data.session.MonitoringSession
import com.theis.agrinovis.data.worker.SyncPlantsWorker
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

enum class SyncStatus {
    SYNCED_ONLINE,
    SAVED_LOCALLY_PENDING_SYNC
}

class PlantRepository(private val plantDao: PlantDao) {

    fun getCurrentUserId(): String? {
        if (!MonitoringSession.activeUserId.isNullOrBlank()) {
            Log.d("ROOM DEBUG", "getCurrentUserId() returning activeUserId: ${MonitoringSession.activeUserId}")
            return MonitoringSession.activeUserId
        }
        if (MonitoringSession.isLoggedOut) {
            Log.d("ROOM DEBUG", "getCurrentUserId() returning null because session is LOGGED_OUT")
            return null
        }
        val supabaseUser = SupabaseClient.client.auth.currentUserOrNull()
        if (supabaseUser != null) {
            MonitoringSession.activeUserId = supabaseUser.id
            MonitoringSession.isLoggedOut = false
            Log.d("ROOM DEBUG", "getCurrentUserId() bound activeUserId from Supabase SDK: ${supabaseUser.id}")
            return supabaseUser.id
        }
        if (MonitoringSession.isOfflineMode && !MonitoringSession.offlineUserEmail.isNullOrBlank()) {
            Log.d("ROOM DEBUG", "getCurrentUserId() returning offlineUserEmail: ${MonitoringSession.offlineUserEmail}")
            return MonitoringSession.offlineUserEmail
        }
        Log.d("ROOM DEBUG", "getCurrentUserId() returning null (no active session)")
        return null
    }

    fun getPlantsForUser(userId: String?, userEmail: String? = null): Flow<List<PlantEntity>> {
        val targetId = userId.orEmpty()
        val targetEmail = userEmail.orEmpty().ifBlank {
            MonitoringSession.offlineUserEmail.orEmpty()
        }
        Log.d("ROOM DEBUG", "getPlantsForUser() called with targetId: '$targetId', targetEmail: '$targetEmail'")
        val baseFlow = if (targetId.isBlank() && targetEmail.isBlank()) {
            Log.d("ROOM DEBUG", "getPlantsForUser() fallback to getAllPlants() because identifiers are blank")
            plantDao.getAllPlants()
        } else {
            plantDao.getPlantsByUserOrEmail(targetId, targetEmail)
        }

        return baseFlow.map { list ->
            list.map { plant ->
                val updatedPlant = com.theis.agrinovis.ui.utils.PlantUtils.getUpdatedPlantForCurrentDate(plant)
                if (updatedPlant != plant) {
                    Log.d("ROOM DEBUG", "Plant ${plant.id} evolved dynamically: DAP ${plant.dap}->${updatedPlant.dap}, Stage ${plant.stage}->${updatedPlant.stage}")
                    try {
                        plantDao.insertPlant(updatedPlant)
                    } catch (e: Exception) {
                        Log.w("ROOM DEBUG", "Error persisting updated plant ${plant.id}: ${e.message}")
                    }
                }
                updatedPlant
            }
        }
    }

    suspend fun syncUnsyncedUsers(userDao: UserDao): Result<Int> {
        return try {
            val unsyncedUsers = userDao.getUnsyncedUsers()
            if (unsyncedUsers.isEmpty()) {
                return Result.success(0)
            }

            var syncedCount = 0
            Log.d("PlantRepository", "Found ${unsyncedUsers.size} unsynced offline user accounts. Syncing...")

            for (userAccount in unsyncedUsers) {
                try {
                    SupabaseClient.client.auth.signUpWith(Email) {
                        email = userAccount.email
                        password = userAccount.passwordHash
                    }
                    val remoteUser = SupabaseClient.client.auth.currentUserOrNull()
                    val supabaseUid = remoteUser?.id ?: userAccount.email
                    userDao.markUserSynced(userAccount.email, supabaseUid)
                    syncedCount++
                    Log.d("PlantRepository", "Successfully synced user account ${userAccount.email} to Supabase Auth.")
                } catch (_: Exception) {
                    try {
                        SupabaseClient.client.auth.signInWith(Email) {
                            email = userAccount.email
                            password = userAccount.passwordHash
                        }
                        val remoteUser = SupabaseClient.client.auth.currentUserOrNull()
                        val supabaseUid = remoteUser?.id ?: userAccount.email
                        userDao.markUserSynced(userAccount.email, supabaseUid)
                        syncedCount++
                        Log.d("PlantRepository", "User account ${userAccount.email} already existed remotely; verified and marked synced.")
                    } catch (signInEx: Exception) {
                        Log.w("PlantRepository", "Failed to sync user ${userAccount.email}: ${signInEx.message}")
                    }
                }
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e("PlantRepository", "Error syncing user accounts: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun insert(plant: PlantEntity, context: Context? = null): Result<SyncStatus> {
        val currentUserId = getCurrentUserId()
        if (currentUserId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Cannot save plant: No authenticated user session found."))
        }
        val plantWithUser = plant.copy(userId = currentUserId)

        // 1. INSTANT LOCAL SAVE TO ROOM DB (isSynced = false)
        val localPlant = plantWithUser.copy(isSynced = false)
        plantDao.insertPlant(localPlant)
        Log.d("PlantRepository", "Saved plant ${localPlant.id} locally for user $currentUserId.")

        // 2. ATTEMPT IMMEDIATE SUPABASE SYNC (with 3.5s timeout safeguard)
        return try {
            val supabaseUser = SupabaseClient.client.auth.currentUserOrNull()
            if (supabaseUser != null) {
                Log.d("PlantRepository", "Attempting immediate online Supabase upload for plant ${localPlant.id}...")
                
                val synced = withTimeoutOrNull(3500L) {
                    SupabaseClient.client.postgrest["plants"].upsert(localPlant.toDto().copy(user_id = supabaseUser.id))
                    true
                }
                
                if (synced == true) {
                    plantDao.markAsSynced(localPlant.id)
                    Log.d("PlantRepository", "Successfully synced plant ${localPlant.id} to Supabase online!")
                    Result.success(SyncStatus.SYNCED_ONLINE)
                } else {
                    Log.w("PlantRepository", "Supabase online sync timed out (>3.5s). Scheduling background worker...")
                    context?.let { enqueueSyncWorker(it) }
                    Result.success(SyncStatus.SAVED_LOCALLY_PENDING_SYNC)
                }
            } else {
                context?.let { enqueueSyncWorker(it) }
                Result.success(SyncStatus.SAVED_LOCALLY_PENDING_SYNC)
            }
        } catch (e: Exception) {
            Log.w("PlantRepository", "Immediate Supabase sync unavailable (${e.message}). Scheduling background worker...")
            context?.let { enqueueSyncWorker(it) }
            Result.success(SyncStatus.SAVED_LOCALLY_PENDING_SYNC)
        }
    }

    suspend fun syncUnsyncedPlants(): Result<Int> {
        val supabaseUser = SupabaseClient.client.auth.currentUserOrNull()
            ?: return Result.failure(IllegalStateException("User not authenticated online."))

        val unsyncedList = plantDao.getUnsyncedPlants()
        if (unsyncedList.isEmpty()) {
            return Result.success(0)
        }

        var syncedCount = 0
        Log.d("PlantRepository", "Found ${unsyncedList.size} unsynced plant records. Syncing to Supabase...")

        for (plant in unsyncedList) {
            try {
                val plantToUpload = plant.copy(userId = supabaseUser.id)
                SupabaseClient.client.postgrest["plants"].upsert(plantToUpload.toDto().copy(user_id = supabaseUser.id))
                plantDao.markAsSynced(plant.id)
                syncedCount++
                Log.d("PlantRepository", "Background sync completed for plant ${plant.id}.")
            } catch (e: Exception) {
                Log.e("PlantRepository", "Failed background sync for plant ${plant.id}: ${e.message}", e)
            }
        }

        return Result.success(syncedCount)
    }

    fun enqueueSyncWorker(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncPlantsWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "SyncPlantsWork",
                ExistingWorkPolicy.KEEP,
                syncWorkRequest
            )
            Log.d("PlantRepository", "Enqueued WorkManager SyncPlantsWorker.")
        } catch (e: Exception) {
            Log.e("PlantRepository", "Failed to enqueue WorkManager: ${e.message}", e)
        }
    }

    suspend fun fetchRemotePlants(): Result<List<PlantEntity>> {
        val user = SupabaseClient.client.auth.currentUserOrNull()
            ?: return Result.failure(IllegalStateException("User not authenticated."))

        return try {
            val remoteDtos = SupabaseClient.client.postgrest["plants"]
                .select {
                    filter {
                        eq("user_id", user.id)
                    }
                }
                .decodeList<PlantDto>()

            val entities = remoteDtos.map { it.toEntity().copy(isSynced = true) }
            entities.forEach { plantDao.insertPlant(it) }

            Log.d("PlantRepository", "Fetched and cached ${entities.size} plants from Supabase for user ${user.id}.")
            Result.success(entities)
        } catch (e: Exception) {
            Log.e("PlantRepository", "Failed to fetch plants from Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun delete(plant: PlantEntity): Result<Unit> {
        plantDao.deletePlant(plant)
        Log.d("PlantRepository", "Deleted plant ${plant.id} locally from Room.")

        return try {
            val user = SupabaseClient.client.auth.currentUserOrNull()
            if (user != null) {
                withTimeoutOrNull(3500L) {
                    SupabaseClient.client.postgrest["plants"].delete {
                        filter {
                            eq("id", plant.id)
                            eq("user_id", user.id)
                        }
                    }
                }
                Log.d("PlantRepository", "Successfully deleted plant ${plant.id} from Supabase online.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PlantRepository", "Failed to delete plant ${plant.id} from Supabase: ${e.message}", e)
            Result.success(Unit)
        }
    }
    
    suspend fun getPlantById(id: String): PlantEntity? {
        return plantDao.getPlantById(id)
    }

    suspend fun forceSync(context: Context? = null): Result<Int> {
        context?.let { ctx ->
            val userDao = AgrinovisDatabase.getDatabase(ctx).userDao()
            syncUnsyncedUsers(userDao)
        }
        val syncResult = syncUnsyncedPlants()
        val fetchResult = fetchRemotePlants()
        return if (syncResult.isSuccess || fetchResult.isSuccess) {
            Result.success(syncResult.getOrDefault(0))
        } else {
            Result.failure(syncResult.exceptionOrNull() ?: Exception("Sync failed"))
        }
    }
}
