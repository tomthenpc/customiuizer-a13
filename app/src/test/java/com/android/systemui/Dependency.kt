package com.android.systemui

import com.android.systemui.statusbar.notification.policy.AppMiniWindowManager

object Dependency {
    private val mocks = mutableMapOf<Class<*>, Any>()

    @JvmStatic
    fun get(cls: Class<*>): Any? =
        mocks[cls] ?: if (cls == AppMiniWindowManager::class.java) AppMiniWindowManager.getInstance() else null

    @JvmStatic
    fun setMock(cls: Class<*>, instance: Any) {
        mocks[cls] = instance
    }

    @JvmStatic
    fun clear() {
        mocks.clear()
    }
}
