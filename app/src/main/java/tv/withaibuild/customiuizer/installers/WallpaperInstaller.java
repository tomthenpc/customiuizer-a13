package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.LauncherAnimationHooks;

/**
 * Installer for the live wallpaper package.
 */
public final class WallpaperInstaller {

    private WallpaperInstaller() {}

    public static void install(PackageReadyParam lpparam) {
        if (MainModule.mPrefs.getBoolean("launcher_disable_wallpaperscale")) {
            LauncherAnimationHooks.DisableUnlockWallpaperScale(lpparam);
        }
    }
}
