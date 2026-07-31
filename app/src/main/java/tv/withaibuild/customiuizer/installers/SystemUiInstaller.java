package tv.withaibuild.customiuizer.installers;

import android.content.Context;
import android.provider.Settings;
import android.widget.LinearLayout;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.GlobalActions;
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
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher;
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper;
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers;

public final class SystemUiInstaller {

    private SystemUiInstaller() {}

    public static void install(final PackageReadyParam lpparam, final Runnable watchPreferences) {
        String pkg = lpparam.getPackageName();
        if (pkg.equals("android") || pkg.equals("com.android.systemui")) {
            if (MainModule.mPrefs.getInt("system_statusbarheight", 19) > 19) SystemStatusBarAndClockHooks.StatusBarHeightRes();
            if (MainModule.mPrefs.getInt("controls_navbarheight", 19) > 19) Controls.NavbarHeightRes();
        }
        if (pkg.equals("android")) {
            if (MainModule.mPrefs.getBoolean("system_cleanshare")) SystemShareAndOpenWithHooks.CleanShareMenuHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_cleanopenwith")) SystemShareAndOpenWithHooks.CleanOpenWithMenuHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) > 1) {
                MainModule.resHooks.setObjectReplacement("android", "bool", "config_allowAllRotations", MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) == 2);
            }
            if (MainModule.mPrefs.getStringAsInt("system_rotateanim", 1) > 1) SystemDisplayAndWindowHooks.RotationAnimationRes();
            watchPreferences.run();
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
                        watchPreferences.run();
                    }
                }
            });
            GlobalActions.setupStatusBar(lpparam);

            if (currentTime - restartTime < 10000) {
                return;
            }

            FeatureRuntime systemuiRuntime = FeatureDispatcher.createRuntime(pkg, lpparam, lpparam.getClassLoader(), MainModule.mPrefs);

            if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0
                || MainModule.mPrefs.getBoolean("controls_volumecursor")
            ) GlobalActions.setupForegroundMonitor(lpparam);

            if (MainModule.mPrefs.getBoolean("system_screenshot_overlay")) {
                SystemUIScreenshotHooks.TempHideOverlaySystemUIHook(lpparam);
            }

            if (
                MainModule.mPrefs.getBoolean("system_fivegtile")
                || MainModule.mPrefs.getBoolean("system_cc_fpstile")
            ) {
                SystemUIMonitorAndTileHooks.AddCustomTileHook(lpparam);
            }

            if (MainModule.mPrefs.getBoolean("system_hidestatusbar_whenscreenshot")) {
                SystemUIScreenshotHooks.HideStatusBarBeforeScreenshotHook(lpparam);
            }

            if (MainModule.mPrefs.getInt("system_qsgridcolumns", 2) > 2 || MainModule.mPrefs.getInt("system_qsgridrows", 1) > 1) SystemUIControlCenterHooks.QSGridRes();
            if (MainModule.mPrefs.getInt("system_qqsgridcolumns", 2) > 2) SystemUIControlCenterHooks.QQSGridRes();
            if (MainModule.mPrefs.getBoolean("system_networkindicator_wifi")) FeatureDispatcher.installById("networkIndicatorWifi", systemuiRuntime);

            if (MainModule.mPrefs.getInt("system_drawer_blur", 100) < 100) SystemDisplayAndWindowHooks.DrawerBlurRatioHook(lpparam);
            if (MainModule.mPrefs.getInt("system_chargeanimtime", 20) < 20) SystemDisplayAndWindowHooks.ChargeAnimationHook(lpparam);
            if (MainModule.mPrefs.getInt("system_betterpopups_delay", 0) > 0 && !MainModule.mPrefs.getBoolean("system_betterpopups_nohide")) SystemNotificationPopupsHooks.BetterPopupsHideDelayHook(lpparam);
            if (MainModule.mPrefs.getInt("system_netspeedinterval", 4) != 4) SystemUIStatusBarHooks.NetSpeedIntervalHook(lpparam);
            if (MainModule.mPrefs.getInt("system_qsgridrows", 1) > 1 || MainModule.mPrefs.getBoolean("system_qsnolabels")) SystemUIControlCenterHooks.QSGridLabelsHook(lpparam);
            if (MainModule.mPrefs.getInt("system_lstimeout", 3) > 3) SystemAudioAndVisualAndMoreHooks.LockScreenTimeoutHook(lpparam);
            if (MainModule.mPrefs.getInt("controls_fsg_assist_left_action", 1) > 1
                || MainModule.mPrefs.getInt("controls_fsg_assist_right_action", 1) > 1
            ) Controls.AssistGestureActionHook(lpparam);
            if (MainModule.mPrefs.getInt("controls_navbarleft_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarleftlong_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarright_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarrightlong_action", 1) > 1) Controls.NavBarButtonsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_scramblepin")) SystemLockScreenHooks.ScramblePINHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_dttosleep")) SystemDisplayAndWindowHooks.DoubleTapToSleepHook(lpparam);
            FeatureDispatcher.installById("statusBarClockTweak", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_noscreenlock_act")) SystemLockScreenMoreHooks.NoScreenLockHook(lpparam);
            if (
                MainModule.mPrefs.getBoolean("system_detailednetspeed")
                && !MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
            ) SystemUIStatusBarHooks.DetailedNetSpeedHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_albumartonlock")) SystemUILockScreenHooks.LockScreenAlbumArtHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_expandheadups", 1) > 1) SystemStatusBarClockAndMoreHooks.ExpandHeadsUpHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_betterpopups_nohide")) SystemNotificationPopupsHooks.BetterPopupsNoHideHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_betterpopups_swipedown")) SystemNotificationPopupsHooks.BetterPopupsSwipeDownHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_betterpopups_center")) SystemNotificationMoreHooks.BetterPopupsCenteredHook(lpparam);
            FeatureDispatcher.installById("noMoreIcon", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_notifafterunlock")) SystemNotificationMoreHooks.ShowNotificationsAfterUnlockHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_notifrowmenu")) SystemNotificationMoreHooks.NotificationRowMenuHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_compactnotif")) SystemNotificationAndShareHooks.CompactNotificationsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_removedismiss")) FeatureDispatcher.installById("hideDismissView", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_drawer_removeshortcut")) SystemUINotificationHooks.HideNoficationAccessIconHook(lpparam);
            if (MainModule.mPrefs.getBoolean("controls_nonavbar")) Controls.HideNavBarHook(lpparam);
            else if (MainModule.mPrefs.getBoolean("controls_hidenavbar_whenscreenshot")) SystemUIScreenshotHooks.HideNavBarBeforeScreenshotHook(lpparam);
            if (MainModule.mPrefs.getBoolean("controls_imebackalticon")) Controls.ImeBackAltIconHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_visualizer")) SystemAudioAndVisualAndMoreHooks.AudioVisualizerHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_nosilentvibrate")
                || MainModule.mPrefs.getBoolean("system_qs_force_systemfonts")
                || MainModule.mPrefs.getBoolean("system_volumetimer")
                || MainModule.mPrefs.getBoolean("system_qsnolabels")
                || MainModule.mPrefs.getBoolean("system_cc_volume_showpct")
                || MainModule.mPrefs.getBoolean("system_volumebar_blur_mtk")
                || MainModule.mPrefs.getBoolean("system_cc_hidedate")
                || MainModule.mPrefs.getBoolean("system_cc_hide_shortcuticons")
                || MainModule.mPrefs.getBoolean("system_cc_clocktweak")
                || MainModule.mPrefs.getBoolean("system_cc_tile_roundedrect")
                || MainModule.mPrefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1
                || (MainModule.mPrefs.getBoolean("system_separatevolume") && MainModule.mPrefs.getBoolean("system_separatevolume_slider"))
                || (MainModule.mPrefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumedialogdelay_expanded", 0) > 0)
                || (MainModule.mPrefs.getInt("system_volumeblur_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumeblur_expanded", 0) > 0)
            ) {
                SystemUIControlCenterHooks.MIUIVolumeDialogHook(lpparam);
            }
            FeatureDispatcher.installById("batteryIndicator", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_disableanynotif")) SystemNotificationMoreHooks.DisableAnyNotificationHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts")) SystemUILockScreenHooks.LockScreenShortcutHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_4gtolte")
                || (MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single") &&
                    !MainModule.mPrefs.getString("system_statusbar_mobile_showname", "").equals(""))
            ) SystemStatusBarMoreHooks.MobileNetworkTypeHook(lpparam);
            boolean moveRight = MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atright")
                || MainModule.mPrefs.getBoolean("system_statusbar_alarm_atright")
                || MainModule.mPrefs.getBoolean("system_statusbar_sound_atright")
                || MainModule.mPrefs.getBoolean("system_statusbar_dnd_atright")
                || MainModule.mPrefs.getBoolean("system_statusbar_nfc_atright")
                || MainModule.mPrefs.getBoolean("system_statusbar_btbattery_atright")
                || MainModule.mPrefs.getBoolean("system_statusbar_headset_atright")
                || MainModule.mPrefs.getBoolean("system_statusbar_vpn_atright");
            boolean moveLeft = MainModule.mPrefs.getBoolean("system_statusbar_alarm_atleft")
                || MainModule.mPrefs.getBoolean("system_statusbar_sound_atleft")
                || MainModule.mPrefs.getBoolean("system_statusbar_dnd_atleft")
                || MainModule.mPrefs.getBoolean("system_statusbar_gps_atleft");
            if (moveRight || moveLeft
                || MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atleft")
                || (MainModule.mPrefs.getBoolean("system_statusbar_dualrows") && MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atsecondrow"))
                || MainModule.mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_swap_wifi_mobile")
            ) {
                SystemUIStatusBarHooks.StatusBarIconsPositionAdjustHook(lpparam, moveRight, moveLeft);
            }
            if (MainModule.mPrefs.getStringAsInt("system_statusbar_clock_position", 1) > 1 && !MainModule.mPrefs.getBoolean("system_statusbar_dualrows")) {
                SystemUIStatusBarHooks.StatusBarClockPositionHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_statusbar_batterystyle")) {
                SystemUIBatteryHooks.StatusBarStyleBatteryIconHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent")
                || MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature")
            ) SystemUIStatusBarHooks.MonitorDeviceInfoHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_statusbar_topmargin") && MainModule.mPrefs.getBoolean("system_statusbar_topmargin_unset_lockscreen")) SystemUILockScreenHooks.LockScreenTopMarginHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_statusbar_horizmargin")) SystemUIStatusBarHooks.HorizMarginHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_showpct")) SystemUIControlCenterHooks.BrightnessPctHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_hidelsstatusbar")) SystemLockScreenMoreHooks.HideLockScreenStatusBarHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_hidelsclock")) SystemLockScreenMoreHooks.HideLockScreenClockHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_ls_force_systemfonts")) SystemUIStatusBarHooks.ForceClockUseSystemFontsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_hidelshint")) FeatureDispatcher.installById("hideLockScreenHint", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_allowdirectreply")) SystemAudioAndVisualAndMoreHooks.AllowDirectReplyHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_allownotifonkeyguard")) SystemAudioAndVisualAndMoreHooks.AllowAllKeyguardHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_allownotiffloat")) SystemAudioAndVisualAndMoreHooks.AllowAllFloatHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_hideqs")) SystemAudioAndVisualAndMoreHooks.HideQSHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_lsalarm")) SystemAudioAndVisualAndMoreHooks.LockScreenAlarmHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_statusbarcontrols")) SystemUIControlCenterHooks.StatusBarGesturesHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_nonetspeedseparator")) FeatureDispatcher.installById("noNetworkSpeedSeparator", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_statusbaricons_clock")) FeatureDispatcher.installById("hideIconsClock", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || (!MainModule.mPrefs.getBoolean("system_detailednetspeed")
                    && (MainModule.mPrefs.getBoolean("system_detailednetspeed_secunit")
                        || MainModule.mPrefs.getBoolean("system_detailednetspeed_low")
                        )
                    )
            ) {
                SystemUIStatusBarHooks.FormatNetworkSpeedHook(lpparam);
            }
            if (
                MainModule.mPrefs.getInt("system_netspeed_fontsize", 13) > 13
                || MainModule.mPrefs.getInt("system_netspeed_verticaloffset", 8) != 8
                || MainModule.mPrefs.getBoolean("system_detailednetspeed")
                || MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || MainModule.mPrefs.getBoolean("system_netspeed_bold")
                || MainModule.mPrefs.getInt("system_netspeed_leftmargin", 0) > 0
                || MainModule.mPrefs.getInt("system_netspeed_fixedcontent_width", 10) > 10
                || MainModule.mPrefs.getInt("system_netspeed_rightmargin", 0) > 0
                || MainModule.mPrefs.getStringAsInt("system_detailednetspeed_align", 1) > 1
            ) {
                SystemUIStatusBarHooks.NetSpeedStyleHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_taptounlock")) SystemAudioAndVisualAndMoreHooks.TapToUnlockHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_nosos")) SystemSecurityAndSystemHooks.NoSOSHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_morenotif")) SystemNotificationMoreHooks.MoreNotificationsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_charginginfo")) SystemChargingAndWallpaperHooks.ChargingInfoHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_secureqs")) SystemUILockScreenHooks.SecureQSTilesHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_mutevisiblenotif")) FeatureDispatcher.installById("muteVisibleNotifications", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("system_statusbaricons_battery1")) SystemStatusBarMoreHooks.HideIconsBattery1Hook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_statusbaricons_battery3")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_battery4")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_battery2")
            ) SystemStatusBarMoreHooks.HideIconsBattery2Hook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_statusbaricons_wifistandard", 1) > 1) SystemStatusBarMoreHooks.DisplayWifiStandardHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_statusbaricons_signal")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim1")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim2")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim_nodata")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_roaming")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_volte")
            ) SystemUIStatusBarHooks.HideIconsSignalHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_statusbaricons_vowifi")) SystemUIStatusBarHooks.HideIconsVoWiFiHook(lpparam);
            if (!MainModule.mPrefs.getBoolean("system_statusbaricons_alarm") && MainModule.mPrefs.getInt("system_statusbaricons_alarmn", 0) > 0) SystemStatusBarMoreHooks.HideIconsSelectiveAlarmHook(lpparam);
            if (!MainModule.mPrefs.getString("system_shortcut_app", "").equals("")
                || !MainModule.mPrefs.getString("system_calendar_app", "").equals("")
                || !MainModule.mPrefs.getString("system_clock_app", "").equals("")) SystemUINotificationHooks.ReplaceShortcutAppHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_qshaptics", 1) > 1) SystemNotificationAndShareHooks.QSHapticHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_qs_hideoperator")) SystemSettingsAndConnectivityHooks.HideCCOperatorHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_cc_hideoperator_delimiter")) SystemSettingsAndConnectivityHooks.HideCCOperatorDelimiterHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_cc_show_stepcount")
                || MainModule.mPrefs.getBoolean("system_drawer_show_stepcount")
            ) SystemUIControlCenterHooks.ShowCCStepCountHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_cc_disable_bluetooth_restrict")) SystemSettingsAndConnectivityHooks.DisableBluetoothRestrictHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_cc_collapse_after_clicked")) SystemSettingsAndConnectivityHooks.CollapseCCAfterClickHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_cc_switch_qsandnotification")) SystemUIControlCenterHooks.SwitchCCAndNotificationHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_expandnotifs", 1) > 1) SystemStatusBarClockAndMoreHooks.ExpandNotificationsHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_inactivebrightness", 1) > 1) SystemAudioAndVisualAndMoreHooks.InactiveBrightnessSliderHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_mobiletypeicon", 1) > 1
                || MainModule.mPrefs.getBoolean("system_networkindicator_mobile")
                || MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")
            ) {
                SystemUIStatusBarHooks.HideMobileNetworkIndicatorHook(lpparam);
            }
            if (MainModule.mPrefs.getStringAsInt("system_statusbaricons_bluetooth", 1) > 1) SystemStatusBarMoreHooks.HideIconsBluetoothHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_epm")) SystemUINotificationHooks.ExtendedPowerMenuHook(lpparam);

            boolean hideIconsActive =
                MainModule.mPrefs.getBoolean("system_statusbaricons_wifi") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_dualwifi") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_alarm") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_profile") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_sound") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_dnd") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_secondspace") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_headset") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_nfc") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_vpn") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_airplane") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_hotspot") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_nosims") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_gps") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_btbattery") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_ble_unlock") ||
                MainModule.mPrefs.getBoolean("system_statusbaricons_volte");
            if (hideIconsActive) SystemUIStatusBarHooks.HideIconsHook(lpparam);

            if (
                MainModule.mPrefs.getBoolean("system_statusbaricons_privacy")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_mute")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_speaker")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_record")
            ) SystemUIStatusBarHooks.HideIconsFromSystemManager(lpparam);
            if (MainModule.mPrefs.getInt("system_messagingstylelines", 0) > 0) SystemFreeformAndMultiWindowHooks.MessagingStyleLinesHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")) SystemFreeformAndMultiWindowHooks.BetterPopupsAllowFloatHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_betterpopups_autoclose_expanded")) SystemNotificationMoreHooks.AutoDismissExpandedPopupsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_betterpopups_disablewhenmute")) SystemUINotificationHooks.DisableHeadsUpWhenMuteHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_securecontrolcenter")) SystemFreeformAndMultiWindowHooks.SecureControlCenterHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_minimalnotifview")) SystemNotificationMoreHooks.MinimalNotificationViewHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_notifchannelsettings")) SystemNotificationMoreHooks.NotificationChannelSettingsHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_maxsbicons", 0) != 0) SystemNotificationMoreHooks.MaxNotificationIconsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single")) {
                SystemUIStatusBarHooks.MobileTypeSingleHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_statusbar_dualsimin2rows")) {
                SystemUIStatusBarHooks.DualRowSignalHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_statusbar_dualrows")) {
                SystemUIStatusBarHooks.DualRowStatusbarHook(lpparam);
            }
            if (MainModule.mPrefs.getInt("system_ccgridcolumns", 4) > 4 || MainModule.mPrefs.getInt("system_ccgridrows", 4) != 4) SystemUIControlCenterHooks.SystemCCGridHook(lpparam);
            if (MainModule.mPrefs.getStringAsInt("system_colorizenotifs", 1) > 1) SystemNotificationAndShareHooks.ColorizeNotificationCardHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_notify_openinfw")) SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_fw_noblacklist")) SystemFreeformAndMultiWindowHooks.DisableSideBarSuggestionHook(lpparam);

            if (MainModule.mPrefs.getBoolean("system_notify_openinfw")
                || MainModule.mPrefs.getBoolean("system_notifrowmenu")
                || MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")
            ) {
                SystemUINotificationHooks.FixOpenNotifyInFreeFormHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_nosafevolume")) {
                SystemUIControlCenterHooks.HideSafeVolumeDlgHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_lockscreen_hidezenmode")) {
                SystemUILockScreenHooks.HideLockscreenZenModeHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_nopassword")) SystemLockScreenHooks.NoPasswordHook(lpparam);
        }

    }
}
