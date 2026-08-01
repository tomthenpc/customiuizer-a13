package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Various;

/**
 * Installer for the in-call UI package.
 */
public final class PhoneInstaller {

    private PhoneInstaller() {}

    public static void install(PackageReadyParam lpparam) {
        if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) {
            Various.ShowCallUIHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("various_calluibright")) {
            Various.InCallBrightnessHook(lpparam);
        }
        if (MainModule.mPrefs.getBoolean("various_answerinheadup")) {
            Various.AnswerCallInHeadUpHook(lpparam);
        }
    }
}
