# Module 5 — Réorganisation

> **Prérequis** : Lire `00_Document_Central.md` pour le contexte global.
> **Dépend de** : Modules 1, 2, 3, 4

---

## Objectif

Permettre à l'utilisateur de réorganiser entièrement ses applications et dossiers sur l'écran d'accueil via drag & drop, avec création/suppression de dossiers et gestion de profils sauvegardés.

---

## Tâches

### 5.1 — Drag & Drop dans la grille

**Entrée** : Module 4 (grille affichée)
**Sortie** : Déplacement d'éléments par drag & drop

**Comportement** :
- Long press sur un item → entre en mode édition, item "flotte"
- Drag vers un emplacement vide → déplace l'item
- Drag vers un autre item → propose de créer un dossier (merge)
- Drag vers le bord gauche/droit → change de page
- Drop sur un dossier → ajoute l'app au dossier
- Feedback haptique au pickup et au drop
- Animation spring pour le retour en place si drop annulé
- Zone de suppression en bas ("Retirer de l'écran") visible pendant le drag

**Implémentation technique** :
```kotlin
// ui/organize/DraggableGrid.kt
@Composable
fun DraggableGrid(
    pages: List<ScreenPage>,
    columns: Int,
    rows: Int,
    onMoveItem: (fromPage: Int, fromPos: Int, toPage: Int, toPos: Int) -> Unit,
    onMergeItems: (page: Int, pos1: Int, pos2: Int) -> Unit,
    onDropInFolder: (folderId: Long, packageName: String) -> Unit,
    onRemoveFromScreen: (page: Int, position: Int) -> Unit
) {
    // Utilise Modifier.pointerInput pour le gesture handling
    // State: draggedItem, dragOffset, currentDropTarget
}
```

**Gestion du state pendant le drag** :
```kotlin
data class DragState(
    val isDragging: Boolean = false,
    val draggedItem: ScreenItem? = null,
    val dragOffset: Offset = Offset.Zero,
    val sourcePage: Int = 0,
    val sourcePosition: Int = 0,
    val currentDropTarget: DropTarget? = null
)

sealed interface DropTarget {
    data class EmptySlot(val page: Int, val position: Int) : DropTarget
    data class ExistingItem(val page: Int, val position: Int) : DropTarget
    data class Folder(val folderId: Long) : DropTarget
    data object RemoveZone : DropTarget
    data object PageEdge : DropTarget
}
```

**Tests** :
```kotlin
class DraggableGridTest {
    @Test
    fun `long press activates drag mode`()

    @Test
    fun `drag to empty slot moves item`()

    @Test
    fun `drag to occupied slot proposes folder creation`()

    @Test
    fun `drag to edge scrolls to next page`()

    @Test
    fun `drop on remove zone removes item`()

    @Test
    fun `cancelled drag returns item to original position`()

    @Test
    fun `haptic feedback fires on pickup and drop`()
}
```

---

### 5.2 — Gestion des dossiers

**Entrée** : Tâche 5.1
**Sortie** : Création, renommage, suppression de dossiers

**Actions** :

| Action | Déclencheur | Résultat |
|--------|------------|----------|
| Créer dossier | Drag une app sur une autre app | Dialog de nommage → dossier créé |
| Renommer | Tap sur le nom du dossier ouvert | Champ éditable inline |
| Supprimer dossier | Long press dossier → "Supprimer dossier" | Apps du dossier reviennent dans la grille |
| Retirer app du dossier | Long press app dans dossier → "Sortir du dossier" | App revient dans la grille |
| Changer couleur | Menu dossier → palette | 8 couleurs prédéfinies |

**Tests** :
```kotlin
class FolderManagementTest {
    @Test
    fun `merging two apps shows naming dialog`()

    @Test
    fun `folder created with correct apps after naming`()

    @Test
    fun `rename folder updates display`()

    @Test
    fun `delete folder disperses apps to free slots`()

    @Test
    fun `remove single app from folder works`()

    @Test
    fun `folder with 1 app auto-dissolves`()
}
```

---

### 5.3 — Ajout d'apps depuis la liste

**Entrée** : Tâches 5.1, Module 2
**Sortie** : Pouvoir ajouter des apps non présentes sur l'écran depuis la liste complète

**Comportement** :
- En mode édition, bouton "+" en bas → ouvre un drawer avec la liste des apps non encore sur l'écran
- Recherche rapide dans le drawer
- Tap ou drag depuis le drawer vers la grille
- Les apps déjà sur l'écran sont grisées dans le drawer

---

### 5.4 — Profils d'organisation

**Entrée** : Tâches 5.1, 5.2
**Sortie** : Sauvegarde et restauration de profils complets

```kotlin
// domain/model/OrganizationProfile.kt
data class OrganizationProfile(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pages: List<ScreenPage>,
    val isActive: Boolean = false,
    val source: ProfileSource = ProfileSource.USER
)

enum class ProfileSource {
    USER,       // Créé manuellement
    AI,         // Généré par l'IA (Module 6)
    IMPORTED    // Importé depuis le launcher
}
```

```kotlin
// data/local/entity/ProfileEntity.kt
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val source: String
)
```

**Fonctionnalités** :
- "Sauvegarder comme profil" → nomme et sauvegarde l'état actuel
- "Charger un profil" → liste des profils sauvegardés avec prévisualisation miniature
- "Appliquer" → remplace l'organisation actuelle
- Maximum 10 profils sauvegardés
- Le profil actif est marqué visuellement

**Tests** :
```kotlin
class OrganizationProfileTest {
    @Test
    fun `save profile persists current layout`()

    @Test
    fun `load profile restores layout`()

    @Test
    fun `max 10 profiles enforced`()

    @Test
    fun `active profile is marked`()

    @Test
    fun `AI source profile is tagged correctly`()
}
```

---

### 5.5 — Application effective au launcher

**Entrée** : Tâches 5.1–5.4
**Sortie** : Appliquer l'organisation de DeskZen à l'écran réel du téléphone

**Stratégie** :
L'application effective est la partie la plus délicate. Android ne permet pas de modifier directement l'écran d'accueil d'un launcher tiers. Deux approches :

1. **Approche raccourcis** (par défaut) :
   - Supprime tous les raccourcis DeskZen existants
   - Recrée les raccourcis dans l'ordre défini
   - Limité : pas de contrôle sur la position exacte ni les dossiers

2. **Approche Accessibility Service** (optionnel, avancé) :
   - DeskZen devient un service d'accessibilité
   - Peut interagir avec le launcher pour placer les items
   - Nécessite permission spéciale de l'utilisateur

**Choix** : Approche 1 par défaut. L'approche 2 peut être envisagée en V2.

---

## Livrables

| Fichier | Description |
|---------|-------------|
| `ui/organize/DraggableGrid.kt` | Grille drag & drop |
| `ui/organize/DragState.kt` | État du drag |
| `ui/organize/FolderDialog.kt` | Dialog création dossier |
| `ui/organize/AddAppDrawer.kt` | Drawer ajout d'apps |
| `ui/organize/ProfileManager.kt` | Gestion profils |
| `ui/organize/ApplyToScreenDialog.kt` | Dialog d'application |
| `domain/model/OrganizationProfile.kt` | Modèle profil |
| `domain/usecase/ApplyLayoutUseCase.kt` | Application au launcher |
| `data/local/entity/ProfileEntity.kt` | Entité Room |
| `data/local/dao/ProfileDao.kt` | DAO profils |
| Tests unitaires + UI | Tous les fichiers de test |

## Vérification

```bash
./gradlew testDebugUnitTest --tests "com.deskzen.ui.organize.*"
./gradlew testDebugUnitTest --tests "com.deskzen.domain.usecase.ApplyLayout*"
./gradlew connectedDebugAndroidTest --tests "com.deskzen.ui.organize.*"
```
