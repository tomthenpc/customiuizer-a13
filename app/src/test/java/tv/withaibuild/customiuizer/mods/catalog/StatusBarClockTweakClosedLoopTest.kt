package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class StatusBarClockTweakClosedLoopTest {

    private val logs = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clear()
        logs.clear()
        DiagnosticRecorder.clock = { 0L }
        DiagnosticRecorder.logger = { logs += it }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
    }

    private fun runtime(prefs: PrefMap<String, Any>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = Proxy.newProxyInstance(
            PackageReadyParam::class.java.classLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.android.systemui"
                "getClassLoader" -> classLoader
                "isFirstPackage" -> true
                "getProcessName" -> "com.android.systemui"
                else -> null
            }
        } as PackageReadyParam
        return FeatureDispatcher.createRuntime("com.android.systemui", lpparam, classLoader, prefs)
    }

    @Test
    fun fullLoop_primaryTargetsFoundAndInstalled() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        prefs["pref_key_system_cc_hidedate"] = true

        val systemui = runtime(prefs)

        assertTrue(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
        assertTrue(logs.any { it.contains("REQUESTED") })
        assertTrue(logs.any { it.contains("PRIMARY_TARGET_FOUND") })
        assertTrue(logs.any { it.contains("INSTALLED") })
    }

    @Test
    fun fullLoop_dateOnlyInstallsOnlyDateTargets() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_cc_hidedate"] = true

        val systemui = runtime(prefs)

        assertTrue(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
        assertEquals(2, summary.installSummary!!.requiredInstalled)
        assertEquals(2, summary.installSummary!!.requiredTotal)
        assertEquals(1, summary.installSummary!!.optionalInstalled)
        assertEquals(1, summary.installSummary!!.optionalTotal)
    }

    @Test
    fun fullLoop_customDateFormatDoesNotExpectVisibilityTarget() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_cc_dateformat"] = "yyyy-MM-dd"

        val systemui = runtime(prefs)

        assertTrue(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
        assertEquals(2, summary.installSummary!!.requiredInstalled)
        assertEquals(2, summary.installSummary!!.requiredTotal)
        assertEquals(0, summary.installSummary!!.optionalTotal)
    }

    @Test
    fun fullLoop_controlCenterClockOnlyInstallsControllerAndClockTargets() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_cc_clocktweak"] = true

        val systemui = runtime(prefs)

        assertTrue(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
        assertEquals(4, summary.installSummary!!.requiredInstalled)
        assertEquals(4, summary.installSummary!!.requiredTotal)
        assertEquals(0, summary.installSummary!!.optionalTotal)
    }

    @Test
    fun fullLoop_statusBarClockOnlyDoesNotExpectDateVisibilityTarget() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true

        val systemui = runtime(prefs)

        assertTrue(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
        assertEquals(5, summary.installSummary!!.requiredInstalled)
        assertEquals(5, summary.installSummary!!.requiredTotal)
        assertEquals(0, summary.installSummary!!.optionalTotal)
    }

    @Test
    fun fullLoop_disabledCreatesNoRuntimeState() {
        val prefs = PrefMap<String, Any>()

        val systemui = runtime(prefs)

        assertFalse(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        assertFalse(systemui.isResolverInitialized())
        assertTrue(DiagnosticRecorder.summarize().isEmpty())
    }

    @Test
    fun fullLoop_incompatibleSkipsInstaller() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true

        val classLoader = ClassLoader.getSystemClassLoader().parent
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val lpparam = Proxy.newProxyInstance(
            PackageReadyParam::class.java.classLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.android.systemui"
                "getClassLoader" -> classLoader
                "isFirstPackage" -> true
                "getProcessName" -> "com.android.systemui"
                else -> null
            }
        } as PackageReadyParam
        val systemui = FeatureDispatcher.createRuntime("com.android.systemui", lpparam, classLoader, prefs)

        assertFalse(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertEquals(CompatibilityState.INCOMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.FAILED, summary.installation)
    }
}
