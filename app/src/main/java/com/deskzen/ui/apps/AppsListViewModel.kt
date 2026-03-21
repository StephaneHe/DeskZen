package com.deskzen.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deskzen.data.repository.AppRepository
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.usecase.ManageShortcutUseCase
import com.deskzen.domain.usecase.ShortcutResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppsListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val manageShortcutUseCase: ManageShortcutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSystemApps = MutableStateFlow(true)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.ALPHABETICAL)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private var allApps: List<AppInfo> = emptyList()

    init {
        loadApps()
        observeSearchQuery()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = AppsUiState.Loading
            try {
                allApps = appRepository.getInstalledApps(_showSystemApps.value)
                applyFiltersAndSort()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load apps")
                _uiState.value = AppsUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
        loadApps()
    }

    fun onSortModeChanged(mode: SortMode) {
        _sortMode.value = mode
        applyFiltersAndSort()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collectLatest { applyFiltersAndSort() }
        }
    }

    suspend fun createShortcut(appInfo: AppInfo): ShortcutResult {
        return manageShortcutUseCase.createShortcut(appInfo)
    }

    suspend fun removeShortcut(packageName: String): ShortcutResult {
        return manageShortcutUseCase.removeShortcut(packageName)
    }

    private fun applyFiltersAndSort() {
        val query = _searchQuery.value.lowercase()
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.label.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
            }
        }

        val sorted = when (_sortMode.value) {
            SortMode.ALPHABETICAL -> filtered.sortedBy { it.label.lowercase() }
            SortMode.INSTALL_DATE -> filtered.sortedByDescending { it.installDate }
            SortMode.CATEGORY -> filtered.sortedBy { it.category ?: "zzz" }
            SortMode.LAST_USED -> filtered.sortedByDescending { it.lastUsedDate ?: 0L }
        }

        _uiState.value = AppsUiState.Success(
            apps = sorted,
            totalCount = allApps.size,
            filteredCount = sorted.size
        )
    }
}
