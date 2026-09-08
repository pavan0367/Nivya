package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity caching device status & last-known telemetry for offline visibility.
 */
@Entity(tableName = "device_status")
data class DeviceStatusEntity(
    @PrimaryKey val deviceId: Long,
    val deviceUuid: String,
    val deviceName: String,
    val platform: String,
    val isOnline: Boolean,
    val batteryPct: Int?,
    val networkType: String?,
    val networkQuality: String?,
    val lastSyncAt: String?,
    val lastSeenAt: String?,
    val isStale: Boolean
)
