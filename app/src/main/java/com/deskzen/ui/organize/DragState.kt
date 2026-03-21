package com.deskzen.ui.organize

import androidx.compose.ui.geometry.Offset
import com.deskzen.domain.model.ScreenItem

data class DragState(
    val isDragging: Boolean = false,
    val draggedItem: ScreenItem? = null,
    val dragOffset: Offset = Offset.Zero,
    val sourcePage: Int = 0,
    val sourcePosition: Int = 0,
    val currentDropTarget: DropTarget? = null
)

sealed interface DropTarget {
    data class EmptySlot(val page: Int, val position: Int) : DropTarget
    data class ExistingItem(val page: Int, val position: Int) : DropTarget
    data class Folder(val folderId: Long) : DropTarget
    data object RemoveZone : DropTarget
    data object PageEdge : DropTarget
}
