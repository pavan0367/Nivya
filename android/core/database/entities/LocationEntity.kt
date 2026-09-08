package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing offline location telemetry queue and latest locally recorded fix.
 */
@Entity(
    tableName = "location_readings",
    indices = [
        Index(value = ["deviceUuid"]),
        Index(value = ["recordedAt"]),
        Index(value = ["isSynced"])
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceUuid: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMetersPerSec: Float?,
    val bearingDegrees: Float?,
    val provider: String,
    val isGpsAvailable: Boolean,
    val isNetworkAvailable: Boolean,
    val permissionState: String,
    val isBackgroundConsented: Boolean,
    val isStale: Boolean,
    val sourceMode: String,
    val recordedAt: Long,
    val isSynced: Boolean = false
)
