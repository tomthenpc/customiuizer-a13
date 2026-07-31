package tv.withaibuild.customiuizer.mods.catalog

/**
 * Strongly-typed feature identifiers.
 *
 * New callers should use [FeatureId]; the legacy string ID entry point in
 * [FeatureDispatcher.installById] is kept as a compatibility shim and logs
 * unknown IDs.
 */
enum class FeatureId {
    PACKAGE_PERMISSIONS,
    STATUS_BAR_CLOCK_TWEAK,
    AUTO_BRIGHTNESS_RANGE,
    MUFFLED_VIBRATION,
    NO_MORE_ICON,
    BATTERY_INDICATOR,
    NO_CLOCK_HIDE,
    NO_WIDGET_ONLY,
    SCREEN_DIM_TIME,
    FIRST_VOLUME_PRESS,
    NETWORK_INDICATOR_WIFI,
    MUTE_VISIBLE_NOTIFICATIONS,
    HIDE_LAUNCHER_TITLES,
    FIX_APP_INFO_LAUNCH,
    HIDE_PROXIMITY_WARNING,
    CLEAR_ALL_TASKS,
    HIDE_DISMISS_VIEW,
    HIDE_LOCK_SCREEN_HINT,
    FOLDER_COLUMNS,
    TITLE_TOP_MARGIN,
    NO_LIGHT_UP_ON_CHARGE,
    ALL_ROTATIONS,
    NO_NETWORK_SPEED_SEPARATOR,
    HIDE_ICONS_CLOCK,
    NO_UNLOCK_ANIMATION;

    companion object {
        private val byString: Map<String, FeatureId> = values().associateBy {
            it.name.lowercase().replace("_", "")
        }

        @JvmStatic
        fun fromString(id: String): FeatureId? = byString[id.lowercase().replace("_", "")]
    }
}
