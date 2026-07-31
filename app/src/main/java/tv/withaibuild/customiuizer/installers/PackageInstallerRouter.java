package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.GlobalActions;
import tv.withaibuild.customiuizer.mods.LauncherAnimationHooks;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVolumeHooks;
import tv.withaibuild.customiuizer.mods.SystemFreeformAndMultiWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsAndConnectivityHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsMoreHooks;
import tv.withaibuild.customiuizer.mods.Various;

public final class PackageInstallerRouter {

    private PackageInstallerRouter() {}

    public static boolean install(String pkg, PackageReadyParam lpparam) {
        if (isInputMethod(pkg)) {
            if (MainModule.mPrefs.getBoolean("controls_volumecursor")) {
                Controls.VolumeCursorHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("controls_nonavbar_fix_inputmethod")
                && MainModule.mPrefs.getBoolean("controls_nonavbar")) {
                Various.FixInputMethodBottomMarginHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.miui.miwallpaper")) {
            if (MainModule.mPrefs.getBoolean("launcher_disable_wallpaperscale")) {
                LauncherAnimationHooks.DisableUnlockWallpaperScale(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.android.settings")) {
            if (MainModule.mPrefs.getStringAsInt("miuizer_settingsiconpos", 1) > 0) {
                GlobalActions.miuizerSettingsHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_separatevolume")) {
                SystemAudioAndVolumeHooks.NotificationVolumeSettingsRes();
                SystemAudioAndVolumeHooks.NotificationVolumeSettingsHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_disableanynotif")) {
                SystemNotificationMoreHooks.DisableAnyNotificationHook(lpparam);
                SystemNotificationMoreHooks.DisableAnyNotificationBlockHook(lpparam);
            }
            if (!"none".equals(MainModule.mPrefs.getString("system_defaultusb", "none"))) {
                SystemSettingsMoreHooks.USBConfigSettingsHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_notifimportance")) {
                SystemNotificationMoreHooks.NotificationImportanceHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_wifipassword")) {
                SystemSettingsAndConnectivityHooks.ViewWifiPasswordHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.lbe.security.miui")) {
            if (MainModule.mPrefs.getStringAsInt("various_clipboard_defaultaction", 1) > 1) {
                Various.SmartClipboardActionHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.android.incallui")) {
            if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) {
                Various.ShowCallUIHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("various_calluibright")) {
                Various.InCallBrightnessHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("various_answerinheadup")) {
                Various.AnswerCallInHeadUpHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.miui.securitycenter")) {
            if (MainModule.mPrefs.getBoolean("various_appdetails")) Various.AppInfoHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_disableapp")) Various.AppsDisableHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_restrictapp")) Various.AppsRestrictHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_applock_scramblepin")) SystemAudioAndVisualAndMoreHooks.ScrambleAppLockPINHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("various_appsort", 1) > 1) Various.AppsDefaultSortHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("various_skip", 0) > 0) Various.AppsDefaultSortHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_skip_interceptperm")) Various.InterceptPermHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_replace_defaultopen_with_openbydefault")) Various.OpenByDefaultHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_skip_securityscan")) Various.SkipSecurityScanHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_show_battery_temperature")) Various.ShowTempInBatteryHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_enable_sc_ai_clipboard_location")) Various.UnlockClipboardAndLocationHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_disable_freeform_suggest_blacklist")) SystemFreeformAndMultiWindowHooks.DisableSideBarSuggestionHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_disable_dock_suggest")) Various.DisableDockSuggestHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_enable_expand_sidebar")) {
                Various.AddSideBarExpandReceiverHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_hidelowbatwarn")) {
                Various.NoLowBatteryWarningHook();
            }
            if (MainModule.mPrefs.getBoolean("various_privacyapps_column_nums4")) {
                Various.PrivacyAppsLayoutHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.miui.powerkeeper")) {
            if (MainModule.mPrefs.getBoolean("various_restrictapp")) Various.AppsRestrictPowerHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_persist_batteryoptimization")) Various.PersistBatteryOptimizationHook(lpparam);
            return true;
        }

        if (pkg.startsWith("com.google.android.inputmethod")) {
            if (MainModule.mPrefs.getInt("various_gboardpadding_port", 0) > 0 || MainModule.mPrefs.getInt("various_gboardpadding_land", 0) > 0) {
                Various.GboardPaddingHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.miui.packageinstaller")) {
            if (MainModule.mPrefs.getBoolean("various_miuiinstaller")) Various.MiuiPackageInstallerHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_installappinfo")) Various.AppInfoDuringMiuiInstallHook(lpparam);
            return true;
        }

        if (pkg.equals("com.miui.screenshot")) {
            if (MainModule.mPrefs.getBoolean("system_screenshot")) SystemAudioAndVisualAndMoreHooks.ScreenshotConfigHook(lpparam);
            if (MainModule.mPrefs.getInt("system_screenshot_floattime", 0) > 0) SystemAudioAndVisualAndMoreHooks.ScreenshotFloatTimeHook(lpparam);
            return true;
        }

        if (pkg.equals("com.miui.gallery")) {
            int folder = MainModule.mPrefs.getStringAsInt("system_gallery_screenshots_path", 1);
            if (folder > 1) {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(lpparam);
            }
            return true;
        }

        return false;
    }

    private static boolean isInputMethod(String pkg) {
        return pkg.equals("com.baidu.input")
            || pkg.equals("com.baidu.input_mi")
            || pkg.equals("com.iflytek.inputmethod")
            || pkg.equals("com.iflytek.inputmethod.miui")
            || pkg.equals("com.sohu.inputmethod.sogou")
            || pkg.equals("com.sohu.inputmethod.sogou.xiaomi")
            || pkg.startsWith("com.google.android.inputmethod")
            || pkg.startsWith("com.touchtype.swiftkey")
            || pkg.startsWith("com.tencent.wetype");
    }
}
