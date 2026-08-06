package tv.withaibuild.customiuizer.utils

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
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

    data class RecordedHook(
        val executable: java.lang.reflect.Executable,
        val hook: tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
    )

    @JvmField
    val recordedHooks = mutableListOf<RecordedHook>()

    @JvmField
    val interceptedExecutables = mutableListOf<java.lang.reflect.Executable>()

    @JvmField
    var remotePreferences: SharedPreferences? = null

    @JvmStatic
    fun reset() {
        recordedHooks.clear()
        interceptedExecutables.clear()
        remotePreferences = null
    }

    /** Find the recorded hook for a given declaring class and method name. */
    @JvmStatic
    fun findHook(declaringClassName: String, methodName: String): RecordedHook? {
        return recordedHooks.find { record ->
            val member = record.executable
            member.declaringClass?.name == declaringClassName && member.name == methodName
        }
    }

    /**
     * Manually invoke the after-callback of a recorded hook with a fake Xposed
     * [XposedInterface.Chain]. This lets unit tests exercise the `isHooked`
     * gating in SystemUiInstaller without a real runtime.
     */
    @JvmStatic
    fun executeAfter(recorded: RecordedHook, thisObject: Any?) {
        val executable = recorded.executable
        val chain = Proxy.newProxyInstance(
            executable.declaringClass.classLoader,
            arrayOf(XposedInterface.Chain::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getExecutable" -> executable
                "getThisObject" -> thisObject
                "getArgs" -> emptyList<Any?>()
                "proceed" -> null
                else -> null
            }
        } as XposedInterface.Chain

        val beforeCtor = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredConstructor(XposedInterface.Chain::class.java)
        beforeCtor.isAccessible = true
        val before = beforeCtor.newInstance(chain)

        val afterCtor = HookerClassHelper.AfterHookCallback::class.java.getDeclaredConstructor(
            HookerClassHelper.BeforeHookCallback::class.java,
            Object::class.java,
            Throwable::class.java
        )
        afterCtor.isAccessible = true
        val after = afterCtor.newInstance(before, null, null)

        recorded.hook.afterHook(after)
    }

    private class RootHandler(private val classLoader: ClassLoader) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return when (method.name) {
                "hook", "hookClassInitializer" -> {
                    val executable = args?.firstOrNull() as? java.lang.reflect.Executable
                    if (executable != null) interceptedExecutables.add(executable)
                    BuilderProxy.create(classLoader)
                }
                "getApiVersion" -> 102
                "getFrameworkName" -> "fake"
                "getFrameworkVersion" -> "0"
                "getFrameworkVersionCode" -> 0L
                "getFrameworkProperties" -> 0L
                "deoptimize" -> true
                "getInvoker" -> null
                "log" -> null
                "getModuleApplicationInfo" -> null
                "getRemotePreferences" -> remotePreferences
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
                "intercept" -> {
                    val executable = interceptedExecutables.removeLastOrNull()
                    val hook = args?.firstOrNull() as? tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
                    if (executable != null && hook != null) {
                        recordedHooks.add(RecordedHook(executable, hook))
                    }
                    HookHandleProxy.create(proxy.javaClass.classLoader!!)
                }
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
