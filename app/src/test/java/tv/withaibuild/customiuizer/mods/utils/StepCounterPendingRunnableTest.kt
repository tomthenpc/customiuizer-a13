package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCounterPendingRunnableTest {

    private val slot = StepCounterController.PendingQuerySlot()

    @Test
    fun replaceReturnsPreviousAndStoresNew() {
        val a = Runnable {}
        val b = Runnable {}

        slot.replace(a)
        val old = slot.replace(b)

        assertSame(a, old)
        assertSame(b, slot.peek())
    }

    @Test
    fun clearByIdentityLeavesNewPending() {
        val a = Runnable {}
        val b = Runnable {}

        slot.replace(a)
        slot.replace(b)

        val clearedByA = slot.clear(a)
        assertFalse(clearedByA)
        assertSame(b, slot.peek())
    }

    @Test
    fun clearByOwnIdentityRemovesPending() {
        val a = Runnable {}
        val b = Runnable {}

        slot.replace(a)
        slot.replace(b)

        assertFalse(slot.clear(a))
        assertTrue(slot.clear(b))
        assertNull(slot.peek())
    }

    @Test
    fun takeReturnsAndEmptiesSlot() {
        val a = Runnable {}

        slot.replace(a)
        val taken = slot.take()

        assertSame(a, taken)
        assertNull(slot.peek())
    }

    @Test
    fun takeIsIdempotent() {
        val a = Runnable {}

        slot.replace(a)
        assertSame(a, slot.take())
        assertNull(slot.take())
        assertNull(slot.peek())
    }
}
