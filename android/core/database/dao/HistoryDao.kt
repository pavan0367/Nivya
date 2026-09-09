package com.nivya.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nivya.core.database.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM local_history WHERE deviceId = :deviceId ORDER BY eventTimestamp DESC LIMIT :limit OFFSET :offset")
    fun getHistoryPaged(deviceId: Long, limit: Int, offset: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM local_history WHERE deviceId = :deviceId ORDER BY eventTimestamp DESC LIMIT 50")
    fun getRecentHistory(deviceId: Long): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<HistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: HistoryEntity)

    @Query("DELETE FROM local_history WHERE deviceId = :deviceId")
    suspend fun clearForDevice(deviceId: Long)

    @Query("DELETE FROM local_history")
    suspend fun clearAll()
}
