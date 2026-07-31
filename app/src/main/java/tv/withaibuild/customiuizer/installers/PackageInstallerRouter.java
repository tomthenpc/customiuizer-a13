package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.GlobalActions;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVolumeHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsAndConnectivityHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsMoreHooks;
import tv.withaibuild.customiuizer.mods.Various;

public final class PackageInstallerRouter {

    private PackageInstallerRouter() {}

    public static void install(String pkg, PackageReadyParam lpparam) {
        if (pkg.equals("com.android.settings")) {
            if (MainModule.mPrefs.getStringAsInt("miuizer_settingsiconpos", 1) > 0) {
                GlobalActions.miuizerSettingsHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_separatevolume")) {
                SystemAudioAndVolumeHooks.NotificationVolumeSettingsRes();
                SystemAudioAndVolumeHooks.NotificationVolumeSettingsHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_disableanynotif")) {
                SystemNotificationMoreHooks.DisableAnyNotificationHook(lpparam);
                SystemNotificationMoreHooks.DisableAnyNotificationBlockHook(lpparam);
            }
            if (!"none".equals(MainModule.mPrefs.getString("system_defaultusb", "none"))) {
                SystemSettingsMoreHooks.USBConfigSettingsHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_notifimportance")) {
                SystemNotificationMoreHooks.NotificationImportanceHook(lpparam);
            }
            if (MainModule.mPrefs.getBoolean("system_wifipassword")) {
                SystemSettingsAndConnectivityHooks.ViewWifiPasswordHook(lpparam);
            }
        }

        if (pkg.startsWith("com.google.android.inputmethod")) {
            if (MainModule.mPrefs.getInt("various_gboardpadding_port", 0) > 0 || MainModule.mPrefs.getInt("various_gboardpadding_land", 0) > 0) {
                Various.GboardPaddingHook(lpparam);
            }
        }

        if (pkg.equals("com.miui.packageinstaller")) {
            if (MainModule.mPrefs.getBoolean("various_miuiinstaller")) Various.MiuiPackageInstallerHook(lpparam);
            if (MainModule.mPrefs.getBoolean("various_installappinfo")) Various.AppInfoDuringMiuiInstallHook(lpparam);
        }

        if (pkg.equals("com.miui.screenshot")) {
            if (MainModule.mPrefs.getBoolean("system_screenshot")) SystemAudioAndVisualAndMoreHooks.ScreenshotConfigHook(lpparam);
            if (MainModule.mPrefs.getInt("system_screenshot_floattime", 0) > 0) SystemAudioAndVisualAndMoreHooks.ScreenshotFloatTimeHook(lpparam);
        }

        if (pkg.equals("com.miui.gallery")) {
            int folder = MainModule.mPrefs.getStringAsInt("system_gallery_screenshots_path", 1);
            if (folder > 1) {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(lpparam);
            }
        }
    }
}
