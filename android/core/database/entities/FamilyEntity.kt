package com.nivya.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity caching persistent Family membership for offline resilience.
 */
@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey val familyId: Long,
    val familyCode: String,
    val familyName: String,
    val userRole: String,
    val isPaired: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
