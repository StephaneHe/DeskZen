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

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val apps = appRepository.getInstalledApps(includeSystem = true)
                val suggestions = categorizer.categorize(apps)
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
        val page1Items = mutableListOf<ScreenItem>()
        var position = 0

        // All categories with 2+ apps become folders
        val categories = suggestions
            .filter { it.themeName != "Autres" && it.apps.size >= 2 }
            .sortedByDescending { it.apps.size }

        for (theme in categories) {
            if (position >= 20) break
            page1Items.add(
                ScreenItem.Folder(
                    position = position,
                    name = "${theme.themeIcon} ${theme.themeName}",
                    apps = theme.apps
                )
            )
            position++
        }

        pages.add(ScreenPage(pageIndex = 0, items = page1Items))

        // Page 2: overflow folders + "Autres"
        if (position >= 20 || suggestions.any { it.themeName == "Autres" && it.apps.isNotEmpty() }) {
            val page2Items = mutableListOf<ScreenItem>()
            var pos2 = 0

            // Overflow categories
            for (theme in categories.drop(20)) {
                if (pos2 >= 20) break
                page2Items.add(
                    ScreenItem.Folder(
                        position = pos2,
                        name = "${theme.themeIcon} ${theme.themeName}",
                        apps = theme.apps
                    )
                )
                pos2++
            }

            // "Autres" as individual apps
            val autres = suggestions.find { it.themeName == "Autres" }
            if (autres != null) {
                for (app in autres.apps) {
                    if (pos2 >= 20) break
                    page2Items.add(ScreenItem.AppShortcut(position = pos2, appInfo = app))
                    pos2++
                }
            }

            if (page2Items.isNotEmpty()) {
                pages.add(ScreenPage(pageIndex = 1, items = page2Items))
            }
        }

        return pages
    }

    // === Folder management ===

    fun showFolderManager() {
        _uiState.value = _uiState.value.copy(showFolderManager = true)
    }

    fun hideFolderManager() {
        _uiState.value = _uiState.value.copy(showFolderManager = false)
    }

    fun addFolder(name: String) {
        // Create empty folder and let IA dispatch apps into it
        val currentPages = _uiState.value.pages.toMutableList()
        val page = currentPages.firstOrNull() ?: return

        val maxPosition = page.items.maxOfOrNull { it.position } ?: -1
        val newPosition = maxPosition + 1

        if (newPosition >= 20) {
            // Page full, add to page 2
            if (currentPages.size < 2) {
                currentPages.add(ScreenPage(pageIndex = 1, items = emptyList()))
            }
            val page2 = currentPages[1]
            val maxPos2 = page2.items.maxOfOrNull { it.position } ?: -1
            val newFolder = ScreenItem.Folder(position = maxPos2 + 1, name = name, apps = emptyList())
            currentPages[1] = page2.copy(items = page2.items + newFolder)
        } else {
            val newFolder = ScreenItem.Folder(position = newPosition, name = name, apps = emptyList())
            currentPages[0] = page.copy(items = page.items + newFolder)
        }

        _uiState.value = _uiState.value.copy(pages = currentPages)
    }

    fun removeFolder(folderName: String) {
        val currentPages = _uiState.value.pages.map { page ->
            page.copy(items = page.items.filter { item ->
                !(item is ScreenItem.Folder && item.name == folderName)
            })
        }
        _uiState.value = _uiState.value.copy(pages = currentPages)
        // Re-dispatch to redistribute orphaned apps
        reDispatchWithIA()
    }

    fun renameFolder(oldName: String, newName: String) {
        val currentPages = _uiState.value.pages.map { page ->
            page.copy(items = page.items.map { item ->
                if (item is ScreenItem.Folder && item.name == oldName) {
                    item.copy(name = newName)
                } else item
            })
        }
        _uiState.value = _uiState.value.copy(pages = currentPages)
    }

    fun reDispatchWithIA() {
        viewModelScope.launch {
            val apps = _uiState.value.allApps
            if (apps.isEmpty()) return@launch

            // Get current custom folders (user-created)
            val currentFolders = _uiState.value.pages
                .flatMap { it.items }
                .filterIsInstance<ScreenItem.Folder>()
                .map { it.name }

            // Re-categorize all apps
            val suggestions = categorizer.categorize(apps)
            val pages = buildPagesFromSuggestions(apps, suggestions)

            _uiState.value = _uiState.value.copy(
                suggestions = suggestions,
                pages = pages
            )

            Timber.d("Re-dispatched ${apps.size} apps into ${suggestions.size} categories")
        }
    }

    fun getAllFolders(): List<Pair<String, Int>> {
        return _uiState.value.pages
            .flatMap { it.items }
            .filterIsInstance<ScreenItem.Folder>()
            .map { it.name to it.apps.size }
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
