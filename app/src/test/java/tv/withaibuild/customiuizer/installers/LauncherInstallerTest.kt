package tv.withaibuild.customiuizer.installers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Tests for [LauncherInstaller] startup family gating.
 */
class LauncherInstallerTest {

    private fun emptyPrefs() = PrefMap<String, Any>()

    @Test
    fun hasAnyLauncherStartupFeature_allDefaultsOff() {
        val prefs = emptyPrefs()
        assertFalse("no launcher startup feature enabled by default", LauncherInstaller.hasAnyLauncherStartupFeature(prefs))
    }

    @Test
    fun hasAnyLauncherStartupFeature_gestureAction() {
        val prefs = emptyPrefs()
        prefs["launcher_swipedown_action"] = 2
        assertTrue("a homescreen swipe action enables the installer", LauncherInstaller.hasAnyLauncherStartupFeature(prefs))
    }

    @Test
    fun hasAnyLauncherStartupFeature_packageReadyResource() {
        val prefs = emptyPrefs()
        prefs["launcher_horizmargin"] = 10
        assertTrue("horizontal margin change enables the installer", LauncherInstaller.hasAnyLauncherStartupFeature(prefs))
    }

    @Test
    fun hasAnyLauncherStartupFeature_folderCols() {
        val prefs = emptyPrefs()
        prefs["launcher_folder_cols"] = 3
        assertTrue("folder columns change enables the installer", LauncherInstaller.hasAnyLauncherStartupFeature(prefs))
    }

    @Test
    fun hasAnyLauncherStartupFeature_closeOnLaunch() {
        val prefs = emptyPrefs()
        prefs["launcher_closedrawer"] = true
        assertTrue("close drawer enables the installer", LauncherInstaller.hasAnyLauncherStartupFeature(prefs))
    }

    @Test
    fun hasAnyLauncherStartupFeature_defaultDoesNotEnable() {
        val prefs = emptyPrefs()
        prefs["launcher_swipedown_action"] = 1
        assertFalse("default value 1 is not enabled", LauncherInstaller.hasAnyLauncherStartupFeature(prefs))
    }
}
