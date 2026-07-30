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
import tv.withaibuild.customiuizer.mods.diagnostics.EnabledState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class CatalogBatch2Test {

    private val logs = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        logs.clear()
        DiagnosticRecorder.clock = { 0L }
        DiagnosticRecorder.logger = { logs += it }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
    }

    private fun serverRuntime(prefs: PrefMap<String, Any?>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = newSystemServerParam(classLoader)
        return FeatureCatalog.createRuntime("android", lpparam, classLoader, prefs)
    }

    private fun systemuiRuntime(prefs: PrefMap<String, Any?>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = newPackageReadyParam("com.android.systemui", classLoader)
        return FeatureCatalog.createRuntime("com.android.systemui", lpparam, classLoader, prefs)
    }

    private fun launcherRuntime(prefs: PrefMap<String, Any?>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = newPackageReadyParam("com.miui.home", classLoader)
        return FeatureCatalog.createRuntime("com.miui.home", lpparam, classLoader, prefs)
    }

    @Test
    fun hideProximityWarning_disabled() {
        val server = serverRuntime(PrefMap())

        assertFalse(FeatureCatalog.installById("hideProximityWarning", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_PROXIMITY_WARNING]
        assertNotNull(summary)
        assertEquals(EnabledState.DISABLED, summary!!.enabled)
    }

    @Test
    fun hideProximityWarning_installed() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_hideproxywarn"] = true
        val server = serverRuntime(prefs)

        assertTrue(FeatureCatalog.installById("hideProximityWarning", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_PROXIMITY_WARNING]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun clearAllTasks_installed() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_clearalltasks"] = true
        val server = serverRuntime(prefs)

        assertTrue(FeatureCatalog.installById("clearAllTasks", server))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.CLEAR_ALL_TASKS]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun hideDismissView_installed() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_removedismiss"] = true
        val systemui = systemuiRuntime(prefs)

        assertTrue(FeatureCatalog.installById("hideDismissView", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_DISMISS_VIEW]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun hideLockScreenHint_installed() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_system_hidelshint"] = true
        val systemui = systemuiRuntime(prefs)

        assertTrue(FeatureCatalog.installById("hideLockScreenHint", systemui))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HIDE_LOCK_SCREEN_HINT]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun folderColumns_installed() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_launcher_folder_cols"] = 4
        val launcher = launcherRuntime(prefs)

        assertTrue(FeatureCatalog.installById("folderColumns", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.FOLDER_COLUMNS]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun titleTopMargin_installed() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_launcher_titletopmargin"] = 20
        val launcher = launcherRuntime(prefs)

        assertTrue(FeatureCatalog.installById("titleTopMargin", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.TITLE_TOP_MARGIN]
        assertNotNull(summary)
        assertEquals(CompatibilityState.COMPATIBLE, summary!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, summary.installation)
    }

    @Test
    fun folderColumns_incompatibleWithSystemClassLoader() {
        val prefs = PrefMap<String, Any?>()
        prefs["pref_key_launcher_folder_cols"] = 4

        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = ClassLoader.getSystemClassLoader().parent
        val lpparam = newPackageReadyParam("com.miui.home", classLoader)
        val launcher = FeatureCatalog.createRuntime("com.miui.home", lpparam, classLoader, prefs)

        assertFalse(FeatureCatalog.installById("folderColumns", launcher))

        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.FOLDER_COLUMNS]
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
