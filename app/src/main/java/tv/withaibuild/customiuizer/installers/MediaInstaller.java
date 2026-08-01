package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;

/**
 * Installer for media packages (screenshot, gallery).
 */
public final class MediaInstaller {

    private MediaInstaller() {}

    public static void install(PackageReadyParam lpparam, String pkg) {
        if ("com.miui.screenshot".equals(pkg)) {
            if (MainModule.mPrefs.getBoolean("system_screenshot")) {
                SystemAudioAndVisualAndMoreHooks.ScreenshotConfigHook(lpparam);
            }
            int floatTime = MainModule.mPrefs.getInt("system_screenshot_floattime", 0);
            if (floatTime > 0) {
                SystemAudioAndVisualAndMoreHooks.ScreenshotFloatTimeHook(lpparam);
            }
        } else if ("com.miui.gallery".equals(pkg)) {
            int folder = MainModule.mPrefs.getStringAsInt("system_gallery_screenshots_path", 1);
            if (folder > 1) {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(lpparam);
            }
        }
    }
}
