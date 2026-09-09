package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room persistence entity caching live activity events for Parent monitoring.
 */
@Entity(tableName = "local_activities")
data class ActivityEntity(
    @PrimaryKey val id: Long,
    val deviceId: Long,
    val packageName: String,
    val appName: String,
    val broadActivity: String,
    val category: String,
    val durationSeconds: Int?,
    val durationFormatted: String?,
    val isCurrent: Boolean,
    val startedAt: String,
    val endedAt: String?,
    val createdAt: String
)
