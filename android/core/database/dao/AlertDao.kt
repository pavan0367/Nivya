package com.nivya.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nivya.core.database.entities.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM local_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM local_alerts WHERE familyId = :familyId ORDER BY createdAt DESC")
    fun getFamilyAlerts(familyId: Long): Flow<List<AlertEntity>>

    @Query("SELECT * FROM local_alerts WHERE deviceId = :deviceId AND targetRole IN ('CHILD', 'ALL') ORDER BY createdAt DESC")
    fun getChildAlerts(deviceId: Long): Flow<List<AlertEntity>>

    @Query("SELECT COUNT(*) FROM local_alerts WHERE familyId = :familyId AND isRead = 0")
    fun getUnreadCount(familyId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AlertEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Query("UPDATE local_alerts SET isRead = 1, readAt = :readAt WHERE id = :id")
    suspend fun markAsRead(id: Long, readAt: String)

    @Query("UPDATE local_alerts SET resolved = 1, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun markAsResolved(id: Long, resolvedAt: String)

    @Query("DELETE FROM local_alerts")
    suspend fun clearAll()
}
