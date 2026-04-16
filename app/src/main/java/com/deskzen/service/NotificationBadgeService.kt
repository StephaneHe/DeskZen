package com.deskzen.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.deskzen.data.repository.NotificationRepository
import timber.log.Timber

/**
 * Listens to all notifications and updates badge counts in NotificationRepository.
 * Must be enabled by the user in Settings > Notifications > Notification access.
 */
class NotificationBadgeService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Timber.d("NotificationBadgeService connected")
        refreshAllCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (isGroupSummary(sbn)) return
        NotificationRepository.increment(sbn.packageName)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        if (isGroupSummary(sbn)) return
        NotificationRepository.decrement(sbn.packageName)
    }

    /** Full scan of active notifications to sync counts */
    private fun refreshAllCounts() {
        try {
            val active = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            for (sbn in active) {
                if (isGroupSummary(sbn)) continue
                counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
            }
            NotificationRepository.setAll(counts)
            Timber.d("Badge counts synced: ${counts.size} apps with notifications")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh notification counts")
        }
    }

    /** Group summary notifications should be ignored to avoid double-counting */
    private fun isGroupSummary(sbn: StatusBarNotification): Boolean {
        return sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0
    }
}
