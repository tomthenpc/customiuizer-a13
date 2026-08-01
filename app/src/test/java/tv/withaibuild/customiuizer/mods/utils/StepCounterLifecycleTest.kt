package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCounterLifecycleTest {

    @Test
    fun addViewThenScreenOnRegistersTimeTickAndCanSchedule() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        assertTrue(lifecycle.registerTimeTick())
        assertTrue(lifecycle.canSchedule())
        assertTrue(lifecycle.timeTickRegistered)
    }

    @Test
    fun repeatedRegisterTimeTickIsIdempotent() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        assertTrue(lifecycle.registerTimeTick())
        assertTrue(lifecycle.registerTimeTick())
        assertTrue(lifecycle.registerTimeTick())

        assertTrue(lifecycle.timeTickRegistered)
    }

    @Test
    fun repeatedStartQueryDoesNotProduceDuplicateTasks() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()
        assertNotNull(t1)
        assertNull(lifecycle.tryStartQuery())
        assertNull(lifecycle.tryStartQuery())
    }

    @Test
    fun startQueryReturnsTicketWithIdentity() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()!!
        assertTrue(lifecycle.isCurrent(t1))
        assertTrue(lifecycle.isQuerying)
        assertFalse(lifecycle.canSchedule())

        val finished = lifecycle.finishQuery(t1)
        assertTrue(finished)
        assertFalse(lifecycle.isQuerying)
        assertTrue(lifecycle.canSchedule())
        assertFalse(lifecycle.isCurrent(t1))
    }

    @Test
    fun finishQueryOnlyReleasesOwnTicket() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()!!
        val clearedByWrong = lifecycle.finishQuery(StepCounterController.QueryTicket(t1.generation, t1.queryId - 1))
        assertFalse(clearedByWrong)
        assertTrue(lifecycle.isQuerying)
        assertTrue(lifecycle.isCurrent(t1))
    }

    @Test
    fun resetInvalidatesActiveTicket() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()!!
        lifecycle.reset()

        assertFalse(lifecycle.isCurrent(t1))
        assertFalse(lifecycle.isQuerying)
    }

    @Test
    fun screenOffStopsSchedulingAndInvalidatesActiveTicket() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()
        lifecycle.registerTimeTick()
        val t1 = lifecycle.tryStartQuery()!!

        lifecycle.onScreenOff()

        assertFalse(lifecycle.screenOn)
        assertFalse(lifecycle.timeTickRegistered)
        assertFalse(lifecycle.canSchedule())
        assertNull(lifecycle.tryStartQuery())
        assertFalse(lifecycle.isCurrent(t1))
    }

    @Test
    fun screenOnRestoresScheduleAndRegisterTimeTickOnce() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOff()

        assertFalse(lifecycle.registerTimeTick())

        lifecycle.onScreenOn()

        assertTrue(lifecycle.registerTimeTick())
        assertTrue(lifecycle.canSchedule())
        assertTrue(lifecycle.registerTimeTick())
        assertTrue(lifecycle.timeTickRegistered)
    }

    @Test
    fun removeAllViewsStopsTimeTickAndSchedule() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()
        lifecycle.registerTimeTick()
        lifecycle.tryStartQuery()

        lifecycle.setHasViews(false)

        assertFalse(lifecycle.hasViews)
        assertFalse(lifecycle.timeTickRegistered)
        assertFalse(lifecycle.canSchedule())
        assertNull(lifecycle.tryStartQuery())
    }

    @Test
    fun unregisterTimeTickIsIdempotent() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()
        lifecycle.registerTimeTick()

        lifecycle.unregisterTimeTick()
        lifecycle.unregisterTimeTick()
        lifecycle.unregisterTimeTick()

        assertFalse(lifecycle.timeTickRegistered)
    }

    @Test
    fun generationBumpsOnResetAndBump() {
        val lifecycle = StepCounterController.Lifecycle()
        val gen1 = lifecycle.generation

        lifecycle.bumpGeneration()
        val gen2 = lifecycle.generation

        assertNotEquals(gen1, gen2)
        assertEquals(gen2, gen1 + 1)

        lifecycle.reset()
        val gen3 = lifecycle.generation

        assertNotEquals(gen2, gen3)
        assertEquals(gen3, gen2 + 1)
    }

    @Test
    fun isCurrentReflectsLastGeneration() {
        val lifecycle = StepCounterController.Lifecycle()
        val gen = lifecycle.generation

        assertTrue(lifecycle.isCurrent(gen))
        assertFalse(lifecycle.isCurrent(gen + 1))

        lifecycle.bumpGeneration()
        assertFalse(lifecycle.isCurrent(gen))
    }

    @Test
    fun queryIdStrictlyIncreasing() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()!!
        lifecycle.finishQuery(t1)
        val t2 = lifecycle.tryStartQuery()!!

        assertTrue(t2.queryId > t1.queryId)
    }
}
