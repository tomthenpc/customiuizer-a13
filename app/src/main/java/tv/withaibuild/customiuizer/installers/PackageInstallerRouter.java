package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Various;

/**
 * Router for the MIUI package installer and other packages that do not yet
 * have a dedicated installer.
 */
public final class PackageInstallerRouter {

    private PackageInstallerRouter() {}

    public static boolean install(String pkg, PackageReadyParam lpparam) {
        if (pkg.equals("com.lbe.security.miui")) {
            if (MainModule.mPrefs.getStringAsInt("various_clipboard_defaultaction", 1) > 1) {
                Various.SmartClipboardActionHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.miui.packageinstaller")) {
            if (MainModule.mPrefs.getBoolean("various_miuiinstaller")) Various.MiuiPackageInstallerHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_installappinfo")) Various.AppInfoDuringMiuiInstallHook(lpparam);
            return true;
        }

        return false;
    }
}
