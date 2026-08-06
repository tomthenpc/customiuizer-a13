package tv.withaibuild.customiuizer.installers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Tests for [SystemUiInstaller] startup family gating.
 */
class SystemUiInstallerTest {

    private fun emptyPrefs() = PrefMap<String, Any>()

    @Test
    fun hasAnySystemUiStartupFeature_allDefaultsOff() {
        val prefs = emptyPrefs()
        assertFalse("no systemui startup feature enabled by default", SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun hasAnySystemUiStartupFeature_statusBarHeight() {
        val prefs = emptyPrefs()
        prefs["system_statusbarheight"] = 20
        assertTrue("status bar height change enables the installer", SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun hasAnySystemUiStartupFeature_navBarHeight() {
        val prefs = emptyPrefs()
        prefs["controls_navbarheight"] = 21
        assertTrue("nav bar height change enables the installer", SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun hasAnySystemUiStartupFeature_statusBarTopMargin() {
        val prefs = emptyPrefs()
        prefs["system_statusbar_topmargin"] = true
        assertTrue("status bar top margin enables the installer", SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun hasAnySystemUiStartupFeature_globalAction() {
        val prefs = emptyPrefs()
        prefs["controls_backlong_action"] = 5
        assertTrue("a global action enables the installer", SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun hasAnySystemUiStartupFeature_scramblePin() {
        val prefs = emptyPrefs()
        prefs["system_scramblepin"] = true
        assertTrue("scramble pin enables the installer", SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun hasAnySystemUiStartupFeature_defaultDoesNotEnable() {
        val prefs = emptyPrefs()
        prefs["controls_navbarheight"] = 19
        assertFalse("default value 19 is not enabled", SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }
}
