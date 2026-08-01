package tv.withaibuild.customiuizer.mods

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.Instrumentation
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.hardware.input.InputManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.MiuiMultiWindowUtils
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import miui.app.MiuiFreeFormManager
import miui.process.ForegroundInfo
import miui.process.ProcessManager
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers.callStaticMethod
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers.findClass
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers.findClassIfExists
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers.findMethodExact
import java.lang.System.currentTimeMillis
import tv.withaibuild.customiuizer.utils.HookUtils

@Suppress("WeakerAccess")
object GlobalActions {

    @JvmField
    var mStatusBar: Any? = null
    private var mGlobalReceiverContext: Context? = null
    private var mSBReceiverContext: Context? = null

    private val mMainHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Handler(Looper.getMainLooper())
    }

    const val ACTION_PREFIX: String = "tv.withaibuild.customiuizer.mods.action."
    const val EVENT_PREFIX: String = "tv.withaibuild.customiuizer.mods.event."

    // Result codes for ordered broadcasts. RESULT_FIRST_USER (1) is used as the sentinel
    // for "no one claimed/handled this action".
    const val ACTION_UNHANDLED: Int = Activity.RESULT_FIRST_USER
    const val ACTION_HANDLED: Int = Activity.RESULT_FIRST_USER + 1

    @JvmStatic
    @JvmOverloads
    fun handleAction(context: Context?, key: String?, skipLock: Boolean = false): Boolean {
        return handleAction(context, key, skipLock, null)
    }

    @JvmStatic
    fun handleAction(context: Context?, key: String?, skipLock: Boolean, bundle: Bundle?): Boolean {
        if (context == null || key.isNullOrEmpty()) return false
        val action = MainModule.mPrefs.getInt(key + "_action", 1)
        if (action <= 1) return false
        if (action >= 85 && action <= 88) {
            if (isMediaActionsAllowed(context))
                sendDownUpKeyEvent(context, action, false)
            return true
        }
        return when (action) {
            2 -> commonSendAction(context, "ExpandNotifications")
            3 -> commonSendAction(context, "ExpandSettings")
            4 -> commonSendAction(context, "LockDevice")
            5 -> commonSendAction(context, "GoToSleep")
            6 -> commonSendAction(context, "TakeScreenshot")
            7 -> commonSendAction(context, "OpenRecents")
            8 -> launchAppIntent(context, key, skipLock)
            9 -> launchShortcutIntent(context, key, skipLock)
            10 -> toggleThis(context, MainModule.mPrefs.getInt(key + "_toggle", 0))
            11 -> commonSendAction(context, "SwitchToPrevApp")
            12 -> commonSendAction(context, "OpenPowerMenu")
            13 -> commonSendAction(context, "ClearMemory")
            14 -> commonSendAction(context, "ToggleColorInversion")
            15 -> commonSendAction(context, "GoBack")
            16 -> commonSendAction(context, "SimulateMenu")
            17 -> commonSendAction(context, "OpenVolumeDialog")
            18 -> commonSendAction(context, "VolumeUp")
            19 -> commonSendAction(context, "VolumeDown")
            20 -> launchActivityIntent(context, key, skipLock)
            21 -> commonSendAction(context, "SwitchKeyboard")
            22 -> commonSendAction(context, "SwitchOneHanded")
            23 -> commonSendAction(context, "ClearNotifications")
            24 -> commonSendAction(context, "ForceClose")
            25 -> commonSendAction(context, "ScrollToTop")
            26 -> showSidebar(context, bundle)
            27 -> commonSendAction(context, "FloatingWindow")
            28 -> commonSendAction(context, "PinningWindow")
            else -> false
        }
    }

    @JvmStatic
    fun getActionResId(action: Int): Int {
        return when (action) {
            0, 1 -> R.string.notselected
            2 -> R.string.array_global_actions_notif
            3 -> R.string.array_global_actions_eqs
            4 -> R.string.array_global_actions_lock
            5 -> R.string.array_global_actions_sleep
            6 -> R.string.array_global_actions_screenshot
            7 -> R.string.array_global_actions_recents
            11 -> R.string.array_global_actions_back
            12 -> R.string.array_global_actions_powermenu_short
            13 -> R.string.array_global_actions_clearmemory
            14 -> R.string.array_global_actions_invertcolors
            15 -> R.string.array_global_actions_goback
            16 -> R.string.array_global_actions_menu
            17 -> R.string.array_global_actions_volume
            18 -> R.string.array_global_actions_volume_up
            19 -> R.string.array_global_actions_volume_down
            21 -> R.string.array_global_actions_switchkeyboard
            22 -> R.string.array_global_actions_onehanded_left
            23 -> R.string.array_global_actions_clear_notifs
            24 -> R.string.array_global_actions_forceclose
            25 -> R.string.array_global_actions_scrolltotop
            26 -> R.string.array_global_actions_expandsidebar
            27 -> R.string.array_global_actions_floatingwindow
            28 -> R.string.array_global_actions_pinningwindow
            else -> 0
        }
    }

    /**
     * Performs the soft reboot and reports the outcome via the ordered broadcast's
     * result code. Must not depend on the settings process LSPosed bind state.
     *
     * [pm] is the PowerManager instance from which the hidden `mService` field is read.
     */
    @JvmStatic
    fun performFastReboot(receiver: BroadcastReceiver, pm: Any?) {
        performFastReboot(pm, receiver.isOrderedBroadcast, { code -> receiver.resultCode = code })
    }

    @JvmStatic
    internal fun performFastReboot(pm: Any?, isOrdered: Boolean, setResultCode: (Int) -> Unit) {
        val mService = try {
            XposedHelpers.getObjectField(pm, "mService")
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            if (isOrdered) setResultCode(ACTION_UNHANDLED)
            XposedHelpers.log(t)
            return
        }
        // Resolve before we claim; a ROM without the field keeps the broadcast unhandled.
        if (isOrdered) setResultCode(ACTION_HANDLED)
        try {
            XposedHelpers.callMethod(mService, "reboot", false, null, false)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            if (isOrdered) setResultCode(ACTION_UNHANDLED)
            XposedHelpers.log(t)
        }
    }

    private val mSBReceiver = object : BroadcastReceiver() {
        @SuppressLint("WrongConstant")
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val modRes = ModuleHelper.getModuleRes(context)
                val action = intent.action ?: return

                when (action) {
                    ACTION_PREFIX + "RestartSystemUI" -> Process.killProcess(Process.myPid())
                    ACTION_PREFIX + "FastReboot" -> {
                        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
                        performFastReboot(this, pm)
                    }
                    ACTION_PREFIX + "ClearNotifications" -> {
                        val nms = callStaticMethod(NotificationManager::class.java, "getService")
                        XposedHelpers.callMethod(nms, "cancelAllNotifications", null, 0)
                    }
                    ACTION_PREFIX + "ClearMemory" -> {
                        val clearIntent = Intent("com.android.systemui.taskmanager.Clear")
                        clearIntent.putExtra("show_toast", true)
                        context.sendBroadcast(clearIntent)
                    }
                    ACTION_PREFIX + "RestartLauncher" -> {
                        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
                        XposedHelpers.callMethod(am, "forceStopPackage", "com.miui.home")
                    }
                    ACTION_PREFIX + "RestartSecurityCenter" -> {
                        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
                        XposedHelpers.callMethod(am, "forceStopPackage", "com.miui.securitycenter")
                    }
                    ACTION_PREFIX + "FloatingWindow" ->
                        try {
                            MiuiMultiWindowUtils.startSmallFreeform(context)
                        } catch (err: Throwable) {
                            if (err is OutOfMemoryError) throw err
                            XposedHelpers.log(err)
                        }
                    ACTION_PREFIX + "PinningWindow" ->
                        try {
                            val foregroundInfo = ProcessManager.getForegroundInfo()
                            val topPackage = if (foregroundInfo != null) foregroundInfo.mForegroundPackageName else null
                            if ("com.miui.home" == topPackage || topPackage == null) return

                            val activityTaskManagerCls = findClassIfExists("android.app.ActivityTaskManager", context.classLoader)
                            val activityTaskManager = callStaticMethod(activityTaskManagerCls, "getService")
                            val rootTaskInfos = XposedHelpers.callMethod(activityTaskManager, "getAllRootTaskInfosOnDisplay", 0) as? List<*>
                            val freeFormStackInfoList = MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay(
                                context.display?.displayId ?: 0
                            )
                            val freeFormCount = freeFormStackInfoList?.size ?: 0
                            if (freeFormCount == 2) return

                            val ao = MiuiMultiWindowUtils.getActivityOptions(context, "com.android.mms", true, false)
                            rootTaskInfos?.forEach { rootTaskInfo ->
                                val conf = XposedHelpers.getObjectField(rootTaskInfo, "configuration")
                                val windowConfiguration = XposedHelpers.getObjectField(conf, "windowConfiguration")
                                val wmode = XposedHelpers.getIntField(windowConfiguration, "mWindowingMode")
                                val mActivityType = XposedHelpers.getIntField(windowConfiguration, "mActivityType")
                                if (wmode < 2 && mActivityType < 2) {
                                    val taskId = XposedHelpers.getIntField(rootTaskInfo, "taskId")
                                    XposedHelpers.callMethod(activityTaskManager, "startActivityFromRecents", taskId, ao.toBundle())
                                    mMainHandler.postDelayed(object : Runnable {
                                        override fun run() {
                                            mMainHandler.removeCallbacks(this)
                                            try {
                                                val injectInputEventMethod = InputManager::class.java.getDeclaredMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType!!)
                                                val instanceMethod = InputManager::class.java.getDeclaredMethod("getInstance")
                                                val im = instanceMethod.invoke(InputManager::class.java)
                                                val now = SystemClock.uptimeMillis()
                                                val homeDown = KeyEvent(now, now, 0, 3, 0, 0, -1, 0)
                                                val homeUp = KeyEvent(now, now, 1, 3, 0, 0, -1, 0)
                                                injectInputEventMethod.invoke(im, homeDown, 0)
                                                injectInputEventMethod.invoke(im, homeUp, 0)
                                            } catch (t: Throwable) {
                                                if (t is OutOfMemoryError) throw t
                                            }
                                        }
                                    }, 120)
                                    return
                                }
                            }
                        } catch (err: Throwable) {
                            if (err is OutOfMemoryError) throw err
                            XposedHelpers.log(err)
                        }
                    ACTION_PREFIX + "SwitchOneHanded" -> {
                        Settings.Secure.putInt(context.contentResolver, "one_handed_mode_activated", 1)
                        return
                    }
                    ACTION_PREFIX + "ScrollToTop" ->
                        mMainHandler.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    val injectInputEventMethod = InputManager::class.java.getDeclaredMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
                                    val instanceMethod = InputManager::class.java.getDeclaredMethod("getInstance")
                                    val im = instanceMethod.invoke(InputManager::class.java)
                                    val uptimeMillis = SystemClock.uptimeMillis()
                                    val swipeDownEvt = MotionEvent.obtain(uptimeMillis, uptimeMillis, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
                                    swipeDownEvt.setSource(InputDevice.SOURCE_TOUCHSCREEN)
                                    injectInputEventMethod.invoke(im, swipeDownEvt, 1)
                                    val swipeMoveEvt = MotionEvent.obtain(uptimeMillis, uptimeMillis + 25, MotionEvent.ACTION_MOVE, 500f, 240000f, 0)
                                    swipeMoveEvt.setSource(InputDevice.SOURCE_TOUCHSCREEN)
                                    injectInputEventMethod.invoke(im, swipeMoveEvt, 2)
                                    val swipeUpEvt = MotionEvent.obtain(uptimeMillis, uptimeMillis + 25, MotionEvent.ACTION_UP, 500f, 240000f, 0)
                                    swipeUpEvt.setSource(InputDevice.SOURCE_TOUCHSCREEN)
                                    injectInputEventMethod.invoke(im, swipeUpEvt, 2)
                                    swipeDownEvt.recycle()
                                    swipeMoveEvt.recycle()
                                    swipeUpEvt.recycle()
                                } catch (e: Throwable) {
                                    if (e is OutOfMemoryError) throw e
                                    XposedHelpers.log("err: $e")
                                }
                            }
                        }, 100L)
                }

                if (mStatusBar != null) {
                    when {
                        action == ACTION_PREFIX + "ExpandNotifications" -> try {
                            val mNotificationPanel = XposedHelpers.getObjectField(mStatusBar, "mNotificationPanel")
                            val mPanelExpanded = XposedHelpers.getBooleanField(mNotificationPanel, "mPanelExpanded")
                            val mQsExpanded = XposedHelpers.getBooleanField(mNotificationPanel, "mQsExpanded")
                            val expandOnly = intent.getBooleanExtra("expand_only", false)
                            if (mPanelExpanded) {
                                if (!expandOnly) {
                                    if (mQsExpanded)
                                        XposedHelpers.callMethod(mStatusBar, "closeQs")
                                    else
                                        XposedHelpers.callMethod(mStatusBar, "animateCollapsePanels")
                                }
                            } else {
                                XposedHelpers.callMethod(mStatusBar, "animateExpandNotificationsPanel")
                            }
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError) throw t
                            val token = Binder.clearCallingIdentity()
                            XposedHelpers.callMethod(context.getSystemService("statusbar"), "expandNotificationsPanel")
                            Binder.restoreCallingIdentity(token)
                        }

                        action == ACTION_PREFIX + "ExpandSettings" -> try {
                            val forceExpand = intent.getBooleanExtra("forceExpand", false)
                            val mControlCenterController = XposedHelpers.getObjectField(mStatusBar, "mControlCenterController")
                            val isUseControlCenter = XposedHelpers.callMethod(mControlCenterController, "isUseControlCenter") as? Boolean ?: false
                            if (isUseControlCenter) {
                                val isCollapsed = XposedHelpers.callMethod(mControlCenterController, "isCollapsed") as? Boolean ?: false
                                if (forceExpand || isCollapsed)
                                    XposedHelpers.callMethod(mControlCenterController, "openPanel")
                                else
                                    XposedHelpers.callMethod(mControlCenterController, "collapseControlCenter", true)
                                return
                            }

                            val mNotificationPanel = XposedHelpers.getObjectField(mStatusBar, "mNotificationPanelViewController")
                            val mPanelExpanded = XposedHelpers.getBooleanField(mNotificationPanel, "mPanelExpanded")
                            val mQsExpanded = XposedHelpers.getBooleanField(mNotificationPanel, "mQsExpanded")
                            if (!forceExpand && mPanelExpanded) {
                                if (mQsExpanded)
                                    XposedHelpers.callMethod(mStatusBar, "animateCollapsePanels", 0, false)
                                else
                                    XposedHelpers.callMethod(mNotificationPanel, "setQsExpanded", true)
                            } else {
                                XposedHelpers.callMethod(mStatusBar, "animateExpandSettingsPanel", null)
                            }
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError) throw t
                            val token = Binder.clearCallingIdentity()
                            XposedHelpers.callMethod(context.getSystemService("statusbar"), "expandSettingsPanel")
                            Binder.restoreCallingIdentity(token)
                        }

                        action == ACTION_PREFIX + "OpenRecents" -> {
                            val recentIntent = Intent("SYSTEM_ACTION_RECENTS")
                            recentIntent.setPackage("com.android.systemui")
                            context.sendBroadcast(recentIntent)
                        }

                        action == ACTION_PREFIX + "OpenVolumeDialog" -> try {
                            val mVolumeComponent = XposedHelpers.getObjectField(mStatusBar, "mVolumeComponent")
                            val mVolumeDialogPlugin = XposedHelpers.getObjectField(mVolumeComponent, "mDialog")
                            val miuiVolumeDialog = XposedHelpers.getObjectField(mVolumeDialogPlugin, "mVolumeDialogImpl")
                            if (miuiVolumeDialog == null) {
                                XposedHelpers.log("OpenVolumeDialog", "MIUI volume dialog is NULL!")
                                return
                            }

                            val mHandler = XposedHelpers.getObjectField(miuiVolumeDialog, "mHandler") as? Handler ?: return
                            mHandler.post(object : Runnable {
                                override fun run() {
                                    try {
                                        val mShowing = XposedHelpers.getBooleanField(miuiVolumeDialog, "mShowing")
                                        val mExpanded = XposedHelpers.getBooleanField(miuiVolumeDialog, "mExpanded")

                                        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                                        val isInCall = am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
                                        if (mShowing) {
                                            if (mExpanded || isInCall) {
                                                XposedHelpers.callMethod(miuiVolumeDialog, "dismissH", 1)
                                            } else {
                                                val mDialogView = XposedHelpers.getObjectField(miuiVolumeDialog, "mDialogView")
                                                val mExpandButton = XposedHelpers.getObjectField(mDialogView, "mExpandButton") as? View
                                                val mClickExpand = XposedHelpers.getObjectField(mDialogView, "expandListener") as? View.OnClickListener
                                                mClickExpand?.onClick(mExpandButton)
                                            }
                                        } else {
                                            val mController = XposedHelpers.getObjectField(mVolumeDialogPlugin, "mController")
                                            if (isInCall) {
                                                XposedHelpers.callMethod(mController, "setActiveStream", 0)
                                                XposedHelpers.setBooleanField(miuiVolumeDialog, "mNeedReInit", true)
                                            } else if (am.isMusicActive) {
                                                XposedHelpers.callMethod(mController, "setActiveStream", 3)
                                                XposedHelpers.setBooleanField(miuiVolumeDialog, "mNeedReInit", true)
                                            }
                                            XposedHelpers.callMethod(miuiVolumeDialog, "showH", 1)
                                        }
                                    } catch (t: Throwable) {
                                        if (t is OutOfMemoryError) throw t
                                        XposedHelpers.log(t)
                                    }
                                }
                            })
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError) throw t
                            XposedHelpers.log(t)
                        }

                        action == ACTION_PREFIX + "ToggleHotspot" -> {
                            val mHotspotController = callStaticMethod(
                                findClass("com.android.systemui.Dependency", context.classLoader),
                                "get",
                                findClassIfExists("com.android.systemui.statusbar.policy.HotspotController", context.classLoader)
                            )
                            if (mHotspotController == null) return
                            val mHotspotSupported = XposedHelpers.callMethod(mHotspotController, "isHotspotSupported") as? Boolean ?: false
                            if (!mHotspotSupported) return
                            val mHotspotEnabled = XposedHelpers.callMethod(mHotspotController, "isHotspotEnabled") as? Boolean ?: false
                            if (mHotspotEnabled)
                                Toast.makeText(context, modRes.getString(R.string.toggle_hotspot_off), Toast.LENGTH_SHORT).show()
                            else
                                Toast.makeText(context, modRes.getString(R.string.toggle_hotspot_on), Toast.LENGTH_SHORT).show()
                            XposedHelpers.callMethod(mHotspotController, "setHotspotEnabled", !mHotspotEnabled)
                        }

                        action == ACTION_PREFIX + "ToggleFlashlight" ->
                            callStaticMethod(findClass("com.miui.systemui.util.CommonUtil", context.classLoader), "toggleTorch")
                    }
                }
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) throw t
                XposedHelpers.log(t)
            }
        }
    }

    private val mGlobalReceiver = object : BroadcastReceiver() {
        @Suppress("ConstantConditions")
        @SuppressLint("MissingPermission", "WrongConstant", "NewApi")
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val modRes = ModuleHelper.getModuleRes(context)
                val action = intent.action ?: return
                // Actions
                if (action == ACTION_PREFIX + "RunParasitic") {
                    val intent2 = Intent()
                    intent2.action = "android.intent.action.MAIN"
                    intent2.addCategory("org.lsposed.manager.LAUNCH_MANAGER")
                    intent2.setClassName("com.android.shell", "com.android.shell.BugreportWarningActivity")
                    intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    context.startActivity(intent2)
                }
                if (action == ACTION_PREFIX + "WakeUp") {
                    XposedHelpers.callMethod(context.getSystemService(Context.POWER_SERVICE), "wakeUp", SystemClock.uptimeMillis())
                }
                if (action == ACTION_PREFIX + "GoToSleep") {
                    XposedHelpers.callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis(), 4, 0)
                }
                if (action == ACTION_PREFIX + "LockDevice") {
                    XposedHelpers.callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis(), 7, 0)
                }
                if (action == ACTION_PREFIX + "TakeScreenshot") {
                    context.sendBroadcast(Intent("android.intent.action.CAPTURE_SCREENSHOT"))
                }

                if (action == ACTION_PREFIX + "GoBack") {
                    Thread {
                        Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                    }.start()
                }

                if (action == ACTION_PREFIX + "SwitchToPrevApp") {
                    val pm = context.packageManager
                    val intentHome = Intent(Intent.ACTION_MAIN)
                    intentHome.addCategory(Intent.CATEGORY_HOME)
                    intentHome.addCategory(Intent.CATEGORY_DEFAULT)
                    val launcherList = pm.queryIntentActivities(intentHome, 0)

                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
                    @Suppress("deprecation")
                    val rti = am.getRecentTasks(Integer.MAX_VALUE, 0)

                    for (rtitem in rti) {
                        try {
                            @Suppress("deprecation")
                            if (am.getRunningTasks(1)[0].topActivity == rtitem.topActivity) continue

                            var isLauncher = false
                            val recentIntent = Intent(rtitem.baseIntent)
                            if (rtitem.origActivity != null) recentIntent.setComponent(rtitem.origActivity)
                            val resolvedAct = recentIntent.resolveActivity(pm)

                            if (resolvedAct != null) {
                                for (launcher in launcherList) {
                                    if (launcher.activityInfo.packageName != "com.android.settings" && launcher.activityInfo.packageName == resolvedAct.packageName) {
                                        isLauncher = true
                                        break
                                    }
                                }
                            }

                            if (!isLauncher) {
                                if (rtitem.id >= 0)
                                    am.moveTaskToFront(rtitem.id, 0)
                                else
                                    context.startActivity(recentIntent)
                                break
                            }
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError) throw t
                            XposedHelpers.log(t)
                        }
                    }
                }

                if (action == ACTION_PREFIX + "LaunchIntent") {
                    val launchIntent = intent.getParcelableExtra<Intent>("intent")
                    if (launchIntent != null) {
                        var user = 0
                        if (launchIntent.hasExtra("user")) {
                            user = launchIntent.getIntExtra("user", 0)
                            launchIntent.removeExtra("user")
                        }
                        if (user != 0)
                            XposedHelpers.callMethod(context, "startActivityAsUser", launchIntent, XposedHelpers.newInstance(UserHandle::class.java, user))
                        else
                            context.startActivity(launchIntent)
                    }
                }

                if (action == ACTION_PREFIX + "VolumeUp") {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, 1 shl 12 /* FLAG_FROM_KEY */ or AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_ALLOW_RINGER_MODES or AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_VIBRATE)
                }

                if (action == ACTION_PREFIX + "VolumeDown") {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, 1 shl 12 /* FLAG_FROM_KEY */ or AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_ALLOW_RINGER_MODES or AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_VIBRATE)
                }

                if (action == ACTION_PREFIX + "OpenPowerMenu") {
                    val clsWMG = XposedHelpers.findClass("android.view.WindowManagerGlobal", null)
                    val wms = callStaticMethod(clsWMG, "getWindowManagerService")
                    XposedHelpers.callMethod(wms, "showGlobalActions")
                }

                if (action == ACTION_PREFIX + "SwitchKeyboard") {
                    context.sendBroadcast(
                        Intent("com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER").setPackage("android")
                    )
                }

                if (action == ACTION_PREFIX + "ToggleColorInversion") {
                    val opt = Settings.Secure.getInt(context.contentResolver, "accessibility_display_inversion_enabled")
                    val conflictProp = ModuleHelper.proxySystemProperties("getInt", "ro.df.effect.conflict", 0, null) as? Int ?: 0
                    val conflictProp2 = ModuleHelper.proxySystemProperties("getInt", "ro.vendor.df.effect.conflict", 0, null) as? Int ?: 0
                    val hasConflict = conflictProp == 1 || conflictProp2 == 1
                    val dfMgr = callStaticMethod(XposedHelpers.findClass("miui.hardware.display.DisplayFeatureManager", null), "getInstance")
                    if (hasConflict && opt == 0) XposedHelpers.callMethod(dfMgr, "setScreenEffect", 15, 1)
                    Settings.Secure.putInt(context.contentResolver, "accessibility_display_inversion_enabled", if (opt == 0) 1 else 0)
                    if (hasConflict && opt != 0) XposedHelpers.callMethod(dfMgr, "setScreenEffect", 15, 0)
                }

                // Toggles
                if (action == ACTION_PREFIX + "ToggleWiFi") {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
                    if (wifiManager.isWifiEnabled) {
                        @Suppress("deprecation")
                        wifiManager.setWifiEnabled(false)
                        Toast.makeText(context, modRes.getString(R.string.toggle_wifi_off), Toast.LENGTH_SHORT).show()
                    } else {
                        @Suppress("deprecation")
                        wifiManager.setWifiEnabled(true)
                        Toast.makeText(context, modRes.getString(R.string.toggle_wifi_on), Toast.LENGTH_SHORT).show()
                    }
                }
                if (action == ACTION_PREFIX + "ToggleBluetooth") {
                    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    if (mBluetoothAdapter.isEnabled) {
                        mBluetoothAdapter.disable()
                        Toast.makeText(context, modRes.getString(R.string.toggle_bt_off), Toast.LENGTH_SHORT).show()
                    } else {
                        mBluetoothAdapter.enable()
                        Toast.makeText(context, modRes.getString(R.string.toggle_bt_on), Toast.LENGTH_SHORT).show()
                    }
                }
                if (action == ACTION_PREFIX + "ToggleNFC") {
                    val clsNfcAdapter = XposedHelpers.findClass("android.nfc.NfcAdapter", null)
                    val mNfcAdapter = XposedHelpers.callStaticMethod(clsNfcAdapter, "getNfcAdapter", context)
                    if (mNfcAdapter == null) return

                    val enableNFC = clsNfcAdapter.getDeclaredMethod("enable")
                    val disableNFC = clsNfcAdapter.getDeclaredMethod("disable")
                    enableNFC.isAccessible = true
                    disableNFC.isAccessible = true

                    if ((mNfcAdapter as? NfcAdapter)?.isEnabled == true) {
                        disableNFC.invoke(mNfcAdapter)
                        Toast.makeText(context, modRes.getString(R.string.toggle_nfc_off), Toast.LENGTH_SHORT).show()
                    } else {
                        enableNFC.invoke(mNfcAdapter)
                        Toast.makeText(context, modRes.getString(R.string.toggle_nfc_on), Toast.LENGTH_SHORT).show()
                    }
                }
                if (action == ACTION_PREFIX + "ToggleSoundProfile") {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                    val currentMode = am.ringerMode
                    when (currentMode) {
                        0 -> {
                            am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                            Toast.makeText(context, modRes.getString(R.string.toggle_sound_vibrate), Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            Toast.makeText(context, modRes.getString(R.string.toggle_sound_normal), Toast.LENGTH_SHORT).show()
                        }
                        2 -> {
                            am.ringerMode = AudioManager.RINGER_MODE_SILENT
                            Toast.makeText(context, modRes.getString(R.string.toggle_sound_silent), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                if (action == ACTION_PREFIX + "ToggleAutoBrightness") {
                    if (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 0) {
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 1)
                        Toast.makeText(context, modRes.getString(R.string.toggle_autobright_on), Toast.LENGTH_SHORT).show()
                    } else {
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
                        Toast.makeText(context, modRes.getString(R.string.toggle_autobright_off), Toast.LENGTH_SHORT).show()
                    }
                }
                if (action == ACTION_PREFIX + "ToggleAutoRotation") {
                    if (Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 0) {
                        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                        Toast.makeText(context, modRes.getString(R.string.toggle_autorotate_on), Toast.LENGTH_SHORT).show()
                    } else {
                        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation ?: 0
                        Settings.System.putInt(context.contentResolver, Settings.System.USER_ROTATION, rotation)
                        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                        Toast.makeText(context, modRes.getString(R.string.toggle_autorotate_off), Toast.LENGTH_SHORT).show()
                    }
                }
                if (action == ACTION_PREFIX + "ToggleMobileData") {
                    val telManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
                    val setMTE = TelephonyManager::class.java.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)
                    @Suppress("ALL")
                    val getMTE = TelephonyManager::class.java.getDeclaredMethod("getDataEnabled")
                    setMTE.isAccessible = true
                    getMTE.isAccessible = true

                    if (getMTE.invoke(telManager) as? Boolean == true) {
                        setMTE.invoke(telManager, false)
                        Toast.makeText(context, modRes.getString(R.string.toggle_mobiledata_off), Toast.LENGTH_SHORT).show()
                    } else {
                        setMTE.invoke(telManager, true)
                        Toast.makeText(context, modRes.getString(R.string.toggle_mobiledata_on), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) throw t
                XposedHelpers.log(t)
            }
        }
    }

    @JvmStatic
    fun miuizerSettingsHook(lpparam: PackageReadyParam) {
        val settingsIconResId = MainModule.getResHooks().addResource("ic_miuizer_settings", R.drawable.ic_miuizer_settings)
        ModuleHelper.findAndHookMethod("com.android.settings.MiuiSettings", lpparam.classLoader, "updateHeaderList", List::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArg(0) == null) return

                val mContext = (param.getThisObject() as? Activity)?.baseContext ?: return
                val opt = MainModule.mPrefs.getStringAsInt("miuizer_settingsiconpos", 1)

                val headerCls = findClassIfExists("com.android.settingslib.miuisettings.preference.PreferenceActivity\$Header", lpparam.classLoader)
                if (headerCls == null) return

                val modRes = ModuleHelper.getModuleRes(mContext)
                val header = XposedHelpers.newInstance(headerCls)
                XposedHelpers.setLongField(header, "id", 666)
                val intent = Intent()
                intent.setClassName(HookUtils.modulePkg, "tv.withaibuild.customiuizer.MainActivity")
                intent.putExtra("from.settings", true)
                XposedHelpers.setObjectField(header, "intent", intent)
                XposedHelpers.setIntField(header, "iconRes", settingsIconResId)
                XposedHelpers.setObjectField(header, "title", modRes.getString(R.string.app_name))
                val bundle = Bundle()
                val users = ArrayList<UserHandle>()
                users.add(XposedHelpers.newInstance(UserHandle::class.java, 0) as? UserHandle ?: return)
                bundle.putParcelableArrayList("header_user", users)
                XposedHelpers.setObjectField(header, "extras", bundle)

                val themes = mContext.resources.getIdentifier("launcher_settings", "id", mContext.packageName)
                val special = mContext.resources.getIdentifier("other_special_feature_settings", "id", mContext.packageName)

                @Suppress("UNCHECKED_CAST")
                val headers = param.getArg(0) as? MutableList<Any> ?: return
                var position = 0
                for (head in headers) {
                    position++
                    val id = XposedHelpers.getLongField(head, "id")
                    if (opt == 1 && id == -1L) { headers.add(position - 1, header); return }
                    if (opt == 2 && id == themes.toLong()) { headers.add(position, header); return }
                    if (opt == 3 && id == special.toLong()) { headers.add(position, header); return }
                }
                if (headers.size > 25)
                    headers.add(25, header)
                else
                    headers.add(header)
            }
        })
        ModuleHelper.hookAllMethods("com.android.settings.MiuiSettings\$HeaderAdapter", lpparam.classLoader, "setIcon", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val iconRes = XposedHelpers.getIntField(param.getArg(1), "iconRes")
                if (iconRes == settingsIconResId) {
                    val icon = XposedHelpers.getObjectField(param.getArg(0), "icon") as? ImageView
                    val iconSize = XposedHelpers.getIntField(XposedHelpers.getSurroundingThis(param.getThisObject()), "mNormalIconSize")
                    icon?.layoutParams?.height = iconSize
                }
            }
        })
    }

    @JvmStatic
    fun setupForegroundMonitor(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = param.getArg(0) as? Context ?: return
                val mBgHandler = XposedHelpers.getObjectField(param.getThisObject(), "mBgHandler") as? Handler ?: return
                ModuleHelper.hookAllMethods("com.miui.systemui.util.MiuiActivityUtil", lpparam.classLoader, "updateTopActivity", object : MethodHook() {
                    private var pkgName = ""
                    override fun after(param: AfterHookCallback) {
                        val mTopActivity = XposedHelpers.getObjectField(param.getThisObject(), "mTopActivity") as? ComponentName ?: return
                        val newPkg = mTopActivity.packageName
                        if (newPkg != pkgName) {
                            pkgName = newPkg
                            Settings.Global.putString(mContext.contentResolver, HookUtils.modulePkg + ".foreground.package", pkgName)
                        }
                    }
                })
                if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) {
                    ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarStateControllerImpl", lpparam.classLoader, "setSystemBarAttributes", object : MethodHook() {
                        private var fullScreen = false
                        override fun after(param: AfterHookCallback) {
                            val isFullScreen = XposedHelpers.getBooleanField(param.getThisObject(), "mIsFullscreen")
                            if (fullScreen != isFullScreen) {
                                mBgHandler.post {
                                    ModuleHelper.guarded("GlobalActions.foregroundFullscreenWriter") {
                                        Settings.Global.putInt(mContext.contentResolver, HookUtils.modulePkg + ".foreground.fullscreen", if (fullScreen) 1 else 0)
                                    }
                                }
                            }
                            fullScreen = isFullScreen
                        }
                    })
                }
            }
        })
    }

    @JvmStatic
    fun setupGlobalActions(lpparam: XposedModuleInterface.SystemServerStartingParam) {
        ModuleHelper.hookAllConstructors("com.android.server.accessibility.AccessibilityManagerService", lpparam.classLoader, object : MethodHook() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            override fun after(param: AfterHookCallback) {
                val mGlobalContext = param.getArg(0) as? Context ?: return

                if (mGlobalReceiverContext != null) {
                    try {
                        mGlobalReceiverContext?.unregisterReceiver(mGlobalReceiver)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) throw t
                    }
                }

                val intentFilter = IntentFilter()

                // Actions
                intentFilter.addAction(ACTION_PREFIX + "WakeUp")
                intentFilter.addAction(ACTION_PREFIX + "GoToSleep")
                intentFilter.addAction(ACTION_PREFIX + "LockDevice")
                intentFilter.addAction(ACTION_PREFIX + "TakeScreenshot")
                intentFilter.addAction(ACTION_PREFIX + "SwitchToPrevApp")
                intentFilter.addAction(ACTION_PREFIX + "GoBack")
                intentFilter.addAction(ACTION_PREFIX + "OpenPowerMenu")
                intentFilter.addAction(ACTION_PREFIX + "SwitchKeyboard")
                intentFilter.addAction(ACTION_PREFIX + "ToggleColorInversion")
                intentFilter.addAction(ACTION_PREFIX + "VolumeUp")
                intentFilter.addAction(ACTION_PREFIX + "VolumeDown")
                intentFilter.addAction(ACTION_PREFIX + "LaunchIntent")

                // Toggles
                intentFilter.addAction(ACTION_PREFIX + "ToggleWiFi")
                intentFilter.addAction(ACTION_PREFIX + "ToggleBluetooth")
                intentFilter.addAction(ACTION_PREFIX + "ToggleNFC")
                intentFilter.addAction(ACTION_PREFIX + "ToggleSoundProfile")
                intentFilter.addAction(ACTION_PREFIX + "ToggleAutoBrightness")
                intentFilter.addAction(ACTION_PREFIX + "ToggleAutoRotation")
                intentFilter.addAction(ACTION_PREFIX + "ToggleMobileData")

                // Tools
//                intentFilter.addAction(ACTION_PREFIX + "RunParasitic")
                //intentFilter.addAction(ACTION_PREFIX + "QueryXposedService")

                mGlobalContext.registerReceiver(mGlobalReceiver, intentFilter, Context.RECEIVER_EXPORTED)
                mGlobalReceiverContext = mGlobalContext
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.policy.BaseMiuiPhoneWindowManager", lpparam.classLoader, "initInternal", object : MethodHook() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val intentFilter = IntentFilter()
                intentFilter.addAction(ACTION_PREFIX + "SimulateMenu")
                intentFilter.addAction(ACTION_PREFIX + "ForceClose")
                intentFilter.addAction(ACTION_PREFIX + "SaveLastMusicPausedTime")
                val thisObject = param.getThisObject()
                val superCls = thisObject.javaClass.superclass ?: return
                val windowReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        ModuleHelper.guarded("GlobalActions.windowReceiver") {
                            val action = intent.action ?: return@guarded

                            if (action == ACTION_PREFIX + "SimulateMenu") try {
                                val fRequestShowMenu = XposedHelpers.findField(superCls, "mRequestShowMenu")
                                fRequestShowMenu.isAccessible = true
                                fRequestShowMenu.set(thisObject, true)
                                val markShortcutTriggered = findMethodExact(superCls, "markShortcutTriggered", *emptyArray<Class<*>>())
                                markShortcutTriggered.isAccessible = true
                                markShortcutTriggered.invoke(thisObject)
                                val injectEvent = findMethodExact(superCls, "injectEvent", Int::class.javaPrimitiveType!!)
                                injectEvent.isAccessible = true
                                injectEvent.invoke(thisObject, 82)
                            } catch (t1: Throwable) {
                                if (t1 is OutOfMemoryError) throw t1
                                try {
                                    val mHandler = XposedHelpers.getObjectField(thisObject, "mHandler") as? Handler
                                    mHandler?.sendMessageDelayed(mHandler.obtainMessage(1, "show_menu"), ViewConfiguration.getLongPressTimeout().toLong())
                                } catch (t2: Throwable) {
                                    if (t2 is OutOfMemoryError) throw t2
                                    XposedHelpers.log(t2)
                                }
                            }

                            if (action == ACTION_PREFIX + "ForceClose") try {
                                val closeApp = findMethodExact(superCls, "closeApp", Boolean::class.javaPrimitiveType!!)
                                closeApp.isAccessible = true
                                closeApp.invoke(thisObject, false)
                            } catch (t: Throwable) {
                                if (t is OutOfMemoryError) throw t
                                XposedHelpers.log(t)
                            }

                            if (action == ACTION_PREFIX + "SaveLastMusicPausedTime") {
                                Settings.System.putLong(context.contentResolver, "last_music_paused_time", currentTimeMillis())
                            }
                        }
                    }
                }
                ModuleHelper.registerModuleReceiver(
                    mContext,
                    "system.globalActionsWindowReceiver",
                    windowReceiver,
                    intentFilter,
                    Context.RECEIVER_EXPORTED
                )
            }
        })
    }

    @JvmStatic
    fun setupStatusBar(lpparam: PackageReadyParam) {
        val statusBarClass = findClassIfExists("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader)
        if (statusBarClass == null) return
        ModuleHelper.findAndHookMethod(statusBarClass, "start", object : MethodHook() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            override fun after(param: AfterHookCallback) {
                mStatusBar = param.getThisObject()
                val mStatusBarContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return

                if (mSBReceiverContext != null) {
                    try {
                        mSBReceiverContext?.unregisterReceiver(mSBReceiver)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) throw t
                    }
                }

                val intentFilter = IntentFilter()

                intentFilter.addAction(ACTION_PREFIX + "ExpandNotifications")
                intentFilter.addAction(ACTION_PREFIX + "ExpandSettings")
                intentFilter.addAction(ACTION_PREFIX + "OpenRecents")
                intentFilter.addAction(ACTION_PREFIX + "OpenVolumeDialog")

                intentFilter.addAction(ACTION_PREFIX + "ToggleGPS")
                intentFilter.addAction(ACTION_PREFIX + "ToggleHotspot")
                intentFilter.addAction(ACTION_PREFIX + "ToggleFlashlight")

                intentFilter.addAction(ACTION_PREFIX + "ClearMemory")
                intentFilter.addAction(ACTION_PREFIX + "ClearNotifications")
                intentFilter.addAction(ACTION_PREFIX + "RestartSystemUI")
                intentFilter.addAction(ACTION_PREFIX + "RestartLauncher")
                intentFilter.addAction(ACTION_PREFIX + "RestartSecurityCenter")
                intentFilter.addAction(ACTION_PREFIX + "FloatingWindow")
                intentFilter.addAction(ACTION_PREFIX + "PinningWindow")
                intentFilter.addAction(ACTION_PREFIX + "SwitchOneHanded")
//                intentFilter.addAction(ACTION_PREFIX + "CopyToExternal")
                intentFilter.addAction(ACTION_PREFIX + "FastReboot")

                intentFilter.addAction(ACTION_PREFIX + "ScrollToTop")

                mStatusBarContext.registerReceiver(mSBReceiver, intentFilter, Context.RECEIVER_EXPORTED)
                mSBReceiverContext = mStatusBarContext
            }
        })
    }

    enum class IntentType {
        APP, ACTIVITY, SHORTCUT
    }

    @SuppressLint("WrongConstant")
    @JvmStatic
    fun getIntent(context: Context, pref: String, intentType: IntentType, skipLock: Boolean): Intent? {
        return try {
            var modifiedPref = pref
            when (intentType) {
                IntentType.APP -> modifiedPref += "_app"
                IntentType.ACTIVITY -> modifiedPref += "_activity"
                IntentType.SHORTCUT -> modifiedPref += "_shortcut_intent"
            }

            val prefValue = MainModule.mPrefs.getString(modifiedPref, "")
            if (prefValue.isEmpty()) null
            else {
                var intent = Intent()
                if (intentType == IntentType.SHORTCUT) {
                    intent = Intent.parseUri(prefValue, 0)
                } else {
                    val pkgAppArray = prefValue.split('|')
                    if (pkgAppArray.size < 2) return null
                    val name = ComponentName(pkgAppArray[0], pkgAppArray[1])
                    intent.setComponent(name)
                    val user = MainModule.mPrefs.getInt(modifiedPref + "_user", 0)
                    if (user != 0) intent.putExtra("user", user)
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                if (intentType == IntentType.APP) {
                    intent.setAction(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                }

                if (skipLock) {
                    intent.addFlags(335544320)
                    intent.putExtra("StartActivityWhenLocked", true)
                }

                intent
            }
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
            null
        }
    }

    @JvmStatic
    fun launchAppIntent(context: Context, key: String, skipLock: Boolean): Boolean {
        return launchIntent(context, getIntent(context, key, IntentType.APP, skipLock))
    }

    @JvmStatic
    fun launchActivityIntent(context: Context, key: String, skipLock: Boolean): Boolean {
        return launchIntent(context, getIntent(context, key, IntentType.ACTIVITY, skipLock))
    }

    @JvmStatic
    fun launchShortcutIntent(context: Context, key: String, skipLock: Boolean): Boolean {
        return launchIntent(context, getIntent(context, key, IntentType.SHORTCUT, skipLock))
    }

    @JvmStatic
    fun launchIntent(context: Context, intent: Intent?): Boolean {
        if (intent == null) return false
        val bIntent = Intent(ACTION_PREFIX + "LaunchIntent")
        bIntent.putExtra("intent", intent)
        context.sendBroadcast(bIntent)
        return true
    }

    @JvmStatic
    fun showSidebar(context: Context, bundle: Bundle?): Boolean {
        return try {
            val showIntent = Intent(ACTION_PREFIX + "ShowSideBar")
            showIntent.setPackage("com.miui.securitycenter")
            if (bundle != null) {
                showIntent.putExtra("actionInfo", bundle)
            }
            context.sendBroadcast(showIntent)
            true
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
            false
        }
    }

    @JvmStatic
    fun commonSendAction(context: Context, action: String): Boolean {
        return try {
            context.sendBroadcast(Intent(ACTION_PREFIX + action))
            true
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
            false
        }
    }

    @JvmStatic
    fun toggleThis(context: Context, what: Int): Boolean {
        return try {
            val whatStr = when (what) {
                1 -> "WiFi"
                2 -> "Bluetooth"
                3 -> "GPS"
                4 -> "NFC"
                5 -> "SoundProfile"
                6 -> "AutoBrightness"
                7 -> "AutoRotation"
                8 -> "Flashlight"
                9 -> "MobileData"
                10 -> "Hotspot"
                else -> return false
            }
            context.sendBroadcast(Intent(ACTION_PREFIX + "Toggle" + whatStr))
            true
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
            false
        }
    }

    @JvmStatic
    fun isMediaActionsAllowed(mContext: Context?): Boolean {
        if (mContext == null) return false
        val am = mContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val isMusicActive = am.isMusicActive
        val isMusicActiveRemotely = XposedHelpers.callMethod(am, "isMusicActiveRemotely") as? Boolean ?: false
        var isAllowed = isMusicActive || isMusicActiveRemotely
        if (!isAllowed) {
            val mCurrentTime = currentTimeMillis()
            val mLastPauseTime = Settings.System.getLong(mContext.contentResolver, "last_music_paused_time", mCurrentTime)
            if (mCurrentTime - mLastPauseTime < 10 * 60 * 1000) isAllowed = true
        }
        return isAllowed
    }

    @JvmStatic
    fun sendDownUpKeyEvent(mContext: Context?, keyCode: Int, vibrate: Boolean) {
        if (mContext == null) return
        val am = mContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

        if (vibrate && MainModule.mPrefs.getBoolean("controls_volumemedia_vibrate", true))
            HookUtils.performStrongVibration(mContext, MainModule.mPrefs.getBoolean("controls_volumemedia_vibrate_ignore"))
    }
}
