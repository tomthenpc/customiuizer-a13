package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.ClockRunnable
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.SecondTickerState
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.TickerScheduler
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.scheduleTicker
import java.lang.ref.WeakReference

class SystemStatusBarClockAndMoreHooksTest {

    class FakeTickerScheduler : TickerScheduler {
        val pending = ArrayList<Runnable>()
        var failNextPost = false
        var throwNext: Throwable? = null

        override fun postDelayed(runnable: Runnable, delayMillis: Long): Boolean {
            val t = throwNext
            if (t != null) {
                throwNext = null
                throw t
            }
            if (failNextPost) {
                failNextPost = false
                return false
            }
            pending.add(runnable)
            return true
        }

        override fun removeCallbacks(runnable: Runnable) {
            pending.removeAll { it == runnable }
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
        private val onTick: (ClockRunnable) -> Unit = {}
    ) : ClockRunnable(
        scheduledGen,
        WeakReference(Any()),
        WeakReference(state),
        scheduler
    ) {
        override fun doTick() = onTick(this)
    }

    @Test
    fun secondTickerState_startsAndRepostsWhenScreenOn() {
        val state = SecondTickerState()
        val gen = 42L
        state.start(gen)
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
        assertFalse(state.canRePost(oldGen))
        assertTrue(state.canRePost(newGen))
    }

    @Test
    fun secondTickerState_stopInvalidatesRepost() {
        val state = SecondTickerState()
        val gen = 42L
        state.start(gen)
        state.stop()
        assertFalse(state.canRePost(gen))
    }

    @Test
    fun scheduleTicker_postsAndMarksPending() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)

        val ok = scheduleTicker(state, scheduler, runnable, gen, 1000)

        assertTrue(ok)
        assertTrue(state.callbackPending)
        assertEquals(1, scheduler.pendingCount())
    }

    @Test
    fun scheduleTicker_postFailureRollsBackPending() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        scheduler.failNextPost = true
        val runnable = TestClockRunnable(gen, state, scheduler)

        val ok = scheduleTicker(state, scheduler, runnable, gen, 1000)

        assertFalse(ok)
        assertFalse(state.callbackPending)
        assertEquals(0, scheduler.pendingCount())
    }

    @Test
    fun scheduleTicker_postExceptionRollsBackPending() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        scheduler.throwNext = RuntimeException("post failed")
        val runnable = TestClockRunnable(gen, state, scheduler)

        val ok = scheduleTicker(state, scheduler, runnable, gen, 1000)

        assertFalse(ok)
        assertFalse(state.callbackPending)
        assertEquals(0, scheduler.pendingCount())
    }

    @Test
    fun scheduleTicker_postOutOfMemoryRethrowsAndRollsBackPending() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        scheduler.throwNext = OutOfMemoryError("post oom")

        assertThrows(OutOfMemoryError::class.java) {
            scheduleTicker(state, scheduler, TestClockRunnable(gen, state, scheduler), gen, 1000)
        }

        assertFalse(state.callbackPending)
        assertEquals(0, scheduler.pendingCount())
    }

    @Test
    fun scheduleTicker_rejectsSameGenerationDoublePost() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)

        assertTrue(scheduleTicker(state, scheduler, runnable, gen, 1000))
        assertFalse(scheduleTicker(state, scheduler, runnable, gen, 1000))

        assertEquals(1, scheduler.pendingCount())
    }

    @Test
    fun scheduleTicker_rejectsOldGenerationAfterRestart() {
        val state = SecondTickerState()
        val gen1 = 100L
        state.start(gen1)
        val gen2 = 200L
        state.start(gen2)
        val scheduler = FakeTickerScheduler()
        val oldRunnable = TestClockRunnable(gen1, state, scheduler)

        assertFalse(scheduleTicker(state, scheduler, oldRunnable, gen1, 1000))
        assertEquals(0, scheduler.pendingCount())
    }

    @Test
    fun scheduleTicker_doesNotDedupeInScheduler() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)

        // FakeScheduler does not dedupe; but state does.
        scheduler.postDelayed(runnable, 100)
        scheduler.postDelayed(runnable, 100)

        assertEquals(2, scheduler.pendingCount())
    }

    @Test
    fun clockRunnable_onePendingAfterRun() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)
        scheduleTicker(state, scheduler, runnable, gen, 1000)

        scheduler.runCurrent()

        assertEquals(1, scheduler.pendingCount())
        assertTrue(state.callbackPending)
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
        scheduleTicker(state, scheduler, newRunnable, gen2, 1000)

        // Old runnable was never posted; running it should not post.
        oldRunnable.run()

        assertEquals(1, scheduler.pendingCount())
        assertTrue(scheduler.pending.contains(newRunnable))
    }

    @Test
    fun clockRunnable_restartsWhileRunningKeepsOnePending() {
        val state = SecondTickerState()
        val gen1 = 100L
        state.start(gen1)
        val scheduler = FakeTickerScheduler()
        val gen2 = 200L
        val newRunnable = TestClockRunnable(gen2, state, scheduler)

        val oldRunnable = TestClockRunnable(gen1, state, scheduler) {
            // Simulate a TIME_SET / SCREEN_ON restart in the middle of a tick.
            state.start(gen2)
            scheduleTicker(state, scheduler, newRunnable, gen2, 1000)
        }
        scheduleTicker(state, scheduler, oldRunnable, gen1, 1000)

        scheduler.runCurrent()

        assertEquals(1, scheduler.pendingCount())
        assertTrue(scheduler.pending.contains(newRunnable))
    }

    @Test
    fun clockRunnable_stopsWhileRunningReleasesPending() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler) { self ->
            // Simulate SCREEN_OFF in the middle of a tick.
            state.stop()
            scheduler.removeCallbacks(self)
        }
        scheduleTicker(state, scheduler, runnable, gen, 1000)

        scheduler.runCurrent()

        assertEquals(0, scheduler.pendingCount())
        assertFalse(state.callbackPending)
    }

    @Test
    fun clockRunnable_screenOffDuringRunPreventsRepost() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler) { self ->
            // simulate stopSecondTimer invoked by SCREEN_OFF
            state.stop()
            scheduler.removeCallbacks(self)
        }
        scheduleTicker(state, scheduler, runnable, gen, 1000)

        scheduler.runCurrent()

        assertEquals(0, scheduler.pendingCount())
        assertFalse(state.callbackPending)
    }

    @Test
    fun clockRunnable_outOfMemoryInDoTickRethrows() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler) {
            throw OutOfMemoryError("tick oom")
        }
        scheduleTicker(state, scheduler, runnable, gen, 1000)

        assertThrows(OutOfMemoryError::class.java) {
            runnable.run()
        }

        assertFalse(state.callbackPending)
        // The originally posted callback remains in the scheduler; no extra one is added.
        assertEquals(1, scheduler.pendingCount())
    }

    @Test
    fun clockRunnable_stateClearedDuringRunDoesNotPost() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()

        var capturedRunnable: Runnable? = null
        val runnable = TestClockRunnable(gen, state, scheduler) {
            // Simulate state being garbage collected between start and end.
            // We can't really null the WeakReference, but we can stop the state.
            state.stop()
        }
        scheduleTicker(state, scheduler, runnable, gen, 1000)

        scheduler.runCurrent()

        assertEquals(0, scheduler.pendingCount())
    }

    @Test
    fun clockRunnable_oneHundredTicksKeepsOnePending() {
        val state = SecondTickerState()
        val gen = 100L
        state.start(gen)
        val scheduler = FakeTickerScheduler()
        val runnable = TestClockRunnable(gen, state, scheduler)
        scheduleTicker(state, scheduler, runnable, gen, 1000)

        repeat(100) {
            assertEquals(1, scheduler.runCurrent())
            assertEquals(1, scheduler.pendingCount())
            assertTrue(state.callbackPending)
            assertTrue(state.running)
        }
    }
}
