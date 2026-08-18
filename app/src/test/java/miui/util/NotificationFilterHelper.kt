package miui.util

import android.content.Context

/** Minimal ABI stub for DisableAnyNotificationHook independent FilterHelper hooks. */
object NotificationFilterHelper {
    @JvmStatic
    fun isNotificationForcedEnabled(pkg: String?): Boolean = false

    @JvmStatic
    fun isNotificationForcedFor(context: Context?, pkg: String?): Boolean = false

    @JvmStatic
    fun canSystemNotificationBeBlocked(pkg: String?): Boolean = false

    @JvmStatic
    fun containNonBlockableChannel(pkg: String?): Boolean = false

    @JvmStatic
    fun getNotificationForcedEnabledList(): Set<String> = emptySet()
}
