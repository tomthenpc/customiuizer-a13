package name.monwf.customiuizer.mods

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * Launcher hooks facade. All implementation is split into functional objects;
 * this object keeps the public static entry points so MainModule.java is unchanged.
 */
object Launcher {

    @JvmStatic
    fun HomescreenSwipesHook(lpparam: PackageReadyParam) = LauncherGestureHooks.HomescreenSwipesHook(lpparam)

    @JvmStatic
    fun HotSeatSwipesHook(lpparam: PackageReadyParam) = LauncherGestureHooks.HotSeatSwipesHook(lpparam)

    @JvmStatic
    fun ShakeHook(lpparam: PackageReadyParam) = LauncherGestureHooks.ShakeHook(lpparam)

    @JvmStatic
    fun FSGesturesHook(lpparam: PackageReadyParam) = LauncherGestureHooks.FSGesturesHook(lpparam)

    @JvmStatic
    fun LauncherDoubleTapHook(lpparam: PackageReadyParam) = LauncherGestureHooks.LauncherDoubleTapHook(lpparam)

    @JvmStatic
    fun LauncherPinchHook(lpparam: PackageReadyParam) = LauncherGestureHooks.LauncherPinchHook(lpparam)

    @JvmStatic
    fun AssistGestureActionHook(lpparam: PackageReadyParam) = LauncherGestureHooks.AssistGestureActionHook(lpparam)

    @JvmStatic
    fun SwipeAndStopActionHook(lpparam: PackageReadyParam) = LauncherGestureHooks.SwipeAndStopActionHook(lpparam)

    @JvmStatic
    fun CloseFolderOnLaunchHook(lpparam: PackageReadyParam) = LauncherFolderHooks.CloseFolderOnLaunchHook(lpparam)

    @JvmStatic
    fun FolderColumnsHook(lpparam: PackageReadyParam) = LauncherFolderHooks.FolderColumnsHook(lpparam)

    @JvmStatic
    fun PrivacyFolderHook(lpparam: PackageReadyParam) = LauncherFolderHooks.PrivacyFolderHook(lpparam)

    @JvmStatic
    fun FolderBlurHook(lpparam: PackageReadyParam) = LauncherFolderHooks.FolderBlurHook(lpparam)

    @JvmStatic
    fun CloseFolderOrDrawerOnLaunchShortcutMenuHook(lpparam: PackageReadyParam) = LauncherFolderHooks.CloseFolderOrDrawerOnLaunchShortcutMenuHook(lpparam)

    @JvmStatic
    fun RenameShortcutsHook(lpparam: PackageReadyParam) = LauncherIconHooks.RenameShortcutsHook(lpparam)

    @JvmStatic
    fun TitleShadowHook(lpparam: PackageReadyParam) = LauncherIconHooks.TitleShadowHook(lpparam)

    @JvmStatic
    fun IconScaleHook(lpparam: PackageReadyParam) = LauncherIconHooks.IconScaleHook(lpparam)

    @JvmStatic
    fun TitleFontSizeHook(lpparam: PackageReadyParam) = LauncherIconHooks.TitleFontSizeHook(lpparam)

    @JvmStatic
    fun TitleTopMarginHook(lpparam: PackageReadyParam) = LauncherIconHooks.TitleTopMarginHook(lpparam)

    @JvmStatic
    fun HideTitlesHook(lpparam: PackageReadyParam) = LauncherIconHooks.HideTitlesHook(lpparam)

    @JvmStatic
    fun HideNavBarHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.HideNavBarHook(lpparam)

    @JvmStatic
    fun HideSeekPointsHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.HideSeekPointsHook(lpparam)

    @JvmStatic
    fun InfiniteScrollHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.InfiniteScrollHook(lpparam)

    @JvmStatic
    fun UnlockGridsRes() = LauncherLayoutHooks.UnlockGridsRes()

    @JvmStatic
    fun UnlockGridsHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.UnlockGridsHook(lpparam)

    @JvmStatic
    fun HorizontalSpacingRes() = LauncherLayoutHooks.HorizontalSpacingRes()

    @JvmStatic
    fun IndicatorHeightRes() = LauncherLayoutHooks.IndicatorHeightRes()

    @JvmStatic
    fun ShowHotseatTitlesRes() = LauncherLayoutHooks.ShowHotseatTitlesRes()

    @JvmStatic
    fun DockMarginTopHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.DockMarginTopHook(lpparam)

    @JvmStatic
    fun DockMarginBottomHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.DockMarginBottomHook(lpparam)

    @JvmStatic
    fun WorkspaceCellPaddingTopHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.WorkspaceCellPaddingTopHook(lpparam)

    @JvmStatic
    fun IndicatorMarginTopHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.IndicatorMarginTopHook(lpparam)

    @JvmStatic
    fun HorizontalWidgetSpacingHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.HorizontalWidgetSpacingHook(lpparam)

    @JvmStatic
    fun NoWidgetOnlyHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.NoWidgetOnlyHook(lpparam)

    @JvmStatic
    fun MaxHotseatIconsCountHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam)

    @JvmStatic
    fun ResizableWidgetsHook(lpparam: PackageReadyParam) = LauncherLayoutHooks.ResizableWidgetsHook(lpparam)

    @JvmStatic
    fun FixAnimHook(lpparam: PackageReadyParam) = LauncherAnimationHooks.FixAnimHook(lpparam)

    @JvmStatic
    fun NoUnlockAnimationHook(lpparam: PackageReadyParam) = LauncherAnimationHooks.NoUnlockAnimationHook(lpparam)

    @JvmStatic
    fun NoZoomAnimationHook(lpparam: PackageReadyParam) = LauncherAnimationHooks.NoZoomAnimationHook(lpparam)

    @JvmStatic
    fun UseOldLaunchAnimationHook(lpparam: PackageReadyParam) = LauncherAnimationHooks.UseOldLaunchAnimationHook(lpparam)

    @JvmStatic
    fun ReverseLauncherPortraitHook(lpparam: PackageReadyParam) = LauncherAnimationHooks.ReverseLauncherPortraitHook(lpparam)

    @JvmStatic
    fun RecentsBlurRatioHook(lpparam: PackageReadyParam) = LauncherAnimationHooks.RecentsBlurRatioHook(lpparam)

    @JvmStatic
    fun DisableUnlockWallpaperScale(lpparam: PackageReadyParam) = LauncherAnimationHooks.DisableUnlockWallpaperScale(lpparam)

    @JvmStatic
    fun DisableLauncherWallpaperScale(lpparam: PackageReadyParam) = LauncherAnimationHooks.DisableLauncherWallpaperScale(lpparam)

    @JvmStatic
    fun NoClockHideHook(lpparam: PackageReadyParam) = LauncherSystemHooks.NoClockHideHook(lpparam)

    @JvmStatic
    fun FixAppInfoLaunchHook(lpparam: PackageReadyParam) = LauncherSystemHooks.FixAppInfoLaunchHook(lpparam)

    @JvmStatic
    fun HideFromRecentsHook(lpparam: PackageReadyParam) = LauncherSystemHooks.HideFromRecentsHook(lpparam)

    @JvmStatic
    fun CloseDrawerOnLaunchHook(lpparam: PackageReadyParam) = LauncherSystemHooks.CloseDrawerOnLaunchHook(lpparam)

    @JvmStatic
    fun StickyFloatingWindowsLauncherHook(lpparam: PackageReadyParam) = LauncherSystemHooks.StickyFloatingWindowsLauncherHook(lpparam)

    @JvmStatic
    fun HideStatusBarInRecentsHook(lpparam: PackageReadyParam) = LauncherSystemHooks.HideStatusBarInRecentsHook(lpparam)

    @JvmStatic
    fun DisableLauncherLogHook(lpparam: PackageReadyParam) = LauncherSystemHooks.DisableLauncherLogHook(lpparam)
}
