package com.deskzen.ui.homescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.components.DeskZenTopBar
import com.deskzen.ui.components.EmptyState
import com.deskzen.ui.components.PageIndicator
import com.deskzen.ui.theme.DeskZenDimens

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
                    is HomeScreenUiState.Success -> "Page ${currentPage + 1} / ${state.pages.size}"
                    else -> null
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is HomeScreenUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chargement...")
                }
            }

            is HomeScreenUiState.Success -> {
                if (state.pages.all { it.items.isEmpty() }) {
                    EmptyState(
                        icon = Icons.Outlined.Smartphone,
                        title = "Écran vide",
                        subtitle = "Ajoutez des applications depuis l'onglet Apps",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    ScreenPager(
                        pages = state.pages,
                        onPageChanged = viewModel::onPageChanged,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            is HomeScreenUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenPager(
    pages: List<ScreenPage>,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            ScreenPageGrid(
                page = pages[pageIndex],
                modifier = Modifier.fillMaxSize()
            )
        }

        if (pages.size > 1) {
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(DeskZenDimens.spacingMd)
            )
        }
    }
}

@Composable
fun ScreenPageGrid(
    page: ScreenPage,
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
                    is ScreenItem.AppShortcut -> ScreenAppItem(item = item)
                    is ScreenItem.Folder -> ScreenFolderItem(folder = item)
                }
            } else {
                EmptySlot()
            }
        }
    }
}

@Composable
fun ScreenAppItem(item: ScreenItem.AppShortcut) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.aspectRatio(1f)
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

@Composable
fun ScreenFolderItem(folder: ScreenItem.Folder) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.aspectRatio(1f)
    ) {
        Card(
            modifier = Modifier.size(DeskZenDimens.appIconMedium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${folder.apps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = folder.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EmptySlot() {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
    )
}
