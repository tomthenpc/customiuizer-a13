package tv.withaibuild.customiuizer.installers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.utils.PrefMap

class SystemUiGateTest {

    private fun prefsWith(vararg pairs: Pair<String, Any>): PrefMap<String, Any> {
        val prefs = PrefMap<String, Any>()
        val map = HashMap<String, Any>()
        for ((key, value) in pairs) {
            map[key] = value
        }
        prefs.replaceSnapshot(map)
        return prefs
    }

    @Test
    fun allDefaults_areFalse() {
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(PrefMap()))
    }

    @Test
    fun launcherSwipeDownAction_isNotSystemUi() {
        val prefs = prefsWith("launcher_swipedown_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun launcherSwipeUpAction_isNotSystemUi() {
        val prefs = prefsWith("launcher_swipeup_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun launcherDoubleTapAction_isNotSystemUi() {
        val prefs = prefsWith("launcher_doubletap_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun launcherPinchAction_isNotSystemUi() {
        val prefs = prefsWith("launcher_pinch_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun launcherSwipeLeftAction_isNotSystemUi() {
        val prefs = prefsWith("launcher_swipeleft_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun launcherSwipeRightAction_isNotSystemUi() {
        val prefs = prefsWith("launcher_swiperight_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun prefKeyLauncherSwipeDownAction_isNotSystemUi() {
        val prefs = prefsWith("pref_key_launcher_swipedown_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun controlsBackLongAction_isSystemUi() {
        val prefs = prefsWith("controls_backlong_action" to 2)
        assertTrue(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun controlsHomeLongAction_isSystemUi() {
        val prefs = prefsWith("controls_homelong_action" to 2)
        assertTrue(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun controlsMenuLongAction_isSystemUi() {
        val prefs = prefsWith("controls_menulong_action" to 2)
        assertTrue(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun prefKeyControlsBackLongAction_isSystemUi() {
        val prefs = prefsWith("pref_key_controls_backlong_action" to 2)
        assertTrue(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun systemLockScreenShortcutsRightAction_isSystemUi() {
        val prefs = prefsWith("system_lockscreenshortcuts_right_action" to 2)
        assertTrue(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun unknownAction_isNotSystemUi() {
        val prefs = prefsWith("unknown_feature_action" to 2)
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }

    @Test
    fun launcherAction_doesNotTriggerViaGlobalActionScan() {
        val prefs = prefsWith(
            "launcher_swipedown_action" to 2,
            "controls_volumemedia_up" to 1,
            "controls_volumemedia_down" to 1
        )
        prefs.putAll(emptyMap())
        assertFalse(SystemUiInstaller.hasAnySystemUiStartupFeature(prefs))
    }
}
