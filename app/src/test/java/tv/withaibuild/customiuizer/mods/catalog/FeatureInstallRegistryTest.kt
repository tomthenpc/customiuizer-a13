package tv.withaibuild.customiuizer.mods.catalog

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.utils.FeatureInstallResult
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
import tv.withaibuild.customiuizer.utils.PrefMap

class FeatureInstallRegistryTest {

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clear()
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clear()
    }

    private fun runtime(processName: String = "test-${System.nanoTime()}"): FeatureRuntime {
        val classLoader = this.javaClass.classLoader!!
        return FeatureRuntime(processName, Any(), classLoader, PrefMap<String, Any>())
    }

    private fun spec(
        id: String = "test",
        condition: (PrefMap<String, Any>) -> Boolean = { true },
        compatibilityCheck: (FeatureRuntime) -> CompatibilityState = { CompatibilityState.COMPATIBLE },
        installer: (FeatureRuntime) -> FeatureInstallResult = { FeatureInstallResult.Installed },
        processScope: ProcessScope? = null,
        installPhase: InstallPhase? = null
    ): FeatureSpec = FeatureSpec(
        contract = null,
        id = id,
        diagnosticId = id,
        processScope = processScope,
        processTarget = ProcessTarget.Any,
        installPhase = installPhase,
        preferenceKeys = emptySet(),
        condition = condition,
        compatibilityCheck = compatibilityCheck,
        installer = installer,
        activationRestartTarget = RestartTarget.REBOOT,
        configReloadMode = ConfigReloadMode.NONE
    )

    @Test
    fun disabledFeatureDoesNotCallFactory() {
        var called = 0
        val s = spec(condition = { false }, installer = { called++; FeatureInstallResult.Installed })
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertEquals(FeatureInstallResult.Disabled, result)
        assertEquals(0, called)
    }

    @Test
    fun wrongScopeDoesNotCallFactory() {
        var called = 0
        val s = spec(processScope = ProcessScope.SYSTEM_UI, installer = { called++; FeatureInstallResult.Installed })
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.LAUNCHER, InstallPhase.PACKAGE_READY, runtime())

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertEquals(0, called)
    }

    @Test
    fun wrongPhaseDoesNotCallFactory() {
        var called = 0
        val s = spec(installPhase = InstallPhase.SYSTEMUI_POST_INIT, installer = { called++; FeatureInstallResult.Installed })
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertTrue(result is FeatureInstallResult.WrongPhase)
        assertEquals(0, called)
    }

    @Test
    fun incompatibleDoesNotCallFactory() {
        var called = 0
        val s = spec(compatibilityCheck = { CompatibilityState.INCOMPATIBLE }, installer = { called++; FeatureInstallResult.Installed })
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertTrue(result is FeatureInstallResult.Incompatible)
        assertEquals(0, called)
    }

    @Test
    fun firstInstallReturnsInstalledSecondReturnsAlreadyInstalled() {
        var called = 0
        val s = spec(installer = { called++; FeatureInstallResult.Installed })
        FeatureInstallRegistry.register(s)
        val rt = runtime()

        val first = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertEquals(FeatureInstallResult.Installed, first)
        assertEquals(FeatureInstallResult.AlreadyInstalled, second)
        assertEquals(1, called)
    }

    @Test
    fun transientFailureAllowsRetry() {
        var fail = true
        val s = spec(installer = {
            if (fail) {
                fail = false
                FeatureInstallResult.FailedTransient("boom")
            } else {
                FeatureInstallResult.Installed
            }
        })
        FeatureInstallRegistry.register(s)
        val rt = runtime()

        val first = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(first is FeatureInstallResult.FailedTransient)
        assertEquals(FeatureInstallResult.Installed, second)
    }

    @Test
    fun permanentFailureIsIdempotent() {
        val s = spec(installer = { FeatureInstallResult.FailedPermanent("nope") })
        FeatureInstallRegistry.register(s)
        val rt = runtime()

        val first = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(first is FeatureInstallResult.FailedPermanent)
        assertTrue(second is FeatureInstallResult.FailedPermanent)
    }

    @Test
    fun oneFailureDoesNotBlockAnotherFeature() {
        val failing = spec(id = "failing", installer = { FeatureInstallResult.FailedTransient("boom") })
        val working = spec(id = "working", installer = { FeatureInstallResult.Installed })
        FeatureInstallRegistry.register(failing)
        FeatureInstallRegistry.register(working)
        val rt = runtime()

        val r1 = FeatureInstallRegistry.installById("failing", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val r2 = FeatureInstallRegistry.installById("working", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(r1 is FeatureInstallResult.FailedTransient)
        assertEquals(FeatureInstallResult.Installed, r2)
    }

    @Test
    fun fatalErrorPropagatesAndDoesNotLeaveInstalling() {
        var seen = false
        val s = spec(id = "fatal", installer = { throw OutOfMemoryError("oom") })
        FeatureInstallRegistry.register(s)

        assertThrows(OutOfMemoryError::class.java) {
            try {
                FeatureInstallRegistry.installById("fatal", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime("fatal-process"))
            } catch (oom: OutOfMemoryError) {
                seen = true
                throw oom
            }
        }

        assertTrue(seen)
    }
}
