package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Test

class NetSpeedClockStyleTest {

    @Test
    fun clockAppearanceNameMatchesSystemUiStyle() {
        assertEquals("TextAppearance.StatusBar.Clock", SystemUIStatusBarHooks.STATUS_BAR_CLOCK_TEXT_APPEARANCE)
    }
}
