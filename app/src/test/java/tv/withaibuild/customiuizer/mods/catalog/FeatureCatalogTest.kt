package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
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
import tv.withaibuild.customiuizer.mods.diagnostics.EnabledState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class FeatureCatalogTest {

    private val logMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        logMessages.clear()
        DiagnosticRecorder.clock = { 0L }
        DiagnosticRecorder.logger = { logMessages += it }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
    }

    private fun runtime(
        processName: String,
        prefs: PrefMap<String, Any?> = PrefMap()
    ): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = when (processName) {
            "android" -> newSystemServerParam(classLoader)
            else -> newPackageReadyParam(processName, classLoader)
        }
        return FeatureCatalog.createRuntime(processName, lpparam, classLoader, prefs)
    }

    @Test
    fun processTargetMatchingUsesMatchesNotIdentity() {
        assertTrue(ProcessTarget.SystemUI.matches("com.android.systemui"))
        val systemui = runtime("com.android.systemui")
        assertFalse(FeatureCatalog.installById("statusBarClockTweak", systemui))
    }

    @Test
    fun disabledFeatureDoesNotRecordRequested() {
        val systemui = runtime("com.android.systemui")

        assertFalse(FeatureCatalog.installById("noMoreIcon", systemui))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.NO_MORE_ICON]
        assertNotNull(snapshot)
        assertEquals(EnabledState.DISABLED, snapshot!!.enabled)
        assertEquals(null, snapshot.compatibility)
        assertEquals(null, snapshot.installation)
        assertTrue(logMessages.any { it.contains("DISABLED") })
        assertFalse(logMessages.any { it.contains("REQUESTED") })
    }

    @Test
    fun requestedCompatibleAndInstalledAreLoggedOnTransition() {
        val server = runtime("android")

        assertTrue(FeatureCatalog.installById("packagePermissions", server))

        val logStates = logMessages.map { extractState(it) }
        assertEquals(listOf("REQUESTED", "COMPATIBLE", "INSTALLED"), logStates)

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.PACKAGE_PERMISSIONS]
        assertEquals(InstallOutcome.INSTALLED, snapshot!!.installation)
    }

    @Test
    fun primaryCompatibilityIsRecordedAndInstalled() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_batteryindicator"] = true
        val systemui = runtime("com.android.systemui", prefs)

        assertTrue(FeatureCatalog.installById("batteryIndicator", systemui))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.BATTERY_INDICATOR]
        assertEquals(CompatibilityState.COMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, snapshot.installation)
        assertTrue(logMessages.any { it.contains("PRIMARY_TARGET_FOUND") })
        assertTrue(logMessages.any { it.contains("INSTALLED") })
    }

    @Test
    fun primaryCompatibilityRecordsInstalledForStatusBarClockTweak() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        prefs["pref_key_system_cc_hidedate"] = true
        val systemui = runtime("com.android.systemui", prefs)

        assertTrue(FeatureCatalog.installById("statusBarClockTweak", systemui))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertEquals(CompatibilityState.COMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, snapshot.installation)
        assertTrue(logMessages.any { it.contains("PRIMARY_TARGET_FOUND") })
    }

    @Test
    fun incompatibleSkipsInstallerAndRecordsFailed() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_autobrightness"] = true

        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = ClassLoader.getSystemClassLoader().parent
        val lpparam = newSystemServerParam(classLoader)
        val server = FeatureCatalog.createRuntime("android", lpparam, classLoader, prefs)

        assertFalse(FeatureCatalog.installById("autoBrightnessRange", server))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.AUTO_BRIGHTNESS_RANGE]
        assertEquals(CompatibilityState.INCOMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.FAILED, snapshot.installation)
    }

    @Test
    fun installerExceptionIsRecordedAsFailedAndDoesNotEscape() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_vibration_amp"] = true

        val throwingLpparam = Proxy.newProxyInstance(
            SystemServerStartingParam::class.java.classLoader,
            arrayOf(SystemServerStartingParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getClassLoader" -> throw RuntimeException("boom")
                else -> null
            }
        } as SystemServerStartingParam

        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val server = FeatureRuntime("android", throwingLpparam, classLoader, prefs)

        assertFalse(FeatureCatalog.installById("muffledVibration", server))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.MUFFLED_VIBRATION]
        assertEquals(CompatibilityState.COMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.FAILED, snapshot.installation)
        assertTrue(logMessages.any { it.contains("INSTALLER_FAILED") })
    }

    @Test
    fun failedFeatureDoesNotBlockSubsequentInstallByIdCalls() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_vibration_amp"] = true

        // First: a failing runtime for muffledVibration.
        val throwingLpparam = Proxy.newProxyInstance(
            SystemServerStartingParam::class.java.classLoader,
            arrayOf(SystemServerStartingParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getClassLoader" -> throw RuntimeException("boom")
                else -> null
            }
        } as SystemServerStartingParam
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val server = FeatureRuntime("android", throwingLpparam, classLoader, prefs)
        assertFalse(FeatureCatalog.installById("muffledVibration", server))

        // Second: a fresh, working runtime for packagePermissions must still install.
        val goodServer = runtime("android")
        assertTrue(FeatureCatalog.installById("packagePermissions", goodServer))

        assertEquals(InstallOutcome.FAILED, DiagnosticRecorder.summarize()[DiagnosticIds.MUFFLED_VIBRATION]!!.installation)
        assertEquals(InstallOutcome.INSTALLED, DiagnosticRecorder.summarize()[DiagnosticIds.PACKAGE_PERMISSIONS]!!.installation)
    }

    @Test
    fun launcherCanaryAreCompatibleAndInstalled() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_launcher_noclockhide"] = true
        prefs["pref_key_launcher_nowidgetonly"] = true
        val launcher = runtime("com.miui.home", prefs)

        assertTrue(FeatureCatalog.installById("noClockHide", launcher))
        assertTrue(FeatureCatalog.installById("noWidgetOnly", launcher))

        val noClockHide = DiagnosticRecorder.summarize()[DiagnosticIds.NO_CLOCK_HIDE]
        assertEquals(CompatibilityState.COMPATIBLE, noClockHide!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, noClockHide.installation)
    }

    @Test
    fun installByIdReturnsFalseForUnknownFeature() {
        val systemui = runtime("com.android.systemui")
        assertFalse(FeatureCatalog.installById("unknown", systemui))
    }

    @Test
    fun allCatalogFeaturesAreRegistered() {
        val ids = setOf(
            "packagePermissions",
            "statusBarClockTweak",
            "autoBrightnessRange",
            "muffledVibration",
            "noMoreIcon",
            "batteryIndicator",
            "noClockHide",
            "noWidgetOnly",
            "screenDimTime",
            "firstVolumePress",
            "networkIndicatorWifi",
            "muteVisibleNotifications",
            "hideLauncherTitles",
            "fixAppInfoLaunch",
            "hideProximityWarning",
            "clearAllTasks",
            "hideDismissView",
            "hideLockScreenHint",
            "folderColumns",
            "titleTopMargin",
            "noLightUpOnCharge",
            "allRotations",
            "noNetworkSpeedSeparator",
            "hideIconsClock",
            "noUnlockAnimation",
            "maxHotseatIconsCount"
        )
        assertEquals(ids, FeatureCatalog.specs().map { it.id }.toSet())
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

    private fun newSystemServerParam(classLoader: ClassLoader): SystemServerStartingParam {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            SystemServerStartingParam::class.java.classLoader,
            arrayOf(SystemServerStartingParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getClassLoader" -> classLoader
                "getProcessName" -> "android"
                else -> null
            }
        } as SystemServerStartingParam
    }

    private fun extractState(log: String): String {
        return log.substringAfter("] ").substringBefore(" ")
    }
}
