package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemShareAndOpenWithHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarAndClockHooks;

public final class AndroidPackageInstaller {

    private AndroidPackageInstaller() {}

    public static void install(final PackageReadyParam lpparam, final Runnable watchPreferences) {
        String pkg = lpparam.getPackageName();
        if (!"android".equals(pkg)) return;

        if (MainModule.mPrefs.getInt("system_statusbarheight", 19) > 19) SystemStatusBarAndClockHooks.StatusBarHeightRes();
        if (MainModule.mPrefs.getInt("controls_navbarheight", 19) > 19) Controls.NavbarHeightRes();

        if (MainModule.mPrefs.getBoolean("system_cleanshare")) SystemShareAndOpenWithHooks.CleanShareMenuHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_cleanopenwith")) SystemShareAndOpenWithHooks.CleanOpenWithMenuHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) > 1) {
            MainModule.getResHooks().setObjectReplacement("android", "bool", "config_allowAllRotations", MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) == 2);
        }
        if (MainModule.mPrefs.getStringAsInt("system_rotateanim", 1) > 1) SystemDisplayAndWindowHooks.RotationAnimationRes();

        watchPreferences.run();
    }
}
