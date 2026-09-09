package com.nivya.data.repository

import android.content.Context
import com.nivya.services.cleanup.CleanUpCategory
import com.nivya.services.cleanup.CleanUpResult
import com.nivya.services.cleanup.StorageCleanUpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository orchestrating storage cleanup scans and secure cache purges.
 */
class CleanUpRepository(
    private val context: Context
) {
    private var cachedLastResult: CleanUpResult? = null

    suspend fun scanRemovableData(): List<CleanUpCategory> = withContext(Dispatchers.IO) {
        StorageCleanUpManager.scanCategories(context)
    }

    suspend fun executeCleanUp(
        categoryIds: Set<String>,
        onProgress: (Float) -> Unit
    ): CleanUpResult = withContext(Dispatchers.IO) {
        val result = StorageCleanUpManager.performCleanUp(context, categoryIds, onProgress)
        cachedLastResult = result
        result
    }

    fun getLastResult(): CleanUpResult? = cachedLastResult

    fun openSystemStorageSettings() {
        val intent = StorageCleanUpManager.createManageStorageIntent(context)
        context.startActivity(intent)
    }
}
