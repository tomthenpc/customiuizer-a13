package tv.withaibuild.customiuizer.mods

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.Helpers

@Suppress("UNUSED_PARAMETER")
object LauncherSystemHooks {

    @JvmStatic
    fun NoClockHideHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "updateStatusBarClock", Long::class.javaPrimitiveType, HookerClassHelper.DO_NOTHING)
    }

    @JvmStatic
    fun FixAppInfoLaunchHook(lpparam: PackageReadyParam) {
        if (lpparam.packageName == "com.mi.android.globallauncher")
            ModuleHelper.hookAllMethods("com.miui.home.launcher.util.Utilities", lpparam.classLoader, "startDetailsActivityForInfo", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val itemInfo = param.getArgs()[0] ?: return
                    val component: ComponentName? = try {
                        XposedHelpers.callMethod(itemInfo, "getComponentName") as? ComponentName
                    } catch (_: Throwable) {
                        try {
                            XposedHelpers.callMethod(XposedHelpers.getObjectField(itemInfo, "intent"), "getComponent") as? ComponentName
                        } catch (_: Throwable) {
                            try {
                                XposedHelpers.getObjectField(itemInfo, "providerName") as? ComponentName
                            } catch (_: Throwable) {
                                XposedHelpers.getObjectField(XposedHelpers.getObjectField(itemInfo, "providerInfo"), "provider") as? ComponentName
                            }
                        }
                    }
                    if (component == null) return
                    val context = param.getArgs()[1] as? Context ?: return
                    val userHandle = XposedHelpers.callMethod(param.getArgs()[0], "getUser") as? UserHandle
                    ModuleHelper.openAppInfo(context, component.packageName, userHandle?.hashCode() ?: 0)
                    param.returnAndSkip(true)
                }
            })
        else
            ModuleHelper.hookAllMethods("com.miui.home.launcher.shortcuts.ShortcutMenuManager", lpparam.classLoader, "startAppDetailsActivity", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val component = XposedHelpers.callMethod(param.getArgs()[0], "getComponentName") as? ComponentName ?: return
                    val view = param.getArgs()[1] as? View ?: return
                    val userHandle = XposedHelpers.callMethod(param.getArgs()[0], "getUserHandle") as? UserHandle
                    ModuleHelper.openAppInfo(view.context, component.packageName, userHandle?.hashCode() ?: 0)
                    param.returnAndSkip(null)
                }
            })
    }

    @JvmStatic
    fun HideFromRecentsHook(lpparam: PackageReadyParam) {
        val activityManagerWrapper = XposedHelpers.findClassIfExists("com.android.systemui.shared.recents.system.ActivityManagerWrapper", lpparam.classLoader)
        val taskInfoCompat = XposedHelpers.findClassIfExists("com.android.systemui.shared.recents.model.GroupedRecentTaskInfoCompat", lpparam.classLoader)
        if (taskInfoCompat == null) {
            XposedHelpers.log("HideFromRecentsHook", "hook failed")
            return
        }
        ModuleHelper.findAndHookMethod(activityManagerWrapper, "needRemoveTask", taskInfoCompat, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArgs()[0] != null) {
                    val mainTask = XposedHelpers.getObjectField(param.getArgs()[0], "mMainTaskInfo")
                    if (mainTask != null) {
                        val componentName = XposedHelpers.getObjectField(mainTask, "topActivity") as? ComponentName
                        if (componentName != null) {
                            val pkgName = componentName.packageName
                            val selectedApps = MainModule.mPrefs.getStringSet("system_hidefromrecents_apps")
                            if (selectedApps.contains(pkgName)) {
                                param.setResult(true)
                            }
                        }
                    }
                }
            }
        })
    }

    @JvmStatic
    fun CloseDrawerOnLaunchHook(lpparam: PackageReadyParam) {
        val hook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val launcher = XposedHelpers.getObjectField(param.getThisObject(), "mLauncher") ?: return
                XposedHelpers.callMethod(launcher, "hideAppView")
            }
        }
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.allapps.category.fragment.AppsListFragment", lpparam.classLoader, "onClick", View::class.java, hook)
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.allapps.category.fragment.RecommendCategoryAppListFragment", lpparam.classLoader, "onClick", View::class.java, hook)
    }

    @JvmStatic
    fun StickyFloatingWindowsLauncherHook(lpparam: PackageReadyParam) {
        val fwBlackList = arrayListOf("com.miui.securitycenter", "com.miui.home", "com.android.camera")
        ModuleHelper.findAndHookMethod("com.miui.home.recents.views.RecentsContainer", lpparam.classLoader, "onAttachedToWindow", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val recents = param.getThisObject() ?: return
                val mContext = XposedHelpers.callMethod(recents, "getContext") as? Context ?: return
                val dismissRecentsReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        try {
                            val pkgName = intent.getStringExtra("package")
                            if (pkgName != null) {
                                XposedHelpers.callMethod(recents, "dismissRecentsToLaunchTargetTaskOrHome", pkgName, true)
                            }
                        } catch (t: Throwable) {
                            XposedHelpers.log(t)
                        }
                    }
                }
                ModuleHelper.registerModuleReceiver(
                    mContext,
                    "launcher.dismissRecentsReceiver",
                    dismissRecentsReceiver,
                    IntentFilter(GlobalActions.ACTION_PREFIX + "dismissRecentsWhenFreeWindowOpen"),
                    Context.RECEIVER_EXPORTED
                )
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher\$PerformLaunchAction", lpparam.classLoader, "run", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val intent = XposedHelpers.getObjectField(param.getThisObject(), "mIntent") as? android.content.Intent ?: return
                val pkgName = intent.component?.packageName ?: return
                if (fwBlackList.contains(pkgName)) return
                val launcher = XposedHelpers.getSurroundingThis(param.getThisObject()) ?: return
                val mAppTransitionManager = XposedHelpers.getObjectField(launcher, "mAppTransitionManager")
                val fwApps = XposedHelpers.getAdditionalInstanceField(launcher, "fwApps") as? String
                if (fwApps != null && fwApps.contains(pkgName)) {
                    XposedHelpers.setAdditionalInstanceField(mAppTransitionManager, "isFwApps", true)
                }
            }

            override fun after(param: AfterHookCallback) {
                val launcher = XposedHelpers.getSurroundingThis(param.getThisObject()) ?: return
                val mAppTransitionManager = XposedHelpers.getObjectField(launcher, "mAppTransitionManager")
                XposedHelpers.removeAdditionalInstanceField(mAppTransitionManager, "isFwApps")
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "launch", "com.miui.home.launcher.ShortcutInfo", View::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val act = param.getThisObject() as? Activity ?: return
                var fwApps = Settings.Global.getString(act.contentResolver, Helpers.modulePkg + ".fw.apps")
                if (fwApps == null) fwApps = ""
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "fwApps", fwApps)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.recents.QuickstepAppTransitionManagerImpl", lpparam.classLoader, "hasControlRemoteAppTransitionPermission", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val isFwApps = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "isFwApps")
                if (isFwApps != null) {
                    param.returnAndSkip(false)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.miui.home.recents.views.TaskView", lpparam.classLoader, "getActivityOptions", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pkgName = XposedHelpers.callMethod(param.getThisObject(), "getBasePackageName") as? String ?: return
                if (fwBlackList.contains(pkgName)) return
                val taskView = param.getThisObject() as? View ?: return
                val fwApps = Settings.Global.getString(taskView.context.contentResolver, Helpers.modulePkg + ".fw.apps")
                if (fwApps != null && fwApps.contains(pkgName)) {
                    param.returnAndSkip(XposedHelpers.callMethod(param.getThisObject(), "getActivityLaunchOptions", taskView))
                }
            }
        })
    }

    @JvmStatic
    fun HideStatusBarInRecentsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.common.DeviceLevelUtils", lpparam.classLoader, "isHideStatusBarWhenEnterRecents", HookerClassHelper.returnConstant(true))
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "keepStatusBarShowingForBetterPerformance", HookerClassHelper.returnConstant(false))
    }

    @JvmStatic
    fun DisableLauncherLogHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.miui.home.launcher.AnalyticalDataCollectorJobService", lpparam.classLoader, "onStartJob", HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.AnalyticalDataCollector", lpparam.classLoader, "canTrackLaunchAppEvent", HookerClassHelper.returnConstant(false))
        val oneTrackInterfaceUtils = XposedHelpers.findClassIfExists("com.miui.home.launcher.common.OneTrackInterfaceUtils", lpparam.classLoader)
        if (oneTrackInterfaceUtils != null) {
            XposedHelpers.setStaticObjectField(oneTrackInterfaceUtils, "IS_ENABLE", false)
        }
    }
}
