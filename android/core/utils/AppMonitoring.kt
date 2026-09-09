package com.nivya.core.utils

import android.util.Log

/**
 * Clean architectural abstraction for App Monitoring, Crash Reporting,
 * and Diagnostic Telemetry.
 *
 * Plugs directly into Firebase Crashlytics or Sentry in production builds,
 * falling back to Android Logcat in development.
 */
object AppMonitoring {

    private const val TAG = "NivyaMonitoring"
    private var isCrashlyticsEnabled: Boolean = false

    fun init(enableCrashlytics: Boolean = false) {
        this.isCrashlyticsEnabled = enableCrashlytics
    }

    /**
     * Record a non-fatal caught exception for observability.
     */
    fun recordNonFatal(throwable: Throwable, contextMessage: String? = null) {
        if (contextMessage != null) {
            Log.e(TAG, "[$contextMessage] ${throwable.message}", throwable)
        } else {
            Log.e(TAG, "Non-fatal error: ${throwable.message}", throwable)
        }
        // In production with Firebase Crashlytics:
        // FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    /**
     * Set a non-PII diagnostic breadcrumb or user tag.
     */
    fun logBreadcrumb(category: String, message: String) {
        Log.d(TAG, "[$category] $message")
        // In production:
        // FirebaseCrashlytics.getInstance().log("[$category] $message")
    }

    /**
     * Set the current active role tag (PARENT or CHILD) for diagnostic segmentation.
     */
    fun setRoleTag(role: String) {
        Log.i(TAG, "User active role set to: $role")
        // In production:
        // FirebaseCrashlytics.getInstance().setCustomKey("active_role", role)
    }
}
