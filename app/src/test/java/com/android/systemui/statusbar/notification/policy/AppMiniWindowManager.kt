package com.android.systemui.statusbar.notification.policy

import android.app.PendingIntent

class AppMiniWindowManager private constructor() {

    val calls = mutableListOf<Pair<String, PendingIntent?>>()
    var throwOnCall: Throwable? = null
    var sideEffectThenThrow: Throwable? = null

    fun launchMiniWindowActivity(pkgName: String, pendingIntent: PendingIntent?) {
        throwOnCall?.let { throw it }
        calls.add(pkgName to pendingIntent)
        sideEffectThenThrow?.let { throw it }
    }

    fun reset() {
        calls.clear()
        throwOnCall = null
        sideEffectThenThrow = null
    }

    companion object {
        private val instance = AppMiniWindowManager()

        @JvmStatic
        fun getInstance(): AppMiniWindowManager = instance
    }
}
