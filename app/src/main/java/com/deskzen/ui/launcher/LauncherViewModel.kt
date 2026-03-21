package com.deskzen.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deskzen.ai.HeuristicCategorizer
import com.deskzen.data.repository.AppRepository
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import com.deskzen.domain.model.ThemeSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val showWidgetPicker: Boolean = false,
    val activeWidgetIds: List<Int> = emptyList(),
    val isFirstLaunch: Boolean = true
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val categorizer: HeuristicCategorizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    // Custom folders created by the user
    private val customFolderNames = mutableSetOf<String>()

    // Manual app placements: packageName -> folderName (locked from IA)
    private val manualPlacements = mutableMapOf<String, String>()

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

        // 1. Custom folders first (user-created) — IA + manual placements
        for (customName in customFolderNames) {
            val cleanName = customName.replace(Regex("^\\p{So}\\s*"), "").trim()
            val iaApps = findAppsForCustomFolder(cleanName, apps, assignedPackages)
            val manualApps = getManualAppsFor(customName)
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
                        manualPlacements[it.packageName] == null // don't IA-assign manually placed apps
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

        // 3. "Autres" — unassigned apps + manual placements to Autres
        val autresManual = getManualAppsFor("📱 Autres")
        val autresApps = (autresManual + apps.filter {
            it.packageName !in assignedPackages && manualPlacements[it.packageName] == null
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
                    }
                }
            )
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
        if (customFolderNames.remove(oldName)) {
            val iconName = pickEmojiForFolder(newName)
            val fullNewName = "$iconName $newName"
            customFolderNames.add(fullNewName)
            // Update manual placements
            manualPlacements.entries.filter { it.value == oldName }.forEach {
                manualPlacements[it.key] = fullNewName
            }
        }
        reDispatchWithIA()
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

    // Add app from drawer directly to a specific folder
    fun addAppToFolder(packageName: String, targetFolderName: String) {
        manualPlacements[packageName] = targetFolderName
        reDispatchWithIA()
    }

    // Add app from drawer as standalone shortcut on home screen
    fun addAppToHomeScreen(packageName: String) {
        val app = _uiState.value.allApps.find { it.packageName == packageName } ?: return
        val currentPages = _uiState.value.pages.toMutableList()
        val page = currentPages.firstOrNull() ?: return

        // Find first page with room for a standalone shortcut
        val maxItems = 20
        for (i in currentPages.indices) {
            val p = currentPages[i]
            if (p.items.size < maxItems) {
                val maxPos = p.items.maxOfOrNull { it.position } ?: -1
                val newItem = ScreenItem.AppShortcut(position = maxPos + 1, appInfo = app)
                currentPages[i] = p.copy(items = p.items + newItem)
                _uiState.value = _uiState.value.copy(pages = currentPages)
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

    fun refreshApps() {
        loadApps()
    }

    // === Widgets ===

    val widgetManager = WidgetManager(context)

    init {
        // Load persisted widget IDs
        val savedIds = widgetManager.loadWidgetIds()
        if (savedIds.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(activeWidgetIds = savedIds)
        }
    }

    fun showWidgetPicker() {
        _uiState.value = _uiState.value.copy(showWidgetPicker = true)
    }

    fun hideWidgetPicker() {
        _uiState.value = _uiState.value.copy(showWidgetPicker = false)
    }

    fun getAvailableWidgets(): List<WidgetInfo> {
        val pm = context.packageManager
        return widgetManager.getInstalledWidgets().mapNotNull { info ->
            try {
                WidgetInfo(
                    providerInfo = info,
                    label = info.loadLabel(pm) ?: info.provider.className,
                    icon = info.loadIcon(context, 0),
                    appLabel = try {
                        pm.getApplicationLabel(
                            pm.getApplicationInfo(info.provider.packageName, 0)
                        ).toString()
                    } catch (_: Exception) { info.provider.packageName }
                )
            } catch (_: Exception) { null }
        }.sortedBy { it.appLabel }
    }

    fun addWidget(widgetId: Int) {
        val currentIds = _uiState.value.activeWidgetIds.toMutableList()
        if (widgetId !in currentIds) {
            currentIds.add(widgetId)
        }
        _uiState.value = _uiState.value.copy(activeWidgetIds = currentIds)
        widgetManager.saveWidgetIds(currentIds)
        Timber.d("Widget added: id=$widgetId, total=${currentIds.size}")
    }

    fun removeWidget(widgetId: Int) {
        widgetManager.deallocateWidgetId(widgetId)
        val currentIds = _uiState.value.activeWidgetIds.toMutableList()
        currentIds.remove(widgetId)
        _uiState.value = _uiState.value.copy(activeWidgetIds = currentIds)
        widgetManager.saveWidgetIds(currentIds)
    }

    override fun onCleared() {
        super.onCleared()
        widgetManager.stopListening()
    }
}
