package com.nivya.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nivya.core.database.entities.DeviceHealthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceHealthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeviceHealthEntity): Long

    @Query("SELECT * FROM device_health_queue WHERE isSynced = 0 ORDER BY recordedAt ASC")
    suspend fun getUnsynced(): List<DeviceHealthEntity>

    @Query("UPDATE device_health_queue SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM device_health_queue ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatest(): DeviceHealthEntity?

    @Query("SELECT * FROM device_health_queue ORDER BY recordedAt DESC LIMIT 1")
    fun observeLatest(): Flow<DeviceHealthEntity?>

    @Query("DELETE FROM device_health_queue WHERE isSynced = 1 AND recordedAt < :threshold")
    suspend fun deleteOldSynced(threshold: Long)

    @Query("SELECT COUNT(*) FROM device_health_queue WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
}
