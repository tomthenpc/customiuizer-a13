package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.GlobalActions;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVolumeHooks;
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsAndConnectivityHooks;
import tv.withaibuild.customiuizer.mods.SystemSettingsMoreHooks;
import tv.withaibuild.customiuizer.utils.UsbDefaultFunctionMapper;

/**
 * Installer for the Settings package.
 */
public final class SettingsInstaller {

    private SettingsInstaller() {}

    public static void install(PackageReadyParam lpparam) {
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
        if (UsbDefaultFunctionMapper.toA13Function(MainModule.mPrefs.getString("system_defaultusb", "none")) != null) {
            SystemSettingsMoreHooks.USBConfigSettingsHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("system_notifimportance")) {
            SystemNotificationMoreHooks.NotificationImportanceHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("system_wifipassword")) {
            SystemSettingsAndConnectivityHooks.ViewWifiPasswordHook(lpparam);
        }
    }
}
