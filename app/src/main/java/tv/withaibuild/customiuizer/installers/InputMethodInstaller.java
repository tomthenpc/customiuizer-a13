package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.Various;

/**
 * Installer for input method packages.
 *
 * This installer replaces the inline package-name checks in {@link MainModule}
 * with one place responsible for all input method hooks.
 */
public final class InputMethodInstaller {

    private InputMethodInstaller() {}

    public static void install(PackageReadyParam lpparam, String pkg) {
        if (MainModule.mPrefs.getBoolean("controls_volumecursor")) {
            Controls.VolumeCursorHook(lpparam);
        }

        if (MainModule.mPrefs.getBoolean("controls_nonavbar_fix_inputmethod")
                && MainModule.mPrefs.getBoolean("controls_nonavbar")) {
            Various.FixInputMethodBottomMarginHook(lpparam);
        }

        if (pkg.startsWith("com.google.android.inputmethod")) {
            if (MainModule.mPrefs.getInt("various_gboardpadding_port", 0) > 0
                    || MainModule.mPrefs.getInt("various_gboardpadding_land", 0) > 0) {
                Various.GboardPaddingHook(lpparam);
            }
        }
    }
}
