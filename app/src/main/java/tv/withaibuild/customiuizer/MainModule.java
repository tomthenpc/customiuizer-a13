package tv.withaibuild.customiuizer;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper;
import tv.withaibuild.customiuizer.mods.utils.ProcessScope;
import tv.withaibuild.customiuizer.mods.utils.ProcessScopes;
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks;
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers;
import tv.withaibuild.customiuizer.installers.AndroidPackageInstaller;
import tv.withaibuild.customiuizer.installers.GenericAppInstaller;
import tv.withaibuild.customiuizer.installers.InputMethodInstaller;
import tv.withaibuild.customiuizer.installers.LauncherInstaller;
import tv.withaibuild.customiuizer.installers.MediaInstaller;
import tv.withaibuild.customiuizer.installers.PackageInstallerRouter;
import tv.withaibuild.customiuizer.installers.PhoneInstaller;
import tv.withaibuild.customiuizer.installers.PowerKeeperInstaller;
import tv.withaibuild.customiuizer.installers.SecurityCenterInstaller;
import tv.withaibuild.customiuizer.installers.SettingsInstaller;
import tv.withaibuild.customiuizer.installers.SystemUiInstaller;
import tv.withaibuild.customiuizer.installers.SystemServerInstaller;
import tv.withaibuild.customiuizer.installers.WallpaperInstaller;
import tv.withaibuild.customiuizer.prefs.PreferenceLoadRegistry;
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
        if (remote == null || !PreferenceLoadRegistry.shouldLoad(remote, pkg)) return;
        initPrefs();

        if (scope == ProcessScope.INPUT_METHOD) {
            InputMethodInstaller.install(lpparam, pkg);
            return;
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

        if (scope == ProcessScope.WALLPAPER) {
            WallpaperInstaller.install(lpparam);
            return;
        }

        if (scope == ProcessScope.MEDIA) {
            MediaInstaller.install(lpparam, pkg);
            return;
        }

        if (scope == ProcessScope.PHONE) {
            PhoneInstaller.install(lpparam);
            return;
        }

        if (scope == ProcessScope.PACKAGE_INSTALLER) {
            PackageInstallerRouter.install(lpparam);
            return;
        }

        if (scope == ProcessScope.GENERIC_APP) {
            GenericAppInstaller.install(lpparam, pkg);
            return;
        }

        if (scope == ProcessScope.ANDROID_PACKAGE) {
            AndroidPackageInstaller.install(lpparam, this::watchPreferenceChange);
            return;
        }

        if (scope == ProcessScope.SYSTEM_UI) {
            if (SystemUiInstaller.hasAnySystemUiStartupFeature(mPrefs)) {
                SystemUiInstaller.install(lpparam, this::watchPreferenceChange);
            }
            return;
        }



        final boolean isLauncherPkg = scope == ProcessScope.LAUNCHER;

        if (isLauncherPkg) {
            if (LauncherInstaller.hasAnyLauncherPackageReadyFeature(mPrefs)) {
                LauncherInstaller.installPackageReady(lpparam);
            }
            if (LauncherInstaller.hasAnyLauncherApplicationFeature(mPrefs)) {
                LauncherInstaller.installApplication(lpparam);
            }
            if (LauncherInstaller.hasAnyLauncherApplicationFeature(mPrefs)) {
                watchPreferenceChange();
            }
        }
    }



}
