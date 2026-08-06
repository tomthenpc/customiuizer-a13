package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarAndClockHooks;
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher;
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime;
import tv.withaibuild.customiuizer.utils.PrefMap;

public final class AndroidPackageInstaller {

    private AndroidPackageInstaller() {}

    /**
     * Returns true if any android-package feature is enabled.
     *
     * This is an intentionally plain OR of the relevant preferences. It lets the
     * installer return early when no feature is enabled, avoiding FeatureRuntime,
     * FeatureDispatcher and the global preference listener.
     *
     * The function is package-visible so it can be tested with a synthetic PrefMap.
     */
    static boolean isAnyFeatureEnabled(PrefMap<String, Object> prefs) {
        return prefs.getInt("system_statusbarheight", 19) > 19
            || prefs.getInt("controls_navbarheight", 19) > 19
            || prefs.getStringAsInt("system_allrotations2", 1) > 1
            || prefs.getStringAsInt("system_rotateanim", 1) > 1
            || prefs.getBoolean("system_cleanshare")
            || prefs.getBoolean("system_cleanopenwith");
    }

    public static void install(final PackageReadyParam lpparam, final Runnable watchPreferences) {
        String pkg = lpparam.getPackageName();
        if (!"android".equals(pkg)) return;

        if (!isAnyFeatureEnabled(MainModule.mPrefs)) {
            return;
        }

        FeatureRuntime androidRuntime = null;

        if (MainModule.mPrefs.getInt("system_statusbarheight", 19) > 19) SystemStatusBarAndClockHooks.StatusBarHeightRes();
        if (MainModule.mPrefs.getInt("controls_navbarheight", 19) > 19) Controls.NavbarHeightRes();

        boolean listenerNeeded = false;

        if (MainModule.mPrefs.getBoolean("system_cleanshare") || MainModule.mPrefs.getBoolean("system_cleanopenwith")) {
            androidRuntime = FeatureDispatcher.createRuntime(pkg, lpparam, lpparam.getClassLoader(), MainModule.mPrefs);

            if (MainModule.mPrefs.getBoolean("system_cleanshare")) {
                if (FeatureDispatcher.installById("cleanShareMenu", androidRuntime)) {
                    listenerNeeded = true;
                }
            }
            if (MainModule.mPrefs.getBoolean("system_cleanopenwith")) {
                if (FeatureDispatcher.installById("cleanOpenWithMenu", androidRuntime)) {
                    listenerNeeded = true;
                }
            }
        }

        if (MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) > 1) {
            MainModule.getResHooks().setObjectReplacement("android", "bool", "config_allowAllRotations", MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) == 2);
        }
        if (MainModule.mPrefs.getStringAsInt("system_rotateanim", 1) > 1) SystemDisplayAndWindowHooks.RotationAnimationRes();

        if (listenerNeeded) watchPreferences.run();
    }
}
