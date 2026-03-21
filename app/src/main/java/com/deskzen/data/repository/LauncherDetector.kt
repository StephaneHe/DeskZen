package com.deskzen.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository
) {

    fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    suspend fun tryReadLauncherConfig(): List<ScreenPage>? {
        val launcherPackage = getDefaultLauncherPackage()
        Timber.d("Default launcher: $launcherPackage")

        // Try known content providers
        val uris = listOf(
            "content://com.google.android.apps.nexuslauncher.settings/favorites",
            "content://com.android.launcher3.settings/favorites",
            "content://$launcherPackage.settings/favorites"
        ).distinct()

        for (uriString in uris) {
            try {
                val result = readFavoritesFromUri(Uri.parse(uriString))
                if (result != null && result.isNotEmpty()) {
                    Timber.d("Read ${result.sumOf { it.items.size }} items from $uriString")
                    return result
                }
            } catch (e: Exception) {
                Timber.d("Cannot read from $uriString: ${e.message}")
            }
        }

        // Fallback: build screen from installed apps
        return buildScreenFromInstalledApps()
    }

    private suspend fun readFavoritesFromUri(uri: Uri): List<ScreenPage>? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor == null || cursor.count == 0) return null

            val pages = mutableMapOf<Int, MutableList<ScreenItem>>()
            val intentCol = cursor.getColumnIndex("intent")
            val screenCol = cursor.getColumnIndex("screen")
            val cellXCol = cursor.getColumnIndex("cellX")
            val cellYCol = cursor.getColumnIndex("cellY")
            val titleCol = cursor.getColumnIndex("title")
            val itemTypeCol = cursor.getColumnIndex("itemType")
            val containerCol = cursor.getColumnIndex("container")

            if (intentCol < 0) return null

            while (cursor.moveToNext()) {
                val container = if (containerCol >= 0) cursor.getInt(containerCol) else -100
                // -100 = desktop, -101 = hotseat
                if (container != -100 && container != -101) continue

                val screen = if (screenCol >= 0) cursor.getInt(screenCol) else 0
                val cellX = if (cellXCol >= 0) cursor.getInt(cellXCol) else 0
                val cellY = if (cellYCol >= 0) cursor.getInt(cellYCol) else 0
                val itemType = if (itemTypeCol >= 0) cursor.getInt(itemTypeCol) else 0
                val title = if (titleCol >= 0) cursor.getString(titleCol) else null
                val intentStr = if (intentCol >= 0) cursor.getString(intentCol) else null

                val position = cellY * 4 + cellX

                when (itemType) {
                    0, 1 -> {
                        // App shortcut
                        val packageName = extractPackageFromIntent(intentStr)
                        if (packageName != null) {
                            val appInfo = appRepository.getAppInfo(packageName)
                            if (appInfo != null) {
                                pages.getOrPut(screen) { mutableListOf() }
                                    .add(ScreenItem.AppShortcut(position = position, appInfo = appInfo))
                            }
                        }
                    }
                    2 -> {
                        // Folder
                        pages.getOrPut(screen) { mutableListOf() }
                            .add(ScreenItem.Folder(
                                position = position,
                                name = title ?: "Dossier",
                                apps = emptyList()
                            ))
                    }
                }
            }

            pages.map { (index, items) ->
                ScreenPage(pageIndex = index, items = items.sortedBy { it.position })
            }.sortedBy { it.pageIndex }
        } catch (e: SecurityException) {
            Timber.d("Permission denied for $uri")
            null
        } catch (e: Exception) {
            Timber.d("Error reading $uri: ${e.message}")
            null
        } finally {
            cursor?.close()
        }
    }

    private fun extractPackageFromIntent(intentStr: String?): String? {
        if (intentStr == null) return null
        return try {
            val intent = Intent.parseUri(intentStr, 0)
            intent.component?.packageName ?: intent.`package`
        } catch (e: Exception) {
            // Try regex fallback
            val regex = Regex("component=([^/;]+)/")
            regex.find(intentStr)?.groupValues?.get(1)
        }
    }

    private suspend fun buildScreenFromInstalledApps(): List<ScreenPage> {
        val apps = appRepository.getInstalledApps(includeSystem = true)
        if (apps.isEmpty()) return listOf(ScreenPage(pageIndex = 0, items = emptyList()))

        val pages = mutableListOf<ScreenPage>()
        val itemsPerPage = 20 // 4 columns x 5 rows

        apps.chunked(itemsPerPage).forEachIndexed { pageIndex, pageApps ->
            val items = pageApps.mapIndexed { index, appInfo ->
                ScreenItem.AppShortcut(position = index, appInfo = appInfo)
            }
            pages.add(ScreenPage(pageIndex = pageIndex, items = items))
        }

        return pages
    }
}
