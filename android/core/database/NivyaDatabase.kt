package com.nivya.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nivya.core.database.dao.BatteryDao
import com.nivya.core.database.dao.DeviceStatusDao
import com.nivya.core.database.dao.FamilyDao
import com.nivya.core.database.dao.NetworkDao
import com.nivya.core.database.dao.UsageDao
import com.nivya.core.database.entities.BatteryEntity
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.database.entities.FamilyEntity
import com.nivya.core.database.entities.NetworkEntity
import com.nivya.core.database.entities.UsageEntity

/**
 * Room Database caching family links, device telemetry, battery offline queue, network queue, and usage stats.
 */
@Database(
    entities = [FamilyEntity::class, DeviceStatusEntity::class, BatteryEntity::class, NetworkEntity::class, UsageEntity::class],
    version = 4,
    exportSchema = false
)
abstract class NivyaDatabase : RoomDatabase() {

    abstract fun familyDao(): FamilyDao
    abstract fun deviceStatusDao(): DeviceStatusDao
    abstract fun batteryDao(): BatteryDao
    abstract fun networkDao(): NetworkDao
    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile
        private var INSTANCE: NivyaDatabase? = null

        fun getInstance(context: Context): NivyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NivyaDatabase::class.java,
                    "nivya_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
