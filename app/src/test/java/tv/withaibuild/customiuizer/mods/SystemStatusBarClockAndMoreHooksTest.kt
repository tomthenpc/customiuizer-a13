package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemStatusBarClockAndMoreHooksTest {

    @Test
    fun secondTickerState_startsAndRepostsWhenScreenOn() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()
        val gen = 42L
        state.start(gen)
        assertTrue(state.shouldRePost(gen))
    }

    @Test
    fun secondTickerState_stopsAfterScreenOff() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()
        val gen = 42L
        state.start(gen)
        state.setScreen(false)
        assertFalse(state.shouldRePost(gen))
    }

    @Test
    fun secondTickerState_newGenerationInvalidatesOldRunnable() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()
        val oldGen = 1L
        state.start(oldGen)
        val newGen = 2L
        state.start(newGen)
        assertFalse(state.shouldRePost(oldGen))
        assertTrue(state.shouldRePost(newGen))
    }

    @Test
    fun secondTickerState_stopInvalidatesRepost() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()
        val gen = 42L
        state.start(gen)
        state.stop()
        assertFalse(state.shouldRePost(gen))
    }

    @Test
    fun secondTickerState_screenOffThenOnWithNewGeneration() {
        val state = SystemStatusBarClockAndMoreHooks.SecondTickerState()
        val first = 1L
        state.start(first)
        state.setScreen(false)
        state.setScreen(true)
        val second = 2L
        state.start(second)
        assertTrue(state.shouldRePost(second))
        assertFalse(state.shouldRePost(first))
    }
}
