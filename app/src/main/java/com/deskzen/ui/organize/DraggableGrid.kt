package com.deskzen.ui.organize

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.theme.DeskZenDimens

/**
 * Legacy draggable grid for the organize screen.
 * The main launcher now uses its own drag system in HomeScreenContent.
 */
@Composable
fun DraggableGrid(
    pages: List<ScreenPage>,
    columns: Int = DeskZenDimens.gridColumns,
    rows: Int = DeskZenDimens.gridRows,
    onMoveItem: (fromPage: Int, fromPos: Int, toPage: Int, toPos: Int) -> Unit,
    onMergeItems: (page: Int, pos1: Int, pos2: Int) -> Unit,
    onDropInFolder: (folderId: Long, packageName: String) -> Unit,
    onRemoveFromScreen: (page: Int, position: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragState by remember { mutableStateOf(DragState()) }

    val currentPage = pages.firstOrNull() ?: return
    val totalSlots = columns * rows

    Column(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .weight(1f)
                .padding(DeskZenDimens.spacingMd)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragState = dragState.copy(isDragging = true)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragState = dragState.copy(
                                dragOffset = dragState.dragOffset.copy(
                                    x = dragState.dragOffset.x + dragAmount.x,
                                    y = dragState.dragOffset.y + dragAmount.y
                                )
                            )
                        },
                        onDragEnd = {
                            val target = dragState.currentDropTarget
                            when (target) {
                                is DropTarget.EmptySlot -> {
                                    onMoveItem(
                                        dragState.sourcePage,
                                        dragState.sourcePosition,
                                        target.page,
                                        target.position
                                    )
                                }
                                is DropTarget.AppOnApp -> {
                                    onMergeItems(
                                        dragState.sourcePage,
                                        dragState.sourcePosition,
                                        target.position
                                    )
                                }
                                is DropTarget.IntoFolder -> {
                                    val item = dragState.draggedItem
                                    if (item is ScreenItem.AppShortcut) {
                                        onDropInFolder(
                                            target.position.toLong(),
                                            item.appInfo.packageName
                                        )
                                    }
                                }
                                is DropTarget.RemoveZone -> {
                                    onRemoveFromScreen(
                                        dragState.sourcePage,
                                        dragState.sourcePosition
                                    )
                                }
                                else -> { /* Cancel */ }
                            }
                            dragState = DragState()
                        },
                        onDragCancel = {
                            dragState = DragState()
                        }
                    )
                },
            contentPadding = PaddingValues(DeskZenDimens.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(DeskZenDimens.gridItemSpacing),
            verticalArrangement = Arrangement.spacedBy(DeskZenDimens.gridItemSpacing)
        ) {
            items(totalSlots) { position ->
                val item = currentPage.items.find { it.position == position }
                if (item != null) {
                    when (item) {
                        is ScreenItem.AppShortcut -> {
                            AppIcon(
                                icon = item.appInfo.icon,
                                label = item.appInfo.label,
                                size = DeskZenDimens.appIconMedium
                            )
                        }
                        is ScreenItem.Folder -> {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.aspectRatio(1f).padding(4.dp))
                }
            }
        }

        // Remove zone
        if (dragState.isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DeskZenDimens.spacingMd)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.shapes.medium
                    )
                    .padding(DeskZenDimens.spacingMd),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Retirer",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Retirer de l'écran",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
