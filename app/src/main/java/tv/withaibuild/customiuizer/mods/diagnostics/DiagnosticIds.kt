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

    /** Generic resolver diagnostic, throttled independently of feature IDs. */
    const val HOOK_TARGET_RESOLVER = "HOOK_TARGET_RESOLVER"
}
