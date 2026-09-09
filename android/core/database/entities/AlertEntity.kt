package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room persistence entity caching safety alerts for offline capability.
 */
@Entity(tableName = "local_alerts")
data class AlertEntity(
    @PrimaryKey val id: Long,
    val familyId: Long,
    val deviceId: Long,
    val deviceName: String?,
    val deviceUuid: String?,
    val alertType: String,
    val severity: String,
    val title: String,
    val message: String,
    val resolved: Boolean,
    val resolvedAt: String?,
    val isRead: Boolean,
    val readAt: String?,
    val targetRole: String,
    val createdAt: String
)
