package com.android.systemui.statusbar.notification.row

/**
 * Fake implementation of the HyperOS/MIUI14 NotificationGuts outer class.
 *
 * The only required inner member for the notification-row menu fixture is
 * `GutsContent`, which appears as the fourth constructor parameter of the real
 * `MiuiNotificationMenuRow$MiuiNotificationMenuItem` on HyperOS veux.
 */
open class NotificationGuts {
    open class GutsContent
}
