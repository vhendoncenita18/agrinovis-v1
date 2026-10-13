package com.theis.agrinovis.ui.utils

import com.theis.agrinovis.data.local.entities.PlantEntity

data class StageRecommendation(
    val primaryFocus: String,
    val irrigationAdvice: String,
    val lightingAdvice: String,
    val nutrientAdvice: String,
    val monitoringTip: String
)

object PlantUtils {
    fun getModelPathForVarietyAndStage(variety: String, stage: String): String {
        val cleanVariety = variety.trim().lowercase()
        val cleanStage = when (stage.trim().lowercase()) {
            "seedling" -> "seedling"
            "vegetative", "early growth" -> "vegetative"
            "heading", "developing", "growing" -> "heading"
            "maturity", "mature", "harvest ready", "ready to harvest" -> "maturity"
            else -> "vegetative"
        }

        val varietyPrefix = when {
            cleanVariety.contains("olmetie") -> "olmetie"
            cleanVariety.contains("limambo") || cleanVariety.contains("limambi") -> "limambo"
            cleanVariety.contains("orbital") -> "orbital"
            else -> "olmetie"
        }

        return "models/${varietyPrefix}_${cleanStage}.glb"
    }

    fun get2dPicturePathForVarietyAndStage(variety: String, stage: String): String {
        val cleanVariety = variety.trim().lowercase()
        val cleanStage = when (stage.trim().lowercase()) {
            "seedling" -> "seedling"
            "vegetative", "early growth" -> "vegetative"
            "heading", "developing", "growing" -> "heading"
            "maturity", "mature", "harvest ready", "ready to harvest" -> "maturity"
            else -> "vegetative"
        }

        val varietyPrefix = when {
            cleanVariety.contains("olmetie") -> "olmetie"
            cleanVariety.contains("limambo") || cleanVariety.contains("limambi") -> "limambo"
            cleanVariety.contains("orbital") -> "orbital"
            else -> "olmetie"
        }

        return "2d-picture/${varietyPrefix}_${cleanStage}.jpg"
    }

    fun getModelPathForStage(stage: String): String {
        return when (stage.trim().lowercase()) {
            "seedling" -> "models/olmetie_seedling.glb"
            "vegetative", "early growth" -> "models/olmetie_vegetative.glb"
            "heading", "developing", "growing" -> "models/olmetie_heading.glb"
            "maturity", "mature", "harvest ready", "ready to harvest" -> "models/olmetie_maturity.glb"
            else -> "models/olmetie_vegetative.glb"
        }
    }

    fun getScaleForStage(stage: String): Float {
        return when (stage.trim().lowercase()) {
            "seedling" -> 1.5f
            "vegetative", "early growth" -> 1.9f
            "heading", "developing", "growing" -> 1.8f
            "maturity", "mature", "harvest ready", "ready to harvest" -> 1.8f
            else -> 2.0f
        }
    }

    fun getCameraRadiusForStage(stage: String): Float {
        return when (stage.trim().lowercase()) {
            "seedling" -> 3.0f
            "vegetative", "early growth" -> 3.2f
            "heading", "developing", "growing" -> 3.5f
            "maturity", "mature", "harvest ready", "ready to harvest" -> 3.8f
            else -> 3.5f
        }
    }

    fun getDefaultRotationForModel(modelPath: String): Triple<Float, Float, Float> {
        return Triple(-90f, 0f, 0f)
    }

    fun getRecommendationsForStage(stage: String): StageRecommendation {
        return when (stage.lowercase()) {
            "seedling" -> StageRecommendation(
                primaryFocus = "Root establishment & delicate leaf formation",
                irrigationAdvice = "Gentle misting twice daily; keep media consistently damp without saturation",
                lightingAdvice = "12–14 hours filtered indirect sunlight or low-heat grow LED",
                nutrientAdvice = "Mild starter solution (EC ~0.8 - 1.0 mS/cm)",
                monitoringTip = "Check daily for root rot or damping-off; keep ambient humidity ~65-75%"
            )
            "vegetative" -> StageRecommendation(
                primaryFocus = "Rapid leaf expansion & vigorous canopy growth",
                irrigationAdvice = "Regular root-level watering; increase volume as leaf area expands",
                lightingAdvice = "14–16 hours bright sunlight to fuel rapid photosynthesis",
                nutrientAdvice = "Nitrogen-rich formulation (EC ~1.4 - 1.8 mS/cm) for leafy growth",
                monitoringTip = "Inspect lower leaves for aphids; maintain steady airflow to prevent tipburn"
            )
            "heading" -> StageRecommendation(
                primaryFocus = "Inner head compaction & structural firmness",
                irrigationAdvice = "Deep ground-level watering; avoid wetting inside the tightening head",
                lightingAdvice = "Full sunlight with temperature control to prevent heat stress",
                nutrientAdvice = "Balanced Calcium & Potassium mix to strengthen leaf cell walls",
                monitoringTip = "Squeeze head lightly to gauge density; ensure ventilation inside canopy"
            )
            "maturity", "harvest ready", "ready to harvest" -> StageRecommendation(
                primaryFocus = "Harvest readiness & flavor/crispness preservation",
                irrigationAdvice = "Slightly reduce irrigation 24h prior to harvest for peak crispness",
                lightingAdvice = "Cool, shaded ambient environment to prevent bitter bolting",
                nutrientAdvice = "Flush with fresh water / low EC solution before harvesting",
                monitoringTip = "Harvest in early morning hours when plant moisture is highest"
            )
            else -> StageRecommendation(
                primaryFocus = "General crop growth monitoring",
                irrigationAdvice = "Maintain steady moisture levels appropriate for environmental conditions",
                lightingAdvice = "Provide consistent daily natural sunlight exposure",
                nutrientAdvice = "Standard hydroponic or soil nutrient balance",
                monitoringTip = "Observe leaf health and capture periodic dimensions for ML tracking"
            )
        }
    }

    fun getUpdatedPlantForCurrentDate(plant: PlantEntity): PlantEntity {
        if (plant.harvestStatus.equals("Harvested", ignoreCase = true) ||
            plant.harvestStatus.equals("Ready to Harvest", ignoreCase = true) ||
            plant.daysToHarvest <= 0 ||
            plant.dap >= 33) {
            return plant
        }

        val now = System.currentTimeMillis()
        val elapsedMillis = now - plant.timestamp
        if (elapsedMillis < 86_400_000L) { // Less than 24 hours elapsed
            return plant
        }

        val oneDayMillis = 86_400_000L
        val elapsedDays = (elapsedMillis / oneDayMillis).toInt()
        if (elapsedDays <= 0) {
            return plant
        }

        // Increase DAP strictly 1 per day
        val updatedDap = plant.dap + 1
        val updatedDaysToHarvest = kotlin.math.max(0, plant.daysToHarvest - 1)

        val updatedStage = when {
            updatedDap <= 10 -> "Seedling"
            updatedDap in 11..22 -> "Vegetative"
            updatedDap in 23..32 -> "Heading"
            else -> "Maturity"
        }

        val updatedHarvestStatus = if (updatedDaysToHarvest <= 0 || updatedDap >= 33 || updatedStage.equals("Maturity", ignoreCase = true)) {
            "Ready to Harvest"
        } else {
            plant.harvestStatus
        }

        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, updatedDaysToHarvest)
        val updatedEstHarvestDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(calendar.time)

        return plant.copy(
            dap = updatedDap,
            daysToHarvest = updatedDaysToHarvest,
            stage = updatedStage,
            harvestStatus = updatedHarvestStatus,
            estHarvestDate = updatedEstHarvestDate,
            timestamp = plant.timestamp + oneDayMillis
        )
    }
}
