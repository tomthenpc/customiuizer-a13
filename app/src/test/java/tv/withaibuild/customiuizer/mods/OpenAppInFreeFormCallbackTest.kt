package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher
import tv.withaibuild.customiuizer.mods.catalog.FeatureInstallRegistry
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Real callback tests for OpenAppInFreeFormHook.
 */
class OpenAppInFreeFormCallbackTest {

    @Before
    fun setUp() {
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    private fun runtime(prefs: PrefMap<String, Any>): FeatureRuntime {
        MainModule.mPrefs = prefs
        val classLoader = this.javaClass.classLoader!!
        val lpparam = Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedModuleInterface.SystemServerStartingParam::class.java),
            InvocationHandler { _, m, _ ->
                when (m.name) {
                    "getClassLoader" -> classLoader
                    "getProcessName" -> "android"
                    else -> null
                }
            }
        ) as XposedModuleInterface.SystemServerStartingParam
        return FeatureDispatcher.createRuntime("android", lpparam, classLoader, prefs)
    }

    private fun installAndFindExecuteRequestHook(prefs: PrefMap<String, Any>): HookerClassHelper.MethodHook {
        val server = runtime(prefs)
        assert(FeatureDispatcher.installById("openAppInFreeForm", server)) { "install should succeed" }
        val recorded = FakeXposedInterface.recordedHooks.filter {
            it.executable.declaringClass.name == "com.android.server.wm.ActivityStarter" &&
                it.executable.name == "executeRequest"
        }
        assert(recorded.isNotEmpty()) { "at least one ActivityStarter.executeRequest hook expected" }
        return recorded.first().hook
    }

    private fun fakeCallback(args: ArrayList<Any?>): HookerClassHelper.BeforeHookCallback {
        val classLoader = this.javaClass.classLoader!!
        val chain = Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedInterface.Chain::class.java),
            InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getArgs" -> args
                    "getThisObject" -> null
                    "getExecutable" -> null
                    "proceed" -> null
                    else -> null
                }
            }
        ) as XposedInterface.Chain
        val constructor = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredConstructor(XposedInterface.Chain::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(chain)
    }

    @Test
    fun emptyArgsDoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        val hook = installAndFindExecuteRequestHook(prefs)
        val callback = fakeCallback(ArrayList())
        hook.beforeHook(callback)
        assertEquals("empty args stay empty", 0, callback.getArgsCount())
    }

    @Test
    fun nullRequestDoesNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        val hook = installAndFindExecuteRequestHook(prefs)
        val args = arrayListOf<Any?>(null)
        val callback = fakeCallback(args)
        hook.beforeHook(callback)
        assertEquals("null request leaves args", 1, callback.getArgsCount())
    }
}
