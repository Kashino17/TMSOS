package com.tms.os.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BadgeNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshBadges()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshBadges()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshBadges()
    }

    private fun refreshBadges() {
        try {
            val active = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            for (sbn in active) {
                if (sbn.isOngoing) continue
                val pkg = sbn.packageName ?: continue
                val num = sbn.notification?.number?.takeIf { it > 0 } ?: 1
                counts[pkg] = (counts[pkg] ?: 0) + num
            }
            _badgeCounts.value = counts
        } catch (_: Exception) {
            // Ignore security exceptions when service isn't bound yet
        }
    }

    companion object {
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()
    }
}
