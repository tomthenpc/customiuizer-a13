package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecondTickerStateTest {

    @Test
    fun repeatedStartDoesNotAllowOldGenerationToRePost() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()

        state.start(1L)
        assertTrue(state.shouldRePost(1L))

        state.start(2L)
        assertFalse(state.shouldRePost(1L))
        assertTrue(state.shouldRePost(2L))
    }

    @Test
    fun stopAfterStartPreventsRePost() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()

        state.start(10L)
        assertTrue(state.shouldRePost(10L))

        state.stop()
        assertFalse(state.shouldRePost(10L))
        assertFalse(state.running)
        assertEquals(0L, state.generation)
    }

    @Test
    fun screenOffStopsScheduling() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()

        state.start(20L)
        assertTrue(state.shouldRePost(20L))

        state.setScreen(false)
        assertFalse(state.shouldRePost(20L))
        assertFalse(state.running)
    }

    @Test
    fun screenOnResumesWithNewGeneration() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()

        state.setScreen(false)
        state.start(30L)
        assertFalse(state.shouldRePost(30L))

        state.setScreen(true)
        state.start(31L)
        assertTrue(state.shouldRePost(31L))
    }

    @Test
    fun oldGenerationCannotRePostAfterRestart() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()

        state.start(5L)
        state.start(6L)

        assertFalse(state.shouldRePost(5L))
        assertTrue(state.shouldRePost(6L))
    }

    @Test
    fun stopIsIdempotent() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()

        state.stop()
        state.stop()
        state.stop()

        assertFalse(state.running)
        assertEquals(0L, state.generation)
        assertFalse(state.shouldRePost(0L))
    }

    @Test
    fun screenOnAndOffCycleKeepsGenerationConsistent() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()

        state.start(42L)
        assertTrue(state.shouldRePost(42L))

        state.setScreen(false)
        assertFalse(state.shouldRePost(42L))

        state.setScreen(true)
        assertFalse(state.shouldRePost(42L))

        state.start(43L)
        assertTrue(state.shouldRePost(43L))
    }
}
