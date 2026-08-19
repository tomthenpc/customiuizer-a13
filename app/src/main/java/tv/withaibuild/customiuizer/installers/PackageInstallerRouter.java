package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Various;

/**
 * Installer for the MIUI package installer package.
 */
public final class PackageInstallerRouter {

    private PackageInstallerRouter() {}

    public static void install(PackageReadyParam lpparam) {
        String pkg = lpparam.getPackageName();
        if ("com.miui.packageinstaller".equals(pkg)) {
            if (MainModule.mPrefs.getBoolean("various_miuiinstaller")) Various.MiuiPackageInstallerHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_installappinfo")) Various.AppInfoDuringMiuiInstallHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_installer_purify")) Various.PurePackageInstallerHook(lpparam);
        }
    }
}
