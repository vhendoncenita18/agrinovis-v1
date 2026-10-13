package com.theis.agrinovis.data.remote.model

import com.theis.agrinovis.data.local.entities.PlantEntity
import kotlinx.serialization.Serializable

@Serializable
data class PlantDto(
    val id: String,
    val user_id: String? = null,
    val variety: String,
    val stage: String,
    val planted_date: String,
    val dap: Int,
    val leaf_count: Int,
    val height: Double,
    val diameter: Double,
    val temp: Double,
    val humidity: Int,
    val pressure: Int,
    val wind_speed: Double,
    val rainfall: Double,
    val condition: String,
    val harvest_status: String,
    val est_harvest_date: String,
    val days_to_harvest: Int,
    val timestamp: Long
)

fun PlantEntity.toDto() = PlantDto(
    id = id,
    user_id = userId,
    variety = variety,
    stage = stage,
    planted_date = plantedDate,
    dap = dap,
    leaf_count = leafCount,
    height = height,
    diameter = diameter,
    temp = temp,
    humidity = humidity,
    pressure = pressure,
    wind_speed = windSpeed,
    rainfall = rainfall,
    condition = condition,
    harvest_status = harvestStatus,
    est_harvest_date = estHarvestDate,
    days_to_harvest = daysToHarvest,
    timestamp = timestamp
)

fun PlantDto.toEntity() = PlantEntity(
    id = id,
    userId = user_id,
    variety = variety,
    stage = stage,
    plantedDate = planted_date,
    dap = dap,
    leafCount = leaf_count,
    height = height,
    diameter = diameter,
    temp = temp,
    humidity = humidity,
    pressure = pressure,
    windSpeed = wind_speed,
    rainfall = rainfall,
    condition = condition,
    harvestStatus = harvest_status,
    estHarvestDate = est_harvest_date,
    daysToHarvest = days_to_harvest,
    timestamp = timestamp
)
