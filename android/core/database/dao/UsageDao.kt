package com.nivya.core.database.dao

import androidx.room.*
import com.nivya.core.database.entities.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageReading(reading: UsageEntity): Long

    @Query("SELECT * FROM usage_readings WHERE date = :date ORDER BY recordedAt DESC LIMIT 1")
    fun getUsageByDateFlow(date: String): Flow<UsageEntity?>

    @Query("SELECT * FROM usage_readings ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestUsageFlow(): Flow<UsageEntity?>

    @Query("SELECT * FROM usage_readings WHERE isSynced = 0 ORDER BY recordedAt ASC LIMIT 10")
    suspend fun getUnsyncedReadings(): List<UsageEntity>

    @Query("UPDATE usage_readings SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}
