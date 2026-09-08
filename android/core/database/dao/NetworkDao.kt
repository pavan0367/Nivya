package com.nivya.core.database.dao

import androidx.room.*
import com.nivya.core.database.entities.NetworkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworkReading(reading: NetworkEntity): Long

    @Query("SELECT * FROM network_readings ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestNetworkFlow(): Flow<NetworkEntity?>

    @Query("SELECT * FROM network_readings WHERE isSynced = 0 ORDER BY recordedAt ASC LIMIT 50")
    suspend fun getUnsyncedReadings(): List<NetworkEntity>

    @Query("UPDATE network_readings SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM network_readings WHERE isSynced = 1 AND recordedAt < :olderThanTimestamp")
    suspend fun purgeOldSyncedReadings(olderThanTimestamp: Long)
}
