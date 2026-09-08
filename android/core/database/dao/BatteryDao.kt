package com.nivya.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nivya.core.database.entities.BatteryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for local battery cache and offline queue.
 */
@Dao
interface BatteryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatteryReading(reading: BatteryEntity): Long

    @Query("SELECT * FROM battery_telemetry ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestBatteryFlow(): Flow<BatteryEntity?>

    @Query("SELECT * FROM battery_telemetry ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestBattery(): BatteryEntity?

    @Query("SELECT * FROM battery_telemetry WHERE isSynced = 0 ORDER BY recordedAt ASC")
    suspend fun getUnsyncedReadings(): List<BatteryEntity>

    @Query("UPDATE battery_telemetry SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM battery_telemetry WHERE isSynced = 1 AND recordedAt < :olderThan")
    suspend fun purgeSyncedOlderThan(olderThan: Long)
}
