package tv.withaibuild.customiuizer.mods.catalog

/**
 * Strongly-typed feature identifiers.
 *
 * Each [FeatureId] carries the canonical id used by [FeatureCatalog],
 * [FeatureInstallRegistry] and the Python invariants. New callers should use
 * [FeatureId]; the legacy string ID entry point in [FeatureDispatcher] is kept
 * as a compatibility shim and logs unknown IDs.
 */
enum class FeatureId(val canonicalId: String) {
    PACKAGE_PERMISSIONS("packagePermissions"),
    STATUS_BAR_CLOCK_TWEAK("statusBarClockTweak"),
    AUTO_BRIGHTNESS_RANGE("autoBrightnessRange"),
    MUFFLED_VIBRATION("muffledVibration"),
    NO_MORE_ICON("noMoreIcon"),
    BATTERY_INDICATOR("batteryIndicator"),
    NO_CLOCK_HIDE("noClockHide"),
    NO_WIDGET_ONLY("noWidgetOnly"),
    SCREEN_DIM_TIME("screenDimTime"),
    FIRST_VOLUME_PRESS("firstVolumePress"),
    NETWORK_INDICATOR_WIFI("networkIndicatorWifi"),
    MUTE_VISIBLE_NOTIFICATIONS("muteVisibleNotifications"),
    HIDE_LAUNCHER_TITLES("hideLauncherTitles"),
    FIX_APP_INFO_LAUNCH("fixAppInfoLaunch"),
    HIDE_PROXIMITY_WARNING("hideProximityWarning"),
    CLEAR_ALL_TASKS("clearAllTasks"),
    HIDE_DISMISS_VIEW("hideDismissView"),
    HIDE_LOCK_SCREEN_HINT("hideLockScreenHint"),
    FOLDER_COLUMNS("folderColumns"),
    TITLE_TOP_MARGIN("titleTopMargin"),
    NO_LIGHT_UP_ON_CHARGE("noLightUpOnCharge"),
    ALL_ROTATIONS("allRotations"),
    NO_NETWORK_SPEED_SEPARATOR("noNetworkSpeedSeparator"),
    HIDE_ICONS_CLOCK("hideIconsClock"),
    NO_UNLOCK_ANIMATION("noUnlockAnimation");

    companion object {
        private val byString: Map<String, FeatureId> = values().associateBy {
            FeatureIdentity.normalizeLookupId(it.canonicalId)
        }

        @JvmStatic
        fun fromString(id: String): FeatureId? = byString[FeatureIdentity.normalizeLookupId(id)]
    }
}
