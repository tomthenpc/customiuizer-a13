package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherAnimationScaleTest {

    @Test
    fun onePointZeroKeepsOnePointZero() {
        assertEquals(1.0f, LauncherAnimationHooks.effectiveAnimatorScale(1.0f), 0.0f)
    }

    @Test
    fun zeroMapsToMinNonZeroScale() {
        assertEquals(0.01f, LauncherAnimationHooks.effectiveAnimatorScale(0.0f), 0.0f)
    }

    @Test
    fun pointFiveStaysPointFive() {
        assertEquals(0.5f, LauncherAnimationHooks.effectiveAnimatorScale(0.5f), 0.0f)
    }

    @Test
    fun twoPointZeroStaysTwoPointZero() {
        assertEquals(2.0f, LauncherAnimationHooks.effectiveAnimatorScale(2.0f), 0.0f)
    }

    @Test
    fun negativeValueStaysUnchanged() {
        assertEquals(-0.5f, LauncherAnimationHooks.effectiveAnimatorScale(-0.5f), 0.0f)
    }

    @Test
    fun positiveInfinityStaysUnchanged() {
        assertEquals(Float.POSITIVE_INFINITY, LauncherAnimationHooks.effectiveAnimatorScale(Float.POSITIVE_INFINITY), 0.0f)
    }

    @Test
    fun nanStaysUnchanged() {
        assertEquals(Float.NaN, LauncherAnimationHooks.effectiveAnimatorScale(Float.NaN), 0.0f)
    }
}
