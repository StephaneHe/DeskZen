# Module 2 — Liste des Applications

> **Prérequis** : Lire `00_Document_Central.md` pour le contexte global.
> **Dépend de** : Module 1 (Infrastructure)

---

## Objectif

Afficher la liste complète des applications installées sur le téléphone, avec recherche, filtrage et tri. C'est l'écran principal de l'onglet "Apps".

---

## Tâches

### 2.1 — Modèle de données AppInfo

**Entrée** : Rien
**Sortie** : Data class représentant une application installée

```kotlin
// domain/model/AppInfo.kt
data class AppInfo(
    val packageName: String,          // "com.whatsapp"
    val label: String,                // "WhatsApp"
    val icon: Drawable?,              // Icône de l'app
    val isSystemApp: Boolean,         // true si app système
    val installDate: Long,            // Timestamp d'installation
    val lastUsedDate: Long?,          // Dernier usage (si dispo)
    val category: String?,            // Catégorie Play Store (si dispo)
    val versionName: String?,         // "2.24.1.6"
    val isOnHomeScreen: Boolean       // true si raccourci existant
)
```

**Tests** :
```kotlin
class AppInfoTest {
    @Test
    fun `AppInfo equality is based on packageName`() {
        val app1 = AppInfo(packageName = "com.test", label = "Test", ...)
        val app2 = AppInfo(packageName = "com.test", label = "Test Modified", ...)
        assertEquals(app1.packageName, app2.packageName)
    }
}
```

---

### 2.2 — Repository : lecture des apps installées

**Entrée** : Tâche 2.1
**Sortie** : Repository qui interroge le `PackageManager`

```kotlin
// data/repository/AppRepository.kt
interface AppRepository {
    suspend fun getInstalledApps(includeSystem: Boolean = false): List<AppInfo>
    suspend fun getAppInfo(packageName: String): AppInfo?
    suspend fun searchApps(query: String, includeSystem: Boolean = false): List<AppInfo>
}
```

```kotlin
// data/repository/AppRepositoryImpl.kt
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    override suspend fun getInstalledApps(includeSystem: Boolean): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = pm.queryIntentActivities(intent, 0)

            activities.mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                if (!includeSystem && isSystem) return@mapNotNull null

                AppInfo(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    isSystemApp = isSystem,
                    installDate = getInstallDate(pm, appInfo.packageName),
                    lastUsedDate = getLastUsedDate(appInfo.packageName),
                    category = getCategoryName(appInfo.category),
                    versionName = getVersionName(pm, appInfo.packageName),
                    isOnHomeScreen = false // Sera enrichi par Module 3
                )
            }.sortedBy { it.label.lowercase() }
        }
    }
    // ... autres méthodes
}
```

**Mapping catégories Android** :
```kotlin
private fun getCategoryName(category: Int): String? = when (category) {
    ApplicationInfo.CATEGORY_GAME -> "Jeux"
    ApplicationInfo.CATEGORY_AUDIO -> "Audio"
    ApplicationInfo.CATEGORY_VIDEO -> "Vidéo"
    ApplicationInfo.CATEGORY_IMAGE -> "Image"
    ApplicationInfo.CATEGORY_SOCIAL -> "Social"
    ApplicationInfo.CATEGORY_NEWS -> "Actualités"
    ApplicationInfo.CATEGORY_MAPS -> "Cartes"
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivité"
    ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Accessibilité"
    else -> null
}
```

**Tests** :
```kotlin
class AppRepositoryImplTest {
    private lateinit var repository: AppRepositoryImpl
    private val context: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)

    @Before
    fun setup() {
        every { context.packageManager } returns packageManager
        repository = AppRepositoryImpl(context)
    }

    @Test
    fun `getInstalledApps returns only launcher apps`() = runTest {
        // Mock queryIntentActivities to return 3 apps (1 system, 2 user)
        val apps = repository.getInstalledApps(includeSystem = false)
        assertEquals(2, apps.size)
    }

    @Test
    fun `getInstalledApps with includeSystem returns all`() = runTest {
        val apps = repository.getInstalledApps(includeSystem = true)
        assertEquals(3, apps.size)
    }

    @Test
    fun `searchApps filters by label`() = runTest {
        val results = repository.searchApps("what")
        assertTrue(results.all { it.label.contains("what", ignoreCase = true) })
    }

    @Test
    fun `results are sorted alphabetically`() = runTest {
        val apps = repository.getInstalledApps()
        assertEquals(apps.sortedBy { it.label.lowercase() }, apps)
    }
}
```

---

### 2.3 — ViewModel

**Entrée** : Tâche 2.2
**Sortie** : ViewModel exposant l'état de la liste des apps

```kotlin
// ui/apps/AppsListViewModel.kt
@HiltViewModel
class AppsListViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.ALPHABETICAL)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    init { loadApps() }

    fun loadApps() { /* ... */ }
    fun onSearchQueryChanged(query: String) { /* ... */ }
    fun onToggleSystemApps() { /* ... */ }
    fun onSortModeChanged(mode: SortMode) { /* ... */ }
}

sealed interface AppsUiState {
    data object Loading : AppsUiState
    data class Success(
        val apps: List<AppInfo>,
        val totalCount: Int,
        val filteredCount: Int
    ) : AppsUiState
    data class Error(val message: String) : AppsUiState
}

enum class SortMode { ALPHABETICAL, INSTALL_DATE, CATEGORY, LAST_USED }
```

**Tests** :
```kotlin
class AppsListViewModelTest {
    private val repository: AppRepository = mockk()
    private lateinit var viewModel: AppsListViewModel

    @Test
    fun `initial state is Loading then Success`() = runTest {
        coEvery { repository.getInstalledApps(any()) } returns testApps
        viewModel = AppsListViewModel(repository)
        // Collect states, verify Loading → Success
    }

    @Test
    fun `search filters results`() = runTest {
        coEvery { repository.searchApps("wha", any()) } returns listOf(whatsApp)
        viewModel.onSearchQueryChanged("wha")
        // Verify filtered results
    }

    @Test
    fun `toggle system apps reloads list`() = runTest {
        viewModel.onToggleSystemApps()
        coVerify { repository.getInstalledApps(includeSystem = true) }
    }

    @Test
    fun `error state on repository failure`() = runTest {
        coEvery { repository.getInstalledApps(any()) } throws Exception("Fail")
        viewModel = AppsListViewModel(repository)
        assertTrue(viewModel.uiState.value is AppsUiState.Error)
    }
}
```

---

### 2.4 — Écran UI : Liste des applications

**Entrée** : Tâches 2.1, 2.2, 2.3
**Sortie** : Composable affichant la liste avec recherche et filtres

**Composables à créer** :

| Composable | Fichier | Rôle |
|-----------|---------|------|
| `AppsListScreen` | `ui/apps/AppsListScreen.kt` | Écran complet avec Scaffold |
| `AppSearchBar` | `ui/apps/AppSearchBar.kt` | Barre de recherche + filtres |
| `AppCard` | `ui/apps/AppCard.kt` | Carte d'une application |
| `AppIcon` | `ui/components/AppIcon.kt` | Composable icône avec fallback |
| `SortModeChip` | `ui/apps/SortModeChip.kt` | Chip sélection du tri |

**Comportements** :
- La recherche filtre en temps réel (debounce 300ms)
- Le scroll est `LazyColumn` avec index rapide (première lettre)
- Long press sur une AppCard → menu contextuel (raccourci, infos, ouvrir)
- Pull-to-refresh pour recharger la liste
- Animation de chargement : shimmer sur les cartes

**Tests UI** :
```kotlin
class AppsListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows loading shimmer initially`()

    @Test
    fun `displays app list after loading`()

    @Test
    fun `search bar filters apps`()

    @Test
    fun `long press shows context menu`()

    @Test
    fun `shows app count at bottom`()

    @Test
    fun `empty search shows no results message`()
}
```

---

## Livrables

| Fichier | Description |
|---------|-------------|
| `domain/model/AppInfo.kt` | Modèle de données |
| `data/repository/AppRepository.kt` | Interface repository |
| `data/repository/AppRepositoryImpl.kt` | Implémentation PackageManager |
| `di/AppModule.kt` | Module Hilt bindings |
| `ui/apps/AppsListViewModel.kt` | ViewModel |
| `ui/apps/AppsListScreen.kt` | Écran principal |
| `ui/apps/AppSearchBar.kt` | Barre de recherche |
| `ui/apps/AppCard.kt` | Carte application |
| `ui/components/AppIcon.kt` | Composable icône |
| Tests unitaires + UI | Tous les fichiers de test listés |

## Vérification

```bash
./gradlew testDebugUnitTest --tests "com.deskzen.data.repository.*"
./gradlew testDebugUnitTest --tests "com.deskzen.ui.apps.*"
./gradlew connectedDebugAndroidTest --tests "com.deskzen.ui.apps.*"
```
