package com.nivya.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nivya.core.database.dao.BatteryDao
import com.nivya.core.database.dao.DeviceHealthDao
import com.nivya.core.database.dao.DeviceStatusDao
import com.nivya.core.database.dao.FamilyDao
import com.nivya.core.database.dao.LocationDao
import com.nivya.core.database.dao.NetworkDao
import com.nivya.core.database.dao.UsageDao
import com.nivya.core.database.entities.BatteryEntity
import com.nivya.core.database.entities.DeviceHealthEntity
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.database.entities.FamilyEntity
import com.nivya.core.database.entities.LocationEntity
import com.nivya.core.database.entities.NetworkEntity
import com.nivya.core.database.entities.UsageEntity

import com.nivya.core.database.dao.AlertDao
import com.nivya.core.database.entities.AlertEntity
import com.nivya.core.database.dao.ActivityDao
import com.nivya.core.database.entities.ActivityEntity
import com.nivya.core.database.dao.HistoryDao
import com.nivya.core.database.entities.HistoryEntity

/**
 * Room Database caching family links, device telemetry, battery offline queue, network queue, usage stats, location, device health, safety alerts, live activity, and history.
 */
@Database(
    entities = [
        FamilyEntity::class,
        DeviceStatusEntity::class,
        BatteryEntity::class,
        NetworkEntity::class,
        UsageEntity::class,
        LocationEntity::class,
        DeviceHealthEntity::class,
        AlertEntity::class,
        ActivityEntity::class,
        HistoryEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class NivyaDatabase : RoomDatabase() {

    abstract fun familyDao(): FamilyDao
    abstract fun deviceStatusDao(): DeviceStatusDao
    abstract fun batteryDao(): BatteryDao
    abstract fun networkDao(): NetworkDao
    abstract fun usageDao(): UsageDao
    abstract fun locationDao(): LocationDao
    abstract fun deviceHealthDao(): DeviceHealthDao
    abstract fun alertDao(): AlertDao
    abstract fun activityDao(): ActivityDao
    abstract fun historyDao(): HistoryDao


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
