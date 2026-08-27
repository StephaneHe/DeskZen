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
    val isOnHomeScreen: Boolean = false,
    /**
     * Launcher activity class name this entry resolves to. A single package can
     * expose several launcher activities (e.g. the Google app), so this is what
     * makes an entry unique — [packageName] alone is not. Null when the entry was
     * built from a package lookup rather than a resolved launcher activity.
     */
    val activityName: String? = null
)
