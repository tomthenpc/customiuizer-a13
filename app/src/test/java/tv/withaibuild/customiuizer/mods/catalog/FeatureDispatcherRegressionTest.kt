package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class FeatureDispatcherRegressionTest {

    private val logs = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clearStatesForTesting()
        logs.clear()
        DiagnosticRecorder.clock = { 0L }
        DiagnosticRecorder.logger = { logs += it }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
        DiagnosticRecorder.logger = null
        MainModule.mPrefs = PrefMap()
        XposedHelpers.moduleInst = null
    }

    private fun systemuiRuntime(prefs: PrefMap<String, Any>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = newPackageReadyParam("com.android.systemui", classLoader)
        return FeatureDispatcher.createRuntime("com.android.systemui", lpparam, classLoader, prefs)
    }

    @Test
    fun diagnosticLoggerRuntimeExceptionDoesNotBlockInstallById() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_batteryindicator"] = true
        val systemui = systemuiRuntime(prefs)

        DiagnosticRecorder.logger = { throw RuntimeException("logger boom") }

        assertTrue(FeatureDispatcher.installById("batteryIndicator", systemui))

        assertTrue(systemui.isResolverInitialized())

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.BATTERY_INDICATOR]
        assertNotNull(snapshot)
    }

    @Test
    fun diagnosticLoggerOomIsRethrownByInstallById() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_batteryindicator"] = true
        val systemui = systemuiRuntime(prefs)

        DiagnosticRecorder.logger = { throw OutOfMemoryError("logger oom") }

        try {
            FeatureDispatcher.installById("batteryIndicator", systemui)
            assertTrue("expected OOM", false)
        } catch (oom: OutOfMemoryError) {
            // expected
        }
    }

    private fun newPackageReadyParam(packageName: String, classLoader: ClassLoader): PackageReadyParam {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            PackageReadyParam::class.java.classLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> packageName
                "getClassLoader" -> classLoader
                "isFirstPackage" -> true
                "getProcessName" -> packageName
                else -> null
            }
        } as PackageReadyParam
    }
}
