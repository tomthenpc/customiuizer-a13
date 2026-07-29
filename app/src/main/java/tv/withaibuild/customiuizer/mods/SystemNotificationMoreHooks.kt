package tv.withaibuild.customiuizer.mods

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.os.UserHandle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

object SystemNotificationMoreHooks {

    @JvmStatic
    fun NoMoreIconHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconAreaController", lpparam.classLoader, "setIconsVisibility", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mMoreIcon = XposedHelpers.getObjectField(param.thisObject, "mMoreIcon")
                if (mMoreIcon != null) XposedHelpers.setBooleanField(param.thisObject, "mForceHideMoreIcon", true)
            }
        })
    }

    @JvmStatic
    fun ShowNotificationsAfterUnlockHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.ExpandedNotification", lpparam.classLoader, "hasShownAfterUnlock", HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.ExpandedNotification", lpparam.classLoader, "setHasShownAfterUnlock", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setBooleanField(param.thisObject, "mHasShownAfterUnlock", false)
            }
        })
        ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.notification.MiuiNotificationCompat", lpparam.classLoader, "isKeptOnKeyguard", Notification::class.java, HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun NotificationRowMenuHook(lpparam: PackageReadyParam) {
        val appInfoIconResId = MainModule.resHooks.addResource("ic_appinfo", R.drawable.ic_appinfo12)
        val forceCloseIconResId = MainModule.resHooks.addResource("ic_forceclose", R.drawable.ic_forceclose12)
        val openInFwIconResId = MainModule.resHooks.addResource("ic_openinfw", R.drawable.ic_openinfw)
        val appInfoDescId = MainModule.resHooks.addResource("miui_notification_menu_appinfo_title", R.string.system_notifrowmenu_appinfo)
        val forceCloseDescId = MainModule.resHooks.addResource("miui_notification_menu_forceclose_title", R.string.system_notifrowmenu_forceclose)
        val openInFwDescId = MainModule.resHooks.addResource("miui_notification_menu_openinfw_title", R.string.system_notifrowmenu_openinfw)
        MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "notification_menu_icon_padding", 0f)
        MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "miui_notification_modal_menu_margin_left_right", 3f)
        MainModule.resHooks.setResReplacement("com.android.systemui", "drawable", "miui_notification_menu_ic_bg_active", R.drawable.miui_notification_menu_ic_bg_active)
        MainModule.resHooks.setResReplacement("com.android.systemui", "drawable", "miui_notification_menu_ic_bg_inactive", R.drawable.miui_notification_menu_ic_bg_inactive)

        val MiuiNotificationMenuItem = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow.MiuiNotificationMenuItem", lpparam.classLoader)
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", lpparam.classLoader, "createMenuViews", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val mMenuItems = XposedHelpers.getObjectField(param.thisObject, "mMenuItems") as? ArrayList<Any> ?: return

                val menuItem: Constructor<*> = MiuiNotificationMenuItem.constructors[0]
                val infoBtn: Any? = try {
                    menuItem.newInstance(param.thisObject, mContext, appInfoDescId, null, appInfoIconResId)
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                    null
                }
                val forceCloseBtn: Any? = try {
                    menuItem.newInstance(param.thisObject, mContext, forceCloseDescId, null, forceCloseIconResId)
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                    null
                }
                val openFwBtn: Any? = try {
                    menuItem.newInstance(param.thisObject, mContext, openInFwDescId, null, openInFwIconResId)
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                    null
                }
                if (infoBtn == null || forceCloseBtn == null || openFwBtn == null) return

                val notification = XposedHelpers.getObjectField(param.thisObject, "mSbn")
                val expandNotifyRow = XposedHelpers.getObjectField(param.thisObject, "mParent")
                mMenuItems.add(infoBtn)
                mMenuItems.add(forceCloseBtn)
                mMenuItems.add(openFwBtn)
                XposedHelpers.setObjectField(param.thisObject, "mMenuItems", mMenuItems)
                val menuMargin = XposedHelpers.getObjectField(param.thisObject, "mMenuMargin") as? Int ?: 0
                val mMenuContainer = XposedHelpers.getObjectField(param.thisObject, "mMenuContainer") as? LinearLayout ?: return
                val mInfoBtn = XposedHelpers.callMethod(infoBtn, "getMenuView") as? View ?: return
                val mForceCloseBtn = XposedHelpers.callMethod(forceCloseBtn, "getMenuView") as? View ?: return
                val mOpenFwBtn = XposedHelpers.callMethod(openFwBtn, "getMenuView") as? View ?: return

                val itemClick = View.OnClickListener { view ->
                    if (view == null) return@OnClickListener
                    val pkgName = XposedHelpers.callMethod(notification, "getPackageName") as? String ?: return@OnClickListener
                    val uid = XposedHelpers.callMethod(notification, "getAppUid") as? Int ?: 0
                    var user = 0
                    try {
                        user = XposedHelpers.callStaticMethod(UserHandle::class.java, "getUserId", uid) as? Int ?: 0
                    } catch (t: Throwable) {
                        XposedHelpers.log(t)
                    }

                    when (view) {
                        mInfoBtn -> {
                            ModuleHelper.openAppInfo(mContext, pkgName, user)
                            mContext.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                        }
                        mForceCloseBtn -> {
                            val am = mContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return@OnClickListener
                            if (user != 0)
                                XposedHelpers.callMethod(am, "forceStopPackageAsUser", pkgName, user)
                            else
                                XposedHelpers.callMethod(am, "forceStopPackage", pkgName)
                            try {
                                val appName = mContext.packageManager.getApplicationLabel(mContext.packageManager.getApplicationInfo(pkgName, 0))
                                Toast.makeText(mContext, ModuleHelper.getModuleRes(mContext).getString(R.string.force_closed, appName), Toast.LENGTH_SHORT).show()
                            } catch (ignore: Throwable) {}
                        }
                        mOpenFwBtn -> {
                            val Dependency = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)
                            val AppMiniWindowManager = XposedHelpers.callStaticMethod(Dependency, "get", XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.policy.AppMiniWindowManager", lpparam.classLoader))
                            val miniWindowPkg = XposedHelpers.callMethod(expandNotifyRow, "getMiniWindowTargetPkg")
                            val notifyIntent = XposedHelpers.callMethod(expandNotifyRow, "getPendingIntent") as? PendingIntent
                            val ModalControllerForDep = "com.android.systemui.statusbar.notification.modal.ModalController"
                            val ModalController = XposedHelpers.callStaticMethod(Dependency, "get", XposedHelpers.findClass(ModalControllerForDep, lpparam.classLoader))
                            XposedHelpers.callMethod(ModalController, "animExitModelCollapsePanels")
                            XposedHelpers.callMethod(AppMiniWindowManager, "launchMiniWindowActivity", miniWindowPkg, notifyIntent)
                        }
                    }
                }
                mInfoBtn.setOnClickListener(itemClick)
                mForceCloseBtn.setOnClickListener(itemClick)
                mOpenFwBtn.setOnClickListener(itemClick)
                val layoutParams = LinearLayout.LayoutParams(-2, -2)
                layoutParams.leftMargin = menuMargin
                layoutParams.rightMargin = menuMargin
                mMenuContainer.addView(mInfoBtn, layoutParams)
                mMenuContainer.addView(mForceCloseBtn, layoutParams)
                mMenuContainer.addView(mOpenFwBtn, layoutParams)
                val menuWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    52f,
                    mContext.resources.displayMetrics
                ).toInt()
                val titleId = mContext.resources.getIdentifier("modal_menu_title", "id", lpparam.packageName)
                for (obj in mMenuItems) {
                    val menuView = XposedHelpers.callMethod(obj, "getMenuView") as? View ?: continue
                    (menuView.findViewById<TextView>(titleId))?.maxWidth = menuWidth
                }
            }
        })
    }

    private fun checkVibration(pkgName: String, thisObject: Any): Boolean {
        return try {
            val opt = XposedHelpers.getAdditionalInstanceField(thisObject, "mVibrationMode") as? Int ?: 0
            val selectedApps = XposedHelpers.getAdditionalInstanceField(thisObject, "mVibrationApps") as? Set<String>
            val isSelected = selectedApps != null && selectedApps.contains(pkgName)
            opt == 2 && !isSelected || opt == 3 && isSelected
        } catch (t: Throwable) {
            XposedHelpers.log(t)
            false
        }
    }

    @JvmStatic
    fun SelectiveVibrationHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.vibrator.VibratorManagerService", lpparam.classLoader, "systemReady", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mVibrationMode", MainModule.mPrefs.getString("system_vibration", "1").toInt())
                ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
                    override fun onChange(key: String) {
                        if (key.endsWith("system_vibration")) {
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mVibrationMode", MainModule.mPrefs.getStringAsInt("system_vibration", 1))
                        }
                    }
                })

                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mVibrationApps", MainModule.mPrefs.getStringSet("system_vibration_apps"))
                ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
                    override fun onChange(key: String) {
                        if (key.contains("system_vibration_apps")) {
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mVibrationApps", MainModule.mPrefs.getStringSet("system_vibration_apps"))
                        }
                    }
                })
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.vibrator.VibratorManagerService", lpparam.classLoader, "vibrate", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pkgName = param.args[1] as? String ?: return
                if (checkVibration(pkgName, param.thisObject)) param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun NoDuckingHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.audio.FocusRequester", lpparam.classLoader, "handleFocusLoss", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.args[0] == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun OrientationLockHook(lpparam: SystemServerStartingParam) {
        val windowClass = "com.android.server.wm.DisplayRotation"
        val rotMethod = "rotationForOrientation"
        ModuleHelper.hookAllMethods(windowClass, lpparam.classLoader, rotMethod, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.args[0] == -1) {
                    val opt = MainModule.mPrefs.getInt("qs_autorotate_state", 0)
                    var prevOrient = param.args[1] as? Int ?: 0
                    if (opt == 1) {
                        if (prevOrient != 0 && prevOrient != 2) prevOrient = 0
                        if (param.result == 1 || param.result == 3) param.setResult(prevOrient)
                    } else if (opt == 2) {
                        if (prevOrient != 1 && prevOrient != 3) prevOrient = 1
                        if (param.result == 0 || param.result == 2) param.setResult(prevOrient)
                    }
                }
            }
        })
    }

    @JvmStatic
    fun DisableAnyNotificationBlockHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("android.app.NotificationChannel", lpparam.classLoader, "isBlockable", HookerClassHelper.returnConstant(true))
        ModuleHelper.findAndHookMethod("android.app.NotificationChannel", lpparam.classLoader, "setBlockable", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = true
            }
        })
    }

    @JvmStatic
    fun DisableAnyNotificationBlockHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("android.app.NotificationChannel", lpparam.classLoader, "isBlockable", HookerClassHelper.returnConstant(true))
        ModuleHelper.findAndHookMethod("android.app.NotificationChannel", lpparam.classLoader, "setBlockable", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = true
            }
        })
    }

    @JvmStatic
    fun DisableAnyNotificationHook(lpparam: PackageReadyParam) {
        if (lpparam.packageName.contains("systemui")) {
            val NotifyManagerCls = XposedHelpers.findClass("com.android.systemui.statusbar.notification.NotificationSettingsManager", lpparam.classLoader)
            XposedHelpers.setStaticBooleanField(NotifyManagerCls, "USE_WHITE_LISTS", false)
            ModuleHelper.findAndHookMethod("com.miui.systemui.NotificationCloudData\$Companion", lpparam.classLoader, "getFloatBlacklist", Context::class.java, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    param.returnAndSkip(ArrayList<String>())
                }
            })
        }
        ModuleHelper.hookAllMethods("miui.util.NotificationFilterHelper", lpparam.classLoader, "isNotificationForcedEnabled", HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("miui.util.NotificationFilterHelper", lpparam.classLoader, "isNotificationForcedFor", Context::class.java, String::class.java, HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("miui.util.NotificationFilterHelper", lpparam.classLoader, "canSystemNotificationBeBlocked", String::class.java, HookerClassHelper.returnConstant(true))
        ModuleHelper.findAndHookMethod("miui.util.NotificationFilterHelper", lpparam.classLoader, "containNonBlockableChannel", String::class.java, HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("miui.util.NotificationFilterHelper", lpparam.classLoader, "getNotificationForcedEnabledList", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(HashSet<String>())
            }
        })
    }

    @JvmStatic
    fun NotificationImportanceHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.settings.notification.BaseNotificationSettings", lpparam.classLoader, "setPrefVisible", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pref = param.args[0]
                if (pref != null) {
                    val prefKey = XposedHelpers.callMethod(pref, "getKey") as? String
                    if ("importance" == prefKey) {
                        param.args[1] = true
                    }
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.android.settings.notification.ChannelNotificationSettings", lpparam.classLoader, "setupChannelDefaultPrefs", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val pref = XposedHelpers.callMethod(param.thisObject, "findPreference", "importance")
                XposedHelpers.setObjectField(param.thisObject, "mImportance", pref)
                var mBackupImportance = XposedHelpers.getObjectField(param.thisObject, "mBackupImportance") as? Int ?: 0
                if (mBackupImportance > 0) {
                    val index = XposedHelpers.callMethod(pref, "findSpinnerIndexOfValue", mBackupImportance.toString()) as? Int ?: -1
                    if (index > -1) {
                        XposedHelpers.callMethod(pref, "setValueIndex", index)
                    }
                    val ImportanceListener = XposedHelpers.findClassIfExists("androidx.preference.Preference\$OnPreferenceChangeListener", lpparam.classLoader)
                    val handler = InvocationHandler { _, method, args ->
                        if (method.name == "onPreferenceChange") {
                            mBackupImportance = Integer.parseInt(args[1] as? String ?: "0")
                            XposedHelpers.setObjectField(param.thisObject, "mBackupImportance", mBackupImportance)
                            val mChannel = XposedHelpers.getObjectField(param.thisObject, "mChannel") as? NotificationChannel
                            mChannel?.importance = mBackupImportance
                            XposedHelpers.callMethod(mChannel, "lockFields", 4)
                            val mBackend = XposedHelpers.getObjectField(param.thisObject, "mBackend")
                            val mPkg = XposedHelpers.getObjectField(param.thisObject, "mPkg") as? String
                            val mUid = XposedHelpers.getObjectField(param.thisObject, "mUid") as? Int ?: 0
                            XposedHelpers.callMethod(mBackend, "updateChannel", mPkg, mUid, mChannel)
                            XposedHelpers.callMethod(param.thisObject, "updateDependents", false)
                        }
                        true
                    }
                    val mImportanceListener = Proxy.newProxyInstance(
                        lpparam.classLoader,
                        arrayOf(ImportanceListener),
                        handler
                    )
                    XposedHelpers.callMethod(pref, "setOnPreferenceChangeListener", mImportanceListener)
                }
            }
        })
    }

    @JvmStatic
    fun MaxNotificationIconsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconContainer", lpparam.classLoader, "miuiShowNotificationIcons", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val isShow = param.args[0] as? Boolean ?: false
                if (isShow) {
                    var opt = MainModule.mPrefs.getStringAsInt("system_maxsbicons", 0)
                    opt = if (opt == -1) 999 else opt
                    XposedHelpers.setObjectField(param.thisObject, "MAX_DOTS", 3)
                    XposedHelpers.setObjectField(param.thisObject, "MAX_STATIC_ICONS", opt)
                    val fieldLockVisible = "MAX_ICONS_ON_LOCKSCREEN"
                    XposedHelpers.setObjectField(param.thisObject, fieldLockVisible, opt)
                    XposedHelpers.callMethod(param.thisObject, "updateState")
                    param.returnAndSkip(null)
                }
            }
        })
    }

    @JvmStatic
    fun MoreNotificationsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.policy.NotificationCountLimitPolicy", lpparam.classLoader, "checkNotificationCountLimit", String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pkgName = param.args[0] as? String ?: return
                val mNotifications = XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mEntryManager"), "getAllNotifs") as? Collection<Any> ?: return
                val list = mNotifications.filter {
                    val notifyPkgName = XposedHelpers.callMethod(XposedHelpers.callMethod(it, "getSbn"), "getPackageName") as? String
                    pkgName == notifyPkgName
                }
                if (list.size < 24) param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun AutoDismissExpandedPopupsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.HeadsUpManagerPhone\$HeadsUpEntryPhone", lpparam.classLoader, "setExpanded", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val newValue = param.args[0] as? Boolean ?: false
                if (newValue) {
                    val expanded = XposedHelpers.getBooleanField(param.thisObject, "expanded")
                    if (expanded != newValue) {
                        XposedHelpers.callMethod(param.thisObject, "removeAutoRemovalCallbacks")
                        val nm = XposedHelpers.getSurroundingThis(param.thisObject)
                        val mHandler = XposedHelpers.getObjectField(nm, "mHandler") as? Handler ?: return
                        val mRemoveAlertRunnable = XposedHelpers.getObjectField(param.thisObject, "mRemoveAlertRunnable") as? Runnable ?: return
                        mHandler.postDelayed(mRemoveAlertRunnable, 5000)
                    }
                    param.returnAndSkip(null)
                }
            }
        })
    }

    @JvmStatic
    fun MinimalNotificationViewHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader, "updateNotification", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.args.size != 3) return
                val expandableRow = XposedHelpers.getObjectField(param.args[0], "row")
                val mNotificationData = XposedHelpers.getObjectField(param.thisObject, "mNotificationData")
                val newLowPriority = XposedHelpers.callMethod(mNotificationData, "isAmbient", XposedHelpers.callMethod(param.args[1], "getKey")) as? Boolean == true &&
                        !(XposedHelpers.callMethod(XposedHelpers.callMethod(param.args[1], "getNotification"), "isGroupSummary") as? Boolean ?: false)
                val hasEntry = XposedHelpers.callMethod(mNotificationData, "get", XposedHelpers.getObjectField(param.args[0], "key")) != null
                val isLowPriority = XposedHelpers.callMethod(expandableRow, "isLowPriority") as? Boolean ?: false
                XposedHelpers.callMethod(expandableRow, "setIsLowPriority", newLowPriority)
                val hasLowPriorityChanged = hasEntry && isLowPriority != newLowPriority
                XposedHelpers.callMethod(expandableRow, "setLowPriorityStateUpdated", hasLowPriorityChanged)
                XposedHelpers.callMethod(expandableRow, "updateNotification", param.args[0])
            }
        })
    }

    @JvmStatic
    fun NotificationChannelSettingsHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", lpparam.classLoader, "onClickInfoItem", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = param.args[0] as? Context ?: return
                val entry = XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mParent"), "getEntry")
                val id = XposedHelpers.callMethod(XposedHelpers.callMethod(entry, "getChannel"), "getId") as? String ?: return
                if ("miscellaneous" == id) return
                val notification = XposedHelpers.callMethod(entry, "getSbn")
                val nuCls = XposedHelpers.findClassIfExists("com.android.systemui.miui.statusbar.notification.NotificationUtil", lpparam.classLoader)
                if (nuCls != null) {
                    val isHybrid = XposedHelpers.callStaticMethod(nuCls, "isHybrid", notification) as? Boolean ?: false
                    if (isHybrid) return
                }
                val pkgName = XposedHelpers.callMethod(notification, "getPackageName") as? String ?: return
                val user = XposedHelpers.callMethod(notification, "getAppUid") as? Int ?: 0

                val bundle = Bundle()
                bundle.putString("android.provider.extra.CHANNEL_ID", id)
                bundle.putString("package", pkgName)
                bundle.putInt("uid", user)
                bundle.putString("miui.targetPkg", pkgName)
                val intent = Intent("android.intent.action.MAIN")
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(":android:show_fragment", "com.android.settings.notification.ChannelNotificationSettings")
                intent.putExtra(":android:show_fragment_args", bundle)
                intent.setClassName("com.android.settings", "com.android.settings.SubSettings")
                try {
                    XposedHelpers.callMethod(mContext, "startActivityAsUser", intent, Process.myUserHandle())
                    param.returnAndSkip(null)
                    val ModalController = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.android.systemui.Dependency", mContext.classLoader), "get", XposedHelpers.findClass("com.android.systemui.statusbar.notification.modal.ModalController", mContext.classLoader))
                    XposedHelpers.callMethod(ModalController, "animExitModelCollapsePanels")
                } catch (ignore: Throwable) {
                    XposedHelpers.log(ignore)
                }
            }
        })
    }

    @JvmStatic
    fun MuteVisibleNotificationsHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.policy.NotificationAlertController", lpparam.classLoader, "buzzBeepBlink", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val powerMgr = mContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager ?: return
                if (powerMgr.isInteractive) {
                    param.returnAndSkip(null)
                }
            }
        })
    }

    @JvmStatic
    fun BetterPopupsCenteredHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpManagerInjector", lpparam.classLoader, "miuiHeadsUpInset", Context::class.java, object : MethodHook() {
            private var mHeadsUpPaddingTop = 0
            private var mHeadsUpHeight = 0
            override fun after(param: AfterHookCallback) {
                val context = param.args[0] as? Context ?: return
                val resources = context.resources
                if (mHeadsUpPaddingTop == 0) {
                    val dimId = resources.getIdentifier("heads_up_status_bar_padding", "dimen", "com.android.systemui")
                    mHeadsUpPaddingTop = resources.getDimensionPixelSize(dimId)
                    mHeadsUpHeight = resources.getDimensionPixelSize(resources.getIdentifier("notification_max_heads_up_height", "dimen", "com.android.systemui"))
                }
                if (resources.configuration.orientation != 2) {
                    val mHeadsUpInset = param.result as? Int ?: 0
                    val mStatusBarHeight = mHeadsUpInset - mHeadsUpPaddingTop
                    val topMargin = (context.resources.displayMetrics.heightPixels + mStatusBarHeight - mHeadsUpHeight) / 2
                    param.setResult(topMargin)
                }
            }
        })
    }

    @JvmStatic
    fun WallpaperScaleLevelHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllConstructors("com.android.server.wm.WallpaperController", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val scale = MainModule.mPrefs.getInt("system_other_wallpaper_scale", 6) / 10.0f
                XposedHelpers.setObjectField(param.thisObject, "mMaxWallpaperScale", scale)
                ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
                    override fun onChange(key: String) {
                        if (key.contains("system_other_wallpaper_scale")) {
                            val value = MainModule.mPrefs.getInt("system_other_wallpaper_scale", 6)
                            XposedHelpers.setObjectField(param.thisObject, "mMaxWallpaperScale", value / 10.0f)
                        }
                    }
                })
            }
        })
    }

    @JvmStatic
    fun Disable72hStrongAuthHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.locksettings.LockSettingsStrongAuth", lpparam.classLoader, "rescheduleStrongAuthTimeoutAlarm", Long::class.javaPrimitiveType, Int::class.javaPrimitiveType, HookerClassHelper.DO_NOTHING)
    }
}
