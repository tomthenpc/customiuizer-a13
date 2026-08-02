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
    VOLUME_STEPS("volumeSteps"),
    TOAST_TIME("toastTime"),
    NO_LIGHT_UP_ON_CHARGE("noLightUpOnCharge"),
    ALL_ROTATIONS("allRotations"),
    NO_NETWORK_SPEED_SEPARATOR("noNetworkSpeedSeparator"),
    HIDE_ICONS_CLOCK("hideIconsClock"),
    NO_UNLOCK_ANIMATION("noUnlockAnimation"),
    TEMP_HIDE_OVERLAY_SYSTEMUI("tempHideOverlaySystemUI"),
    OPEN_APP_IN_FREE_FORM("openAppInFreeForm"),
    HIDE_STATUS_BAR_BEFORE_SCREENSHOT("hideStatusBarBeforeScreenshot"),
    HIDE_NAV_BAR_BEFORE_SCREENSHOT("hideNavBarBeforeScreenshot"),
    CLEAN_SHARE_MENU("cleanShareMenu"),
    CLEAN_SHARE_MENU_SERVICE("cleanShareMenuService"),
    CLEAN_OPEN_WITH_MENU("cleanOpenWithMenu"),
    CLEAN_OPEN_WITH_MENU_SERVICE("cleanOpenWithMenuService"),
    CHARGING_INFO("chargingInfo"),
    SET_LOCKSCREEN_WALLPAPER("setLockscreenWallpaper"),
    NO_VERSION_CHECK("noVersionCheck"),
    REMOVE_ACT_START_CONFIRM("removeActStartConfirm"),
    FORCE_CLOSE("forceClose"),
    DISABLE_SYSTEM_INTEGRITY("disableSystemIntegrity"),
    ORIENTATION_LOCK("orientationLock"),
    NO_DUCKING("noDucking"),
    DISABLE_72H_STRONG_AUTH("disable72hStrongAuth"),
    DISABLE_ANY_NOTIFICATION_BLOCK("disableAnyNotificationBlock"),
    ENHANCED_SECURITY("enhancedSecurity"),
    APP_LOCK("appLock"),
    SKIP_APP_LOCK("skipAppLock"),
    NO_CALL_INTERRUPTION("noCallInterruption"),
    REMOVE_SECURE("removeSecure"),
    NO_SIGNATURE_VERIFY("noSignatureVerify"),
    NO_DARK_FORCE("noDarkForce"),
    STICKY_FLOATING_WINDOWS("stickyFloatingWindows"),
    SCREEN_ANIM("screenAnim"),
    ROTATION_ANIMATION("rotationAnimation"),
    NOTIFICATION_VOLUME("notificationVolume"),
    SELECTIVE_VIBRATION("selectiveVibration"),
    WALLPAPER_SCALE_LEVEL("wallpaperScaleLevel"),
    APPS_DISABLE_SERVICE("appsDisableService"),
    NO_ACCESS_DEVICE_LOGS_REQUEST("noAccessDeviceLogsRequest"),
    AUTO_GROUP_NOTIFICATIONS("autoGroupNotifications"),
    APP_LOCK_TIMEOUT("appLockTimeout"),
    TEMP_HIDE_OVERLAY_APP("tempHideOverlayApp");

    companion object {
        private val byString: Map<String, FeatureId> = values().associateBy {
            FeatureIdentity.normalizeLookupId(it.canonicalId)
        }

        @JvmStatic
        fun fromString(id: String): FeatureId? = byString[FeatureIdentity.normalizeLookupId(id)]
    }
}
