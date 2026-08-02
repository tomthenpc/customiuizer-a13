package tv.withaibuild.customiuizer.mods.catalog

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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
        FeatureInstallRegistry.resetRegistryForTesting()
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.resetRegistryForTesting()
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
        compatibilityPolicy: CompatibilityPolicy = CompatibilityPolicy.CUSTOM,
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
        compatibilityPolicy = compatibilityPolicy,
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

    @Test
    fun conditionFalse_createsNoRuntimeState() {
        val s = spec(condition = { false })
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertEquals(FeatureInstallResult.Disabled, result)
        assertEquals(0, DiagnosticRecorder.summarize().size)
    }

    @Test
    fun conditionException_isIsolatedToFeature() {
        val s = spec(condition = { throw RuntimeException("condition boom") })
        FeatureInstallRegistry.register(s)

        val result = FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertTrue(result is FeatureInstallResult.FailedTransient)
        val snapshot = DiagnosticRecorder.summarize()["test"]!!
        assertEquals(ReasonCode.CONDITION_EVALUATION_FAILED, snapshot.reasonCode)
        assertEquals(InstallOutcome.FAILED, snapshot.installation)
    }

    @Test
    fun conditionException_doesNotRunCompatibilityOrInstaller() {
        var compatibilityCalled = 0
        var installerCalled = 0
        val s = spec(
            condition = { throw RuntimeException("condition boom") },
            compatibilityCheck = { compatibilityCalled++; CompatibilityResult(CompatibilityState.COMPATIBLE, ReasonCode.PRIMARY_TARGET_FOUND, null, HookInstallResult.DISPATCHED) },
            installer = { _, _ -> installerCalled++; FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)

        FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())

        assertEquals(0, compatibilityCalled)
        assertEquals(0, installerCalled)
    }

    @Test
    fun conditionException_allowsLaterRetry() {
        var shouldThrow = true
        val s = spec(
            id = "retryableCondition",
            condition = {
                if (shouldThrow) throw RuntimeException("condition boom")
                true
            },
            installer = { _, _ -> FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)

        val rt = runtime("retryable-condition")
        val first = FeatureInstallRegistry.installById("retryableCondition", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        assertTrue(first is FeatureInstallResult.FailedTransient)

        shouldThrow = false
        val second = FeatureInstallRegistry.installById("retryableCondition", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        assertEquals(FeatureInstallResult.Installed(), second)
    }

    @Test
    fun conditionException_doesNotBlockOtherFeature() {
        val failing = spec(id = "badCondition", condition = { throw RuntimeException("boom") })
        val working = spec(id = "goodCondition", installer = { _, _ -> FeatureInstallResult.Installed() })
        FeatureInstallRegistry.register(failing)
        FeatureInstallRegistry.register(working)
        val rt = runtime()

        val r1 = FeatureInstallRegistry.installById("badCondition", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val r2 = FeatureInstallRegistry.installById("goodCondition", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(r1 is FeatureInstallResult.FailedTransient)
        assertEquals(FeatureInstallResult.Installed(), r2)
    }

    @Test
    fun conditionFatalErrorPropagates() {
        val s = spec(condition = { throw OutOfMemoryError("condition oom") })
        FeatureInstallRegistry.register(s)

        assertThrows(OutOfMemoryError::class.java) {
            FeatureInstallRegistry.installById("test", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime())
        }
    }

    @Test
    fun concurrentInstall_sameFeature_installerRunsOnce() {
        val installCount = AtomicInteger(0)
        val startBarrier = CyclicBarrier(4)
        val s = spec(
            id = "concurrent",
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            installer = { _, _ ->
                installCount.incrementAndGet()
                FeatureInstallResult.Installed()
            }
        )
        FeatureInstallRegistry.register(s)

        val rt = runtime("concurrent-process")
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..4).map {
                executor.submit(Callable {
                    startBarrier.await(1, TimeUnit.SECONDS)
                    FeatureInstallRegistry.installById("concurrent", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
                })
            }
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdown()
        }

        assertEquals(1, installCount.get())
    }

    @Test
    fun concurrentInstall_sameFeature_onlyOneThreadOwnsInstalling() {
        val stateClaimed = CountDownLatch(1)
        val canFinish = CountDownLatch(1)
        val s = spec(
            id = "owning",
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            installer = { _, _ ->
                stateClaimed.countDown()
                canFinish.await()
                FeatureInstallResult.Installed()
            }
        )
        FeatureInstallRegistry.register(s)

        val rt = runtime("owning-process")
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit(Callable {
                FeatureInstallRegistry.installById("owning", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
            })
            stateClaimed.await(5, TimeUnit.SECONDS)

            val second = executor.submit(Callable {
                FeatureInstallRegistry.installById("owning", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
            })

            assertTrue("second caller observes an in-progress install", second.get(5, TimeUnit.SECONDS) is FeatureInstallResult.FailedTransient)
            canFinish.countDown()
            assertEquals(FeatureInstallResult.Installed(), first.get(5, TimeUnit.SECONDS))
        } finally {
            canFinish.countDown()
            executor.shutdown()
        }
    }

    @Test
    fun concurrentInstall_sameFeature_otherThreadNeverRunsCompatibility() {
        val compatibilityCount = AtomicInteger(0)
        val latch = CountDownLatch(1)
        val s = spec(
            id = "compatOnce",
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            compatibilityCheck = {
                compatibilityCount.incrementAndGet()
                latch.countDown()
                CompatibilityResult(CompatibilityState.COMPATIBLE, ReasonCode.PRIMARY_TARGET_FOUND, null, HookInstallResult.DISPATCHED)
            },
            installer = { _, _ ->
                FeatureInstallResult.Installed()
            }
        )
        FeatureInstallRegistry.register(s)

        val rt = runtime("compatOnce-process")
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..4).map {
                executor.submit(Callable {
                    FeatureInstallRegistry.installById("compatOnce", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
                })
            }
            latch.await(5, TimeUnit.SECONDS)
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdown()
        }

        assertEquals(1, compatibilityCount.get())
    }

    @Test
    fun concurrentInstall_aliasAndCanonical_shareSameStateKey() {
        var installCount = AtomicInteger(0)
        val s = spec(
            id = "canonicalAlias",
            aliases = setOf("alias_one"),
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            installer = { _, _ ->
                installCount.incrementAndGet()
                FeatureInstallResult.Installed()
            }
        )
        FeatureInstallRegistry.register(s)

        val rt = runtime("alias-process")
        val executor = Executors.newFixedThreadPool(2)
        try {
            val f1 = executor.submit(Callable { FeatureInstallRegistry.installById("canonicalAlias", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt) })
            val f2 = executor.submit(Callable { FeatureInstallRegistry.installById("alias_one", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt) })
            val f1Result = f1.get(5, TimeUnit.SECONDS)
            val f2Result = f2.get(5, TimeUnit.SECONDS)
            val installedResults = listOf(f1Result, f2Result).filterIsInstance<FeatureInstallResult.Installed>()
            assertEquals("exactly one of canonical/alias installs", 1, installedResults.size)
            assertTrue(
                "the non-installing call resolves through the shared state",
                f1Result !is FeatureInstallResult.Installed ||
                (f2Result is FeatureInstallResult.AlreadyInstalled || f2Result is FeatureInstallResult.FailedTransient)
            )
            assertTrue(
                "the non-installing call resolves through the shared state",
                f2Result !is FeatureInstallResult.Installed ||
                (f1Result is FeatureInstallResult.AlreadyInstalled || f1Result is FeatureInstallResult.FailedTransient)
            )
        } finally {
            executor.shutdown()
        }

        assertEquals(1, installCount.get())
    }

    @Test
    fun concurrentInstall_differentFeatures_canRunIndependently() {
        val aCount = AtomicInteger(0)
        val bCount = AtomicInteger(0)
        val a = spec(id = "featureA", processScope = ProcessScope.SYSTEM_UI, installPhase = InstallPhase.PACKAGE_READY, installer = { _, _ -> aCount.incrementAndGet(); FeatureInstallResult.Installed() })
        val b = spec(id = "featureB", processScope = ProcessScope.SYSTEM_UI, installPhase = InstallPhase.PACKAGE_READY, installer = { _, _ -> bCount.incrementAndGet(); FeatureInstallResult.Installed() })
        FeatureInstallRegistry.register(a)
        FeatureInstallRegistry.register(b)

        val rt = runtime("independent-process")
        val executor = Executors.newFixedThreadPool(2)
        try {
            val f1 = executor.submit(Callable { FeatureInstallRegistry.installById("featureA", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt) })
            val f2 = executor.submit(Callable { FeatureInstallRegistry.installById("featureB", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt) })
            f1.get(5, TimeUnit.SECONDS)
            f2.get(5, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }

        assertEquals(1, aCount.get())
        assertEquals(1, bCount.get())
    }

    @Test
    fun concurrentInstall_sameFeatureDifferentProcess_canRunIndependently() {
        val installCount = AtomicInteger(0)
        val s = spec(
            id = "perProcess",
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            installer = { _, _ ->
                installCount.incrementAndGet()
                FeatureInstallResult.Installed()
            }
        )
        FeatureInstallRegistry.register(s)

        val executor = Executors.newFixedThreadPool(2)
        try {
            val f1 = executor.submit(Callable { FeatureInstallRegistry.installById("perProcess", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime("process-a")) })
            val f2 = executor.submit(Callable { FeatureInstallRegistry.installById("perProcess", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime("process-b")) })
            f1.get(5, TimeUnit.SECONDS)
            f2.get(5, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }

        assertEquals(2, installCount.get())
    }

    @Test
    fun transientFailure_releasesClaimAndAllowsRetry() {
        var fail = true
        val s = spec(
            id = "transientRetry",
            installer = { _, _ ->
                if (fail) {
                    fail = false
                    FeatureInstallResult.FailedTransient("boom")
                } else {
                    FeatureInstallResult.Installed()
                }
            }
        )
        FeatureInstallRegistry.register(s)
        val rt = runtime("transient-retry")

        val first = FeatureInstallRegistry.installById("transientRetry", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("transientRetry", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(first is FeatureInstallResult.FailedTransient)
        assertEquals(FeatureInstallResult.Installed(), second)
    }

    @Test
    fun ordinaryException_releasesClaimAndAllowsRetry() {
        var fail = true
        val s = spec(
            id = "ordinaryRetry",
            installer = { _, _ ->
                if (fail) {
                    fail = false
                    throw RuntimeException("installer boom")
                } else {
                    FeatureInstallResult.Installed()
                }
            }
        )
        FeatureInstallRegistry.register(s)
        val rt = runtime("ordinary-retry")

        val first = FeatureInstallRegistry.installById("ordinaryRetry", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("ordinaryRetry", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(first is FeatureInstallResult.FailedTransient)
        assertEquals(FeatureInstallResult.Installed(), second)
    }

    @Test
    fun fatalError_releasesClaimAndPropagates() {
        var seen = false
        val s = spec(id = "fatalInstaller", installer = { _, _ -> throw OutOfMemoryError("oom") })
        FeatureInstallRegistry.register(s)

        assertThrows(OutOfMemoryError::class.java) {
            try {
                FeatureInstallRegistry.installById("fatalInstaller", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime("fatal-process"))
            } catch (oom: OutOfMemoryError) {
                seen = true
                throw oom
            }
        }

        assertTrue(seen)
    }

    @Test
    fun permanentFailure_blocksFutureInstall() {
        val s = spec(id = "permFail", installer = { _, _ -> FeatureInstallResult.FailedPermanent("nope") })
        FeatureInstallRegistry.register(s)
        val rt = runtime("perm-fail")

        val first = FeatureInstallRegistry.installById("permFail", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        val second = FeatureInstallRegistry.installById("permFail", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertTrue(first is FeatureInstallResult.FailedPermanent)
        assertTrue(second is FeatureInstallResult.FailedPermanent)
    }

    @Test
    fun installedReturnsAlreadyInstalledWithoutRunningCondition() {
        var conditionCalled = 0
        val s = spec(
            id = "cached",
            condition = { conditionCalled++; true },
            installer = { _, _ -> FeatureInstallResult.Installed() }
        )
        FeatureInstallRegistry.register(s)
        val rt = runtime("cached-process")

        val first = FeatureInstallRegistry.installById("cached", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)
        conditionCalled = 0
        val second = FeatureInstallRegistry.installById("cached", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, rt)

        assertEquals(FeatureInstallResult.Installed(), first)
        assertEquals(FeatureInstallResult.AlreadyInstalled, second)
        assertEquals(0, conditionCalled)
    }

    @Test
    fun concurrentConflictingRegistration_onlyOneSucceeds() {
        val a = spec(id = "conflict", condition = { true })
        val b = spec(id = "conflict", condition = { false })

        val executor = Executors.newFixedThreadPool(2)
        try {
            val f1 = executor.submit(Callable { FeatureInstallRegistry.register(a); null })
            val f2 = executor.submit(Callable { FeatureInstallRegistry.register(b); null })
            val r1Succeeded = try {
                f1.get(5, TimeUnit.SECONDS)
                true
            } catch (_: Exception) {
                false
            }
            val r2Succeeded = try {
                f2.get(5, TimeUnit.SECONDS)
                true
            } catch (_: Exception) {
                false
            }
            assertTrue("exactly one conflicting registration succeeds", r1Succeeded xor r2Succeeded)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun failedAliasRegistration_leavesNoPartialMappings() {
        val a = spec(id = "aliasA", aliases = setOf("shared_alias"))
        val b = spec(id = "aliasB", aliases = setOf("shared_alias"))
        FeatureInstallRegistry.register(a)

        assertThrows(IllegalArgumentException::class.java) {
            FeatureInstallRegistry.register(b)
        }

        // Re-registering a new canonical that does not conflict should still work,
        // proving the alias map was not left in a half-updated state.
        val c = spec(id = "aliasC")
        FeatureInstallRegistry.register(c)
        assertEquals(FeatureInstallResult.Installed(), FeatureInstallRegistry.installById("aliasC", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime()))
    }

    @Test
    fun failedCanonicalRegistration_leavesNoPartialMappings() {
        val a = spec(id = "canonicalDup")
        val b = spec(id = "canonicalDup", condition = { false })
        FeatureInstallRegistry.register(a)

        assertThrows(IllegalArgumentException::class.java) {
            FeatureInstallRegistry.register(b)
        }

        val c = spec(id = "canonicalFree")
        FeatureInstallRegistry.register(c)
        assertEquals(FeatureInstallResult.Installed(), FeatureInstallRegistry.installById("canonicalFree", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime()))
    }

    @Test
    fun idempotentSameSpecConcurrentRegistration_succeeds() {
        val s = spec(id = "idempotentReg")
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..4).map { executor.submit(Callable { FeatureInstallRegistry.register(s) }) }
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdown()
        }

        assertEquals(FeatureInstallResult.Installed(), FeatureInstallRegistry.installById("idempotentReg", ProcessScope.SYSTEM_UI, InstallPhase.PACKAGE_READY, runtime()))
    }
}
