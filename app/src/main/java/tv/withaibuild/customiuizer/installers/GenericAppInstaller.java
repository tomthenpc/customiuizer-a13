package tv.withaibuild.customiuizer.installers;

import android.app.Application;
import android.content.Context;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.Controls;
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks;
import tv.withaibuild.customiuizer.mods.SystemStatusBarAndClockHooks;
import tv.withaibuild.customiuizer.mods.Various;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper;

/**
 * Installer for generic applications that are not part of a dedicated process scope.
 */
public final class GenericAppInstaller {

    private GenericAppInstaller() {}

    public static void install(PackageReadyParam lpparam, String pkg) {
        if ("com.lbe.security.miui".equals(pkg)) {
            if (MainModule.mPrefs.getStringAsInt("various_clipboard_defaultaction", 1) > 1) {
                Various.SmartClipboardActionHook(lpparam);
            }
        }

        if (MainModule.mPrefs.getBoolean("various_alarmcompat")
                && MainModule.mPrefs.getStringSet("various_alarmcompat_apps").contains(pkg)) {
            Various.AlarmCompatHook();
        }

        final boolean isStatusBarColor = MainModule.mPrefs.getBoolean("system_statusbarcolor") && MainModule.mPrefs.getStringSet("system_statusbarcolor_apps").contains(pkg);
        final boolean isNoOverscroll = MainModule.mPrefs.getBoolean("system_nooverscroll") && MainModule.mPrefs.getStringSet("system_nooverscroll_apps").contains(pkg);
        final boolean controlMedia = (MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0
                || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) && MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").contains(pkg);

        if (isStatusBarColor || isNoOverscroll || controlMedia) {
            ModuleHelper.findAndHookMethod(Application.class, "attach", Context.class, new MethodHook() {
                @Override
                protected void after(AfterHookCallback param) throws Throwable {
                    if (!isTargetPackage(param.getThisObject(), lpparam)) return;
                    if (isStatusBarColor) {
                        SystemStatusBarAndClockHooks.StatusBarBackgroundCompatHook(lpparam);
                        SystemStatusBarAndClockHooks.StatusBarBackgroundHook(lpparam);
                    }
                    if (isNoOverscroll) SystemAudioAndVisualAndMoreHooks.NoOverscrollAppHook(lpparam);
                    if (controlMedia) Controls.VolumeMediaPlayerHook(lpparam);
                }
            });
        }
    }

    /**
     * Verifies the hooked Application instance belongs to the package this installer was
     * registered for. This prevents a foreign package's Application.attach in the same
     * process from re-running legacy hook installation with a stale lpparam.
     */
    static boolean isTargetPackage(Object thisObject, PackageReadyParam lpparam) {
        if (!(thisObject instanceof Application)) return false;
        return lpparam.getPackageName().equals(((Application) thisObject).getPackageName());
    }
}
