package tv.withaibuild.customiuizer.installers;

import android.content.Context;
import tv.withaibuild.customiuizer.utils.PrefMap;
import android.provider.Settings;
import android.widget.LinearLayout;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
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
        if (!pkg.equals("com.android.systemui")) return;

        if (MainModule.mPrefs.getInt("system_statusbarheight", 19) > 19) SystemStatusBarAndClockHooks.StatusBarHeightRes();
        if (MainModule.mPrefs.getInt("controls_navbarheight", 19) > 19) Controls.NavbarHeightRes();
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

            if (isWithinSystemUiRestartGuard(restartTime, currentTime)) {
                return;
            }

            FeatureRuntime systemuiRuntime = FeatureDispatcher.createRuntime(pkg, lpparam, lpparam.getClassLoader(), MainModule.mPrefs);

            if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0
                || MainModule.mPrefs.getBoolean("controls_volumecursor")
            ) GlobalActions.setupForegroundMonitor(lpparam);

            FeatureDispatcher.installById("tempHideOverlaySystemUI", systemuiRuntime);

            if (
                MainModule.mPrefs.getBoolean("system_fivegtile")
                || MainModule.mPrefs.getBoolean("system_cc_fpstile")
            ) {
                SystemUIMonitorAndTileHooks.AddCustomTileHook(lpparam);
            }

            FeatureDispatcher.installById("hideStatusBarBeforeScreenshot", systemuiRuntime);

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
            else if (MainModule.mPrefs.getBoolean("controls_hidenavbar_whenscreenshot")) FeatureDispatcher.installById("hideNavBarBeforeScreenshot", systemuiRuntime);
            if (MainModule.mPrefs.getBoolean("controls_imebackalticon")) Controls.ImeBackAltIconHook(lpparam);
            if (MainModule.mPrefs.getBoolean("controls_hide_ime_dismiss_button")) Controls.HideImeDismissButtonHook(lpparam);
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
                || MainModule.mPrefs.getBoolean("system_netspeed_use_clock_style")
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
            if (MainModule.mPrefs.getBoolean("system_charginginfo")) FeatureDispatcher.installById("chargingInfo", systemuiRuntime);
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
                MainModule.mPrefs.getBoolean("system_statusbaricons_wireless_headset") ||
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

// Startup family predicate for SystemUI
    public static boolean hasAnySystemUiStartupFeature(PrefMap<String, Object> prefs) {
        if (hasAnyGlobalAction(prefs)) return true;
        if (prefs.getInt("system_statusbarheight", 19) > 19) return true;
        if (prefs.getInt("controls_navbarheight", 19) > 19) return true;
        if (prefs.getStringAsInt("various_showcallui", 0) > 0 || prefs.getBoolean("controls_volumecursor")) return true;
        if (prefs.getBoolean("system_fivegtile") || prefs.getBoolean("system_cc_fpstile")) return true;
        if (prefs.getInt("system_qsgridcolumns", 2) > 2 || prefs.getInt("system_qsgridrows", 1) > 1) return true;
        if (prefs.getInt("system_qqsgridcolumns", 2) > 2) return true;
        if (prefs.getBoolean("system_networkindicator_wifi")) return true;
        if (prefs.getInt("system_drawer_blur", 100) < 100) return true;
        if (prefs.getInt("system_chargeanimtime", 20) < 20) return true;
        if (prefs.getInt("system_betterpopups_delay", 0) > 0 && !prefs.getBoolean("system_betterpopups_nohide")) return true;
        if (prefs.getInt("system_netspeedinterval", 4) != 4) return true;
        if (prefs.getInt("system_qsgridrows", 1) > 1 || prefs.getBoolean("system_qsnolabels")) return true;
        if (prefs.getInt("system_lstimeout", 3) > 3) return true;
        if (prefs.getInt("controls_fsg_assist_left_action", 1) > 1 || prefs.getInt("controls_fsg_assist_right_action", 1) > 1) return true;
        if (prefs.getInt("controls_navbarleft_action", 1) > 1 || prefs.getInt("controls_navbarleftlong_action", 1) > 1 || prefs.getInt("controls_navbarright_action", 1) > 1 || prefs.getInt("controls_navbarrightlong_action", 1) > 1) return true;
        if (prefs.getBoolean("system_scramblepin")) return true;
        if (prefs.getBoolean("system_dttosleep")) return true;
        if (prefs.getBoolean("system_noscreenlock_act")) return true;
        if (prefs.getBoolean("system_detailednetspeed") && !prefs.getBoolean("system_detailednetspeed_fakedualrow")) return true;
        if (prefs.getBoolean("system_albumartonlock")) return true;
        if (prefs.getStringAsInt("system_expandheadups", 1) > 1) return true;
        if (prefs.getBoolean("system_betterpopups_nohide")) return true;
        if (prefs.getBoolean("system_betterpopups_swipedown")) return true;
        if (prefs.getBoolean("system_betterpopups_center")) return true;
        if (prefs.getBoolean("system_notifafterunlock")) return true;
        if (prefs.getBoolean("system_notifrowmenu")) return true;
        if (prefs.getBoolean("system_compactnotif")) return true;
        if (prefs.getBoolean("system_removedismiss")) return true;
        if (prefs.getBoolean("system_drawer_removeshortcut")) return true;
        if (prefs.getBoolean("controls_nonavbar")) return true;
        if (prefs.getBoolean("controls_hidenavbar_whenscreenshot")) return true;
        if (prefs.getBoolean("controls_imebackalticon")) return true;
        if (prefs.getBoolean("controls_hide_ime_dismiss_button")) return true;
        if (prefs.getBoolean("system_visualizer")) return true;
        if (prefs.getBoolean("system_nosilentvibrate") || prefs.getBoolean("system_qs_force_systemfonts") || prefs.getBoolean("system_volumetimer") || prefs.getBoolean("system_qsnolabels") || prefs.getBoolean("system_cc_volume_showpct") || prefs.getBoolean("system_volumebar_blur_mtk") || prefs.getBoolean("system_cc_hidedate") || prefs.getBoolean("system_cc_hide_shortcuticons") || prefs.getBoolean("system_cc_clocktweak") || prefs.getBoolean("system_cc_tile_roundedrect") || prefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1 || (prefs.getBoolean("system_separatevolume") && prefs.getBoolean("system_separatevolume_slider")) || (prefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || prefs.getInt("system_volumedialogdelay_expanded", 0) > 0) || (prefs.getInt("system_volumeblur_collapsed", 0) > 0 || prefs.getInt("system_volumeblur_expanded", 0) > 0)) return true;
        if (prefs.getBoolean("system_disableanynotif")) return true;
        if (prefs.getBoolean("system_lockscreenshortcuts")) return true;
        if (prefs.getBoolean("system_4gtolte") || (prefs.getBoolean("system_statusbar_mobiletype_single") && !prefs.getString("system_statusbar_mobile_showname", "").equals(""))) return true;
        if ((prefs.getBoolean("system_statusbar_netspeed_atright") || prefs.getBoolean("system_statusbar_alarm_atright") || prefs.getBoolean("system_statusbar_sound_atright") || prefs.getBoolean("system_statusbar_dnd_atright") || prefs.getBoolean("system_statusbar_nfc_atright") || prefs.getBoolean("system_statusbar_btbattery_atright") || prefs.getBoolean("system_statusbar_headset_atright") || prefs.getBoolean("system_statusbar_vpn_atright")) || (prefs.getBoolean("system_statusbar_alarm_atleft") || prefs.getBoolean("system_statusbar_sound_atleft") || prefs.getBoolean("system_statusbar_dnd_atleft") || prefs.getBoolean("system_statusbar_gps_atleft")) || prefs.getBoolean("system_statusbar_netspeed_atleft") || (prefs.getBoolean("system_statusbar_dualrows") && prefs.getBoolean("system_statusbar_netspeed_atsecondrow")) || prefs.getBoolean("system_statusbaricons_wifi_mobile_atleft") || prefs.getBoolean("system_statusbaricons_swap_wifi_mobile")) return true;
        if (prefs.getStringAsInt("system_statusbar_clock_position", 1) > 1 && !prefs.getBoolean("system_statusbar_dualrows")) return true;
        if (prefs.getBoolean("system_statusbar_batterystyle")) return true;
        if (prefs.getBoolean("system_statusbar_batterytempandcurrent") || prefs.getBoolean("system_statusbar_showdevicetemperature")) return true;
        if (prefs.getBoolean("system_statusbar_topmargin") && prefs.getBoolean("system_statusbar_topmargin_unset_lockscreen")) return true;
        if (prefs.getBoolean("system_statusbar_horizmargin")) return true;
        if (prefs.getBoolean("system_showpct")) return true;
        if (prefs.getBoolean("system_hidelsstatusbar")) return true;
        if (prefs.getBoolean("system_hidelsclock")) return true;
        if (prefs.getBoolean("system_ls_force_systemfonts")) return true;
        if (prefs.getBoolean("system_hidelshint")) return true;
        if (prefs.getBoolean("system_allowdirectreply")) return true;
        if (prefs.getBoolean("system_allownotifonkeyguard")) return true;
        if (prefs.getBoolean("system_allownotiffloat")) return true;
        if (prefs.getBoolean("system_hideqs")) return true;
        if (prefs.getBoolean("system_lsalarm")) return true;
        if (prefs.getBoolean("system_statusbarcontrols")) return true;
        if (prefs.getBoolean("system_nonetspeedseparator")) return true;
        if (prefs.getBoolean("system_statusbaricons_clock")) return true;
        if (prefs.getBoolean("system_detailednetspeed_fakedualrow") || (!prefs.getBoolean("system_detailednetspeed") && (prefs.getBoolean("system_detailednetspeed_secunit") || prefs.getBoolean("system_detailednetspeed_low") ) )) return true;
        if (prefs.getInt("system_netspeed_fontsize", 13) > 13 || prefs.getInt("system_netspeed_verticaloffset", 8) != 8 || prefs.getBoolean("system_detailednetspeed") || prefs.getBoolean("system_detailednetspeed_fakedualrow") || prefs.getBoolean("system_netspeed_bold") || prefs.getBoolean("system_netspeed_use_clock_style") || prefs.getInt("system_netspeed_leftmargin", 0) > 0 || prefs.getInt("system_netspeed_fixedcontent_width", 10) > 10 || prefs.getInt("system_netspeed_rightmargin", 0) > 0 || prefs.getStringAsInt("system_detailednetspeed_align", 1) > 1) return true;
        if (prefs.getBoolean("system_taptounlock")) return true;
        if (prefs.getBoolean("system_nosos")) return true;
        if (prefs.getBoolean("system_morenotif")) return true;
        if (prefs.getBoolean("system_charginginfo")) return true;
        if (prefs.getBoolean("system_secureqs")) return true;
        if (prefs.getBoolean("system_mutevisiblenotif")) return true;
        if (prefs.getBoolean("system_statusbaricons_battery1")) return true;
        if (prefs.getBoolean("system_statusbaricons_battery3") || prefs.getBoolean("system_statusbaricons_battery4") || prefs.getBoolean("system_statusbaricons_battery2")) return true;
        if (prefs.getStringAsInt("system_statusbaricons_wifistandard", 1) > 1) return true;
        if (prefs.getBoolean("system_statusbaricons_signal") || prefs.getBoolean("system_statusbaricons_sim1") || prefs.getBoolean("system_statusbaricons_sim2") || prefs.getBoolean("system_statusbaricons_sim_nodata") || prefs.getBoolean("system_statusbaricons_roaming") || prefs.getBoolean("system_statusbaricons_volte")) return true;
        if (prefs.getBoolean("system_statusbaricons_vowifi")) return true;
        if (!prefs.getBoolean("system_statusbaricons_alarm") && prefs.getInt("system_statusbaricons_alarmn", 0) > 0) return true;
        if (!prefs.getString("system_shortcut_app", "").equals("") || !prefs.getString("system_calendar_app", "").equals("") || !prefs.getString("system_clock_app", "").equals("")) return true;
        if (prefs.getStringAsInt("system_qshaptics", 1) > 1) return true;
        if (prefs.getBoolean("system_qs_hideoperator")) return true;
        if (prefs.getBoolean("system_cc_hideoperator_delimiter")) return true;
        if (prefs.getBoolean("system_cc_show_stepcount") || prefs.getBoolean("system_drawer_show_stepcount")) return true;
        if (prefs.getBoolean("system_cc_disable_bluetooth_restrict")) return true;
        if (prefs.getBoolean("system_cc_collapse_after_clicked")) return true;
        if (prefs.getBoolean("system_cc_switch_qsandnotification")) return true;
        if (prefs.getStringAsInt("system_expandnotifs", 1) > 1) return true;
        if (prefs.getStringAsInt("system_inactivebrightness", 1) > 1) return true;
        if (prefs.getStringAsInt("system_mobiletypeicon", 1) > 1 || prefs.getBoolean("system_networkindicator_mobile") || prefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")) return true;
        if (prefs.getStringAsInt("system_statusbaricons_bluetooth", 1) > 1) return true;
        if (prefs.getBoolean("system_epm")) return true;
        if ((prefs.getBoolean("system_statusbaricons_wifi") || prefs.getBoolean("system_statusbaricons_dualwifi") || prefs.getBoolean("system_statusbaricons_alarm") || prefs.getBoolean("system_statusbaricons_profile") || prefs.getBoolean("system_statusbaricons_sound") || prefs.getBoolean("system_statusbaricons_dnd") || prefs.getBoolean("system_statusbaricons_secondspace") || prefs.getBoolean("system_statusbaricons_headset") || prefs.getBoolean("system_statusbaricons_wireless_headset") || prefs.getBoolean("system_statusbaricons_nfc") || prefs.getBoolean("system_statusbaricons_vpn") || prefs.getBoolean("system_statusbaricons_airplane") || prefs.getBoolean("system_statusbaricons_hotspot") || prefs.getBoolean("system_statusbaricons_nosims") || prefs.getBoolean("system_statusbaricons_gps") || prefs.getBoolean("system_statusbaricons_btbattery") || prefs.getBoolean("system_statusbaricons_ble_unlock") || prefs.getBoolean("system_statusbaricons_volte"))) return true;
        if (prefs.getBoolean("system_statusbaricons_privacy") || prefs.getBoolean("system_statusbaricons_mute") || prefs.getBoolean("system_statusbaricons_speaker") || prefs.getBoolean("system_statusbaricons_record")) return true;
        if (prefs.getInt("system_messagingstylelines", 0) > 0) return true;
        if (prefs.getBoolean("system_betterpopups_allowfloat")) return true;
        if (prefs.getBoolean("system_betterpopups_autoclose_expanded")) return true;
        if (prefs.getBoolean("system_betterpopups_disablewhenmute")) return true;
        if (prefs.getBoolean("system_securecontrolcenter")) return true;
        if (prefs.getBoolean("system_minimalnotifview")) return true;
        if (prefs.getBoolean("system_notifchannelsettings")) return true;
        if (prefs.getStringAsInt("system_maxsbicons", 0) != 0) return true;
        if (prefs.getBoolean("system_statusbar_mobiletype_single")) return true;
        if (prefs.getBoolean("system_statusbar_dualsimin2rows")) return true;
        if (prefs.getBoolean("system_statusbar_dualrows")) return true;
        if (prefs.getInt("system_ccgridcolumns", 4) > 4 || prefs.getInt("system_ccgridrows", 4) != 4) return true;
        if (prefs.getStringAsInt("system_colorizenotifs", 1) > 1) return true;
        if (prefs.getBoolean("system_notify_openinfw")) return true;
        if (prefs.getBoolean("system_fw_noblacklist")) return true;
        if (prefs.getBoolean("system_notify_openinfw") || prefs.getBoolean("system_notifrowmenu") || prefs.getBoolean("system_betterpopups_allowfloat")) return true;
        if (prefs.getBoolean("system_nosafevolume")) return true;
        if (prefs.getBoolean("system_lockscreen_hidezenmode")) return true;
        if (prefs.getBoolean("system_nopassword")) return true;
        if (prefs.getBoolean("system_statusbar_topmargin")) return true;
        if (prefs.getBoolean("system_cc_enable_style_switch")) return true;
        if (prefs.getBoolean("system_qs_force_systemfonts")) return true;
        if (prefs.getBoolean("system_detailednetspeed_fakedualrow")) return true;
        if (prefs.getBoolean("system_volumetimer")) return true;
        if (prefs.getBoolean("system_cc_tile_roundedrect")) return true;
        if ((prefs.getInt("system_statusbar_iconsize", 6)) > 6) return true;
        if (prefs.getBoolean("system_cc_show_stepcount")) return true;
        if ((prefs.getBoolean("system_statusbaricons_swap_wifi_mobile")) || (prefs.getBoolean("system_statusbaricons_wifi_mobile_atleft"))) return true;
        if (prefs.getBoolean("system_screenshot_overlay")) return true;
        if (prefs.getBoolean("system_hidestatusbar_whenscreenshot")) return true;
        if (prefs.getBoolean("system_statusbar_clocktweak") ||
                prefs.getBoolean("system_cc_clocktweak") ||
                prefs.getBoolean("system_cc_hidedate") ||
                !prefs.getString("system_cc_dateformat", "").isEmpty()) return true;
        if (prefs.getBoolean("system_hidemoreicon")) return true;
        if (prefs.getBoolean("system_batteryindicator")) return true;
        return false;
    }

    private static boolean hasAnyGlobalAction(PrefMap<String, Object> prefs) {
        for (java.util.Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isSystemUiGlobalActionKey(key) && value instanceof Integer && (Integer) value > 1) {
                return true;
            }
        }
        if (prefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || prefs.getStringAsInt("controls_volumemedia_down", 0) > 0) {
            return !prefs.getStringSet("controls_mediaplayer_apps").isEmpty();
        }
        return false;
    }

    /**
     * Returns true only for action keys that belong to the SystemUI / GlobalActions
     * domain. Launcher actions, app-internal actions and unknown action keys are
     * rejected by default (positive-domain gating).
     */
    static boolean isSystemUiGlobalActionKey(String key) {
        if (key == null || !key.endsWith("_action")) return false;
        String base = key;
        if (base.startsWith("pref_key_")) {
            base = base.substring("pref_key_".length());
        }
        return base.startsWith("controls_") || base.startsWith("system_");
    }

    /**
     * Pure predicate for the restart-time guard. Returns true when the elapsed
     * time since the last recorded SystemUI restart is shorter than the guard
     * window, indicating that the heavy runtime setup should be skipped this pass.
     */
    static boolean isWithinSystemUiRestartGuard(long restartTime, long currentTime) {
        return currentTime - restartTime < 10000;
    }
}
