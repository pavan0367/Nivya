package com.nivya.core.database.dao

import androidx.room.*
import com.nivya.core.database.entities.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationReading(reading: LocationEntity): Long

    @Query("SELECT * FROM location_readings ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestLocationFlow(): Flow<LocationEntity?>

    @Query("SELECT * FROM location_readings WHERE isSynced = 0 ORDER BY recordedAt ASC LIMIT 20")
    suspend fun getUnsyncedLocations(): List<LocationEntity>

    @Query("UPDATE location_readings SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}
