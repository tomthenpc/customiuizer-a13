package com.android.systemui.statusbar.phone

import android.app.PendingIntent
import android.content.Intent
import com.android.systemui.statusbar.notification.collection.NotificationEntry

class MiuiStatusBarNotificationActivityStarter {
    fun startNotificationIntent(
        pendingIntent: PendingIntent,
        fillInIntent: Intent?,
        entry: NotificationEntry,
        row: Any?,
        animate: Boolean,
        isActivityIntent: Boolean
    ) {}
}
