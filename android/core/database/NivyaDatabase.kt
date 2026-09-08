package com.nivya.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nivya.core.database.dao.BatteryDao
import com.nivya.core.database.dao.DeviceStatusDao
import com.nivya.core.database.dao.FamilyDao
import com.nivya.core.database.dao.NetworkDao
import com.nivya.core.database.entities.BatteryEntity
import com.nivya.core.database.entities.DeviceStatusEntity
import com.nivya.core.database.entities.FamilyEntity
import com.nivya.core.database.entities.NetworkEntity

/**
 * Room Database caching family links, device telemetry, battery offline queue, and network queue.
 */
@Database(
    entities = [FamilyEntity::class, DeviceStatusEntity::class, BatteryEntity::class, NetworkEntity::class],
    version = 3,
    exportSchema = false
)
abstract class NivyaDatabase : RoomDatabase() {

    abstract fun familyDao(): FamilyDao
    abstract fun deviceStatusDao(): DeviceStatusDao
    abstract fun batteryDao(): BatteryDao
    abstract fun networkDao(): NetworkDao

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
