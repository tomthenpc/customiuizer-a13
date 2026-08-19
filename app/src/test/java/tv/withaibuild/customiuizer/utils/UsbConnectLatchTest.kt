package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbConnectLatchTest {

    private data class Step(
        val connected: Boolean,
        val applySucceeded: Boolean = true,
    )

    private data class Run(val latched: Boolean, val applyCount: Int)

    /**
     * Mirrors [tv.withaibuild.customiuizer.mods.SystemSettingsMoreHooks] USB_STATE
     * latch commit: disconnect/duplicate connected skip apply; rising-edge
     * early-exit leaves the latch unset.
     */
    private fun run(steps: List<Step>): Run {
        var latched = false
        var applyCount = 0
        for (step in steps) {
            if (!UsbConnectLatch.shouldAttemptApply(latched, step.connected)) {
                latched = UsbConnectLatch.nextLatch(latched, step.connected, false)
                continue
            }
            applyCount++
            if (step.applySucceeded) {
                latched = UsbConnectLatch.nextLatch(latched, step.connected, true)
            }
        }
        return Run(latched, applyCount)
    }

    @Test
    fun disconnectedToConnectedAppliesOnce() {
        val result = run(listOf(Step(connected = true)))
        assertEquals(1, result.applyCount)
        assertTrue(result.latched)
        assertTrue(UsbConnectLatch.shouldAttemptApply(false, true))
    }

    @Test
    fun connectedToConnectedDoesNotReapply() {
        val result = run(listOf(Step(connected = true), Step(connected = true)))
        assertEquals(1, result.applyCount)
        assertTrue(result.latched)
        assertFalse(UsbConnectLatch.shouldAttemptApply(true, true))
    }

    @Test
    fun connectedToDisconnectedResetsLatch() {
        val afterConnect = run(listOf(Step(connected = true)))
        assertTrue(afterConnect.latched)
        val afterDisconnect = run(
            listOf(Step(connected = true), Step(connected = false))
        )
        assertEquals(1, afterDisconnect.applyCount)
        assertFalse(afterDisconnect.latched)
        assertFalse(UsbConnectLatch.nextLatch(true, false, false))
    }

    @Test
    fun disconnectedToConnectedAfterDisconnectAppliesAgain() {
        val result = run(
            listOf(
                Step(connected = true),
                Step(connected = false),
                Step(connected = true),
            )
        )
        assertEquals(2, result.applyCount)
        assertTrue(result.latched)
        assertTrue(UsbConnectLatch.shouldAttemptApply(false, true))
    }

    @Test
    fun replugMtpSequenceYieldsTwoApplyOpportunities() {
        val result = run(
            listOf(
                Step(connected = true),
                Step(connected = false),
                Step(connected = true),
            )
        )
        assertEquals(2, result.applyCount)
        assertTrue(result.latched)
    }

    @Test
    fun earlyExitLeavesLatchUnsetForRetry() {
        val result = run(
            listOf(
                Step(connected = true, applySucceeded = false),
                Step(connected = true, applySucceeded = true),
            )
        )
        assertEquals(2, result.applyCount)
        assertTrue(result.latched)
    }
}
