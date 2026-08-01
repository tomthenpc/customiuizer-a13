package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Various;

/**
 * Installer for generic applications that are not part of a dedicated process scope.
 */
public final class GenericAppInstaller {

    private GenericAppInstaller() {}

    public static void install(PackageReadyParam lpparam, String pkg) {
        if ("com.lbe.security.miui".equals(pkg)) {
            if (MainModule.mPrefs.getStringAsInt("various_clipboard_defaultaction", 1) > 1) {
                Various.SmartClipboardActionHook(lpparam);
            }
        }

        if (MainModule.mPrefs.getBoolean("various_alarmcompat")
                && MainModule.mPrefs.getStringSet("various_alarmcompat_apps").contains(pkg)) {
            Various.AlarmCompatHook();
        }
    }
}
