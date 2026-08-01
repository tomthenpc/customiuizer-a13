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
import tv.withaibuild.customiuizer.mods.utils.ProcessScope;
import tv.withaibuild.customiuizer.mods.utils.ProcessScopes;
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks;
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers;
import tv.withaibuild.customiuizer.installers.LauncherInstaller;
import tv.withaibuild.customiuizer.installers.PackageInstallerRouter;
import tv.withaibuild.customiuizer.installers.SystemUiInstaller;
import tv.withaibuild.customiuizer.installers.SystemServerInstaller;
import tv.withaibuild.customiuizer.utils.PrefMap;
import tv.withaibuild.customiuizer.utils.PreferenceBootstrap;

public class MainModule extends XposedModule {

    public static PrefMap<String, Object> mPrefs = new PrefMap<String, Object>();
    String processName;

    private static final class ResourceHooksHolder {
        private static final ResourceHooks INSTANCE = new ResourceHooks();
    }

    public static ResourceHooks getResHooks() {
        return ResourceHooksHolder.INSTANCE;
    }

    private final PreferenceBootstrap preferenceBootstrap = new PreferenceBootstrap(
        this::getRemotePreferences,
        ModuleHelper.prefsName + "_remote",
        MainModule.mPrefs
    );

    @Override
    public void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        processName = param.getProcessName();
        XposedHelpers.moduleInst = this;
        XposedHelpers.log("CustoMIUIzer-A13 " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ", buildTime=" + BuildConfig.BUILD_TIME + "): loaded in " + processName);
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
        if (ProcessScopes.isKnownPackage(pkg)) return true;

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
        SystemServerInstaller.install(lpparam);
        watchPreferenceChange();
    }

    @Override
    public void onPackageReady(final PackageReadyParam lpparam) {
        if (!isSupportedAndroidVersion()) return;
        if (!lpparam.isFirstPackage()) return;

        String pkg = lpparam.getPackageName();
        ProcessScope scope = ProcessScopes.resolve(pkg, processName);
        if (ProcessScopes.isRejected(pkg, processName)) {
            return;
        }

        SharedPreferences remote = getRemotePrefs();
        if (remote == null || !needLoadPrefs(pkg, remote)) return;
        initPrefs();

        if (scope == ProcessScope.INPUT_METHOD) {
            if (mPrefs.getBoolean("controls_volumecursor")) Controls.VolumeCursorHook(lpparam);
            if (mPrefs.getBoolean("controls_nonavbar_fix_inputmethod")
                && mPrefs.getBoolean("controls_nonavbar")) {
                Various.FixInputMethodBottomMarginHook(lpparam);
            }
            if (pkg.startsWith("com.google.android.inputmethod")) {
                if (mPrefs.getInt("various_gboardpadding_port", 0) > 0 || mPrefs.getInt("various_gboardpadding_land", 0) > 0) Various.GboardPaddingHook(lpparam);
            }
            return;
        }

        if (mPrefs.getBoolean("various_alarmcompat") && mPrefs.getStringSet("various_alarmcompat_apps").contains(pkg)) {
            Various.AlarmCompatHook();
        }

        if (PackageInstallerRouter.install(pkg, lpparam)) return;

        if (scope == ProcessScope.SYSTEM_UI) {
            SystemUiInstaller.install(lpparam, this::watchPreferenceChange);
        }



        final boolean isLauncherPkg = scope == ProcessScope.LAUNCHER;

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
                    if (isLauncherPkg) LauncherInstaller.handleLoadLauncher(lpparam);
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



}
