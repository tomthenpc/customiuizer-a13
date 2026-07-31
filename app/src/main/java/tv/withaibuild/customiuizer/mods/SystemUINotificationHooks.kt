package tv.withaibuild.customiuizer.mods

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.UserHandle
import android.util.MiuiMultiWindowUtils
import android.view.View
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import miui.app.MiuiFreeFormManager
import miui.process.ForegroundInfo
import miui.process.ProcessManager
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.HookUtils
import tv.withaibuild.customiuizer.utils.PrefPair
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Suppress("UNUSED_PARAMETER")
object SystemUINotificationHooks {

    @JvmStatic
    fun HideDismissViewHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", lpparam.classLoader, "updateDismissView", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mDismissView = XposedHelpers.getObjectField(param.getThisObject(), "mDismissView") as? View
                if (mDismissView != null) {
                    mDismissView.visibility = View.GONE
                    param.returnAndSkip(null)
                }
            }
        })
    }

    @JvmStatic
    fun HideNoficationAccessIconHook(lpparam: PackageReadyParam) {
        val hideViewHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mShortCut = XposedHelpers.getObjectField(param.getThisObject(), "mShortCut") as? View
                if (mShortCut != null) {
                    mShortCut.visibility = View.GONE
                    param.returnAndSkip(null)
                }
            }
        }
        ModuleHelper.findAndHookMethod("com.android.systemui.qs.MiuiQSHeaderView", lpparam.classLoader, "updateShortCutVisibility", hideViewHook)
        ModuleHelper.findAndHookMethod("com.android.systemui.qs.MiuiNotificationHeaderView", lpparam.classLoader, "updateShortCutVisibility", hideViewHook)
    }

    @JvmStatic
    fun ReplaceShortcutAppHook(lpparam: PackageReadyParam) {
        val openAppHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = ModuleHelper.findContext(lpparam)
                var user = 0
                var pkgAppName = ""
                when (param.member.name) {
                    "startCalendarApp" -> {
                        user = MainModule.mPrefs.getInt("system_calendar_app_user", 0)
                        pkgAppName = MainModule.mPrefs.getString("system_calendar_app", "")
                    }
                    "startClockApp" -> {
                        user = MainModule.mPrefs.getInt("system_clock_app_user", 0)
                        pkgAppName = MainModule.mPrefs.getString("system_clock_app", "")
                    }
                    "startSettingsApp" -> {
                        user = MainModule.mPrefs.getInt("system_shortcut_app_user", 0)
                        pkgAppName = MainModule.mPrefs.getString("system_shortcut_app", "")
                    }
                }
                if (pkgAppName.isNotEmpty()) {
                    val pkg = PrefPair.first(pkgAppName)
                    val cls = PrefPair.second(pkgAppName)
                    if (cls.isEmpty()) return

                    val name = ComponentName(pkg, cls)
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    intent.component = name
                    if (user != 0) {
                        try {
                            val Dependency = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)
                            val StatusbarClsForDep = "com.android.systemui.statusbar.phone.CentralSurfaces"
                            val mStatusBar = XposedHelpers.callStaticMethod(Dependency, "get", XposedHelpers.findClass(StatusbarClsForDep, lpparam.classLoader))
                            XposedHelpers.callMethod(mStatusBar, "collapsePanels")
                            XposedHelpers.callMethod(mContext, "startActivityAsUser", intent, XposedHelpers.newInstance(UserHandle::class.java, user))
                        } catch (t: Throwable) {
                            XposedHelpers.log(t)
                        }
                    } else {
                        val activiyStarter = XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("com.android.systemui.Dependency", mContext.classLoader),
                            "get",
                            XposedHelpers.findClass("com.android.systemui.plugins.ActivityStarter", mContext.classLoader)
                        )
                        XposedHelpers.callMethod(activiyStarter, "startActivity", intent, true)
                    }
                    param.returnAndSkip(null)
                }
            }
        }
        if (MainModule.mPrefs.getString("system_shortcut_app", "").isNotEmpty()) {
            ModuleHelper.findAndHookMethod("com.miui.systemui.util.CommonUtil", lpparam.classLoader, "startSettingsApp", openAppHook)
        }
        if (MainModule.mPrefs.getString("system_calendar_app", "").isNotEmpty()) {
            ModuleHelper.findAndHookMethod("com.miui.systemui.util.CommonUtil", lpparam.classLoader, "startCalendarApp", Context::class.java, openAppHook)
        }
        if (MainModule.mPrefs.getString("system_clock_app", "").isNotEmpty()) {
            ModuleHelper.findAndHookMethod("com.miui.systemui.util.CommonUtil", lpparam.classLoader, "startClockApp", openAppHook)
        }
    }

    @JvmStatic
    fun OpenNotifyInFloatingWindowHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter", lpparam.classLoader, "startNotificationIntent", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pendingIntent = param.getArg(0) as? PendingIntent ?: return
                val mSbn = XposedHelpers.getObjectField(param.getArg(2), "mSbn") ?: return
                val pkgName: String = if (XposedHelpers.callMethod(mSbn, "isSubstituteNotification") as? Boolean == true) {
                    XposedHelpers.getObjectField(mSbn, "mPkgName") as? String ?: ""
                } else {
                    pendingIntent.creatorPackage ?: ""
                }
                val foregroundInfo = ProcessManager.getForegroundInfo()
                if (foregroundInfo != null) {
                    val topPackage = foregroundInfo.mForegroundPackageName
                    if (pkgName == topPackage || "com.miui.home" == topPackage) {
                        return
                    }
                }
                val whitelist = MainModule.mPrefs.getBoolean("system_notify_openinfw_in_whitelist")
                val appInList = MainModule.mPrefs.getStringSet("system_notify_openinfw_apps").contains(pkgName)
                if (whitelist xor appInList) {
                    return
                }
                val Dependency = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)
                val AppMiniWindowManager = XposedHelpers.callStaticMethod(
                    Dependency,
                    "get",
                    XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.policy.AppMiniWindowManager", lpparam.classLoader)
                )
                XposedHelpers.callMethod(AppMiniWindowManager, "launchMiniWindowActivity", pkgName, pendingIntent)
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun FixOpenNotifyInFreeFormHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.policy.AppMiniWindowManager", lpparam.classLoader, "launchMiniWindowActivity", String::class.java, PendingIntent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pkgName = param.getArg(0) as? String ?: return
                val pendingIntent = param.getArg(1) as? PendingIntent ?: return
                val foregroundInfo = ProcessManager.getForegroundInfo()
                if (foregroundInfo != null) {
                    val topPackage = foregroundInfo.mForegroundPackageName
                    if (pkgName == topPackage) {
                        return
                    }
                }
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "context") as? Context ?: return
                val freeFormStackInfoList = MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay(if (mContext.display != null) mContext.display!!.displayId else 0)
                var freeFormCount = freeFormStackInfoList?.size ?: 0
                if (freeFormCount == 2) return
                if (freeFormStackInfoList != null) {
                    for (rootTaskInfo in freeFormStackInfoList) {
                        if (pkgName == rootTaskInfo.packageName) return
                    }
                }
                if (!pendingIntent.isActivity) {
                    val bIntent = Intent(GlobalActions.ACTION_PREFIX + "SetFreeFormPackage")
                    bIntent.putExtra("package", pkgName)
                    bIntent.setPackage("android")
                    mContext.sendBroadcast(bIntent)
                }
                val intent = Intent()
                if (pkgName != "com.tencent.tim") {
                    XposedHelpers.callMethod(intent, "addFlags", 134217728)
                    XposedHelpers.callMethod(intent, "addFlags", 268435456)
                    XposedHelpers.callMethod(intent, "addMiuiFlags", 256)
                }
                val options = MiuiMultiWindowUtils.getActivityOptions(mContext, pkgName, true, false)
                pendingIntent.send(mContext, 0, intent, null, null, null, options?.toBundle())
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun DisableHeadsUpWhenMuteHook(lpparam: PackageReadyParam) {
        var mMuteVisible = false
        val disableHeadsUpHook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArgsCount() != 2) return
                val canPopup = param.getResult() as? Boolean ?: return
                if (canPopup && mMuteVisible) {
                    param.setResult(false)
                }
            }
        }
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.interruption.MiuiNotificationInterruptStateProviderImpl", lpparam.classLoader, "shouldPeek", disableHeadsUpHook)
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy", lpparam.classLoader, "updateVolumeZen", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                mMuteVisible = XposedHelpers.getBooleanField(param.getThisObject(), "mMuteVisible")
            }
        })
    }

    @JvmStatic
    fun ExtendedPowerMenuHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", object : MethodHook() {
            private var isListened = false

            override fun after(param: AfterHookCallback) {
                if (!isListened) {
                    isListened = true
                    val mContext = XposedHelpers.callMethod(param.getThisObject(), "getApplicationContext") as? Context ?: return
                    val powermenu = File(mContext.cacheDir, "extended_power_menu")
                    if (powermenu == null) {
                        XposedHelpers.log("ExtendedPowerMenuHook", "No writable path found!")
                        return
                    }
                    if (powermenu.exists()) powermenu.delete()

                    val resources = ModuleHelper.getModuleRes(mContext)
                    val inputStream: InputStream
                    val outputStream: FileOutputStream
                    val fileBytes: ByteArray
                    inputStream = resources.openRawResource(resources.getIdentifier("extended_power_menu", "raw", HookUtils.modulePkg))
                    fileBytes = ByteArray(inputStream.available())
                    inputStream.read(fileBytes)
                    outputStream = FileOutputStream(powermenu)
                    outputStream.write(fileBytes)
                    outputStream.close()
                    inputStream.close()

                    if (!powermenu.exists()) {
                        XposedHelpers.log("ExtendedPowerMenuHook", "MAML file not found in cache")
                    } else {
                        ModuleHelper.findAndHookConstructor("com.miui.maml.util.ZipResourceLoader", lpparam.classLoader, String::class.java, object : MethodHook() {
                            override fun before(param: BeforeHookCallback) {
                                val res = param.getArg(0) as? String ?: return
                                if ("/system/media/theme/default/powermenu" == res) {
                                    param.getArgs()[0] = powermenu.path
                                }
                            }
                        })
                    }
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.maml.ScreenElementRoot", lpparam.classLoader, "issueExternCommand", String::class.java, java.lang.Double::class.java, String::class.java, object : MethodHook() {
            @SuppressLint("MissingPermission")
            override fun before(param: BeforeHookCallback) {
                val cmd = param.getArg(0) as? String ?: return
                val scrContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext")
                val mContext = XposedHelpers.getObjectField(scrContext, "mContext") as? Context ?: return
                val pm = mContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
                val mService = XposedHelpers.getObjectField(pm, "mService")
                val mSystemExternCommandListener = XposedHelpers.getObjectField(param.getThisObject(), "mSystemExternCommandListener")

                var custom = false
                when (cmd) {
                    "recovery" -> {
                        XposedHelpers.callMethod(mService, "reboot", false, "recovery", false)
                        custom = true
                    }
                    "bootloader" -> {
                        XposedHelpers.callMethod(mService, "reboot", false, "bootloader", false)
                        custom = true
                    }
                    "softreboot" -> {
                        XposedHelpers.callMethod(mService, "reboot", false, null, false)
                        custom = true
                    }
                }

                if (custom) {
                    mSystemExternCommandListener?.let {
                        XposedHelpers.callMethod(it, "onCommand", param.getArg(0), param.getArg(1), param.getArg(2))
                    }
                    param.returnAndSkip(null)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.plugins.PluginEnablerImpl", lpparam.classLoader, "isEnabled", ComponentName::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val componentName = param.getArg(0) as? ComponentName ?: return
                if (componentName.className.contains("GlobalActions")) {
                    param.returnAndSkip(false)
                }
            }
        })
    }
}
