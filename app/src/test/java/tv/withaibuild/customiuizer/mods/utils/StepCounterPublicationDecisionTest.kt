package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCounterPublicationDecisionTest {

    @Test
    fun canPublishGatesPublicationCallback() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()!!
        lifecycle.finishQuery(t1)

        var published = false
        if (lifecycle.canPublish(t1)) {
            published = true
            lifecycle.consumeResult(t1)
        }
        assertTrue(published)

        // New query replaces the publication window.
        val t2 = lifecycle.tryStartQuery()!!
        lifecycle.finishQuery(t2)

        published = false
        if (lifecycle.canPublish(t1)) {
            published = true
        }
        assertFalse("t1 must not be allowed to publish after t2 starts", published)
    }

    @Test
    fun screenOffPreventsPublicationCallback() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()!!
        lifecycle.finishQuery(t1)
        lifecycle.onScreenOff()

        var published = false
        if (lifecycle.canPublish(t1)) {
            published = true
        }
        assertFalse(published)
    }

    @Test
    fun removeViewsPreventsPublicationCallback() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val t1 = lifecycle.tryStartQuery()!!
        lifecycle.finishQuery(t1)
        lifecycle.setHasViews(false)

        var published = false
        if (lifecycle.canPublish(t1)) {
            published = true
        }
        assertFalse(published)
    }
}
