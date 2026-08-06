package com.android.systemui

class Dependency {
    companion object {
        @JvmField
        var instances: MutableMap<Class<*>, Any> = mutableMapOf()

        @JvmStatic
        fun get(clazz: Class<*>): Any? = instances[clazz]

        @JvmStatic
        fun setMock(clazz: Class<*>, instance: Any) {
            instances[clazz] = instance
        }

        @JvmStatic
        fun clear() {
            instances.clear()
        }
    }
}
