package com.android.systemui

import com.android.systemui.statusbar.notification.policy.AppMiniWindowManager

object Dependency {
    private val mocks = mutableMapOf<Class<*>, Any?>()
    private val explicitNulls = mutableSetOf<Class<*>>()
    var throwOnGet: Throwable? = null

    @JvmStatic
    fun get(cls: Class<*>): Any? {
        throwOnGet?.let { throw it }
        if (explicitNulls.contains(cls)) return null
        return mocks[cls] ?: if (cls == AppMiniWindowManager::class.java) AppMiniWindowManager.getInstance() else null
    }

    @JvmStatic
    fun setMock(cls: Class<*>, instance: Any?) {
        if (instance == null) {
            explicitNulls.add(cls)
        } else {
            explicitNulls.remove(cls)
            mocks[cls] = instance
        }
    }

    @JvmStatic
    fun clear() {
        mocks.clear()
        explicitNulls.clear()
        throwOnGet = null
    }
}
