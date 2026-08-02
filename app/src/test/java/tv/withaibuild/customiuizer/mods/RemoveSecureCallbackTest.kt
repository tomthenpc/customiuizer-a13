package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Real callback tests for RemoveSecureHook.
 */
class RemoveSecureCallbackTest {

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

    private fun installAndFindHooks(prefs: PrefMap<String, Any>): List<FakeXposedInterface.RecordedHook> {
        val server = runtime(prefs)
        assert(FeatureDispatcher.installById("removeSecure", server)) { "install should succeed" }
        return FakeXposedInterface.recordedHooks.filter { it.executable.declaringClass.name == "com.android.server.wm.WindowSurfaceController" }
    }

    private fun constructorHook(hooks: List<FakeXposedInterface.RecordedHook>) =
        hooks.first { it.executable.name == "com.android.server.wm.WindowSurfaceController" }.hook

    private fun setSecureHook(hooks: List<FakeXposedInterface.RecordedHook>) =
        hooks.first { it.executable.name == "setSecure" }.hook

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
    fun shortConstructorArgsDoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_removesecure"] = true
        val recorded = installAndFindHooks(prefs)
        val constructorHook = constructorHook(recorded)
        val callback = fakeCallback(ArrayList())
        constructorHook.beforeHook(callback)
        assertEquals("empty constructor args stay empty", 0, callback.getArgsCount())
    }

    @Test
    fun constructorClearsSecureFlag() {
        val prefs = PrefMap<String, Any>()
        prefs["system_removesecure"] = true
        val recorded = installAndFindHooks(prefs)
        val constructorHook = constructorHook(recorded)
        val args = arrayListOf<Any?>(Any(), Any(), 0x80 or 0x1, Any(), 0)
        val callback = fakeCallback(args)
        constructorHook.beforeHook(callback)
        assertEquals("secure flag (128) is cleared", 0x1, callback.getArgs()[2])
    }

    @Test
    fun setSecureForcesFalse() {
        val prefs = PrefMap<String, Any>()
        prefs["system_removesecure"] = true
        val recorded = installAndFindHooks(prefs)
        val setSecureHook = setSecureHook(recorded)
        val args = arrayListOf<Any?>(true)
        val callback = fakeCallback(args)
        setSecureHook.beforeHook(callback)
        assertEquals("setSecure arg forced to false", false, callback.getArgs()[0])
    }

    @Test
    fun wrongTypesDoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_removesecure"] = true
        val recorded = installAndFindHooks(prefs)
        val constructorHook = constructorHook(recorded)
        val args = arrayListOf<Any?>(Any(), Any(), "not-int", Any(), "not-int")
        val callback = fakeCallback(args)
        constructorHook.beforeHook(callback)
        assertEquals("wrong types leave args", "not-int", callback.getArgs()[2])
    }
}
