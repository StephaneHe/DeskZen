package com.deskzen.domain.usecase

import com.deskzen.domain.model.AppInfo

interface ManageShortcutUseCase {
    suspend fun createShortcut(appInfo: AppInfo): ShortcutResult
    suspend fun removeShortcut(packageName: String): ShortcutResult
    suspend fun isShortcutPinned(packageName: String): Boolean
    fun canPinShortcuts(): Boolean
}

sealed interface ShortcutResult {
    data object Success : ShortcutResult
    data class Error(val reason: String) : ShortcutResult
    data object NotSupported : ShortcutResult
    data object PermissionRequired : ShortcutResult
}
