package tv.withaibuild.customiuizer.mods.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    fun requestedCompatibleInstalledOnlyLoggedOnStateTransition() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id, DiagnosticState.REQUESTED, reason = "start")
        DiagnosticRecorder.record(id, DiagnosticState.REQUESTED, reason = "start2")
        DiagnosticRecorder.record(id, DiagnosticState.COMPATIBLE, reason = "ok")
        DiagnosticRecorder.record(id, DiagnosticState.INSTALLED, reason = "done")
        DiagnosticRecorder.record(id, DiagnosticState.INSTALLED, reason = "done2")

        assertEquals(
            listOf(
                "Diagnostic[PACKAGE_PERMISSIONS] REQUESTED: start",
                "Diagnostic[PACKAGE_PERMISSIONS] COMPATIBLE: ok",
                "Diagnostic[PACKAGE_PERMISSIONS] INSTALLED: done"
            ),
            logMessages
        )
    }

    @Test
    fun failedEscalatesFromDegradedImmediatelyAndIsThrottled() {
        val id = DiagnosticIds.STATUSBAR_CLOCK_TWEAK

        // First DEGRADED logs and starts a throttle window.
        DiagnosticRecorder.record(id, DiagnosticState.DEGRADED, reason = "missing field")
        assertEquals(1, logMessages.size)

        // A repeated DEGRADED inside the throttle window is silent.
        DiagnosticRecorder.record(id, DiagnosticState.DEGRADED, reason = "missing field")
        assertEquals(1, logMessages.size)

        // Escalation from DEGRADED to FAILED must log immediately, even though
        // the DEGRADED throttle window has not elapsed.
        DiagnosticRecorder.record(id, DiagnosticState.FAILED, reason = "controller missing")
        assertEquals(2, logMessages.size)
        assertTrue(logMessages.last().contains("FAILED"))

        // Repeated FAILED inside the new throttle window is silent.
        DiagnosticRecorder.record(id, DiagnosticState.FAILED, reason = "controller missing")
        assertEquals(2, logMessages.size)

        // After the throttle window, the next FAILED logs again.
        now += 120_000L
        DiagnosticRecorder.record(id, DiagnosticState.FAILED, reason = "controller missing")
        assertEquals(3, logMessages.size)
    }

    @Test
    fun failedThrottlesRepeatedLogButKeepsCount() {
        val id = DiagnosticIds.STEP_COUNTER

        repeat(100) {
            DiagnosticRecorder.record(id, DiagnosticState.FAILED, reason = "boom $it")
        }

        assertEquals(1, logMessages.size)
        val snapshot = DiagnosticRecorder.summarize()[id]
        assertNotNull(snapshot)
        assertEquals(100L, snapshot!!.count)
        assertEquals(DiagnosticState.FAILED, snapshot.state)
    }

    @Test
    fun logContainsFeatureIdStateAndStableReason() {
        val id = DiagnosticIds.DEVICE_INFO_MONITOR
        DiagnosticRecorder.record(id, DiagnosticState.FAILED, reason = "no sensor")
        assertEquals("Diagnostic[DEVICE_INFO_MONITOR] FAILED: no sensor", logMessages.single())
    }

    @Test
    fun throwableIsNotStoredInSnapshot() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS
        val throwable = RuntimeException("secret message")

        DiagnosticRecorder.record(id, DiagnosticState.FAILED, throwable = throwable, reason = "install failed")

        val snapshot = DiagnosticRecorder.summarize()[id]
        assertNotNull(snapshot)
        assertEquals("install failed", snapshot!!.reason)
        // Snapshot should not contain the throwable message or stack trace.
        assertTrue(snapshot.reason?.contains("secret message") != true)
        assertEquals(DiagnosticState.FAILED, snapshot.state)
        assertEquals(1L, snapshot.count)
    }

    @Test
    fun logContainsDesensitizedThrowableSummary() {
        val id = DiagnosticIds.PACKAGE_PERMISSIONS
        val throwable = RuntimeException("secret message")

        DiagnosticRecorder.record(id, DiagnosticState.FAILED, throwable = throwable, reason = "install failed")

        val log = logMessages.single()
        assertTrue(log.startsWith("Diagnostic[PACKAGE_PERMISSIONS] FAILED: install failed"))
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
        DiagnosticRecorder.record(DiagnosticIds.PACKAGE_PERMISSIONS, DiagnosticState.REQUESTED)
        assertEquals(1, called)
    }

    @Test
    fun internalLoggerCanBeInjected() {
        val logs = mutableListOf<String>()
        DiagnosticRecorder.logger = { logs += it }
        DiagnosticRecorder.record(DiagnosticIds.PACKAGE_PERMISSIONS, DiagnosticState.INSTALLED, reason = "ok")
        assertEquals("Diagnostic[PACKAGE_PERMISSIONS] INSTALLED: ok", logs.single())
    }

    @Test
    fun summarizeReturnsExpectedCounts() {
        val id1 = DiagnosticIds.DEVICE_INFO_MONITOR
        val id2 = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id1, DiagnosticState.REQUESTED)
        DiagnosticRecorder.record(id1, DiagnosticState.REQUESTED)
        DiagnosticRecorder.record(id1, DiagnosticState.COMPATIBLE)

        DiagnosticRecorder.record(id2, DiagnosticState.FAILED, reason = "bad")
        DiagnosticRecorder.record(id2, DiagnosticState.FAILED, reason = "bad2")

        val summary = DiagnosticRecorder.summarize()

        assertEquals(3L, summary[id1]?.count)
        assertEquals(DiagnosticState.COMPATIBLE, summary[id1]?.state)

        assertEquals(2L, summary[id2]?.count)
        assertEquals(DiagnosticState.FAILED, summary[id2]?.state)
    }
}
