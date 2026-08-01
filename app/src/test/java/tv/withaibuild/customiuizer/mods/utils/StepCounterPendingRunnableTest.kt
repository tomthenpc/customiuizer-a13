package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StepCounterPendingRunnableTest {

    @Test
    fun getAndSetReplacesPendingAndReturnsOld() {
        val slot = java.util.concurrent.atomic.AtomicReference<Runnable?>(null)
        val a = Runnable {}
        val b = Runnable {}

        slot.set(a)
        val old = slot.getAndSet(b)

        assertSame(a, old)
        assertSame(b, slot.get())
    }

    @Test
    fun clearByIdentityLeavesNewPending() {
        val slot = java.util.concurrent.atomic.AtomicReference<Runnable?>(null)
        val a = Runnable {}
        val b = Runnable {}

        slot.set(a)
        slot.getAndSet(b)

        // Simulating old A's finally: compareAndSet only clears if the slot is still A.
        val clearedByA = slot.compareAndSet(a, null)
        assertSame(false, clearedByA)
        assertSame(b, slot.get())
    }

    @Test
    fun clearByOwnIdentityRemovesPending() {
        val slot = java.util.concurrent.atomic.AtomicReference<Runnable?>(null)
        val a = Runnable {}
        val b = Runnable {}

        slot.set(a)
        slot.getAndSet(b)
        slot.compareAndSet(a, null) // old A cannot clear

        // B clears itself
        val clearedByB = slot.compareAndSet(b, null)
        assertSame(true, clearedByB)
        assertNull(slot.get())
    }

    @Test
    fun getAndSetNullReplacesAndReturnsCurrentPending() {
        val slot = java.util.concurrent.atomic.AtomicReference<Runnable?>(null)
        val a = Runnable {}

        slot.set(a)
        val old = slot.getAndSet(null)

        assertSame(a, old)
        assertNull(slot.get())
    }
}
