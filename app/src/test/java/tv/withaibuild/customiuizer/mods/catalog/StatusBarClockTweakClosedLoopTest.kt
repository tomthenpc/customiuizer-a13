package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticState
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class StatusBarClockTweakClosedLoopTest {

    private val logs = mutableListOf<String>()
    private var now = 0L

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        logs.clear()
        now = 0L
        DiagnosticRecorder.clock = { now }
        DiagnosticRecorder.logger = { logs += it }
    }

    @Test
    fun fullLoop_recordsRequestedThenFailedWhenTargetMissing() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>

        val classLoader = ClassLoader.getSystemClassLoader()
        val lpparam = newPackageReadyParam("com.android.systemui", classLoader)

        FeatureCatalog.installForPackage(lpparam, "com.android.systemui")

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertNotNull(summary)
        assertEquals(DiagnosticState.FAILED, summary!!.state)

        // The state flow must include REQUESTED and FAILED, and must not reach INSTALLED.
        assertTrue(logs.any { it.contains("REQUESTED") })
        assertTrue(logs.any { it.contains("FAILED") })
        assertFalse(logs.any { it.contains("INSTALLED") })
    }

    @Test
    fun fullLoop_skipsInstallerWhenTargetMissing() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>

        val classLoader = ClassLoader.getSystemClassLoader()
        val lpparam = newPackageReadyParam("com.android.systemui", classLoader)

        // The real installer must not throw or run; the function returns cleanly.
        FeatureCatalog.installForPackage(lpparam, "com.android.systemui")
    }

    @Test
    fun fullLoop_preferenceConditionFalse_recordsOnlyRequested() {
        val prefs = PrefMap<String, Any?>()
        // No statusbar clock tweak key set.
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>

        val classLoader = ClassLoader.getSystemClassLoader()
        val lpparam = newPackageReadyParam("com.android.systemui", classLoader)

        FeatureCatalog.installForPackage(lpparam, "com.android.systemui")

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        // REQUESTED is recorded; condition is false, so no further state.
        assertNotNull(summary)
        assertEquals(DiagnosticState.REQUESTED, summary!!.state)
        assertEquals(1L, summary.count)
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
