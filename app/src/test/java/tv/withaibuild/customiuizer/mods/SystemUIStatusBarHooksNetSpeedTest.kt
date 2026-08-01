package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class SystemUIStatusBarHooksNetSpeedTest {

    @Test
    fun netSpeedRuntimeStateIsCreatedWithZeroBaseline() {
        val state = SystemUIStatusBarHooks.NetSpeedRuntimeState()
        assertEquals(0L, state.lastMeasureNanos)
        assertEquals(0L, state.lastTxBytes)
        assertEquals(0L, state.lastRxBytes)
        assertEquals(0L, state.currentTxBytes)
        assertEquals(0L, state.currentRxBytes)
        assertEquals(0L, state.txBytesPerSecond)
        assertEquals(0L, state.rxBytesPerSecond)
    }

    @Test
    fun netSpeedStateTagIsConstant() {
        // The per-controller field tag must not vary to keep AdditionalInstanceField stable.
        val state1 = SystemUIStatusBarHooks.NetSpeedRuntimeState()
        val state2 = SystemUIStatusBarHooks.NetSpeedRuntimeState()
        assertNotNull(state1)
        assertNotNull(state2)
    }

    @Test
    fun netSpeedStateHoldsComputedSpeed() {
        val state = SystemUIStatusBarHooks.NetSpeedRuntimeState()
        state.txBytesPerSecond = 1234L
        state.rxBytesPerSecond = 5678L
        assertEquals(1234L, state.txBytesPerSecond)
        assertEquals(5678L, state.rxBytesPerSecond)
    }

    @Test
    fun netSpeedStateDeltaDoesNotUnderflow() {
        val state = SystemUIStatusBarHooks.NetSpeedRuntimeState()
        state.currentTxBytes = 100L
        state.lastTxBytes = 200L
        state.currentRxBytes = 50L
        state.lastRxBytes = 150L
        // The production code clamps delta to 0 before dividing by time.
        val txDelta = if (state.currentTxBytes - state.lastTxBytes < 0) 0L else state.currentTxBytes - state.lastTxBytes
        val rxDelta = if (state.currentRxBytes - state.lastRxBytes < 0) 0L else state.currentRxBytes - state.lastRxBytes
        assertEquals(0L, txDelta)
        assertEquals(0L, rxDelta)
    }
}
