package tv.withaibuild.customiuizer.mods

import android.view.WindowManager
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
 * Real callback tests for TempHideOverlayAppHook.
 */
class TempHideOverlayAppCallbackTest {

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

    private fun installAndFindHook(prefs: PrefMap<String, Any>): HookerClassHelper.MethodHook {
        val server = runtime(prefs)
        val installed = FeatureDispatcher.installById("tempHideOverlayApp", server)
        assert(installed) { "install should succeed" }
        val recorded = FakeXposedInterface.recordedHooks.filter { it.executable.declaringClass.name == "com.android.server.wm.WindowSurfaceController" }
        assert(recorded.isNotEmpty()) { "at least one WindowSurfaceController constructor hook expected" }
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
    fun shortArgsDoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_screenshot_overlay"] = true
        val hook = installAndFindHook(prefs)
        val callback = fakeCallback(ArrayList())
        hook.beforeHook(callback)
        assertNull("no result for empty args", callback.getThisObject())
    }

    @Test
    fun argsSize2DoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_screenshot_overlay"] = true
        val hook = installAndFindHook(prefs)
        val callback = fakeCallback(arrayListOf(Any(), Any()))
        hook.beforeHook(callback)
        assertEquals("args size 2 stays unchanged", 2, callback.getArgsCount())
    }

    @Test
    fun argsSize4DoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_screenshot_overlay"] = true
        val hook = installAndFindHook(prefs)
        val callback = fakeCallback(arrayListOf(Any(), Any(), 0, Any()))
        hook.beforeHook(callback)
        assertEquals("args size 4 stays unchanged", 4, callback.getArgsCount())
    }

    @Test
    fun nonOverlayWindowTypeLeavesFlags() {
        val prefs = PrefMap<String, Any>()
        prefs["system_screenshot_overlay"] = true
        val hook = installAndFindHook(prefs)
        val args = arrayListOf<Any?>(Any(), Any(), 0, Any(), WindowManager.LayoutParams.TYPE_APPLICATION)
        val callback = fakeCallback(args)
        hook.beforeHook(callback)
        assertEquals("non-overlay type leaves flags", 0, callback.getArgs()[2])
    }

    @Test
    fun overlayWindowTypeSetsSkipFlag() {
        val prefs = PrefMap<String, Any>()
        prefs["system_screenshot_overlay"] = true
        val hook = installAndFindHook(prefs)
        val args = arrayListOf<Any?>(Any(), Any(), 0, Any(), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        val callback = fakeCallback(args)
        hook.beforeHook(callback)
        assertEquals("overlay type sets skip flag", 64, callback.getArgs()[2])
    }

    @Test
    fun wrongTypesDoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_screenshot_overlay"] = true
        val hook = installAndFindHook(prefs)
        val args = arrayListOf<Any?>(Any(), Any(), "not-int", Any(), "not-int")
        val callback = fakeCallback(args)
        hook.beforeHook(callback)
        assertEquals("wrong types leave args", "not-int", callback.getArgs()[2])
    }
}
