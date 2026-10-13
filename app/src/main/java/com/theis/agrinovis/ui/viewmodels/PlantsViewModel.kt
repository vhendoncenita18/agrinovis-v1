package com.theis.agrinovis.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.theis.agrinovis.data.local.database.AgrinovisDatabase
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.data.local.entities.ActivityLogEntity
import com.theis.agrinovis.data.repository.PlantRepository
import com.theis.agrinovis.data.repository.SyncStatus
import com.theis.agrinovis.data.remote.StorageService
import com.theis.agrinovis.data.session.MonitoringSession
import io.github.jan.supabase.auth.auth
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SavePlantState {
    object Idle : SavePlantState()
    object Loading : SavePlantState()
    data class Success(val plantId: String, val message: String) : SavePlantState()
    data class Error(val message: String) : SavePlantState()
}

class PlantsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlantRepository
    private val activityDao = AgrinovisDatabase.getDatabase(application).activityLogDao()

    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _allPlants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val allPlants: StateFlow<List<PlantEntity>> = _allPlants.asStateFlow()

    val allActivities: StateFlow<List<ActivityLogEntity>>

    private val _savePlantState = MutableStateFlow<SavePlantState>(SavePlantState.Idle)
    val savePlantState: StateFlow<SavePlantState> = _savePlantState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        val plantDao = AgrinovisDatabase.getDatabase(application).plantDao()
        repository = PlantRepository(plantDao)
        
        _currentUserId.value = repository.getCurrentUserId()

        viewModelScope.launch {
            _currentUserId.collectLatest { userId ->
                android.util.Log.d("ROOM DEBUG", "PlantsViewModel collectLatest userId: '$userId'")
                _allPlants.value = emptyList() // Immediately purge stale plants from previous user session
                val email = MonitoringSession.offlineUserEmail ?: com.theis.agrinovis.data.remote.SupabaseClient.client.auth.currentUserOrNull()?.email
                if (!userId.isNullOrBlank() || !email.isNullOrBlank()) {
                    repository.getPlantsForUser(userId, email).collect { plants ->
                        android.util.Log.d("ROOM DEBUG", "PlantsViewModel collected ${plants.size} plants for userId: '$userId', email: '$email'")
                        _allPlants.value = plants
                    }
                } else {
                    android.util.Log.d("ROOM DEBUG", "PlantsViewModel userId/email is null/blank, keeping empty plants list")
                    _allPlants.value = emptyList()
                }
            }
        }

        allActivities = activityDao.getAllActivities().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Automatically sync latest records from Supabase on init
        refreshUserAndPlants()
    }

    fun refreshUserAndPlants() {
        val activeId = repository.getCurrentUserId()
        android.util.Log.d("ROOM DEBUG", "refreshUserAndPlants() activeId = '$activeId', currentStoredId = '${_currentUserId.value}'")
        if (_currentUserId.value != activeId) {
            _allPlants.value = emptyList()
            _currentUserId.value = activeId
        }
        syncFromRemote()
    }

    fun savePlant(plant: PlantEntity) {
        viewModelScope.launch {
            _savePlantState.value = SavePlantState.Loading
            _currentUserId.value = repository.getCurrentUserId()

            // Capture image URIs before clearing session
            val diameterUri = MonitoringSession.diameterImageUri
            val heightUri = MonitoringSession.heightImageUri

            // 1. Perform Instant Offline-First save & sync
            val result = repository.insert(plant, getApplication())

            // 2. Launch Image Upload asynchronously in background IO thread (non-blocking for UI)
            if (diameterUri != null || heightUri != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    uploadImages(plant.id, diameterUri, heightUri)
                }
            }

            if (result.isSuccess) {
                val logDetails = "Plant ${plant.id} (${plant.variety}) — Dia: ${plant.diameter}cm, Ht: ${plant.height}cm, Target: ${plant.estHarvestDate} (${plant.daysToHarvest}d)"
                activityDao.insertActivity(ActivityLogEntity(action = "REGISTERED", affectedRecord = logDetails))
                
                // Clear temporary capture session
                MonitoringSession.diameterImageUri = null
                MonitoringSession.heightImageUri = null

                val status = result.getOrNull()
                val message = if (status == SyncStatus.SYNCED_ONLINE) {
                    "Plant ${plant.id} saved & synced online!"
                } else {
                    "Plant ${plant.id} saved locally. Syncing in background!"
                }

                _savePlantState.value = SavePlantState.Success(plant.id, message)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to save plant."
                _savePlantState.value = SavePlantState.Error(errorMsg)
            }
        }
    }

    fun forceSyncAll() {
        viewModelScope.launch {
            _currentUserId.value = repository.getCurrentUserId()
            _isSyncing.value = true
            repository.forceSync(getApplication())
            repository.enqueueSyncWorker(getApplication())
            _isSyncing.value = false
        }
    }

    fun syncFromRemote() {
        viewModelScope.launch {
            _currentUserId.value = repository.getCurrentUserId()
            repository.fetchRemotePlants()
        }
    }

    fun resetSaveState() {
        _savePlantState.value = SavePlantState.Idle
    }

    private suspend fun uploadImages(plantId: String, diameterUri: String?, heightUri: String?) {
        diameterUri?.let { uriString ->
            readUriBytes(uriString)?.let { bytes ->
                StorageService.uploadLettuceImage("diameter_$plantId.jpg", bytes)
            }
        }

        heightUri?.let { uriString ->
            readUriBytes(uriString)?.let { bytes ->
                StorageService.uploadLettuceImage("height_$plantId.jpg", bytes)
            }
        }
    }

    private fun readUriBytes(uriString: String): ByteArray? {
        return try {
            val uri = Uri.parse(uriString)
            val contentResolver = getApplication<Application>().contentResolver

            // 1. Decode bounds to determine sample size
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }

            // Target max dimension around 1280px for dataset storage
            val maxDimension = 1280
            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun updatePlant(plant: PlantEntity) {
        viewModelScope.launch {
            repository.insert(plant, getApplication())
            activityDao.insertActivity(ActivityLogEntity(action = "UPDATED", affectedRecord = "Plant ${plant.id}"))
        }
    }

    fun harvestPlant(plant: PlantEntity) {
        viewModelScope.launch {
            if (MonitoringSession.currentPlant?.id == plant.id) {
                MonitoringSession.currentPlant = null
            }
            val updatedPlant = plant.copy(harvestStatus = "Harvested")
            repository.insert(updatedPlant, getApplication())
            activityDao.insertActivity(
                ActivityLogEntity(
                    action = "HARVESTED",
                    affectedRecord = "Plant ${plant.id} (${plant.variety}) at ${plant.dap} DAP"
                )
            )
            com.theis.agrinovis.ui.utils.NotificationHelper.showNotification(
                context = getApplication(),
                title = "🥬 Lettuce Harvested Successfully",
                message = "Plant ID ${plant.id} (${plant.variety}) has been finalized at ${plant.dap} DAP."
            )
        }
    }

    fun deletePlant(plant: PlantEntity) {
        viewModelScope.launch {
            if (MonitoringSession.currentPlant?.id == plant.id) {
                MonitoringSession.currentPlant = null
            }
            repository.delete(plant)
            activityDao.insertActivity(ActivityLogEntity(action = "DELETED", affectedRecord = "Plant ${plant.id}"))
        }
    }
    
    suspend fun getPlantById(id: String): PlantEntity? {
        return repository.getPlantById(id)
    }
}
