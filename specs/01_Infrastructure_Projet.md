# Module 1 — Infrastructure & Projet

> **Prérequis** : Lire `00_Document_Central.md` pour le contexte global.

---

## Objectif

Mettre en place le squelette du projet Android avec toute la configuration technique nécessaire pour que les modules suivants puissent être développés indépendamment.

---

## Tâches

### 1.1 — Création du projet Gradle

**Entrée** : Rien
**Sortie** : Projet compilable avec écran vide

**Détails** :
- Créer le projet avec namespace `com.deskzen`
- `minSdk = 28`, `targetSdk = 35`, `compileSdk = 35`
- Kotlin 2.0+, Compose BOM dernière version stable
- Configurer `build.gradle.kts` (root + app) avec les dépendances :

```kotlin
// Compose
implementation(platform("androidx.compose:compose-bom:2025.01.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
debugImplementation("androidx.compose.ui:ui-tooling")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.5")

// Hilt
implementation("com.google.dagger:hilt-android:2.53")
kapt("com.google.dagger:hilt-compiler:2.53")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// Logging
implementation("com.jakewharton.timber:timber:5.0.1")

// Tests
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.13")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

**Critère de validation** :
```bash
./gradlew assembleDebug  # Compile sans erreur
```

---

### 1.2 — Application Hilt

**Entrée** : Tâche 1.1
**Sortie** : Classe Application annotée, injection fonctionnelle

**Fichiers à créer** :

```kotlin
// DeskZenApp.kt
@HiltAndroidApp
class DeskZenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeskZenTheme {
                DeskZenNavHost()
            }
        }
    }
}
```

**AndroidManifest.xml** :
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />

    <application
        android:name=".DeskZenApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="DeskZen"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.DeskZen">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.DeskZen">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Tests** :
```kotlin
// DeskZenAppTest.kt
class DeskZenAppTest {
    @Test
    fun `app class is annotated with HiltAndroidApp`() {
        val annotation = DeskZenApp::class.java.getAnnotation(HiltAndroidApp::class.java)
        assertNotNull(annotation)
    }
}
```

---

### 1.3 — Navigation Shell

**Entrée** : Tâche 1.2
**Sortie** : Navigation 3 onglets fonctionnelle avec écrans placeholder

**Fichiers à créer** :

```kotlin
// navigation/DeskZenNavHost.kt
// Navigation avec 3 routes : Apps, Screen, Suggestions
// Bottom bar avec icônes Material :
//   - Apps : Icons.Outlined.Apps
//   - Écran : Icons.Outlined.Smartphone
//   - Suggestions : Icons.Outlined.AutoAwesome
```

Chaque onglet affiche un `Scaffold` avec un titre et un texte "En construction".

**Tests** :
```kotlin
// DeskZenNavHostTest.kt
class DeskZenNavHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `bottom bar shows three tabs`() {
        composeTestRule.setContent { DeskZenNavHost() }
        composeTestRule.onNodeWithText("Apps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Écran").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggestions").assertIsDisplayed()
    }

    @Test
    fun `clicking tab navigates to correct screen`() {
        composeTestRule.setContent { DeskZenNavHost() }
        composeTestRule.onNodeWithText("Suggestions").performClick()
        composeTestRule.onNodeWithText("Suggestions IA").assertIsDisplayed()
    }
}
```

---

### 1.4 — Room Database Shell

**Entrée** : Tâche 1.1
**Sortie** : Base de données Room vide, prête à recevoir les entités des modules suivants

```kotlin
// data/local/DeskZenDatabase.kt
@Database(
    entities = [], // Sera peuplé par les modules suivants
    version = 1,
    exportSchema = true
)
abstract class DeskZenDatabase : RoomDatabase()
```

```kotlin
// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DeskZenDatabase {
        return Room.databaseBuilder(
            context,
            DeskZenDatabase::class.java,
            "deskzen.db"
        ).build()
    }
}
```

---

## Livrables

| Fichier | Description |
|---------|-------------|
| `build.gradle.kts` (root + app) | Configuration Gradle complète |
| `DeskZenApp.kt` | Application Hilt |
| `MainActivity.kt` | Activité principale |
| `AndroidManifest.xml` | Manifest avec permissions |
| `DeskZenNavHost.kt` | Shell de navigation 3 onglets |
| `DeskZenDatabase.kt` | Room database shell |
| `DatabaseModule.kt` | Module Hilt pour Room |
| `DeskZenAppTest.kt` | Test unitaire Application |
| `DeskZenNavHostTest.kt` | Tests navigation |

## Vérification

```bash
./gradlew assembleDebug          # Compile
./gradlew testDebugUnitTest      # Tests unitaires passent
./gradlew connectedDebugAndroidTest  # Tests UI passent
```
