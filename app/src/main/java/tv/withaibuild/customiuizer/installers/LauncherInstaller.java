package tv.withaibuild.customiuizer.installers;

import android.app.Application;
import android.content.Context;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.LauncherAnimationHooks;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper;
import tv.withaibuild.customiuizer.mods.LauncherFolderHooks;
import tv.withaibuild.customiuizer.mods.LauncherGestureHooks;
import tv.withaibuild.customiuizer.mods.LauncherIconHooks;
import tv.withaibuild.customiuizer.mods.LauncherLayoutHooks;
import tv.withaibuild.customiuizer.mods.LauncherSystemHooks;
import tv.withaibuild.customiuizer.mods.SystemFreeformAndMultiWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarAndClockHooks;
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher;
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime;

public final class LauncherInstaller {

    private LauncherInstaller() {}

    public static void installPackageReady(final PackageReadyParam lpparam) {
        if (MainModule.mPrefs.getInt("launcher_horizmargin", 0) > 0) LauncherLayoutHooks.HorizontalSpacingRes();
        if (MainModule.mPrefs.getInt("launcher_indicatorheight", 9) > 9) LauncherLayoutHooks.IndicatorHeightRes();
        if (MainModule.mPrefs.getInt("launcher_indicator_topmargin", 0) > 0) LauncherLayoutHooks.IndicatorMarginTopHook(lpparam);
        if (MainModule.mPrefs.getBoolean("launcher_unlockgrids")) {
            LauncherLayoutHooks.UnlockGridsRes();
            LauncherLayoutHooks.UnlockGridsHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("launcher_docktitles")) LauncherLayoutHooks.ShowHotseatTitlesRes();
        if (MainModule.mPrefs.getBoolean("launcher_disable_log")) {
            LauncherSystemHooks.DisableLauncherLogHook(lpparam);
        }
        if (MainModule.mPrefs.getInt("launcher_topmargin", 0) > 0) LauncherLayoutHooks.WorkspaceCellPaddingTopHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_dock_topmargin", 0) > 0) LauncherLayoutHooks.DockMarginTopHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_dock_bottommargin", 0) > 0) LauncherLayoutHooks.DockMarginBottomHook(lpparam);
    }

    public static void handleLoadLauncher(final PackageReadyParam lpparam) {
        boolean closeOnLaunch = false;
        FeatureRuntime launcherRuntime = FeatureDispatcher.createRuntime(lpparam.getPackageName(), lpparam, lpparam.getClassLoader(), MainModule.mPrefs);
        if (MainModule.mPrefs.getInt("launcher_swipedown_action", 1) != 1 ||
                MainModule.mPrefs.getInt("launcher_swipeup_action", 1) != 1 ||
                MainModule.mPrefs.getInt("launcher_swipedown2_action", 1) != 1 ||
                MainModule.mPrefs.getInt("launcher_swipeup2_action", 1) != 1) LauncherGestureHooks.HomescreenSwipesHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_swipeleft_action", 1) != 1 ||
                MainModule.mPrefs.getInt("launcher_swiperight_action", 1) != 1) LauncherGestureHooks.HotSeatSwipesHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_shake_action", 1) != 1) LauncherGestureHooks.ShakeHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_doubletap_action", 1) != 1) LauncherGestureHooks.LauncherDoubleTapHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_pinch_action", 1) != 1) LauncherGestureHooks.LauncherPinchHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_folder_cols", 1) > 1) FeatureDispatcher.installById("folderColumns", launcherRuntime);
        if (MainModule.mPrefs.getInt("launcher_iconscale", 45) > 45) LauncherIconHooks.IconScaleHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_titlefontsize", 5) > 5) LauncherIconHooks.TitleFontSizeHook(lpparam);
        if (MainModule.mPrefs.getInt("launcher_titletopmargin", 0) > 0) FeatureDispatcher.installById("titleTopMargin", launcherRuntime);
        FeatureDispatcher.installById("noClockHide", launcherRuntime);
        if (MainModule.mPrefs.getBoolean("launcher_renameapps")) LauncherIconHooks.RenameShortcutsHook(lpparam);
        if (MainModule.mPrefs.getBoolean("launcher_darkershadow")) LauncherIconHooks.TitleShadowHook(lpparam);
        if (MainModule.mPrefs.getBoolean("controls_nonavbar")) LauncherLayoutHooks.HideNavBarHook(lpparam);
        if (MainModule.mPrefs.getBoolean("launcher_infinitescroll")) LauncherLayoutHooks.InfiniteScrollHook(lpparam);
        if (MainModule.mPrefs.getBoolean("launcher_hidetitles")) FeatureDispatcher.installById("hideLauncherTitles", launcherRuntime);
        if (MainModule.mPrefs.getBoolean("launcher_fixlaunch")) FeatureDispatcher.installById("fixAppInfoLaunch", launcherRuntime);
        FeatureDispatcher.installById("noWidgetOnly", launcherRuntime);
        if (MainModule.mPrefs.getBoolean("launcher_sensorportrait")) LauncherAnimationHooks.ReverseLauncherPortraitHook(lpparam);
        if (MainModule.mPrefs.getBoolean("launcher_unlockhotseat")) LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("launcher_closefolders", 1) > 1) { LauncherFolderHooks.CloseFolderOnLaunchHook(lpparam); closeOnLaunch = true; }
        if ("com.miui.home".equals(lpparam.getPackageName())) {
            if (MainModule.mPrefs.getInt("system_recents_blur", 100) < 100) LauncherAnimationHooks.RecentsBlurRatioHook(lpparam);
            if (MainModule.mPrefs.getInt("controls_fsg_coverage", 60) != 60) Controls.BackGestureAreaHeightHook(lpparam);
            if (MainModule.mPrefs.getInt("controls_fsg_width", 100) > 100) Controls.BackGestureAreaWidthHook(lpparam);
            if (MainModule.mPrefs.getBoolean("controls_fsg_horiz")) LauncherGestureHooks.FSGesturesHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_removecleaner")) SystemStatusBarAndClockHooks.HideMemoryCleanHook(lpparam, true);
            if (MainModule.mPrefs.getBoolean("system_recents_disable_wallpaperscale") || MainModule.mPrefs.getBoolean("launcher_disable_wallpaperscale")) LauncherAnimationHooks.DisableLauncherWallpaperScale(lpparam);
            if (MainModule.mPrefs.getBoolean("system_fw_sticky")) LauncherSystemHooks.StickyFloatingWindowsLauncherHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_recents_hide_statusbar")) LauncherSystemHooks.HideStatusBarInRecentsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_fw_splitscreen")) SystemFreeformAndMultiWindowHooks.MultiWindowPlusHook(lpparam);
            if (MainModule.mPrefs.getBoolean("launcher_fixanim")) LauncherAnimationHooks.FixAnimHook(lpparam);
            if (MainModule.mPrefs.getBoolean("launcher_hideseekpoints")) LauncherLayoutHooks.HideSeekPointsHook(lpparam);
            if (MainModule.mPrefs.getBoolean("launcher_privacyapps_gest")
                || MainModule.mPrefs.getInt("launcher_spread_action", 1) != 1) LauncherFolderHooks.PrivacyFolderHook(lpparam);
            if (MainModule.mPrefs.getBoolean("system_hidefromrecents")) LauncherSystemHooks.HideFromRecentsHook(lpparam);
            if (MainModule.mPrefs.getInt("launcher_folderblur_opacity", 0) > 0) LauncherFolderHooks.FolderBlurHook(lpparam);
            if (MainModule.mPrefs.getBoolean("launcher_nounlockanim")) FeatureDispatcher.installById("noUnlockAnimation", launcherRuntime);
            if (MainModule.mPrefs.getBoolean("launcher_nozoomanim")) LauncherAnimationHooks.NoZoomAnimationHook(lpparam);
            if (MainModule.mPrefs.getBoolean("launcher_oldlaunchanim")) LauncherAnimationHooks.UseOldLaunchAnimationHook(lpparam);
            if (MainModule.mPrefs.getBoolean("launcher_closedrawer")) { LauncherSystemHooks.CloseDrawerOnLaunchHook(lpparam); closeOnLaunch = true; }
            if (MainModule.mPrefs.getInt("launcher_horizwidgetmargin", 0) > 0) LauncherLayoutHooks.HorizontalWidgetSpacingHook(lpparam);
            if (MainModule.mPrefs.getInt("controls_fsg_assist_left_action", 1) > 1
                || MainModule.mPrefs.getInt("controls_fsg_assist_right_action", 1) > 1
            )  LauncherGestureHooks.AssistGestureActionHook(lpparam);
            if (MainModule.mPrefs.getInt("controls_fsg_swipeandstop_action", 1) > 1) LauncherGestureHooks.SwipeAndStopActionHook(lpparam);
        }
        if (closeOnLaunch) LauncherFolderHooks.CloseFolderOrDrawerOnLaunchShortcutMenuHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_resizablewidgets")) LauncherLayoutHooks.ResizableWidgetsHook(lpparam);
    }

    public static void installApplication(final PackageReadyParam lpparam) {
        ModuleHelper.findAndHookMethod(Application.class, "attach", Context.class, new MethodHook() {
            @Override
            protected void after(AfterHookCallback param) throws Throwable {
                handleLoadLauncher(lpparam);
            }
        });
    }
}
