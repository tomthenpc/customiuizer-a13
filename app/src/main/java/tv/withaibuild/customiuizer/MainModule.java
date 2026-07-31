package tv.withaibuild.customiuizer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.GlobalActions;
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher;
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime;
import tv.withaibuild.customiuizer.mods.LauncherAnimationHooks;
import tv.withaibuild.customiuizer.mods.LauncherFolderHooks;
import tv.withaibuild.customiuizer.mods.LauncherGestureHooks;
import tv.withaibuild.customiuizer.mods.LauncherIconHooks;
import tv.withaibuild.customiuizer.mods.LauncherLayoutHooks;
import tv.withaibuild.customiuizer.mods.LauncherSystemHooks;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVolumeHooks;
import tv.withaibuild.customiuizer.mods.SystemChargingAndWallpaperHooks;
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemFreeformAndMultiWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemLockScreenHooks;
import tv.withaibuild.customiuizer.mods.SystemLockScreenMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationAndShareHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationPopupsHooks;
import tv.withaibuild.customiuizer.mods.SystemSecurityAndSystemHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsAndConnectivityHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemShareAndOpenWithHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarAndClockHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemUIBatteryHooks;
import tv.withaibuild.customiuizer.mods.SystemUIControlCenterHooks;
import tv.withaibuild.customiuizer.mods.SystemUILockScreenHooks;
import tv.withaibuild.customiuizer.mods.SystemUIMonitorAndTileHooks;
import tv.withaibuild.customiuizer.mods.SystemUINotificationHooks;
import tv.withaibuild.customiuizer.mods.SystemUIScreenshotHooks;
import tv.withaibuild.customiuizer.mods.SystemUIStatusBarHooks;
import tv.withaibuild.customiuizer.mods.Various;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper;
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks;
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers;
import tv.withaibuild.customiuizer.utils.PrefMap;
import tv.withaibuild.customiuizer.utils.PreferenceBootstrap;

public class MainModule extends XposedModule {

    public static PrefMap<String, Object> mPrefs = new PrefMap<String, Object>();
    public static ResourceHooks resHooks = new ResourceHooks();
    String processName;

    private final PreferenceBootstrap preferenceBootstrap = new PreferenceBootstrap(
        this::getRemotePreferences,
        ModuleHelper.prefsName + "_remote",
        MainModule.mPrefs
    );

    @Override
    public void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        processName = param.getProcessName();
        XposedHelpers.moduleInst = this;
        XposedHelpers.log("CustoMIUIzer-A13 " + BuildConfig.VERSION_NAME + ": loaded in " + processName);
    }

    private boolean isSupportedAndroidVersion() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        XposedHelpers.log("CustoMIUIzer-A13 disabled on Android API " + Build.VERSION.SDK_INT);
        return false;
    }

    private SharedPreferences getRemotePrefs() {
        return preferenceBootstrap.resolveRemote();
    }

    private PreferenceBootstrap.State initPrefs() {
        return preferenceBootstrap.start();
    }

    private boolean isPrefEnabled(SharedPreferences prefs, String key) {
        return prefs.getBoolean(key, false);
    }

    private boolean isInPrefSet(SharedPreferences prefs, String key, String pkg) {
        Set<String> set = prefs.getStringSet(key, null);
        return set != null && set.contains(pkg);
    }

    private boolean isVolumeMediaEnabled(SharedPreferences prefs) {
        String up = prefs.getString("pref_key_controls_volumemedia_up", "0");
        String down = prefs.getString("pref_key_controls_volumemedia_down", "0");
        try {
            return (up != null && Integer.parseInt(up) > 0)
                || (down != null && Integer.parseInt(down) > 0);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean needLoadPrefs(String pkg, SharedPreferences prefs) {
        if ("android".equals(pkg)
            || "com.android.systemui".equals(pkg)
            || "com.miui.home".equals(pkg)
            || "com.mi.android.globallauncher".equals(pkg)
            || "com.miui.miwallpaper".equals(pkg)
            || "com.lbe.security.miui".equals(pkg)
            || "com.android.incallui".equals(pkg)
            || "com.miui.securitycenter".equals(pkg)
            || "com.miui.powerkeeper".equals(pkg)
            || "com.android.settings".equals(pkg)
            || "com.miui.packageinstaller".equals(pkg)
            || "com.miui.screenshot".equals(pkg)
            || "com.miui.gallery".equals(pkg)
        ) return true;

        if (pkg.startsWith("com.google.android.inputmethod")) return true;

        if ("com.baidu.input".equals(pkg)
            || "com.baidu.input_mi".equals(pkg)
            || "com.iflytek.inputmethod".equals(pkg)
            || "com.iflytek.inputmethod.miui".equals(pkg)
            || "com.sohu.inputmethod.sogou".equals(pkg)
            || "com.sohu.inputmethod.sogou.xiaomi".equals(pkg)
            || pkg.startsWith("com.touchtype.swiftkey")
            || pkg.startsWith("com.tencent.wetype")) return true;

        if (isPrefEnabled(prefs, "pref_key_various_alarmcompat")
                && isInPrefSet(prefs, "pref_key_various_alarmcompat_apps", pkg)) return true;

        if (isPrefEnabled(prefs, "pref_key_system_statusbarcolor")
                && isInPrefSet(prefs, "pref_key_system_statusbarcolor_apps", pkg)) return true;

        if (isPrefEnabled(prefs, "pref_key_system_nooverscroll")
                && isInPrefSet(prefs, "pref_key_system_nooverscroll_apps", pkg)) return true;

        if (isVolumeMediaEnabled(prefs)
                && isInPrefSet(prefs, "pref_key_controls_mediaplayer_apps", pkg)) return true;

        return false;
    }

    private void watchPreferenceChange() {
        preferenceBootstrap.ensureWatcher();
    }

    @Override
    public void onSystemServerStarting(final SystemServerStartingParam lpparam) {
        if (!isSupportedAndroidVersion()) return;
        initPrefs();
        FeatureRuntime serverRuntime = FeatureDispatcher.createRuntime("android", lpparam, lpparam.getClassLoader(), mPrefs);
        FeatureDispatcher.installById("packagePermissions", serverRuntime);
        if (needGlobalActions()) GlobalActions.setupGlobalActions(lpparam);

        if (mPrefs.getBoolean("system_screenshot_overlay")) {
            SystemAudioAndVisualAndMoreHooks.TempHideOverlayAppHook(lpparam);
        }

        if (mPrefs.getBoolean("system_notify_openinfw")
            || mPrefs.getBoolean("system_fw_forcein_actionsend")
            || mPrefs.getBoolean("system_betterpopups_allowfloat")
        ) {
            SystemFreeformAndMultiWindowHooks.OpenAppInFreeFormHook(lpparam);
        }

        if (mPrefs.getInt("controls_backlong_action", 1) > 1 ||
            mPrefs.getInt("controls_homelong_action", 1) > 1 ||
            mPrefs.getInt("controls_menulong_action", 1) > 1) Controls.NavBarActionsHook(lpparam);
        if (mPrefs.getInt("controls_powerdt_action", 1) > 1 || mPrefs.getBoolean("controls_volumedowndt_torch")) Controls.PowerDoubleTapActionHook(lpparam);
        if (mPrefs.getInt("system_screenanim_duration", 0) > 0) SystemDisplayAndWindowHooks.ScreenAnimHook(lpparam);
        if (mPrefs.getInt("system_volumesteps", 0) > 0) SystemAudioAndVolumeHooks.VolumeStepsHook(lpparam);
        if (mPrefs.getInt("system_applock_timeout", 1) > 1) SystemLockScreenMoreHooks.AppLockTimeoutHook(lpparam);
        if (mPrefs.getInt("system_dimtime", 0) > 0) FeatureDispatcher.installById("screenDimTime", serverRuntime);
        if (mPrefs.getInt("system_toasttime", 0) > 0) SystemAudioAndVisualAndMoreHooks.ToastTimeHook(lpparam);
        if (!mPrefs.getString("system_defaultusb", "none").equals("none")) SystemSettingsMoreHooks.USBConfigHook(lpparam);
        if (mPrefs.getBoolean("system_removesecure")) SystemSecurityAndSystemHooks.RemoveSecureHook(lpparam);
        if (mPrefs.getBoolean("system_remove_startactconfirm")) SystemSecurityAndSystemHooks.RemoveActStartConfirmHook(lpparam);
        if (mPrefs.getBoolean("system_securelock")) SystemLockScreenHooks.EnhancedSecurityHook(lpparam);
        if (mPrefs.getBoolean("system_separatevolume")) SystemAudioAndVolumeHooks.NotificationVolumeServiceHook(lpparam);
        if (mPrefs.getBoolean("system_downgrade")) SystemSecurityAndSystemHooks.NoVersionCheckHook(lpparam);
        if (mPrefs.getBoolean("system_orientationlock")) SystemNotificationMoreHooks.OrientationLockHook(lpparam);
        if (mPrefs.getBoolean("system_noducking")) SystemNotificationMoreHooks.NoDuckingHook(lpparam);
        if (mPrefs.getBoolean("system_cleanshare")) SystemShareAndOpenWithHooks.CleanShareMenuServiceHook(lpparam);
        if (mPrefs.getBoolean("system_cleanopenwith")) SystemShareAndOpenWithHooks.CleanOpenWithMenuServiceHook(lpparam);
        FeatureDispatcher.installById("autoBrightnessRange", serverRuntime);
        if (mPrefs.getBoolean("system_lockscreen_disable_strongauth_72h")) SystemNotificationMoreHooks.Disable72hStrongAuthHook(lpparam);
        if (mPrefs.getBoolean("system_applock")) SystemLockScreenMoreHooks.AppLockHook(lpparam);
        if (mPrefs.getBoolean("system_applock_skip")) SystemLockScreenMoreHooks.SkipAppLockHook(lpparam);
        if (mPrefs.getBoolean("various_alarmcompat")) Various.AlarmCompatServiceHook(lpparam);
        if (mPrefs.getBoolean("system_ignorecalls")) SystemAudioAndVisualAndMoreHooks.NoCallInterruptionHook(lpparam);
        if (mPrefs.getBoolean("system_forceclose")) SystemSecurityAndSystemHooks.ForceCloseHook(lpparam);
        if (mPrefs.getBoolean("system_hideproxywarn")) FeatureDispatcher.installById("hideProximityWarning", serverRuntime);
        if (mPrefs.getBoolean("system_firstpress")) FeatureDispatcher.installById("firstVolumePress", serverRuntime);
        if (mPrefs.getBoolean("system_apksign")) SystemSecurityAndSystemHooks.NoSignatureVerifyServiceHook(lpparam);
        if (mPrefs.getBoolean("system_disableintegrity")) SystemSecurityAndSystemHooks.DisableSystemIntegrityHook(lpparam);
        FeatureDispatcher.installById("muffledVibration", serverRuntime);
        if (mPrefs.getBoolean("system_clearalltasks")) FeatureDispatcher.installById("clearAllTasks", serverRuntime);
        if (mPrefs.getBoolean("system_nodarkforce")) SystemSecurityAndSystemHooks.NoDarkForceHook(lpparam);
        if (mPrefs.getBoolean("system_fw_sticky")) SystemFreeformAndMultiWindowHooks.StickyFloatingWindowsHook(lpparam);
        if (mPrefs.getBoolean("system_lswallpaper")) SystemChargingAndWallpaperHooks.SetLockscreenWallpaperHook(lpparam);
        if (mPrefs.getBoolean("controls_powerflash")) Controls.PowerKeyHook(lpparam);
        if (mPrefs.getBoolean("controls_fingerprintfailure")) Controls.FingerprintHapticFailureHook(lpparam);
        if (mPrefs.getBoolean("controls_fingerprintscreen")) Controls.FingerprintScreenOnHook(lpparam);
        if (mPrefs.getBoolean("controls_fingerprintwake")) Controls.NoFingerprintWakeHook(lpparam);
        if (mPrefs.getBoolean("various_disableapp")) Various.AppsDisableServiceHook(lpparam);
        if (mPrefs.getBoolean("system_disableanynotif")) SystemNotificationMoreHooks.DisableAnyNotificationBlockHook(lpparam);
        if (mPrefs.getStringAsInt("system_allrotations2", 1) > 1) FeatureDispatcher.installById("allRotations", serverRuntime);
        if (mPrefs.getStringAsInt("system_nolightuponcharges", 1) > 1) FeatureDispatcher.installById("noLightUpOnCharge", serverRuntime);
        if (mPrefs.getStringAsInt("system_autogroupnotif", 1) > 1) SystemNotificationAndShareHooks.AutoGroupNotificationsHook(lpparam);
        if (mPrefs.getStringAsInt("system_vibration", 1) > 1) SystemNotificationMoreHooks.SelectiveVibrationHook(lpparam);
        if (mPrefs.getStringAsInt("system_blocktoasts", 1) > 1) SystemStatusBarAndClockHooks.SelectiveToastsHook(lpparam);
        if (mPrefs.getStringAsInt("system_rotateanim", 1) > 1) SystemDisplayAndWindowHooks.RotationAnimationHook(lpparam);
        if (mPrefs.getStringAsInt("controls_fingerprintsuccess", 1) > 1) Controls.FingerprintHapticSuccessHook(lpparam);
        if (mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 ||
            mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) Controls.VolumeMediaButtonsHook(lpparam);

        if (mPrefs.getBoolean("system_fw_splitscreen")) SystemFreeformAndMultiWindowHooks.MultiWindowPlusHook(lpparam);
        if (mPrefs.getBoolean("system_fw_noblacklist")) SystemFreeformAndMultiWindowHooks.NoFloatingWindowBlacklistHook(lpparam);
        if (mPrefs.getBoolean("various_disable_access_devicelogs")) {
            SystemDisplayAndWindowHooks.NoAccessDeviceLogsRequest(lpparam);
        }
        if (mPrefs.getInt("system_other_wallpaper_scale", 6) > 6) SystemNotificationMoreHooks.WallpaperScaleLevelHook(lpparam);

        watchPreferenceChange();
    }

    @Override
    public void onPackageReady(final PackageReadyParam lpparam) {
        if (!isSupportedAndroidVersion()) return;
        if (!lpparam.isFirstPackage()) return;

        String pkg = lpparam.getPackageName();
        if (
            pkg.equals("com.android.settings") && !"com.android.settings".equals(processName)
            || pkg.equals("com.miui.securitycenter") && "com.miui.securitycenter.bootaware".equals(processName)
            || pkg.equals("com.android.location.fused")
            || pkg.startsWith("com.android.networkstack")
        ) {
            return;
        }

        SharedPreferences remote = getRemotePrefs();
        if (remote == null || !needLoadPrefs(pkg, remote)) return;
        initPrefs();

        if (pkg.equals("android") || pkg.equals("com.android.systemui")) {
            if (mPrefs.getInt("system_statusbarheight", 19) > 19) SystemStatusBarAndClockHooks.StatusBarHeightRes();
            if (mPrefs.getInt("controls_navbarheight", 19) > 19) Controls.NavbarHeightRes();
        }

        if (pkg.equals("android")) {
            if (mPrefs.getBoolean("system_cleanshare")) SystemShareAndOpenWithHooks.CleanShareMenuHook(lpparam);
            if (mPrefs.getBoolean("system_cleanopenwith")) SystemShareAndOpenWithHooks.CleanOpenWithMenuHook(lpparam);
            if (mPrefs.getStringAsInt("system_allrotations2", 1) > 1) {
                MainModule.resHooks.setObjectReplacement("android", "bool", "config_allowAllRotations", MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) == 2);
            }
            if (mPrefs.getStringAsInt("system_rotateanim", 1) > 1) SystemDisplayAndWindowHooks.RotationAnimationRes();
            watchPreferenceChange();
        }

        if (pkg.equals("com.baidu.input")
            || pkg.equals("com.baidu.input_mi")
            || pkg.equals("com.iflytek.inputmethod")
            || pkg.equals("com.iflytek.inputmethod.miui")
            || pkg.equals("com.sohu.inputmethod.sogou")
            || pkg.equals("com.sohu.inputmethod.sogou.xiaomi")
            || pkg.startsWith("com.google.android.inputmethod")
            || pkg.startsWith("com.touchtype.swiftkey")
            || pkg.startsWith("com.tencent.wetype")
        ) {
            if (mPrefs.getBoolean("controls_volumecursor")) Controls.VolumeCursorHook(lpparam);
            if (mPrefs.getBoolean("controls_nonavbar_fix_inputmethod")
                && mPrefs.getBoolean("controls_nonavbar")) {
                Various.FixInputMethodBottomMarginHook(lpparam);
            }
            return;
        }

        if (mPrefs.getBoolean("various_alarmcompat") && mPrefs.getStringSet("various_alarmcompat_apps").contains(pkg)) {
            Various.AlarmCompatHook();
        }

        if (pkg.equals("com.miui.miwallpaper")) {
            if (mPrefs.getBoolean("launcher_disable_wallpaperscale")) LauncherAnimationHooks.DisableUnlockWallpaperScale(lpparam);
        }
        if (pkg.equals("com.android.systemui")) {
            Context mContext = ModuleHelper.findContext(lpparam);
            long restartTime = Settings.System.getLong(mContext.getContentResolver(), "systemui_restart_time", 0L);
            long currentTime = java.lang.System.currentTimeMillis();
            Class<?> NetworkSpeedViewCls = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.getClassLoader());
            if (NetworkSpeedViewCls != null) {
                SystemUIStatusBarHooks.newStyle = LinearLayout.class.isAssignableFrom(NetworkSpeedViewCls);
            }
            ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.getClassLoader(), "onCreate", new MethodHook() {
                private boolean isHooked = false;
                @Override
                protected void after(final AfterHookCallback param) throws Throwable {
                    if (!isHooked) {
                        isHooked = true;
                        Context context = (Context) XposedHelpers.callMethod(param.getThisObject(), "getApplicationContext");
                        SystemUIStatusBarHooks.setupStatusBar(context);
                        watchPreferenceChange();
                    }
                }
            });
            GlobalActions.setupStatusBar(lpparam);

            if (currentTime - restartTime < 10000) {
                return;
            }

            FeatureRuntime systemuiRuntime = FeatureDispatcher.createRuntime(pkg, lpparam, lpparam.getClassLoader(), mPrefs);

            if (mPrefs.getStringAsInt("various_showcallui", 0) > 0
                || mPrefs.getBoolean("controls_volumecursor")
            ) GlobalActions.setupForegroundMonitor(lpparam);

            if (mPrefs.getBoolean("system_screenshot_overlay")) {
                SystemUIScreenshotHooks.TempHideOverlaySystemUIHook(lpparam);
            }

            if (
                mPrefs.getBoolean("system_fivegtile")
                || mPrefs.getBoolean("system_cc_fpstile")
            ) {
                SystemUIMonitorAndTileHooks.AddCustomTileHook(lpparam);
            }

            if (mPrefs.getBoolean("system_hidestatusbar_whenscreenshot")) {
                SystemUIScreenshotHooks.HideStatusBarBeforeScreenshotHook(lpparam);
            }

            if (mPrefs.getInt("system_qsgridcolumns", 2) > 2 || mPrefs.getInt("system_qsgridrows", 1) > 1) SystemUIControlCenterHooks.QSGridRes();
            if (mPrefs.getInt("system_qqsgridcolumns", 2) > 2) SystemUIControlCenterHooks.QQSGridRes();
            if (mPrefs.getBoolean("system_networkindicator_wifi")) FeatureDispatcher.installById("networkIndicatorWifi", systemuiRuntime);

            if (mPrefs.getInt("system_drawer_blur", 100) < 100) SystemDisplayAndWindowHooks.DrawerBlurRatioHook(lpparam);
            if (mPrefs.getInt("system_chargeanimtime", 20) < 20) SystemDisplayAndWindowHooks.ChargeAnimationHook(lpparam);
            if (mPrefs.getInt("system_betterpopups_delay", 0) > 0 && !mPrefs.getBoolean("system_betterpopups_nohide")) SystemNotificationPopupsHooks.BetterPopupsHideDelayHook(lpparam);
            if (mPrefs.getInt("system_netspeedinterval", 4) != 4) SystemUIStatusBarHooks.NetSpeedIntervalHook(lpparam);
            if (mPrefs.getInt("system_qsgridrows", 1) > 1 || mPrefs.getBoolean("system_qsnolabels")) SystemUIControlCenterHooks.QSGridLabelsHook(lpparam);
            if (mPrefs.getInt("system_lstimeout", 3) > 3) SystemAudioAndVisualAndMoreHooks.LockScreenTimeoutHook(lpparam);
            if (mPrefs.getInt("controls_fsg_assist_left_action", 1) > 1
                || mPrefs.getInt("controls_fsg_assist_right_action", 1) > 1
            ) Controls.AssistGestureActionHook(lpparam);
            if (mPrefs.getInt("controls_navbarleft_action", 1) > 1 ||
                    mPrefs.getInt("controls_navbarleftlong_action", 1) > 1 ||
                    mPrefs.getInt("controls_navbarright_action", 1) > 1 ||
                    mPrefs.getInt("controls_navbarrightlong_action", 1) > 1) Controls.NavBarButtonsHook(lpparam);
            if (mPrefs.getBoolean("system_scramblepin")) SystemLockScreenHooks.ScramblePINHook(lpparam);
            if (mPrefs.getBoolean("system_dttosleep")) SystemDisplayAndWindowHooks.DoubleTapToSleepHook(lpparam);
            FeatureDispatcher.installById("statusBarClockTweak", systemuiRuntime);
            if (mPrefs.getBoolean("system_noscreenlock_act")) SystemLockScreenMoreHooks.NoScreenLockHook(lpparam);
            if (
                mPrefs.getBoolean("system_detailednetspeed")
                && !mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
            ) SystemUIStatusBarHooks.DetailedNetSpeedHook(lpparam);
            if (mPrefs.getBoolean("system_albumartonlock")) SystemUILockScreenHooks.LockScreenAlbumArtHook(lpparam);
            if (mPrefs.getStringAsInt("system_expandheadups", 1) > 1) SystemStatusBarClockAndMoreHooks.ExpandHeadsUpHook(lpparam);
            if (mPrefs.getBoolean("system_betterpopups_nohide")) SystemNotificationPopupsHooks.BetterPopupsNoHideHook(lpparam);
            if (mPrefs.getBoolean("system_betterpopups_swipedown")) SystemNotificationPopupsHooks.BetterPopupsSwipeDownHook(lpparam);
            if (mPrefs.getBoolean("system_betterpopups_center")) SystemNotificationMoreHooks.BetterPopupsCenteredHook(lpparam);
            FeatureDispatcher.installById("noMoreIcon", systemuiRuntime);
            if (mPrefs.getBoolean("system_notifafterunlock")) SystemNotificationMoreHooks.ShowNotificationsAfterUnlockHook(lpparam);
            if (mPrefs.getBoolean("system_notifrowmenu")) SystemNotificationMoreHooks.NotificationRowMenuHook(lpparam);
            if (mPrefs.getBoolean("system_compactnotif")) SystemNotificationAndShareHooks.CompactNotificationsHook(lpparam);
            if (mPrefs.getBoolean("system_removedismiss")) FeatureDispatcher.installById("hideDismissView", systemuiRuntime);
            if (mPrefs.getBoolean("system_drawer_removeshortcut")) SystemUINotificationHooks.HideNoficationAccessIconHook(lpparam);
            if (mPrefs.getBoolean("controls_nonavbar")) Controls.HideNavBarHook(lpparam);
            else if (mPrefs.getBoolean("controls_hidenavbar_whenscreenshot")) SystemUIScreenshotHooks.HideNavBarBeforeScreenshotHook(lpparam);
            if (mPrefs.getBoolean("controls_imebackalticon")) Controls.ImeBackAltIconHook(lpparam);
            if (mPrefs.getBoolean("system_visualizer")) SystemAudioAndVisualAndMoreHooks.AudioVisualizerHook(lpparam);
            if (mPrefs.getBoolean("system_nosilentvibrate")
                || mPrefs.getBoolean("system_qs_force_systemfonts")
                || mPrefs.getBoolean("system_volumetimer")
                || mPrefs.getBoolean("system_qsnolabels")
                || mPrefs.getBoolean("system_cc_volume_showpct")
                || mPrefs.getBoolean("system_volumebar_blur_mtk")
                || mPrefs.getBoolean("system_cc_hidedate")
                || mPrefs.getBoolean("system_cc_hide_shortcuticons")
                || mPrefs.getBoolean("system_cc_clocktweak")
                || mPrefs.getBoolean("system_cc_tile_roundedrect")
                || mPrefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1
                || (mPrefs.getBoolean("system_separatevolume") && mPrefs.getBoolean("system_separatevolume_slider"))
                || (mPrefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || mPrefs.getInt("system_volumedialogdelay_expanded", 0) > 0)
                || (mPrefs.getInt("system_volumeblur_collapsed", 0) > 0 || mPrefs.getInt("system_volumeblur_expanded", 0) > 0)
            ) {
                SystemUIControlCenterHooks.MIUIVolumeDialogHook(lpparam);
            }
            FeatureDispatcher.installById("batteryIndicator", systemuiRuntime);
            if (mPrefs.getBoolean("system_disableanynotif")) SystemNotificationMoreHooks.DisableAnyNotificationHook(lpparam);
            if (mPrefs.getBoolean("system_lockscreenshortcuts")) SystemUILockScreenHooks.LockScreenShortcutHook(lpparam);
            if (mPrefs.getBoolean("system_4gtolte")
                || (mPrefs.getBoolean("system_statusbar_mobiletype_single") &&
                    !mPrefs.getString("system_statusbar_mobile_showname", "").equals(""))
            ) SystemStatusBarMoreHooks.MobileNetworkTypeHook(lpparam);
            boolean moveRight = mPrefs.getBoolean("system_statusbar_netspeed_atright")
                || mPrefs.getBoolean("system_statusbar_alarm_atright")
                || mPrefs.getBoolean("system_statusbar_sound_atright")
                || mPrefs.getBoolean("system_statusbar_dnd_atright")
                || mPrefs.getBoolean("system_statusbar_nfc_atright")
                || mPrefs.getBoolean("system_statusbar_btbattery_atright")
                || mPrefs.getBoolean("system_statusbar_headset_atright")
                || mPrefs.getBoolean("system_statusbar_vpn_atright");
            boolean moveLeft = mPrefs.getBoolean("system_statusbar_alarm_atleft")
                || mPrefs.getBoolean("system_statusbar_sound_atleft")
                || mPrefs.getBoolean("system_statusbar_dnd_atleft")
                || mPrefs.getBoolean("system_statusbar_gps_atleft");
            if (moveRight || moveLeft
                || mPrefs.getBoolean("system_statusbar_netspeed_atleft")
                || (mPrefs.getBoolean("system_statusbar_dualrows") && mPrefs.getBoolean("system_statusbar_netspeed_atsecondrow"))
                || mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft")
                || mPrefs.getBoolean("system_statusbaricons_swap_wifi_mobile")
            ) {
                SystemUIStatusBarHooks.StatusBarIconsPositionAdjustHook(lpparam, moveRight, moveLeft);
            }
            if (mPrefs.getStringAsInt("system_statusbar_clock_position", 1) > 1 && !mPrefs.getBoolean("system_statusbar_dualrows")) {
                SystemUIStatusBarHooks.StatusBarClockPositionHook(lpparam);
            }
            if (mPrefs.getBoolean("system_statusbar_batterystyle")) {
                SystemUIBatteryHooks.StatusBarStyleBatteryIconHook(lpparam);
            }
            if (mPrefs.getBoolean("system_statusbar_batterytempandcurrent")
                || mPrefs.getBoolean("system_statusbar_showdevicetemperature")
            ) SystemUIStatusBarHooks.MonitorDeviceInfoHook(lpparam);
            if (mPrefs.getBoolean("system_statusbar_topmargin") && mPrefs.getBoolean("system_statusbar_topmargin_unset_lockscreen")) SystemUILockScreenHooks.LockScreenTopMarginHook(lpparam);
            if (mPrefs.getBoolean("system_statusbar_horizmargin")) SystemUIStatusBarHooks.HorizMarginHook(lpparam);
            if (mPrefs.getBoolean("system_showpct")) SystemUIControlCenterHooks.BrightnessPctHook(lpparam);
            if (mPrefs.getBoolean("system_hidelsstatusbar")) SystemLockScreenMoreHooks.HideLockScreenStatusBarHook(lpparam);
            if (mPrefs.getBoolean("system_hidelsclock")) SystemLockScreenMoreHooks.HideLockScreenClockHook(lpparam);
            if (mPrefs.getBoolean("system_ls_force_systemfonts")) SystemUIStatusBarHooks.ForceClockUseSystemFontsHook(lpparam);
            if (mPrefs.getBoolean("system_hidelshint")) FeatureDispatcher.installById("hideLockScreenHint", systemuiRuntime);
            if (mPrefs.getBoolean("system_allowdirectreply")) SystemAudioAndVisualAndMoreHooks.AllowDirectReplyHook(lpparam);
            if (mPrefs.getBoolean("system_allownotifonkeyguard")) SystemAudioAndVisualAndMoreHooks.AllowAllKeyguardHook(lpparam);
            if (mPrefs.getBoolean("system_allownotiffloat")) SystemAudioAndVisualAndMoreHooks.AllowAllFloatHook(lpparam);
            if (mPrefs.getBoolean("system_hideqs")) SystemAudioAndVisualAndMoreHooks.HideQSHook(lpparam);
            if (mPrefs.getBoolean("system_lsalarm")) SystemAudioAndVisualAndMoreHooks.LockScreenAlarmHook(lpparam);
            if (mPrefs.getBoolean("system_statusbarcontrols")) SystemUIControlCenterHooks.StatusBarGesturesHook(lpparam);
            if (mPrefs.getBoolean("system_nonetspeedseparator")) FeatureDispatcher.installById("noNetworkSpeedSeparator", systemuiRuntime);
            if (mPrefs.getBoolean("system_statusbaricons_clock")) FeatureDispatcher.installById("hideIconsClock", systemuiRuntime);
            if (mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || (!mPrefs.getBoolean("system_detailednetspeed")
                    && (mPrefs.getBoolean("system_detailednetspeed_secunit")
                        || mPrefs.getBoolean("system_detailednetspeed_low")
                        )
                    )
            ) {
                SystemUIStatusBarHooks.FormatNetworkSpeedHook(lpparam);
            }
            if (
                mPrefs.getInt("system_netspeed_fontsize", 13) > 13
                || mPrefs.getInt("system_netspeed_verticaloffset", 8) != 8
                || mPrefs.getBoolean("system_detailednetspeed")
                || mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || mPrefs.getBoolean("system_netspeed_bold")
                || mPrefs.getInt("system_netspeed_leftmargin", 0) > 0
                || mPrefs.getInt("system_netspeed_fixedcontent_width", 10) > 10
                || mPrefs.getInt("system_netspeed_rightmargin", 0) > 0
                || mPrefs.getStringAsInt("system_detailednetspeed_align", 1) > 1
            ) {
                SystemUIStatusBarHooks.NetSpeedStyleHook(lpparam);
            }
            if (mPrefs.getBoolean("system_taptounlock")) SystemAudioAndVisualAndMoreHooks.TapToUnlockHook(lpparam);
            if (mPrefs.getBoolean("system_nosos")) SystemSecurityAndSystemHooks.NoSOSHook(lpparam);
            if (mPrefs.getBoolean("system_morenotif")) SystemNotificationMoreHooks.MoreNotificationsHook(lpparam);
            if (mPrefs.getBoolean("system_charginginfo")) SystemChargingAndWallpaperHooks.ChargingInfoHook(lpparam);
            if (mPrefs.getBoolean("system_secureqs")) SystemUILockScreenHooks.SecureQSTilesHook(lpparam);
            if (mPrefs.getBoolean("system_mutevisiblenotif")) FeatureDispatcher.installById("muteVisibleNotifications", systemuiRuntime);
            if (mPrefs.getBoolean("system_statusbaricons_battery1")) SystemStatusBarMoreHooks.HideIconsBattery1Hook(lpparam);
            if (mPrefs.getBoolean("system_statusbaricons_battery3")
                || mPrefs.getBoolean("system_statusbaricons_battery4")
                || mPrefs.getBoolean("system_statusbaricons_battery2")
            ) SystemStatusBarMoreHooks.HideIconsBattery2Hook(lpparam);
            if (mPrefs.getStringAsInt("system_statusbaricons_wifistandard", 1) > 1) SystemStatusBarMoreHooks.DisplayWifiStandardHook(lpparam);
            if (mPrefs.getBoolean("system_statusbaricons_signal")
                || mPrefs.getBoolean("system_statusbaricons_sim1")
                || mPrefs.getBoolean("system_statusbaricons_sim2")
                || mPrefs.getBoolean("system_statusbaricons_sim_nodata")
                || mPrefs.getBoolean("system_statusbaricons_roaming")
                || mPrefs.getBoolean("system_statusbaricons_volte")
            ) SystemUIStatusBarHooks.HideIconsSignalHook(lpparam);
            if (mPrefs.getBoolean("system_statusbaricons_vowifi")) SystemUIStatusBarHooks.HideIconsVoWiFiHook(lpparam);
            if (!mPrefs.getBoolean("system_statusbaricons_alarm") && mPrefs.getInt("system_statusbaricons_alarmn", 0) > 0) SystemStatusBarMoreHooks.HideIconsSelectiveAlarmHook(lpparam);
            if (!mPrefs.getString("system_shortcut_app", "").equals("")
                || !mPrefs.getString("system_calendar_app", "").equals("")
                || !mPrefs.getString("system_clock_app", "").equals("")) SystemUINotificationHooks.ReplaceShortcutAppHook(lpparam);
            if (mPrefs.getStringAsInt("system_qshaptics", 1) > 1) SystemNotificationAndShareHooks.QSHapticHook(lpparam);
            if (mPrefs.getBoolean("system_qs_hideoperator")) SystemSettingsAndConnectivityHooks.HideCCOperatorHook(lpparam);
            if (mPrefs.getBoolean("system_cc_hideoperator_delimiter")) SystemSettingsAndConnectivityHooks.HideCCOperatorDelimiterHook(lpparam);
            if (mPrefs.getBoolean("system_cc_show_stepcount")
                || mPrefs.getBoolean("system_drawer_show_stepcount")
            ) SystemUIControlCenterHooks.ShowCCStepCountHook(lpparam);
            if (mPrefs.getBoolean("system_cc_disable_bluetooth_restrict")) SystemSettingsAndConnectivityHooks.DisableBluetoothRestrictHook(lpparam);
            if (mPrefs.getBoolean("system_cc_collapse_after_clicked")) SystemSettingsAndConnectivityHooks.CollapseCCAfterClickHook(lpparam);
            if (mPrefs.getBoolean("system_cc_switch_qsandnotification")) SystemUIControlCenterHooks.SwitchCCAndNotificationHook(lpparam);
            if (mPrefs.getStringAsInt("system_expandnotifs", 1) > 1) SystemStatusBarClockAndMoreHooks.ExpandNotificationsHook(lpparam);
            if (mPrefs.getStringAsInt("system_inactivebrightness", 1) > 1) SystemAudioAndVisualAndMoreHooks.InactiveBrightnessSliderHook(lpparam);
            if (mPrefs.getStringAsInt("system_mobiletypeicon", 1) > 1
                || mPrefs.getBoolean("system_networkindicator_mobile")
                || mPrefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")
            ) {
                SystemUIStatusBarHooks.HideMobileNetworkIndicatorHook(lpparam);
            }
            if (mPrefs.getStringAsInt("system_statusbaricons_bluetooth", 1) > 1) SystemStatusBarMoreHooks.HideIconsBluetoothHook(lpparam);
            if (mPrefs.getBoolean("system_epm")) SystemUINotificationHooks.ExtendedPowerMenuHook(lpparam);

            boolean hideIconsActive =
                mPrefs.getBoolean("system_statusbaricons_wifi") ||
                mPrefs.getBoolean("system_statusbaricons_dualwifi") ||
                mPrefs.getBoolean("system_statusbaricons_alarm") ||
                mPrefs.getBoolean("system_statusbaricons_profile") ||
                mPrefs.getBoolean("system_statusbaricons_sound") ||
                mPrefs.getBoolean("system_statusbaricons_dnd") ||
                mPrefs.getBoolean("system_statusbaricons_secondspace") ||
                mPrefs.getBoolean("system_statusbaricons_headset") ||
                mPrefs.getBoolean("system_statusbaricons_nfc") ||
                mPrefs.getBoolean("system_statusbaricons_vpn") ||
                mPrefs.getBoolean("system_statusbaricons_airplane") ||
                mPrefs.getBoolean("system_statusbaricons_hotspot") ||
                mPrefs.getBoolean("system_statusbaricons_nosims") ||
                mPrefs.getBoolean("system_statusbaricons_gps") ||
                mPrefs.getBoolean("system_statusbaricons_btbattery") ||
                mPrefs.getBoolean("system_statusbaricons_ble_unlock") ||
                mPrefs.getBoolean("system_statusbaricons_volte");
            if (hideIconsActive) SystemUIStatusBarHooks.HideIconsHook(lpparam);

            if (
                mPrefs.getBoolean("system_statusbaricons_privacy")
                || mPrefs.getBoolean("system_statusbaricons_mute")
                || mPrefs.getBoolean("system_statusbaricons_speaker")
                || mPrefs.getBoolean("system_statusbaricons_record")
            ) SystemUIStatusBarHooks.HideIconsFromSystemManager(lpparam);
            if (mPrefs.getInt("system_messagingstylelines", 0) > 0) SystemFreeformAndMultiWindowHooks.MessagingStyleLinesHook(lpparam);
            if (mPrefs.getBoolean("system_betterpopups_allowfloat")) SystemFreeformAndMultiWindowHooks.BetterPopupsAllowFloatHook(lpparam);
            if (mPrefs.getBoolean("system_betterpopups_autoclose_expanded")) SystemNotificationMoreHooks.AutoDismissExpandedPopupsHook(lpparam);
            if (mPrefs.getBoolean("system_betterpopups_disablewhenmute")) SystemUINotificationHooks.DisableHeadsUpWhenMuteHook(lpparam);
            if (mPrefs.getBoolean("system_securecontrolcenter")) SystemFreeformAndMultiWindowHooks.SecureControlCenterHook(lpparam);
            if (mPrefs.getBoolean("system_minimalnotifview")) SystemNotificationMoreHooks.MinimalNotificationViewHook(lpparam);
            if (mPrefs.getBoolean("system_notifchannelsettings")) SystemNotificationMoreHooks.NotificationChannelSettingsHook(lpparam);
            if (mPrefs.getStringAsInt("system_maxsbicons", 0) != 0) SystemNotificationMoreHooks.MaxNotificationIconsHook(lpparam);
            if (mPrefs.getBoolean("system_statusbar_mobiletype_single")) {
                SystemUIStatusBarHooks.MobileTypeSingleHook(lpparam);
            }
            if (mPrefs.getBoolean("system_statusbar_dualsimin2rows")) {
                SystemUIStatusBarHooks.DualRowSignalHook(lpparam);
            }
            if (mPrefs.getBoolean("system_statusbar_dualrows")) {
                SystemUIStatusBarHooks.DualRowStatusbarHook(lpparam);
            }
            if (mPrefs.getInt("system_ccgridcolumns", 4) > 4 || mPrefs.getInt("system_ccgridrows", 4) != 4) SystemUIControlCenterHooks.SystemCCGridHook(lpparam);
            if (mPrefs.getStringAsInt("system_colorizenotifs", 1) > 1) SystemNotificationAndShareHooks.ColorizeNotificationCardHook(lpparam);
            if (mPrefs.getBoolean("system_notify_openinfw")) SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam);
            if (mPrefs.getBoolean("system_fw_noblacklist")) SystemFreeformAndMultiWindowHooks.DisableSideBarSuggestionHook(lpparam);

            if (mPrefs.getBoolean("system_notify_openinfw")
                || mPrefs.getBoolean("system_notifrowmenu")
                || mPrefs.getBoolean("system_betterpopups_allowfloat")
            ) {
                SystemUINotificationHooks.FixOpenNotifyInFreeFormHook(lpparam);
            }
            if (mPrefs.getBoolean("system_nosafevolume")) {
                SystemUIControlCenterHooks.HideSafeVolumeDlgHook(lpparam);
            }
            if (mPrefs.getBoolean("system_lockscreen_hidezenmode")) {
                SystemUILockScreenHooks.HideLockscreenZenModeHook(lpparam);
            }
            if (mPrefs.getBoolean("system_nopassword")) SystemLockScreenHooks.NoPasswordHook(lpparam);
        }

        if (pkg.equals("com.lbe.security.miui")) {
            if (mPrefs.getStringAsInt("various_clipboard_defaultaction", 1) > 1) {
                Various.SmartClipboardActionHook(lpparam);
            }
        }

        if (pkg.equals("com.android.incallui")) {
            if (mPrefs.getStringAsInt("various_showcallui", 0) > 0) Various.ShowCallUIHook(lpparam);
            if (mPrefs.getBoolean("various_calluibright")) Various.InCallBrightnessHook(lpparam);
            if (mPrefs.getBoolean("various_answerinheadup")) Various.AnswerCallInHeadUpHook(lpparam);
        }

        if (pkg.equals("com.miui.securitycenter")) {
            if (mPrefs.getBoolean("various_appdetails")) Various.AppInfoHook(lpparam);
            if (mPrefs.getBoolean("various_disableapp")) Various.AppsDisableHook(lpparam);
            if (mPrefs.getBoolean("various_restrictapp")) Various.AppsRestrictHook(lpparam);
            if (mPrefs.getBoolean("system_applock_scramblepin")) SystemAudioAndVisualAndMoreHooks.ScrambleAppLockPINHook(lpparam);
            if (mPrefs.getStringAsInt("various_appsort", 1) > 1) Various.AppsDefaultSortHook(lpparam);
            if (mPrefs.getStringAsInt("various_skip", 0) > 0) Various.AppsDefaultSortHook(lpparam);
            if (mPrefs.getBoolean("various_skip_interceptperm")) Various.InterceptPermHook(lpparam);
            if (mPrefs.getBoolean("various_replace_defaultopen_with_openbydefault")) Various.OpenByDefaultHook(lpparam);
            if (mPrefs.getBoolean("various_skip_securityscan")) Various.SkipSecurityScanHook(lpparam);
            if (mPrefs.getBoolean("various_show_battery_temperature")) Various.ShowTempInBatteryHook(lpparam);
            if (mPrefs.getBoolean("various_enable_sc_ai_clipboard_location")) Various.UnlockClipboardAndLocationHook(lpparam);
            if (mPrefs.getBoolean("various_disable_freeform_suggest_blacklist")) SystemFreeformAndMultiWindowHooks.DisableSideBarSuggestionHook(lpparam);
            if (mPrefs.getBoolean("various_disable_dock_suggest")) Various.DisableDockSuggestHook(lpparam);
            if (mPrefs.getBoolean("various_enable_expand_sidebar")) {
                Various.AddSideBarExpandReceiverHook(lpparam);
            }
            if (mPrefs.getBoolean("system_hidelowbatwarn")) {
                Various.NoLowBatteryWarningHook();
            }
            if (mPrefs.getBoolean("various_privacyapps_column_nums4")) {
                Various.PrivacyAppsLayoutHook(lpparam);
            }
        }

        if (pkg.equals("com.miui.powerkeeper")) {
            if (mPrefs.getBoolean("various_restrictapp")) Various.AppsRestrictPowerHook(lpparam);
            if (mPrefs.getBoolean("various_persist_batteryoptimization")) Various.PersistBatteryOptimizationHook(lpparam);
        }

        if (pkg.equals("com.android.settings")) {
            if (mPrefs.getStringAsInt("miuizer_settingsiconpos", 1) > 0) {
                GlobalActions.miuizerSettingsHook(lpparam);
            }
            if (mPrefs.getBoolean("system_separatevolume")) {
                SystemAudioAndVolumeHooks.NotificationVolumeSettingsRes();
                SystemAudioAndVolumeHooks.NotificationVolumeSettingsHook(lpparam);
            }
            if (mPrefs.getBoolean("system_disableanynotif")) {
                SystemNotificationMoreHooks.DisableAnyNotificationHook(lpparam);
                SystemNotificationMoreHooks.DisableAnyNotificationBlockHook(lpparam);
            }
            if (!mPrefs.getString("system_defaultusb", "none").equals("none")) SystemSettingsMoreHooks.USBConfigSettingsHook(lpparam);
            if (mPrefs.getBoolean("system_notifimportance")) {
                SystemNotificationMoreHooks.NotificationImportanceHook(lpparam);
            }
            if (mPrefs.getBoolean("system_wifipassword")) {
                SystemSettingsAndConnectivityHooks.ViewWifiPasswordHook(lpparam);
            }
        }

        if (pkg.startsWith("com.google.android.inputmethod")) {
            if (mPrefs.getInt("various_gboardpadding_port", 0) > 0 || mPrefs.getInt("various_gboardpadding_land", 0) > 0) Various.GboardPaddingHook(lpparam);
        }

        if (pkg.equals("com.miui.packageinstaller")) {
//            Legacy MIUI signature hook remains intentionally disabled.
            if (mPrefs.getBoolean("various_miuiinstaller")) Various.MiuiPackageInstallerHook(lpparam);
            if (mPrefs.getBoolean("various_installappinfo")) Various.AppInfoDuringMiuiInstallHook(lpparam);
        }

        if (pkg.equals("com.miui.screenshot")) {
//            resHooks.setResReplacement(pkg, "array", "config_forbidenLongScreenshot", R.array.config_forbidenLongScreenshot);
            if (mPrefs.getBoolean("system_screenshot")) SystemAudioAndVisualAndMoreHooks.ScreenshotConfigHook(lpparam);
            if (mPrefs.getInt("system_screenshot_floattime", 0) > 0) SystemAudioAndVisualAndMoreHooks.ScreenshotFloatTimeHook(lpparam);
        }

        if (pkg.equals("com.miui.gallery")) {
            int folder = mPrefs.getStringAsInt("system_gallery_screenshots_path", 1);
            if (folder > 1) {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(lpparam);
            }
        }

        final boolean isMIUILauncherPkg = pkg.equals("com.miui.home");
        final boolean isLauncherPkg = isMIUILauncherPkg || pkg.equals("com.mi.android.globallauncher");

        if (isLauncherPkg) {
            if (mPrefs.getInt("launcher_horizmargin", 0) > 0) LauncherLayoutHooks.HorizontalSpacingRes();
            if (mPrefs.getInt("launcher_indicatorheight", 9) > 9) LauncherLayoutHooks.IndicatorHeightRes();
            if (mPrefs.getInt("launcher_indicator_topmargin", 0) > 0) LauncherLayoutHooks.IndicatorMarginTopHook(lpparam);
            if (mPrefs.getBoolean("launcher_unlockgrids")) {
                LauncherLayoutHooks.UnlockGridsRes();
                LauncherLayoutHooks.UnlockGridsHook(lpparam);
            }
            if (mPrefs.getBoolean("launcher_docktitles")) LauncherLayoutHooks.ShowHotseatTitlesRes();
            if (mPrefs.getBoolean("launcher_disable_log")) {
                LauncherSystemHooks.DisableLauncherLogHook(lpparam);
            }
            if (mPrefs.getInt("launcher_topmargin", 0) > 0) LauncherLayoutHooks.WorkspaceCellPaddingTopHook(lpparam);
            if (mPrefs.getInt("launcher_dock_topmargin", 0) > 0) LauncherLayoutHooks.DockMarginTopHook(lpparam);
            if (mPrefs.getInt("launcher_dock_bottommargin", 0) > 0) LauncherLayoutHooks.DockMarginBottomHook(lpparam);

            watchPreferenceChange();
        }

        final boolean isStatusBarColor = mPrefs.getBoolean("system_statusbarcolor") && mPrefs.getStringSet("system_statusbarcolor_apps").contains(pkg);
        final boolean isNoOverscroll = mPrefs.getBoolean("system_nooverscroll") && mPrefs.getStringSet("system_nooverscroll_apps").contains(pkg);
        final boolean controlMedia = (mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0
            || mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) && mPrefs.getStringSet("controls_mediaplayer_apps").contains(pkg);
        if (isLauncherPkg || isStatusBarColor || isNoOverscroll || controlMedia) {
            ModuleHelper.findAndHookMethod(Application.class, "attach", Context.class, new MethodHook() {
                @Override
                protected void after(AfterHookCallback param) throws Throwable {
                    if (isLauncherPkg) handleLoadLauncher(lpparam);
                    if (isStatusBarColor) {
                        SystemStatusBarAndClockHooks.StatusBarBackgroundCompatHook(lpparam);
                        SystemStatusBarAndClockHooks.StatusBarBackgroundHook(lpparam);
                    }
                    if (isNoOverscroll) SystemAudioAndVisualAndMoreHooks.NoOverscrollAppHook(lpparam);
                    if (controlMedia) Controls.VolumeMediaPlayerHook(lpparam);
                }
            });
        }
    }

    private void handleLoadLauncher(final PackageReadyParam lpparam) {
        boolean closeOnLaunch = false;
        FeatureRuntime launcherRuntime = FeatureDispatcher.createRuntime(lpparam.getPackageName(), lpparam, lpparam.getClassLoader(), mPrefs);
        if (mPrefs.getInt("launcher_swipedown_action", 1) != 1 ||
                mPrefs.getInt("launcher_swipeup_action", 1) != 1 ||
                mPrefs.getInt("launcher_swipedown2_action", 1) != 1 ||
                mPrefs.getInt("launcher_swipeup2_action", 1) != 1) LauncherGestureHooks.HomescreenSwipesHook(lpparam);
        if (mPrefs.getInt("launcher_swipeleft_action", 1) != 1 ||
                mPrefs.getInt("launcher_swiperight_action", 1) != 1) LauncherGestureHooks.HotSeatSwipesHook(lpparam);
        if (mPrefs.getInt("launcher_shake_action", 1) != 1) LauncherGestureHooks.ShakeHook(lpparam);
        if (mPrefs.getInt("launcher_doubletap_action", 1) != 1) LauncherGestureHooks.LauncherDoubleTapHook(lpparam);
        if (mPrefs.getInt("launcher_pinch_action", 1) != 1) LauncherGestureHooks.LauncherPinchHook(lpparam);
        if (mPrefs.getInt("launcher_folder_cols", 1) > 1) FeatureDispatcher.installById("folderColumns", launcherRuntime);
        if (mPrefs.getInt("launcher_iconscale", 45) > 45) LauncherIconHooks.IconScaleHook(lpparam);
        if (mPrefs.getInt("launcher_titlefontsize", 5) > 5) LauncherIconHooks.TitleFontSizeHook(lpparam);
        if (mPrefs.getInt("launcher_titletopmargin", 0) > 0) FeatureDispatcher.installById("titleTopMargin", launcherRuntime);
        FeatureDispatcher.installById("noClockHide", launcherRuntime);
        if (mPrefs.getBoolean("launcher_renameapps")) LauncherIconHooks.RenameShortcutsHook(lpparam);
        if (mPrefs.getBoolean("launcher_darkershadow")) LauncherIconHooks.TitleShadowHook(lpparam);
        if (mPrefs.getBoolean("controls_nonavbar")) LauncherLayoutHooks.HideNavBarHook(lpparam);
        if (mPrefs.getBoolean("launcher_infinitescroll")) LauncherLayoutHooks.InfiniteScrollHook(lpparam);
        if (mPrefs.getBoolean("launcher_hidetitles")) FeatureDispatcher.installById("hideLauncherTitles", launcherRuntime);
        if (mPrefs.getBoolean("launcher_fixlaunch")) FeatureDispatcher.installById("fixAppInfoLaunch", launcherRuntime);
        FeatureDispatcher.installById("noWidgetOnly", launcherRuntime);
        if (mPrefs.getBoolean("launcher_sensorportrait")) LauncherAnimationHooks.ReverseLauncherPortraitHook(lpparam);
        if (mPrefs.getBoolean("launcher_unlockhotseat")) LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam);
        if (mPrefs.getStringAsInt("launcher_closefolders", 1) > 1) { LauncherFolderHooks.CloseFolderOnLaunchHook(lpparam); closeOnLaunch = true; }
        if ("com.miui.home".equals(lpparam.getPackageName())) {
            if (mPrefs.getInt("system_recents_blur", 100) < 100) LauncherAnimationHooks.RecentsBlurRatioHook(lpparam);
            if (mPrefs.getInt("controls_fsg_coverage", 60) != 60) Controls.BackGestureAreaHeightHook(lpparam);
            if (mPrefs.getInt("controls_fsg_width", 100) > 100) Controls.BackGestureAreaWidthHook(lpparam);
            if (mPrefs.getBoolean("controls_fsg_horiz")) LauncherGestureHooks.FSGesturesHook(lpparam);
            if (mPrefs.getBoolean("system_removecleaner")) SystemStatusBarAndClockHooks.HideMemoryCleanHook(lpparam, true);
            if (mPrefs.getBoolean("system_recents_disable_wallpaperscale") || mPrefs.getBoolean("launcher_disable_wallpaperscale")) LauncherAnimationHooks.DisableLauncherWallpaperScale(lpparam);
            if (mPrefs.getBoolean("system_fw_sticky")) LauncherSystemHooks.StickyFloatingWindowsLauncherHook(lpparam);
            if (mPrefs.getBoolean("system_recents_hide_statusbar")) LauncherSystemHooks.HideStatusBarInRecentsHook(lpparam);
            if (mPrefs.getBoolean("system_fw_splitscreen")) SystemFreeformAndMultiWindowHooks.MultiWindowPlusHook(lpparam);
            if (mPrefs.getBoolean("launcher_fixanim")) LauncherAnimationHooks.FixAnimHook(lpparam);
            if (mPrefs.getBoolean("launcher_hideseekpoints")) LauncherLayoutHooks.HideSeekPointsHook(lpparam);
            if (mPrefs.getBoolean("launcher_privacyapps_gest")
                || mPrefs.getInt("launcher_spread_action", 1) != 1) LauncherFolderHooks.PrivacyFolderHook(lpparam);
            if (mPrefs.getBoolean("system_hidefromrecents")) LauncherSystemHooks.HideFromRecentsHook(lpparam);
            if (mPrefs.getInt("launcher_folderblur_opacity", 0) > 0) LauncherFolderHooks.FolderBlurHook(lpparam);
            if (mPrefs.getBoolean("launcher_nounlockanim")) FeatureDispatcher.installById("noUnlockAnimation", launcherRuntime);
            if (mPrefs.getBoolean("launcher_nozoomanim")) LauncherAnimationHooks.NoZoomAnimationHook(lpparam);
            if (mPrefs.getBoolean("launcher_oldlaunchanim")) LauncherAnimationHooks.UseOldLaunchAnimationHook(lpparam);
            if (mPrefs.getBoolean("launcher_closedrawer")) { LauncherSystemHooks.CloseDrawerOnLaunchHook(lpparam); closeOnLaunch = true; }
            if (mPrefs.getInt("launcher_horizwidgetmargin", 0) > 0) LauncherLayoutHooks.HorizontalWidgetSpacingHook(lpparam);
            if (mPrefs.getInt("controls_fsg_assist_left_action", 1) > 1
                || mPrefs.getInt("controls_fsg_assist_right_action", 1) > 1
            )  LauncherGestureHooks.AssistGestureActionHook(lpparam);
            if (mPrefs.getInt("controls_fsg_swipeandstop_action", 1) > 1) LauncherGestureHooks.SwipeAndStopActionHook(lpparam);
        }
        if (closeOnLaunch) LauncherFolderHooks.CloseFolderOrDrawerOnLaunchShortcutMenuHook(lpparam);
        if (mPrefs.getBoolean("system_resizablewidgets")) LauncherLayoutHooks.ResizableWidgetsHook(lpparam);
    }

    private boolean needGlobalActions() {
        try {
            for (Map.Entry<String, ?> entry : mPrefs.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && key.endsWith("_action") && value instanceof Integer && (Integer) value > 1) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        if (mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) {
            return !mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();
        }
        return false;
    }

}
