package com.miui.systemui

import android.content.Context

/** Minimal ABI stub so CloudData still installs when NotificationSettingsManager is missing. */
class NotificationCloudData {
    companion object {
        @JvmStatic
        fun getFloatBlacklist(@Suppress("UNUSED_PARAMETER") context: Context): ArrayList<String> {
            return ArrayList()
        }
    }
}
