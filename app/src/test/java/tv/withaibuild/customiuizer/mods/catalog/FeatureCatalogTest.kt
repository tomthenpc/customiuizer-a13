package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.FeatureInstallResult
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class FeatureCatalogTest {

    private val logMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clearStatesForTesting()
        logMessages.clear()
        DiagnosticRecorder.clock = { 0L }
        // Rom environment records via DiagnosticRecorder; filter it from the install log
        // assertions so tests only observe the feature they are exercising.
        DiagnosticRecorder.logger = { line ->
            if (!line.startsWith("Diagnostic[rom.environment]")) logMessages += line
        }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clearStatesForTesting()
        MainModule.mPrefs = PrefMap()
        XposedHelpers.moduleInst = null
    }

    private fun runtime(
        processName: String,
        prefs: PrefMap<String, Any> = PrefMap()
    ): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = when (processName) {
            "android" -> newSystemServerParam(classLoader)
            else -> newPackageReadyParam(processName, classLoader)
        }
        return FeatureDispatcher.createRuntime(processName, lpparam, classLoader, prefs)
    }

    @Test
    fun processTargetMatchingUsesMatchesNotIdentity() {
        assertTrue(ProcessTarget.SystemUI.matches("com.android.systemui"))
        val systemui = runtime("com.android.systemui")
        assertFalse(FeatureDispatcher.installById("statusBarClockTweak", systemui))
    }

    @Test
    fun packagePermissions_installsWithCorrectScopeAndPhase() {
        val server = runtime("android")

        val result = FeatureInstallRegistry.installById(
            "packagePermissions",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            server
        )

        assertTrue("packagePermissions installs in system_server scope/phase", result.isActive)
    }

    @Test
    fun packagePermissions_rejectsWrongScopeWithoutProbing() {
        val server = runtime("android")

        val result = FeatureInstallRegistry.installById(
            "packagePermissions",
            ProcessScope.SYSTEM_UI,
            InstallPhase.SYSTEM_SERVER_STARTING,
            server
        )

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertFalse(server.isResolverInitialized())
    }

    @Test
    fun packagePermissions_rejectsWrongPhaseWithoutProbing() {
        val server = runtime("android")

        val result = FeatureInstallRegistry.installById(
            "packagePermissions",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.PACKAGE_READY,
            server
        )

        assertTrue(result is FeatureInstallResult.WrongPhase)
        assertFalse(server.isResolverInitialized())
    }

    @Test
    fun packagePermissions_rejectsWrongProcessNameWithoutProbing() {
        val systemui = runtime("com.android.systemui")

        val result = FeatureInstallRegistry.installById(
            "packagePermissions",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            systemui
        )

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertFalse(systemui.isResolverInitialized())
    }

    @Test
    fun statusBarClockTweak_rejectsWrongScopeWithoutProbing() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        val systemui = runtime("com.android.systemui", prefs)

        val result = FeatureInstallRegistry.installById(
            "statusBarClockTweak",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.PACKAGE_READY,
            systemui
        )

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertFalse(systemui.isResolverInitialized())
    }

    @Test
    fun statusBarClockTweak_rejectsWrongPhaseWithoutProbing() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        val systemui = runtime("com.android.systemui", prefs)

        val result = FeatureInstallRegistry.installById(
            "statusBarClockTweak",
            ProcessScope.SYSTEM_UI,
            InstallPhase.SYSTEM_SERVER_STARTING,
            systemui
        )

        assertTrue(result is FeatureInstallResult.WrongPhase)
        assertFalse(systemui.isResolverInitialized())
    }

    @Test
    fun statusBarClockTweak_rejectsWrongProcessNameWithoutProbing() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        val server = runtime("android", prefs)

        val result = FeatureInstallRegistry.installById(
            "statusBarClockTweak",
            ProcessScope.SYSTEM_UI,
            InstallPhase.PACKAGE_READY,
            server
        )

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertFalse(server.isResolverInitialized())
    }

    @Test
    fun statusBarClockTweak_installsWithCorrectScopePhaseAndProcessName() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        val systemui = runtime("com.android.systemui", prefs)

        val result = FeatureInstallRegistry.installById(
            "statusBarClockTweak",
            ProcessScope.SYSTEM_UI,
            InstallPhase.PACKAGE_READY,
            systemui
        )

        assertTrue(result.isActive)
    }

    @Test
    fun autoBrightnessRange_rejectsWrongScopeWithoutProbing() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_autobrightness"] = true
        val server = runtime("android", prefs)

        val result = FeatureInstallRegistry.installById(
            "autoBrightnessRange",
            ProcessScope.SYSTEM_UI,
            InstallPhase.SYSTEM_SERVER_STARTING,
            server
        )

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertFalse(server.isResolverInitialized())
    }

    @Test
    fun autoBrightnessRange_rejectsWrongPhaseWithoutProbing() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_autobrightness"] = true
        val server = runtime("android", prefs)

        val result = FeatureInstallRegistry.installById(
            "autoBrightnessRange",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.PACKAGE_READY,
            server
        )

        assertTrue(result is FeatureInstallResult.WrongPhase)
        assertFalse(server.isResolverInitialized())
    }

    @Test
    fun autoBrightnessRange_rejectsWrongProcessNameWithoutProbing() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_autobrightness"] = true
        val systemui = runtime("com.android.systemui", prefs)

        val result = FeatureInstallRegistry.installById(
            "autoBrightnessRange",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            systemui
        )

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertFalse(systemui.isResolverInitialized())
    }

    @Test
    fun autoBrightnessRange_installsWithCorrectScopePhaseAndProcessName() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_autobrightness"] = true
        val server = runtime("android", prefs)

        val result = FeatureInstallRegistry.installById(
            "autoBrightnessRange",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            server
        )

        // The test classloader does not contain the display framework classes,
        // so the install may degrade/incompatible, but the registry must accept
        // the scope and phase and actually probe the contract.
        assertFalse(result is FeatureInstallResult.UnsupportedProcess)
        assertFalse(result is FeatureInstallResult.WrongPhase)
        assertTrue(server.isResolverInitialized())
    }

    @Test
    fun disabledFeatureCreatesNoRuntimeState() {
        val systemui = runtime("com.android.systemui")

        assertFalse(FeatureDispatcher.installById("batteryIndicator", systemui))

        assertFalse(systemui.isEnvironmentInitialized())
        assertFalse(systemui.isResolverInitialized())
    }

    @Test
    fun unknownFeatureCreatesNoRuntimeState() {
        val systemui = runtime("com.android.systemui")

        assertFalse(FeatureDispatcher.installById("unknown", systemui))

        assertFalse(systemui.isResolverInitialized())

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.UNKNOWN_FEATURE_ID]
        assertNotNull(snapshot)
        assertEquals(InstallOutcome.FAILED, snapshot!!.installation)
        assertEquals(ReasonCode.UNKNOWN, snapshot.reasonCode)
    }

    @Test
    fun requestedCompatibleAndInstalledAreLoggedOnTransition() {
        val server = runtime("android")

        assertTrue(FeatureDispatcher.installById("packagePermissions", server))

        val logStates = logMessages.map { extractState(it) }
        assertEquals(listOf("REQUESTED", "COMPATIBLE", "INSTALLED"), logStates)

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.PACKAGE_PERMISSIONS]
        assertEquals(InstallOutcome.INSTALLED, snapshot!!.installation)
    }

    @Test
    fun primaryCompatibilityIsRecordedAndInstalled() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_batteryindicator"] = true
        val systemui = runtime("com.android.systemui", prefs)

        assertTrue(FeatureDispatcher.installById("batteryIndicator", systemui))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.BATTERY_INDICATOR]
        assertEquals(CompatibilityState.COMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, snapshot.installation)
        assertTrue(logMessages.any { it.contains("PRIMARY_TARGET_FOUND") })
        assertTrue(logMessages.any { it.contains("INSTALLED") })
    }

    @Test
    fun primaryCompatibilityRecordsInstalledForStatusBarClockTweak() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_statusbar_clocktweak"] = true
        prefs["pref_key_system_cc_hidedate"] = true
        val systemui = runtime("com.android.systemui", prefs)

        assertTrue(FeatureDispatcher.installById("statusBarClockTweak", systemui))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.STATUSBAR_CLOCK_TWEAK]
        assertEquals(CompatibilityState.COMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, snapshot.installation)
        assertTrue(logMessages.any { it.contains("PRIMARY_TARGET_FOUND") })
    }

    @Test
    fun incompatibleSkipsInstallerAndRecordsFailed() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_system_autobrightness"] = true

        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = ClassLoader.getSystemClassLoader().parent
        val lpparam = newSystemServerParam(classLoader)
        val server = FeatureDispatcher.createRuntime("android", lpparam, classLoader, prefs)

        assertFalse(FeatureDispatcher.installById("autoBrightnessRange", server))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.AUTO_BRIGHTNESS_RANGE]
        assertEquals(CompatibilityState.INCOMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.FAILED, snapshot.installation)
    }

    @Test
    fun installerExceptionIsRecordedAsFailedAndDoesNotEscape() {
        val prefs = PrefMap<String, Any>()
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

        assertFalse(FeatureDispatcher.installById("muffledVibration", server))

        val snapshot = DiagnosticRecorder.summarize()[DiagnosticIds.MUFFLED_VIBRATION]
        assertEquals(CompatibilityState.COMPATIBLE, snapshot!!.compatibility)
        assertEquals(InstallOutcome.FAILED, snapshot.installation)
        assertTrue(logMessages.any { it.contains("INSTALLER_FAILED") })
    }

    @Test
    fun failedFeatureDoesNotBlockSubsequentInstallByIdCalls() {
        val prefs = PrefMap<String, Any>()
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
        assertFalse(FeatureDispatcher.installById("muffledVibration", server))

        // Second: a fresh, working runtime for packagePermissions must still install.
        val goodServer = runtime("android")
        assertTrue(FeatureDispatcher.installById("packagePermissions", goodServer))

        assertEquals(InstallOutcome.FAILED, DiagnosticRecorder.summarize()[DiagnosticIds.MUFFLED_VIBRATION]!!.installation)
        assertEquals(InstallOutcome.INSTALLED, DiagnosticRecorder.summarize()[DiagnosticIds.PACKAGE_PERMISSIONS]!!.installation)
    }

    @Test
    fun launcherCanaryAreCompatibleAndInstalled() {
        val prefs = PrefMap<String, Any>()
        prefs["pref_key_launcher_noclockhide"] = true
        prefs["pref_key_launcher_nowidgetonly"] = true
        val launcher = runtime("com.miui.home", prefs)

        assertTrue(FeatureDispatcher.installById("noClockHide", launcher))
        assertTrue(FeatureDispatcher.installById("noWidgetOnly", launcher))

        val noClockHide = DiagnosticRecorder.summarize()[DiagnosticIds.NO_CLOCK_HIDE]
        assertEquals(CompatibilityState.COMPATIBLE, noClockHide!!.compatibility)
        assertEquals(InstallOutcome.INSTALLED, noClockHide.installation)
    }

    @Test
    fun installByIdReturnsFalseForUnknownFeature() {
        val systemui = runtime("com.android.systemui")
        assertFalse(FeatureDispatcher.installById("unknown", systemui))
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
            "volumeSteps",
            "toastTime",
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
            "tempHideOverlaySystemUI",
            "hideStatusBarBeforeScreenshot"
        )
        assertEquals(ids, FeatureCatalog.specs().map { it.id }.toSet())
    }

    @Test
    fun registrySpecsContainsOnlyMigratedFeatures() {
        val registryIds = FeatureCatalog.registrySpecs().map { it.id }.toSet()
        val migrated = setOf(
            "packagePermissions",
            "statusBarClockTweak",
            "autoBrightnessRange",
            "muffledVibration",
            "noMoreIcon",
            "batteryIndicator",
            "noClockHide",
            "noWidgetOnly"
        )

        assertEquals("registry specs are limited to migrated features", migrated, registryIds)
    }

    @Test
    fun registrySpecsDoesNotContainLegacyFeatures() {
        val registryIds = FeatureCatalog.registrySpecs().map { it.id }.toSet()
        val legacy = setOf("screenDimTime", "firstVolumePress", "hideLauncherTitles")

        assertTrue("legacy features are not pre-registered", legacy.none { it in registryIds })
    }

    @Test
    fun registrySpecsHaveOneDispatcherEntry() {
        val registryIds = FeatureCatalog.registrySpecs().map { it.id }.toSet()

        for (id in registryIds) {
            assertNotNull("registry feature $id has dispatcher entry", FeatureId.fromString(id))
        }
    }

    @Test
    fun registrySpecsHaveNoDuplicateCanonicalId() {
        val ids = FeatureCatalog.registrySpecs().map { it.id }
        assertEquals("canonical ids are unique", ids.size, ids.toSet().size)
    }

    @Test
    fun allCatalogSpecsHaveExplicitCompatibilityPolicy() {
        for (spec in FeatureCatalog.specs()) {
            assertTrue(
                "${spec.id} compatibility policy is explicit",
                spec.compatibilityPolicy == CompatibilityPolicy.CONTRACT_REQUIRED ||
                spec.compatibilityPolicy == CompatibilityPolicy.CUSTOM
            )
        }
    }

    @Test
    fun registrySpecsDoNotBuildLegacySpecs() {
        FeatureCatalog.CatalogBuildProbe.reset()
        val specs = FeatureCatalog.registrySpecs()
        assertEquals(8, specs.size)
        assertEquals("registrySpecs only builds the 8 migrated specs", 8, FeatureCatalog.CatalogBuildProbe.registrySpecsBuilt)
        assertEquals("registrySpecs does not touch adapted specs", 0, FeatureCatalog.CatalogBuildProbe.adaptedSpecsBuilt)
    }

    @Test
    fun specsBuildsCompleteCatalogWithoutDoubleCounting() {
        FeatureCatalog.CatalogBuildProbe.reset()
        val specs = FeatureCatalog.specs()
        assertEquals(29, specs.size)
        assertEquals("specs builds the 8 registry specs once", 8, FeatureCatalog.CatalogBuildProbe.registrySpecsBuilt)
        assertEquals("specs builds the 21 adapted specs once", 21, FeatureCatalog.CatalogBuildProbe.adaptedSpecsBuilt)
    }

    @Test
    fun migrationStatistics() {
        val catalogIds = FeatureCatalog.specs().map { it.id }.toSet()
        val registryIds = FeatureCatalog.registrySpecs().map { it.id }.toSet()
        val dispatcherIds = FeatureId.values().map { it.canonicalId }.toSet()
        val legacyIds = catalogIds - registryIds

        val catalogTotal = catalogIds.size
        val registryMigrated = registryIds.size
        val dispatcherLegacy = legacyIds.size

        assertEquals("catalog total", 29, catalogTotal)
        assertEquals("registry migrated", 8, registryMigrated)
        assertEquals("dispatcher legacy", 21, dispatcherLegacy)

        assertTrue("registry ids are a subset of catalog ids", registryIds.all { it in catalogIds })
        assertEquals("dispatcher ids match catalog ids", catalogIds, dispatcherIds)
        val duplicatePaths = registryIds.intersect(legacyIds).size
        assertEquals("registry and legacy features have no duplicate path", 0, duplicatePaths)
        assertEquals("registry + legacy == catalog", catalogIds, registryIds union legacyIds)

        println("catalogTotal=$catalogTotal registryMigrated=$registryMigrated dispatcherLegacy=$dispatcherLegacy duplicatePaths=$duplicatePaths")
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
