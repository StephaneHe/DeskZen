package com.deskzen.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.deskzen.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    override suspend fun getInstalledApps(includeSystem: Boolean): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val activities = pm.queryIntentActivities(intent, 0)

                activities.mapNotNull { resolveInfo ->
                    val appInfo = resolveInfo.activityInfo.applicationInfo ?: return@mapNotNull null
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                    // Only hide pure system apps (not updated by user) when filter is on
                    // Apps like Chrome, Maps, Gmail are FLAG_SYSTEM but also FLAG_UPDATED_SYSTEM_APP
                    if (!includeSystem && isSystem && !isUpdatedSystem) {
                        // Still include if it has a known user-facing package
                        val isKnownUserApp = KNOWN_USER_FACING_PACKAGES.any {
                            appInfo.packageName.contains(it)
                        }
                        if (!isKnownUserApp) return@mapNotNull null
                    }

                    mapToAppInfo(pm, appInfo)
                }.sortedBy { it.label.lowercase() }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get installed apps")
                emptyList()
            }
        }
    }

    companion object {
        // System apps that users expect to see
        private val KNOWN_USER_FACING_PACKAGES = listOf(
            "chrome", "gmail", "maps", "youtube", "photos", "camera",
            "calculator", "calendar", "clock", "contacts", "dialer",
            "messages", "files", "settings", "play", "drive", "keep"
        )
    }

    override suspend fun getAppInfo(packageName: String): AppInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                mapToAppInfo(pm, appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.w("Package not found: $packageName")
                null
            }
        }
    }

    override suspend fun searchApps(query: String, includeSystem: Boolean): List<AppInfo> {
        val allApps = getInstalledApps(includeSystem)
        if (query.isBlank()) return allApps

        val lowerQuery = query.lowercase()
        return allApps.filter { app ->
            app.label.lowercase().contains(lowerQuery) ||
                    app.packageName.lowercase().contains(lowerQuery)
        }
    }

    private fun mapToAppInfo(pm: PackageManager, appInfo: ApplicationInfo): AppInfo {
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        return AppInfo(
            packageName = appInfo.packageName,
            label = pm.getApplicationLabel(appInfo).toString(),
            icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                null
            },
            isSystemApp = isSystem,
            installDate = getInstallDate(pm, appInfo.packageName),
            lastUsedDate = null,
            category = getCategoryName(appInfo.category),
            versionName = getVersionName(pm, appInfo.packageName),
            isOnHomeScreen = false
        )
    }

    private fun getInstallDate(pm: PackageManager, packageName: String): Long {
        return try {
            pm.getPackageInfo(packageName, 0).firstInstallTime
        } catch (e: Exception) {
            0L
        }
    }

    private fun getVersionName(pm: PackageManager, packageName: String): String? {
        return try {
            pm.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    }

    private fun getCategoryName(category: Int): String? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "Jeux"
        ApplicationInfo.CATEGORY_AUDIO -> "Audio"
        ApplicationInfo.CATEGORY_VIDEO -> "Vidéo"
        ApplicationInfo.CATEGORY_IMAGE -> "Image"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_NEWS -> "Actualités"
        ApplicationInfo.CATEGORY_MAPS -> "Cartes"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivité"
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Accessibilité"
        else -> null
    }
}
