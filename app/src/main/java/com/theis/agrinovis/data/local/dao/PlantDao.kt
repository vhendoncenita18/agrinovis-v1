package com.theis.agrinovis.data.local.dao

import androidx.room.*
import com.theis.agrinovis.data.local.entities.PlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPlantsByUser(userId: String): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE (userId = :userId AND :userId != '') OR (userId = :email AND :email != '') OR userId IS NULL OR userId = '' ORDER BY timestamp DESC")
    fun getPlantsByUserOrEmail(userId: String, email: String): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants ORDER BY timestamp DESC")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity)

    @Delete
    suspend fun deletePlant(plant: PlantEntity)

    @Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getPlantById(plantId: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE isSynced = 0")
    suspend fun getUnsyncedPlants(): List<PlantEntity>

    @Query("UPDATE plants SET isSynced = 1 WHERE id = :plantId")
    suspend fun markAsSynced(plantId: String)

    @Query("DELETE FROM plants WHERE userId = :userId")
    suspend fun deletePlantsByUser(userId: String)
}
