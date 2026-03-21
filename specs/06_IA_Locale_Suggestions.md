# Module 6 — IA Locale — Suggestions

> **Prérequis** : Lire `00_Document_Central.md` pour le contexte global.
> **Dépend de** : Modules 1 (Infrastructure), 2 (Liste Applications)

---

## Objectif

Proposer une organisation thématique intelligente des applications installées, calculée entièrement en local (pas de réseau), avec possibilité d'accepter/refuser chaque suggestion individuellement.

---

## Approche technique

### Pourquoi une IA locale ?

- **Vie privée** : la liste des apps installées est une donnée sensible
- **Hors-ligne** : fonctionne sans connexion
- **Instantané** : pas de latence réseau

### Stratégie à deux niveaux

| Niveau | Méthode | Quand |
|--------|---------|-------|
| **Règles heuristiques** | Catégorisation par catégorie Play Store + package name patterns | Toujours (fallback fiable) |
| **Modèle ML léger** | Classification par embedding de nom + catégorie | Si disponible (optionnel, améliore la qualité) |

Le niveau heuristique est la base et doit fonctionner parfaitement seul. Le modèle ML est un bonus qui affine les résultats.

---

## Tâches

### 6.1 — Catégorisation heuristique

**Entrée** : Liste d'AppInfo (Module 2)
**Sortie** : Regroupement par thèmes

**Thèmes prédéfinis** :

| Thème | Icône | Exemples de patterns |
|-------|-------|---------------------|
| Social | 💬 | `whatsapp`, `telegram`, `signal`, `messenger`, catégorie SOCIAL |
| Productivité | 📊 | `docs`, `sheets`, `notion`, `slack`, `teams`, catégorie PRODUCTIVITY |
| Médias | 🎬 | `youtube`, `netflix`, `spotify`, `podcast`, catégorie VIDEO/AUDIO |
| Jeux | 🎮 | Catégorie GAME |
| Photo & Vidéo | 📷 | `camera`, `gallery`, `photo`, `snapseed`, catégorie IMAGE |
| Navigation | 🗺️ | `maps`, `waze`, `uber`, `bolt`, catégorie MAPS |
| Finance | 💳 | `bank`, `pay`, `wallet`, `revolut`, `wise` |
| Santé | ❤️ | `health`, `fitness`, `strava`, `calm`, `headspace` |
| Shopping | 🛒 | `amazon`, `aliexpress`, `shein`, `ebay` |
| Utilitaires | 🔧 | `calculator`, `clock`, `calendar`, `settings`, `files` |
| Actualités | 📰 | `news`, `reddit`, `twitter`, `bbc`, catégorie NEWS |
| Éducation | 📚 | `duolingo`, `coursera`, `kindle`, `audible` |

```kotlin
// ai/HeuristicCategorizer.kt
class HeuristicCategorizer {

    fun categorize(apps: List<AppInfo>): List<ThemeSuggestion> {
        val themes = mutableMapOf<String, MutableList<AppInfo>>()

        for (app in apps) {
            val theme = detectTheme(app)
            themes.getOrPut(theme) { mutableListOf() }.add(app)
        }

        return themes.map { (name, apps) ->
            ThemeSuggestion(
                themeName = name,
                apps = apps,
                confidence = calculateConfidence(apps),
                source = SuggestionSource.HEURISTIC
            )
        }.sortedByDescending { it.apps.size }
    }

    private fun detectTheme(app: AppInfo): String {
        // 1. Vérifier la catégorie Android (plus fiable)
        app.category?.let { return mapAndroidCategory(it) }

        // 2. Pattern matching sur le package name
        val pkg = app.packageName.lowercase()
        for ((theme, patterns) in PACKAGE_PATTERNS) {
            if (patterns.any { pkg.contains(it) }) return theme
        }

        // 3. Pattern matching sur le label
        val label = app.label.lowercase()
        for ((theme, keywords) in LABEL_KEYWORDS) {
            if (keywords.any { label.contains(it) }) return theme
        }

        return "Autres"
    }

    companion object {
        val PACKAGE_PATTERNS = mapOf(
            "Social" to listOf("whatsapp", "telegram", "signal", "messenger", "viber",
                              "discord", "snapchat", "tiktok", "instagram"),
            "Productivité" to listOf("docs", "sheets", "slides", "notion", "slack",
                                    "teams", "trello", "asana", "todoist", "evernote"),
            // ... etc pour chaque thème
        )
    }
}
```

**Tests** :
```kotlin
class HeuristicCategorizerTest {
    private val categorizer = HeuristicCategorizer()

    @Test
    fun `WhatsApp is categorized as Social`() {
        val app = testApp(packageName = "com.whatsapp", label = "WhatsApp")
        val result = categorizer.categorize(listOf(app))
        assertEquals("Social", result.first().themeName)
    }

    @Test
    fun `app with Android category uses category first`() {
        val app = testApp(packageName = "com.unknown", category = "Jeux")
        val result = categorizer.categorize(listOf(app))
        assertEquals("Jeux", result.first().themeName)
    }

    @Test
    fun `unknown app goes to Autres`() {
        val app = testApp(packageName = "com.xyz.abc123", label = "Xyz")
        val result = categorizer.categorize(listOf(app))
        assertEquals("Autres", result.first().themeName)
    }

    @Test
    fun `all test apps are categorized`() {
        val apps = generateRealWorldAppList() // 50+ apps réalistes
        val result = categorizer.categorize(apps)
        val totalCategorized = result.sumOf { it.apps.size }
        assertEquals(apps.size, totalCategorized)
    }

    @Test
    fun `themes are sorted by app count descending`() {
        val result = categorizer.categorize(generateRealWorldAppList())
        val sizes = result.map { it.apps.size }
        assertEquals(sizes.sortedDescending(), sizes)
    }
}
```

---

### 6.2 — Modèle ML embarqué (optionnel)

**Entrée** : Tâche 6.1
**Sortie** : Classification améliorée via modèle léger

**Approche recommandée** :
- Modèle : MobileBERT tiny ou DistilBERT quantifié (INT8)
- Taille cible : < 20 Mo
- Runtime : ONNX Runtime Mobile ou TensorFlow Lite
- Input : `label + " " + packageName.split(".").last()`
- Output : vecteur de probabilités sur les 12 thèmes

```kotlin
// ai/MlCategorizer.kt
class MlCategorizer(
    private val context: Context
) {
    private var session: OrtSession? = null

    fun isAvailable(): Boolean = session != null

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                val env = OrtEnvironment.getEnvironment()
                val modelBytes = context.assets.open("ml/app_categorizer.onnx").readBytes()
                session = env.createSession(modelBytes)
            } catch (e: Exception) {
                Timber.w(e, "ML model not available, falling back to heuristics")
            }
        }
    }

    suspend fun categorize(apps: List<AppInfo>): List<ThemeSuggestion>? {
        val session = session ?: return null  // Fallback signal
        // ... tokenize, infer, decode
    }
}
```

> **Note pour Claude Code** : Si l'intégration ONNX est trop complexe pour une V1, se concentrer sur l'heuristique (tâche 6.1) qui couvre déjà 85%+ des cas. Le ML peut être ajouté en V2.

---

### 6.3 — Modèle de suggestion

**Entrée** : Tâches 6.1, 6.2
**Sortie** : Structure de données pour les suggestions

```kotlin
// domain/model/ThemeSuggestion.kt
data class ThemeSuggestion(
    val themeName: String,
    val themeIcon: String,             // Emoji ou nom d'icône Material
    val apps: List<AppInfo>,
    val confidence: Float,             // 0.0 - 1.0
    val source: SuggestionSource,
    val status: SuggestionStatus = SuggestionStatus.PENDING
)

enum class SuggestionSource { HEURISTIC, ML }

enum class SuggestionStatus {
    PENDING,    // Pas encore vue par l'utilisateur
    ACCEPTED,   // Thème accepté entièrement
    PARTIAL,    // Certaines apps acceptées, d'autres rejetées
    REJECTED    // Thème entièrement rejeté
}

// Pour le contrôle app par app dans un thème
data class AppSuggestion(
    val appInfo: AppInfo,
    val suggestedTheme: String,
    val isAccepted: Boolean = true  // true par défaut, l'utilisateur peut décocher
)
```

---

### 6.4 — Écran UI : Suggestions IA

**Entrée** : Tâches 6.1–6.3
**Sortie** : Écran de l'onglet "Suggestions" avec accept/reject par thème et par app

**Interactions** :

| Action | Effet |
|--------|-------|
| Toggle thème (✓/✕) | Accepte/rejette tout le thème |
| Décocher une app | L'app sort du thème (reste dans "Autres") |
| Drag une app vers un autre thème | Déplace l'app entre thèmes |
| "Regénérer" | Relance la catégorisation |
| "Appliquer les suggestions" | Crée les dossiers dans la vue Écran (Module 5) |

**Composables** :

| Composable | Fichier | Rôle |
|-----------|---------|------|
| `SuggestionsScreen` | `ui/suggestions/SuggestionsScreen.kt` | Écran principal |
| `ThemeCard` | `ui/suggestions/ThemeCard.kt` | Carte d'un thème avec apps |
| `AppSuggestionRow` | `ui/suggestions/AppSuggestionRow.kt` | Ligne d'une app dans un thème |
| `ApplySuggestionsDialog` | `ui/suggestions/ApplySuggestionsDialog.kt` | Confirmation d'application |

**Tests** :
```kotlin
class SuggestionsScreenTest {
    @Test
    fun `shows theme cards for each suggestion`()

    @Test
    fun `toggle theme changes all app checkboxes`()

    @Test
    fun `unchecking single app keeps theme partially accepted`()

    @Test
    fun `regenerate button triggers new categorization`()

    @Test
    fun `apply creates folders in screen layout`()

    @Test
    fun `rejected themes are not applied`()

    @Test
    fun `drag app between themes works`()

    @Test
    fun `confidence indicator shown per theme`()
}
```

---

### 6.5 — Intégration avec Module 5 (Profils)

**Entrée** : Tâche 6.4, Module 5
**Sortie** : Les suggestions acceptées créent un profil d'organisation

**Flux** :
1. L'utilisateur accepte/modifie les suggestions
2. Tap "Appliquer les suggestions"
3. Dialog de confirmation avec prévisualisation miniature
4. DeskZen génère un `OrganizationProfile` avec `source = AI`
5. Le profil est sauvegardé et activé
6. Navigation vers l'onglet "Écran" pour voir le résultat

---

## Livrables

| Fichier | Description |
|---------|-------------|
| `ai/HeuristicCategorizer.kt` | Catégorisation par règles |
| `ai/MlCategorizer.kt` | Catégorisation ML (optionnel) |
| `ai/AppCategorizerFacade.kt` | Facade combinant les deux |
| `domain/model/ThemeSuggestion.kt` | Modèles de suggestion |
| `ui/suggestions/SuggestionsScreen.kt` | Écran principal |
| `ui/suggestions/SuggestionsViewModel.kt` | ViewModel |
| `ui/suggestions/ThemeCard.kt` | Carte thème |
| `ui/suggestions/AppSuggestionRow.kt` | Ligne app |
| `assets/ml/app_categorizer.onnx` | Modèle ML (optionnel) |
| Tests unitaires + UI | Tous les fichiers de test |

## Vérification

```bash
./gradlew testDebugUnitTest --tests "com.deskzen.ai.*"
./gradlew testDebugUnitTest --tests "com.deskzen.ui.suggestions.*"
./gradlew connectedDebugAndroidTest --tests "com.deskzen.ui.suggestions.*"
```

> **Priorité** : La catégorisation heuristique (6.1) est le MVP. Le modèle ML (6.2) est un nice-to-have pour V2.
