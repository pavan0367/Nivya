package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing local battery telemetry readings and offline synchronization queue.
 */
@Entity(tableName = "battery_telemetry")
data class BatteryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceUuid: String,
    val batteryPct: Int,
    val chargingState: String,
    val batteryState: String,
    val health: String,
    val temperatureCelsius: Double?,
    val recordedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
