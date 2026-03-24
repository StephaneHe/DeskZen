package com.deskzen.ui.organize

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.deskzen.domain.model.ScreenItem

data class DragState(
    val isDragging: Boolean = false,
    val draggedItem: ScreenItem? = null,
    /** Current finger position in global (root) coordinates */
    val fingerPosition: Offset = Offset.Zero,
    /** Offset of the overlay icon relative to finger start */
    val dragOffset: Offset = Offset.Zero,
    /** Position where the icon was picked up (for overlay initial placement) */
    val startPosition: Offset = Offset.Zero,
    val sourcePage: Int = 0,
    val sourcePosition: Int = 0,
    val currentDropTarget: DropTarget? = null
)

sealed interface DropTarget {
    /** Drop on an empty grid slot */
    data class EmptySlot(val page: Int, val position: Int) : DropTarget
    /** Insert before this item (shift right) */
    data class InsertBefore(val page: Int, val position: Int) : DropTarget
    /** Drop app on another app → create folder */
    data class AppOnApp(val page: Int, val position: Int) : DropTarget
    /** Drop app into existing folder */
    data class IntoFolder(val page: Int, val position: Int) : DropTarget
    /** Remove from home screen */
    data object RemoveZone : DropTarget
}

/** Tracks the bounds of each grid cell for hit-testing */
data class CellBounds(
    val page: Int,
    val position: Int,
    val bounds: Rect,
    val item: ScreenItem?
)
