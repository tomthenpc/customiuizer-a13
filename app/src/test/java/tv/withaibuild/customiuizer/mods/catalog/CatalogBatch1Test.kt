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
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder

import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class CatalogBatch1Test {

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

    private fun serverRuntime(prefs: PrefMap<String, Any>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = newSystemServerParam(classLoader)
        return FeatureDispatcher.createRuntime("android", lpparam, classLoader, prefs)
    }

    private fun systemuiRuntime(prefs: PrefMap<String, Any>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = newPackageReadyParam("com.android.systemui", classLoader)
        return FeatureDispatcher.createRuntime("com.android.systemui", lpparam, classLoader, prefs)
    }

    private fun launcherRuntime(
        prefs: PrefMap<String, Any>,
        packageName: String = "com.miui.home"
    ): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = newPackageReadyParam(packageName, classLoader)
        return FeatureDispatcher.createRuntime(packageName, lpparam, classLoader, prefs)
    }

    @Test
    fun screenDimTime_disabled() {
        val server = serverRuntime(PrefMap())

        assertFalse(FeatureDispatcher.installById("screenDimTime", server))

        assertFalse(server.isResolverInitialized())
        assertTrue(DiagnosticRecorder.summarize().isEmpty())
    }

    @Test
    fun screenDimTime_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_dimtime"] = 40
        val server = serverRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("screenDimTime", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.SCREEN_DIM_TIME]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun firstVolumePress_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_firstpress"] = true
        val server = serverRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("firstVolumePress", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.FIRST_VOLUME_PRESS]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun networkIndicatorWifi_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_networkindicator_wifi"] = true
        val systemui = systemuiRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("networkIndicatorWifi", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.NETWORK_INDICATOR_WIFI]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun muteVisibleNotifications_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_mutevisiblenotif"] = true
        val systemui = systemuiRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("muteVisibleNotifications", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun hideLauncherTitles_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_launcher_hidetitles"] = true
        val launcher = launcherRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("hideLauncherTitles", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_LAUNCHER_TITLES]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun fixAppInfoLaunch_primaryInstalled() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_launcher_fixlaunch"] = true
        val launcher = launcherRuntime(prefs, packageName = "com.miui.home")

        assertTrue(FeatureDispatcher.installById("fixAppInfoLaunch", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.FIX_APP_INFO_LAUNCH]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
        assertFalse(summary.installSummary?.fallbackUsed ?: true)
    }

    @Test
    fun fixAppInfoLaunch_fallbackInstalled() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_launcher_fixlaunch"] = true
        val launcher = launcherRuntime(prefs, packageName = "com.mi.android.globallauncher")

        assertTrue(FeatureDispatcher.installById("fixAppInfoLaunch", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.FIX_APP_INFO_LAUNCH]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.DEGRADED, summary.installation)
        assertTrue(summary.installSummary?.fallbackUsed ?: false)
    }

    @Test
    fun screenDimTime_incompatibleWithSystemClassLoader() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_dimtime"] = 40

        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = ClassLoader.getSystemClassLoader().parent
        val lpparam = newSystemServerParam(classLoader)
        val server = FeatureDispatcher.createRuntime("android", lpparam, classLoader, prefs)

        assertFalse(FeatureDispatcher.installById("screenDimTime", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.SCREEN_DIM_TIME]
        assertNotNull(summary)
        assertEquals(CompatibilityState.INCOMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.FAILED, summary.installation)
    }

    @Test
    fun installerExceptionIsRecordedAsFailed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_launcher_hidetitles"] = true

        val throwingLpparam = Proxy.newProxyInstance(
            PackageReadyParam::class.java.classLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getClassLoader" -> throw RuntimeException("boom")
                "getPackageName" -> "com.miui.home"
                else -> null
            }
        } as PackageReadyParam

        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val launcher = FeatureDispatcher.createRuntime("com.miui.home", throwingLpparam, classLoader, prefs)

        assertFalse(FeatureDispatcher.installById("hideLauncherTitles", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_LAUNCHER_TITLES]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.FAILED, summary.installation)
    }

    private fun newSystemServerParam(classLoader: ClassLoader): SystemServerStartingParam {
        return Proxy.newProxyInstance(
            SystemServerStartingParam::class.java.classLoader,
            arrayOf(SystemServerStartingParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getClassLoader" -> classLoader
                else -> null
            }
        } as SystemServerStartingParam
    }

    private fun newPackageReadyParam(packageName: String, classLoader: ClassLoader): PackageReadyParam {
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
