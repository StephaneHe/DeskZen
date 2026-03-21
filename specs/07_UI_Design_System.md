# Module 7 — UI & Design System

> **Prérequis** : Lire `00_Document_Central.md` pour le contexte global.
> **Dépend de** : Module 1 (Infrastructure)

---

## Objectif

Définir et implémenter le design system complet de DeskZen : thème Material 3, composants réutilisables, animations et tokens de design. Garantir une interface épurée, claire et agréable.

---

## Philosophie visuelle

### Principes

1. **Calme** : Pas de couleurs criardes, pas de notifications intrusives. Tons doux et naturels.
2. **Espace** : Padding généreux, cartes aérées, pas de surcharge.
3. **Hiérarchie** : Un seul point focal par écran. Le reste est secondaire.
4. **Cohérence** : Les mêmes patterns partout. L'utilisateur apprend une fois.

### Anti-patterns à éviter

- Gradients décoratifs inutiles
- Ombres excessives
- Plus de 2 niveaux de nesting visuel
- Texte de moins de 14sp
- Icônes incohérentes (mélange filled/outlined)
- Animations de plus de 400ms

---

## Tâches

### 7.1 — Thème Material 3

**Entrée** : Palette définie dans le Document Central
**Sortie** : Thème Compose complet avec support light/dark et Material You

```kotlin
// ui/theme/Color.kt
// Palette DeskZen — Tons zen/nature
val DeskZenGreen = Color(0xFF2D5A3D)
val DeskZenGreenLight = Color(0xFF8FD4A4)
val DeskZenSage = Color(0xFF5B7C6B)
val DeskZenSageLight = Color(0xFFA3C4B0)
val DeskZenGold = Color(0xFFE8A849)
val DeskZenGoldLight = Color(0xFFFFD180)
val DeskZenSurface = Color(0xFFF8FAF9)
val DeskZenSurfaceDark = Color(0xFF1A1C1B)
val DeskZenBackground = Color(0xFFFFFFFF)
val DeskZenBackgroundDark = Color(0xFF111312)
val DeskZenError = Color(0xFFBA1A1A)
val DeskZenErrorDark = Color(0xFFFFB4AB)
```

```kotlin
// ui/theme/Theme.kt
@Composable
fun DeskZenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Material You sur Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DeskZenDarkColorScheme
        else -> DeskZenLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeskZenTypography,
        shapes = DeskZenShapes,
        content = content
    )
}
```

```kotlin
// ui/theme/Type.kt
val DeskZenTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

```kotlin
// ui/theme/Shape.kt
val DeskZenShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
```

**Tests** :
```kotlin
class ThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `light theme applies correct primary color`() {
        composeTestRule.setContent {
            DeskZenTheme(darkTheme = false, dynamicColor = false) {
                val color = MaterialTheme.colorScheme.primary
                assertEquals(DeskZenGreen, color)
            }
        }
    }

    @Test
    fun `dark theme applies correct surface color`() {
        composeTestRule.setContent {
            DeskZenTheme(darkTheme = true, dynamicColor = false) {
                val color = MaterialTheme.colorScheme.surface
                assertEquals(DeskZenSurfaceDark, color)
            }
        }
    }
}
```

---

### 7.2 — Design Tokens & Dimensions

```kotlin
// ui/theme/Dimens.kt
object DeskZenDimens {
    // Spacing
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 16.dp
    val spacingLg = 24.dp
    val spacingXl = 32.dp

    // App icon sizes
    val appIconSmall = 36.dp    // Dans les listes compactes
    val appIconMedium = 48.dp   // Dans la grille écran
    val appIconLarge = 56.dp    // Dans la liste principale

    // Card
    val cardElevation = 1.dp
    val cardPadding = 12.dp

    // Grid
    val gridColumns = 4
    val gridRows = 5
    val gridItemSpacing = 8.dp

    // Bottom bar
    val bottomBarHeight = 80.dp

    // Folder
    val folderMiniIconSize = 18.dp
    val folderCornerRadius = 12.dp
}
```

---

### 7.3 — Composants réutilisables

**Composants à créer** :

#### DeskZenTopBar
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeskZenTopBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
)
```

#### AppIcon (avec fallback et badge)
```kotlin
@Composable
fun AppIcon(
    icon: Drawable?,
    label: String,
    size: Dp = DeskZenDimens.appIconLarge,
    badge: AppBadge? = null,  // Petit indicateur (ex: check pour raccourci)
    modifier: Modifier = Modifier
)

enum class AppBadge { SHORTCUT, AI_SUGGESTED, NONE }
```

#### EmptyState
```kotlin
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: @Composable (() -> Unit)? = null
)
```
Usage : "Aucune app trouvée", "Aucune suggestion", "Écran vide"

#### LoadingShimmer
```kotlin
@Composable
fun LoadingShimmer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
)
```
Effet shimmer animé pour les états de chargement.

#### ConfidenceBadge
```kotlin
@Composable
fun ConfidenceBadge(
    confidence: Float,  // 0.0 - 1.0
    modifier: Modifier = Modifier
)
```
Petit badge coloré montrant le niveau de confiance IA :
- >= 0.8 : Vert
- >= 0.5 : Doré
- < 0.5 : Gris

#### ThemeTag
```kotlin
@Composable
fun ThemeTag(
    themeName: String,
    icon: String,  // Emoji
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
)
```
Chip coloré pour les catégories thématiques.

**Tests** :
```kotlin
class ComponentsTest {
    @Test
    fun `AppIcon displays fallback for null drawable`()

    @Test
    fun `AppIcon shows badge when provided`()

    @Test
    fun `EmptyState shows action button when provided`()

    @Test
    fun `ConfidenceBadge shows correct color for high confidence`()

    @Test
    fun `ThemeTag shows selected state`()

    @Test
    fun `LoadingShimmer animates`()
}
```

---

### 7.4 — Animations

```kotlin
// ui/theme/Animations.kt
object DeskZenAnimations {
    // Durées
    val durationQuick = 150    // Feedback immédiat
    val durationNormal = 250   // Transitions standard
    val durationMedium = 350   // Ouverture de panels
    val durationSlow = 500     // Réorganisation complète (jamais > 500ms)

    // Easing
    val easeOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val easeInOut = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    val spring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Transitions nommées
    val itemAppear = fadeIn(tween(durationNormal, easing = easeOut)) +
                     scaleIn(tween(durationNormal, easing = easeOut), initialScale = 0.92f)

    val itemDisappear = fadeOut(tween(durationQuick)) +
                        scaleOut(tween(durationQuick), targetScale = 0.92f)

    val folderOpen = expandVertically(tween(durationMedium, easing = easeInOut))
    val folderClose = shrinkVertically(tween(durationNormal, easing = easeOut))
}
```

**Animations clés** :
1. **Chargement** : Shimmer effect sur les cartes
2. **Apparition de liste** : Staggered animation (chaque carte arrive 50ms après la précédente)
3. **Drag & drop** : L'item suit le doigt avec une ombre élevée, les autres items se réarrangent en spring
4. **Ouverture dossier** : Expand depuis le coin du dossier avec scale + fade
5. **Suggestion acceptée** : Check mark vert animé + léger bounce
6. **Suggestion rejetée** : Slide out vers la gauche + fade

---

### 7.5 — Mode accessibilité

- Tous les Composables ont un `contentDescription` approprié
- Contraste minimum WCAG AA (4.5:1 pour le texte)
- Support talkback pour la navigation
- Touch targets minimum 48dp
- Pas de timing critique dans les interactions

---

## Livrables

| Fichier | Description |
|---------|-------------|
| `ui/theme/Color.kt` | Palette de couleurs |
| `ui/theme/Theme.kt` | Thème Material 3 |
| `ui/theme/Type.kt` | Typographie |
| `ui/theme/Shape.kt` | Formes |
| `ui/theme/Dimens.kt` | Tokens de dimensions |
| `ui/theme/Animations.kt` | Animations réutilisables |
| `ui/components/AppIcon.kt` | Icône d'app avec badge |
| `ui/components/EmptyState.kt` | État vide |
| `ui/components/LoadingShimmer.kt` | Shimmer loading |
| `ui/components/ConfidenceBadge.kt` | Badge de confiance |
| `ui/components/ThemeTag.kt` | Tag thématique |
| `ui/components/DeskZenTopBar.kt` | Top bar custom |
| Tests unitaires + UI | Tous les fichiers de test |

## Vérification

```bash
./gradlew testDebugUnitTest --tests "com.deskzen.ui.theme.*"
./gradlew connectedDebugAndroidTest --tests "com.deskzen.ui.components.*"
```

> **Astuce** : Créer un écran `DesignShowcase` (accessible uniquement en debug) qui affiche tous les composants pour validation visuelle rapide.
