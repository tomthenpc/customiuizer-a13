@file:JvmName("SystemUI")

package tv.withaibuild.customiuizer.mods

import android.content.Context
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

object SystemUI {

    @JvmField
    var newStyle: Boolean = false

    @JvmStatic
    fun setupStatusBar(mContext: Context) = SystemUIStatusBarHooks.setupStatusBar(mContext)

    @JvmStatic
    fun MonitorDeviceInfoHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.MonitorDeviceInfoHook(lpparam)

    @JvmStatic
    fun AddCustomTileHook(lpparam: PackageReadyParam) = SystemUIMonitorAndTileHooks.AddCustomTileHook(lpparam)

    @JvmStatic
    fun DualRowStatusbarHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.DualRowStatusbarHook(lpparam)

    @JvmStatic
    fun DualRowSignalHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.DualRowSignalHook(lpparam)

    @JvmStatic
    fun StatusBarIconsPositionAdjustHook(lpparam: PackageReadyParam, moveRight: Boolean, moveLeft: Boolean) =
        SystemUIStatusBarHooks.StatusBarIconsPositionAdjustHook(lpparam, moveRight, moveLeft)

    @JvmStatic
    fun StatusBarClockPositionHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.StatusBarClockPositionHook(lpparam)

    @JvmStatic
    fun NoNetworkSpeedSeparatorHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook(lpparam)

    @JvmStatic
    fun FormatNetworkSpeedHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.FormatNetworkSpeedHook(lpparam)

    @JvmStatic
    fun NetSpeedStyleHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.NetSpeedStyleHook(lpparam)

    @JvmStatic
    fun MobileTypeSingleHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.MobileTypeSingleHook(lpparam)

    @JvmStatic
    fun VolumeDialogAutohideDelayHook(classLoader: ClassLoader) = SystemUIControlCenterHooks.VolumeDialogAutohideDelayHook(classLoader)

    @JvmStatic
    fun BlurVolumeDialogBackgroundHook(classLoader: ClassLoader) = SystemUIControlCenterHooks.BlurVolumeDialogBackgroundHook(classLoader)

    @JvmStatic
    fun BlurMTKVolumeBarHook(classLoader: ClassLoader) = SystemUIControlCenterHooks.BlurMTKVolumeBarHook(classLoader)

    @JvmStatic
    fun SingleNotificationSliderHook(classLoader: ClassLoader) = SystemUIControlCenterHooks.SingleNotificationSliderHook(classLoader)

    @JvmStatic
    fun MIUIVolumeDialogHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.MIUIVolumeDialogHook(lpparam)

    @JvmStatic
    fun SystemCCGridHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.SystemCCGridHook(lpparam)

    @JvmStatic
    fun QQSGridRes() = SystemUIControlCenterHooks.QQSGridRes()

    @JvmStatic
    fun QSGridRes() = SystemUIControlCenterHooks.QSGridRes()

    @JvmStatic
    fun QSGridLabelsHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.QSGridLabelsHook(lpparam)

    @JvmStatic
    fun VolumeTimerValuesRes(pluginLoader: ClassLoader) = SystemUIControlCenterHooks.VolumeTimerValuesRes(pluginLoader)

    @JvmStatic
    fun CCTileCornerHook(pluginLoader: ClassLoader) = SystemUIControlCenterHooks.CCTileCornerHook(pluginLoader)

    @JvmStatic
    fun StatusBarGesturesHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.StatusBarGesturesHook(lpparam)

    @JvmStatic
    fun HorizMarginHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.HorizMarginHook(lpparam)

    @JvmStatic
    fun LockScreenTopMarginHook(lpparam: PackageReadyParam) = SystemUILockScreenHooks.LockScreenTopMarginHook(lpparam)

    @JvmStatic
    fun HideIconsClockHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.HideIconsClockHook(lpparam)

    @JvmStatic
    fun HideIconsVoWiFiHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.HideIconsVoWiFiHook(lpparam)

    @JvmStatic
    fun HideIconsSignalHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.HideIconsSignalHook(lpparam)

    @JvmStatic
    fun HideIconsHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.HideIconsHook(lpparam)

    @JvmStatic
    fun HideIconsFromSystemManager(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.HideIconsFromSystemManager(lpparam)

    @JvmStatic
    fun BatteryIndicatorHook(lpparam: PackageReadyParam) = SystemUIBatteryHooks.BatteryIndicatorHook(lpparam)

    @JvmStatic
    fun TempHideOverlaySystemUIHook(lpparam: PackageReadyParam) = SystemUIScreenshotHooks.TempHideOverlaySystemUIHook(lpparam)

    @JvmStatic
    fun NetSpeedIntervalHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.NetSpeedIntervalHook(lpparam)

    @JvmStatic
    fun DetailedNetSpeedHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.DetailedNetSpeedHook(lpparam)

    @JvmStatic
    fun LockScreenAlbumArtHook(lpparam: PackageReadyParam) = SystemUILockScreenHooks.LockScreenAlbumArtHook(lpparam)

    @JvmStatic
    fun LockScreenShortcutHook(lpparam: PackageReadyParam) = SystemUILockScreenHooks.LockScreenShortcutHook(lpparam)

    @JvmStatic
    fun LockScreenSecureLaunchHook() = SystemUILockScreenHooks.LockScreenSecureLaunchHook()

    @JvmStatic
    fun SecureQSTilesHook(lpparam: PackageReadyParam) = SystemUILockScreenHooks.SecureQSTilesHook(lpparam)

    @JvmStatic
    fun ExtendedPowerMenuHook(lpparam: PackageReadyParam) = SystemUINotificationHooks.ExtendedPowerMenuHook(lpparam)

    @JvmStatic
    fun HideDismissViewHook(lpparam: PackageReadyParam) = SystemUINotificationHooks.HideDismissViewHook(lpparam)

    @JvmStatic
    fun HideNoficationAccessIconHook(lpparam: PackageReadyParam) = SystemUINotificationHooks.HideNoficationAccessIconHook(lpparam)

    @JvmStatic
    fun ReplaceShortcutAppHook(lpparam: PackageReadyParam) = SystemUINotificationHooks.ReplaceShortcutAppHook(lpparam)

    @JvmStatic
    fun StatusBarStyleBatteryIconHook(lpparam: PackageReadyParam) = SystemUIBatteryHooks.StatusBarStyleBatteryIconHook(lpparam)

    @JvmStatic
    fun ForceClockUseSystemFontsHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.ForceClockUseSystemFontsHook(lpparam)

    @JvmStatic
    fun HideStatusBarBeforeScreenshotHook(lpparam: PackageReadyParam) = SystemUIScreenshotHooks.HideStatusBarBeforeScreenshotHook(lpparam)

    @JvmStatic
    fun HideNavBarBeforeScreenshotHook(lpparam: PackageReadyParam) = SystemUIScreenshotHooks.HideNavBarBeforeScreenshotHook(lpparam)

    @JvmStatic
    fun OpenNotifyInFloatingWindowHook(lpparam: PackageReadyParam) = SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam)

    @JvmStatic
    fun FixOpenNotifyInFreeFormHook(lpparam: PackageReadyParam) = SystemUINotificationHooks.FixOpenNotifyInFreeFormHook(lpparam)

    @JvmStatic
    fun BrightnessPctHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.BrightnessPctHook(lpparam)

    @JvmStatic
    fun ShowVolumePctHook(pluginLoader: ClassLoader) = SystemUIControlCenterHooks.ShowVolumePctHook(pluginLoader)

    @JvmStatic
    fun HideCCDateView(pluginLoader: ClassLoader) = SystemUIControlCenterHooks.HideCCDateView(pluginLoader)

    @JvmStatic
    fun hideCCSettingsTilesEdit(pluginLoader: ClassLoader) = SystemUIControlCenterHooks.hideCCSettingsTilesEdit(pluginLoader)

    @JvmStatic
    fun initCCClockStyle(pluginLoader: ClassLoader) = SystemUIControlCenterHooks.initCCClockStyle(pluginLoader)

    @JvmStatic
    fun HideSafeVolumeDlgHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.HideSafeVolumeDlgHook(lpparam)

    @JvmStatic
    fun DisableHeadsUpWhenMuteHook(lpparam: PackageReadyParam) = SystemUINotificationHooks.DisableHeadsUpWhenMuteHook(lpparam)

    @JvmStatic
    fun HideLockscreenZenModeHook(lpparam: PackageReadyParam) = SystemUILockScreenHooks.HideLockscreenZenModeHook(lpparam)

    @JvmStatic
    fun SwitchCCAndNotificationHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.SwitchCCAndNotificationHook(lpparam)

    @JvmStatic
    fun ShowCCStepCountHook(lpparam: PackageReadyParam) = SystemUIControlCenterHooks.ShowCCStepCountHook(lpparam)

    @JvmStatic
    fun BluetoothTileStyleHook(pluginLoader: ClassLoader) = SystemUIControlCenterHooks.BluetoothTileStyleHook(pluginLoader)

    @JvmStatic
    fun HideMobileNetworkIndicatorHook(lpparam: PackageReadyParam) = SystemUIStatusBarHooks.HideMobileNetworkIndicatorHook(lpparam)
}
