package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockHeightOverrideTest {

    @Test
    fun defaultDoesNotOverride() {
        assertFalse(LauncherLayoutHooks.shouldOverrideDockHeight(60))
    }

    @Test
    fun customValueOverrides() {
        assertTrue(LauncherLayoutHooks.shouldOverrideDockHeight(80))
    }
}
