package tv.withaibuild.customiuizer.installers;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Various;

/**
 * Installer for the PowerKeeper package.
 */
public final class PowerKeeperInstaller {

    private PowerKeeperInstaller() {}

    public static void install(PackageReadyParam lpparam) {
        if (MainModule.mPrefs.getBoolean("various_restrictapp")) Various.AppsRestrictPowerHook(lpparam);
        if (MainModule.mPrefs.getBoolean("various_persist_batteryoptimization")) Various.PersistBatteryOptimizationHook(lpparam);
    }
}
