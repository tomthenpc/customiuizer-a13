package tv.withaibuild.customiuizer.mods

import android.app.MiuiNotification
import android.app.PendingIntent
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

object SystemNotificationPopupsHooks {

    @JvmStatic
    fun BetterPopupsHideDelayHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethodSilently(MiuiNotification::class.java, "getFloatTime", HookerClassHelper.returnConstant(0))
        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.policy.HeadsUpManager", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                var delay = MainModule.mPrefs.getInt("system_betterpopups_delay", 0) * 1000
                if (delay == 0) delay = 5000
                XposedHelpers.setIntField(param.thisObject, "mMinimumDisplayTime", delay)
                XposedHelpers.setIntField(param.thisObject, "mHeadsUpNotificationDecay", delay)
                ModuleHelper.observePreferenceChange("systemui.headsUpDelay", param.thisObject, object : ModuleHelper.PreferenceObserver {
                    override fun onChange(key: String) {
                        if (key.contains("system_betterpopups_delay")) {
                            var delay = MainModule.mPrefs.getInt("system_betterpopups_delay", 0) * 1000
                            if (delay == 0) delay = 5000
                            XposedHelpers.setIntField(param.thisObject, "mMinimumDisplayTime", delay)
                            XposedHelpers.setIntField(param.thisObject, "mHeadsUpNotificationDecay", delay)
                        }
                    }
                })
            }
        })
    }

    @JvmStatic
    fun BetterPopupsNoHideHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpManager", lpparam.classLoader, "removeHeadsUpNotification", HookerClassHelper.DO_NOTHING)
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpManager", lpparam.classLoader, "removeOldHeadsUpNotification", HookerClassHelper.DO_NOTHING)

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpManager\$HeadsUpEntry", lpparam.classLoader, "updateEntry", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                XposedHelpers.setObjectField(param.thisObject, "mRemoveHeadsUpRunnable", Runnable {})
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpManager", lpparam.classLoader, "onExpandingFinished", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                XposedHelpers.setBooleanField(param.thisObject, "mReleaseOnExpandFinish", true)
            }
        })
    }

    @JvmStatic
    fun BetterPopupsSwipeDownHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.policy.AppMiniWindowManager", lpparam.classLoader, "canNotificationSlide", String::class.java, PendingIntent::class.java, HookerClassHelper.returnConstant(false))
    }
}
