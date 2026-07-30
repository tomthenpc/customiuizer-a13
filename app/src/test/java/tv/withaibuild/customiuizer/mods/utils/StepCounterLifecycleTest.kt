package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertFalse
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

        assertTrue(lifecycle.tryStartQuery())
        assertFalse(lifecycle.tryStartQuery())
        assertFalse(lifecycle.tryStartQuery())

        lifecycle.finishQuery()
        assertTrue(lifecycle.tryStartQuery())
    }

    @Test
    fun screenOffStopsSchedulingAndUnregistersTimeTick() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()
        lifecycle.registerTimeTick()
        lifecycle.tryStartQuery()

        lifecycle.onScreenOff()

        assertFalse(lifecycle.screenOn)
        assertFalse(lifecycle.timeTickRegistered)
        assertFalse(lifecycle.canSchedule())
        assertFalse(lifecycle.tryStartQuery())

        // Simulate the in-flight query completing while the screen is still off.
        lifecycle.finishQuery()
        assertFalse(lifecycle.isQuerying)
        assertFalse(lifecycle.canSchedule())
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
        assertFalse(lifecycle.tryStartQuery())
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
    fun finishQueryReleasesTheSlot() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        assertTrue(lifecycle.tryStartQuery())
        assertFalse(lifecycle.canSchedule())

        lifecycle.finishQuery()
        assertTrue(lifecycle.canSchedule())
    }
}
