package tv.withaibuild.customiuizer.installers;

import java.util.Map;

import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.GlobalActions;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVolumeHooks;
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemFreeformAndMultiWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemLockScreenHooks;
import tv.withaibuild.customiuizer.mods.SystemLockScreenMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationAndShareHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemSecurityAndSystemHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarAndClockHooks;
import tv.withaibuild.customiuizer.mods.Various;
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher;
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime;

public final class SystemServerInstaller {

    private SystemServerInstaller() {}

    public static void install(SystemServerStartingParam lpparam) {
        FeatureRuntime serverRuntime = FeatureDispatcher.createRuntime("android", lpparam, lpparam.getClassLoader(), MainModule.mPrefs);
        FeatureDispatcher.installById("packagePermissions", serverRuntime);
        if (needGlobalActions()) GlobalActions.setupGlobalActions(lpparam);

        if (MainModule.mPrefs.getBoolean("system_screenshot_overlay")) {
            SystemAudioAndVisualAndMoreHooks.TempHideOverlayAppHook(lpparam);
        }

        if (MainModule.mPrefs.getBoolean("system_notify_openinfw")
            || MainModule.mPrefs.getBoolean("system_fw_forcein_actionsend")
            || MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")
        ) {
            SystemFreeformAndMultiWindowHooks.OpenAppInFreeFormHook(lpparam);
        }

        if (MainModule.mPrefs.getInt("controls_backlong_action", 1) > 1 ||
            MainModule.mPrefs.getInt("controls_homelong_action", 1) > 1 ||
            MainModule.mPrefs.getInt("controls_menulong_action", 1) > 1) Controls.NavBarActionsHook(lpparam);
        if (MainModule.mPrefs.getInt("controls_powerdt_action", 1) > 1 || MainModule.mPrefs.getBoolean("controls_volumedowndt_torch")) Controls.PowerDoubleTapActionHook(lpparam);
        if (MainModule.mPrefs.getInt("system_screenanim_duration", 0) > 0) SystemDisplayAndWindowHooks.ScreenAnimHook(lpparam);
        if (MainModule.mPrefs.getInt("system_volumesteps", 0) > 0) FeatureDispatcher.installById("volumeSteps", serverRuntime);
        if (MainModule.mPrefs.getInt("system_applock_timeout", 1) > 1) SystemLockScreenMoreHooks.AppLockTimeoutHook(lpparam);
        if (MainModule.mPrefs.getInt("system_dimtime", 0) > 0) FeatureDispatcher.installById("screenDimTime", serverRuntime);
        if (MainModule.mPrefs.getInt("system_toasttime", 0) > 0) FeatureDispatcher.installById("toastTime", serverRuntime);
        if (!"none".equals(MainModule.mPrefs.getString("system_defaultusb", "none"))) SystemSettingsMoreHooks.USBConfigHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_removesecure")) SystemSecurityAndSystemHooks.RemoveSecureHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_remove_startactconfirm")) FeatureDispatcher.installById("removeActStartConfirm", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_securelock")) SystemLockScreenHooks.EnhancedSecurityHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_separatevolume")) SystemAudioAndVolumeHooks.NotificationVolumeServiceHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_downgrade")) FeatureDispatcher.installById("noVersionCheck", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_orientationlock")) FeatureDispatcher.installById("orientationLock", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_noducking")) FeatureDispatcher.installById("noDucking", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_cleanshare")) FeatureDispatcher.installById("cleanShareMenuService", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_cleanopenwith")) FeatureDispatcher.installById("cleanOpenWithMenuService", serverRuntime);
        FeatureDispatcher.installById("autoBrightnessRange", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_lockscreen_disable_strongauth_72h")) FeatureDispatcher.installById("disable72hStrongAuth", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_applock")) SystemLockScreenMoreHooks.AppLockHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_applock_skip")) SystemLockScreenMoreHooks.SkipAppLockHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_alarmcompat")) Various.AlarmCompatServiceHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_ignorecalls")) SystemAudioAndVisualAndMoreHooks.NoCallInterruptionHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_forceclose")) FeatureDispatcher.installById("forceClose", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_hideproxywarn")) FeatureDispatcher.installById("hideProximityWarning", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_firstpress")) FeatureDispatcher.installById("firstVolumePress", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_apksign")) SystemSecurityAndSystemHooks.NoSignatureVerifyServiceHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_disableintegrity")) FeatureDispatcher.installById("disableSystemIntegrity", serverRuntime);
        FeatureDispatcher.installById("muffledVibration", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_clearalltasks")) FeatureDispatcher.installById("clearAllTasks", serverRuntime);
        if (MainModule.mPrefs.getBoolean("system_nodarkforce")) SystemSecurityAndSystemHooks.NoDarkForceHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_fw_sticky")) SystemFreeformAndMultiWindowHooks.StickyFloatingWindowsHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_lswallpaper")) FeatureDispatcher.installById("setLockscreenWallpaper", serverRuntime);
        if (MainModule.mPrefs.getBoolean("controls_powerflash")) Controls.PowerKeyHook(lpparam);
        if (MainModule.mPrefs.getBoolean("controls_fingerprintfailure")) Controls.FingerprintHapticFailureHook(lpparam);
        if (MainModule.mPrefs.getBoolean("controls_fingerprintscreen")) Controls.FingerprintScreenOnHook(lpparam);
        if (MainModule.mPrefs.getBoolean("controls_fingerprintwake")) Controls.NoFingerprintWakeHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_disableapp")) Various.AppsDisableServiceHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_disableanynotif")) FeatureDispatcher.installById("disableAnyNotificationBlock", serverRuntime);
        if (MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) > 1) FeatureDispatcher.installById("allRotations", serverRuntime);
        if (MainModule.mPrefs.getStringAsInt("system_nolightuponcharges", 1) > 1) FeatureDispatcher.installById("noLightUpOnCharge", serverRuntime);
        if (MainModule.mPrefs.getStringAsInt("system_autogroupnotif", 1) > 1) SystemNotificationAndShareHooks.AutoGroupNotificationsHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("system_vibration", 1) > 1) SystemNotificationMoreHooks.SelectiveVibrationHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("system_blocktoasts", 1) > 1) SystemStatusBarAndClockHooks.SelectiveToastsHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("system_rotateanim", 1) > 1) SystemDisplayAndWindowHooks.RotationAnimationHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("controls_fingerprintsuccess", 1) > 1) Controls.FingerprintHapticSuccessHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 ||
            MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) Controls.VolumeMediaButtonsHook(lpparam);

        if (MainModule.mPrefs.getBoolean("system_fw_splitscreen")) SystemFreeformAndMultiWindowHooks.MultiWindowPlusHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_fw_noblacklist")) SystemFreeformAndMultiWindowHooks.NoFloatingWindowBlacklistHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_disable_access_devicelogs")) {
            SystemDisplayAndWindowHooks.NoAccessDeviceLogsRequest(lpparam);
        }
        if (MainModule.mPrefs.getInt("system_other_wallpaper_scale", 6) > 6) SystemNotificationMoreHooks.WallpaperScaleLevelHook(lpparam);
    }

    private static boolean needGlobalActions() {
        try {
            for (Map.Entry<String, Object> entry : MainModule.mPrefs.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && key.endsWith("_action") && value instanceof Integer && (Integer) value > 1) {
                    return true;
                }
            }
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;
        }
        if (MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) {
            return !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();
        }
        return false;
    }
}
