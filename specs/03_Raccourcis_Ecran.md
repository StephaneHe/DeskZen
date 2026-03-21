# Module 3 — Raccourcis Écran

> **Prérequis** : Lire `00_Document_Central.md` pour le contexte global.
> **Dépend de** : Modules 1 (Infrastructure), 2 (Liste Applications)

---

## Objectif

Permettre à l'utilisateur de créer et supprimer des raccourcis d'applications sur l'écran d'accueil du téléphone, directement depuis la liste des apps.

---

## Contexte technique

Android gère les raccourcis via deux APIs principales :
- **`ShortcutManager`** (API 25+) : raccourcis dynamiques et statiques
- **`ShortcutManagerCompat`** (AndroidX) : wrapper rétrocompatible pour `requestPinShortcut`

DeskZen utilise `ShortcutManagerCompat` pour la compatibilité maximale.

---

## Tâches

### 3.1 — Service de raccourcis

**Entrée** : Module 2 (AppInfo disponible)
**Sortie** : Service capable de créer/supprimer des raccourcis pinnés

```kotlin
// domain/usecase/ManageShortcutUseCase.kt
interface ManageShortcutUseCase {
    suspend fun createShortcut(appInfo: AppInfo): ShortcutResult
    suspend fun removeShortcut(packageName: String): ShortcutResult
    suspend fun isShortcutPinned(packageName: String): Boolean
    fun canPinShortcuts(): Boolean
}

sealed interface ShortcutResult {
    data object Success : ShortcutResult
    data class Error(val reason: String) : ShortcutResult
    data object NotSupported : ShortcutResult
    data object PermissionRequired : ShortcutResult
}
```

```kotlin
// domain/usecase/ManageShortcutUseCaseImpl.kt
class ManageShortcutUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ManageShortcutUseCase {

    override suspend fun createShortcut(appInfo: AppInfo): ShortcutResult {
        if (!canPinShortcuts()) return ShortcutResult.NotSupported

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "deskzen_${appInfo.packageName}")
            .setShortLabel(appInfo.label)
            .setIcon(IconCompat.createWithAdaptiveBitmap(appInfo.icon.toBitmap()))
            .setIntent(
                context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    ?: return ShortcutResult.Error("Impossible de lancer ${appInfo.label}")
            )
            .build()

        val success = ShortcutManagerCompat.requestPinShortcut(
            context, shortcutInfo, null
        )

        return if (success) ShortcutResult.Success
               else ShortcutResult.Error("Le launcher ne supporte pas les raccourcis pinnés")
    }

    override fun canPinShortcuts(): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }
}
```

**Tests** :
```kotlin
class ManageShortcutUseCaseTest {
    private val context: Context = mockk(relaxed = true)
    private lateinit var useCase: ManageShortcutUseCaseImpl

    @Test
    fun `createShortcut returns Success when supported`()

    @Test
    fun `createShortcut returns NotSupported when launcher doesnt support pins`()

    @Test
    fun `createShortcut returns Error when no launch intent`()

    @Test
    fun `removeShortcut removes by package name`()

    @Test
    fun `canPinShortcuts delegates to ShortcutManagerCompat`()
}
```

---

### 3.2 — Intégration dans AppCard

**Entrée** : Tâches 2.4, 3.1
**Sortie** : Actions de raccourci accessibles depuis la carte app

**Comportement** :
- **Long press** sur une `AppCard` → Bottom sheet avec actions :
  - "Ajouter à l'écran d'accueil" (si pas déjà présent)
  - "Retirer de l'écran d'accueil" (si présent)
  - "Ouvrir l'application"
  - "Informations de l'app" (ouvre les paramètres Android)
- **Feedback visuel** : badge vert ✓ sur l'icône si raccourci existant
- **Snackbar** de confirmation après création/suppression

```kotlin
// ui/apps/AppActionsSheet.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    appInfo: AppInfo,
    onCreateShortcut: () -> Unit,
    onRemoveShortcut: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismiss: () -> Unit
)
```

**Tests UI** :
```kotlin
class AppActionsSheetTest {
    @Test
    fun `shows create shortcut when app not on home screen`()

    @Test
    fun `shows remove shortcut when app is on home screen`()

    @Test
    fun `create shortcut triggers callback`()

    @Test
    fun `open app triggers callback`()
}
```

---

### 3.3 — Batch shortcuts (sélection multiple)

**Entrée** : Tâches 3.1, 3.2
**Sortie** : Sélection multiple pour ajouter/retirer plusieurs raccourcis d'un coup

**Comportement** :
- Mode sélection activé par long press sur une app
- Checkboxes apparaissent sur chaque AppCard
- Barre d'action en haut : "X sélectionnées" + boutons batch
- Actions batch : "Ajouter tous à l'écran" / "Retirer tous"
- Chaque raccourci est créé séquentiellement (Android limite les requests simultanées)
- Progress indicator pendant le batch

**Tests** :
```kotlin
class BatchShortcutTest {
    @Test
    fun `long press activates selection mode`()

    @Test
    fun `selection count updates correctly`()

    @Test
    fun `batch create processes all selected apps`()

    @Test
    fun `exit selection mode clears selection`()

    @Test
    fun `progress shown during batch operation`()
}
```

---

## Livrables

| Fichier | Description |
|---------|-------------|
| `domain/usecase/ManageShortcutUseCase.kt` | Interface |
| `domain/usecase/ManageShortcutUseCaseImpl.kt` | Implémentation |
| `ui/apps/AppActionsSheet.kt` | Bottom sheet d'actions |
| `ui/apps/BatchShortcutBar.kt` | Barre d'actions batch |
| Tests unitaires + UI | Tous les fichiers de test listés |

## Vérification

```bash
./gradlew testDebugUnitTest --tests "com.deskzen.domain.usecase.ManageShortcut*"
./gradlew connectedDebugAndroidTest --tests "com.deskzen.ui.apps.AppActionsSheet*"
```

> **Note** : Les tests de création effective de raccourci nécessitent un appareil/émulateur avec un launcher supportant les pinned shortcuts (ex: Pixel Launcher).
