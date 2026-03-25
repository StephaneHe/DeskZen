# DeskZen

Un launcher Android intelligent qui organise automatiquement tes applications grâce à un moteur de catégorisation heuristique, avec une interface personnalisable inspirée de l'univers *Solo Leveling*.

---

## Fonctionnalités

### Écran d'accueil
- **Pages multiples** – les apps sont réparties sur plusieurs pages (20 icônes max par page) navigables par swipe
- **Dossiers intelligents** – création automatique de dossiers thématiques (Social, Finance, Jeux, Musique…) via l'IA heuristique
- **Dossiers personnalisés** – crée, renomme ou supprime tes propres dossiers
- **Drag & drop** – déplace des apps ou crée un dossier en glissant une icône sur une autre
- **Raccourcis web** – ajoute des liens web à l'écran d'accueil avec récupération automatique du favicon
- **Appui long sur zone vide** – ajoute un raccourci vers une URL directement depuis l'écran d'accueil

### Barre de toggles rapides
Accès direct depuis l'écran d'accueil aux commandes système :
- Wi-Fi, Bluetooth, VPN, NFC, Lampe de poche

### Tiroir d'applications
- Liste complète des apps installées avec recherche en temps réel
- Tri et filtrage par catégorie

### Suggestions IA
- Recommandations de rangement par thème avec score de confiance
- Détection multi-niveaux : catégorie Android → pattern de package → mots-clés du nom de l'app

### Sauvegarde / Restauration
- Export/import du layout complet au format JSON
- Préserve : dossiers personnalisés, placements manuels, apps standalone et raccourcis web
- Les favicons des raccourcis web sont re-téléchargés automatiquement après une restauration

---

## Stack technique

| Couche | Technologies |
|--------|-------------|
| Langage | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2025.01.00) · Material Design 3 |
| Architecture | MVVM · Repository pattern · StateFlow |
| Injection de dépendances | Hilt 2.53 |
| Base de données | Room 2.6.1 |
| Sérialisation | Kotlin Serialization 1.7.3 |
| Navigation | Navigation Compose 2.8.5 (type-safe) |
| Build | Gradle 8.7.3 · KSP · Java 17 |
| Logging | Timber 5.0.1 |
| Tests | JUnit 4 · MockK 1.13.13 · Coroutines Test |

---

## Architecture

```
com.deskzen/
├── ui/
│   ├── launcher/          # Écran principal, ViewModel, BackupManager, QuickToggles
│   ├── apps/              # Tiroir d'applications
│   ├── suggestions/       # Écran de suggestions IA
│   ├── components/        # Composants réutilisables (AppIcon, PageIndicator…)
│   └── theme/             # Thème Solo Leveling (dark, bleus électriques)
├── domain/
│   ├── model/             # AppInfo, ScreenItem (sealed), ThemeSuggestion…
│   └── usecase/           # ManageShortcutUseCase
├── data/
│   ├── repository/        # AppRepository, ScreenRepository (PackageManager + Room)
│   └── local/             # Room DB – entités ScreenPage, ScreenItem, Profile, FolderApp
├── ai/
│   ├── HeuristicCategorizer.kt   # Moteur de catégorisation multi-niveaux
│   └── AppCategorizerFacade.kt
├── di/                    # Modules Hilt (App, Database, UseCase)
├── navigation/            # DeskZenNavHost – 3 onglets
└── MainActivity.kt
```

### Modèle de données principal

```kotlin
sealed interface ScreenItem {
    data class AppShortcut(val position: Int, val appInfo: AppInfo) : ScreenItem
    data class Folder(val position: Int, val name: String, val apps: List<AppInfo>, ...) : ScreenItem
    data class WebShortcut(val position: Int, val url: String, val label: String, val favicon: Bitmap?) : ScreenItem
}
```

---

## Installation

### Prérequis
- Android Studio Hedgehog ou supérieur
- JDK 17 (ex : Microsoft Build of OpenJDK 17)
- Android SDK 35 · minSdk 26

### Build & install

```bash
# Debug
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Release
./gradlew assembleRelease
```

### Définir comme launcher par défaut
1. Installer l'APK
2. Appuyer sur le bouton Home
3. Sélectionner **DeskZen** → *Toujours*

---

## Thème

DeskZen utilise un thème sombre personnalisé inspiré de l'anime *Solo Leveling* :
- Fond : noir profond `#0A0A0F`
- Accent principal : bleu électrique `#4FC3F7`
- Accent secondaire : violet `#9C27B0` / cyan `#00BCD4`
- Typographie : Material Design 3 avec hiérarchie adaptée aux petits écrans

---

## Licence

Projet privé — tous droits réservés.
