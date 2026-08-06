package tv.withaibuild.customiuizer.mods

import android.app.Activity
import android.content.Context
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherHotSeatGestureStateTest {

    @Test
    fun initialDensityDpiIsIntMinValue() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        assertEquals(Int.MIN_VALUE, state.densityDpi)
    }

    @Test
    fun firstCallUpdatesAllThresholds() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        val changed = state.updateThresholdsIfNeeded(440, 2.75f, 24)
        assertTrue(changed)
        assertEquals(440, state.densityDpi)
        assertEquals(Math.round(75f * 2.75f), state.minDistance)
        assertEquals(Math.round(33f * 2.75f), state.velocityThreshold)
        assertEquals(24, state.touchSlop)
    }

    @Test
    fun minDistanceIs75dp() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.updateThresholdsIfNeeded(320, 2.0f, 10)
        assertEquals(150, state.minDistance)
    }

    @Test
    fun velocityThresholdIs33dpPerSecond() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.updateThresholdsIfNeeded(320, 2.0f, 10)
        assertEquals(66, state.velocityThreshold)
    }

    @Test
    fun touchSlopStoredUnchanged() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.updateThresholdsIfNeeded(320, 2.0f, 42)
        assertEquals(42, state.touchSlop)
    }

    @Test
    fun sameDensityDpiReturnsFalse() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.updateThresholdsIfNeeded(320, 2.0f, 10)
        val changed = state.updateThresholdsIfNeeded(320, 3.0f, 99)
        assertFalse(changed)
    }

    @Test
    fun sameDensityDpiDoesNotOverwriteThresholds() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.updateThresholdsIfNeeded(320, 2.0f, 10)
        state.updateThresholdsIfNeeded(320, 3.0f, 99)
        assertEquals(Math.round(75f * 2.0f), state.minDistance)
        assertEquals(Math.round(33f * 2.0f), state.velocityThreshold)
        assertEquals(10, state.touchSlop)
    }

    @Test
    fun differentDensityDpiReturnsTrue() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.updateThresholdsIfNeeded(320, 2.0f, 10)
        val changed = state.updateThresholdsIfNeeded(480, 3.0f, 20)
        assertTrue(changed)
    }

    @Test
    fun differentDensityDpiRecalculatesThresholds() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.updateThresholdsIfNeeded(320, 2.0f, 10)
        state.updateThresholdsIfNeeded(480, 3.0f, 20)
        assertEquals(480, state.densityDpi)
        assertEquals(Math.round(75f * 3.0f), state.minDistance)
        assertEquals(Math.round(33f * 3.0f), state.velocityThreshold)
        assertEquals(20, state.touchSlop)
    }

    @Test
    fun downXyTimeCanBeSaved() {
        val state = LauncherGestureHooks.HotSeatGestureState()
        state.downX = 12.5f
        state.downY = 34.7f
        state.downTime = 12345678L
        assertEquals(12.5f, state.downX, 0.0f)
        assertEquals(34.7f, state.downY, 0.0f)
        assertEquals(12345678L, state.downTime)
    }

    @Test
    fun twoInstancesAreIndependent() {
        val a = LauncherGestureHooks.HotSeatGestureState()
        val b = LauncherGestureHooks.HotSeatGestureState()
        a.updateThresholdsIfNeeded(320, 2.0f, 10)
        b.updateThresholdsIfNeeded(480, 3.0f, 20)
        assertNotSame(a, b)
        assertEquals(Math.round(75f * 2.0f), a.minDistance)
        assertEquals(Math.round(75f * 3.0f), b.minDistance)
    }

    @Test
    fun noContextOrViewFields() {
        val clazz = LauncherGestureHooks.HotSeatGestureState::class.java
        for (field in clazz.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue
            val type = field.type
            assertFalse(
                "HotSeatGestureState must not hold Context/View/Activity/ClassLoader fields: ${field.name}",
                Context::class.java.isAssignableFrom(type) ||
                    android.view.View::class.java.isAssignableFrom(type) ||
                    Activity::class.java.isAssignableFrom(type) ||
                    ClassLoader::class.java.isAssignableFrom(type)
            )
        }
    }
}
