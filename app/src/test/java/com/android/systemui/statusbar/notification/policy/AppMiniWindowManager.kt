package com.android.systemui.statusbar.notification.policy

import android.app.PendingIntent

class AppMiniWindowManager private constructor() {

    val calls = mutableListOf<Pair<String, PendingIntent?>>()

    fun launchMiniWindowActivity(pkgName: String, pendingIntent: PendingIntent?) {
        calls.add(pkgName to pendingIntent)
    }

    companion object {
        private val instance = AppMiniWindowManager()

        @JvmStatic
        fun getInstance(): AppMiniWindowManager = instance
    }
}
