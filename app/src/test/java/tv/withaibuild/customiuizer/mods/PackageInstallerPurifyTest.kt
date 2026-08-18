package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageInstallerPurifyTest {

    @Test
    fun disablesAdsAndRecommendKeys() {
        assertEquals(false, Various.purifiedInstallerBoolean("ads_enable"))
        assertEquals(false, Various.purifiedInstallerBoolean("app_store_recommend"))
        assertEquals(false, Various.purifiedInstallerBoolean("secure_verify_enable"))
        assertEquals(true, Various.purifiedInstallerBoolean("secure_verify_cloud_once"))
        assertNull(Various.purifiedInstallerBoolean("unrelated"))
    }

    @Test
    fun disablesScanAndSafeModeInts() {
        assertEquals(0, Various.purifiedInstallerSystemInt("virus_scan_install"))
        assertNull(Various.purifiedInstallerSystemInt("other"))
        assertEquals(0, Various.purifiedInstallerSecureInt("miui_safe_mode"))
    }
}

class HideReportButtonTest {

    @Test
    fun hidesKnownReportItemId() {
        assertTrue(Various.shouldHideReportMenuItem(4))
        assertFalse(Various.shouldHideReportMenuItem(2))
    }
}
