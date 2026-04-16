package com.deskzen.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deskzen.ai.HeuristicCategorizer
import com.deskzen.data.repository.AppRepository
import com.deskzen.data.repository.NotificationRepository
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ContactAction
import com.deskzen.domain.model.QuickContact
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import com.deskzen.domain.model.ThemeSuggestion
import com.deskzen.ui.theme.DeskZenDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LauncherUiState(
    val pages: List<ScreenPage> = listOf(ScreenPage(0, emptyList())),
    val currentPage: Int = 0,
    val drawerOpen: Boolean = false,
    val allApps: List<AppInfo> = emptyList(),
    val drawerSearchQuery: String = "",
    val suggestions: List<ThemeSuggestion> = emptyList(),
    val showFolderManager: Boolean = false,
    val dockApps: List<AppInfo?> = listOf(null, null, null, null, null),
    val isFirstLaunch: Boolean = true,
    val quickContacts: List<QuickContact?> = List(8) { null }
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val categorizer: HeuristicCategorizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    /** Notification badge counts, observed from NotificationRepository */
    val badgeCounts: StateFlow<Map<String, Int>> = NotificationRepository.badgeCounts

    private val _scrollToFirstPage = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToFirstPage: SharedFlow<Unit> = _scrollToFirstPage.asSharedFlow()

    fun onHomeDoubleTap() { _scrollToFirstPage.tryEmit(Unit) }

    // Custom folders created by the user
    private val customFolderNames = mutableSetOf<String>()

    // Manual app placements: packageName -> folderName (locked from IA)
    private val manualPlacements = mutableMapOf<String, String>()

    // Standalone items on home screen (app shortcuts + web shortcuts) — survive IA rebuilds
    // Key: "page:position", Value: the item
    private val standaloneItems = mutableListOf<StandaloneItem>()

    data class StandaloneItem(
        val pageIndex: Int,
        val position: Int,
        val packageName: String? = null,  // for AppShortcut
        val webUrl: String? = null,       // for WebShortcut
        val webLabel: String? = null,
        val webFavicon: android.graphics.Bitmap? = null
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val apps = appRepository.getInstalledApps(includeSystem = true)
                val suggestions = categorizer.categorize(apps)
                val pages = buildPages(apps, suggestions)

                _uiState.value = _uiState.value.copy(
                    allApps = apps,
                    suggestions = suggestions,
                    pages = pages,
                    isFirstLaunch = false
                )
                updateDockState()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load apps")
            }
        }
    }

    private fun buildPages(
        apps: List<AppInfo>,
        suggestions: List<ThemeSuggestion>
    ): List<ScreenPage> {
        val allItems = mutableListOf<ScreenItem>()
        val assignedPackages = mutableSetOf<String>()
        val appsByPackage = apps.associateBy { it.packageName }

        // 0. Collect all folder names we'll create (for manual placement targets)
        val allFolderNames = mutableSetOf<String>()
        allFolderNames.addAll(customFolderNames)
        suggestions.filter { it.themeName != "Autres" && it.apps.size >= 2 }
            .forEach { allFolderNames.add("${it.themeIcon} ${it.themeName}") }
        allFolderNames.add("📱 Autres")

        // Helper: get manually placed apps for a folder
        fun getManualAppsFor(folderName: String): List<AppInfo> {
            return manualPlacements
                .filter { it.value == folderName }
                .mapNotNull { appsByPackage[it.key] }
        }

        // Apps manually placed elsewhere should NEVER be assigned by IA to another folder
        val manuallyPlacedElsewhere = { pkg: String, currentFolder: String ->
            val target = manualPlacements[pkg]
            target != null && target != currentFolder
        }

        // 1. Custom folders first (user-created) — manual placements + IA fill
        for (customName in customFolderNames) {
            val cleanName = customName.replace(Regex("^\\p{So}\\s*"), "").trim()
            val manualApps = getManualAppsFor(customName)
            val iaApps = findAppsForCustomFolder(cleanName, apps, assignedPackages)
                .filter { !manuallyPlacedElsewhere(it.packageName, customName) }
            val combined = (manualApps + iaApps)
                .distinctBy { it.packageName }
                .filter { it.packageName !in assignedPackages }
            assignedPackages.addAll(combined.map { it.packageName })
            allItems.add(
                ScreenItem.Folder(
                    position = allItems.size,
                    name = customName,
                    apps = combined
                )
            )
        }

        // 2. IA categories (only apps not already assigned)
        val iaCategories = suggestions
            .filter { it.themeName != "Autres" && it.apps.size >= 2 }
            .sortedByDescending { it.apps.size }

        for (theme in iaCategories) {
            val folderName = "${theme.themeIcon} ${theme.themeName}"
            if (customFolderNames.any { it.contains(theme.themeName, ignoreCase = true) }) continue

            val manualApps = getManualAppsFor(folderName)
            val iaApps = theme.apps.filter {
                it.packageName !in assignedPackages &&
                        !manuallyPlacedElsewhere(it.packageName, folderName)
            }
            val combined = (manualApps.filter { it.packageName !in assignedPackages } + iaApps)
                .distinctBy { it.packageName }
            if (combined.isEmpty()) continue

            assignedPackages.addAll(combined.map { it.packageName })
            allItems.add(
                ScreenItem.Folder(
                    position = allItems.size,
                    name = folderName,
                    apps = combined
                )
            )
        }

        // 3. "Autres" — manual placements + unassigned apps
        val autresManual = getManualAppsFor("📱 Autres")
        val autresApps = (autresManual + apps.filter {
            it.packageName !in assignedPackages &&
                    !manuallyPlacedElsewhere(it.packageName, "📱 Autres")
        }).distinctBy { it.packageName }.filter { it.packageName !in assignedPackages }
        if (autresApps.isNotEmpty()) {
            allItems.add(
                ScreenItem.Folder(
                    position = allItems.size,
                    name = "📱 Autres",
                    apps = autresApps
                )
            )
        }

        // Split into pages of 20
        val pages = allItems.chunked(20).mapIndexed { pageIndex, items ->
            ScreenPage(
                pageIndex = pageIndex,
                items = items.mapIndexed { idx, item ->
                    when (item) {
                        is ScreenItem.Folder -> item.copy(position = idx)
                        is ScreenItem.AppShortcut -> item.copy(position = idx)
                        is ScreenItem.WebShortcut -> item.copy(position = idx)
                    }
                }
            )
        }.toMutableList()

        // Re-inject standalone items (app shortcuts + web shortcuts) that were manually placed
        for (standalone in standaloneItems) {
            // Ensure page exists
            while (pages.size <= standalone.pageIndex) {
                pages.add(ScreenPage(pageIndex = pages.size, items = emptyList()))
            }
            val page = pages[standalone.pageIndex]

            // Check position not already occupied
            if (page.items.any { it.position == standalone.position }) continue

            val item: ScreenItem? = when {
                standalone.packageName != null -> {
                    val appInfo = appsByPackage[standalone.packageName]
                    if (appInfo != null) ScreenItem.AppShortcut(standalone.position, appInfo)
                    else null
                }
                standalone.webUrl != null -> {
                    ScreenItem.WebShortcut(
                        position = standalone.position,
                        url = standalone.webUrl,
                        label = standalone.webLabel ?: standalone.webUrl,
                        favicon = standalone.webFavicon
                    )
                }
                else -> null
            }

            if (item != null) {
                pages[standalone.pageIndex] = page.copy(items = page.items + item)
            }
        }

        return pages.ifEmpty { listOf(ScreenPage(0, emptyList())) }
    }

    private fun findAppsForCustomFolder(
        folderName: String,
        allApps: List<AppInfo>,
        alreadyAssigned: Set<String>
    ): List<AppInfo> {
        val lower = folderName.lowercase()

        // 1. Try to match against IA theme names and use their patterns
        val matchedPatterns = mutableListOf<String>()

        for ((themeName, patterns) in HeuristicCategorizer.PACKAGE_PATTERNS) {
            if (lower.contains(themeName.lowercase()) ||
                themeName.lowercase().contains(lower.take(5))) {
                matchedPatterns.addAll(patterns)
            }
        }
        for ((themeName, keywords) in HeuristicCategorizer.LABEL_KEYWORDS) {
            if (lower.contains(themeName.lowercase()) ||
                themeName.lowercase().contains(lower.take(5))) {
                matchedPatterns.addAll(keywords)
            }
        }

        // 2. Also use folder name words as direct keywords
        val folderKeywords = lower.split(" ", ",", "-", "&", "/")
            .filter { it.length >= 3 }

        val allKeywords = (matchedPatterns + folderKeywords).distinct()

        if (allKeywords.isEmpty()) return emptyList()

        return allApps.filter { app ->
            if (app.packageName in alreadyAssigned) return@filter false
            val pkg = app.packageName.lowercase()
            val label = app.label.lowercase()
            val category = app.category?.lowercase() ?: ""

            allKeywords.any { kw ->
                pkg.contains(kw) || label.contains(kw) || category.contains(kw)
            }
        }
    }

    // === Folder management ===

    fun showFolderManager() {
        _uiState.value = _uiState.value.copy(showFolderManager = true)
    }

    fun hideFolderManager() {
        _uiState.value = _uiState.value.copy(showFolderManager = false)
    }

    fun addFolder(name: String) {
        val iconName = pickEmojiForFolder(name)
        val fullName = "$iconName $name"
        customFolderNames.add(fullName)

        // Re-dispatch with the new folder
        reDispatchWithIA()
    }

    fun removeFolder(folderName: String) {
        customFolderNames.remove(folderName)
        // Remove manual placements targeting this folder
        manualPlacements.entries.removeAll { it.value == folderName }
        reDispatchWithIA()
    }

    fun renameFolder(oldName: String, newName: String) {
        val iconName = pickEmojiForFolder(newName)
        val fullNewName = "$iconName $newName"

        // Update custom folder names
        if (customFolderNames.remove(oldName)) {
            customFolderNames.add(fullNewName)
        } else {
            // Was an IA-created folder — register as custom so it persists
            customFolderNames.add(fullNewName)
        }

        // Update manual placements
        manualPlacements.entries.filter { it.value == oldName }.forEach {
            manualPlacements[it.key] = fullNewName
        }

        // Update folder name directly in current pages
        val pages = _uiState.value.pages.toMutableList()
        for (pi in pages.indices) {
            val page = pages[pi]
            val updatedItems = page.items.map { item ->
                if (item is ScreenItem.Folder && item.name == oldName) {
                    item.copy(name = fullNewName)
                } else item
            }
            if (updatedItems != page.items) {
                pages[pi] = page.copy(items = updatedItems)
            }
        }
        _uiState.value = _uiState.value.copy(pages = pages)
    }

    fun reDispatchWithIA() {
        viewModelScope.launch {
            val apps = _uiState.value.allApps
            if (apps.isEmpty()) return@launch

            val suggestions = categorizer.categorize(apps)
            val pages = buildPages(apps, suggestions)

            _uiState.value = _uiState.value.copy(
                suggestions = suggestions,
                pages = pages
            )

            Timber.d("Re-dispatched ${apps.size} apps, ${customFolderNames.size} custom folders preserved")
        }
    }

    fun getAllFolders(): List<Pair<String, Int>> {
        return _uiState.value.pages
            .flatMap { it.items }
            .filterIsInstance<ScreenItem.Folder>()
            .map { it.name to it.apps.size }
    }

    fun getAllFolderNames(): List<String> {
        return _uiState.value.pages
            .flatMap { it.items }
            .filterIsInstance<ScreenItem.Folder>()
            .map { it.name }
    }

    // Move app from one folder to another (manual, locked from IA)
    fun moveAppToFolder(packageName: String, targetFolderName: String) {
        manualPlacements[packageName] = targetFolderName
        reDispatchWithIA()
    }

    fun isAppLocked(packageName: String): Boolean {
        return manualPlacements.containsKey(packageName)
    }

    fun unlockApp(packageName: String) {
        manualPlacements.remove(packageName)
        reDispatchWithIA()
    }

    /** Remove app from its current folder → becomes standalone or goes to "Autres" */
    fun removeAppFromFolder(packageName: String) {
        // Remove any manual placement so IA won't put it back
        manualPlacements.remove(packageName)

        // Find and remove from folder in current pages
        val pages = _uiState.value.pages.toMutableList()
        for (pi in pages.indices) {
            val page = pages[pi]
            val updatedItems = page.items.map { item ->
                if (item is ScreenItem.Folder && item.apps.any { it.packageName == packageName }) {
                    item.copy(apps = item.apps.filter { it.packageName != packageName })
                } else item
            }
            // Remove empty folders
            val cleaned = updatedItems.filter {
                it !is ScreenItem.Folder || it.apps.isNotEmpty()
            }
            if (cleaned != page.items) {
                pages[pi] = page.copy(items = compactPositions(cleaned))
            }
        }
        _uiState.value = _uiState.value.copy(pages = cleanupEmptyPages(pages))
    }

    /** Find which folder contains this app, or null */
    fun getAppFolder(packageName: String): String? {
        return _uiState.value.pages
            .flatMap { it.items }
            .filterIsInstance<ScreenItem.Folder>()
            .find { folder -> folder.apps.any { it.packageName == packageName } }
            ?.name
    }

    // Add app from drawer directly to a specific folder
    fun addAppToFolder(packageName: String, targetFolderName: String) {
        manualPlacements[packageName] = targetFolderName
        reDispatchWithIA()
    }

    // Add app from drawer as standalone shortcut on home screen
    fun addAppToHomeScreen(packageName: String) {
        val app = _uiState.value.allApps.find { it.packageName == packageName } ?: return
        val currentPages = _uiState.value.pages.toMutableList()

        // Find first page with room for a standalone shortcut
        val maxItems = 20
        for (i in currentPages.indices) {
            val p = currentPages[i]
            if (p.items.size < maxItems) {
                val maxPos = p.items.maxOfOrNull { it.position } ?: -1
                val pos = maxPos + 1
                val newItem = ScreenItem.AppShortcut(position = pos, appInfo = app)
                currentPages[i] = p.copy(items = p.items + newItem)
                _uiState.value = _uiState.value.copy(pages = currentPages)
                // Persist as standalone
                standaloneItems.add(StandaloneItem(pageIndex = i, position = pos, packageName = packageName))
                return
            }
        }

        // All pages full — add new page
        val newPage = ScreenPage(
            pageIndex = currentPages.size,
            items = listOf(ScreenItem.AppShortcut(position = 0, appInfo = app))
        )
        currentPages.add(newPage)
        _uiState.value = _uiState.value.copy(pages = currentPages)
        standaloneItems.add(StandaloneItem(pageIndex = currentPages.size - 1, position = 0, packageName = packageName))
    }

    private fun pickEmojiForFolder(name: String): String {
        val lower = name.lowercase()
        val emojiMap = mapOf(
            // Finance
            "financ" to "💳", "banque" to "💳", "bank" to "💳", "argent" to "💰",
            "money" to "💰", "crypto" to "₿", "trading" to "📈", "bourse" to "📈",
            // Social
            "social" to "💬", "message" to "💬", "chat" to "💬", "communic" to "💬",
            // Media
            "media" to "🎬", "video" to "🎬", "film" to "🎬", "stream" to "📺",
            "musique" to "🎵", "music" to "🎵", "audio" to "🎵", "podcast" to "🎙️",
            // Games
            "jeu" to "🎮", "game" to "🎮",
            // Photo
            "photo" to "📷", "camera" to "📷", "image" to "🖼️",
            // Navigation
            "navig" to "🗺️", "transport" to "🚗", "map" to "🗺️", "taxi" to "🚕",
            "voyage" to "✈️", "travel" to "✈️", "hotel" to "🏨",
            // Health
            "sante" to "❤️", "health" to "❤️", "sport" to "💪", "fitness" to "💪",
            // Shopping
            "shop" to "🛒", "achat" to "🛒", "boutique" to "🛍️",
            // Food
            "food" to "🍔", "restaurant" to "🍽️", "cuisine" to "👨‍🍳", "livraison" to "🛵",
            // Work
            "product" to "📊", "travail" to "💼", "work" to "💼", "bureau" to "🏢",
            "emploi" to "💼", "job" to "💼",
            // Education
            "educ" to "📚", "learn" to "📚", "cours" to "📚", "langue" to "🌍",
            // News
            "news" to "📰", "actu" to "📰", "journal" to "📰", "info" to "ℹ️",
            // System
            "systeme" to "🔧", "system" to "🔧", "outil" to "🔧", "util" to "🔧",
            "securite" to "🔒", "security" to "🔒", "vpn" to "🔒",
            // Home
            "maison" to "🏠", "immo" to "🏠", "immobilier" to "🏠",
            // Family
            "famille" to "👨‍👩‍👧", "enfant" to "👶", "kids" to "👶",
            // Dev
            "dev" to "💻", "code" to "💻", "program" to "💻",
            // AI
            "ia" to "🤖", "ai" to "🤖", "intelligen" to "🤖"
        )

        for ((keyword, emoji) in emojiMap) {
            if (lower.contains(keyword)) return emoji
        }

        // Fallback: pick based on first letter
        return when (lower.firstOrNull()) {
            in 'a'..'d' -> "📂"
            in 'e'..'h' -> "📁"
            in 'i'..'l' -> "🗂️"
            in 'm'..'p' -> "📋"
            in 'q'..'t' -> "🏷️"
            in 'u'..'z' -> "📌"
            else -> "📂"
        }
    }

    // === Navigation ===

    fun onPageChanged(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }

    fun openDrawer() {
        _uiState.value = _uiState.value.copy(drawerOpen = true, drawerSearchQuery = "")
    }

    fun closeDrawer() {
        _uiState.value = _uiState.value.copy(drawerOpen = false)
    }

    fun onDrawerSearchChanged(query: String) {
        _uiState.value = _uiState.value.copy(drawerSearchQuery = query)
    }

    fun getFilteredApps(): List<AppInfo> {
        val state = _uiState.value
        val query = state.drawerSearchQuery.lowercase()
        return if (query.isBlank()) state.allApps
        else state.allApps.filter {
            it.label.lowercase().contains(query) ||
                    it.packageName.lowercase().contains(query)
        }
    }

    // === App actions ===

    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getAppShortcuts(packageName: String): List<android.content.pm.ShortcutInfo> {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
            val query = android.content.pm.LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
                setPackage(packageName)
            }
            launcherApps.getShortcuts(query, android.os.Process.myUserHandle()) ?: emptyList()
        } catch (e: Exception) {
            Timber.d("No shortcuts for $packageName: ${e.message}")
            emptyList()
        }
    }

    fun launchShortcut(shortcutInfo: android.content.pm.ShortcutInfo) {
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
            launcherApps.startShortcut(shortcutInfo, null, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch shortcut")
        }
    }

    fun refreshApps() {
        loadApps()
    }

    // === Web shortcuts ===

    /** Add a web shortcut to the home screen at the given page/position */
    fun addWebShortcut(pageIndex: Int, position: Int, url: String, label: String) {
        viewModelScope.launch {
            // Fetch favicon in background
            val favicon = fetchFavicon(url)
            android.util.Log.e("DeskZen", "addWebShortcut: url=$url favicon=${favicon != null} size=${favicon?.width}x${favicon?.height}")
            val shortcut = ScreenItem.WebShortcut(
                position = position,
                url = url,
                label = label,
                favicon = favicon
            )
            val pages = _uiState.value.pages.toMutableList()
            if (pageIndex < pages.size) {
                val page = pages[pageIndex]
                pages[pageIndex] = page.copy(items = page.items + shortcut)
            }
            _uiState.value = _uiState.value.copy(pages = pages)
            // Persist as standalone
            standaloneItems.add(StandaloneItem(
                pageIndex = pageIndex,
                position = position,
                webUrl = url,
                webLabel = label,
                webFavicon = favicon
            ))
        }
    }

    /** Launch a URL in the browser */
    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open URL: $url")
        }
    }

    /** Fetch favicon for a URL. Tries: 1) direct /favicon.ico on site, 2) Google's service */
    private suspend fun fetchFavicon(url: String): android.graphics.Bitmap? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val uri = Uri.parse(url)
                val scheme = uri.scheme ?: "https"
                val host = uri.host ?: return@withContext null
                val port = uri.port
                val authority = if (port > 0 && port != 80 && port != 443) "$host:$port" else host

                // 1) Try direct favicon from the site itself
                val directUrls = listOf(
                    "$scheme://$authority/favicon.ico",
                    "$scheme://$authority/static/favicon.ico"
                )
                for (directUrl in directUrls) {
                    val bitmap = fetchBitmapFromUrl(directUrl)
                    if (bitmap != null) {
                        android.util.Log.d("DeskZen", "Favicon from direct: $directUrl (${bitmap.width}x${bitmap.height})")
                        return@withContext bitmap
                    }
                }

                // 2) Try Google's favicon service (for public sites)
                val googleUrl = "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=$scheme://$authority&size=128"
                val bitmap = fetchBitmapFromUrl(googleUrl)
                if (bitmap != null) {
                    android.util.Log.d("DeskZen", "Favicon from Google: ${bitmap.width}x${bitmap.height}")
                }
                bitmap
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch favicon")
                null
            }
        }
    }

    /** Helper: fetch a bitmap from URL, following redirects */
    private fun fetchBitmapFromUrl(targetUrl: String): android.graphics.Bitmap? {
        var currentUrl = targetUrl
        for (i in 0 until 5) {
            try {
                val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                val code = connection.responseCode

                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location != null) { currentUrl = location; continue }
                    else return null
                }

                if (code == 200) {
                    val bitmap = android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                    connection.disconnect()
                    return bitmap
                }

                connection.disconnect()
                return null
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    /** Fetch page title from a URL */
    suspend fun fetchPageTitle(url: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Timber.d("Fetching title for: $url")
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                val responseCode = connection.responseCode
                Timber.d("Title fetch response code: $responseCode")
                if (responseCode != 200) {
                    connection.disconnect()
                    return@withContext null
                }
                val html = connection.inputStream.bufferedReader().use { it.readText().take(15000) }
                connection.disconnect()
                val match = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE).find(html)
                val title = match?.groupValues?.get(1)?.trim()
                Timber.d("Fetched title: $title")
                title
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch title for $url")
                null
            }
        }
    }

    // === Drag & drop ===

    private val maxItemsPerPage = DeskZenDimens.homeGridColumns * DeskZenDimens.homeGridRows

    /** Move item to an empty slot (no shifting needed) */
    fun moveItem(fromPage: Int, fromPos: Int, toPage: Int, toPos: Int) {
        if (fromPage == toPage && fromPos == toPos) return
        val pages = _uiState.value.pages.toMutableList()

        val srcPage = pages.getOrNull(fromPage) ?: return
        val item = srcPage.items.find { it.position == fromPos } ?: return

        // Remove from source
        val srcItems = srcPage.items.filter { it.position != fromPos }
        pages[fromPage] = srcPage.copy(items = compactPositions(srcItems))

        // Add to destination
        val dstPage = pages.getOrNull(toPage) ?: return
        val movedItem = setItemPosition(item, toPos)
        pages[toPage] = dstPage.copy(items = dstPage.items + movedItem)

        _uiState.value = _uiState.value.copy(pages = cleanupEmptyPages(pages))
    }

    /** Insert item at position, shifting all items at >= insertPos to the right. Cascades cross-page. */
    fun insertItem(fromPage: Int, fromPos: Int, toPage: Int, insertAtPos: Int) {
        if (fromPage == toPage && fromPos == insertAtPos) return
        val pages = _uiState.value.pages.toMutableList()

        // 1. Extract the dragged item from its source page
        val srcPage = pages.getOrNull(fromPage) ?: return
        val draggedItem = srcPage.items.find { it.position == fromPos } ?: return

        if (fromPage == toPage) {
            // Same page: work on a single item list to avoid position confusion
            val pageItems = srcPage.items.toMutableList()

            // Remove the dragged item
            pageItems.removeAll { it.position == fromPos }

            // Compact positions to close the gap
            val compacted = compactPositions(pageItems).toMutableList()

            // Adjust insert position: if we removed an item before insertAtPos, shift back by 1
            val effectiveInsert = if (fromPos < insertAtPos) {
                (insertAtPos - 1).coerceAtLeast(0)
            } else {
                insertAtPos
            }

            // Shift items at >= effectiveInsert to make room
            val shifted = compacted.map { item ->
                if (item.position >= effectiveInsert) setItemPosition(item, item.position + 1) else item
            }.toMutableList()

            // Insert the dragged item
            shifted.add(setItemPosition(draggedItem, effectiveInsert))

            // Handle overflow
            cascadeOverflow(pages, fromPage, shifted)
        } else {
            // Different pages
            // Step 1: Remove from source and compact
            val srcItems = srcPage.items.filter { it.position != fromPos }
            pages[fromPage] = srcPage.copy(items = compactPositions(srcItems))

            // Step 2: Insert into destination page
            val dstPage = pages.getOrNull(toPage) ?: return
            val dstItems = dstPage.items.toMutableList()

            // Shift items at >= insertAtPos to the right
            val shifted = dstItems.map { item ->
                if (item.position >= insertAtPos) setItemPosition(item, item.position + 1) else item
            }.toMutableList()

            // Add the dragged item
            shifted.add(setItemPosition(draggedItem, insertAtPos))

            // Handle cascade overflow
            cascadeOverflow(pages, toPage, shifted)
        }

        _uiState.value = _uiState.value.copy(pages = cleanupEmptyPages(pages))
    }

    /** Drop an app onto an existing folder */
    fun dropIntoFolder(pageIndex: Int, folderPosition: Int, appPackageName: String) {
        val pages = _uiState.value.pages.toMutableList()
        val page = pages.getOrNull(pageIndex) ?: return
        val folder = page.items.find { it.position == folderPosition } as? ScreenItem.Folder ?: return
        val app = _uiState.value.allApps.find { it.packageName == appPackageName } ?: return

        if (folder.apps.any { it.packageName == appPackageName }) return // Already in folder

        // Update folder with new app
        val updatedFolder = folder.copy(apps = folder.apps + app)
        val updatedItems = page.items.map { if (it.position == folderPosition) updatedFolder else it }
        pages[pageIndex] = page.copy(items = updatedItems)

        // Remove the app from its original position (if it was a standalone shortcut)
        for (pi in pages.indices) {
            val p = pages[pi]
            val shortcut = p.items.find { it is ScreenItem.AppShortcut && (it as ScreenItem.AppShortcut).appInfo.packageName == appPackageName }
            if (shortcut != null && !(pi == pageIndex && shortcut.position == folderPosition)) {
                val filtered = p.items.filter { it !== shortcut }
                pages[pi] = p.copy(items = compactPositions(filtered))
                break
            }
        }

        _uiState.value = _uiState.value.copy(pages = cleanupEmptyPages(pages))
    }

    /** Drop an app onto another app → create a new folder with both */
    fun createFolderFromDrop(pageIndex: Int, targetPos: Int, draggedAppPackage: String) {
        val pages = _uiState.value.pages.toMutableList()
        val page = pages.getOrNull(pageIndex) ?: return
        val targetItem = page.items.find { it.position == targetPos } as? ScreenItem.AppShortcut ?: return
        val draggedApp = _uiState.value.allApps.find { it.packageName == draggedAppPackage } ?: return

        val bothApps = listOf(targetItem.appInfo, draggedApp)

        // Auto-name: try to find a common category, fallback to generic name
        val folderName = guessAutoFolderName(bothApps)

        val newFolder = ScreenItem.Folder(
            position = targetPos,
            name = folderName,
            apps = bothApps
        )

        // Replace target app with the new folder
        var updatedItems = page.items.map { if (it.position == targetPos) newFolder else it }

        // Remove the dragged app from wherever it was
        updatedItems = updatedItems.filter {
            !(it is ScreenItem.AppShortcut && it.appInfo.packageName == draggedAppPackage)
        }
        pages[pageIndex] = page.copy(items = compactPositions(updatedItems))

        // Also remove dragged app from other pages if needed
        for (pi in pages.indices) {
            if (pi == pageIndex) continue
            val p = pages[pi]
            val shortcut = p.items.find {
                it is ScreenItem.AppShortcut && it.appInfo.packageName == draggedAppPackage
            }
            if (shortcut != null) {
                pages[pi] = p.copy(items = compactPositions(p.items.filter { it !== shortcut }))
                break
            }
        }

        _uiState.value = _uiState.value.copy(pages = cleanupEmptyPages(pages))
    }

    /** Remove an item from the home screen */
    fun removeFromScreen(pageIndex: Int, position: Int) {
        val pages = _uiState.value.pages.toMutableList()
        val page = pages.getOrNull(pageIndex) ?: return
        val item = page.items.find { it.position == position }

        // Remove from standalone tracking
        if (item is ScreenItem.AppShortcut) {
            standaloneItems.removeAll { it.packageName == item.appInfo.packageName }
        } else if (item is ScreenItem.WebShortcut) {
            standaloneItems.removeAll { it.webUrl == item.url }
        }

        val filtered = page.items.filter { it.position != position }
        pages[pageIndex] = page.copy(items = compactPositions(filtered))
        _uiState.value = _uiState.value.copy(pages = cleanupEmptyPages(pages))
    }

    // --- Drag helpers ---

    private fun setItemPosition(item: ScreenItem, newPos: Int): ScreenItem = when (item) {
        is ScreenItem.AppShortcut -> item.copy(position = newPos)
        is ScreenItem.Folder -> item.copy(position = newPos)
        is ScreenItem.WebShortcut -> item.copy(position = newPos)
    }

    /** Re-number positions 0..n-1 preserving order */
    private fun compactPositions(items: List<ScreenItem>): List<ScreenItem> {
        return items.sortedBy { it.position }.mapIndexed { idx, item ->
            setItemPosition(item, idx)
        }
    }

    /** Handle overflow when a page has more than maxItemsPerPage items */
    private fun cascadeOverflow(pages: MutableList<ScreenPage>, pageIdx: Int, items: MutableList<ScreenItem>) {
        val sorted = items.sortedBy { it.position }
        if (sorted.size <= maxItemsPerPage) {
            pages[pageIdx] = pages[pageIdx].copy(items = sorted)
            return
        }

        // Keep first maxItemsPerPage, overflow the rest
        val keep = sorted.take(maxItemsPerPage)
        val overflow = sorted.drop(maxItemsPerPage)

        pages[pageIdx] = pages[pageIdx].copy(items = keep)

        // Push overflow to next page
        val nextPageIdx = pageIdx + 1
        if (nextPageIdx >= pages.size) {
            // Create new page
            val newPage = ScreenPage(
                pageIndex = nextPageIdx,
                items = overflow.mapIndexed { idx, item -> setItemPosition(item, idx) }
            )
            pages.add(newPage)
        } else {
            // Insert at beginning of next page, shifting existing items
            val nextPage = pages[nextPageIdx]
            val shiftAmount = overflow.size
            val shiftedExisting = nextPage.items.map { setItemPosition(it, it.position + shiftAmount) }
            val insertedOverflow = overflow.mapIndexed { idx, item -> setItemPosition(item, idx) }
            val combined = (insertedOverflow + shiftedExisting).toMutableList()

            // Recurse if next page also overflows
            cascadeOverflow(pages, nextPageIdx, combined)
        }
    }

    /** Remove empty pages (except keep at least one) */
    private fun cleanupEmptyPages(pages: List<ScreenPage>): List<ScreenPage> {
        val nonEmpty = pages.filter { it.items.isNotEmpty() }
        return if (nonEmpty.isEmpty()) {
            listOf(ScreenPage(0, emptyList()))
        } else {
            nonEmpty.mapIndexed { idx, page -> page.copy(pageIndex = idx) }
        }
    }

    /** Auto-generate folder name from apps using the categorizer */
    private fun guessAutoFolderName(apps: List<AppInfo>): String {
        // Try to find common category from IA suggestions
        val suggestions = _uiState.value.suggestions
        for (theme in suggestions) {
            val themePackages = theme.apps.map { it.packageName }.toSet()
            val matchCount = apps.count { it.packageName in themePackages }
            if (matchCount >= 2) {
                return "${theme.themeIcon} ${theme.themeName}"
            }
        }

        // Try common category field
        val categories = apps.mapNotNull { it.category }.distinct()
        if (categories.size == 1) {
            val emoji = pickEmojiForFolder(categories.first())
            return "$emoji ${categories.first()}"
        }

        // Fallback
        return "📂 Dossier"
    }

    // === Dock ===

    private val dockSlots = arrayOfNulls<String>(5) // packageName or null

    init {
        loadDock()
    }

    private fun loadDock() {
        val prefs = context.getSharedPreferences("deskzen_dock", Context.MODE_PRIVATE)
        for (i in 0 until 5) {
            dockSlots[i] = prefs.getString("dock_$i", null)
        }
        updateDockState()
    }

    private fun saveDock() {
        val prefs = context.getSharedPreferences("deskzen_dock", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (i in 0 until 5) {
            if (dockSlots[i] != null) {
                editor.putString("dock_$i", dockSlots[i])
            } else {
                editor.remove("dock_$i")
            }
        }
        editor.apply()
    }

    private fun updateDockState() {
        val appsByPkg = _uiState.value.allApps.associateBy { it.packageName }
        val dockApps = dockSlots.map { pkg ->
            if (pkg != null) appsByPkg[pkg] else null
        }
        _uiState.value = _uiState.value.copy(dockApps = dockApps)
    }

    fun setDockApp(position: Int, packageName: String) {
        if (position in 0..4) {
            // Remove if already in dock at another position
            for (i in 0 until 5) {
                if (dockSlots[i] == packageName) dockSlots[i] = null
            }
            dockSlots[position] = packageName
            saveDock()
            updateDockState()
        }
    }

    fun removeDockApp(position: Int) {
        if (position in 0..4) {
            dockSlots[position] = null
            saveDock()
            updateDockState()
        }
    }

    fun getDockPositions(): List<Int> = (0..4).toList()

    // === Quick Contacts (landscape mode) ===

    init {
        loadQuickContacts()
    }

    private fun loadQuickContacts() {
        val prefs = context.getSharedPreferences("deskzen_contacts", Context.MODE_PRIVATE)
        val contacts = (0 until 8).map { i ->
            val json = prefs.getString("contact_$i", null) ?: return@map null
            try {
                val obj = org.json.JSONObject(json)
                QuickContact(
                    position = i,
                    contactName = obj.getString("name"),
                    phoneNumber = obj.getString("phone"),
                    photoUri = obj.optString("photoUri", null),
                    action = ContactAction.valueOf(obj.optString("action", "CALL_PHONE"))
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load quick contact $i")
                null
            }
        }
        _uiState.value = _uiState.value.copy(quickContacts = contacts)
    }

    private fun saveQuickContacts() {
        val prefs = context.getSharedPreferences("deskzen_contacts", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val contacts = _uiState.value.quickContacts
        for (i in 0 until 8) {
            val contact = contacts.getOrNull(i)
            if (contact != null) {
                val obj = org.json.JSONObject().apply {
                    put("name", contact.contactName)
                    put("phone", contact.phoneNumber)
                    put("photoUri", contact.photoUri)
                    put("action", contact.action.name)
                }
                editor.putString("contact_$i", obj.toString())
            } else {
                editor.remove("contact_$i")
            }
        }
        editor.apply()
    }

    fun setQuickContact(position: Int, contact: QuickContact) {
        if (position !in 0..7) return
        val contacts = _uiState.value.quickContacts.toMutableList()
        contacts[position] = contact.copy(position = position)
        _uiState.value = _uiState.value.copy(quickContacts = contacts)
        saveQuickContacts()
    }

    fun removeQuickContact(position: Int) {
        if (position !in 0..7) return
        val contacts = _uiState.value.quickContacts.toMutableList()
        contacts[position] = null
        _uiState.value = _uiState.value.copy(quickContacts = contacts)
        saveQuickContacts()
    }

    fun executeContactAction(contact: QuickContact) {
        val phone = contact.phoneNumber.replace(" ", "").replace("-", "")
        val intent = when (contact.action) {
            ContactAction.CALL_PHONE -> {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            ContactAction.WHATSAPP_CALL -> {
                // WhatsApp VoIP call via wa.me link
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${phone.removePrefix("+")}")).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            ContactAction.WHATSAPP_MESSAGE -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${phone.removePrefix("+")}")).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            ContactAction.SMS -> {
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute contact action: ${contact.action}")
        }
    }

    // === Backup/Restore ===

    fun exportBackup(): String {
        val backupStandalone = standaloneItems.map { item ->
            BackupStandaloneItem(
                pageIndex = item.pageIndex,
                position = item.position,
                packageName = item.packageName,
                webUrl = item.webUrl,
                webLabel = item.webLabel
            )
        }
        val backupContacts = _uiState.value.quickContacts.map { contact ->
            contact?.let {
                BackupQuickContact(
                    position = it.position,
                    contactName = it.contactName,
                    phoneNumber = it.phoneNumber,
                    action = it.action.name
                )
            }
        }
        return BackupManager.exportToJson(customFolderNames, manualPlacements, backupStandalone, backupContacts)
    }

    fun importBackup(backupData: BackupData) {
        customFolderNames.clear()
        customFolderNames.addAll(backupData.customFolders)
        manualPlacements.clear()
        manualPlacements.putAll(backupData.manualPlacements)
        standaloneItems.clear()
        standaloneItems.addAll(backupData.standaloneItems.map { item ->
            StandaloneItem(
                pageIndex = item.pageIndex,
                position = item.position,
                packageName = item.packageName,
                webUrl = item.webUrl,
                webLabel = item.webLabel,
                webFavicon = null  // favicon re-fetched lazily if needed
            )
        })
        // Restore quick contacts
        val restoredContacts = backupData.quickContacts.mapIndexed { idx, backup ->
            backup?.let {
                QuickContact(
                    position = idx,
                    contactName = it.contactName,
                    phoneNumber = it.phoneNumber,
                    action = try { ContactAction.valueOf(it.action) } catch (_: Exception) { ContactAction.CALL_PHONE }
                )
            }
        }
        _uiState.value = _uiState.value.copy(quickContacts = restoredContacts)
        saveQuickContacts()

        reDispatchWithIA()
        refreshWebShortcutFavicons()
        Timber.d("Imported backup: ${backupData.customFolders.size} folders, ${backupData.manualPlacements.size} placements, ${backupData.standaloneItems.size} standalone items, ${restoredContacts.count { it != null }} quick contacts")
    }

    /** Re-fetch favicons for all web shortcuts that have none (e.g. after a restore) */
    private fun refreshWebShortcutFavicons() {
        val itemsToRefresh = standaloneItems.filter { it.webUrl != null && it.webFavicon == null }
        for (item in itemsToRefresh) {
            viewModelScope.launch {
                val favicon = fetchFavicon(item.webUrl!!) ?: return@launch
                val idx = standaloneItems.indexOf(item)
                if (idx < 0) return@launch
                standaloneItems[idx] = item.copy(webFavicon = favicon)
                // Patch the favicon directly in the pages state without a full rebuild
                val pages = _uiState.value.pages.toMutableList()
                for (pi in pages.indices) {
                    val page = pages[pi]
                    val updated = page.items.map { screenItem ->
                        if (screenItem is ScreenItem.WebShortcut &&
                            screenItem.url == item.webUrl &&
                            screenItem.position == item.position
                        ) screenItem.copy(favicon = favicon)
                        else screenItem
                    }
                    if (updated !== page.items) pages[pi] = page.copy(items = updated)
                }
                _uiState.value = _uiState.value.copy(pages = pages)
            }
        }
    }
}
