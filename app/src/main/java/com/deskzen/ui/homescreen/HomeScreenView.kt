package com.deskzen.ui.homescreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.components.DeskZenTopBar
import com.deskzen.ui.components.EmptyState
import com.deskzen.ui.components.PageIndicator
import com.deskzen.ui.theme.DeskZenDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenView(
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()

    Scaffold(
        topBar = {
            DeskZenTopBar(
                title = "Mon Écran",
                subtitle = when (val state = uiState) {
                    is HomeScreenUiState.Success -> {
                        val itemCount = state.pages.sumOf { it.items.size }
                        "Page ${currentPage + 1}/${state.pages.size} — $itemCount éléments"
                    }
                    else -> null
                },
                actions = {
                    IconButton(onClick = { viewModel.onAddPage() }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter une page")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is HomeScreenUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Chargement...") }
            }

            is HomeScreenUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    val pagerState = rememberPagerState(pageCount = { state.pages.size })

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { viewModel.onPageChanged(it) }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { pageIndex ->
                        ScreenPageGrid(
                            page = state.pages[pageIndex],
                            pageIndex = pageIndex,
                            onEmptySlotTap = { pos -> viewModel.onEmptySlotTap(pageIndex, pos) },
                            onEmptySlotLongPress = { pos -> viewModel.onShowCreateFolderDialog(pageIndex, pos) },
                            onItemLongPress = { pos -> viewModel.onRemoveItem(pageIndex, pos) },
                            onFolderTap = { /* TODO: open folder detail */ },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (state.pages.size > 1) {
                        PageIndicator(
                            pageCount = state.pages.size,
                            currentPage = pagerState.currentPage,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(DeskZenDimens.spacingMd)
                        )
                    }
                }

                // Add app bottom sheet
                if (state.showAddAppSheet) {
                    AddAppSheet(
                        apps = state.availableApps,
                        onAppSelected = { app ->
                            viewModel.onAddApp(app)
                        },
                        onDismiss = { viewModel.onDismissSheet() }
                    )
                }

                // Create folder dialog
                if (state.showCreateFolderDialog) {
                    CreateFolderDialog(
                        onConfirm = { name ->
                            viewModel.onCreateFolder(
                                state.selectedPageIndex,
                                state.selectedPosition,
                                name
                            )
                            viewModel.onDismissSheet()
                        },
                        onDismiss = { viewModel.onDismissSheet() }
                    )
                }
            }

            is HomeScreenUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenPageGrid(
    page: ScreenPage,
    pageIndex: Int,
    onEmptySlotTap: (Int) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit,
    onItemLongPress: (Int) -> Unit,
    onFolderTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSlots = DeskZenDimens.gridColumns * DeskZenDimens.gridRows

    LazyVerticalGrid(
        columns = GridCells.Fixed(DeskZenDimens.gridColumns),
        modifier = modifier.padding(DeskZenDimens.spacingMd),
        contentPadding = PaddingValues(DeskZenDimens.spacingSm),
        horizontalArrangement = Arrangement.spacedBy(DeskZenDimens.gridItemSpacing),
        verticalArrangement = Arrangement.spacedBy(DeskZenDimens.gridItemSpacing)
    ) {
        items(totalSlots) { position ->
            val item = page.items.find { it.position == position }
            if (item != null) {
                when (item) {
                    is ScreenItem.AppShortcut -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .aspectRatio(0.8f)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onItemLongPress(position) }
                                )
                        ) {
                            AppIcon(
                                icon = item.appInfo.icon,
                                label = item.appInfo.label,
                                size = DeskZenDimens.appIconMedium
                            )
                            Text(
                                text = item.appInfo.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is ScreenItem.Folder -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .aspectRatio(0.8f)
                                .combinedClickable(
                                    onClick = { onFolderTap(position) },
                                    onLongClick = { onItemLongPress(position) }
                                )
                        ) {
                            Card(
                                modifier = Modifier.size(DeskZenDimens.appIconMedium),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${item.apps.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Empty slot — tap to add
                Box(
                    modifier = Modifier
                        .aspectRatio(0.8f)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(8.dp)
                        )
                        .combinedClickable(
                            onClick = { onEmptySlotTap(position) },
                            onLongClick = { onEmptySlotLongPress(position) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = "Ajouter",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppSheet(
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filtered = if (searchQuery.isBlank()) apps
    else apps.filter {
        it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DeskZenDimens.spacingMd)
        ) {
            Text(
                text = "Ajouter une application",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = DeskZenDimens.spacingSm)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Rechercher...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))

            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAppSelected(app)
                                onDismiss()
                            }
                            .padding(vertical = DeskZenDimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            icon = app.icon,
                            label = app.label,
                            size = DeskZenDimens.appIconSmall
                        )
                        Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingMd))
        }
    }
}

@Composable
fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
        title = { Text("Nouveau dossier") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                placeholder = { Text("Nom du dossier") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
