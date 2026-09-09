package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nivya.core.network.dto.HistoryEventDto

/**
 * Local Room entity caching chronological history events for Parent review.
 */
@Entity(tableName = "local_history")
data class HistoryEntity(
    @PrimaryKey val id: Long,
    val deviceId: Long,
    val packageName: String,
    val appName: String,
    val broadActivity: String,
    val activityLabel: String?,
    val category: String,
    val durationSeconds: Int?,
    val durationFormatted: String?,
    val eventTimestamp: String,
    val details: String?
)

fun HistoryEntity.toDto(): HistoryEventDto {
    return HistoryEventDto(
        id = this.id,
        deviceId = this.deviceId,
        packageName = this.packageName,
        appName = this.appName,
        broadActivity = this.broadActivity,
        activityLabel = this.activityLabel,
        category = this.category,
        durationSeconds = this.durationSeconds,
        durationFormatted = this.durationFormatted,
        eventTimestamp = this.eventTimestamp
    )
}

fun HistoryEventDto.toEntity(): HistoryEntity {
    return HistoryEntity(
        id = this.id,
        deviceId = this.deviceId ?: 0L,
        packageName = this.packageName,
        appName = this.appName,
        broadActivity = this.broadActivity,
        activityLabel = this.activityLabel,
        category = this.category,
        durationSeconds = this.durationSeconds,
        durationFormatted = this.durationFormatted,
        eventTimestamp = this.eventTimestamp,
        details = null
    )
}

