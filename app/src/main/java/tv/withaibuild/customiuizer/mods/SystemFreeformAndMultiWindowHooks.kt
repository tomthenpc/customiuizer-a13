package tv.withaibuild.customiuizer.mods

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings
import android.util.MiuiMultiWindowUtils
import android.util.Pair
import android.view.View
import android.widget.ImageView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import miui.app.MiuiFreeFormManager
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.GlobalActions
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.HookUtils
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

object SystemFreeformAndMultiWindowHooks {

    val fwApps = ConcurrentHashMap<String, Pair<Float, Rect?>>()

    fun serializeFwApps(): String {
        val data = StringBuilder()
        for ((key, value) in fwApps) {
            data.append(key).append(":").append(value.first).append(":")
            data.append(value.second?.flattenToString() ?: "-").append("|")
        }
        return data.toString().replaceFirst(Regex("\\|$"), "")
    }

    fun unserializeFwApps(data: String?) {
        fwApps.clear()
        if (data == null || "" == data) return
        val dataArr = data.split('|')
        for (appData in dataArr) {
            if ("" == appData) continue
            val appDataArr = appData.split(":")
            fwApps[appDataArr[0]] = Pair(appDataArr[1].toFloat(), if ("-" == appDataArr[2]) null else Rect.unflattenFromString(appDataArr[2]))
        }
    }

    fun storeFwAppsInSetting(context: Context) {
        Settings.Global.putString(context.contentResolver, HookUtils.modulePkg + ".fw.apps", serializeFwApps())
    }

    fun restoreFwAppsInSetting(context: Context) {
        unserializeFwApps(Settings.Global.getString(context.contentResolver, HookUtils.modulePkg + ".fw.apps"))
    }

    fun getTaskPackageName(thisObject: Any, taskId: Int): String? {
        return getTaskPackageName(thisObject, taskId, false, null)
    }

    fun getTaskPackageName(thisObject: Any, taskId: Int, options: ActivityOptions?): String? {
        return getTaskPackageName(thisObject, taskId, options != null, options)
    }

    fun getTaskPackageName(thisObject: Any, taskId: Int, withOptions: Boolean, options: ActivityOptions?): String? {
        val mRootWindowContainer = XposedHelpers.getObjectField(thisObject, "mRootWindowContainer") ?: return null
        val task = if (withOptions) {
            XposedHelpers.callMethod(mRootWindowContainer, "anyTaskForId", taskId, 2, options, true)
        } else {
            XposedHelpers.callMethod(mRootWindowContainer, "anyTaskForId", taskId, 0)
        } ?: return null
        val intent = XposedHelpers.getObjectField(task, "intent") as? Intent
        return intent?.component?.packageName
    }

    private fun patchActivityOptions(mContext: Context, options: ActivityOptions?, pkgName: String): ActivityOptions {
        var opt = options ?: ActivityOptions.makeBasic()
        XposedHelpers.callMethod(opt, "setLaunchWindowingMode", 5)
        XposedHelpers.callMethod(opt, "setMiuiConfigFlag", 2)

        val scale: Float
        val rect: Rect?
        val values = fwApps[pkgName]
        if (values == null || values.first == 0f || values.second == null) {
            scale = 0.7f
            rect = MiuiMultiWindowUtils.getFreeformRect(mContext)
        } else {
            scale = values.first
            rect = values.second
        }
        opt.setLaunchBounds(rect)
        try {
            val injector = XposedHelpers.callMethod(opt, "getActivityOptionsInjector")
            XposedHelpers.callMethod(injector, "setFreeformScale", scale)
        } catch (ignore: Throwable) {
            if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
            XposedHelpers.log(ignore)
        }
        return opt
    }

    private fun shouldOpenInFreeForm(intent: Intent?, callingPackage: String?): Boolean {
        if (intent == null || intent.component == null) return false
        val fwBlackList = listOf("com.miui.home", "com.android.camera", "com.android.systemui")
        val pkgName = intent.component!!.packageName
        if (fwBlackList.contains(pkgName)) return false

        var openInFw = false
        val openFwWhenShare = MainModule.mPrefs.getBoolean("system_fw_forcein_actionsend")
        if (openFwWhenShare) {
            val whitelist = MainModule.mPrefs.getBoolean("system_fw_forcein_actionsend_in_whitelist")
            val appInList = MainModule.mPrefs.getStringSet("system_fw_forcein_actionsend_apps").contains(pkgName)
            if (whitelist xor appInList) return false
            if ("com.miui.packageinstaller" == pkgName && intent.component!!.className.contains("com.miui.packageInstaller.NewPackageInstallerActivity")) {
                return true
            }
            if (Intent.ACTION_SEND == intent.action && pkgName != callingPackage) {
                openInFw = true
            } else if ("com.tencent.mm" == pkgName && intent.component!!.className.contains(".plugin.base.stub.WXEntryActivity")) {
                openInFw = true
            }
        }
        if (!openInFw) {
            val pkg = XposedHelpers.getAdditionalStaticField(MiuiFreeFormManager::class.java, "nextFreeformPackage")
            openInFw = pkgName == pkg
            if (openInFw) {
                XposedHelpers.removeAdditionalStaticField(MiuiFreeFormManager::class.java, "nextFreeformPackage")
            }
        }
        return openInFw
    }

    @JvmStatic
    fun OpenAppInFreeFormHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader, "onSystemReady", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val service = param.thisObject
                val mContext = XposedHelpers.getObjectField(service, "mContext") as? Context ?: return
                val intentFilter = IntentFilter()
                intentFilter.addAction(GlobalActions.ACTION_PREFIX + "SetFreeFormPackage")
                val mReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        ModuleHelper.guarded {
                            val action = intent.action ?: return@guarded
                            if (action == GlobalActions.ACTION_PREFIX + "SetFreeFormPackage") {
                                val pkg = intent.getStringExtra("package")
                                XposedHelpers.setAdditionalStaticField(MiuiFreeFormManager::class.java, "nextFreeformPackage", pkg)
                            }
                        }
                    }
                }
                ModuleHelper.registerModuleReceiver(
                    mContext,
                    "system.freeFormPackageReceiver",
                    mReceiver,
                    intentFilter,
                    Context.RECEIVER_EXPORTED
                )
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.wm.ActivityStarter", lpparam.classLoader, "executeRequest", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.args.isEmpty()) return
                val request = param.args[0]
                val intent = XposedHelpers.getObjectField(request, "intent") as? Intent ?: return
                val safeOptions = XposedHelpers.getObjectField(request, "activityOptions")
                if (safeOptions != null) {
                    val ao = XposedHelpers.getObjectField(safeOptions, "mOriginalOptions") as? ActivityOptions
                    if (ao != null && XposedHelpers.getIntField(ao, "mLaunchWindowingMode") == 5) {
                        return
                    }
                }
                val callingPackage = XposedHelpers.getObjectField(request, "callingPackage") as? String
                val openInFw = shouldOpenInFreeForm(intent, callingPackage)

                if (openInFw) {
                    val mService = XposedHelpers.getObjectField(param.thisObject, "mService")
                    val mContext = XposedHelpers.getObjectField(mService, "mContext") as? Context ?: return
                    val options = MiuiMultiWindowUtils.getActivityOptions(mContext, intent.component!!.packageName, true, false)
                    XposedHelpers.callMethod(param.thisObject, "setActivityOptions", options.toBundle())
                }
            }
        })
    }

    @JvmStatic
    fun StickyFloatingWindowsHook(lpparam: SystemServerStartingParam) {
        val fwBlackList = listOf("com.miui.securitycenter", "com.miui.home", "com.android.camera")

        ModuleHelper.hookAllMethods("com.android.server.wm.ActivityStarterInjector", lpparam.classLoader, "modifyLaunchActivityOptionIfNeed", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (!Modifier.isPublic(param.member.modifiers)) return
                val intent = param.args[5] as? Intent ?: return
                if (intent.component == null) return
                val pkgName = intent.component!!.packageName
                val mContext: Context? = try {
                    XposedHelpers.getObjectField(param.args[0], "mContext") as? Context
                } catch (ignore: Throwable) {
                    if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
                    val mService = XposedHelpers.getObjectField(param.args[0], "mService")
                    XposedHelpers.getObjectField(mService, "mContext") as? Context
                }
                if (mContext == null || fwBlackList.contains(pkgName)) return

                val options = param.result as? ActivityOptions
                val windowingMode = if (options == null) -1 else XposedHelpers.callMethod(options, "getLaunchWindowingMode") as? Int ?: -1

                if (windowingMode != 5 && fwApps.containsKey(pkgName)) {
                    try {
                        param.setResult(patchActivityOptions(mContext, options, pkgName))
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                        XposedHelpers.log(t)
                    }
                } else if (windowingMode == 5 && !fwApps.containsKey(pkgName)) {
                    fwApps[pkgName] = Pair(0f, null)
                    storeFwAppsInSetting(mContext)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.wm.ActivityTaskSupervisor", lpparam.classLoader, "startActivityFromRecents", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val safeOptions = param.args[3]
                val options = XposedHelpers.callMethod(safeOptions, "getOptions", param.thisObject) as? ActivityOptions
                if (options != null) {
                    val windowToken = XposedHelpers.callMethod(options, "getLaunchRootTask")
                    if (windowToken != null) return
                }
                val pkgName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "startPackageName") as? String
                XposedHelpers.removeAdditionalInstanceField(param.thisObject, "startPackageName")
                if (pkgName == null || fwBlackList.contains(pkgName)) return
                val windowingMode = if (options == null) -1 else XposedHelpers.callMethod(options, "getLaunchWindowingMode") as? Int ?: -1
                if (windowingMode == 5) {
                    fwApps[pkgName] = Pair(0f, null)
                    val mService = XposedHelpers.getObjectField(param.thisObject, "mService")
                    val mContext = XposedHelpers.getObjectField(mService, "mContext") as? Context ?: return
                    storeFwAppsInSetting(mContext)
                }
            }

            override fun before(param: BeforeHookCallback) {
                val safeOptions = param.args[3]
                val options = XposedHelpers.callMethod(safeOptions, "getOptions", param.thisObject) as? ActivityOptions
                if (options != null) {
                    val windowToken = XposedHelpers.callMethod(options, "getLaunchRootTask")
                    if (windowToken != null) return
                }
                val pkgName = getTaskPackageName(param.thisObject, param.args[2] as? Int ?: 0, options)
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "startPackageName", pkgName)
                if (pkgName == null || fwBlackList.contains(pkgName)) return
                val windowingMode = if (options == null) -1 else XposedHelpers.callMethod(options, "getLaunchWindowingMode") as? Int ?: -1
                if (windowingMode != 5 && fwApps.containsKey(pkgName)) {
                    val mService = XposedHelpers.getObjectField(param.thisObject, "mService")
                    val mContext = XposedHelpers.getObjectField(mService, "mContext") as? Context ?: return
                    val patched = patchActivityOptions(mContext, options, pkgName)
                    XposedHelpers.setObjectField(safeOptions, "mOriginalOptions", patched)
                    param.args[3] = safeOptions
                    val intent = Intent(GlobalActions.ACTION_PREFIX + "dismissRecentsWhenFreeWindowOpen")
                    intent.putExtra("package", pkgName)
                    mContext.sendBroadcast(intent)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController\$FreeFormReceiver", lpparam.classLoader, "onReceive", Context::class.java, Intent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val intent = param.args[1] as? Intent ?: return
                val action = intent.action
                if ("miui.intent.action_launch_fullscreen_from_freeform" == action) {
                    val parentThis = XposedHelpers.getSurroundingThis(param.thisObject)
                    XposedHelpers.setAdditionalInstanceField(parentThis, "skipFreeFormStateClear", true)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.wm.MiuiFreeFormGestureController", lpparam.classLoader, "notifyFullScreenWidnowModeStart", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.args.size != 3) return
                val pkgName = XposedHelpers.callMethod(param.args[1], "getStackPackageName") as? String ?: return
                val skipClear = XposedHelpers.getAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear")
                var skipFreeFormStateClear = false
                if (skipClear != null) {
                    skipFreeFormStateClear = skipClear as? Boolean ?: false
                }
                if (!skipFreeFormStateClear) {
                    if (fwBlackList.contains(pkgName)) return
                    if (fwApps.remove(pkgName) != null) {
                        val mService = XposedHelpers.getObjectField(param.thisObject, "mService")
                        val mContext = XposedHelpers.getObjectField(mService, "mContext") as? Context ?: return
                        storeFwAppsInSetting(mContext)
                    }
                } else {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear", false)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader, "launchSmallFreeFormWindow", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val taskId = XposedHelpers.getObjectField(param.args[0], "taskId")
                val mMiuiFreeFormManagerService = XposedHelpers.getObjectField(param.thisObject, "mMiuiFreeFormManagerService")
                val miuiFreeFormActivityStack = XposedHelpers.callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", taskId)
                val pkgName = XposedHelpers.callMethod(miuiFreeFormActivityStack, "getStackPackageName") as? String ?: return
                if (fwBlackList.contains(pkgName)) return
                if (!fwApps.containsKey(pkgName)) {
                    fwApps[pkgName] = Pair(0f, null)
                    val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                    storeFwAppsInSetting(mContext)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader, "onSystemReady", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val service = param.thisObject
                val mContext = XposedHelpers.getObjectField(service, "mContext") as? Context ?: return
                restoreFwAppsInSetting(mContext)
                val MiuiMultiWindowAdapter = XposedHelpers.findClass("android.util.MiuiMultiWindowAdapter", lpparam.classLoader)
                val blackList = XposedHelpers.getStaticObjectField(MiuiMultiWindowAdapter, "FREEFORM_BLACK_LIST") as? MutableList<String> ?: return
                blackList.clear()
                val freeformReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        ModuleHelper.guarded {
                            val action = intent.action
                            if ("miui.intent.action_launch_fullscreen_from_freeform" == action) {
                                XposedHelpers.setAdditionalInstanceField(service, "skipFreeFormStateClear", true)
                            }
                        }
                    }
                }
                ModuleHelper.registerModuleReceiver(
                    mContext,
                    "system.freeFormFullscreenReceiver",
                    freeformReceiver,
                    IntentFilter("miui.intent.action_launch_fullscreen_from_freeform"),
                    Context.RECEIVER_EXPORTED
                )
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader, "resizeTask", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pkgName = getTaskPackageName(param.thisObject, param.args[0] as? Int ?: 0) ?: return
                val skipClear = XposedHelpers.getAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear")
                var skipFreeFormStateClear = false
                if (skipClear != null) {
                    skipFreeFormStateClear = skipClear as? Boolean ?: false
                }
                if (skipFreeFormStateClear) {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear", false)
                } else {
                    if (fwBlackList.contains(pkgName)) return
                    val mMiuiFreeFormManagerService = XposedHelpers.getObjectField(param.thisObject, "mMiuiFreeFormManagerService")
                    val miuiFreeFormActivityStack = XposedHelpers.callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", param.args[0])
                    if (fwApps.containsKey(pkgName)) {
                        val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                        val sScale = XposedHelpers.callMethod(miuiFreeFormActivityStack, "getFreeFormScale") as? Float ?: 0f
                        fwApps[pkgName] = Pair(sScale, Rect(param.args[1] as? Rect ?: return))
                        storeFwAppsInSetting(mContext)
                    }
                }
            }
        })
    }

    @JvmStatic
    fun MessagingStyleLinesHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.notification.NotificationMessagingTemplateViewWrapper", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mMessagingLinearLayout = XposedHelpers.getObjectField(param.thisObject, "mMessagingLinearLayout")
                val mMaxDisplayedLines = XposedHelpers.getIntField(mMessagingLinearLayout, "mMaxDisplayedLines")
                if (mMaxDisplayedLines == Integer.MAX_VALUE) {
                    XposedHelpers.callMethod(mMessagingLinearLayout, "setMaxDisplayedLines", MainModule.mPrefs.getInt("system_messagingstylelines", Integer.MAX_VALUE))
                }
            }
        })
    }

    @JvmStatic
    fun MultiWindowPlusHook(lpparam: SystemServerStartingParam) {
        MainModule.getResHooks().setResReplacement("android", "array", "miui_resize_black_list", R.array.miui_resize_black_list)
        MainModule.getResHooks().setResReplacement("com.miui.rom", "array", "miui_resize_black_list", R.array.miui_resize_black_list)
        val AtmClass = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerServiceImpl", lpparam.classLoader)
        if (AtmClass != null) {
            ModuleHelper.findAndHookMethod(AtmClass, "updateResizeBlackList", Context::class.java, HookerClassHelper.DO_NOTHING)
            ModuleHelper.findAndHookMethod(AtmClass, "getSplitScreenBlackListFromXml", HookerClassHelper.DO_NOTHING)
            ModuleHelper.hookAllMethods(AtmClass, "inResizeBlackList", HookerClassHelper.returnConstant(false))
        }
    }

    @JvmStatic
    fun MultiWindowPlusHook(lpparam: PackageReadyParam) {
        if (lpparam.packageName == "com.miui.home") {
            ModuleHelper.findAndHookMethodSilently("com.android.systemui.shared.recents.model.Task", lpparam.classLoader, "isSupportSplit", HookerClassHelper.returnConstant(true))
            ModuleHelper.hookAllMethods("com.miui.home.recents.views.RecentMenuView", lpparam.classLoader, "onMessageEvent", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler") as? Handler ?: return
                    mHandler.postDelayed({
                        ModuleHelper.guarded {
                            val mMenuItemMultiWindow = XposedHelpers.getObjectField(param.thisObject, "mMenuItemMultiWindow") as? ImageView
                            val mMenuItemSmallWindow = XposedHelpers.getObjectField(param.thisObject, "mMenuItemSmallWindow") as? ImageView
                            mMenuItemMultiWindow?.isEnabled = true
                            mMenuItemMultiWindow?.imageAlpha = 255
                            mMenuItemSmallWindow?.isEnabled = true
                            mMenuItemSmallWindow?.imageAlpha = 255
                        }
                    }, 200)
                }
            })
        }
    }

    private fun DisableFloatingWindowBlacklistHook(cl: ClassLoader) {
        val clearHook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val blackList = param.result as? MutableList<String>
                if (blackList != null) {
                    blackList.clear()
                    blackList.add("com.android.camera")
                    param.setResult(blackList)
                }
            }
        }
        ModuleHelper.hookAllMethodsSilently("android.util.MiuiMultiWindowAdapter", cl, "getListFromCloudData", clearHook)
        ModuleHelper.hookAllMethodsSilently("android.util.MiuiMultiWindowAdapter", cl, "getStartFromFreeformBlackListFromCloud", clearHook)
        ModuleHelper.hookAllMethods("android.util.MiuiMultiWindowAdapter", cl, "getFreeformBlackList", clearHook)
        ModuleHelper.hookAllMethods("android.util.MiuiMultiWindowAdapter", cl, "getFreeformBlackListFromCloud", clearHook)
        ModuleHelper.hookAllMethods("android.util.MiuiMultiWindowAdapter", cl, "setFreeformBlackList", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val blackList = ArrayList<String>()
                blackList.add("com.android.camera")
                param.args[0] = blackList
            }
        })
        ModuleHelper.findAndHookMethod("android.util.MiuiMultiWindowUtils", cl, "isForceResizeable", HookerClassHelper.returnConstant(true))
        ModuleHelper.hookAllMethodsSilently("android.util.MiuiMultiWindowUtils", cl, "isPkgMainActivityResizeable", HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun DisableSideBarSuggestionHook(lpparam: PackageReadyParam) {
        DisableFloatingWindowBlacklistHook(lpparam.classLoader)
    }

    @JvmStatic
    fun NoFloatingWindowBlacklistHook(lpparam: SystemServerStartingParam) {
        MainModule.getResHooks().setResReplacement("android", "array", "freeform_black_list", R.array.miui_resize_black_list)
        MainModule.getResHooks().setResReplacement("com.miui.rom", "array", "freeform_black_list", R.array.miui_resize_black_list)
        DisableFloatingWindowBlacklistHook(lpparam.classLoader)
        ModuleHelper.findAndHookMethod("com.android.server.wm.MiuiFreeformServicesUtils", lpparam.classLoader, "supportsFreeform", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(true)
            }
        })
    }

    @JvmStatic
    fun BetterPopupsAllowFloatHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.NotificationSettingsManager", lpparam.classLoader, "canSlide", String::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val pkgName = param.args[0] as? String ?: return
                val selectedApps = MainModule.mPrefs.getStringSet("system_betterpopups_allowfloat_apps")
                val selectedAppsBlack = MainModule.mPrefs.getStringSet("system_betterpopups_allowfloat_apps_black")
                if (selectedApps.contains(pkgName)) param.setResult(true)
                else if (selectedAppsBlack.contains(pkgName)) param.setResult(false)
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.policy.MiniWindowPolicy", lpparam.classLoader, "canSlidePackage", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(true)
            }
        })
    }

    @JvmStatic
    fun SecureControlCenterHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethodSilently("com.android.keyguard.utils.MiuiKeyguardUtils", lpparam.classLoader, "supportExpandableStatusbarUnderKeyguard", HookerClassHelper.returnConstant(false))
        ModuleHelper.hookAllMethods("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl", lpparam.classLoader, "onContentChanged", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val key = param.args[0] as? String ?: return
                if ("expandable_under_lock_screen" == key) {
                    param.args[1] = "0"
                }
            }
        })
    }
}
