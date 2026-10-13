package com.theis.agrinovis.features.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.theis.agrinovis.data.local.database.AgrinovisDatabase
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.data.repository.PlantRepository
import com.theis.agrinovis.data.repository.NotificationRepository
import com.theis.agrinovis.data.model.NotificationModel
import com.theis.agrinovis.data.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class InsightsUiState {
    object Loading : InsightsUiState()
    data class Success(val data: InsightsData) : InsightsUiState()
    data class Error(val message: String) : InsightsUiState()
    object Empty : InsightsUiState()
}

data class InsightsData(
    val totalPlants: Int,
    val healthyGrowingCount: Int,
    val nearHarvestCount: Int,
    val readyToHarvestCount: Int,
    val readyToHarvestPlants: List<PlantEntity>,
    val stageDistribution: Map<String, Int>,
    val varietyDistribution: Map<String, Int>,
    val upcomingHarvests: List<PlantEntity>,
    val avgHeight: Double,
    val avgDiameter: Double,
    val avgTemp: Double,
    val avgHumidity: Double,
    val avgWind: Double,
    val avgPressure: Double,
    val dominantCondition: String
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlantRepository
    private val notificationRepository: NotificationRepository
    
    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        val database = AgrinovisDatabase.getDatabase(application)
        repository = PlantRepository(database.plantDao())
        notificationRepository = NotificationRepository(database.notificationDao())
        loadInsights()
    }

    fun loadInsights() {
        _uiState.value = InsightsUiState.Loading
        viewModelScope.launch {
            try {
                val currentUserId = repository.getCurrentUserId()
                repository.getPlantsForUser(currentUserId).collect { plants ->
                    if (plants.isEmpty()) {
                        _uiState.value = InsightsUiState.Empty
                    } else {
                        // Check for auto notifications on crops ready to harvest or predicted harvest date is today
                        val context = getApplication<Application>().applicationContext
                        val existingNotifications = notificationRepository.allNotifications.first()

                        plants.forEach { plant ->
                            if (!plant.harvestStatus.equals("Harvested", ignoreCase = true)) {
                                if (plant.dap >= 33 || plant.daysToHarvest == 0) {
                                    val title = "🥬 Ready for Harvest Matrix"
                                    val message = "Plant ID ${plant.id} (${plant.variety}) is mature at ${plant.dap} DAP."
                                    
                                    if (existingNotifications.none { it.title == title && it.message == message }) {
                                        val newNotif = NotificationModel(
                                            id = java.util.UUID.randomUUID().toString(),
                                            title = title,
                                            message = message,
                                            type = NotificationType.HARVEST,
                                            isSystemNotification = true
                                        )
                                        notificationRepository.insert(newNotif)
                                        com.theis.agrinovis.ui.utils.NotificationHelper.showNotification(
                                            context = context,
                                            title = title,
                                            message = message
                                        )
                                    }
                                }
                            }
                        }

                        val data = InsightsCalculator.calculateData(plants)
                        _uiState.value = InsightsUiState.Success(data)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = InsightsUiState.Error(e.message ?: "Sync calculation anomaly detected.")
            }
        }
    }

    fun harvestPlant(plant: PlantEntity, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val updatedPlant = plant.copy(harvestStatus = "Harvested")
                repository.insert(updatedPlant, getApplication())
                
                // Log activity
                val activityDao = AgrinovisDatabase.getDatabase(getApplication()).activityLogDao()
                activityDao.insertActivity(
                    com.theis.agrinovis.data.local.entities.ActivityLogEntity(
                        action = "HARVESTED",
                        affectedRecord = "Plant ${plant.id}"
                    )
                )

                val title = "🥬 Lettuce Harvested Successfully"
                val message = "Plant ID ${plant.id} (${plant.variety}) has been finalized at ${plant.dap} DAP."
                
                val newNotif = NotificationModel(
                    id = java.util.UUID.randomUUID().toString(),
                    title = title,
                    message = message,
                    type = NotificationType.SYSTEM,
                    isSystemNotification = true
                )
                notificationRepository.insert(newNotif)

                com.theis.agrinovis.ui.utils.NotificationHelper.showNotification(
                    context = context,
                    title = title,
                    message = message
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

object InsightsCalculator {
    fun calculateData(plants: List<PlantEntity>): InsightsData {
        // Filter out already harvested plants for active tracking
        val activePlants = plants.filter { !it.harvestStatus.equals("Harvested", ignoreCase = true) }
        
        val total = activePlants.size
        
        val readyToHarvest = activePlants.filter { it.dap >= 33 || it.daysToHarvest <= 0 }
        val readyToHarvestCount = readyToHarvest.size
        
        val nearHarvest = activePlants.count { it.daysToHarvest in 1..7 && it.dap < 33 }
        val healthyGrowing = total - readyToHarvestCount

        // Growth Stage splits
        val stages = listOf("Seedling", "Vegetative", "Heading", "Maturity")
        val stageMap = stages.associateWith { stageName ->
            activePlants.count { it.stage.equals(stageName, ignoreCase = true) }
        }

        // Variety Distribution counts mapping
        val standardVarieties = listOf("Olmetie", "Limambo", "Orbital")
        val varietyMap = standardVarieties.associateWith { varietyName ->
            activePlants.count { it.variety.equals(varietyName, ignoreCase = true) }
        }

        // Sorted Upcoming outlooks with Stage Priority (excluding those already ready)
        val stagePriority = mapOf(
            "Maturity" to 0,
            "Heading" to 1,
            "Vegetative" to 2,
            "Seedling" to 3
        )

        val upcoming = activePlants.filter { it.daysToHarvest > 0 && it.dap < 33 }
            .sortedWith(
                compareBy<PlantEntity> { stagePriority[it.stage] ?: 99 }
                    .thenBy { it.daysToHarvest }
            )

        // Size metrics aggregation (filtering missing zero values safely)
        val validHeights = activePlants.map { it.height }.filter { it > 0.0 }
        val avgH = if (validHeights.isNotEmpty()) validHeights.average() else 0.0

        val validDiameters = activePlants.map { it.diameter }.filter { it > 0.0 }
        val avgD = if (validDiameters.isNotEmpty()) validDiameters.average() else 0.0

        // Weather analysis blocks
        val validTemps = activePlants.map { it.temp }.filter { it > 0.0 }
        val avgT = if (validTemps.isNotEmpty()) validTemps.average() else 0.0

        val validHumidities = activePlants.map { it.humidity }.filter { it > 0 }
        val avgHum = if (validHumidities.isNotEmpty()) validHumidities.map { it.toDouble() }.average() else 0.0

        val validWinds = activePlants.map { it.windSpeed }.filter { it > 0.0 }
        val avgW = if (validWinds.isNotEmpty()) validWinds.average() else 0.0

        val validPressures = activePlants.map { it.pressure }.filter { it > 0 }
        val avgP = if (validPressures.isNotEmpty()) validPressures.map { it.toDouble() }.average() else 0.0

        val conditionGroups = activePlants.map { it.condition }.filter { it.isNotEmpty() }
            .groupBy { it }
            .maxByOrNull { it.value.size }?.key ?: "N/A"

        return InsightsData(
            totalPlants = total,
            healthyGrowingCount = healthyGrowing,
            nearHarvestCount = nearHarvest,
            readyToHarvestCount = readyToHarvestCount,
            readyToHarvestPlants = readyToHarvest,
            stageDistribution = stageMap,
            varietyDistribution = varietyMap,
            upcomingHarvests = upcoming,
            avgHeight = avgH,
            avgDiameter = avgD,
            avgTemp = avgT,
            avgHumidity = avgHum,
            avgWind = avgW,
            avgPressure = avgP,
            dominantCondition = conditionGroups
        )
    }
}
