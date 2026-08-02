package tv.withaibuild.customiuizer.mods.catalog

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.EnabledState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.FeatureInstallResult
import tv.withaibuild.customiuizer.mods.utils.HookInstallResult
import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
import tv.withaibuild.customiuizer.mods.catalog.RestartTarget
import tv.withaibuild.customiuizer.utils.PrefMap

class FeatureInstallRegistryTest {

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.resetForTesting()
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.resetForTesting()
        // Restore the production catalog so other test suites that rely on
        // FeatureDispatcher's class-init registration still find the specs.
        FeatureInstallRegistry.registerAll(FeatureCatalog.registrySpecs())
    }

    private fun runtime(processName: String = "test-${System.nanoTime()}"): FeatureRuntime {
        val classLoader = this.javaClass.classLoader!!
        return FeatureRuntime(processName, Any(), classLoader, PrefMap<String, Any>())
    }

    private fun spec(
        id: String = "test",
        aliases: Set<String> = emptySet(),
        condition: (PrefMap<String, Any>) -> Boolean = { true },
        compatibilityCheck: (FeatureRuntime) -> CompatibilityResult = {
            CompatibilityResult(
                CompatibilityState.COMPATIBLE,
                ReasonCode.PRIMARY_TARGET_FOUND,
                null,
                HookInstallResult.DISPATCHED
            )
        },
        installer: (FeatureRuntime, HookInstallResult) -> FeatureInstallResult = { _, _ -> FeatureInstallResult.Installed() },
        processScope: ProcessScope? = null,
        installPhase: InstallPhase? = null
    ): FeatureSpec = FeatureSpec(
        contract = null,
        id = id,
        aliases = aliases,
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
        val s = spec(
            condition = { false },
            installer = { _, _ -> called++; FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertEquals(FeatureInstallResult.Disabled, result)
        assertEquals(0, called)
    }

    @Test
    fun wrongScopeDoesNotCallFactory() {
        var called = 0
        val s = spec(
            processScope = ProcessScope.SYSTEM_UI,
            installer = { _, _ -> called++; FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.LAUNCHER, InstallPhase.PACKAGE_READY, runtime())

        assertTrue(result is FeatureInstallResult.UnsupportedProcess)
        assertEquals(0, called)
    }

    @Test
    fun wrongPhaseDoesNotCallFactory() {
        var called = 0
        val s = spec(
            installPhase = InstallPhase.SYSTEMUI_POST_INIT,
            installer = { _, _ -> called++; FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertTrue(result is FeatureInstallResult.WrongPhase)
        assertEquals(0, called)
    }

    @Test
    fun incompatibleDoesNotCallFactory() {
        var called = 0
        val s = spec(
            compatibilityCheck = {
                CompatibilityResult(
                    CompatibilityState.INCOMPATIBLE,
                    ReasonCode.TARGET_NOT_FOUND,
                    null,
                    HookInstallResult()
                )
            },
            installer = { _, _ -> called++; FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertTrue(result is FeatureInstallResult.Incompatible)
        assertEquals(0, called)
    }

    @Test
    fun firstInstallReturnsInstalledSecondReturnsAlreadyInstalled() {
        var called = 0
        val s = spec(installer = { _, _ -> called++; FeatureInstallResult.Installed() })
        FeatureInstallRegistry.register(s)
        val rt = runtime()

        val first = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertEquals(FeatureInstallResult.Installed(), first)
        assertEquals(FeatureInstallResult.AlreadyInstalled, second)
        assertEquals(1, called)
    }

    @Test
    fun transientFailureAllowsRetry() {
        var fail = true
        val s = spec(installer = { _, _ ->
            if (fail) {
                fail = false
                FeatureInstallResult.FailedTransient("boom")
            } else {
                FeatureInstallResult.Installed()
            }
        })
        FeatureInstallRegistry.register(s)
        val rt = runtime()

        val first = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(first is FeatureInstallResult.FailedTransient)
        assertEquals(FeatureInstallResult.Installed(), second)
    }

    @Test
    fun permanentFailureIsIdempotent() {
        val s = spec(installer = { _, _ -> FeatureInstallResult.FailedPermanent("nope") })
        FeatureInstallRegistry.register(s)
        val rt = runtime()

        val first = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(first is FeatureInstallResult.FailedPermanent)
        assertTrue(second is FeatureInstallResult.FailedPermanent)
    }

    @Test
    fun oneFailureDoesNotBlockAnotherFeature() {
        val failing = spec(id = "failing", installer = { _, _ -> FeatureInstallResult.FailedTransient("boom") })
        val working = spec(id = "working", installer = { _, _ -> FeatureInstallResult.Installed() })
        FeatureInstallRegistry.register(failing)
        FeatureInstallRegistry.register(working)
        val rt = runtime()

        val r1 = FeatureInstallRegistry.installById("failing", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val r2 = FeatureInstallRegistry.installById("working", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(r1 is FeatureInstallResult.FailedTransient)
        assertEquals(FeatureInstallResult.Installed(), r2)
    }

    @Test
    fun fatalErrorPropagatesAndDoesNotLeaveInstalling() {
        var seen = false
        val s = spec(id = "fatal", installer = { _, _ -> throw OutOfMemoryError("oom") })
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

    @Test
    fun eachLifecycleStateRecordedAtMostOnce() {
        val s = spec(
            id = "once",
            installer = { _, _ -> FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)

        FeatureInstallRegistry.installById("once", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        val snapshot = DiagnosticRecorder.summarize()["once"]!!
        assertEquals("REQUESTED, COMPATIBLE and INSTALLED are three records", 3, snapshot.count)
        assertEquals(CompatibilityState.COMPATIBLE, snapshot.compatibility)
        assertEquals(InstallOutcome.INSTALLED, snapshot.installation)
        assertEquals(EnabledState.REQUESTED, snapshot.enabled)
    }

    @Test
    fun rejectsDifferentCanonicalIdsWithSameNormalizedForm() {
        FeatureInstallRegistry.register(spec(id = "statusBarClockTweak"))

        val thrown = assertThrows(IllegalArgumentException::class.java) {
            FeatureInstallRegistry.register(spec(id = "statusbarclocktweak"))
        }

        assertTrue("mentions both canonical ids", thrown.message!!.contains("statusBarClockTweak"))
        assertTrue("mentions the normalized collision", thrown.message!!.contains("statusbarclocktweak"))
    }

    @Test
    fun rejectsCanonicalIdCollidingWithExistingAlias() {
        FeatureInstallRegistry.register(spec(id = "statusbarclocktweak", aliases = setOf("status_bar_clock", "status bar clock")))

        val thrown = assertThrows(IllegalArgumentException::class.java) {
            FeatureInstallRegistry.register(spec(id = "status_bar_clock"))
        }

        assertTrue("mentions alias", thrown.message!!.contains("status_bar_clock"))
    }

    @Test
    fun rejectsAliasMappingToDifferentCanonicalId() {
        FeatureInstallRegistry.register(spec(id = "statusbarclocktweak"))

        val thrown = assertThrows(IllegalArgumentException::class.java) {
            FeatureInstallRegistry.register(spec(id = "otherClock", aliases = setOf("statusbarclocktweak")))
        }

        assertTrue("mentions colliding alias", thrown.message!!.contains("statusbarclocktweak"))
    }

    @Test
    fun sameSpecRegisteredTwiceIsIdempotent() {
        val s = spec(id = "idempotent")
        FeatureInstallRegistry.register(s)
        FeatureInstallRegistry.register(s)

        assertEquals(FeatureInstallResult.Installed(), FeatureInstallRegistry.installById(
            "idempotent", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime()
        ))
    }

    @Test
    fun rejectsSameCanonicalIdWithDifferentSpec() {
        FeatureInstallRegistry.register(spec(id = "samespec", condition = { true }))

        val thrown = assertThrows(IllegalArgumentException::class.java) {
            FeatureInstallRegistry.register(spec(id = "samespec", condition = { false }))
        }

        assertTrue(thrown.message!!.contains("samespec"))
    }

    @Test
    fun resolvesCaseInsensitiveAndSpacedInputToCanonicalId() {
        FeatureInstallRegistry.register(spec(id = "statusBarClockTweak"))

        assertEquals(
            FeatureInstallResult.Installed(),
            FeatureInstallRegistry.installById("STATUS_BAR_CLOCK_TWEAK", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())
        )
        assertEquals(
            FeatureInstallResult.Installed(),
            FeatureInstallRegistry.installById("status bar clock tweak", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())
        )
    }

    @Test
    fun resolvesExplicitAliasToCanonicalId() {
        FeatureInstallRegistry.register(spec(id = "statusBarClockTweak", aliases = setOf("status_bar_clock")))

        assertEquals(
            FeatureInstallResult.Installed(),
            FeatureInstallRegistry.installById("status_bar_clock", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())
        )
    }

    @Test
    fun unknownAliasReturnsFailedPermanent() {
        FeatureInstallRegistry.register(spec(id = "statusBarClockTweak"))

        val result = FeatureInstallRegistry.installById(
            "clockTweak", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime()
        )

        assertTrue(result is FeatureInstallResult.FailedPermanent)
    }

    @Test
    fun resolvedContractIsReusedBetweenCompatibilityAndInstaller() {
        val contract = CanaryContracts.packagePermissions
        var capturedContract: HookTargetContract? = null

        val s = FeatureSpec(
            id = "contractReuse",
            diagnosticId = "contractReuse",
            processTarget = ProcessTarget.Any,
            preferenceKeys = emptySet(),
            condition = { true },
            compatibilityPolicy = CompatibilityPolicy.CUSTOM,
            compatibilityCheck = {
                capturedContract = contract
                CompatibilityResult(
                    CompatibilityState.COMPATIBLE,
                    ReasonCode.PRIMARY_TARGET_FOUND,
                    "contract built once",
                    HookInstallResult(
                        resolvedContract = contract,
                        reasonCode = ReasonCode.PRIMARY_TARGET_FOUND,
                        detail = "contract built once"
                    )
                )
            },
            installer = { _, compatResult ->
                assertEquals("compatibility and installer must use the same contract", contract, compatResult.resolvedContract)
                FeatureInstallResult.Installed()
            },
            activationRestartTarget = RestartTarget.NONE,
            configReloadMode = ConfigReloadMode.NONE
        )
        FeatureInstallRegistry.register(s)

        FeatureInstallRegistry.installById("contractReuse", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())
        assertNotNull("compatibilityCheck built the contract", capturedContract)
    }

    @Test
    fun incompatibleRecordedOnlyOnce() {
        val s = spec(
            id = "inconce",
            compatibilityCheck = {
                CompatibilityResult(
                    CompatibilityState.INCOMPATIBLE,
                    ReasonCode.TARGET_NOT_FOUND,
                    null,
                    HookInstallResult()
                )
            }
        )
        FeatureInstallRegistry.register(s)

        FeatureInstallRegistry.installById("inconce", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        val snapshot = DiagnosticRecorder.summarize()["inconce"]!!
        assertEquals("REQUESTED, INCOMPATIBLE and INSTALLATION FAILED are three records", 3, snapshot.count)
        assertEquals(CompatibilityState.INCOMPATIBLE, snapshot.compatibility)
        assertEquals(InstallOutcome.FAILED, snapshot.installation)
        assertEquals(EnabledState.REQUESTED, snapshot.enabled)
    }
}
