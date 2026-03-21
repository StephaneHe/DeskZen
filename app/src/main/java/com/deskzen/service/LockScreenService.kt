package com.deskzen.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LockScreenService : AccessibilityService() {

    companion object {
        var instance: LockScreenService? = null
            private set

        fun lockScreen() {
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun isAvailable(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — we only need the lock screen action
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
