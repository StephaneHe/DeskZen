package com.deskzen.ai

import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ThemeSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCategorizerFacade @Inject constructor(
    private val heuristicCategorizer: HeuristicCategorizer
) {
    fun categorize(apps: List<AppInfo>): List<ThemeSuggestion> {
        // For V1, use heuristic only
        // ML categorizer can be added in V2
        return heuristicCategorizer.categorize(apps)
    }
}
