package com.nivya.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nivya.core.database.entities.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM local_activities WHERE deviceId = :deviceId AND isCurrent = 1 ORDER BY startedAt DESC LIMIT 1")
    fun getCurrentActivity(deviceId: Long): Flow<ActivityEntity?>

    @Query("SELECT * FROM local_activities WHERE deviceId = :deviceId ORDER BY startedAt DESC LIMIT 20")
    fun getRecentActivities(deviceId: Long): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity)

    @Query("DELETE FROM local_activities WHERE deviceId = :deviceId")
    suspend fun clearForDevice(deviceId: Long)

    @Query("DELETE FROM local_activities")
    suspend fun clearAll()
}
