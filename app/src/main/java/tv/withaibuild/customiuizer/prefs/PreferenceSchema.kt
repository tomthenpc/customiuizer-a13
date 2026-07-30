package tv.withaibuild.customiuizer.prefs

import tv.withaibuild.customiuizer.mods.catalog.RestartTarget

/**
 * Type-safe declaration of one preference key.
 *
 * @param key The user-visible / code-visible preference key (without the
 *        `pref_key_` prefix; normalization is the job of consumers).
 * @param type The runtime type of the preference value.
 * @param defaultValue The default used when the preference is absent. Must be
 *        an instance of [type].
 * @param constraint Type-safe value constraint.
 * @param ownerFeature Stable feature ID from [FeatureCatalog].
 * @param restartTarget The restart granularity needed for the feature to first
 *                      take effect (activation restart).
 * @param hotReloadable Whether the feature can react to live preference changes
 *                      at runtime (config reload mode). `true` maps to
 *                      [ConfigReloadMode.FULL], `false` to [ConfigReloadMode.NONE].
 * @param legacyAliases Alternative historical keys that resolve to [key].
 */
data class PreferenceEntry(
    val key: String,
    val type: PreferenceType,
    val defaultValue: Any,
    val constraint: PreferenceConstraint,
    val ownerFeature: String,
    val restartTarget: RestartTarget,
    val hotReloadable: Boolean,
    val legacyAliases: Set<String>
)

enum class PreferenceType {
    BOOLEAN, INT, STRING, STRING_SET
}

object PreferenceSchema {

    val entries: List<PreferenceEntry> = listOf(
        // StatusBarClockTweak
        PreferenceEntry(
            key = "system_statusbar_clocktweak",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "statusBarClockTweak",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = true,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_cc_clocktweak",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "statusBarClockTweak",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = true,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_cc_hidedate",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "statusBarClockTweak",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = true,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_cc_dateformat",
            type = PreferenceType.STRING,
            defaultValue = "",
            constraint = PreferenceConstraint.None,
            ownerFeature = "statusBarClockTweak",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = true,
            legacyAliases = emptySet()
        ),
        // Canary: system_server
        PreferenceEntry(
            key = "system_autobrightness",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "autoBrightnessRange",
            restartTarget = RestartTarget.REBOOT,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_vibration_amp",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "muffledVibration",
            restartTarget = RestartTarget.REBOOT,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Canary: SystemUI
        PreferenceEntry(
            key = "system_hidemoreicon",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "noMoreIcon",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_batteryindicator",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "batteryIndicator",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Canary: Launcher
        PreferenceEntry(
            key = "launcher_noclockhide",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "noClockHide",
            restartTarget = RestartTarget.LAUNCHER_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "launcher_nowidgetonly",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "noWidgetOnly",
            restartTarget = RestartTarget.LAUNCHER_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Catalog expansion batch 1: system_server
        PreferenceEntry(
            key = "system_dimtime",
            type = PreferenceType.INT,
            defaultValue = 0,
            constraint = PreferenceConstraint.IntRange(min = 0, max = 300000),
            ownerFeature = "screenDimTime",
            restartTarget = RestartTarget.REBOOT,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_firstpress",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "firstVolumePress",
            restartTarget = RestartTarget.REBOOT,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Catalog expansion batch 1: SystemUI
        PreferenceEntry(
            key = "system_networkindicator_wifi",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "networkIndicatorWifi",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_mutevisiblenotif",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "muteVisibleNotifications",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Catalog expansion batch 1: Launcher
        PreferenceEntry(
            key = "launcher_hidetitles",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "hideLauncherTitles",
            restartTarget = RestartTarget.LAUNCHER_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "launcher_fixlaunch",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "fixAppInfoLaunch",
            restartTarget = RestartTarget.LAUNCHER_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Catalog expansion batch 2: system_server
        PreferenceEntry(
            key = "system_hideproxywarn",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "hideProximityWarning",
            restartTarget = RestartTarget.REBOOT,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_clearalltasks",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "clearAllTasks",
            restartTarget = RestartTarget.REBOOT,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Catalog expansion batch 2: SystemUI
        PreferenceEntry(
            key = "system_removedismiss",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "hideDismissView",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "system_hidelshint",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            constraint = PreferenceConstraint.BooleanValue(expected = false),
            ownerFeature = "hideLockScreenHint",
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        // Catalog expansion batch 2: Launcher
        PreferenceEntry(
            key = "launcher_folder_cols",
            type = PreferenceType.INT,
            defaultValue = 1,
            constraint = PreferenceConstraint.IntRange(min = 1, max = 10),
            ownerFeature = "folderColumns",
            restartTarget = RestartTarget.LAUNCHER_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        ),
        PreferenceEntry(
            key = "launcher_titletopmargin",
            type = PreferenceType.INT,
            defaultValue = 0,
            constraint = PreferenceConstraint.IntRange(min = 0, max = 100),
            ownerFeature = "titleTopMargin",
            restartTarget = RestartTarget.LAUNCHER_RESTART,
            hotReloadable = false,
            legacyAliases = emptySet()
        )
    )

    val byKey: Map<String, PreferenceEntry> = entries.associateBy { it.key }
}
