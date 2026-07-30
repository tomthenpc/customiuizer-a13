package tv.withaibuild.customiuizer.utils

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * A no-op [XposedInterface] suitable for unit tests.
 *
 * The library's [XposedHelpers.moduleInst] must be non-null for
 * [XposedHelpers.doHookMethod] to avoid NPE. This fake returns proxy
 * builders/handles so the catalog can record that a hook was installed
 * without requiring a real Xposed runtime.
 */
object FakeXposedInterface {

    fun create(): XposedInterface {
        val classLoader = FakeXposedInterface::class.java.classLoader!!
        return Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedInterface::class.java),
            RootHandler(classLoader)
        ) as XposedInterface
    }

    private class RootHandler(private val classLoader: ClassLoader) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return when (method.name) {
                "hook", "hookClassInitializer" -> BuilderProxy.create(classLoader)
                "getApiVersion" -> 102
                "getFrameworkName" -> "fake"
                "getFrameworkVersion" -> "0"
                "getFrameworkVersionCode" -> 0L
                "getFrameworkProperties" -> 0L
                "deoptimize" -> true
                "getInvoker" -> null
                "log" -> null
                "getModuleApplicationInfo" -> null
                "getRemotePreferences" -> null
                "listRemoteFiles" -> emptyArray<String>()
                "openRemoteFile" -> null
                else -> null
            }
        }
    }

    private object BuilderHandler : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return when (method.name) {
                "setPriority", "setExceptionMode", "setId" -> proxy
                "intercept" -> HookHandleProxy.create(proxy.javaClass.classLoader!!)
                else -> null
            }
        }
    }

    private object BuilderProxy {
        fun create(classLoader: ClassLoader): XposedInterface.HookBuilder {
            return Proxy.newProxyInstance(
                classLoader,
                arrayOf(XposedInterface.HookBuilder::class.java),
                BuilderHandler
            ) as XposedInterface.HookBuilder
        }
    }

    private object HookHandleHandler : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return when (method.name) {
                "getExecutable" -> null
                "unhook" -> null
                "getId" -> "fake"
                "replaceHook" -> proxy
                else -> null
            }
        }
    }

    private object HookHandleProxy {
        fun create(classLoader: ClassLoader): XposedInterface.HookHandle {
            return Proxy.newProxyInstance(
                classLoader,
                arrayOf(XposedInterface.HookHandle::class.java),
                HookHandleHandler
            ) as XposedInterface.HookHandle
        }
    }
}
