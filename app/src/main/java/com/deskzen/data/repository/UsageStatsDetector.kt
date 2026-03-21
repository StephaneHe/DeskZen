package com.deskzen.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.deskzen.ai.HeuristicCategorizer
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val categorizer: HeuristicCategorizer
) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    suspend fun buildScreenFromUsageStats(): List<ScreenPage> {
        if (!hasUsageStatsPermission()) {
            Timber.d("No usage stats permission")
            return emptyList()
        }

        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 30L * 24 * 60 * 60 * 1000

        val stats = usageManager.queryUsageStats(
            UsageStatsManager.INTERVAL_MONTHLY, startTime, endTime
        )

        if (stats.isNullOrEmpty()) {
            Timber.d("No usage stats available")
            return emptyList()
        }

        // Get packages sorted by usage time
        val packageUsage = stats
            .groupBy { it.packageName }
            .mapValues { (_, s) -> s.sumOf { it.totalTimeInForeground } }
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

        val launcherApps = appRepository.getInstalledApps(includeSystem = true)
            .associateBy { it.packageName }

        val topApps = packageUsage
            .mapNotNull { pkg -> launcherApps[pkg] }
            .take(60)

        if (topApps.isEmpty()) {
            return emptyList()
        }

        // Categorize apps into themes
        val suggestions = categorizer.categorize(topApps)

        // Page 1: Top 8 most-used apps as individual shortcuts (first 2 rows)
        // + folders for each category (remaining rows)
        val pages = mutableListOf<ScreenPage>()

        // === PAGE 1: Favorites + main folders ===
        val page1Items = mutableListOf<ScreenItem>()
        var position = 0

        // Top 8 most used apps as direct shortcuts (rows 1-2)
        val favorites = topApps.take(8)
        for (app in favorites) {
            page1Items.add(ScreenItem.AppShortcut(position = position, appInfo = app))
            position++
        }

        // Fill remaining slots with category folders (rows 3-5 = 12 slots)
        val favPackages = favorites.map { it.packageName }.toSet()
        val categoriesForFolders = suggestions
            .filter { it.themeName != "Autres" }
            .filter { theme -> theme.apps.any { it.packageName !in favPackages } }
            .sortedByDescending { it.apps.size }
            .take(12) // max 12 folders on page 1

        for (theme in categoriesForFolders) {
            val folderApps = theme.apps.filter { it.packageName !in favPackages }
            if (folderApps.isNotEmpty()) {
                page1Items.add(
                    ScreenItem.Folder(
                        position = position,
                        name = "${theme.themeIcon} ${theme.themeName}",
                        apps = folderApps
                    )
                )
                position++
            }
        }

        pages.add(ScreenPage(pageIndex = 0, items = page1Items))

        // === PAGE 2: "Autres" category + overflow ===
        val autresTheme = suggestions.find { it.themeName == "Autres" }
        if (autresTheme != null && autresTheme.apps.isNotEmpty()) {
            val page2Items = mutableListOf<ScreenItem>()
            val autresApps = autresTheme.apps.filter { it.packageName !in favPackages }
            autresApps.forEachIndexed { index, app ->
                if (index < 20) {
                    page2Items.add(ScreenItem.AppShortcut(position = index, appInfo = app))
                }
            }
            if (page2Items.isNotEmpty()) {
                pages.add(ScreenPage(pageIndex = 1, items = page2Items))
            }
        }

        Timber.d("Built ${pages.size} pages: ${favorites.size} favorites + ${categoriesForFolders.size} folders")
        return pages
    }
}
