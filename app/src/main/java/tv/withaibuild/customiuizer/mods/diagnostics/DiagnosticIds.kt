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
}
