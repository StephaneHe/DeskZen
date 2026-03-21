# DeskZen — Document Central

> **Ce document doit accompagner chaque module individuel.**
> Il fournit le contexte global du projet. La tâche spécifique est décrite dans le document de module associé.

---

## 1. Vision Produit

**DeskZen** est une application Android qui permet de visualiser, organiser et réorganiser intelligemment les applications et dossiers de l'écran d'accueil du téléphone. Une IA embarquée légère peut suggérer une organisation thématique des applications.

### Philosophie

- **Zen** : interface épurée, calme, sans surcharge visuelle
- **Clair** : chaque action est évidente, pas de menus cachés
- **Simple** : une seule chose à la fois, bien faite

---

## 2. Stack Technique

| Élément | Choix | Justification |
|---------|-------|---------------|
| Langage | Kotlin | Standard Android moderne |
| UI | Jetpack Compose + Material 3 | UI déclarative, thèmes dynamiques |
| Architecture | MVVM + Repository | Séparation claire, testable |
| DI | Hilt | Standard Android, intégration ViewModel |
| Base de données | Room | Persistance locale des profils d'organisation |
| IA locale | ONNX Runtime / TFLite | Inférence embarquée sans réseau |
| Tests | JUnit 5 + Compose Testing + Mockk | TDD systématique |
| Min SDK | 28 (Android 9) | Couverture ~95% des appareils actifs |
| Target SDK | 35 (Android 15) | Dernière version stable |

---

## 3. Découpage Modulaire

Le projet est découpé en **7 modules**. Pour travailler sur un module, il faut **ce document central + le document du module**.

| # | Module | Fichier | Dépend de |
|---|--------|---------|-----------|
| 1 | Infrastructure & Projet | `01_Infrastructure_Projet.md` | — |
| 2 | Liste des Applications | `02_Liste_Applications.md` | Module 1 |
| 3 | Raccourcis Écran | `03_Raccourcis_Ecran.md` | Modules 1, 2 |
| 4 | Visualisation Écran | `04_Visualisation_Ecran.md` | Modules 1, 2, 3 |
| 5 | Réorganisation | `05_Reorganisation.md` | Modules 1, 2, 3, 4 |
| 6 | IA Locale — Suggestions | `06_IA_Locale_Suggestions.md` | Modules 1, 2 |
| 7 | UI & Design System | `07_UI_Design_System.md` | Module 1 |

### Ordre de développement recommandé

```
Module 1 (Infrastructure)
    ↓
Module 7 (Design System) ←── en parallèle ──→ Module 2 (Liste Apps)
    ↓                                              ↓
Module 3 (Raccourcis)                          Module 6 (IA Locale)
    ↓
Module 4 (Visualisation)
    ↓
Module 5 (Réorganisation)
```

---

## 4. Structure du Projet

```
C:\Dev\DeskZen\
├── app\
│   ├── src\
│   │   ├── main\
│   │   │   ├── java\com\deskzen\
│   │   │   │   ├── DeskZenApp.kt              # Application Hilt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── di\                         # Modules Hilt
│   │   │   │   ├── data\
│   │   │   │   │   ├── local\                  # Room DB, DAOs
│   │   │   │   │   ├── repository\             # Repositories
│   │   │   │   │   └── model\                  # Data classes
│   │   │   │   ├── domain\
│   │   │   │   │   ├── model\                  # Domain entities
│   │   │   │   │   └── usecase\                # Use cases
│   │   │   │   ├── ui\
│   │   │   │   │   ├── theme\                  # Design tokens, thème
│   │   │   │   │   ├── components\             # Composables réutilisables
│   │   │   │   │   ├── apps\                   # Écran liste des apps
│   │   │   │   │   ├── homescreen\             # Visualisation écran
│   │   │   │   │   ├── organize\               # Réorganisation
│   │   │   │   │   └── suggestions\            # Suggestions IA
│   │   │   │   └── ai\                         # Moteur IA local
│   │   │   ├── res\
│   │   │   └── AndroidManifest.xml
│   │   ├── test\                               # Tests unitaires
│   │   └── androidTest\                        # Tests instrumentation
│   └── build.gradle.kts
├── build.gradle.kts                            # Root build
├── settings.gradle.kts
├── gradle.properties
├── specs\                                      # Cahier des charges
└── assets\
    └── ml\                                     # Modèle IA embarqué
```

---

## 5. Conventions

### Nommage

| Type | Convention | Exemple |
|------|-----------|---------|
| Package | `com.deskzen.feature` | `com.deskzen.ui.apps` |
| Composable | PascalCase, préfixe par écran | `AppsListScreen`, `AppCard` |
| ViewModel | PascalCase + ViewModel | `AppsListViewModel` |
| UseCase | Verbe + Nom + UseCase | `GetInstalledAppsUseCase` |
| Repository | Nom + Repository | `AppRepository` |
| DAO | Nom + Dao | `OrganizationDao` |
| Test | Même nom + Test | `AppsListViewModelTest` |

### Patterns obligatoires

1. **TDD** : Écrire le test AVANT l'implémentation
2. **State hoisting** : Les Composables ne gèrent pas d'état directement
3. **Single source of truth** : Le ViewModel expose un `StateFlow<UiState>`
4. **Sealed classes** pour les états UI :
   ```kotlin
   sealed interface AppsUiState {
       data object Loading : AppsUiState
       data class Success(val apps: List<AppInfo>) : AppsUiState
       data class Error(val message: String) : AppsUiState
   }
   ```
5. **Repository pattern** : Les ViewModels n'accèdent jamais directement aux sources de données

### Navigation

- Jetpack Compose Navigation avec routes typées
- Bottom navigation bar avec 3 onglets : **Apps** | **Écran** | **Suggestions**

---

## 6. Permissions Android

| Permission | Obligatoire | Usage |
|-----------|-------------|-------|
| `QUERY_ALL_PACKAGES` | Oui | Lister toutes les applications installées |
| `INSTALL_SHORTCUT` | Oui | Créer des raccourcis sur l'écran d'accueil |
| `READ_EXTERNAL_STORAGE` | Non | Lecture icônes (fallback) |

> **Note** : `QUERY_ALL_PACKAGES` nécessite une justification lors de la publication sur le Play Store. Prévoir un filtrage par `<queries>` dans le manifest si possible.

---

## 7. Identité Visuelle

### Nom : DeskZen

### Palette de couleurs

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `primary` | `#2D5A3D` | `#8FD4A4` | Actions principales, accents |
| `secondary` | `#5B7C6B` | `#A3C4B0` | Éléments secondaires |
| `surface` | `#F8FAF9` | `#1A1C1B` | Fonds de cartes |
| `background` | `#FFFFFF` | `#111312` | Fond général |
| `accent` | `#E8A849` | `#FFD180` | Highlights, badges IA |
| `error` | `#BA1A1A` | `#FFB4AB` | Erreurs |

> Palette inspirée nature/zen : verts doux + accent chaud doré pour les suggestions IA

### Typographie

- **Titres** : `Google Sans` (ou fallback `Product Sans` → `Sans-serif`)
- **Corps** : `Roboto` (Material 3 default)
- **Monospace** : `JetBrains Mono` (si code affiché)

### Icône

- Fond : Cercle vert doux (`#2D5A3D`)
- Symbole : Grille 2×2 stylisée avec un élément en mouvement (suggestion de réorganisation)
- Style : Material You, arrondi, simple

---

## 8. Gestion d'erreurs

Toute erreur utilisateur doit :
1. Être affichée dans un `Snackbar` Material 3
2. Proposer une action corrective quand possible
3. Logger en `Timber` pour le debug

Les erreurs système (crash) sont capturées par un `UncaughtExceptionHandler` minimal.

---

## 9. Glossaire

| Terme | Définition |
|-------|-----------|
| **Launcher** | Application qui gère l'écran d'accueil Android |
| **Raccourci (Shortcut)** | Lien vers une app placé sur l'écran d'accueil |
| **Dossier** | Groupe de raccourcis sur l'écran d'accueil |
| **Profil** | Configuration sauvegardée de l'organisation de l'écran |
| **Suggestion IA** | Proposition de regroupement thématique générée localement |
| **Thème** | Catégorie fonctionnelle d'applications (ex: "Productivité", "Réseaux sociaux") |
