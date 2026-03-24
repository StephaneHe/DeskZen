package com.deskzen.ui.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deskzen.data.repository.AppRepository
import com.deskzen.data.repository.ScreenRepository
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface HomeScreenUiState {
    data object Loading : HomeScreenUiState
    data class Success(
        val pages: List<ScreenPage>,
        val availableApps: List<AppInfo> = emptyList(),
        val showAddAppSheet: Boolean = false,
        val showCreateFolderDialog: Boolean = false,
        val selectedPageIndex: Int = 0,
        val selectedPosition: Int = -1
    ) : HomeScreenUiState
    data class Error(val message: String) : HomeScreenUiState
}

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val screenRepository: ScreenRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeScreenUiState>(HomeScreenUiState.Loading)
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private var allPages: MutableList<ScreenPage> = mutableListOf()

    init {
        loadScreenLayout()
    }

    private fun loadScreenLayout() {
        viewModelScope.launch {
            try {
                val savedPages = screenRepository.getScreenLayout()
                    .catch { Timber.e(it, "Error reading saved layout") }
                    .first()

                if (savedPages.isNotEmpty() && savedPages.any { it.items.isNotEmpty() }) {
                    allPages = savedPages.toMutableList()
                } else {
                    // Start with 2 empty pages
                    allPages = mutableListOf(
                        ScreenPage(pageIndex = 0, items = emptyList()),
                        ScreenPage(pageIndex = 1, items = emptyList())
                    )
                }
                emitSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load screen layout")
                allPages = mutableListOf(ScreenPage(pageIndex = 0, items = emptyList()))
                emitSuccess()
            }
        }
    }

    fun onPageChanged(page: Int) {
        _currentPage.value = page
    }

    fun onEmptySlotTap(pageIndex: Int, position: Int) {
        viewModelScope.launch {
            val appsOnScreen = allPages.flatMap { page ->
                page.items.flatMap { item ->
                    when (item) {
                        is ScreenItem.AppShortcut -> listOf(item.appInfo.packageName)
                        is ScreenItem.Folder -> item.apps.map { it.packageName }
                        is ScreenItem.WebShortcut -> emptyList()
                    }
                }
            }
            val allApps = appRepository.getInstalledApps(includeSystem = true)
            val available = allApps.filter { it.packageName !in appsOnScreen }

            val state = _uiState.value as? HomeScreenUiState.Success ?: return@launch
            _uiState.value = state.copy(
                showAddAppSheet = true,
                availableApps = available,
                selectedPageIndex = pageIndex,
                selectedPosition = position
            )
        }
    }

    fun onAddApp(appInfo: AppInfo) {
        val state = _uiState.value as? HomeScreenUiState.Success ?: return
        val pageIndex = state.selectedPageIndex
        val position = state.selectedPosition

        val page = allPages.getOrNull(pageIndex) ?: return
        val newItem = ScreenItem.AppShortcut(position = position, appInfo = appInfo)
        val updatedItems = page.items + newItem
        allPages[pageIndex] = page.copy(items = updatedItems)

        saveAndEmit()
    }

    fun onRemoveItem(pageIndex: Int, position: Int) {
        val page = allPages.getOrNull(pageIndex) ?: return
        val updatedItems = page.items.filter { it.position != position }
        allPages[pageIndex] = page.copy(items = updatedItems)

        saveAndEmit()
    }

    fun onCreateFolder(pageIndex: Int, position: Int, name: String) {
        val page = allPages.getOrNull(pageIndex) ?: return
        val newFolder = ScreenItem.Folder(position = position, name = name, apps = emptyList())
        val updatedItems = page.items + newFolder
        allPages[pageIndex] = page.copy(items = updatedItems)

        saveAndEmit()
    }

    fun onAddAppToFolder(pageIndex: Int, folderPosition: Int, appInfo: AppInfo) {
        val page = allPages.getOrNull(pageIndex) ?: return
        val updatedItems = page.items.map { item ->
            if (item is ScreenItem.Folder && item.position == folderPosition) {
                item.copy(apps = item.apps + appInfo)
            } else item
        }
        allPages[pageIndex] = page.copy(items = updatedItems)

        saveAndEmit()
    }

    fun onRemoveAppFromFolder(pageIndex: Int, folderPosition: Int, packageName: String) {
        val page = allPages.getOrNull(pageIndex) ?: return
        val updatedItems = page.items.map { item ->
            if (item is ScreenItem.Folder && item.position == folderPosition) {
                item.copy(apps = item.apps.filter { it.packageName != packageName })
            } else item
        }
        allPages[pageIndex] = page.copy(items = updatedItems)

        saveAndEmit()
    }

    fun onAddPage() {
        val newIndex = allPages.size
        allPages.add(ScreenPage(pageIndex = newIndex, items = emptyList()))
        emitSuccess()
    }

    fun onDismissSheet() {
        val state = _uiState.value as? HomeScreenUiState.Success ?: return
        _uiState.value = state.copy(showAddAppSheet = false, showCreateFolderDialog = false)
    }

    fun onShowCreateFolderDialog(pageIndex: Int, position: Int) {
        val state = _uiState.value as? HomeScreenUiState.Success ?: return
        _uiState.value = state.copy(
            showCreateFolderDialog = true,
            selectedPageIndex = pageIndex,
            selectedPosition = position
        )
    }

    private fun saveAndEmit() {
        viewModelScope.launch {
            try { screenRepository.saveScreenLayout(allPages) }
            catch (e: Exception) { Timber.e(e, "Failed to save layout") }
        }
        emitSuccess()
    }

    private fun emitSuccess() {
        _uiState.value = HomeScreenUiState.Success(
            pages = allPages.toList(),
            showAddAppSheet = false,
            showCreateFolderDialog = false
        )
    }
}
