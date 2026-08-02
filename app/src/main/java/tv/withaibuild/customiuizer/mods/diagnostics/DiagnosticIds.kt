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

    /** Catalog request for an unknown or malformed feature id. */
    const val UNKNOWN_FEATURE_ID = "UNKNOWN_FEATURE_ID"

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
    const val VOLUME_STEPS = "VOLUME_STEPS"
    const val TOAST_TIME = "TOAST_TIME"
    const val NO_LIGHT_UP_ON_CHARGE = "NO_LIGHT_UP_ON_CHARGE"
    const val ALL_ROTATIONS = "ALL_ROTATIONS"
    const val NO_NETWORK_SPEED_SEPARATOR = "NO_NETWORK_SPEED_SEPARATOR"
    const val HIDE_ICONS_CLOCK = "HIDE_ICONS_CLOCK"
    const val NO_UNLOCK_ANIMATION = "NO_UNLOCK_ANIMATION"

    /** Catalog expansion batch 4: SystemUI screenshot. */
    const val TEMP_HIDE_OVERLAY_SYSTEMUI = "TEMP_HIDE_OVERLAY_SYSTEMUI"
    const val HIDE_STATUS_BAR_BEFORE_SCREENSHOT = "HIDE_STATUS_BAR_BEFORE_SCREENSHOT"
    const val HIDE_NAV_BAR_BEFORE_SCREENSHOT = "HIDE_NAV_BAR_BEFORE_SCREENSHOT"

    /** Catalog expansion batch 5: Share/OpenWith menu cleaning. */
    const val CLEAN_SHARE_MENU = "CLEAN_SHARE_MENU"
    const val CLEAN_SHARE_MENU_SERVICE = "CLEAN_SHARE_MENU_SERVICE"
    const val CLEAN_OPEN_WITH_MENU = "CLEAN_OPEN_WITH_MENU"
    const val CLEAN_OPEN_WITH_MENU_SERVICE = "CLEAN_OPEN_WITH_MENU_SERVICE"

    /** Catalog expansion batch 6: Charging info and lockscreen wallpaper. */
    const val CHARGING_INFO = "CHARGING_INFO"
    const val SET_LOCKSCREEN_WALLPAPER = "SET_LOCKSCREEN_WALLPAPER"

    /** Catalog expansion batch 7: SystemServer security hooks. */
    const val NO_VERSION_CHECK = "NO_VERSION_CHECK"
    const val REMOVE_ACT_START_CONFIRM = "REMOVE_ACT_START_CONFIRM"
    const val FORCE_CLOSE = "FORCE_CLOSE"
    const val DISABLE_SYSTEM_INTEGRITY = "DISABLE_SYSTEM_INTEGRITY"
}
