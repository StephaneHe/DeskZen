package com.deskzen.ui.apps

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.usecase.ManageShortcutUseCase
import com.deskzen.domain.usecase.ShortcutResult
import com.deskzen.ui.components.DeskZenTopBar
import com.deskzen.ui.components.EmptyState
import com.deskzen.ui.components.LoadingShimmer
import com.deskzen.ui.theme.DeskZenDimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsListScreen(
    viewModel: AppsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    // Bottom sheet for selected app
    selectedApp?.let { appInfo ->
        AppActionsSheet(
            appInfo = appInfo,
            onCreateShortcut = {
                scope.launch {
                    val result = viewModel.createShortcut(appInfo)
                    val message = when (result) {
                        is ShortcutResult.Success -> "${appInfo.label} ajouté à l'écran d'accueil"
                        is ShortcutResult.Error -> result.reason
                        is ShortcutResult.NotSupported -> "Raccourcis non supportés sur ce launcher"
                        is ShortcutResult.PermissionRequired -> "Permission requise"
                    }
                    snackbarHostState.showSnackbar(message)
                }
            },
            onRemoveShortcut = {
                scope.launch {
                    viewModel.removeShortcut(appInfo.packageName)
                    snackbarHostState.showSnackbar("${appInfo.label} retiré")
                }
            },
            onOpenApp = {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            },
            onOpenAppSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${appInfo.packageName}")
                }
                context.startActivity(intent)
            },
            onDismiss = { selectedApp = null }
        )
    }

    Scaffold(
        topBar = {
            DeskZenTopBar(
                title = "Applications",
                subtitle = when (val state = uiState) {
                    is AppsUiState.Success -> "${state.filteredCount} / ${state.totalCount} apps"
                    else -> null
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState is AppsUiState.Loading,
            onRefresh = { viewModel.loadApps() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AppsUiState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(DeskZenDimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(DeskZenDimens.spacingSm)
                    ) {
                        items(8) {
                            LoadingShimmer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                            )
                        }
                    }
                }

                is AppsUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = DeskZenDimens.spacingMd,
                            end = DeskZenDimens.spacingMd,
                            bottom = DeskZenDimens.spacingMd
                        ),
                        verticalArrangement = Arrangement.spacedBy(DeskZenDimens.spacingSm)
                    ) {
                        item {
                            AppSearchBar(
                                query = searchQuery,
                                onQueryChanged = viewModel::onSearchQueryChanged,
                                sortMode = sortMode,
                                onSortModeChanged = viewModel::onSortModeChanged,
                                showSystemApps = showSystemApps,
                                onToggleSystemApps = viewModel::onToggleSystemApps
                            )
                            Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))
                        }

                        if (state.apps.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.SearchOff,
                                    title = "Aucune application",
                                    subtitle = "Aucun résultat pour \"$searchQuery\"",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = DeskZenDimens.spacingXl)
                                )
                            }
                        } else {
                            items(
                                items = state.apps,
                                key = { it.packageName }
                            ) { appInfo ->
                                AppCard(
                                    appInfo = appInfo,
                                    onClick = {
                                        // Tap = open bottom sheet with actions
                                        selectedApp = appInfo
                                    },
                                    onLongClick = {
                                        // Long press = also open actions
                                        selectedApp = appInfo
                                    }
                                )
                            }
                        }
                    }
                }

                is AppsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
