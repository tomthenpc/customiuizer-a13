package tv.withaibuild.customiuizer.mods.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState

class DiagnosticRecorderTest {

    private val logMessages = mutableListOf<String>()
    private var now = 0L

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        logMessages.clear()
        now = 0L
        DiagnosticRecorder.clock = { now }
        DiagnosticRecorder.logger = { logMessages += it }
    }

    @Test
    fun disabledDoesNotLogRequested() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id, enabled = EnabledState.REQUESTED, reasonCode = ReasonCode.REQUESTED)
        DiagnosticRecorder.record(id, enabled = EnabledState.DISABLED, reasonCode = ReasonCode.PREFERENCE_DISABLED)
        DiagnosticRecorder.record(id, enabled = EnabledState.DISABLED, reasonCode = ReasonCode.PREFERENCE_DISABLED)

        assertEquals(2, logMessages.size)
        assertTrue(logMessages[0].contains("REQUESTED"))
        assertTrue(logMessages[1].contains("DISABLED"))
    }

    @Test
    fun requestedCompatibleDispatchedOnlyLoggedOnTransition() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id, enabled = EnabledState.REQUESTED, reasonCode = ReasonCode.REQUESTED)
        DiagnosticRecorder.record(id, enabled = EnabledState.REQUESTED, reasonCode = ReasonCode.REQUESTED)
        DiagnosticRecorder.record(
            id,
            compatibility = CompatibilityState.COMPATIBLE,
            reasonCode = ReasonCode.PRIMARY_TARGET_FOUND
        )
        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.DISPATCHED,
            reasonCode = ReasonCode.INSTALLER_DISPATCHED
        )
        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.DISPATCHED,
            reasonCode = ReasonCode.INSTALLER_DISPATCHED
        )

        assertEquals(3, logMessages.size)
        assertTrue(logMessages[0].contains("REQUESTED"))
        assertTrue(logMessages[1].contains("COMPATIBLE"))
        assertTrue(logMessages[2].contains("DISPATCHED"))
    }

    @Test
    fun failedEscalatesFromDegradedImmediatelyAndIsThrottled() {
        val id = DiagnosticIds.STATUSBAR_CLOCK_TWEAK

        DiagnosticRecorder.record(
            id,
            compatibility = CompatibilityState.DEGRADED,
            reasonCode = ReasonCode.FALLBACK_TARGET_FOUND
        )
        assertEquals(1, logMessages.size)

        DiagnosticRecorder.record(
            id,
            compatibility = CompatibilityState.DEGRADED,
            reasonCode = ReasonCode.FALLBACK_TARGET_FOUND
        )
        assertEquals(1, logMessages.size)

        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.FAILED,
            reasonCode = ReasonCode.INSTALLER_FAILED
        )
        assertEquals(2, logMessages.size)
        assertTrue(logMessages.last().contains("FAILED"))

        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.FAILED,
            reasonCode = ReasonCode.INSTALLER_FAILED
        )
        assertEquals(2, logMessages.size)

        now += 120_000L
        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.FAILED,
            reasonCode = ReasonCode.INSTALLER_FAILED
        )
        assertEquals(3, logMessages.size)
    }

    @Test
    fun failedThrottlesRepeatedLogButKeepsCount() {
        val id = DiagnosticIds.STEP_COUNTER

        repeat(100) {
            DiagnosticRecorder.record(
                id,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.INSTALLER_FAILED
            )
        }

        assertEquals(1, logMessages.size)
        val snapshot = DiagnosticRecorder.summarize()[id]
        assertNotNull(snapshot)
        assertEquals(100L, snapshot!!.count)
        assertEquals(InstallOutcome.FAILED, snapshot.installation)
    }

    @Test
    fun logContainsFeatureIdStateAndStableReasonCode() {
        val id = DiagnosticIds.DEVICE_INFO_MONITOR
        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.FAILED,
            reasonCode = ReasonCode.TARGET_NOT_FOUND,
            detail = "no sensor"
        )
        val log = logMessages.single()
        assertTrue(log.startsWith("Diagnostic[DEVICE_INFO_MONITOR]"))
        assertTrue(log.contains("FAILED"))
        assertTrue(log.contains("TARGET_NOT_FOUND"))
        assertTrue(log.contains("no sensor"))
    }

    @Test
    fun snapshotKeepsCompatibilityAndInstallationSeparate() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS
        val throwable = RuntimeException("secret message")

        DiagnosticRecorder.record(
            id,
            compatibility = CompatibilityState.DEGRADED,
            reasonCode = ReasonCode.FALLBACK_TARGET_FOUND
        )
        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.DISPATCHED,
            reasonCode = ReasonCode.INSTALLER_DISPATCHED,
            throwable = throwable
        )

        val snapshot = DiagnosticRecorder.summarize()[id]
        assertNotNull(snapshot)
        assertEquals(CompatibilityState.DEGRADED, snapshot!!.compatibility)
        assertEquals(InstallOutcome.DISPATCHED, snapshot.installation)
        assertNull(snapshot.detail?.contains("secret message"))
    }

    @Test
    fun logContainsDesensitizedThrowableSummary() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS
        val throwable = RuntimeException("secret message")

        DiagnosticRecorder.record(
            id,
            installation = InstallOutcome.FAILED,
            reasonCode = ReasonCode.INSTALLER_FAILED,
            detail = "install failed",
            throwable = throwable
        )

        val log = logMessages.single()
        assertTrue(log.startsWith("Diagnostic[PACKAGE_PERMISSIONS]"))
        assertTrue(log.contains("java.lang.RuntimeException"))
        assertTrue(log.contains("secret message"))
    }

    @Test
    fun internalClockCanBeInjected() {
        var called = 0
        DiagnosticRecorder.clock = {
            called++
            1234L
        }
        DiagnosticRecorder.record(DiagnosticIds.PACKAGE_PERMISSIONS, enabled = EnabledState.REQUESTED, reasonCode = ReasonCode.REQUESTED)
        assertEquals(1, called)
    }

    @Test
    fun internalLoggerCanBeInjected() {
        val logs = mutableListOf<String>()
        DiagnosticRecorder.logger = { logs += it }
        DiagnosticRecorder.record(
            DiagnosticIds.PACKAGE_PERMISSIONS,
            installation = InstallOutcome.INSTALLED,
            reasonCode = ReasonCode.INSTALLER_SUCCEEDED
        )
        assertTrue(logs.single().contains("INSTALLED"))
    }

    @Test
    fun summarizeReturnsExpectedCountsAndTimestamps() {
        val id1 = DiagnosticIds.DEVICE_INFO_MONITOR
        val id2 = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id1, enabled = EnabledState.REQUESTED, reasonCode = ReasonCode.REQUESTED)
        DiagnosticRecorder.record(id1, compatibility = CompatibilityState.COMPATIBLE, reasonCode = ReasonCode.PRIMARY_TARGET_FOUND)
        DiagnosticRecorder.record(id1, compatibility = CompatibilityState.COMPATIBLE, reasonCode = ReasonCode.PRIMARY_TARGET_FOUND)

        now += 5000L
        DiagnosticRecorder.record(id2, installation = InstallOutcome.FAILED, reasonCode = ReasonCode.INSTALLER_FAILED)
        DiagnosticRecorder.record(id2, installation = InstallOutcome.FAILED, reasonCode = ReasonCode.INSTALLER_FAILED)

        val summary = DiagnosticRecorder.summarize()

        assertEquals(3L, summary[id1]?.count)
        assertEquals(CompatibilityState.COMPATIBLE, summary[id1]?.compatibility)

        assertEquals(2L, summary[id2]?.count)
        assertEquals(InstallOutcome.FAILED, summary[id2]?.installation)
        assertEquals(5000L, summary[id2]?.firstSeenMs)
        assertEquals(now, summary[id2]?.lastSeenMs)
    }

    @Test
    fun boundsSnapshotsToMaxLimit() {
        repeat(50) { index ->
            val id = "feature_$index"
            DiagnosticRecorder.record(id, installation = InstallOutcome.INSTALLED, reasonCode = ReasonCode.INSTALLER_SUCCEEDED)
        }

        assertEquals(32, DiagnosticRecorder.summarize().size)
    }

    @Test
    fun evictedThrottlerEntriesAreRemovedWithSnapshots() {
        repeat(33) { index ->
            val id = "feature_$index"
            DiagnosticRecorder.record(id, installation = InstallOutcome.FAILED, reasonCode = ReasonCode.INSTALLER_FAILED)
        }

        val firstId = "feature_0"
        val summary = DiagnosticRecorder.summarize()
        assertNull(summary[firstId])
    }

    @Test
    fun detailIsTruncatedToMaxLength() {
        val longDetail = "x".repeat(600)
        val id = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id, installation = InstallOutcome.FAILED, reasonCode = ReasonCode.INSTALLER_FAILED, detail = longDetail)

        val snapshot = DiagnosticRecorder.summarize()[id]
        assertNotNull(snapshot)
        assertEquals(512, snapshot!!.detail?.length)
        assertTrue(snapshot.detail?.startsWith("x".repeat(10)) == true)
    }

    @Test
    fun failedLogsImmediatelyDespiteBounds() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id, installation = InstallOutcome.FAILED, reasonCode = ReasonCode.INSTALLER_FAILED)
        assertEquals(1, logMessages.size)
        assertTrue(logMessages.single().contains("FAILED"))
    }

    @Test
    fun resetClearsAllState() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS
        DiagnosticRecorder.record(id, installation = InstallOutcome.FAILED, reasonCode = ReasonCode.INSTALLER_FAILED)
        assertFalse(DiagnosticRecorder.summarize().isEmpty())

        DiagnosticRecorder.reset()

        assertTrue(DiagnosticRecorder.summarize().isEmpty())
    }
}
