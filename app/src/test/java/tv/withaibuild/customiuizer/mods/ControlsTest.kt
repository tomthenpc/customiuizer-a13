package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsTest {
    @Test
    fun powerDoubleTapReasonsUseExactMatching() {
        assertTrue(Controls.isPowerDoubleTapReason("double_click_power"))
        assertTrue(Controls.isPowerDoubleTapReason("power_double_tap"))
        assertTrue(Controls.isPowerDoubleTapReason("double_click_power_key"))
        assertFalse(Controls.isPowerDoubleTapReason("double_click_power_key_extra"))
        assertFalse(Controls.isPowerDoubleTapReason(null))
    }
}
