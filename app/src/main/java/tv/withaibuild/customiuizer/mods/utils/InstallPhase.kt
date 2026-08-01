package tv.withaibuild.customiuizer.mods.utils

/**
 * Well-known installation phase for a feature.
 *
 * Each feature declares the phase at which it may be installed. The phase
 * is matched against the current lifecycle point before any factory or
 * compatibility work is performed.
 */
enum class InstallPhase {
    SYSTEM_SERVER_STARTING,
    ANDROID_PACKAGE_READY,
    PACKAGE_READY,
    APPLICATION_ATTACHED,
    SYSTEMUI_POST_INIT,
    VIEW_CREATED,
    UNKNOWN;
}
