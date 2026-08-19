package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HideImeDismissButtonTest {

    @Test
    fun hidesOnlyGesturalImeAlt() {
        assertTrue(Controls.shouldHideImeDismissButton(0x1, 2))
    }

    @Test
    fun keepsVisibleWithoutImeHint() {
        assertFalse(Controls.shouldHideImeDismissButton(0, 2))
    }

    @Test
    fun keepsVisibleInNonGesturalMode() {
        assertFalse(Controls.shouldHideImeDismissButton(0x1, 1))
    }
}
