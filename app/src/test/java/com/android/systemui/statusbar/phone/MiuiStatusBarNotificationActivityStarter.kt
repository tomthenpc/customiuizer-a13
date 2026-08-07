package com.android.systemui.statusbar.phone

import android.app.PendingIntent
import android.content.Intent
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow

class MiuiStatusBarNotificationActivityStarter {
    fun startNotificationIntent(
        pendingIntent: PendingIntent,
        fillInIntent: Intent?,
        entry: NotificationEntry,
        row: Any?,
        animate: Boolean,
        isActivityIntent: Boolean
    ) {}

    fun startNotificationIntent(
        pendingIntent: PendingIntent,
        fillInIntent: Intent?,
        row: ExpandableNotificationRow,
        animate: Boolean
    ) {}

    // Overload without an mSbn field; used to verify fail-open behavior.
    fun startNotificationIntent(
        pendingIntent: PendingIntent,
        fillInIntent: Intent?,
        rawTarget: String,
        animate: Boolean
    ) {}
}
