package tv.withaibuild.customiuizer.subs

/**
 * Static ownership for Launcher MultiAction gestures.
 *
 * Configuration is read and executed in the Launcher process. GlobalActions
 * one-shot broadcasts target SystemUI receivers that are always registered, so
 * enabling a new gesture requires a Launcher restart, not SystemUI or
 * system_server.
 */
object LauncherGestureRestartScope {
    const val RESTART_MENU = "launcher"
    const val HOOK_PROCESS = "launcher"
    const val SYSTEM_SERVER_RESTART = false

    val GESTURE_PREF_KEYS = arrayOf(
        "launcher_swipedown",
        "launcher_swipedown2",
        "launcher_swipeup",
        "launcher_swipeup2",
        "launcher_swiperight",
        "launcher_swipeleft",
        "launcher_shake",
        "launcher_doubletap",
        "launcher_pinch",
        "launcher_spread"
    )

    fun restartMenuForGestureKey(prefKey: String): String? {
        val key = prefKey.removePrefix("pref_key_")
        return if (key in GESTURE_PREF_KEYS) RESTART_MENU else null
    }
}
