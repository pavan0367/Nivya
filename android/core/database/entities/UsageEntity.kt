package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing daily aggregated screen time and offline telemetry queue.
 */
@Entity(
    tableName = "usage_readings",
    indices = [
        Index(value = ["deviceUuid"]),
        Index(value = ["date"]),
        Index(value = ["isSynced"])
    ]
)
data class UsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceUuid: String,
    val date: String,
    val totalForegroundSeconds: Long,
    val educationalSeconds: Long,
    val recreationalSeconds: Long,
    val socialSeconds: Long,
    val productivitySeconds: Long,
    val topAppsJson: String,
    val recordedAt: Long,
    val isSynced: Boolean = false
)
