package tv.withaibuild.customiuizer.mods

import android.app.ActivityOptions
import android.content.Context
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

/** Facade for legacy `System.java` static entry points.
 *  Each method delegates to the corresponding `System*Hooks` object.
 *  Do not add implementation logic here.
 */
@Suppress("UNUSED_PARAMETER")
object System {

    @JvmStatic
    fun ScreenAnimHook(lpparam: SystemServerStartingParam) {
        SystemDisplayAndWindowHooks.ScreenAnimHook(lpparam)
    }

    @JvmStatic
    fun NoAccessDeviceLogsRequest(lpparam: SystemServerStartingParam) {
        SystemDisplayAndWindowHooks.NoAccessDeviceLogsRequest(lpparam)
    }

    @JvmStatic
    fun NoLightUpOnChargeHook(lpparam: SystemServerStartingParam) {
        SystemDisplayAndWindowHooks.NoLightUpOnChargeHook(lpparam)
    }

    @JvmStatic
    fun ScramblePINHook(lpparam: PackageReadyParam) {
        SystemLockScreenHooks.ScramblePINHook(lpparam)
    }

    @JvmStatic
    fun NoPasswordHook(lpparam: PackageReadyParam) {
        SystemLockScreenHooks.NoPasswordHook(lpparam)
    }

    @JvmStatic
    fun EnhancedSecurityHook(lpparam: SystemServerStartingParam) {
        SystemLockScreenHooks.EnhancedSecurityHook(lpparam)
    }

    @JvmStatic
    fun NoScreenLockHook(lpparam: PackageReadyParam) {
        SystemLockScreenMoreHooks.NoScreenLockHook(lpparam)
    }

    @JvmStatic
    fun DoubleTapToSleepHook(lpparam: PackageReadyParam) {
        SystemDisplayAndWindowHooks.DoubleTapToSleepHook(lpparam)
    }

    @JvmStatic
    fun NotificationVolumeSettingsRes() {
        SystemAudioAndVolumeHooks.NotificationVolumeSettingsRes()
    }

    @JvmStatic
    fun NotificationVolumeServiceHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVolumeHooks.NotificationVolumeServiceHook(lpparam)
    }

    @JvmStatic
    fun NotificationVolumeSettingsHook(lpparam: PackageReadyParam) {
        SystemAudioAndVolumeHooks.NotificationVolumeSettingsHook(lpparam)
    }

    @JvmStatic
    fun ViewWifiPasswordHook(lpparam: PackageReadyParam) {
        SystemSettingsAndConnectivityHooks.ViewWifiPasswordHook(lpparam)
    }

    @JvmStatic
    fun StatusBarClockTweakHook(lpparam: PackageReadyParam) {
        SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook(lpparam)
    }

    @JvmStatic
    fun ExpandNotificationsHook(lpparam: PackageReadyParam) {
        SystemStatusBarClockAndMoreHooks.ExpandNotificationsHook(lpparam)
    }

    @JvmStatic
    fun ExpandHeadsUpHook(lpparam: PackageReadyParam) {
        SystemStatusBarClockAndMoreHooks.ExpandHeadsUpHook(lpparam)
    }

    @JvmStatic
    fun DrawerBlurRatioHook(lpparam: PackageReadyParam) {
        SystemDisplayAndWindowHooks.DrawerBlurRatioHook(lpparam)
    }

    @JvmStatic
    fun ChargeAnimationHook(lpparam: PackageReadyParam) {
        SystemDisplayAndWindowHooks.ChargeAnimationHook(lpparam)
    }

    @JvmStatic
    fun VolumeStepsHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVolumeHooks.VolumeStepsHook(lpparam)
    }

    @JvmStatic
    fun AutoBrightnessRangeHook(lpparam: SystemServerStartingParam) {
        SystemDisplayAndWindowHooks.AutoBrightnessRangeHook(lpparam)
    }

    @JvmStatic
    fun BetterPopupsHideDelayHook(lpparam: PackageReadyParam) {
        SystemNotificationPopupsHooks.BetterPopupsHideDelayHook(lpparam)
    }

    @JvmStatic
    fun BetterPopupsNoHideHook(lpparam: PackageReadyParam) {
        SystemNotificationPopupsHooks.BetterPopupsNoHideHook(lpparam)
    }

    @JvmStatic
    fun BetterPopupsSwipeDownHook(lpparam: PackageReadyParam) {
        SystemNotificationPopupsHooks.BetterPopupsSwipeDownHook(lpparam)
    }

    @JvmStatic
    fun RotationAnimationRes() {
        SystemDisplayAndWindowHooks.RotationAnimationRes()
    }

    @JvmStatic
    fun RotationAnimationHook(lpparam: SystemServerStartingParam) {
        SystemDisplayAndWindowHooks.RotationAnimationHook(lpparam)
    }

    @JvmStatic
    fun NoVersionCheckHook(lpparam: SystemServerStartingParam) {
        SystemSecurityAndSystemHooks.NoVersionCheckHook(lpparam)
    }

    @JvmStatic
    fun ColorizeNotificationCardHook(lpparam: PackageReadyParam) {
        SystemNotificationAndShareHooks.ColorizeNotificationCardHook(lpparam)
    }

    @JvmStatic
    fun CompactNotificationsHook(lpparam: PackageReadyParam) {
        SystemNotificationAndShareHooks.CompactNotificationsHook(lpparam)
    }

    @JvmStatic
    fun QSHapticHook(lpparam: PackageReadyParam) {
        SystemNotificationAndShareHooks.QSHapticHook(lpparam)
    }

    @JvmStatic
    fun HideCCOperatorHook(lpparam: PackageReadyParam) {
        SystemSettingsAndConnectivityHooks.HideCCOperatorHook(lpparam)
    }

    @JvmStatic
    fun HideCCOperatorDelimiterHook(lpparam: PackageReadyParam) {
        SystemSettingsAndConnectivityHooks.HideCCOperatorDelimiterHook(lpparam)
    }

    @JvmStatic
    fun CollapseCCAfterClickHook(lpparam: PackageReadyParam) {
        SystemSettingsAndConnectivityHooks.CollapseCCAfterClickHook(lpparam)
    }

    @JvmStatic
    fun DisableBluetoothRestrictHook(lpparam: PackageReadyParam) {
        SystemSettingsAndConnectivityHooks.DisableBluetoothRestrictHook(lpparam)
    }

    @JvmStatic
    fun AutoGroupNotificationsHook(lpparam: SystemServerStartingParam) {
        SystemNotificationAndShareHooks.AutoGroupNotificationsHook(lpparam)
    }

    @JvmStatic
    fun NoMoreIconHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.NoMoreIconHook(lpparam)
    }

    @JvmStatic
    fun ShowNotificationsAfterUnlockHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.ShowNotificationsAfterUnlockHook(lpparam)
    }

    @JvmStatic
    fun NotificationRowMenuHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.NotificationRowMenuHook(lpparam)
    }

    @JvmStatic
    fun SelectiveVibrationHook(lpparam: SystemServerStartingParam) {
        SystemNotificationMoreHooks.SelectiveVibrationHook(lpparam)
    }

    @JvmStatic
    fun NoDuckingHook(lpparam: SystemServerStartingParam) {
        SystemNotificationMoreHooks.NoDuckingHook(lpparam)
    }

    @JvmStatic
    fun OrientationLockHook(lpparam: SystemServerStartingParam) {
        SystemNotificationMoreHooks.OrientationLockHook(lpparam)
    }

    @JvmStatic
    fun StatusBarHeightRes() {
        SystemStatusBarAndClockHooks.StatusBarHeightRes()
    }

    @JvmStatic
    fun HideMemoryCleanHook(lpparam: PackageReadyParam, isInLauncher: Boolean) {
        SystemStatusBarAndClockHooks.HideMemoryCleanHook(lpparam, isInLauncher)
    }

    @JvmStatic
    fun StatusBarBackgroundHook(lpparam: PackageReadyParam) {
        SystemStatusBarAndClockHooks.StatusBarBackgroundHook(lpparam)
    }

    @JvmStatic
    fun StatusBarBackgroundCompatHook(lpparam: PackageReadyParam) {
        SystemStatusBarAndClockHooks.StatusBarBackgroundCompatHook(lpparam)
    }

    @JvmStatic
    fun SelectiveToastsHook(lpparam: SystemServerStartingParam) {
        SystemStatusBarAndClockHooks.SelectiveToastsHook(lpparam)
    }

    @JvmStatic
    fun CleanShareMenuHook(lpparam: PackageReadyParam) {
        SystemShareAndOpenWithHooks.CleanShareMenuHook(lpparam)
    }

    @JvmStatic
    fun CleanShareMenuServiceHook(lpparam: SystemServerStartingParam) {
        SystemShareAndOpenWithHooks.CleanShareMenuServiceHook(lpparam)
    }

    @JvmStatic
    fun CleanOpenWithMenuHook(lpparam: PackageReadyParam) {
        SystemShareAndOpenWithHooks.CleanOpenWithMenuHook(lpparam)
    }

    @JvmStatic
    fun CleanOpenWithMenuServiceHook(lpparam: SystemServerStartingParam) {
        SystemShareAndOpenWithHooks.CleanOpenWithMenuServiceHook(lpparam)
    }

    @JvmStatic
    fun AppLockHook(lpparam: SystemServerStartingParam) {
        SystemLockScreenMoreHooks.AppLockHook(lpparam)
    }

    @JvmStatic
    fun AppLockTimeoutHook(lpparam: SystemServerStartingParam) {
        SystemLockScreenMoreHooks.AppLockTimeoutHook(lpparam)
    }

    @JvmStatic
    fun AudioVisualizerHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.AudioVisualizerHook(lpparam)
    }

    @JvmStatic
    fun NoCallInterruptionHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.NoCallInterruptionHook(lpparam)
    }

    @JvmStatic
    fun AllRotationsHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.AllRotationsHook(lpparam)
    }

    @JvmStatic
    fun USBConfigHook(lpparam: SystemServerStartingParam) {
        SystemSettingsMoreHooks.USBConfigHook(lpparam)
    }

    @JvmStatic
    fun USBConfigSettingsHook(lpparam: PackageReadyParam) {
        SystemSettingsMoreHooks.USBConfigSettingsHook(lpparam)
    }

    @JvmStatic
    fun HideIconsBattery1Hook(lpparam: PackageReadyParam) {
        SystemStatusBarMoreHooks.HideIconsBattery1Hook(lpparam)
    }

    @JvmStatic
    fun HideIconsBattery2Hook(lpparam: PackageReadyParam) {
        SystemStatusBarMoreHooks.HideIconsBattery2Hook(lpparam)
    }

    @JvmStatic
    fun HideIconsSelectiveAlarmHook(lpparam: PackageReadyParam) {
        SystemStatusBarMoreHooks.HideIconsSelectiveAlarmHook(lpparam)
    }

    @JvmStatic
    fun HideIconsBluetoothHook(lpparam: PackageReadyParam) {
        SystemStatusBarMoreHooks.HideIconsBluetoothHook(lpparam)
    }

    @JvmStatic
    fun DisplayWifiStandardHook(lpparam: PackageReadyParam) {
        SystemStatusBarMoreHooks.DisplayWifiStandardHook(lpparam)
    }

    @JvmStatic
    fun ForceCloseHook(lpparam: SystemServerStartingParam) {
        SystemSecurityAndSystemHooks.ForceCloseHook(lpparam)
    }

    @JvmStatic
    fun DisableAnyNotificationBlockHook(lpparam: SystemServerStartingParam) {
        SystemNotificationMoreHooks.DisableAnyNotificationBlockHook(lpparam)
    }

    @JvmStatic
    fun DisableAnyNotificationBlockHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.DisableAnyNotificationBlockHook(lpparam)
    }

    @JvmStatic
    fun DisableAnyNotificationHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.DisableAnyNotificationHook(lpparam)
    }

    @JvmStatic
    fun NotificationImportanceHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.NotificationImportanceHook(lpparam)
    }

    @JvmStatic
    fun MobileNetworkTypeHook(lpparam: PackageReadyParam) {
        SystemStatusBarMoreHooks.MobileNetworkTypeHook(lpparam)
    }

    @JvmStatic
    fun HideProximityWarningHook(lpparam: SystemServerStartingParam) {
        SystemDisplayAndWindowHooks.HideProximityWarningHook(lpparam)
    }

    @JvmStatic
    fun HideLockScreenClockHook(lpparam: PackageReadyParam) {
        SystemLockScreenMoreHooks.HideLockScreenClockHook(lpparam)
    }

    @JvmStatic
    fun FirstVolumePressHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook(lpparam)
    }

    @JvmStatic
    fun DisableSystemIntegrityHook(lpparam: SystemServerStartingParam) {
        SystemSecurityAndSystemHooks.DisableSystemIntegrityHook(lpparam)
    }

    @JvmStatic
    fun NoSignatureVerifyServiceHook(lpparam: SystemServerStartingParam) {
        SystemSecurityAndSystemHooks.NoSignatureVerifyServiceHook(lpparam)
    }

    @JvmStatic
    fun ScreenDimTimeHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook(lpparam)
    }

    @JvmStatic
    fun NoOverscrollAppHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.NoOverscrollAppHook(lpparam)
    }

    @JvmStatic
    fun RemoveSecureHook(lpparam: SystemServerStartingParam) {
        SystemSecurityAndSystemHooks.RemoveSecureHook(lpparam)
    }

    @JvmStatic
    fun RemoveActStartConfirmHook(lpparam: SystemServerStartingParam) {
        SystemSecurityAndSystemHooks.RemoveActStartConfirmHook(lpparam)
    }

    @JvmStatic
    fun AllowAllKeyguardHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.AllowAllKeyguardHook(lpparam)
    }

    @JvmStatic
    fun AllowAllFloatHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.AllowAllFloatHook(lpparam)
    }

    @JvmStatic
    fun AllowDirectReplyHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.AllowDirectReplyHook(lpparam)
    }

    @JvmStatic
    fun HideQSHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.HideQSHook(lpparam)
    }

    @JvmStatic
    fun LockScreenTimeoutHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.LockScreenTimeoutHook(lpparam)
    }

    @JvmStatic
    fun MuffledVibrationHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.MuffledVibrationHook(lpparam)
    }

    @JvmStatic
    fun LockScreenAlarmHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.LockScreenAlarmHook(lpparam)
    }

    @JvmStatic
    fun ScreenshotConfigHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.ScreenshotConfigHook(lpparam)
    }

    @JvmStatic
    fun ToastTimeHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.ToastTimeHook(lpparam)
    }

    @JvmStatic
    fun ClearAllTasksHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook(lpparam)
    }

    @JvmStatic
    fun InactiveBrightnessSliderHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.InactiveBrightnessSliderHook(lpparam)
    }

    @JvmStatic
    fun TapToUnlockHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.TapToUnlockHook(lpparam)
    }

    @JvmStatic
    fun TempHideOverlayAppHook(lpparam: SystemServerStartingParam) {
        SystemAudioAndVisualAndMoreHooks.TempHideOverlayAppHook(lpparam)
    }

    @JvmStatic
    fun GalleryScreenshotPathHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(lpparam)
    }

    @JvmStatic
    fun ScreenshotFloatTimeHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.ScreenshotFloatTimeHook(lpparam)
    }

    @JvmStatic
    fun ScrambleAppLockPINHook(lpparam: PackageReadyParam) {
        SystemAudioAndVisualAndMoreHooks.ScrambleAppLockPINHook(lpparam)
    }

    @JvmStatic
    fun ChargingInfoHook(lpparam: PackageReadyParam) {
        SystemChargingAndWallpaperHooks.ChargingInfoHook(lpparam)
    }

    @JvmStatic
    fun NoSOSHook(lpparam: PackageReadyParam) {
        SystemSecurityAndSystemHooks.NoSOSHook(lpparam)
    }

    @JvmStatic
    fun NoDarkForceHook(lpparam: SystemServerStartingParam) {
        SystemSecurityAndSystemHooks.NoDarkForceHook(lpparam)
    }

    @JvmStatic
    fun MaxNotificationIconsHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.MaxNotificationIconsHook(lpparam)
    }

    @JvmStatic
    fun MoreNotificationsHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.MoreNotificationsHook(lpparam)
    }

    @JvmStatic
    fun AutoDismissExpandedPopupsHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.AutoDismissExpandedPopupsHook(lpparam)
    }

    @JvmStatic
    fun BetterPopupsAllowFloatHook(lpparam: PackageReadyParam) {
        SystemFreeformAndMultiWindowHooks.BetterPopupsAllowFloatHook(lpparam)
    }

    @JvmStatic
    fun DisableSideBarSuggestionHook(lpparam: PackageReadyParam) {
        SystemFreeformAndMultiWindowHooks.DisableSideBarSuggestionHook(lpparam)
    }

    @JvmStatic
    fun NoFloatingWindowBlacklistHook(lpparam: SystemServerStartingParam) {
        SystemFreeformAndMultiWindowHooks.NoFloatingWindowBlacklistHook(lpparam)
    }

    @JvmStatic
    fun getTaskPackageName(thisObject: Any, taskId: Int): String? {
        return SystemFreeformAndMultiWindowHooks.getTaskPackageName(thisObject, taskId)
    }

    @JvmStatic
    fun getTaskPackageName(thisObject: Any, taskId: Int, options: ActivityOptions?): String? {
        return SystemFreeformAndMultiWindowHooks.getTaskPackageName(thisObject, taskId, options)
    }

    @JvmStatic
    fun getTaskPackageName(thisObject: Any, taskId: Int, withOptions: Boolean, options: ActivityOptions?): String? {
        return SystemFreeformAndMultiWindowHooks.getTaskPackageName(thisObject, taskId, withOptions, options)
    }

    @JvmStatic
    fun serializeFwApps(): String {
        return SystemFreeformAndMultiWindowHooks.serializeFwApps()
    }

    @JvmStatic
    fun unserializeFwApps(data: String?) {
        SystemFreeformAndMultiWindowHooks.unserializeFwApps(data)
    }

    @JvmStatic
    fun storeFwAppsInSetting(context: Context) {
        SystemFreeformAndMultiWindowHooks.storeFwAppsInSetting(context)
    }

    @JvmStatic
    fun restoreFwAppsInSetting(context: Context) {
        SystemFreeformAndMultiWindowHooks.restoreFwAppsInSetting(context)
    }

    @JvmStatic
    fun OpenAppInFreeFormHook(lpparam: SystemServerStartingParam) {
        SystemFreeformAndMultiWindowHooks.OpenAppInFreeFormHook(lpparam)
    }

    @JvmStatic
    fun StickyFloatingWindowsHook(lpparam: SystemServerStartingParam) {
        SystemFreeformAndMultiWindowHooks.StickyFloatingWindowsHook(lpparam)
    }

    @JvmStatic
    fun MessagingStyleLinesHook(lpparam: PackageReadyParam) {
        SystemFreeformAndMultiWindowHooks.MessagingStyleLinesHook(lpparam)
    }

    @JvmStatic
    fun MultiWindowPlusHook(lpparam: SystemServerStartingParam) {
        SystemFreeformAndMultiWindowHooks.MultiWindowPlusHook(lpparam)
    }

    @JvmStatic
    fun MultiWindowPlusHook(lpparam: PackageReadyParam) {
        SystemFreeformAndMultiWindowHooks.MultiWindowPlusHook(lpparam)
    }

    @JvmStatic
    fun SecureControlCenterHook(lpparam: PackageReadyParam) {
        SystemFreeformAndMultiWindowHooks.SecureControlCenterHook(lpparam)
    }

    @JvmStatic
    fun MinimalNotificationViewHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.MinimalNotificationViewHook(lpparam)
    }

    @JvmStatic
    fun NotificationChannelSettingsHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.NotificationChannelSettingsHook(lpparam)
    }

    @JvmStatic
    fun SkipAppLockHook(lpparam: SystemServerStartingParam) {
        SystemLockScreenMoreHooks.SkipAppLockHook(lpparam)
    }

    @JvmStatic
    fun HideLockScreenHintHook(lpparam: PackageReadyParam) {
        SystemLockScreenMoreHooks.HideLockScreenHintHook(lpparam)
    }

    @JvmStatic
    fun HideLockScreenStatusBarHook(lpparam: PackageReadyParam) {
        SystemLockScreenMoreHooks.HideLockScreenStatusBarHook(lpparam)
    }

    @JvmStatic
    fun MuteVisibleNotificationsHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.MuteVisibleNotificationsHook(lpparam)
    }

    @JvmStatic
    fun NetworkIndicatorWifi(lpparam: PackageReadyParam) {
        SystemStatusBarMoreHooks.NetworkIndicatorWifi(lpparam)
    }

    @JvmStatic
    fun SetLockscreenWallpaperHook(lpparam: SystemServerStartingParam) {
        SystemChargingAndWallpaperHooks.SetLockscreenWallpaperHook(lpparam)
    }

    @JvmStatic
    fun BetterPopupsCenteredHook(lpparam: PackageReadyParam) {
        SystemNotificationMoreHooks.BetterPopupsCenteredHook(lpparam)
    }

    @JvmStatic
    fun WallpaperScaleLevelHook(lpparam: SystemServerStartingParam) {
        SystemNotificationMoreHooks.WallpaperScaleLevelHook(lpparam)
    }

    @JvmStatic
    fun Disable72hStrongAuthHook(lpparam: SystemServerStartingParam) {
        SystemNotificationMoreHooks.Disable72hStrongAuthHook(lpparam)
    }

}