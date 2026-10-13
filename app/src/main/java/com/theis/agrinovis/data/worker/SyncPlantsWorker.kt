package com.theis.agrinovis.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.theis.agrinovis.data.local.database.AgrinovisDatabase
import com.theis.agrinovis.data.repository.PlantRepository

class SyncPlantsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncPlantsWorker", "Starting background sync worker (users & plants)...")
        return try {
            val database = AgrinovisDatabase.getDatabase(applicationContext)
            val repository = PlantRepository(database.plantDao())
            
            val userSyncResult = repository.syncUnsyncedUsers(database.userDao())
            val plantSyncResult = repository.syncUnsyncedPlants()
            
            if (plantSyncResult.isSuccess || userSyncResult.isSuccess) {
                Log.d("SyncPlantsWorker", "Background sync worker completed successfully.")
                Result.success()
            } else {
                Log.w("SyncPlantsWorker", "Background sync worker failed, scheduling retry.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncPlantsWorker", "Error during background sync execution: ${e.message}", e)
            Result.retry()
        }
    }
}
