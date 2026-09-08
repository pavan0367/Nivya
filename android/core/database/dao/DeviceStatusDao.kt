package com.nivya.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nivya.core.database.entities.DeviceStatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for cached device status records.
 */
@Dao
interface DeviceStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceStatusEntity>)

    @Query("SELECT * FROM device_status")
    fun getDevicesFlow(): Flow<List<DeviceStatusEntity>>

    @Query("SELECT * FROM device_status WHERE deviceId = :deviceId")
    fun getDeviceById(deviceId: Long): Flow<DeviceStatusEntity?>

    @Query("DELETE FROM device_status")
    suspend fun clearDevices()
}
