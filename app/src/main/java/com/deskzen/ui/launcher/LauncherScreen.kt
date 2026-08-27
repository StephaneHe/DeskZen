package com.deskzen.ui.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallpaper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalConfiguration
import com.deskzen.ui.contacts.QuickContactsScreen
import com.deskzen.ui.organize.CellBounds
import com.deskzen.ui.organize.DragState
import com.deskzen.ui.organize.DropTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.components.PageIndicator
import com.deskzen.ui.theme.DeskZenDimens
import com.deskzen.ui.theme.SoloDeepBlack
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloGlow
import com.deskzen.ui.theme.SoloPurple
import com.deskzen.ui.theme.SoloSurface
import com.deskzen.ui.theme.SoloTextMuted

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val badgeCounts by viewModel.badgeCounts.collectAsState()
    val wallpaperBitmap by viewModel.wallpaperBitmap.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var appToMove by remember { mutableStateOf<String?>(null) }
    // Web shortcut dialog state: Pair(pageIndex, position) or null
    var webShortcutTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Empty cell context menu state: Pair(pageIndex, position) or null
    var emptyCellMenuTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.setWallpaper(uri) }
        }
    }

    // Ambient mode state — reset to invisible on every resume (unlock)
    var iconsVisible by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) iconsVisible = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val iconAlpha by animateFloatAsState(
        targetValue = if (iconsVisible) 1f else 0.06f,
        animationSpec = tween(180),
        label = "iconAlpha"
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (iconsVisible) 0.52f else 0.35f,
        animationSpec = tween(180),
        label = "overlayAlpha"
    )

    AnimatedContent(
        targetState = isLandscape,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "orientation_switch"
    ) { landscape ->
        if (landscape) {
            QuickContactsScreen(viewModel = viewModel)
        } else {

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            // Initial pass fires before any child — detects double tap regardless of icon state.
            // On double tap, consume the event so children (folders, icons) don't also react.
            awaitPointerEventScope {
                var lastTap = 0L
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val isDown = event.changes.any { it.pressed && !it.previousPressed }
                    if (isDown) {
                        val now = System.currentTimeMillis()
                        if (now - lastTap < 350L && lastTap > 0L) {
                            event.changes.forEach { it.consume() }
                            iconsVisible = !iconsVisible
                            lastTap = 0L
                        } else {
                            lastTap = now
                        }
                    }
                }
            }
        }
    ) {
        // Background: custom wallpaper or default Solo Leveling gradient
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF050810),
                                Color(0xFF0A1543),
                                Color(0xFF0D0D2B),
                                Color(0xFF1A0A2E),
                                Color(0xFF0A1543),
                                Color(0xFF050810)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(SoloPurple.copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(540f, 600f),
                                radius = 800f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(SoloElectricBlue.copy(alpha = 0.06f), Color.Transparent),
                                center = Offset(540f, 1800f),
                                radius = 600f
                            )
                        )
                )
            }
        }

        // Dim overlay — animates for BOTH wallpaper and gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )

        // Home screen pages — ambient opacity only, no pointer blocking here
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(iconAlpha)
        ) {
        HomeScreenContent(
            pages = uiState.pages,
            currentPageIndex = uiState.currentPage,
            badgeCounts = badgeCounts,
            onPageChanged = viewModel::onPageChanged,
            onAppClick = viewModel::launchApp,
            onAppLongClick = { pkg -> appToMove = pkg },
            onSwipeUp = viewModel::openDrawer,
            isAppLocked = viewModel::isAppLocked,
            dockApps = uiState.dockApps,
            onDockAppClick = viewModel::launchApp,
            onDockAppLongClick = { pos -> viewModel.removeDockApp(pos) },
            onWebShortcutClick = viewModel::openUrl,
            onEmptyCellLongPress = { page, pos -> emptyCellMenuTarget = Pair(page, pos) },
            onMoveItem = viewModel::moveItem,
            onInsertItem = viewModel::insertItem,
            onDropIntoFolder = viewModel::dropIntoFolder,
            onCreateFolderFromDrop = viewModel::createFolderFromDrop,
            onRemoveFromScreen = viewModel::removeFromScreen,
            scrollToFirstPage = viewModel.scrollToFirstPage
        )
        } // end ambient wrapper

        // When icons hidden: full-screen overlay above icons catches ALL taps,
        // double tap re-shows icons. Removed when icons are visible so icon taps work normally.
        if (!iconsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { iconsVisible = true })
                    }
            )
        }

        // App drawer overlay
        AnimatedVisibility(
            visible = uiState.drawerOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            AppDrawer(
                apps = viewModel.getFilteredApps(),
                searchQuery = uiState.drawerSearchQuery,
                onSearchChanged = viewModel::onDrawerSearchChanged,
                onAppClick = { pkg ->
                    viewModel.closeDrawer()
                    viewModel.launchApp(pkg)
                },
                onAppLongClick = { pkg ->
                    viewModel.closeDrawer()
                    appToMove = pkg
                },
                onClose = viewModel::closeDrawer,
                onOpenFolderManager = {
                    viewModel.closeDrawer()
                    viewModel.showFolderManager()
                },
            )
        }

        // Folder manager sheet
        if (uiState.showFolderManager) {
            FolderManagerSheet(
                folders = viewModel.getAllFolders(),
                onAddFolder = viewModel::addFolder,
                onRemoveFolder = viewModel::removeFolder,
                onRenameFolder = viewModel::renameFolder,
                onReDispatch = {
                    viewModel.reDispatchWithIA()
                    viewModel.hideFolderManager()
                },
                onExportBackup = {
                    val json = viewModel.exportBackup()
                    val file = java.io.File(context.filesDir, "deskzen_backup.json")
                    file.writeText(json)
                    android.widget.Toast.makeText(context, "Sauvegarde: ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                },
                onImportBackup = {
                    val file = java.io.File(context.filesDir, "deskzen_backup.json")
                    if (file.exists()) {
                        val data = BackupManager.importFromJson(file.readText())
                        if (data != null) {
                            viewModel.importBackup(data)
                            android.widget.Toast.makeText(context, "Configuration restaurée", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Fichier invalide", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Aucune sauvegarde trouvée", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    viewModel.hideFolderManager()
                },
                onChangeWallpaper = {
                    val pickIntent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                        type = "image/*"
                    }
                    wallpaperPickerLauncher.launch(
                        android.content.Intent.createChooser(pickIntent, "Choisir un fond d'écran")
                    )
                    viewModel.hideFolderManager()
                },
                onDismiss = viewModel::hideFolderManager
            )
        }

        // Move app to folder dialog
        appToMove?.let { pkg ->
            MoveToFolderDialog(
                packageName = pkg,
                appLabel = uiState.allApps.find { it.packageName == pkg }?.label ?: pkg,
                folders = viewModel.getAllFolderNames(),
                shortcuts = viewModel.getAppShortcuts(pkg),
                isLocked = viewModel.isAppLocked(pkg),
                currentFolderName = viewModel.getAppFolder(pkg),
                dockPositions = viewModel.getDockPositions(),
                currentDockApps = uiState.dockApps.map { it?.packageName },
                onMoveToFolder = { folderName ->
                    viewModel.moveAppToFolder(pkg, folderName)
                    appToMove = null
                },
                onAddToHomeScreen = {
                    viewModel.addAppToHomeScreen(pkg)
                    appToMove = null
                },
                onRemoveFromFolder = {
                    viewModel.removeAppFromFolder(pkg)
                    appToMove = null
                },
                onSetDockPosition = { pos ->
                    viewModel.setDockApp(pos, pkg)
                    appToMove = null
                },
                onOpenInfo = {
                    viewModel.openAppInfo(pkg)
                    appToMove = null
                },
                onLaunchShortcut = { shortcut ->
                    viewModel.launchShortcut(shortcut)
                    appToMove = null
                },
                onToggleLock = {
                    if (viewModel.isAppLocked(pkg)) {
                        viewModel.unlockApp(pkg)
                    } else {
                        // Lock in current folder — find which folder it's in
                        val currentFolder = uiState.pages
                            .flatMap { page -> page.items.filterIsInstance<ScreenItem.Folder>() }
                            .find { folder -> folder.apps.any { it.packageName == pkg } }
                            ?.name
                        if (currentFolder != null) {
                            viewModel.moveAppToFolder(pkg, currentFolder)
                        }
                    }
                    appToMove = null
                },
                onDismiss = { appToMove = null }
            )
        }

        // Web shortcut dialog
        webShortcutTarget?.let { (page, pos) ->
            AddWebShortcutDialog(
                onConfirm = { url, label ->
                    viewModel.addWebShortcut(page, pos, url, label)
                    webShortcutTarget = null
                },
                onFetchTitle = { url -> viewModel.fetchPageTitle(url) },
                onDismiss = { webShortcutTarget = null }
            )
        }

        // Empty cell action picker (web shortcut / wallpaper)
        emptyCellMenuTarget?.let { (page, pos) ->
            EmptyCellActionDialog(
                onCreateWebShortcut = {
                    emptyCellMenuTarget = null
                    webShortcutTarget = Pair(page, pos)
                },
                onChangeWallpaper = {
                    emptyCellMenuTarget = null
                    val pickIntent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                        type = "image/*"
                    }
                    wallpaperPickerLauncher.launch(
                        android.content.Intent.createChooser(pickIntent, "Choisir un fond d'écran")
                    )
                },
                onDismiss = { emptyCellMenuTarget = null }
            )
        }

    } // end Box
        } // end else (portrait)
    } // end AnimatedContent
}

@Composable
fun HomeScreenContent(
    pages: List<ScreenPage>,
    currentPageIndex: Int = 0,
    badgeCounts: Map<String, Int> = emptyMap(),
    onPageChanged: (Int) -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onSwipeUp: () -> Unit,
    isAppLocked: (String) -> Boolean = { false },
    dockApps: List<AppInfo?> = emptyList(),
    onDockAppClick: (String) -> Unit = {},
    onDockAppLongClick: (Int) -> Unit = {},
    onWebShortcutClick: (String) -> Unit = {},
    onEmptyCellLongPress: (Int, Int) -> Unit = { _, _ -> },
    onMoveItem: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onInsertItem: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onDropIntoFolder: (Int, Int, String) -> Unit = { _, _, _ -> },
    onCreateFolderFromDrop: (Int, Int, String) -> Unit = { _, _, _ -> },
    onRemoveFromScreen: (Int, Int) -> Unit = { _, _ -> },
    scrollToFirstPage: Flow<Unit>? = null
) {
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Drag state
    var dragState by remember { mutableStateOf(DragState()) }
    // Cell bounds registry for hit-testing (keyed by "page:position")
    val cellBoundsMap = remember { mutableStateMapOf<String, CellBounds>() }
    // Remove zone bounds
    var removeZoneBounds by remember { mutableStateOf<Rect?>(null) }
    // Throttle page edge scrolling
    var lastEdgeScrollTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) }
    }

    LaunchedEffect(scrollToFirstPage) {
        scrollToFirstPage?.collect { pagerState.animateScrollToPage(0) }
    }

    // Screen width for edge detection
    val configuration = LocalContext.current.resources.displayMetrics
    val screenWidthPx = configuration.widthPixels.toFloat()

    // Resolve drop target from finger position
    fun resolveDropTarget(fingerPos: Offset): DropTarget? {
        // Check remove zone first
        removeZoneBounds?.let { bounds ->
            if (bounds.contains(fingerPos)) return DropTarget.RemoveZone
        }

        val currentPage = pagerState.currentPage

        // Find cell under finger on current page
        for ((_, cell) in cellBoundsMap) {
            if (cell.page != currentPage) continue
            if (!cell.bounds.contains(fingerPos)) continue

            val item = cell.item
            val draggedItem = dragState.draggedItem

            // Same item as source — skip
            if (cell.page == dragState.sourcePage && cell.position == dragState.sourcePosition) return null

            // Empty slot → direct placement
            if (item == null) return DropTarget.EmptySlot(cell.page, cell.position)

            // Cell has an item — check if finger is on center (drop-on) or edge (insert)
            val cellWidth = cell.bounds.width
            val relativeX = fingerPos.x - cell.bounds.left
            val centerZone = 0.30f..0.70f // Center 40% = drop on item
            val normalizedX = relativeX / cellWidth

            val isOnCenter = normalizedX in centerZone

            return if (isOnCenter) {
                // Center of cell → action depends on item types
                when {
                    draggedItem is ScreenItem.AppShortcut && item is ScreenItem.Folder ->
                        DropTarget.IntoFolder(cell.page, cell.position)
                    draggedItem is ScreenItem.AppShortcut && item is ScreenItem.AppShortcut ->
                        DropTarget.AppOnApp(cell.page, cell.position)
                    // Folder on anything center → insert (no merge)
                    else -> DropTarget.InsertBefore(cell.page, cell.position)
                }
            } else {
                // Edge of cell → insert before or after
                if (normalizedX < 0.30f) {
                    DropTarget.InsertBefore(cell.page, cell.position)
                } else {
                    // Right edge → insert after = insert before next position
                    DropTarget.InsertBefore(cell.page, cell.position + 1)
                }
            }
        }
        return null
    }

    // Handle edge scrolling during drag
    fun checkEdgeScroll(fingerPos: Offset, screenWidth: Float) {
        val now = System.currentTimeMillis()
        if (now - lastEdgeScrollTime < 600) return // Throttle

        val edgeZone = screenWidth * 0.15f
        val targetPage = when {
            fingerPos.x < edgeZone && pagerState.currentPage > 0 ->
                pagerState.currentPage - 1
            fingerPos.x > screenWidth - edgeZone && pagerState.currentPage < pages.size - 1 ->
                pagerState.currentPage + 1
            else -> null
        }
        if (targetPage != null) {
            lastEdgeScrollTime = now
            coroutineScope.launch {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    // Track root offset of parent box for coordinate conversion
    var parentRootOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
            .onGloballyPositioned { coords ->
                parentRootOffset = coords.positionInRoot()
            }
            // Parent-level drag tracking: captures ALL pointer events during drag.
            // Uses PointerEventPass.Initial to intercept before children.
            // Does NOT wait for a new pointer down — the finger is already pressing.
            .pointerInput(dragState.isDragging) {
                if (!dragState.isDragging) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue

                        // Convert local position to root coordinates
                        val rootPos = change.position + parentRootOffset

                        if (!change.pressed) {
                            // Finger released → resolve drop
                            val target = dragState.currentDropTarget
                            val srcPage = dragState.sourcePage
                            val srcPos = dragState.sourcePosition
                            val item = dragState.draggedItem

                            when (target) {
                                is DropTarget.EmptySlot ->
                                    onMoveItem(srcPage, srcPos, target.page, target.position)
                                is DropTarget.InsertBefore ->
                                    onInsertItem(srcPage, srcPos, target.page, target.position)
                                is DropTarget.AppOnApp -> {
                                    if (item is ScreenItem.AppShortcut) {
                                        onCreateFolderFromDrop(target.page, target.position, item.appInfo.packageName)
                                    }
                                }
                                is DropTarget.IntoFolder -> {
                                    if (item is ScreenItem.AppShortcut) {
                                        onDropIntoFolder(target.page, target.position, item.appInfo.packageName)
                                    }
                                }
                                is DropTarget.RemoveZone ->
                                    onRemoveFromScreen(srcPage, srcPos)
                                null -> { /* Cancelled */ }
                            }

                            if (target != null) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }

                            dragState = DragState()
                            break
                        }

                        // Finger moved → update drag state
                        change.consume()
                        val offset = rootPos - dragState.startPosition
                        val newTarget = resolveDropTarget(rootPos)

                        if (newTarget != null && newTarget != dragState.currentDropTarget) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        dragState = dragState.copy(
                            fingerPosition = rootPos,
                            dragOffset = offset,
                            currentDropTarget = newTarget
                        )

                        checkEdgeScroll(rootPos, screenWidthPx)
                    }
                }
            }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    var triggered = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val dx = change.position.x - startPos.x
                        val dy = change.position.y - startPos.y
                        // Yield to HorizontalPager if horizontal motion dominates
                        if (kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f) break
                        if (!triggered && dy < -50f && !dragState.isDragging) {
                            onSwipeUp()
                            triggered = true
                        }
                    }
                }
            }
    ) {
        // Quick Toggles
        QuickTogglesBar()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = !dragState.isDragging,
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) { pageIndex ->
            if (pageIndex < pages.size) {
                HomePageGrid(
                    page = pages[pageIndex],
                    pageIndex = pageIndex,
                    badgeCounts = badgeCounts,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    onWebShortcutClick = onWebShortcutClick,
                    onEmptyCellLongPress = onEmptyCellLongPress,
                    isAppLocked = isAppLocked,
                    dragState = dragState,
                    onRegisterCellBounds = { pos, bounds, item ->
                        cellBoundsMap["$pageIndex:$pos"] = CellBounds(pageIndex, pos, bounds, item)
                    },
                    onDragStart = { item, position, globalPos ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragState = DragState(
                            isDragging = true,
                            draggedItem = item,
                            fingerPosition = globalPos,
                            startPosition = globalPos,
                            dragOffset = Offset.Zero,
                            sourcePage = pageIndex,
                            sourcePosition = position
                        )
                        // Parent pointerInput takes over from here
                    }
                )
            }
        }

        // Remove zone (visible during drag)
        AnimatedVisibility(visible = dragState.isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DeskZenDimens.spacingMd, vertical = DeskZenDimens.spacingSm)
                    .background(
                        if (dragState.currentDropTarget is DropTarget.RemoveZone)
                            Color(0xFFB71C1C).copy(alpha = 0.8f)
                        else Color(0xFF5D0000).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val size = coords.size
                        removeZoneBounds = Rect(
                            pos.x, pos.y,
                            pos.x + size.width, pos.y + size.height
                        )
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Retirer",
                        tint = Color.White
                    )
                    Text(
                        text = "Retirer de l'écran",
                        style = TextStyle(fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        // Dock (hidden during drag)
        if (!dragState.isDragging && dockApps.any { it != null }) {
            DockBar(
                dockApps = dockApps,
                badgeCounts = badgeCounts,
                onAppClick = onDockAppClick,
                onAppLongClick = onDockAppLongClick
            )
        }

        // Page indicator + swipe hint
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pages.size > 1) {
                PageIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!dragState.isDragging) {
                Text(
                    text = "⌃",
                    style = TextStyle(fontSize = 20.sp, color = Color.White.copy(alpha = 0.4f)),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Drag overlay — floating icon following finger
    if (dragState.isDragging && dragState.draggedItem != null) {
        DragOverlay(
            dragState = dragState
        )
    }

    } // end Box
}

/** Floating icon overlay that follows the finger during drag */
@Composable
fun DragOverlay(dragState: DragState) {
    val item = dragState.draggedItem ?: return
    val iconSize = DeskZenDimens.homeIconSize

    // Position: startPosition + dragOffset, centered on finger
    val offsetX = dragState.startPosition.x + dragState.dragOffset.x - with(LocalDensity.current) { iconSize.toPx() / 2 }
    val offsetY = dragState.startPosition.y + dragState.dragOffset.y - with(LocalDensity.current) { iconSize.toPx() / 2 }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .graphicsLayer {
                scaleX = 1.15f
                scaleY = 1.15f
                alpha = 0.9f
                shadowElevation = 16f
            }
    ) {
        when (item) {
            is ScreenItem.AppShortcut -> {
                AppIcon(
                    icon = item.appInfo.icon,
                    label = item.appInfo.label,
                    size = iconSize
                )
            }
            is ScreenItem.Folder -> {
                FolderIcon(folder = item)
            }
            is ScreenItem.WebShortcut -> {
                WebShortcutIcon(shortcut = item, size = iconSize)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePageGrid(
    page: ScreenPage,
    pageIndex: Int = 0,
    badgeCounts: Map<String, Int> = emptyMap(),
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onWebShortcutClick: (String) -> Unit = {},
    onEmptyCellLongPress: (Int, Int) -> Unit = { _, _ -> },
    isAppLocked: (String) -> Boolean = { false },
    dragState: DragState = DragState(),
    onRegisterCellBounds: (Int, Rect, ScreenItem?) -> Unit = { _, _, _ -> },
    onDragStart: (ScreenItem, Int, Offset) -> Unit = { _, _, _ -> }
) {
    val sortedItems = page.items.sortedBy { it.position }
    val totalSlots = DeskZenDimens.homeGridColumns * DeskZenDimens.homeGridRows

    // Build position→item map
    val itemByPosition = remember(sortedItems) {
        sortedItems.associateBy { it.position }
    }

    // Text shadow for readability over wallpaper
    val labelStyle = TextStyle(
        fontSize = DeskZenDimens.homeLabelSize,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        textAlign = TextAlign.Center,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(0f, 1f),
            blurRadius = 4f
        )
    )

    // Track which folder position is expanded (grid-level state)
    var expandedFolderPos by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize()) {

    LazyVerticalGrid(
        columns = GridCells.Fixed(DeskZenDimens.homeGridColumns),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = DeskZenDimens.homeGridPaddingH),
        contentPadding = PaddingValues(
            top = DeskZenDimens.spacingSm,
            bottom = 4.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(DeskZenDimens.homeGridHSpacing),
        verticalArrangement = Arrangement.SpaceBetween,
        userScrollEnabled = false
    ) {
        items(totalSlots) { position ->
            val item = itemByPosition[position]
            val isDragSource = dragState.isDragging &&
                    dragState.sourcePage == pageIndex &&
                    dragState.sourcePosition == position

            // Determine if this cell is a drop target
            val isDropTarget = dragState.isDragging && when (val target = dragState.currentDropTarget) {
                is DropTarget.EmptySlot -> target.page == pageIndex && target.position == position
                is DropTarget.InsertBefore -> target.page == pageIndex && target.position == position
                is DropTarget.AppOnApp -> target.page == pageIndex && target.position == position
                is DropTarget.IntoFolder -> target.page == pageIndex && target.position == position
                else -> false
            }

            // Track cell root position for coordinate conversion
            var cellRootPos by remember { mutableStateOf(Offset.Zero) }
            val folderExpanded = item is ScreenItem.Folder && expandedFolderPos == position

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val size = coords.size
                        cellRootPos = pos
                        val bounds = Rect(pos.x, pos.y, pos.x + size.width, pos.y + size.height)
                        onRegisterCellBounds(position, bounds, item)
                    }
                    .then(
                        if (isDropTarget) Modifier
                            .drawBehind {
                                drawRoundRect(
                                    color = SoloElectricBlue.copy(alpha = 0.3f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                )
                            }
                        else Modifier
                    )
                    .then(
                        if (isDragSource) Modifier.graphicsLayer { alpha = 0.3f }
                        else Modifier
                    )
                    .then(
                        if (!dragState.isDragging) {
                            Modifier.pointerInput(item, pageIndex, position) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val longPress = awaitLongPressOrCancellation(down.id)
                                    if (longPress == null) {
                                        // Tap or swipe — only handle tap on items
                                        if (item != null) {
                                            val isUp = currentEvent.changes.all { !it.pressed }
                                            if (isUp) {
                                                when (item) {
                                                    is ScreenItem.AppShortcut ->
                                                        onAppClick(item.appInfo.packageName)
                                                    is ScreenItem.Folder ->
                                                        expandedFolderPos = position
                                                    is ScreenItem.WebShortcut ->
                                                        onWebShortcutClick(item.url)
                                                }
                                            }
                                        }
                                        return@awaitEachGesture
                                    }

                                    // Long press confirmed
                                    if (item == null) {
                                        // Empty cell → open context menu (web shortcut / wallpaper)
                                        onEmptyCellLongPress(pageIndex, position)
                                        return@awaitEachGesture
                                    }

                                    // Item present — wait for movement or release
                                    val touchSlop = viewConfiguration.touchSlop

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break

                                        if (!change.pressed) {
                                            // Long press + release without move → context menu
                                            when (item) {
                                                is ScreenItem.AppShortcut ->
                                                    onAppLongClick(item.appInfo.packageName)
                                                is ScreenItem.Folder ->
                                                    onAppLongClick("folder:${item.name}")
                                                is ScreenItem.WebShortcut ->
                                                    onAppLongClick("web:${item.url}")
                                            }
                                            break
                                        }

                                        val dragDistance = (change.position - longPress.position).getDistance()
                                        if (dragDistance > touchSlop) {
                                            // Movement detected → start drag, parent takes over
                                            change.consume()
                                            val rootPos = cellRootPos + change.position
                                            onDragStart(item, position, rootPos)
                                            break
                                        }
                                    }
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item != null && !isDragSource) {
                    when (item) {
                        is ScreenItem.AppShortcut -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = DeskZenDimens.homeIconPadding)
                            ) {
                                AppIcon(
                                    icon = item.appInfo.icon,
                                    label = item.appInfo.label,
                                    size = DeskZenDimens.homeIconSize,
                                    notificationCount = badgeCounts[item.appInfo.packageName] ?: 0
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.appInfo.label,
                                    style = labelStyle,
                                    maxLines = DeskZenDimens.homeLabelMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        is ScreenItem.Folder -> {
                            val folderBadgeCount = item.apps.sumOf { app ->
                                badgeCounts[app.packageName] ?: 0
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = DeskZenDimens.homeIconPadding)
                            ) {
                                FolderIcon(folder = item, notificationCount = folderBadgeCount)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.name,
                                    style = labelStyle,
                                    maxLines = DeskZenDimens.homeLabelMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (folderExpanded) {
                                FolderSheet(
                                    folder = item,
                                    badgeCounts = badgeCounts,
                                    onAppClick = { pkg ->
                                        expandedFolderPos = -1
                                        onAppClick(pkg)
                                    },
                                    onMoveApp = { pkg ->
                                        expandedFolderPos = -1
                                        onAppLongClick(pkg)
                                    },
                                    isAppLocked = isAppLocked,
                                    onDismiss = { expandedFolderPos = -1 }
                                )
                            }
                        }
                        is ScreenItem.WebShortcut -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = DeskZenDimens.homeIconPadding)
                            ) {
                                WebShortcutIcon(shortcut = item, size = DeskZenDimens.homeIconSize)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    style = labelStyle,
                                    maxLines = DeskZenDimens.homeLabelMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    } // end Column
}

@Composable
fun FolderIcon(folder: ScreenItem.Folder, notificationCount: Int = 0) {
    Box {
    Card(
        modifier = Modifier.size(DeskZenDimens.folderIconSize),
        shape = RoundedCornerShape(DeskZenDimens.folderCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = SoloSurface.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, SoloElectricBlue.copy(alpha = 0.3f)
        )
    ) {
        val previewApps = folder.apps.take(4)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            userScrollEnabled = false
        ) {
            items(previewApps) { app ->
                AppIcon(
                    icon = app.icon,
                    label = app.label,
                    size = DeskZenDimens.folderMiniIconSize
                )
            }
        }
    }
    // Notification badge on folder
    if (notificationCount > 0) {
        val countText = if (notificationCount > 99) "99+" else notificationCount.toString()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .widthIn(min = 18.dp)
                .background(Color(0xFFE53935), CircleShape)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = countText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
    } // end Box
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderSheet(
    folder: ScreenItem.Folder,
    badgeCounts: Map<String, Int> = emptyMap(),
    onAppClick: (String) -> Unit,
    onMoveApp: ((String) -> Unit)? = null,
    isAppLocked: (String) -> Boolean = { false },
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = SoloSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DeskZenDimens.spacingMd)
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                color = SoloGlow,
                modifier = Modifier.padding(bottom = DeskZenDimens.spacingMd)
            )
            Text(
                text = "${folder.apps.size} apps — appui long pour actions",
                style = TextStyle(fontSize = 12.sp, color = SoloPurple.copy(alpha = 0.6f)),
                modifier = Modifier.padding(bottom = DeskZenDimens.spacingSm)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.height(
                    ((folder.apps.size / 4 + 1) * 80).coerceAtMost(400).dp
                )
            ) {
                items(folder.apps) { app ->
                    val locked = isAppLocked(app.packageName)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = { onMoveApp?.invoke(app.packageName) }
                        )
                    ) {
                        Box {
                            AppIcon(
                                icon = app.icon,
                                label = app.label,
                                size = DeskZenDimens.homeIconSize,
                                notificationCount = badgeCounts[app.packageName] ?: 0
                            )
                            if (locked) {
                                Text(
                                    "🔒",
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            style = TextStyle(
                                fontSize = DeskZenDimens.homeLabelSize,
                                color = if (locked) SoloElectricBlue else Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(DeskZenDimens.spacingLg))
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onClose: () -> Unit,
    onOpenFolderManager: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SoloDeepBlack.copy(alpha = 0.98f),
                        SoloSurface.copy(alpha = 0.98f)
                    )
                )
            )
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DeskZenDimens.spacingMd,
                    vertical = DeskZenDimens.spacingSm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Applications",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = SoloGlow,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${apps.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = SoloPurple
            )
            Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
            IconButton(onClick = onOpenFolderManager) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Dossiers", tint = SoloElectricBlue)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DeskZenDimens.spacingMd),
            placeholder = { Text("Rechercher...", color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = SoloElectricBlue)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Close, "Effacer", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoloElectricBlue,
                unfocusedBorderColor = SoloPurple.copy(alpha = 0.3f),
                cursorColor = SoloElectricBlue,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))

        // App list
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = DeskZenDimens.spacingMd,
                vertical = DeskZenDimens.spacingSm
            )
        ) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = { onAppLongClick(app.packageName) }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        icon = app.icon,
                        label = app.label,
                        size = DeskZenDimens.drawerIconSize
                    )
                    Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = TextStyle(
                                fontSize = DeskZenDimens.drawerLabelSize,
                                color = Color.White,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        app.category?.let {
                            Text(
                                text = it,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = SoloPurple.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Icon for a web shortcut — shows favicon or default globe */
@Composable
fun WebShortcutIcon(shortcut: ScreenItem.WebShortcut, size: androidx.compose.ui.unit.Dp) {
    if (shortcut.favicon != null) {
        Image(
            bitmap = shortcut.favicon.asImageBitmap(),
            contentDescription = shortcut.label,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = shortcut.label,
            modifier = Modifier.size(size),
            tint = SoloElectricBlue
        )
    }
}

/** Dialog to add a web shortcut */
@Composable
fun AddWebShortcutDialog(
    onConfirm: (url: String, label: String) -> Unit,
    onFetchTitle: suspend (String) -> String?,
    onDismiss: () -> Unit
) {
    var useHttps by remember { mutableStateOf(true) }
    var domain by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun buildUrl(): String {
        val scheme = if (useHttps) "https://" else "http://"
        return scheme + domain.trim()
    }

    fun fetchTitleFromUrl() {
        val fullUrl = buildUrl()
        if (domain.isNotBlank()) {
            isFetching = true
            fetchError = null
            coroutineScope.launch {
                val title = onFetchTitle(fullUrl)
                if (title != null) {
                    label = title
                } else {
                    fetchError = "Impossible de charger le titre"
                }
                isFetching = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoloSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Raccourci web", color = SoloGlow, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // Protocol toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    TextButton(
                        onClick = { useHttps = true },
                        modifier = Modifier.background(
                            if (useHttps) SoloElectricBlue.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text("https://", color = if (useHttps) SoloElectricBlue else SoloPurple.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { useHttps = false },
                        modifier = Modifier.background(
                            if (!useHttps) SoloElectricBlue.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text("http://", color = if (!useHttps) SoloElectricBlue else SoloPurple.copy(alpha = 0.5f))
                    }
                }

                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it.removePrefix("https://").removePrefix("http://") },
                    label = { Text("Adresse", color = SoloPurple) },
                    placeholder = { Text("example.com", color = Color.White.copy(alpha = 0.3f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoloElectricBlue,
                        unfocusedBorderColor = SoloPurple.copy(alpha = 0.3f),
                        cursorColor = SoloElectricBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nom", color = SoloPurple) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoloElectricBlue,
                        unfocusedBorderColor = SoloPurple.copy(alpha = 0.3f),
                        cursorColor = SoloElectricBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isFetching) {
                    Text("Chargement du titre...", color = SoloPurple, fontSize = 12.sp)
                }
                fetchError?.let {
                    Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                }
                TextButton(
                    onClick = { fetchTitleFromUrl() },
                    enabled = !isFetching && domain.isNotBlank()
                ) {
                    Text("Charger le titre automatiquement", color = SoloElectricBlue, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fullUrl = buildUrl()
                    val finalLabel = label.ifBlank { domain }
                    if (domain.isNotBlank()) {
                        onConfirm(fullUrl.trim(), finalLabel.trim())
                    }
                },
                enabled = domain.isNotBlank()
            ) {
                Text("Ajouter", color = SoloGlow)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = SoloTextMuted)
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockBar(
    dockApps: List<AppInfo?>,
    badgeCounts: Map<String, Int> = emptyMap(),
    onAppClick: (String) -> Unit,
    onAppLongClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                SoloDeepBlack.copy(alpha = 0.5f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = DeskZenDimens.spacingMd, vertical = DeskZenDimens.spacingSm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dockApps.forEachIndexed { index, app ->
            if (app != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = { onAppLongClick(index) }
                        )
                ) {
                    AppIcon(
                        icon = app.icon,
                        label = app.label,
                        size = DeskZenDimens.dockIconSize,
                        notificationCount = badgeCounts[app.packageName] ?: 0
                    )
                    Text(
                        text = app.label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = Offset(0f, 1f),
                                blurRadius = 3f
                            )
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // Empty dock slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(DeskZenDimens.dockIconSize),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SoloPurple.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

/** Context menu shown on long-press of an empty home-screen cell */
@Composable
fun EmptyCellActionDialog(
    onCreateWebShortcut: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoloSurface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Cellule vide", color = SoloGlow, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreateWebShortcut)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = SoloElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Créer un raccourci web", color = Color.White, fontSize = 15.sp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onChangeWallpaper)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = SoloElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Changer le fond d'écran", color = Color.White, fontSize = 15.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = SoloTextMuted) }
        }
    )
}
