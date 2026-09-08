package com.nivya.services.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.*

data class AppUsageRecord(
    val packageName: String,
    val appName: String,
    val category: String, // EDUCATION, GAMES, SOCIAL, ENTERTAINMENT, PRODUCTIVITY, UTILITIES, OTHER
    val foregroundSeconds: Long,
    val lastTimeUsed: Long
)

data class UsageCollectionResult(
    val totalForegroundSeconds: Long,
    val educationalSeconds: Long,
    val recreationalSeconds: Long,
    val socialSeconds: Long,
    val productivitySeconds: Long,
    val apps: List<AppUsageRecord>
)

/**
 * Official Android UsageStatsCollector using UsageStatsManager.
 * Strictly adheres to user privacy: never captures messages, chat text, or passwords.
 */
object UsageStatsCollector {

    fun collectDailyUsage(context: Context): UsageCollectionResult {
        if (!UsagePermissionHelper.isUsagePermissionGranted(context)) {
            return UsageCollectionResult(0L, 0L, 0L, 0L, 0L, emptyList())
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return UsageCollectionResult(0L, 0L, 0L, 0L, 0L, emptyList())

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val statsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val pm = context.packageManager
        val appRecords = mutableListOf<AppUsageRecord>()

        var totalSeconds = 0L
        var eduSeconds = 0L
        var recSeconds = 0L
        var socSeconds = 0L
        var prodSeconds = 0L

        for (stats in statsList) {
            val totalTimeInForegroundMs = stats.totalTimeInForeground
            if (totalTimeInForegroundMs < 5000) continue // Skip micro-background activations (< 5s)

            val seconds = totalTimeInForegroundMs / 1000
            val pkg = stats.packageName

            // Resolve friendly app label
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                pkg.substringAfterLast('.')
            }

            val category = categorizeApp(pkg, appLabel)

            when (category) {
                "EDUCATION" -> eduSeconds += seconds
                "GAMES", "ENTERTAINMENT" -> recSeconds += seconds
                "SOCIAL" -> socSeconds += seconds
                "PRODUCTIVITY", "UTILITIES" -> prodSeconds += seconds
            }
            totalSeconds += seconds

            appRecords.add(
                AppUsageRecord(
                    packageName = pkg,
                    appName = appLabel,
                    category = category,
                    foregroundSeconds = seconds,
                    lastTimeUsed = stats.lastTimeUsed
                )
            )
        }

        // Sort descending by foreground usage time
        appRecords.sortByDescending { it.foregroundSeconds }

        return UsageCollectionResult(
            totalForegroundSeconds = totalSeconds,
            educationalSeconds = eduSeconds,
            recreationalSeconds = recSeconds,
            socialSeconds = socSeconds,
            productivitySeconds = prodSeconds,
            apps = appRecords
        )
    }

    private fun categorizeApp(packageName: String, appLabel: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = appLabel.lowercase()

        return when {
            lowerPkg.contains("duolingo") || lowerPkg.contains("khan") || lowerPkg.contains("coursera") ||
                    lowerPkg.contains("edx") || lowerPkg.contains("quizlet") || lowerLabel.contains("learning") ||
                    lowerLabel.contains("school") || lowerLabel.contains("study") -> "EDUCATION"

            lowerPkg.contains("minecraft") || lowerPkg.contains("roblox") || lowerPkg.contains("supercell") ||
                    lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerLabel.contains("game") -> "GAMES"

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("spotify") ||
                    lowerPkg.contains("twitch") || lowerPkg.contains("disney") || lowerLabel.contains("music") ||
                    lowerLabel.contains("video") -> "ENTERTAINMENT"

            lowerPkg.contains("messaging") || lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") ||
                    lowerPkg.contains("discord") || lowerPkg.contains("instagram") || lowerPkg.contains("snapchat") ||
                    lowerLabel.contains("chat") || lowerLabel.contains("messages") -> "SOCIAL"

            lowerPkg.contains("docs") || lowerPkg.contains("sheets") || lowerPkg.contains("drive") ||
                    lowerPkg.contains("notion") || lowerPkg.contains("calculator") || lowerPkg.contains("calendar") ||
                    lowerLabel.contains("notes") || lowerLabel.contains("office") -> "PRODUCTIVITY"

            else -> "OTHER"
        }
    }
}
