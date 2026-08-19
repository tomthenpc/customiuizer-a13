package tv.withaibuild.customiuizer.subs

import android.app.Activity
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.prefs.SpinnerSelection
import java.io.File

class MultiActionContractTest {

    private val launcherValues = intArrayOf(
        1, 2, 3, 4, 5, 6, 7, 17, 12, 26, 8, 9, 20, 10, 13, 14, 22, 23
    )

    @Test
    fun savedZeroNormalizesToNoAction() {
        assertEquals(1, MultiActionContract.normalize(0, launcherValues))
    }

    @Test
    fun savedNegativeNormalizesToNoAction() {
        assertEquals(1, MultiActionContract.normalize(-3, launcherValues))
    }

    @Test
    fun savedUnknownIdNormalizesToNoAction() {
        assertEquals(1, MultiActionContract.normalize(99, launcherValues))
    }

    @Test
    fun validSavedActionIsUnchanged() {
        assertEquals(8, MultiActionContract.normalize(8, launcherValues))
        assertEquals(1, MultiActionContract.normalize(1, launcherValues))
        assertEquals(10, MultiActionContract.normalize(10, launcherValues))
    }

    @Test
    fun saveThenReopenKeepsNormalizedValue() {
        val first = MultiActionContract.persistSelection(0, launcherValues)
        assertEquals(1, first)
        assertEquals(1, MultiActionContract.normalize(first, launcherValues))
    }

    @Test
    fun noActionValueIsOne() {
        assertEquals(1, MultiActionContract.NO_ACTION)
        assertTrue(launcherValues.contains(MultiActionContract.NO_ACTION))
    }

    @Test
    fun toggleActionIsTen() {
        assertEquals(10, MultiActionContract.TOGGLE_ACTION)
        assertTrue(launcherValues.contains(MultiActionContract.TOGGLE_ACTION))
    }

    @Test
    fun spinnerSelectionRejectsInvalidIndex() {
        assertEquals(null, SpinnerSelection.valueAt(launcherValues, -1))
        assertEquals(null, SpinnerSelection.valueAt(launcherValues, launcherValues.size))
        assertEquals(1, SpinnerSelection.valueAt(launcherValues, 0))
        assertEquals(-1, SpinnerSelection.indexOfValue(0, launcherValues))
        assertEquals(-1, SpinnerSelection.indexOfValue(99, launcherValues))
        assertEquals(0, SpinnerSelection.indexOfValue(1, launcherValues))
    }
}

class MultiActionArrayContractTest {

    private val repo = File("").absoluteFile.let { root ->
        if (File(root, "app/src/main/res/values/arrays.xml").isFile) root
        else File("..").canonicalFile
    }

    private fun integerArray(name: String): IntArray {
        val xml = File(repo, "app/src/main/res/values/arrays.xml").readText()
        val block = Regex(
            """<integer-array name="$name">([\s\S]*?)</integer-array>"""
        ).find(xml)?.groupValues?.get(1) ?: error("missing $name")
        return Regex("""<item>(\d+)</item>""").findAll(block)
            .map { it.groupValues[1].toInt() }
            .toList()
            .toIntArray()
    }

    private fun stringArraySize(name: String): Int {
        val xml = File(repo, "app/src/main/res/values/arrays.xml").readText()
        val block = Regex(
            """<string-array name="$name">([\s\S]*?)</string-array>"""
        ).find(xml)?.groupValues?.get(1) ?: error("missing $name")
        return Regex("""<item>""").findAll(block).count()
    }

    private fun handledActionIds(): Set<Int> {
        val src = File(repo, "app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt").readText()
        val handled = mutableSetOf<Int>()
        val media = Regex("""if \(action >= 85 && action <= 88\)""").containsMatchIn(src)
        assertTrue(media)
        handled.addAll(85..88)
        val whenBlock = Regex(
            """return when \(action\) \{([\s\S]*?)else -> false"""
        ).find(src)?.groupValues?.get(1) ?: error("handleAction when-block missing")
        Regex("""^\s+(\d+)\s+->""", RegexOption.MULTILINE).findAll(whenBlock).forEach {
            handled.add(it.groupValues[1].toInt())
        }
        return handled
    }

    private fun handledToggleIds(): Set<Int> {
        val src = File(repo, "app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt").readText()
        val block = Regex(
            """val whatStr = when \(what\) \{([\s\S]*?)else -> return false"""
        ).find(src)?.groupValues?.get(1) ?: error("toggleThis when-block missing")
        return Regex("""^\s+(\d+)\s+->""", RegexOption.MULTILINE).findAll(block)
            .map { it.groupValues[1].toInt() }
            .toSet()
    }

    @Test
    fun advertisedActionArraysMatchHandlersAndLabels() {
        val handled = handledActionIds()
        val arrays = listOf(
            "global_actions_launcher" to "global_actions_launcher_val",
            "global_actions_navbar" to "global_actions_navbar_val",
            "global_actions_controls" to "global_actions_controls_val",
            "global_actions_statusbar" to "global_actions_statusbar_val",
            "global_lockscreen_actions" to "global_lockscreen_actions_val",
            "global_launch_actions" to "global_launch_actions_val"
        )
        for ((labels, valuesName) in arrays) {
            val values = integerArray(valuesName)
            assertEquals(labels, stringArraySize(labels), values.size)
            assertEquals(labels, 1, values.first())
            for (value in values) {
                if (value == MultiActionContract.NO_ACTION) continue
                assertTrue("$labels value $value has no GlobalActions handler", handled.contains(value))
            }
            if (valuesName != "global_launch_actions_val") {
                assertTrue("$valuesName must expose toggle 10", values.contains(MultiActionContract.TOGGLE_ACTION))
            }
        }
    }

    @Test
    fun advertisedTogglesHaveExecutionPaths() {
        val labels = stringArraySize("global_toggles")
        val values = integerArray("global_toggles_val")
        assertEquals(labels, values.size)
        val handled = handledToggleIds()
        for (value in values) {
            assertTrue("toggle $value has no toggleThis path", handled.contains(value))
        }
    }
}

class LauncherGestureRestartScopeTest {

    private fun repoFile(rel: String): File {
        val fromRoot = File(rel)
        if (fromRoot.isFile) return fromRoot
        return File("..", rel)
    }

    @Test
    fun launcherGesturesRestartLauncherNotSystemServer() {
        assertEquals("launcher", LauncherGestureRestartScope.RESTART_MENU)
        assertEquals("launcher", LauncherGestureRestartScope.HOOK_PROCESS)
        assertFalse(LauncherGestureRestartScope.SYSTEM_SERVER_RESTART)
        for (key in LauncherGestureRestartScope.GESTURE_PREF_KEYS) {
            assertEquals("launcher", LauncherGestureRestartScope.restartMenuForGestureKey(key))
            assertEquals("launcher", LauncherGestureRestartScope.restartMenuForGestureKey("pref_key_$key"))
        }
    }

    @Test
    fun gesturePageEnablesLauncherRestartMenu() {
        val src = repoFile("app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt").readText()
        assertTrue(src.contains("pref_key_launcher_cat_gestures"))
        assertTrue(src.contains("LauncherGestureRestartScope.RESTART_MENU"))
        assertFalse(src.contains("activeMenus = \"systemui\""))
        val installer = repoFile("app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java").readText()
        assertTrue(installer.contains("HomescreenSwipesHook"))
        assertTrue(installer.contains("launcher_swipedown_action"))
    }
}

class MultiActionSelectorResultRegressionTest {

    @Test
    fun backStackTargetCanAcceptResultWhileNotAdded() {
        assertTrue(
            SelectorResultDelivery.canAcceptAtBackStackTarget(
                targetExists = true,
                _targetIsAdded = false
            )
        )
    }

    @Test
    fun staleSourceCannotDeliverResult() {
        assertFalse(SelectorResultDelivery.canDeliverFromSource(sourceIsAdded = false, targetExists = true))
        assertFalse(SelectorResultDelivery.canDeliverFromSource(sourceIsAdded = true, targetExists = false))
    }

    @Test
    fun activityTwoHopRelayAllowsBackStackIntermediaryAndUpdatesMultiAction() {
        val activityFirstHopAllowed = SelectorResultDelivery.canDeliverFromSource(
            sourceIsAdded = true,
            targetExists = true
        )
        assertTrue(activityFirstHopAllowed)

        val appSelectorRelayAllowed = SelectorResultDelivery.canAcceptAtBackStackTarget(
            targetExists = true,
            _targetIsAdded = false
        )
        assertTrue(appSelectorRelayAllowed)

        var state = MultiActionSelectionState()
        if (activityFirstHopAllowed && appSelectorRelayAllowed) {
            state = MultiActionSelectionStateReducer.reduce(
                state,
                2,
                Activity.RESULT_OK,
                Intent().apply {
                    putExtra("activity", "com.example.activity|RelayedActivity")
                    putExtra("user", 99)
                }
            )
        }
        assertEquals("com.example.activity|RelayedActivity", state.activityValue)
        assertEquals(99, state.activityUser)
    }

    @Test
    fun activityTwoHopRelayRejectsDetachedSourceOrMissingTarget() {
        val detachedSource = SelectorResultDelivery.canDeliverFromSource(
            sourceIsAdded = false,
            targetExists = true
        )
        assertFalse(detachedSource)

        val missingTargetAtSource = SelectorResultDelivery.canDeliverFromSource(
            sourceIsAdded = true,
            targetExists = false
        )
        assertFalse(missingTargetAtSource)

        val missingRelayTarget = SelectorResultDelivery.canAcceptAtBackStackTarget(
            targetExists = false,
            _targetIsAdded = false
        )
        assertFalse(missingRelayTarget)
    }

    @Test
    fun appShortcutActivityResultsUpdatePendingStateAndPersistContract() {
        var state = MultiActionSelectionState()

        state = MultiActionSelectionStateReducer.reduce(
            state,
            0,
            Activity.RESULT_OK,
            Intent().apply {
                putExtra("app", "com.example.app|MainActivity")
                putExtra("user", 10)
            }
        )
        assertEquals("com.example.app|MainActivity", state.appValue)
        assertEquals(10, state.appUser)

        state = MultiActionSelectionStateReducer.reduce(
            state,
            1,
            Activity.RESULT_OK,
            Intent().apply {
                putExtra("shortcut_contents", "com.example.shortcut|Entry")
                putExtra("shortcut_name", "Shortcut Name")
                putExtra("shortcut_icon", "/tmp/icon.png")
            }
        )
        assertEquals("com.example.shortcut|Entry", state.shortcutValue)
        assertEquals("Shortcut Name", state.shortcutName)
        assertEquals("/tmp/icon.png", state.shortcutIcon)
        assertEquals(null, state.shortcutIntent)

        state = MultiActionSelectionStateReducer.reduce(
            state,
            2,
            Activity.RESULT_OK,
            Intent().apply {
                putExtra("activity", "com.example.activity|SomeActivity")
                putExtra("user", 0)
            }
        )
        assertEquals("com.example.activity|SomeActivity", state.activityValue)
        assertEquals(0, state.activityUser)

        val values = intArrayOf(1, 8, 9, 10, 20)
        assertEquals(8, MultiActionContract.persistSelection(8, values))
        assertEquals(1, MultiActionContract.persistSelection(0, values))
    }
}
