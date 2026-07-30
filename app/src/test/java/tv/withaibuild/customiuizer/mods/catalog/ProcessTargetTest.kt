package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessTargetTest {

    @Test
    fun systemServerMatchesAndroidAndSystemServer() {
        assertTrue(ProcessTarget.SystemServer.matches("android"))
        assertTrue(ProcessTarget.SystemServer.matches("system_server"))
        assertFalse(ProcessTarget.SystemServer.matches("com.android.systemui"))
    }

    @Test
    fun systemUIMatchesSystemUI() {
        assertTrue(ProcessTarget.SystemUI.matches("com.android.systemui"))
        assertFalse(ProcessTarget.SystemUI.matches("android"))
        assertFalse(ProcessTarget.SystemUI.matches("com.miui.home"))
    }

    @Test
    fun launcherMatchesMiuiAndGlobalLauncher() {
        assertTrue(ProcessTarget.Launcher.matches("com.miui.home"))
        assertTrue(ProcessTarget.Launcher.matches("com.mi.android.globallauncher"))
        assertFalse(ProcessTarget.Launcher.matches("com.android.systemui"))
        assertFalse(ProcessTarget.Launcher.matches("com.android.settings"))
    }

    @Test
    fun packageMatchesExactPackage() {
        val target = ProcessTarget.Package("com.android.settings")
        assertTrue(target.matches("com.android.settings"))
        assertFalse(target.matches("com.android.systemui"))
    }
}
