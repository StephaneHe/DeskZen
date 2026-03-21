package com.deskzen.ui.apps

import com.deskzen.domain.model.AppInfo

sealed interface AppsUiState {
    data object Loading : AppsUiState
    data class Success(
        val apps: List<AppInfo>,
        val totalCount: Int,
        val filteredCount: Int
    ) : AppsUiState
    data class Error(val message: String) : AppsUiState
}

enum class SortMode {
    ALPHABETICAL,
    INSTALL_DATE,
    CATEGORY,
    LAST_USED
}
