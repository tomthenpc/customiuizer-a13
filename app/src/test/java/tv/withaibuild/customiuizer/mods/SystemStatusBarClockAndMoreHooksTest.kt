package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.ClockRunnable
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.SecondTickerState
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.TickerScheduler
import java.lang.ref.WeakReference

class SystemStatusBarClockAndMoreHooksTest {

    class FakeTickerScheduler : TickerScheduler {
        val pending = ArrayList<Runnable>()

        override fun postDelayed(runnable: Runnable, delayMillis: Long) {
            if (!pending.contains(runnable)) pending.add(runnable)
        }

        override fun removeCallbacks(runnable: Runnable) {
            pending.remove(runnable)
        }

        fun runCurrent(): Int {
            if (pending.isEmpty()) return 0
            val r = pending.removeAt(0)
            r.run()
            return 1
        }

        fun pendingCount(): Int = pending.size
    }

    private class TestClockRunnable(
        scheduledGen: Long,
        state: SecondTickerState,
        scheduler: TickerScheduler,
        private val onTick: () -> Unit = {}
    ) : ClockRunnable(
        scheduledGen,
        WeakReference(Any()),
        WeakReference(state),
        scheduler
    ) {
        override fun doTick() = onTick()
    }

    @Test
    fun secondTickerState_startsAndRepostsWhenScreenOn() {
        val state = SecondTickerState()
        val gen = 42L
        state.start(gen)
        state.markCallbackPending(false)
        assertTrue(state.canRePost(gen))
    }

    @Test
    fun secondTickerState_callbackPendingPreventsRepost() {
        val state = SecondTickerState()
        val gen = 42L
        state.start(gen)
        state.markCallbackPending(true)
        assertFalse(state.canRePost(gen))
    }

    @Test
    fun secondTickerState_stopsAfterScreenOff() {
        val state = SecondTickerState()
        val gen = 42L
        state.start(gen)
        state.markCallbackPending(false)
        state.setScreen(false)
        assertFalse(state.canRePost(gen))
    }

    @Test
    fun secondTickerState_newGenerationInvalidatesOldRunnable() {
        val state = SecondTickerState()
        val oldGen = 1L
        state.start(oldGen)
        val newGen = 2L
        state.start(newGen)
        state.markCallbackPending(false)
        assertFalse(state.canRePost(oldGen))
        assertTrue(state.canRePost(newGen))
    }

    @Test
    fun secondTickerState_stopInvalidatesRepost() {
        val state = SecondTickerState()
        val gen = 42L
        state.start(gen)
        state.markCallbackPending(false)
        state.stop()
        assertFalse(state.canRePost(gen))
    }

    @Test
    fun clockRunnable_onePendingAfterRun() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)

        runnable.run()

        assertEquals(1, scheduler.pendingCount())
    }

    @Test
    fun clockRunnable_oldRunnableDoesNotRepostAfterRestart() {
        val state = SecondTickerState()
        val gen1 = 100L
        state.start(gen1)
        val scheduler = FakeTickerScheduler()
        val oldRunnable = TestClockRunnable(gen1, state, scheduler)

        val gen2 = 200L
        state.start(gen2)
        val newRunnable = TestClockRunnable(gen2, state, scheduler)

        oldRunnable.run()
        newRunnable.run()

        assertEquals(1, scheduler.pendingCount())
    }

    @Test
    fun clockRunnable_stopPreventsRepost() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)

        state.stop()
        runnable.run()

        assertEquals(0, scheduler.pendingCount())
    }

    @Test
    fun clockRunnable_screenOffDuringRunPreventsRepost() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler) {
            state.setScreen(false)
        }

        runnable.run()

        assertEquals(0, scheduler.pendingCount())
    }

    @Test
    fun clockRunnable_chainOfRunsKeepsExactlyOnePending() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)

        scheduler.postDelayed(runnable, 1000)
        repeat(20) {
            assertEquals(1, scheduler.runCurrent())
            assertEquals(1, scheduler.pendingCount())
        }
    }

}
