package com.deskzen.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deskzen.ai.AppCategorizerFacade
import com.deskzen.data.repository.AppRepository
import com.deskzen.domain.model.SuggestionStatus
import com.deskzen.domain.model.ThemeSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface SuggestionsUiState {
    data object Loading : SuggestionsUiState
    data class Success(val suggestions: List<ThemeSuggestion>) : SuggestionsUiState
    data class Error(val message: String) : SuggestionsUiState
}

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val categorizer: AppCategorizerFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow<SuggestionsUiState>(SuggestionsUiState.Loading)
    val uiState: StateFlow<SuggestionsUiState> = _uiState.asStateFlow()

    init {
        generateSuggestions()
    }

    fun generateSuggestions() {
        viewModelScope.launch {
            _uiState.value = SuggestionsUiState.Loading
            try {
                val apps = appRepository.getInstalledApps(includeSystem = true)
                val suggestions = categorizer.categorize(apps)
                _uiState.value = SuggestionsUiState.Success(suggestions)
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate suggestions")
                _uiState.value = SuggestionsUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun onToggleTheme(themeName: String) {
        val state = _uiState.value as? SuggestionsUiState.Success ?: return
        val updated = state.suggestions.map { suggestion ->
            if (suggestion.themeName == themeName) {
                val newStatus = when (suggestion.status) {
                    SuggestionStatus.ACCEPTED, SuggestionStatus.PARTIAL -> SuggestionStatus.REJECTED
                    SuggestionStatus.REJECTED -> SuggestionStatus.ACCEPTED
                    SuggestionStatus.PENDING -> SuggestionStatus.ACCEPTED
                }
                suggestion.copy(status = newStatus)
            } else suggestion
        }
        _uiState.value = SuggestionsUiState.Success(updated)
    }

    fun onToggleApp(themeName: String, packageName: String) {
        val state = _uiState.value as? SuggestionsUiState.Success ?: return
        val updated = state.suggestions.map { suggestion ->
            if (suggestion.themeName == themeName) {
                val updatedApps = suggestion.apps.filterNot { it.packageName == packageName }
                suggestion.copy(
                    apps = updatedApps,
                    status = if (updatedApps.isEmpty()) SuggestionStatus.REJECTED
                    else SuggestionStatus.PARTIAL
                )
            } else suggestion
        }
        _uiState.value = SuggestionsUiState.Success(updated)
    }

    fun getAcceptedSuggestions(): List<ThemeSuggestion> {
        val state = _uiState.value as? SuggestionsUiState.Success ?: return emptyList()
        return state.suggestions.filter {
            it.status == SuggestionStatus.ACCEPTED || it.status == SuggestionStatus.PARTIAL
        }
    }
}
