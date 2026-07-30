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
 * @param restartTarget The smallest restart granularity needed for changes to
 *        take effect.
 * @param hotReloadable Whether the feature can react to live preference changes
 *        without a restart.
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

    /**
     * Starter schema covering only the keys owned by the two catalog features.
     * It is intentionally small; the audit scripts classify the remaining
     * XML/code keys so they can be backfilled feature-by-feature.
     */
    val entries: List<PreferenceEntry> = listOf(
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
        )
    )

    val byKey: Map<String, PreferenceEntry> = entries.associateBy { it.key }
}
