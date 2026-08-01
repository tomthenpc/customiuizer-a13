package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSystemHooksTest {

    @Test
    fun floatingWindowBlocklistMatchesOnlyExactPackages() {
        assertTrue(LauncherSystemHooks.isFloatingWindowBlockedPackage("com.miui.securitycenter"))
        assertTrue(LauncherSystemHooks.isFloatingWindowBlockedPackage("com.miui.home"))
        assertTrue(LauncherSystemHooks.isFloatingWindowBlockedPackage("com.android.camera"))
        assertFalse(LauncherSystemHooks.isFloatingWindowBlockedPackage("com.android.camera.extra"))
        assertFalse(LauncherSystemHooks.isFloatingWindowBlockedPackage("com.example.app"))
    }
}
