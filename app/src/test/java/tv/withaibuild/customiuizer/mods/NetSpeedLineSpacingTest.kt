package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetSpeedLineSpacingTest {

    @Test
    fun defaultSpacingAt100MatchesCurrentBehavior() {
        assertEquals(0.90f, SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 100), 0.0001f)
        assertEquals(0.85f, SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(18, 100), 0.0001f)
    }

    @Test
    fun below100IsMoreCompactThanDefault() {
        val at100 = SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 100)
        val at70 = SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 70)
        assertTrue("70 should be more compact than 100", at70 < at100)
    }

    @Test
    fun above100IsMoreLooseThanDefault() {
        val at100 = SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 100)
        val at130 = SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 130)
        assertTrue("130 should be more loose than 100", at130 > at100)
    }

    @Test
    fun clampsToMinimumAndMaximum() {
        assertEquals(
            SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 70),
            SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 50),
            0.0001f
        )
        assertEquals(
            SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 130),
            SystemUIStatusBarHooks.resolveNetSpeedLineSpacing(16, 200),
            0.0001f
        )
    }
}
