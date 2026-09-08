package com.nivya.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nivya.core.database.entities.FamilyEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local Family entities.
 */
@Dao
interface FamilyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: FamilyEntity)

    @Query("SELECT * FROM families LIMIT 1")
    fun getFamilyFlow(): Flow<FamilyEntity?>

    @Query("SELECT * FROM families LIMIT 1")
    suspend fun getFamily(): FamilyEntity?

    @Query("DELETE FROM families")
    suspend fun clearFamily()
}
