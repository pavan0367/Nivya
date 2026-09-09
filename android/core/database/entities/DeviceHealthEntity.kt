package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity caching device health snapshots and offline telemetry queue.
 */
@Entity(tableName = "device_health_queue")
data class DeviceHealthEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceUuid: String,
    val deviceModel: String,
    val deviceManufacturer: String,
    val osVersion: String,
    val sdkVersion: Int,
    val storageTotalBytes: Long,
    val storageUsedBytes: Long,
    val storageFreeBytes: Long,
    val ramTotalBytes: Long,
    val ramUsedBytes: Long,
    val ramFreeBytes: Long,
    val isLowRam: Boolean,
    val batteryPct: Int,
    val chargingState: String,
    val batteryHealth: String,
    val batteryTempCelsius: Double?,
    val networkType: String,
    val isOnline: Boolean,
    val locationPermission: String,
    val usagePermission: String,
    val notificationPermission: String,
    val batteryOptimization: String,
    val allPermissionsHealthy: Boolean,
    val recordedAt: Long,
    val isSynced: Boolean = false
)
