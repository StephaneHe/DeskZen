package com.deskzen.ui.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deskzen.data.repository.LauncherDetector
import com.deskzen.data.repository.ScreenRepository
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
    data class Success(val pages: List<ScreenPage>) : HomeScreenUiState
    data class Error(val message: String) : HomeScreenUiState
}

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val screenRepository: ScreenRepository,
    private val launcherDetector: LauncherDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeScreenUiState>(HomeScreenUiState.Loading)
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    init {
        loadScreenLayout()
    }

    private fun loadScreenLayout() {
        viewModelScope.launch {
            try {
                // First try to load from saved DB
                val savedPages = screenRepository.getScreenLayout()
                    .catch { Timber.e(it, "Error reading saved layout") }
                    .first()

                if (savedPages.isNotEmpty() && savedPages.any { it.items.isNotEmpty() }) {
                    _uiState.value = HomeScreenUiState.Success(savedPages)
                } else {
                    // DB is empty — detect from launcher or installed apps
                    Timber.d("No saved layout, detecting from launcher...")
                    val detectedPages = launcherDetector.tryReadLauncherConfig()
                    if (detectedPages != null && detectedPages.isNotEmpty()) {
                        _uiState.value = HomeScreenUiState.Success(detectedPages)
                        // Save for next time
                        screenRepository.saveScreenLayout(detectedPages)
                    } else {
                        _uiState.value = HomeScreenUiState.Success(
                            listOf(ScreenPage(pageIndex = 0, items = emptyList()))
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load screen layout")
                // Fallback: try launcher detection even on error
                try {
                    val detected = launcherDetector.tryReadLauncherConfig()
                    if (detected != null && detected.isNotEmpty()) {
                        _uiState.value = HomeScreenUiState.Success(detected)
                        return@launch
                    }
                } catch (_: Exception) {}
                _uiState.value = HomeScreenUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun onPageChanged(page: Int) {
        _currentPage.value = page
    }
}
