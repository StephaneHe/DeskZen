package com.deskzen.domain.model

import android.graphics.Bitmap

data class ScreenPage(
    val pageIndex: Int,
    val items: List<ScreenItem>
)

sealed interface ScreenItem {
    val position: Int

    data class AppShortcut(
        override val position: Int,
        val appInfo: AppInfo
    ) : ScreenItem

    data class Folder(
        override val position: Int,
        val id: Long = 0,
        val name: String,
        val apps: List<AppInfo>,
        val color: Long? = null
    ) : ScreenItem

    data class WebShortcut(
        override val position: Int,
        val url: String,
        val label: String,
        val favicon: Bitmap? = null
    ) : ScreenItem
}
