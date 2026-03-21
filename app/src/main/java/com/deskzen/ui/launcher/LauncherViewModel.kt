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

    // Custom folders created by the user (name -> isCustom)
    private val customFolderNames = mutableSetOf<String>()

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

        // 1. Custom folders first (user-created) — try to fill them with IA
        for (customName in customFolderNames) {
            val cleanName = customName.replace(Regex("^\\p{So}\\s*"), "").trim()
            val matchedApps = findAppsForCustomFolder(cleanName, apps, assignedPackages)
            assignedPackages.addAll(matchedApps.map { it.packageName })
            allItems.add(
                ScreenItem.Folder(
                    position = allItems.size,
                    name = customName,
                    apps = matchedApps
                )
            )
        }

        // 2. IA categories (only apps not already assigned)
        val iaCategories = suggestions
            .filter { it.themeName != "Autres" && it.apps.size >= 2 }
            .sortedByDescending { it.apps.size }

        for (theme in iaCategories) {
            val folderName = "${theme.themeIcon} ${theme.themeName}"
            // Skip if a custom folder covers this theme
            if (customFolderNames.any { it.contains(theme.themeName, ignoreCase = true) }) continue

            val remainingApps = theme.apps.filter { it.packageName !in assignedPackages }
            if (remainingApps.isEmpty()) continue

            assignedPackages.addAll(remainingApps.map { it.packageName })
            allItems.add(
                ScreenItem.Folder(
                    position = allItems.size,
                    name = folderName,
                    apps = remainingApps
                )
            )
        }

        // 3. "Autres" — unassigned apps
        val autresApps = apps.filter { it.packageName !in assignedPackages }
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
        reDispatchWithIA()
    }

    fun renameFolder(oldName: String, newName: String) {
        if (customFolderNames.remove(oldName)) {
            val iconName = pickEmojiForFolder(newName)
            customFolderNames.add("$iconName $newName")
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
}
