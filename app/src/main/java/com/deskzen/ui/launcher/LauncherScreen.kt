package com.deskzen.ui.launcher

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
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

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var appToMove by remember { mutableStateOf<String?>(null) }

    // Start widget host
    LaunchedEffect(Unit) {
        viewModel.widgetManager.startListening()
    }

    // System wallpaper
    val wallpaperBitmap = remember {
        try {
            val wm = WallpaperManager.getInstance(context)
            wm.drawable?.toBitmap()?.asImageBitmap()
        } catch (e: Exception) { null }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper background
        if (wallpaperBitmap != null) {
            Image(
                painter = BitmapPainter(wallpaperBitmap),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoloDeepBlack.copy(alpha = 0.4f))
            )
        } else {
            // Gradient fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SoloDeepBlack, SoloSurface, SoloDeepBlack)
                        )
                    )
            )
        }

        // Home screen pages
        HomeScreenContent(
            pages = uiState.pages,
            onPageChanged = viewModel::onPageChanged,
            onAppClick = viewModel::launchApp,
            onAppLongClick = { pkg -> appToMove = pkg },
            onSwipeUp = viewModel::openDrawer
        )

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
                onOpenWidgetPicker = {
                    viewModel.closeDrawer()
                    viewModel.showWidgetPicker()
                }
            )
        }

        // Folder manager sheet
        if (uiState.showFolderManager) {
            FolderManagerSheet(
                folders = viewModel.getAllFolders(),
                onAddFolder = viewModel::addFolder,
                onRemoveFolder = viewModel::removeFolder,
                onReDispatch = {
                    viewModel.reDispatchWithIA()
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
                onMoveToFolder = { folderName ->
                    viewModel.moveAppToFolder(pkg, folderName)
                    appToMove = null
                },
                onAddToHomeScreen = {
                    viewModel.addAppToHomeScreen(pkg)
                    appToMove = null
                },
                onOpenInfo = {
                    viewModel.openAppInfo(pkg)
                    appToMove = null
                },
                onDismiss = { appToMove = null }
            )
        }

        // Widget picker
        if (uiState.showWidgetPicker) {
            val activity = context as? com.deskzen.MainActivity
            WidgetPickerSheet(
                widgets = viewModel.getAvailableWidgets(),
                onWidgetSelected = { providerInfo ->
                    val widgetId = viewModel.widgetManager.allocateWidgetId()
                    val bound = viewModel.widgetManager.bindWidget(widgetId, providerInfo)
                    if (bound) {
                        // Check if widget needs configuration
                        val configActivity = providerInfo.configure
                        if (configActivity != null) {
                            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                                component = configActivity
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            }
                            activity?.launchWidgetConfig(configIntent) { success ->
                                if (success) viewModel.addWidget(widgetId)
                                else viewModel.widgetManager.deallocateWidgetId(widgetId)
                            }
                        } else {
                            viewModel.addWidget(widgetId)
                        }
                    } else {
                        // Request bind permission via system dialog
                        val bindIntent = viewModel.widgetManager.getBindIntent(widgetId, providerInfo)
                        activity?.requestWidgetBind(bindIntent) { success ->
                            if (success) {
                                val configActivity2 = providerInfo.configure
                                if (configActivity2 != null) {
                                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                                        component = configActivity2
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                    }
                                    activity.launchWidgetConfig(configIntent) { configSuccess ->
                                        if (configSuccess) viewModel.addWidget(widgetId)
                                        else viewModel.widgetManager.deallocateWidgetId(widgetId)
                                    }
                                } else {
                                    viewModel.addWidget(widgetId)
                                }
                            } else {
                                viewModel.widgetManager.deallocateWidgetId(widgetId)
                            }
                        }
                    }
                    viewModel.hideWidgetPicker()
                },
                onDismiss = viewModel::hideWidgetPicker
            )
        }

        // Display active widgets at the top of home screen
        if (uiState.activeWidgetIds.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
            ) {
                uiState.activeWidgetIds.forEach { widgetId ->
                    val widgetView = remember(widgetId) {
                        viewModel.widgetManager.createWidgetView(widgetId)
                    }
                    widgetView?.let { view ->
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { view },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DeskZenDimens.spacingMd)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    pages: List<ScreenPage>,
    onPageChanged: (Int) -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onSwipeUp: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -50) onSwipeUp()
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            if (pageIndex < pages.size) {
                HomePageGrid(
                    page = pages[pageIndex],
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick
                )
            }
        }

        // Page indicator + swipe hint
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pages.size > 1) {
                PageIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = "⌃",
                style = TextStyle(fontSize = 20.sp, color = Color.White.copy(alpha = 0.4f)),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePageGrid(
    page: ScreenPage,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit
) {
    val sortedItems = page.items.sortedBy { it.position }

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

    LazyVerticalGrid(
        columns = GridCells.Fixed(DeskZenDimens.homeGridColumns),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DeskZenDimens.homeGridPaddingH),
        contentPadding = PaddingValues(
            top = DeskZenDimens.spacingMd,
            bottom = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(DeskZenDimens.homeGridHSpacing),
        verticalArrangement = Arrangement.spacedBy(DeskZenDimens.homeGridVSpacing),
        userScrollEnabled = false
    ) {
        items(sortedItems) { item ->
            when (item) {
                is ScreenItem.AppShortcut -> {
                    var showMenu by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onAppClick(item.appInfo.packageName) },
                                onLongClick = { showMenu = true }
                            )
                            .padding(vertical = DeskZenDimens.homeIconPadding)
                    ) {
                        AppIcon(
                            icon = item.appInfo.icon,
                            label = item.appInfo.label,
                            size = DeskZenDimens.homeIconSize
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.appInfo.label,
                            style = labelStyle,
                            maxLines = DeskZenDimens.homeLabelMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Informations") },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                onClick = {
                                    showMenu = false
                                    onAppLongClick(item.appInfo.packageName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Désinstaller") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
                is ScreenItem.Folder -> {
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(vertical = DeskZenDimens.homeIconPadding)
                    ) {
                        FolderIcon(folder = item)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.name,
                            style = labelStyle,
                            maxLines = DeskZenDimens.homeLabelMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (expanded) {
                        FolderSheet(
                            folder = item,
                            onAppClick = { pkg ->
                                expanded = false
                                onAppClick(pkg)
                            },
                            onMoveApp = { pkg ->
                                expanded = false
                                onAppLongClick(pkg) // triggers move dialog
                            },
                            onDismiss = { expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderIcon(folder: ScreenItem.Folder) {
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderSheet(
    folder: ScreenItem.Folder,
    onAppClick: (String) -> Unit,
    onMoveApp: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var appToMove by remember { mutableStateOf<String?>(null) }

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
                text = "${folder.apps.size} apps — appui long pour déplacer",
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
                    var showMenu by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = {
                                if (onMoveApp != null) {
                                    appToMove = app.packageName
                                } else {
                                    showMenu = true
                                }
                            }
                        )
                    ) {
                        AppIcon(
                            icon = app.icon,
                            label = app.label,
                            size = DeskZenDimens.homeIconSize
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            style = TextStyle(
                                fontSize = DeskZenDimens.homeLabelSize,
                                color = Color.White
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

    // Move app dialog
    appToMove?.let { pkg ->
        onMoveApp?.let { move ->
            move(pkg)
            appToMove = null
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
    onOpenFolderManager: () -> Unit = {},
    onOpenWidgetPicker: () -> Unit = {}
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
            IconButton(onClick = onOpenWidgetPicker) {
                Icon(Icons.Default.Widgets, contentDescription = "Widgets", tint = SoloPurple)
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
                var showMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = { showMenu = true }
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
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Informations") },
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        onClick = {
                            showMenu = false
                            onAppLongClick(app.packageName)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Désinstaller") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { showMenu = false }
                    )
                }
            }
        }
    }
}
