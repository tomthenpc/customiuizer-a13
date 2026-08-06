package tv.withaibuild.customiuizer.installers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Tests for [AndroidPackageInstaller] zero-feature-cost gating.
 */
class AndroidPackageInstallerTest {

    private fun emptyPrefs() = PrefMap<String, Any>()

    @Test
    fun isAnyFeatureEnabled_allDefaultsOff() {
        val prefs = emptyPrefs()
        assertFalse("no android package feature enabled by default", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }

    @Test
    fun isAnyFeatureEnabled_statusBarHeight() {
        val prefs = emptyPrefs()
        prefs["system_statusbarheight"] = 20
        assertTrue("status bar height change enables the installer", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }

    @Test
    fun isAnyFeatureEnabled_navBarHeight() {
        val prefs = emptyPrefs()
        prefs["controls_navbarheight"] = 21
        assertTrue("nav bar height change enables the installer", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }

    @Test
    fun isAnyFeatureEnabled_allRotations() {
        val prefs = emptyPrefs()
        prefs["system_allrotations2"] = 2
        assertTrue("all rotations option enables the installer", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }

    @Test
    fun isAnyFeatureEnabled_rotateAnim() {
        val prefs = emptyPrefs()
        prefs["system_rotateanim"] = 2
        assertTrue("rotation animation option enables the installer", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }

    @Test
    fun isAnyFeatureEnabled_cleanShare() {
        val prefs = emptyPrefs()
        prefs["system_cleanshare"] = true
        assertTrue("clean share menu enables the installer", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }

    @Test
    fun isAnyFeatureEnabled_cleanOpenWith() {
        val prefs = emptyPrefs()
        prefs["system_cleanopenwith"] = true
        assertTrue("clean open-with menu enables the installer", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }

    @Test
    fun isAnyFeatureEnabled_defaultFalseForUnknownKey() {
        val prefs = emptyPrefs()
        prefs["system_statusbarheight"] = 19
        assertFalse("default value 19 is not enabled", AndroidPackageInstaller.isAnyFeatureEnabled(prefs))
    }
}
