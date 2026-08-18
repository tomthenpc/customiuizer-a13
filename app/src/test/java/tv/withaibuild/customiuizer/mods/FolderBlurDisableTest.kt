package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderBlurDisableTest {

    @Test
    fun disableForcesZeroEvenWhenOpacityStored() {
        assertEquals(0f, LauncherFolderHooks.resolveFolderBlurRatio(true, 80), 0f)
    }

    @Test
    fun opacityMapsWhenEnabled() {
        assertEquals(0.8f, LauncherFolderHooks.resolveFolderBlurRatio(false, 80), 0.0001f)
    }

    @Test
    fun defaultOpacityStaysZero() {
        assertEquals(0f, LauncherFolderHooks.resolveFolderBlurRatio(false, 0), 0f)
    }
}
