package tv.withaibuild.customiuizer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Set;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarAndClockHooks;
import tv.withaibuild.customiuizer.mods.Various;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper;
import tv.withaibuild.customiuizer.mods.utils.ProcessScope;
import tv.withaibuild.customiuizer.mods.utils.ProcessScopes;
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks;
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers;
import tv.withaibuild.customiuizer.installers.InputMethodInstaller;
import tv.withaibuild.customiuizer.installers.LauncherInstaller;
import tv.withaibuild.customiuizer.installers.PackageInstallerRouter;
import tv.withaibuild.customiuizer.installers.PowerKeeperInstaller;
import tv.withaibuild.customiuizer.installers.SecurityCenterInstaller;
import tv.withaibuild.customiuizer.installers.SettingsInstaller;
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
            InputMethodInstaller.install(lpparam, pkg);
            return;
        }

        if (mPrefs.getBoolean("various_alarmcompat") && mPrefs.getStringSet("various_alarmcompat_apps").contains(pkg)) {
            Various.AlarmCompatHook();
        }

        if (scope == ProcessScope.SETTINGS_MAIN) {
            SettingsInstaller.install(lpparam);
            return;
        }

        if (scope == ProcessScope.SECURITY_CENTER_MAIN) {
            SecurityCenterInstaller.install(lpparam);
            return;
        }

        if (scope == ProcessScope.POWER_KEEPER) {
            PowerKeeperInstaller.install(lpparam);
            return;
        }

        if (PackageInstallerRouter.install(pkg, lpparam)) return;

        if (scope == ProcessScope.SYSTEM_UI) {
            SystemUiInstaller.install(lpparam, this::watchPreferenceChange);
        }



        final boolean isLauncherPkg = scope == ProcessScope.LAUNCHER;

        if (isLauncherPkg) {
            LauncherInstaller.installPackageReady(lpparam);
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
