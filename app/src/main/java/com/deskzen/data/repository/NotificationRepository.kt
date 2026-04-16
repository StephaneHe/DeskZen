package com.deskzen.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton repository tracking notification counts per package.
 * Updated by NotificationBadgeService, observed by LauncherViewModel.
 */
object NotificationRepository {

    private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

    fun getCount(packageName: String): Int = _badgeCounts.value[packageName] ?: 0

    /** Replace all counts (used on initial scan) */
    fun setAll(counts: Map<String, Int>) {
        _badgeCounts.value = counts
    }

    /** Increment count for a package */
    fun increment(packageName: String) {
        val current = _badgeCounts.value.toMutableMap()
        current[packageName] = (current[packageName] ?: 0) + 1
        _badgeCounts.value = current
    }

    /** Decrement count for a package (min 0) */
    fun decrement(packageName: String) {
        val current = _badgeCounts.value.toMutableMap()
        val newCount = ((current[packageName] ?: 0) - 1).coerceAtLeast(0)
        if (newCount == 0) current.remove(packageName) else current[packageName] = newCount
        _badgeCounts.value = current
    }
}
