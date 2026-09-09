package com.nivya.services.cleanup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import java.io.File

data class CleanUpCategory(
    val id: String,
    val name: String,
    val description: String,
    val estimatedBytes: Long,
    val isSupported: Boolean,
    val unsupportedReason: String? = null,
    val isSelected: Boolean = true
)

data class CleanUpResult(
    val freedBytes: Long,
    val deletedFilesCount: Int,
    val timestamp: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Storage clean-up engine with strict safety guardrails.
 * ONLY supported temporary cache and diagnostics files are removable.
 * NEVER deletes photos, videos, documents, personal files, user databases, or credentials.
 */
object StorageCleanUpManager {

    const val CATEGORY_APP_CACHE = "APP_CACHE"
    const val CATEGORY_DIAGNOSTICS_TEMP = "DIAGNOSTICS_TEMP"
    const val CATEGORY_SYSTEM_APPS_CACHE = "SYSTEM_APPS_CACHE"

    /**
     * Scans storage for removable temporary data across defined categories.
     */
    fun scanCategories(context: Context): List<CleanUpCategory> {
        val categories = mutableListOf<CleanUpCategory>()

        // 1. App Cache Category (Internal & External Cache)
        var appCacheBytes = 0L
        val internalCache = context.cacheDir
        if (internalCache != null && internalCache.exists()) {
            appCacheBytes += calculateDirectorySize(internalCache)
        }
        val externalCache = context.externalCacheDir
        if (externalCache != null && externalCache.exists()) {
            appCacheBytes += calculateDirectorySize(externalCache)
        }

        categories.add(
            CleanUpCategory(
                id = CATEGORY_APP_CACHE,
                name = "App Temporary Cache",
                description = "Temporary image thumbnails, network cache buffers, and web preview files.",
                estimatedBytes = appCacheBytes,
                isSupported = true,
                isSelected = true
            )
        )

        // 2. Diagnostics & Temp Sync Files
        var diagBytes = 0L
        val tempDir = File(context.filesDir, "temp_diagnostics")
        if (tempDir.exists()) {
            diagBytes += calculateDirectorySize(tempDir)
        }
        val exportTempDir = File(context.cacheDir, "export_temp")
        if (exportTempDir.exists()) {
            diagBytes += calculateDirectorySize(exportTempDir)
        }

        categories.add(
            CleanUpCategory(
                id = CATEGORY_DIAGNOSTICS_TEMP,
                name = "Sync & Diagnostics Buffers",
                description = "Temporary telemetry logs and completed offline sync buffers.",
                estimatedBytes = diagBytes,
                isSupported = true,
                isSelected = true
            )
        )

        // 3. Other Apps / System Storage Cache (Explicitly Unsupported Directly)
        categories.add(
            CleanUpCategory(
                id = CATEGORY_SYSTEM_APPS_CACHE,
                name = "Other Apps Cache",
                description = "Cache created by other apps on your phone.",
                estimatedBytes = 0L,
                isSupported = false,
                unsupportedReason = "Android security sandbox prevents third-party apps from deleting other apps' data. Use Android Settings instead.",
                isSelected = false
            )
        )

        return categories
    }

    /**
     * Executes cleanup on whitelisted categories.
     * Enforces strict safety validation for every single file.
     */
    fun performCleanUp(
        context: Context,
        categoryIds: Set<String>,
        onProgress: (Float) -> Unit
    ): CleanUpResult {
        var totalFreedBytes = 0L
        var totalDeletedFiles = 0
        val filesToDelete = mutableListOf<File>()

        if (categoryIds.contains(CATEGORY_APP_CACHE)) {
            context.cacheDir?.let { collectSafeFiles(it, context, filesToDelete) }
            context.externalCacheDir?.let { collectSafeFiles(it, context, filesToDelete) }
        }

        if (categoryIds.contains(CATEGORY_DIAGNOSTICS_TEMP)) {
            val tempDir = File(context.filesDir, "temp_diagnostics")
            if (tempDir.exists()) {
                collectSafeFiles(tempDir, context, filesToDelete)
            }
            val exportTempDir = File(context.cacheDir, "export_temp")
            if (exportTempDir.exists()) {
                collectSafeFiles(exportTempDir, context, filesToDelete)
            }
        }

        val totalFiles = filesToDelete.size
        if (totalFiles == 0) {
            onProgress(1.0f)
            return CleanUpResult(
                freedBytes = 0L,
                deletedFilesCount = 0,
                timestamp = System.currentTimeMillis(),
                success = true
            )
        }

        for ((index, file) in filesToDelete.withIndex()) {
            // Strict safety validation prior to deletion
            if (isSafeToDelete(file, context)) {
                val size = file.length()
                if (file.delete()) {
                    totalFreedBytes += size
                    totalDeletedFiles++
                }
            }
            onProgress((index + 1).toFloat() / totalFiles)
        }

        return CleanUpResult(
            freedBytes = totalFreedBytes,
            deletedFilesCount = totalDeletedFiles,
            timestamp = System.currentTimeMillis(),
            success = true
        )
    }

    /**
     * Strict safety verification ensuring that a file is safe to delete.
     * Rejects any database, credential, shared preference, or external user media file.
     */
    fun isSafeToDelete(file: File, context: Context): Boolean {
        return isSafePath(file, context.cacheDir, context.externalCacheDir, context.filesDir)
    }

    fun isSafePath(file: File, cacheDir: File?, externalCacheDir: File?, filesDir: File?): Boolean {
        if (!file.exists() || file.isDirectory) return false

        val canonicalPath = try {
            file.canonicalPath
        } catch (_: Exception) {
            return false
        }

        // 1. Strict Blacklist: Never touch databases, shared_prefs, or keystore files
        val lowerPath = canonicalPath.lowercase()
        if (lowerPath.endsWith(".db") ||
            lowerPath.endsWith(".db-wal") ||
            lowerPath.endsWith(".db-shm") ||
            lowerPath.contains("/databases/") ||
            lowerPath.contains("\\databases\\") ||
            lowerPath.contains("/shared_prefs/") ||
            lowerPath.contains("\\shared_prefs\\") ||
            lowerPath.contains("secure_tokens") ||
            lowerPath.contains("user_preferences")
        ) {
            return false
        }

        // 2. Strict Whitelist: File MUST reside inside designated cache or temp directories
        val allowedRoots = mutableListOf<File>()
        cacheDir?.let { allowedRoots.add(it) }
        externalCacheDir?.let { allowedRoots.add(it) }
        filesDir?.let {
            allowedRoots.add(File(it, "temp_diagnostics"))
            allowedRoots.add(File(it, "temp"))
        }

        for (allowedRoot in allowedRoots) {
            val rootCanonical = try {
                allowedRoot.canonicalPath
            } catch (_: Exception) {
                null
            }
            if (rootCanonical != null && canonicalPath.startsWith(rootCanonical)) {
                return true
            }
        }

        return false
    }

    /**
     * Creates an intent opening the Android System Storage Settings screen
     * for safely managing system-wide app caches.
     */
    @Suppress("UNUSED_PARAMETER")
    fun createManageStorageIntent(context: Context): Intent {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(StorageManager.ACTION_MANAGE_STORAGE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        return intent
    }

    private fun collectSafeFiles(dir: File, context: Context, result: MutableList<File>) {
        if (!dir.exists()) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                collectSafeFiles(child, context, result)
            } else if (isSafeToDelete(child, context)) {
                result.add(child)
            }
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            size += if (child.isDirectory) {
                calculateDirectorySize(child)
            } else {
                child.length()
            }
        }
        return size
    }
}
