package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticState
import tv.withaibuild.customiuizer.mods.utils.HookTargetResolver
import tv.withaibuild.customiuizer.utils.PrefMap

class FeatureCatalogTest {

    @Test
    fun catalogContainsRepresentativeFeatures() {
        val specs = FeatureCatalog.specs()
        assertTrue(specs.any { it.id == "packagePermissions" })
        assertTrue(specs.any { it.id == "statusBarClockTweak" })
    }

    @Test
    fun packagePermissionsIsSystemServerScoped() {
        val feature = FeatureCatalog.specs().find { it.id == "packagePermissions" }
        assertNotNull(feature)
        assertEquals(ProcessTarget.SystemServer, feature?.processTarget)
        assertEquals(RestartTarget.REBOOT, feature?.restartTarget)
        assertEquals(DiagnosticIds.PACKAGE_PERMISSIONS, feature?.diagnosticId)
    }

    @Test
    fun statusBarClockTweakIsSystemUIScoped() {
        val feature = FeatureCatalog.specs().find { it.id == "statusBarClockTweak" }
        assertNotNull(feature)
        assertEquals(ProcessTarget.Package("com.android.systemui"), feature?.processTarget)
        assertEquals(RestartTarget.SYSTEMUI_RESTART, feature?.restartTarget)
        assertEquals(DiagnosticIds.STATUSBAR_CLOCK_TWEAK, feature?.diagnosticId)
        assertTrue(feature?.hotReloadable == true)
    }

    @Test
    fun statusBarClockTweakConditionMatchesOriginal() {
        val feature = FeatureCatalog.specs().find { it.id == "statusBarClockTweak" }!!
        val prefs = PrefMap<String, Any?>()

        assertFalse(feature.condition(prefs))

        prefs["pref_key_system_statusbar_clocktweak"] = true
        assertTrue(feature.condition(prefs))

        prefs.clear()
        prefs["pref_key_system_cc_clocktweak"] = true
        assertTrue(feature.condition(prefs))

        prefs.clear()
        prefs["pref_key_system_cc_hidedate"] = true
        assertTrue(feature.condition(prefs))

        prefs.clear()
        prefs["pref_key_system_cc_dateformat"] = "MM/dd EEEE"
        assertTrue(feature.condition(prefs))
    }

    @Test
    fun allFeatureIdsAreStableAndUnique() {
        val ids = FeatureCatalog.specs().map { it.id }
        val diagnosticIds = FeatureCatalog.specs().map { it.diagnosticId }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(diagnosticIds.size, diagnosticIds.toSet().size)
    }

    @Test
    fun preferenceKeysAreCoveredBySchema() {
        val feature = FeatureCatalog.specs().find { it.id == "statusBarClockTweak" }!!
        assertEquals(
            setOf(
                "system_statusbar_clocktweak",
                "system_cc_clocktweak",
                "system_cc_hidedate",
                "system_cc_dateformat"
            ),
            feature.preferenceKeys
        )
    }

    @Test
    fun statusBarClockTweakCompatibilityProbeWorksWithMissingClass() {
        val feature = FeatureCatalog.specs().find { it.id == "statusBarClockTweak" }!!
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_statusbar_clocktweak"] = true

        val logs = mutableListOf<String>()
        DiagnosticRecorder.reset()
        DiagnosticRecorder.logger = { logs += it }
        DiagnosticRecorder.clock = { 0L }

        // Use a ClassLoader that cannot find the SystemUI class.
        val classLoader = ClassLoader.getSystemClassLoader()
        val resolver = HookTargetResolver(classLoader)
        val runtime = FeatureRuntime(
            processName = "com.android.systemui",
            lpparam = Any(),
            classLoader = classLoader,
            resolver = resolver,
            prefs = prefs
        )

        val compat = feature.compatibilityCheck(runtime)
        assertEquals(CompatibilityState.INCOMPATIBLE, compat)

        val resolution = resolver.lastResolution(DiagnosticIds.STATUSBAR_CLOCK_TWEAK)
        assertNotNull(resolution)
        assertTrue(resolution!!.failures.isNotEmpty())
    }

    @Test
    fun closedLoopInstallDoesNotRunInstallerWhenIncompatible() {
        val feature = FeatureCatalog.specs().find { it.id == "statusBarClockTweak" }!!
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_statusbar_clocktweak"] = true

        val logs = mutableListOf<String>()
        DiagnosticRecorder.reset()
        DiagnosticRecorder.logger = { logs += it }
        DiagnosticRecorder.clock = { 0L }

        var installed = false
        val testFeature = feature.copy(installer = { installed = true })

        val classLoader = ClassLoader.getSystemClassLoader()
        val resolver = HookTargetResolver(classLoader)
        val runtime = FeatureRuntime(
            processName = "com.android.systemui",
            lpparam = Any(),
            classLoader = classLoader,
            resolver = resolver,
            prefs = prefs
        )

        // Simulate what FeatureCatalog.install() would do for an incompatible target.
        val compat = testFeature.compatibilityCheck(runtime)
        if (compat == CompatibilityState.INCOMPATIBLE) {
            DiagnosticRecorder.record(
                testFeature.diagnosticId,
                DiagnosticState.FAILED,
                reason = "incompatible"
            )
        } else {
            testFeature.installer(runtime)
        }

        assertFalse(installed)
        assertEquals(DiagnosticState.FAILED, DiagnosticRecorder.summarize()[testFeature.diagnosticId]?.state)
    }
}
