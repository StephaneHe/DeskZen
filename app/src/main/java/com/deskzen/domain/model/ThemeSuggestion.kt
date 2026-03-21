package com.deskzen.domain.model

data class ThemeSuggestion(
    val themeName: String,
    val themeIcon: String,
    val apps: List<AppInfo>,
    val confidence: Float,
    val source: SuggestionSource,
    val status: SuggestionStatus = SuggestionStatus.PENDING
)

enum class SuggestionSource { HEURISTIC, ML }

enum class SuggestionStatus {
    PENDING,
    ACCEPTED,
    PARTIAL,
    REJECTED
}

data class AppSuggestion(
    val appInfo: AppInfo,
    val suggestedTheme: String,
    val isAccepted: Boolean = true
)
