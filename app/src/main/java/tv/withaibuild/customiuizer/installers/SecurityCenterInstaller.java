package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemFreeformAndMultiWindowHooks;
import tv.withaibuild.customiuizer.mods.Various;

/**
 * Installer for the SecurityCenter package.
 */
public final class SecurityCenterInstaller {

    private SecurityCenterInstaller() {}

    public static void install(PackageReadyParam lpparam) {
        if (MainModule.mPrefs.getBoolean("various_appdetails")) Various.AppInfoHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_disableapp")) Various.AppsDisableHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_restrictapp")) Various.AppsRestrictHook(lpparam);
        if (MainModule.mPrefs.getBoolean("system_applock_scramblepin")) SystemAudioAndVisualAndMoreHooks.ScrambleAppLockPINHook(lpparam);
        if (MainModule.mPrefs.getStringAsInt("various_appsort", 1) > 1
                || MainModule.mPrefs.getStringAsInt("various_skip", 0) > 0) {
            Various.AppsDefaultSortHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("various_skip_interceptperm")) Various.InterceptPermHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_replace_defaultopen_with_openbydefault")) Various.OpenByDefaultHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_skip_securityscan")) Various.SkipSecurityScanHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_show_battery_temperature")) Various.ShowTempInBatteryHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_enable_sc_ai_clipboard_location")) Various.UnlockClipboardAndLocationHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_disable_freeform_suggest_blacklist")) SystemFreeformAndMultiWindowHooks.DisableSideBarSuggestionHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_disable_dock_suggest")) Various.DisableDockSuggestHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_enable_expand_sidebar")) {
            Various.AddSideBarExpandReceiverHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("system_hidelowbatwarn")) {
            Various.NoLowBatteryWarningHook();
        }
        if (MainModule.mPrefs.getBoolean("various_privacyapps_column_nums4")) {
            Various.PrivacyAppsLayoutHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("various_hide_report_ondetails")) {
            Various.HideReportButtonHook(lpparam);
        }
    }
}
