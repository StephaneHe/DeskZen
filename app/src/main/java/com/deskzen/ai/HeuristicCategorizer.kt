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
        // 1. Android category (most reliable)
        app.category?.let { category ->
            CATEGORY_MAP[category]?.let { return it }
        }

        // 2. Package name patterns
        val pkg = app.packageName.lowercase()
        for ((theme, patterns) in PACKAGE_PATTERNS) {
            if (patterns.any { pkg.contains(it) }) return theme
        }

        // 3. Label keywords
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
            "Audio" to "Musique & Audio",
            "Vidéo" to "Vidéo & Streaming",
            "Image" to "Photo & Vidéo",
            "Social" to "Social",
            "Actualités" to "Actualités",
            "Cartes" to "Transport & Navigation",
            "Productivité" to "Productivité",
            "Accessibilité" to "Système"
        )

        val PACKAGE_PATTERNS = mapOf(
            // --- Communication ---
            "Social" to listOf(
                "whatsapp", "telegram", "signal", "messenger", "viber",
                "discord", "snapchat", "tiktok", "instagram", "facebook",
                "wechat", "line.", "kakaotalk", "threema", "element",
                "mastodon", "bluesky", "threads"
            ),

            // --- Productivité & Bureau ---
            "Productivité" to listOf(
                "docs", "sheets", "slides", "notion", "slack",
                "teams", "trello", "asana", "todoist", "evernote",
                "office", "outlook", "onenote", "dropbox", "drive",
                "onedrive", "icloud", "notes", "keep", "memo",
                "scanner", "pdf", "printer", "zoom", "meet",
                "webex", "skype", "jira", "confluence", "clickup",
                "monday", "airtable", "miro", "figma"
            ),

            // --- Finance & Banque ---
            "Finance" to listOf(
                "bank", "banque", "bnp", "credit", "caisse", "societe.generale",
                "lcl", "boursorama", "bourso", "fortuneo", "ing.",
                "n26", "revolut", "wise", "paypal", "venmo",
                "pay.", "wallet", "gpay", "applepay",
                "crypto", "bitcoin", "binance", "coinbase", "kraken",
                "trading", "degiro", "etoro", "bourse", "stock",
                "budget", "bankin", "linxo", "lydia", "sumup",
                "stripe", "square", "qonto", "shine", "finary",
                "max.", "orange.bank", "hello.bank", "monabanq"
            ),

            // --- Vidéo & Streaming ---
            "Vidéo & Streaming" to listOf(
                "youtube", "netflix", "primevideo", "disney", "hbo",
                "hulu", "paramount", "peacock", "crunchyroll", "funimation",
                "twitch", "dailymotion", "vimeo", "plex", "kodi",
                "vlc", "player", "molotov", "mycanal", "canal",
                "arte", "france.tv", "tf1", "salto", "ocs"
            ),

            // --- Musique & Audio ---
            "Musique & Audio" to listOf(
                "spotify", "deezer", "music", "soundcloud", "tidal",
                "podcast", "radio", "shazam", "audible", "audiomack",
                "tunein", "castbox", "overcast", "pocketcasts", "anghami",
                "apple.music", "amazon.music"
            ),

            // --- Photo & Vidéo ---
            "Photo & Vidéo" to listOf(
                "camera", "gallery", "photo", "snapseed", "lightroom",
                "vsco", "canva", "editor", "picsart", "capcut",
                "inshot", "gopro", "filmic", "procreate", "pixlr",
                "remini", "faceapp", "pics", "collage"
            ),

            // --- Transport & Navigation ---
            "Transport & Navigation" to listOf(
                "maps", "waze", "uber", "bolt", "lyft",
                "citymapper", "transit", "gps", "blablacar", "kapten",
                "freenow", "heetch", "lime", "bird", "tier",
                "sncf", "ratp", "trainline", "flixbus", "ouigo",
                "moovit", "tomtom", "navmii", "coyote"
            ),

            // --- Santé & Bien-être ---
            "Santé & Bien-être" to listOf(
                "health", "fitness", "strava", "calm", "headspace",
                "meditation", "workout", "sport", "running", "nike",
                "adidas", "fitbit", "garmin", "myfitnesspal",
                "sleep", "flo.", "clue", "yuka", "doctolib",
                "pharmacie", "sante", "ameli", "alan", "qare",
                "withings", "polar", "freeletics", "peloton"
            ),

            // --- Shopping & Achats ---
            "Shopping" to listOf(
                "amazon", "aliexpress", "shein", "ebay", "vinted",
                "leboncoin", "wish", "zalando", "asos", "cdiscount",
                "fnac", "darty", "ldlc", "boulanger", "ikea",
                "leroy.merlin", "castorama", "etsy", "rakuten",
                "veepee", "showroom", "manomano", "decathlon"
            ),

            // --- Alimentation & Livraison ---
            "Food & Livraison" to listOf(
                "ubereats", "deliveroo", "justeat", "doordash", "grubhub",
                "glovo", "frichti", "toogoodtogo", "phenix",
                "mcdonald", "burger", "domino", "pizza",
                "starbucks", "restaurant", "food", "recipe",
                "marmiton", "jow", "hellofresh", "quitoque"
            ),

            // --- Voyage & Hébergement ---
            "Voyage" to listOf(
                "booking", "airbnb", "expedia", "tripadvisor", "kayak",
                "skyscanner", "hotel", "hostel", "flight", "avion",
                "airport", "airline", "easyjet", "ryanair", "airfrance",
                "klm", "lufthansa", "vueling", "amadeus",
                "trip.com", "agoda", "trivago", "hopper", "omio"
            ),

            // --- Utilitaires & Système ---
            "Système" to listOf(
                "calculator", "clock", "calendar", "settings", "files",
                "flashlight", "compass", "weather", "meteo", "cleaner",
                "antivirus", "vpn", "password", "authenticator",
                "bitwarden", "lastpass", "1password", "nordvpn",
                "expressvpn", "proton", "wifi", "bluetooth",
                "manager", "monitor", "battery", "booster",
                "launcher", "keyboard", "gboard", "swiftkey"
            ),

            // --- Actualités & Lecture ---
            "Actualités" to listOf(
                "news", "reddit", "twitter", "bbc", "lemonde",
                "figaro", "press", "journal", "actu", "liberation",
                "mediapart", "ouest.france", "20minutes", "bfm",
                "cnews", "lci", "rss", "feedly", "flipboard",
                "pocket", "medium", "substack", "reuters", "afp"
            ),

            // --- Éducation & Apprentissage ---
            "Éducation" to listOf(
                "duolingo", "coursera", "kindle", "udemy", "skillshare",
                "learn", "study", "school", "university", "quiz",
                "khan", "edx", "openclassroom", "babbel", "busuu",
                "memrise", "anki", "quizlet", "wikipedia", "dictionnaire",
                "translator", "translate", "deepl", "wordreference"
            ),

            // --- Jeux (complément au CATEGORY_GAME) ---
            "Jeux" to listOf(
                "game", "games", "clash", "candy", "pubg",
                "fortnite", "roblox", "minecraft", "pokemon", "supercell",
                "gameloft", "ea.com", "ubisoft", "steam", "epic",
                "playstation", "xbox", "nintendo", "chess", "sudoku",
                "wordle", "among.us"
            ),

            // --- Immobilier ---
            "Immobilier" to listOf(
                "seloger", "leboncoin.immo", "bien.ici", "pap.",
                "logic.immo", "orpi", "century21", "laforet",
                "immo", "realestate", "zillow", "realtor",
                "appartement", "meilleurs.agents"
            ),

            // --- Emploi & Carrière ---
            "Emploi" to listOf(
                "linkedin", "indeed", "glassdoor", "monster",
                "pole.emploi", "hellowork", "welcome.to.the.jungle",
                "cadremploi", "apec", "interim", "randstad", "adecco"
            ),

            // --- Enfants & Famille ---
            "Famille" to listOf(
                "kids", "enfant", "bebe", "baby", "family",
                "parentale", "grossesse", "ludo", "coloring",
                "disney.junior", "youtube.kids", "toca"
            )
        )

        val LABEL_KEYWORDS = mapOf(
            "Jeux" to listOf("game", "jeu", "play", "puzzle", "arcade", "casino", "solitaire"),
            "Finance" to listOf("banque", "paiement", "argent", "money", "bank", "finance", "bourse", "trading", "crédit", "assurance"),
            "Shopping" to listOf("shop", "store", "boutique", "achat", "promo", "soldes"),
            "Food & Livraison" to listOf("restaurant", "pizza", "burger", "sushi", "livraison", "delivery", "recette", "cuisine"),
            "Voyage" to listOf("hotel", "vol", "flight", "voyage", "travel", "vacances", "réservation"),
            "Santé & Bien-être" to listOf("santé", "health", "médecin", "pharmacie", "fitness", "yoga", "méditation"),
            "Éducation" to listOf("cours", "apprendre", "learn", "langue", "language", "dictionnaire"),
            "Immobilier" to listOf("immobilier", "appartement", "maison", "louer", "acheter"),
            "Emploi" to listOf("emploi", "job", "recrutement", "cv", "carrière"),
            "Transport & Navigation" to listOf("taxi", "vtc", "train", "bus", "métro", "trottinette", "vélo"),
            "Musique & Audio" to listOf("musique", "music", "radio", "podcast"),
            "Vidéo & Streaming" to listOf("film", "série", "stream", "vidéo", "replay", "tv"),
            "Famille" to listOf("enfant", "kids", "bébé", "famille", "parental")
        )

        val THEME_ICONS = mapOf(
            "Social" to "💬",
            "Productivité" to "📊",
            "Finance" to "💳",
            "Vidéo & Streaming" to "🎬",
            "Musique & Audio" to "🎵",
            "Jeux" to "🎮",
            "Photo & Vidéo" to "📷",
            "Transport & Navigation" to "🗺️",
            "Santé & Bien-être" to "❤️",
            "Shopping" to "🛒",
            "Food & Livraison" to "🍔",
            "Voyage" to "✈️",
            "Système" to "🔧",
            "Actualités" to "📰",
            "Éducation" to "📚",
            "Immobilier" to "🏠",
            "Emploi" to "💼",
            "Famille" to "👨‍👩‍👧",
            "Autres" to "📱"
        )
    }
}
