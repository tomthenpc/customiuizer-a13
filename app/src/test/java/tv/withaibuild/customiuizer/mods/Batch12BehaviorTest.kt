package tv.withaibuild.customiuizer.mods

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher
import tv.withaibuild.customiuizer.mods.catalog.FeatureInstallRegistry
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Behavior tests for catalog expansion batch 12 (continued) screenshot overlay hook.
 */
class Batch12BehaviorTest {

    private val logMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        logMessages.clear()
        DiagnosticRecorder.clock = { 0L }
        DiagnosticRecorder.logger = { line ->
            if (!line.startsWith("Diagnostic[rom.environment]")) logMessages += line
        }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        MainModule.mPrefs = PrefMap()
        XposedHelpers.moduleInst = null
    }

    private fun runtime(prefs: PrefMap<String, Any>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = Proxy.newProxyInstance(
            classLoader,
            arrayOf(io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam::class.java),
            InvocationHandler { _, m, _ ->
                when (m.name) {
                    "getClassLoader" -> classLoader
                    "getProcessName" -> "android"
                    else -> null
                }
            }
        ) as io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
        return FeatureDispatcher.createRuntime("android", lpparam, classLoader, prefs)
    }

    @Test
    fun tempHideOverlayApp_disabledPathDoesNotExecuteInstaller() {
        val prefs = PrefMap<String, Any>()
        val server = runtime(prefs)

        assertFalse("tempHideOverlayApp disabled", FeatureDispatcher.installById("tempHideOverlayApp", server))
    }

    @Test
    fun tempHideOverlayApp_installsWhenEnabled() {
        val prefs = PrefMap<String, Any>()
        prefs["system_screenshot_overlay"] = true
        val server = runtime(prefs)

        assertTrue("tempHideOverlayApp first install", FeatureDispatcher.installById("tempHideOverlayApp", server))
        assertTrue("tempHideOverlayApp second install is AlreadyInstalled", FeatureDispatcher.installById("tempHideOverlayApp", server))
    }

}
