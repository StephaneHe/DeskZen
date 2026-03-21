package com.deskzen.ui.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
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
    val showSuggestions: Boolean = false,
    val suggestions: List<ThemeSuggestion> = emptyList(),
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

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val apps = appRepository.getInstalledApps(includeSystem = true)
                val suggestions = categorizer.categorize(apps)

                // Build initial pages from suggestions
                val pages = buildPagesFromSuggestions(apps, suggestions)

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

    private fun buildPagesFromSuggestions(
        apps: List<AppInfo>,
        suggestions: List<ThemeSuggestion>
    ): List<ScreenPage> {
        val pages = mutableListOf<ScreenPage>()

        // Page 1: Top apps as shortcuts + main folders
        val page1Items = mutableListOf<ScreenItem>()
        var position = 0

        // Top row: most common apps (first 4)
        val topApps = apps.take(4)
        for (app in topApps) {
            page1Items.add(ScreenItem.AppShortcut(position = position, appInfo = app))
            position++
        }

        // Remaining: category folders
        val usedPackages = topApps.map { it.packageName }.toSet()
        val mainCategories = suggestions
            .filter { it.themeName != "Autres" && it.apps.size >= 2 }
            .sortedByDescending { it.apps.size }
            .take(16) // fill rest of page

        for (theme in mainCategories) {
            val folderApps = theme.apps.filter { it.packageName !in usedPackages }
            if (folderApps.isNotEmpty() && position < 20) {
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

        // Page 2: "Autres" apps
        val autres = suggestions.find { it.themeName == "Autres" }
        if (autres != null && autres.apps.isNotEmpty()) {
            val page2Items = autres.apps
                .filter { it.packageName !in usedPackages }
                .take(20)
                .mapIndexed { index, app ->
                    ScreenItem.AppShortcut(position = index, appInfo = app)
                }
            if (page2Items.isNotEmpty()) {
                pages.add(ScreenPage(pageIndex = 1, items = page2Items))
            }
        }

        return pages
    }

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

    fun toggleSuggestions() {
        _uiState.value = _uiState.value.copy(showSuggestions = !_uiState.value.showSuggestions)
    }

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
