package tv.withaibuild.customiuizer.mods.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DiagnosticRecorderTest {

    private val logMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        DiagnosticRecorder.logger = { logMessages += it }
    }

    @Test
    fun hundredConsecutiveFailedRecordsOnlyLogOncePerMinute() {
        val id = DiagnosticIds.STATUSBAR_CLOCK_TWEAK
        repeat(100) {
            DiagnosticRecorder.record(id, DiagnosticState.FAILED, message = "failure $it")
        }

        assertEquals(1, logMessages.size)

        val snapshot = DiagnosticRecorder.summarize()[id]
        assertNotNull(snapshot)
        assertEquals(100L, snapshot!!.count)
        assertEquals(DiagnosticState.FAILED, snapshot.state)
    }

    @Test
    fun stateTransitionsRequestedCompatibleInstalledDegradedAreLogged() {
        val id = DiagnosticIds.STEP_COUNTER
        DiagnosticRecorder.record(id, DiagnosticState.REQUESTED)
        DiagnosticRecorder.record(id, DiagnosticState.COMPATIBLE)
        DiagnosticRecorder.record(id, DiagnosticState.INSTALLED)
        DiagnosticRecorder.record(id, DiagnosticState.DEGRADED)

        assertEquals(
            listOf(
                "Diagnostic [STEP_COUNTER] REQUESTED",
                "Diagnostic [STEP_COUNTER] COMPATIBLE",
                "Diagnostic [STEP_COUNTER] INSTALLED",
                "Diagnostic [STEP_COUNTER] DEGRADED"
            ),
            logMessages
        )

        val snapshot = DiagnosticRecorder.summarize()[id]
        assertNotNull(snapshot)
        assertEquals(4L, snapshot!!.count)
        assertEquals(DiagnosticState.DEGRADED, snapshot.state)
    }

    @Test
    fun summarizeReturnsExpectedCounts() {
        val id1 = DiagnosticIds.DEVICE_INFO_MONITOR
        val id2 = DiagnosticIds.PACKAGE_PERMISSIONS

        DiagnosticRecorder.record(id1, DiagnosticState.REQUESTED)
        DiagnosticRecorder.record(id1, DiagnosticState.REQUESTED)
        DiagnosticRecorder.record(id1, DiagnosticState.COMPATIBLE)

        DiagnosticRecorder.record(id2, DiagnosticState.FAILED, message = "bad")
        DiagnosticRecorder.record(id2, DiagnosticState.FAILED, message = "bad2")

        val summary = DiagnosticRecorder.summarize()

        assertEquals(3L, summary[id1]?.count)
        assertEquals(DiagnosticState.COMPATIBLE, summary[id1]?.state)

        assertEquals(2L, summary[id2]?.count)
        assertEquals(DiagnosticState.FAILED, summary[id2]?.state)
    }
}
