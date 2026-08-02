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
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class CatalogBatch3Test {

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
    fun noLightUpOnCharge_disabled() {
        val server = serverRuntime(PrefMap())

        assertFalse(FeatureDispatcher.installById("noLightUpOnCharge", server))

        assertFalse(server.isResolverInitialized())
        assertTrue(DiagnosticRecorder.summarize().isEmpty())
    }

    @Test
    fun noLightUpOnCharge_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_nolightuponcharges"] = "2"
        val server = serverRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("noLightUpOnCharge", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.NO_LIGHT_UP_ON_CHARGE]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun allRotations_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_allrotations2"] = "2"
        val server = serverRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("allRotations", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.ALL_ROTATIONS]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun noNetworkSpeedSeparator_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_nonetspeedseparator"] = true
        val systemui = systemuiRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("noNetworkSpeedSeparator", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun hideIconsClock_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbaricons_clock"] = true
        val systemui = systemuiRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("hideIconsClock", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_ICONS_CLOCK]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun noUnlockAnimation_installed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_launcher_nounlockanim"] = true
        val launcher = launcherRuntime(prefs)

        assertTrue(FeatureDispatcher.installById("noUnlockAnimation", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.NO_UNLOCK_ANIMATION]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun hideIconsClock_incompatibleWithSystemClassLoader() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbaricons_clock"] = true

        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = ClassLoader.getSystemClassLoader().parent
        val lpparam = newPackageReadyParam("com.android.systemui", classLoader)
        val systemui = FeatureDispatcher.createRuntime("com.android.systemui", lpparam, classLoader, prefs)

        assertFalse(FeatureDispatcher.installById("hideIconsClock", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_ICONS_CLOCK]
        assertNotNull(summary)
        assertEquals(CompatibilityState.INCOMPATIBLE, summary!!.compatibility)
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
