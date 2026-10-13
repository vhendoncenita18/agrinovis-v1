package com.theis.agrinovis.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.theis.agrinovis.data.local.dao.PlantDao
import com.theis.agrinovis.data.local.dao.ActivityLogDao
import com.theis.agrinovis.data.local.dao.NotificationDao
import com.theis.agrinovis.data.local.dao.UserDao
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.data.local.entities.ActivityLogEntity
import com.theis.agrinovis.data.local.entities.NotificationEntity
import com.theis.agrinovis.data.local.entities.UserAccountEntity
import com.theis.agrinovis.data.local.database.converters.NotificationConverters

@Database(entities = [PlantEntity::class, ActivityLogEntity::class, NotificationEntity::class, UserAccountEntity::class], version = 8, exportSchema = false)
@TypeConverters(NotificationConverters::class)
abstract class AgrinovisDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AgrinovisDatabase? = null

        fun getDatabase(context: Context): AgrinovisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgrinovisDatabase::class.java,
                    "agrinovis_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
