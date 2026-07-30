package tv.withaibuild.customiuizer.mods.catalog

/**
 * The restart granularity required for a preference or feature change to take
 * effect.
 */
enum class RestartTarget {
    REBOOT,
    SYSTEMUI_RESTART,
    LAUNCHER_RESTART,
    IN_APP,
    NONE
}
