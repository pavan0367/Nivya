package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing local network connectivity snapshots and queuing readings when offline.
 */
@Entity(
    tableName = "network_readings",
    indices = [
        Index(value = ["deviceUuid"]),
        Index(value = ["isSynced"]),
        Index(value = ["recordedAt"])
    ]
)
data class NetworkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceUuid: String,
    val networkType: String,
    val connectionType: String?,
    val isNetworkAvailable: Boolean,
    val isInternetAvailable: Boolean,
    val signalLevel: Int?,
    val signalDbm: Int?,
    val quality: String,
    val recordedAt: Long,
    val isSynced: Boolean = false
)
