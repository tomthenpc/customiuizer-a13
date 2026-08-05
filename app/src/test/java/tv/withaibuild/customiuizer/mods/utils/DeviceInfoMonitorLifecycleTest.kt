package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.utils.PrefMap

class DeviceInfoMonitorLifecycleTest {

    @Test
    fun disabledFeatureDoesNotStartTicker() {
        val state = DeviceInfoMonitor.LifecycleState()

        assertFalse(state.start(enabled = false, interactive = true))
        assertFalse(state.canSchedule())
    }

    @Test
    fun screenOffSuspendsAndScreenOnRequestsImmediateTick() {
        val state = DeviceInfoMonitor.LifecycleState()
        assertTrue(state.start(enabled = true, interactive = true))

        state.onScreenOff()
        assertFalse(state.canSchedule())

        assertTrue(state.onScreenOn())
        assertTrue(state.canSchedule())
    }

    @Test
    fun startWhileScreenIsOffWaitsForScreenOn() {
        val state = DeviceInfoMonitor.LifecycleState()

        assertFalse(state.start(enabled = true, interactive = false))
        assertTrue(state.onScreenOn())
        assertTrue(state.canSchedule())
    }

    @Test
    fun failuresBackOffToBoundAndSuccessRestoresBaseDelay() {
        val state = DeviceInfoMonitor.LifecycleState()
        state.start(enabled = true, interactive = true)

        assertEquals(4_000L, state.recordRead(success = false))
        assertEquals(8_000L, state.recordRead(success = false))
        repeat(10) { state.recordRead(success = false) }
        assertEquals(DeviceInfoMonitor.MAX_DELAY_MS, state.recordRead(success = false))
        assertEquals(DeviceInfoMonitor.BASE_DELAY_MS, state.recordRead(success = true))
        assertEquals(0, state.consecutiveFailures)
    }

    @Test
    fun repeatedStopIsIdempotent() {
        val state = DeviceInfoMonitor.LifecycleState()
        state.start(enabled = true, interactive = true)
        state.recordRead(success = false)

        state.stop()
        state.stop()

        assertFalse(state.running)
        assertFalse(state.screenOn)
        assertEquals(0, state.consecutiveFailures)
    }

    @Test
    fun currentTickRequiresMatchingGenerationSnapshotAndScheduleState() {
        val snapshot = DeviceInfoMonitor.readSnapshot(PrefMap())

        assertTrue(DeviceInfoMonitor.isCurrentTick(3, 3, snapshot, snapshot, true))
        assertFalse(DeviceInfoMonitor.isCurrentTick(2, 3, snapshot, snapshot, true))
        assertFalse(
            DeviceInfoMonitor.isCurrentTick(
                3,
                3,
                snapshot,
                snapshot.copy(batteryPositive = !snapshot.batteryPositive),
                true
            )
        )
        assertFalse(DeviceInfoMonitor.isCurrentTick(3, 3, snapshot, snapshot, false))
    }

    @Test
    fun screenOffRejectsCurrentTickUntilScreenOn() {
        val state = DeviceInfoMonitor.LifecycleState()
        val snapshot = DeviceInfoMonitor.readSnapshot(PrefMap())

        state.start(enabled = true, interactive = true)
        assertTrue(state.canSchedule())
        assertTrue(DeviceInfoMonitor.isCurrentTick(1, 1, snapshot, snapshot, state.canSchedule()))

        state.onScreenOff()
        assertFalse(state.canSchedule())
        assertFalse(DeviceInfoMonitor.isCurrentTick(1, 1, snapshot, snapshot, state.canSchedule()))

        assertTrue(state.onScreenOn())
        assertTrue(state.canSchedule())
        assertTrue(DeviceInfoMonitor.isCurrentTick(1, 1, snapshot, snapshot, state.canSchedule()))
    }

    @Test
    fun stopRejectsCurrentTick() {
        val state = DeviceInfoMonitor.LifecycleState()
        val snapshot = DeviceInfoMonitor.readSnapshot(PrefMap())

        state.start(enabled = true, interactive = true)
        assertTrue(DeviceInfoMonitor.isCurrentTick(1, 1, snapshot, snapshot, state.canSchedule()))

        state.stop()
        assertFalse(DeviceInfoMonitor.isCurrentTick(1, 1, snapshot, snapshot, state.canSchedule()))
    }
}
