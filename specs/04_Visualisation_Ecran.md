# Module 4 — Visualisation Écran

> **Prérequis** : Lire `00_Document_Central.md` pour le contexte global.
> **Dépend de** : Modules 1 (Infrastructure), 2 (Liste Applications), 3 (Raccourcis)

---

## Objectif

Afficher une représentation visuelle de l'organisation actuelle de l'écran d'accueil du téléphone : pages, dossiers, et applications dans chaque dossier. C'est l'écran principal de l'onglet "Écran".

---

## Contexte technique

### Limites d'Android

Android ne fournit **pas d'API publique** pour lire l'organisation de l'écran d'accueil. Les launchers stockent leur configuration de manière propriétaire. Deux approches possibles :

| Approche | Avantage | Inconvénient |
|----------|----------|-------------|
| **Lecture ContentProvider du launcher** | Données réelles | Spécifique à chaque launcher, non garanti |
| **Gestion interne DeskZen** | Contrôle total, portable | Ne reflète pas l'état réel du launcher |

**Choix** : Approche hybride. DeskZen maintient sa propre base de données d'organisation, et tente de lire les données du launcher par défaut (Pixel Launcher, Samsung OneUI, AOSP) au premier lancement pour synchroniser. L'utilisateur peut aussi importer manuellement.

---

## Tâches

### 4.1 — Modèles de données écran

**Entrée** : Module 2 (AppInfo)
**Sortie** : Entités Room pour représenter la structure de l'écran

```kotlin
// domain/model/HomeScreenModels.kt

data class ScreenPage(
    val pageIndex: Int,
    val items: List<ScreenItem>
)

sealed interface ScreenItem {
    val position: Int  // Position dans la grille (0-based)

    data class AppShortcut(
        override val position: Int,
        val appInfo: AppInfo
    ) : ScreenItem

    data class Folder(
        override val position: Int,
        val name: String,
        val apps: List<AppInfo>,
        val color: Long? = null  // Couleur optionnelle du dossier
    ) : ScreenItem
}
```

```kotlin
// data/local/entity/ScreenEntities.kt

@Entity(tableName = "screen_pages")
data class ScreenPageEntity(
    @PrimaryKey val pageIndex: Int
)

@Entity(
    tableName = "screen_items",
    foreignKeys = [ForeignKey(
        entity = ScreenPageEntity::class,
        parentColumns = ["pageIndex"],
        childColumns = ["pageIndex"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ScreenItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageIndex: Int,
    val position: Int,
    val type: String,           // "app" ou "folder"
    val packageName: String?,   // null si folder
    val folderName: String?,    // null si app
    val folderColor: Long?
)

@Entity(
    tableName = "folder_apps",
    foreignKeys = [ForeignKey(
        entity = ScreenItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["folderId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FolderAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val packageName: String,
    val positionInFolder: Int
)
```

**Tests** :
```kotlin
class ScreenEntitiesTest {
    @Test
    fun `ScreenPage contains ordered items`()

    @Test
    fun `Folder can contain multiple apps`()

    @Test
    fun `AppShortcut maps to single app`()
}
```

---

### 4.2 — DAO et Repository écran

**Entrée** : Tâche 4.1
**Sortie** : Accès données pour la structure de l'écran

```kotlin
// data/local/dao/ScreenDao.kt
@Dao
interface ScreenDao {
    @Query("SELECT * FROM screen_pages ORDER BY pageIndex")
    fun getAllPages(): Flow<List<ScreenPageEntity>>

    @Query("SELECT * FROM screen_items WHERE pageIndex = :pageIndex ORDER BY position")
    fun getItemsForPage(pageIndex: Int): Flow<List<ScreenItemEntity>>

    @Query("SELECT * FROM folder_apps WHERE folderId = :folderId ORDER BY positionInFolder")
    fun getAppsInFolder(folderId: Long): Flow<List<FolderAppEntity>>

    @Transaction
    @Query("SELECT * FROM screen_pages ORDER BY pageIndex")
    fun getFullScreen(): Flow<List<ScreenPageWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: ScreenPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScreenItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderApp(folderApp: FolderAppEntity)

    @Query("DELETE FROM screen_pages")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceFullScreen(pages: List<ScreenPageEntity>, items: List<ScreenItemEntity>, folderApps: List<FolderAppEntity>) {
        clearAll()
        pages.forEach { insertPage(it) }
        items.forEach { insertItem(it) }
        folderApps.forEach { insertFolderApp(it) }
    }
}
```

```kotlin
// data/repository/ScreenRepository.kt
interface ScreenRepository {
    fun getScreenLayout(): Flow<List<ScreenPage>>
    suspend fun saveScreenLayout(pages: List<ScreenPage>)
    suspend fun addAppToFolder(folderId: Long, packageName: String)
    suspend fun removeAppFromFolder(folderId: Long, packageName: String)
    suspend fun createFolder(pageIndex: Int, position: Int, name: String, apps: List<String>)
    suspend fun deleteFolder(folderId: Long)
    suspend fun renameFolder(folderId: Long, newName: String)
    suspend fun moveItem(fromPage: Int, fromPos: Int, toPage: Int, toPos: Int)
}
```

**Tests** :
```kotlin
class ScreenRepositoryTest {
    @Test
    fun `getScreenLayout returns pages with items`()

    @Test
    fun `saveScreenLayout persists and can be read back`()

    @Test
    fun `createFolder adds folder with apps`()

    @Test
    fun `moveItem swaps positions correctly`()

    @Test
    fun `deleteFolder cascades to folder apps`()
}
```

---

### 4.3 — Écran UI : Visualisation grille

**Entrée** : Tâches 4.1, 4.2
**Sortie** : Écran affichant la grille de l'écran d'accueil

**Composables** :

| Composable | Fichier | Rôle |
|-----------|---------|------|
| `HomeScreenView` | `ui/homescreen/HomeScreenView.kt` | Écran complet avec pager |
| `ScreenPageGrid` | `ui/homescreen/ScreenPageGrid.kt` | Grille d'une page |
| `ScreenAppItem` | `ui/homescreen/ScreenAppItem.kt` | App dans la grille |
| `ScreenFolderItem` | `ui/homescreen/ScreenFolderItem.kt` | Dossier dans la grille (mini-grille 2×2) |
| `FolderDetailSheet` | `ui/homescreen/FolderDetailSheet.kt` | Contenu complet d'un dossier ouvert |
| `PageIndicator` | `ui/components/PageIndicator.kt` | Dots indicateurs de page |

**Comportements** :
- Swipe horizontal entre les pages (HorizontalPager)
- Tap sur un dossier → ouvre un bottom sheet avec le contenu du dossier
- Tap sur une app → ouvre l'application
- Grille 4 colonnes × 5 lignes par défaut (configurable dans les settings)
- Les emplacements vides sont visibles (slots grisés)
- Animation d'ouverture de dossier : expand depuis la position du dossier

**Tests UI** :
```kotlin
class HomeScreenViewTest {
    @Test
    fun `displays grid with correct number of columns`()

    @Test
    fun `swipe changes page`()

    @Test
    fun `page indicator reflects current page`()

    @Test
    fun `tap folder opens detail sheet`()

    @Test
    fun `folder shows first 4 app icons in mini grid`()

    @Test
    fun `empty slots are shown as placeholders`()
}
```

---

### 4.4 — Détection launcher (optionnel, best-effort)

**Entrée** : Module 1
**Sortie** : Tentative de lecture de la configuration du launcher par défaut

```kotlin
// data/repository/LauncherDetector.kt
interface LauncherDetector {
    fun getDefaultLauncherPackage(): String?
    suspend fun tryReadLauncherConfig(): List<ScreenPage>?
}
```

**Launchers supportés (best-effort)** :
- Pixel Launcher : `content://com.google.android.apps.nexuslauncher.settings/favorites`
- AOSP Launcher3 : `content://com.android.launcher3.settings/favorites`
- Samsung OneUI : Non accessible (propriétaire)

> **Important** : Si la lecture échoue, DeskZen propose à l'utilisateur de construire son organisation manuellement via l'écran de réorganisation (Module 5). Ce n'est pas bloquant.

---

## Livrables

| Fichier | Description |
|---------|-------------|
| `domain/model/HomeScreenModels.kt` | Modèles domaine |
| `data/local/entity/ScreenEntities.kt` | Entités Room |
| `data/local/dao/ScreenDao.kt` | DAO |
| `data/repository/ScreenRepository.kt` | Interface + Impl |
| `data/repository/LauncherDetector.kt` | Détection launcher |
| `ui/homescreen/HomeScreenView.kt` | Écran principal |
| `ui/homescreen/ScreenPageGrid.kt` | Grille |
| `ui/homescreen/ScreenAppItem.kt` | Item app |
| `ui/homescreen/ScreenFolderItem.kt` | Item dossier |
| `ui/homescreen/FolderDetailSheet.kt` | Détail dossier |
| `ui/components/PageIndicator.kt` | Indicateur |
| Migration Room dans `DeskZenDatabase.kt` | Ajout des entités |
| Tests unitaires + UI | Tous les fichiers de test |

## Vérification

```bash
./gradlew testDebugUnitTest --tests "com.deskzen.data.local.dao.ScreenDao*"
./gradlew testDebugUnitTest --tests "com.deskzen.data.repository.Screen*"
./gradlew connectedDebugAndroidTest --tests "com.deskzen.ui.homescreen.*"
```
