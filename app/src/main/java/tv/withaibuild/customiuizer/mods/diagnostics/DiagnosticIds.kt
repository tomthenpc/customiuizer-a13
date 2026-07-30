package tv.withaibuild.customiuizer.mods.diagnostics

/**
 * Stable diagnostic IDs used by [DiagnosticRecorder], [FeatureCatalog] and
 * [HookTargetResolver].
 */
object DiagnosticIds {
    const val STATUSBAR_CLOCK_TWEAK = "STATUSBAR_CLOCK_TWEAK"
    const val STEP_COUNTER = "STEP_COUNTER"
    const val DEVICE_INFO_MONITOR = "DEVICE_INFO_MONITOR"
    const val PACKAGE_PERMISSIONS = "PACKAGE_PERMISSIONS"
    const val STATUSBAR_CLOCK_SECONDS = "STATUSBAR_CLOCK_SECONDS"

    /** Canary batch diagnostic IDs. */
    const val AUTO_BRIGHTNESS_RANGE = "AUTO_BRIGHTNESS_RANGE"
    const val MUFFLED_VIBRATION = "MUFFLED_VIBRATION"
    const val NO_MORE_ICON = "NO_MORE_ICON"
    const val BATTERY_INDICATOR = "BATTERY_INDICATOR"
    const val NO_CLOCK_HIDE = "NO_CLOCK_HIDE"
    const val NO_WIDGET_ONLY = "NO_WIDGET_ONLY"

    /** Generic resolver diagnostic, throttled independently of feature IDs. */
    const val HOOK_TARGET_RESOLVER = "HOOK_TARGET_RESOLVER"

    /** Catalog expansion batch 1. */
    const val SCREEN_DIM_TIME = "SCREEN_DIM_TIME"
    const val FIRST_VOLUME_PRESS = "FIRST_VOLUME_PRESS"
    const val NETWORK_INDICATOR_WIFI = "NETWORK_INDICATOR_WIFI"
    const val MUTE_VISIBLE_NOTIFICATIONS = "MUTE_VISIBLE_NOTIFICATIONS"
    const val HIDE_LAUNCHER_TITLES = "HIDE_LAUNCHER_TITLES"
    const val FIX_APP_INFO_LAUNCH = "FIX_APP_INFO_LAUNCH"

    /** Catalog expansion batch 2. */
    const val HIDE_PROXIMITY_WARNING = "HIDE_PROXIMITY_WARNING"
    const val CLEAR_ALL_TASKS = "CLEAR_ALL_TASKS"
    const val HIDE_DISMISS_VIEW = "HIDE_DISMISS_VIEW"
    const val HIDE_LOCK_SCREEN_HINT = "HIDE_LOCK_SCREEN_HINT"
    const val FOLDER_COLUMNS = "FOLDER_COLUMNS"
    const val TITLE_TOP_MARGIN = "TITLE_TOP_MARGIN"

    /** Catalog expansion batch 3. */
    const val NO_LIGHT_UP_ON_CHARGE = "NO_LIGHT_UP_ON_CHARGE"
    const val ALL_ROTATIONS = "ALL_ROTATIONS"
    const val NO_NETWORK_SPEED_SEPARATOR = "NO_NETWORK_SPEED_SEPARATOR"
    const val HIDE_ICONS_CLOCK = "HIDE_ICONS_CLOCK"
    const val NO_UNLOCK_ANIMATION = "NO_UNLOCK_ANIMATION"
}
