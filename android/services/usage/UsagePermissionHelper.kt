package com.nivya.services.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

enum class UsagePermissionState {
    GRANTED,
    REQUIRED,
    TRY_AGAIN
}

/**
 * Official helper for Android PACKAGE_USAGE_STATS permission lifecycle.
 * Strictly uses AppOpsManager and standard Android settings intents without security bypasses.
 */
object UsagePermissionHelper {

    /**
     * Checks if PACKAGE_USAGE_STATS has been granted by the user in Android system settings.
     */
    fun isUsagePermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Creates an official intent launching Android's Usage Access system settings screen.
     */
    fun createUsageAccessSettingsIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            // Suggest the app package where supported by modern OEMs
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return intent
    }
}
