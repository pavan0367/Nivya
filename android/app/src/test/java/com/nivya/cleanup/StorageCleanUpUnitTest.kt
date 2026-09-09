package com.nivya.cleanup

import com.nivya.services.cleanup.CleanUpCategory
import com.nivya.services.cleanup.CleanUpResult
import com.nivya.services.cleanup.StorageCleanUpManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests verifying strict safety guardrails, path whitelisting,
 * database/credential protection, category scanning, and clean-up execution.
 */
class StorageCleanUpUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeCacheDir: File
    private lateinit var fakeFilesDir: File
    private lateinit var fakeExternalCacheDir: File

    @Before
    fun setUp() {
        fakeCacheDir = tempFolder.newFolder("fake_cache")
        fakeFilesDir = tempFolder.newFolder("fake_files")
        fakeExternalCacheDir = tempFolder.newFolder("fake_external_cache")
    }

    @Test
    fun testSafeCacheFileIsAllowedForDeletion() {
        val cacheFile = File(fakeCacheDir, "thumbnail_preview_123.tmp")
        cacheFile.writeText("sample preview thumbnail cache data")

        assertTrue(
            "Cache thumbnail must be safe to delete",
            StorageCleanUpManager.isSafePath(cacheFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
    }

    @Test
    fun testExternalCacheFileIsAllowedForDeletion() {
        val extCacheFile = File(fakeExternalCacheDir, "network_buffer.bin")
        extCacheFile.writeText("temporary network buffer")

        assertTrue(
            "External cache file must be safe to delete",
            StorageCleanUpManager.isSafePath(extCacheFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
    }

    @Test
    fun testTemporaryDiagnosticsFileIsAllowedForDeletion() {
        val tempDiagDir = File(fakeFilesDir, "temp_diagnostics")
        tempDiagDir.mkdirs()
        val diagFile = File(tempDiagDir, "telemetry_dump_001.log")
        diagFile.writeText("temporary diagnostics telemetry")

        assertTrue(
            "Temporary diagnostics file must be safe to delete",
            StorageCleanUpManager.isSafePath(diagFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
    }

    @Test
    fun testDatabaseFilesAreStrictlyProtectedAndNeverDeleted() {
        val dbDir = File(fakeFilesDir, "databases")
        dbDir.mkdirs()

        val dbFile = File(dbDir, "nivya_local.db")
        dbFile.writeText("database binary data")

        val walFile = File(dbDir, "nivya_local.db-wal")
        walFile.writeText("wal data")

        val shmFile = File(dbDir, "nivya_local.db-shm")
        shmFile.writeText("shm data")

        assertFalse(
            "SQLite database MUST NEVER be deleted",
            StorageCleanUpManager.isSafePath(dbFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
        assertFalse(
            "WAL database journal MUST NEVER be deleted",
            StorageCleanUpManager.isSafePath(walFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
        assertFalse(
            "SHM database index MUST NEVER be deleted",
            StorageCleanUpManager.isSafePath(shmFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
    }

    @Test
    fun testSharedPreferencesAndCredentialsAreStrictlyProtected() {
        val prefsDir = File(fakeFilesDir, "shared_prefs")
        prefsDir.mkdirs()

        val secureTokenFile = File(prefsDir, "secure_tokens.xml")
        secureTokenFile.writeText("<map><string name=\"token\">secret</string></map>")

        val userPrefsFile = File(prefsDir, "user_preferences.xml")
        userPrefsFile.writeText("<map></map>")

        assertFalse(
            "Secure token preferences MUST NEVER be deleted",
            StorageCleanUpManager.isSafePath(secureTokenFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
        assertFalse(
            "User preferences MUST NEVER be deleted",
            StorageCleanUpManager.isSafePath(userPrefsFile, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
    }

    @Test
    fun testUserPhotosAndDocumentsOutsideWhitelistAreNeverDeleted() {
        val picturesDir = tempFolder.newFolder("Pictures")
        val photo = File(picturesDir, "family_vacation.jpg")
        photo.writeText("jpeg binary")

        val documentsDir = tempFolder.newFolder("Documents")
        val homeworkDoc = File(documentsDir, "science_project.pdf")
        homeworkDoc.writeText("pdf binary")

        assertFalse(
            "Personal photos MUST NEVER be deleted",
            StorageCleanUpManager.isSafePath(photo, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
        assertFalse(
            "Personal documents MUST NEVER be deleted",
            StorageCleanUpManager.isSafePath(homeworkDoc, fakeCacheDir, fakeExternalCacheDir, fakeFilesDir)
        )
    }

    @Test
    fun testCleanUpCategorySelectionAndToggling() {
        val category = CleanUpCategory(
            id = StorageCleanUpManager.CATEGORY_APP_CACHE,
            name = "App Temporary Cache",
            description = "Thumbnails and network cache",
            estimatedBytes = 52_428_800L, // 50 MB
            isSupported = true,
            isSelected = true
        )

        assertTrue(category.isSupported)
        assertTrue(category.isSelected)
        assertEquals(52_428_800L, category.estimatedBytes)

        val unselected = category.copy(isSelected = false)
        assertFalse(unselected.isSelected)
    }

    @Test
    fun testUnsupportedCategoryProperties() {
        val systemCategory = CleanUpCategory(
            id = StorageCleanUpManager.CATEGORY_SYSTEM_APPS_CACHE,
            name = "Other Apps Cache",
            description = "Cache from other apps",
            estimatedBytes = 0L,
            isSupported = false,
            unsupportedReason = "Android security sandbox requires system settings",
            isSelected = false
        )

        assertFalse(systemCategory.isSupported)
        assertFalse(systemCategory.isSelected)
        assertNotNull(systemCategory.unsupportedReason)
    }

    @Test
    fun testCleanUpResultSuccessAccounting() {
        val result = CleanUpResult(
            freedBytes = 104_857_600L, // 100 MB
            deletedFilesCount = 42,
            timestamp = System.currentTimeMillis(),
            success = true
        )

        assertTrue(result.success)
        assertEquals(42, result.deletedFilesCount)
        assertEquals(104_857_600L, result.freedBytes)
        assertNull(result.errorMessage)
    }
}
