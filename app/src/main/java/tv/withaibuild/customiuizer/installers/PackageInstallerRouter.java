package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.LauncherAnimationHooks;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemFreeformAndMultiWindowHooks;
import tv.withaibuild.customiuizer.mods.Various;

public final class PackageInstallerRouter {

    private PackageInstallerRouter() {}

    public static boolean install(String pkg, PackageReadyParam lpparam) {
        if (pkg.equals("com.miui.miwallpaper")) {
            if (MainModule.mPrefs.getBoolean("launcher_disable_wallpaperscale")) {
                LauncherAnimationHooks.DisableUnlockWallpaperScale(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.lbe.security.miui")) {
            if (MainModule.mPrefs.getStringAsInt("various_clipboard_defaultaction", 1) > 1) {
                Various.SmartClipboardActionHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.android.incallui")) {
            if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) {
                Various.ShowCallUIHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("various_calluibright")) {
                Various.InCallBrightnessHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("various_answerinheadup")) {
                Various.AnswerCallInHeadUpHook(lpparam);
            }
            return true;
        }

        if (pkg.equals("com.miui.packageinstaller")) {
            if (MainModule.mPrefs.getBoolean("various_miuiinstaller")) Various.MiuiPackageInstallerHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_installappinfo")) Various.AppInfoDuringMiuiInstallHook(lpparam);
            return true;
        }

        if (pkg.equals("com.miui.screenshot")) {
            if (MainModule.mPrefs.getBoolean("system_screenshot")) SystemAudioAndVisualAndMoreHooks.ScreenshotConfigHook(lpparam);
            if (MainModule.mPrefs.getInt("system_screenshot_floattime", 0) > 0) SystemAudioAndVisualAndMoreHooks.ScreenshotFloatTimeHook(lpparam);
            return true;
        }

        if (pkg.equals("com.miui.gallery")) {
            int folder = MainModule.mPrefs.getStringAsInt("system_gallery_screenshots_path", 1);
            if (folder > 1) {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(lpparam);
            }
            return true;
        }

        return false;
    }
}
