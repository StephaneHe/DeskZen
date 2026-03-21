package com.deskzen.ai

import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.SuggestionSource
import com.deskzen.domain.model.ThemeSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicCategorizer @Inject constructor() {

    fun categorize(apps: List<AppInfo>): List<ThemeSuggestion> {
        val themes = mutableMapOf<String, MutableList<AppInfo>>()

        for (app in apps) {
            val theme = detectTheme(app)
            themes.getOrPut(theme) { mutableListOf() }.add(app)
        }

        return themes.map { (name, themeApps) ->
            ThemeSuggestion(
                themeName = name,
                themeIcon = THEME_ICONS[name] ?: "📱",
                apps = themeApps,
                confidence = calculateConfidence(themeApps),
                source = SuggestionSource.HEURISTIC
            )
        }.sortedByDescending { it.apps.size }
    }

    private fun detectTheme(app: AppInfo): String {
        // 1. Check Android category first (most reliable)
        app.category?.let { category ->
            CATEGORY_MAP[category]?.let { return it }
        }

        // 2. Pattern matching on package name
        val pkg = app.packageName.lowercase()
        for ((theme, patterns) in PACKAGE_PATTERNS) {
            if (patterns.any { pkg.contains(it) }) return theme
        }

        // 3. Pattern matching on label
        val label = app.label.lowercase()
        for ((theme, keywords) in LABEL_KEYWORDS) {
            if (keywords.any { label.contains(it) }) return theme
        }

        return "Autres"
    }

    private fun calculateConfidence(apps: List<AppInfo>): Float {
        if (apps.isEmpty()) return 0f

        val withCategory = apps.count { it.category != null }
        val withPatternMatch = apps.count { app ->
            val pkg = app.packageName.lowercase()
            PACKAGE_PATTERNS.values.any { patterns ->
                patterns.any { pkg.contains(it) }
            }
        }

        val categoryRatio = withCategory.toFloat() / apps.size
        val patternRatio = withPatternMatch.toFloat() / apps.size

        return ((categoryRatio * 0.7f) + (patternRatio * 0.3f)).coerceIn(0.3f, 1f)
    }

    companion object {
        val CATEGORY_MAP = mapOf(
            "Jeux" to "Jeux",
            "Audio" to "Médias",
            "Vidéo" to "Médias",
            "Image" to "Photo & Vidéo",
            "Social" to "Social",
            "Actualités" to "Actualités",
            "Cartes" to "Navigation",
            "Productivité" to "Productivité",
            "Accessibilité" to "Utilitaires"
        )

        val PACKAGE_PATTERNS = mapOf(
            "Social" to listOf(
                "whatsapp", "telegram", "signal", "messenger", "viber",
                "discord", "snapchat", "tiktok", "instagram", "facebook"
            ),
            "Productivité" to listOf(
                "docs", "sheets", "slides", "notion", "slack",
                "teams", "trello", "asana", "todoist", "evernote",
                "office", "outlook", "onenote"
            ),
            "Médias" to listOf(
                "youtube", "netflix", "spotify", "podcast", "music",
                "player", "radio", "deezer", "soundcloud", "twitch",
                "primevideo", "disney"
            ),
            "Photo & Vidéo" to listOf(
                "camera", "gallery", "photo", "snapseed", "lightroom",
                "vsco", "canva", "editor"
            ),
            "Navigation" to listOf(
                "maps", "waze", "uber", "bolt", "lyft",
                "citymapper", "transit", "gps"
            ),
            "Finance" to listOf(
                "bank", "pay", "wallet", "revolut", "wise",
                "paypal", "venmo", "crypto", "trading", "bourso"
            ),
            "Santé" to listOf(
                "health", "fitness", "strava", "calm", "headspace",
                "meditation", "workout", "sport", "running"
            ),
            "Shopping" to listOf(
                "amazon", "aliexpress", "shein", "ebay", "vinted",
                "leboncoin", "wish", "zalando"
            ),
            "Utilitaires" to listOf(
                "calculator", "clock", "calendar", "settings", "files",
                "flashlight", "compass", "weather", "meteo", "cleaner"
            ),
            "Actualités" to listOf(
                "news", "reddit", "twitter", "bbc", "lemonde",
                "figaro", "press", "journal", "actu"
            ),
            "Éducation" to listOf(
                "duolingo", "coursera", "kindle", "audible",
                "learn", "study", "school", "university", "quiz"
            )
        )

        val LABEL_KEYWORDS = mapOf(
            "Jeux" to listOf("game", "jeu", "play", "puzzle", "arcade"),
            "Finance" to listOf("banque", "paiement", "argent", "money")
        )

        val THEME_ICONS = mapOf(
            "Social" to "💬",
            "Productivité" to "📊",
            "Médias" to "🎬",
            "Jeux" to "🎮",
            "Photo & Vidéo" to "📷",
            "Navigation" to "🗺️",
            "Finance" to "💳",
            "Santé" to "❤️",
            "Shopping" to "🛒",
            "Utilitaires" to "🔧",
            "Actualités" to "📰",
            "Éducation" to "📚",
            "Autres" to "📱"
        )
    }
}
