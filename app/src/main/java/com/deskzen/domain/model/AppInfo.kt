package com.deskzen.domain.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val installDate: Long,
    val lastUsedDate: Long? = null,
    val category: String? = null,
    val versionName: String? = null,
    val isOnHomeScreen: Boolean = false
)
