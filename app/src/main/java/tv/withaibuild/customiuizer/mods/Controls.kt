package tv.withaibuild.customiuizer.mods

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers.findMethodExact
import tv.withaibuild.customiuizer.utils.HookUtils

@Suppress("UNUSED_PARAMETER")
object Controls {

    private var isPowerPressed = false
    private var isPowerLongPressed = false
    private var isVolumePressed = false
    private var isVolumeLongPressed = false
    private var isWaitingForPowerLongPressed = false
    private var isWaitingForVolumeLongPressed = false
    private var wasRaise2WakeEnabled = false
    private var mHandler: Handler? = null
    private var sScreenOnContext: Context? = null
    private var sPowerContext: Context? = null
    private var sPowerManager: PowerManager? = null

    private val mPowerLongPressRunnable = Runnable {
        ModuleHelper.guarded {
            if (isPowerPressed) {
                isPowerLongPressed = true
                val ctx = sPowerContext ?: return@guarded
                val pm = sPowerManager ?: return@guarded

                if (HookUtils.mWakeLock == null) {
                    HookUtils.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "miuizer:flashlight")
                }

                if (!isTorchEnabled(ctx) || HookUtils.mWakeLock?.isHeld == false) {
                    setTorch(ctx, true)
                    if (HookUtils.mWakeLock?.isHeld == false) HookUtils.mWakeLock?.acquire(600000)
                } else {
                    setTorch(ctx, true)
                    if (HookUtils.mWakeLock?.isHeld == true) HookUtils.mWakeLock?.release()
                }
            }
            isPowerPressed = false
            isWaitingForPowerLongPressed = false
        }
    }

    private var sVolumeContext: Context? = null
    private var sVolumeKeyCode = KeyEvent.KEYCODE_UNKNOWN

    private val mVolumeLongPressRunnable = Runnable {
        try {
            ModuleHelper.guarded {
                if (isVolumePressed) {
                    val ctx = sVolumeContext ?: return@guarded
                    if (!GlobalActions.isMediaActionsAllowed(ctx)) return@guarded
                    isVolumeLongPressed = true
                    when (sVolumeKeyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            val prefMediaUp = MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0)
                            if (prefMediaUp == 0) return@guarded
                            GlobalActions.sendDownUpKeyEvent(ctx, prefMediaUp, true)
                        }
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            val prefMediaDown = MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0)
                            if (prefMediaDown == 0) return@guarded
                            GlobalActions.sendDownUpKeyEvent(ctx, prefMediaDown, true)
                        }
                    }
                }
            }
        } finally {
            isVolumePressed = false
            isWaitingForVolumeLongPressed = false
            sVolumeKeyCode = KeyEvent.KEYCODE_UNKNOWN
        }
    }

    private fun isTorchEnabled(mContext: Context): Boolean {
        return Settings.Global.getInt(mContext.contentResolver, "torch_state", 0) != 0
    }

    private fun setTorch(context: Context, state: Boolean) {
        if (state) {
            val wakeup = Settings.System.getInt(context.contentResolver, "pick_up_gesture_wakeup_mode", 0)
            wasRaise2WakeEnabled = wakeup == 1
            if (wasRaise2WakeEnabled) Settings.System.putInt(context.contentResolver, "pick_up_gesture_wakeup_mode", 0)
        }
        val intent = Intent("miui.intent.action.TOGGLE_TORCH")
        intent.putExtra("miui.intent.extra.IS_ENABLE", state)
        context.sendBroadcast(intent)
    }

    private val mScreenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            ModuleHelper.guarded("Controls.screenOnReceiver") {
                if (isTorchEnabled(context)) setTorch(context, false)
                if (HookUtils.mWakeLock != null && HookUtils.mWakeLock?.isHeld == true) HookUtils.mWakeLock?.release()
                if (wasRaise2WakeEnabled) {
                    wasRaise2WakeEnabled = false
                    Settings.System.putInt(context.contentResolver, "pick_up_gesture_wakeup_mode", 1)
                }
            }
        }
    }

    @JvmStatic
    fun PowerKeyHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.policy.PhoneWindowManager", lpparam.classLoader, "init", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                if (sScreenOnContext != null) {
                    try {
                        sScreenOnContext?.unregisterReceiver(mScreenOnReceiver)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) throw t
                    }
                }
                mContext.registerReceiver(mScreenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON), Context.RECEIVER_NOT_EXPORTED)
                sScreenOnContext = mContext
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.policy.MiuiPhoneWindowManager", lpparam.classLoader, "interceptKeyBeforeQueueing", KeyEvent::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                // Power and volkeys are pressed at the same time
                if (isVolumePressed) return
                val keyEvent = param.getArg(0) as? KeyEvent ?: return

                val keycode = keyEvent.keyCode
                val action = keyEvent.action
                val flags = keyEvent.flags

                // Ignore repeated KeyEvents simulated on Power Key Up
                if ((flags and KeyEvent.FLAG_VIRTUAL_HARD_KEY) == KeyEvent.FLAG_VIRTUAL_HARD_KEY) return
                if ((flags and KeyEvent.FLAG_FROM_SYSTEM) != KeyEvent.FLAG_FROM_SYSTEM || keycode != KeyEvent.KEYCODE_POWER) return

                // Power long press
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mPowerManager = XposedHelpers.getObjectField(param.getThisObject(), "mPowerManager") as? PowerManager ?: return
                if (mPowerManager.isInteractive) return

                if (action == KeyEvent.ACTION_DOWN) {
                    isPowerPressed = true
                    isPowerLongPressed = false

                    mHandler = XposedHelpers.getObjectField(param.getThisObject(), "mHandler") as? Handler
                    sPowerContext = mContext
                    sPowerManager = mPowerManager

                    val longPressDelay = (if (MainModule.mPrefs.getBoolean("controls_powerflash_delay")) ViewConfiguration.getLongPressTimeout() * 3 else ViewConfiguration.getLongPressTimeout()) + 500
                    // Post only one delayed runnable that waits for long press timeout
                    if (!isWaitingForPowerLongPressed) {
                        mHandler?.removeCallbacks(mPowerLongPressRunnable)
                        mHandler?.postDelayed(mPowerLongPressRunnable, longPressDelay.toLong())
                    }

                    isWaitingForPowerLongPressed = true
                    param.returnAndSkip(0)
                }

                if (action == KeyEvent.ACTION_UP) {
                    mHandler?.removeCallbacks(mPowerLongPressRunnable)
                    if (isPowerPressed && !isPowerLongPressed) try {
                        if (isTorchEnabled(mContext)) setTorch(mContext, false)
                        if (HookUtils.mWakeLock != null && HookUtils.mWakeLock?.isHeld == true) HookUtils.mWakeLock?.release()
                        XposedHelpers.callMethod(mPowerManager, "wakeUp", SystemClock.uptimeMillis())
                        param.returnAndSkip(0)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) throw t
                        XposedHelpers.log(t)
                    } else if (wasRaise2WakeEnabled && !isTorchEnabled(mContext)) {
                        wasRaise2WakeEnabled = false
                        Settings.System.putInt(mContext.contentResolver, "pick_up_gesture_wakeup_mode", 1)
                    }
                    isPowerPressed = false
                    isWaitingForPowerLongPressed = false
                }
            }
        })
    }

    @JvmStatic
    fun VolumeMediaButtonsHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.policy.MiuiPhoneWindowManager", lpparam.classLoader, "interceptKeyBeforeQueueing", KeyEvent::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            @SuppressLint("MissingPermission")
            override fun before(param: BeforeHookCallback) {
                // Power and volkeys are pressed at the same time
                if (isPowerPressed) return
                val keyEvent = param.getArg(0) as? KeyEvent ?: return

                val keycode = keyEvent.keyCode
                val action = keyEvent.action
                val flags = keyEvent.flags

                // Ignore repeated KeyEvents simulated on volume Key Up
                if ((flags and KeyEvent.FLAG_VIRTUAL_HARD_KEY) == KeyEvent.FLAG_VIRTUAL_HARD_KEY) return
                if ((flags and KeyEvent.FLAG_FROM_SYSTEM) != KeyEvent.FLAG_FROM_SYSTEM || (keycode != KeyEvent.KEYCODE_VOLUME_UP && keycode != KeyEvent.KEYCODE_VOLUME_DOWN)) return

                // Volume long press
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mPowerManager = XposedHelpers.getObjectField(param.getThisObject(), "mPowerManager") as? PowerManager ?: return
                if (mPowerManager.isInteractive) return

                if (action == KeyEvent.ACTION_DOWN) {
                    isVolumePressed = true
                    isVolumeLongPressed = false

                    mHandler = XposedHelpers.getObjectField(param.getThisObject(), "mHandler") as? Handler
                    sVolumeContext = mContext
                    sVolumeKeyCode = keycode

                    // Post only one delayed runnable that waits for long press timeout
                    if (mHandler != null && !isWaitingForVolumeLongPressed) {
                        mHandler?.removeCallbacks(mVolumeLongPressRunnable)
                        mHandler?.postDelayed(mVolumeLongPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                    }

                    isWaitingForVolumeLongPressed = true
                    param.returnAndSkip(0)
                }

                if (action == KeyEvent.ACTION_UP) {
                    isVolumePressed = false
                    mHandler?.removeCallbacks(mVolumeLongPressRunnable)
                    if (!isVolumeLongPressed) {
                        val am = mContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                        val tm = mContext.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
                        val mBroadcastWakeLock = XposedHelpers.getObjectField(param.getThisObject(), "mBroadcastWakeLock") as? PowerManager.WakeLock
                        var k = AudioManager.ADJUST_RAISE
                        if (keycode != KeyEvent.KEYCODE_VOLUME_UP) k = AudioManager.ADJUST_LOWER
                        mBroadcastWakeLock?.acquire(5000)
                        // If music stream is playing, adjust its volume
                        if (am.isMusicActive) am.adjustStreamVolume(AudioManager.STREAM_MUSIC, k, 0)
                        // If voice call is active while screen off by proximity sensor, adjust its volume
                        else if (tm.isInCall) am.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, k, 0)
                        // If volume keys to wake option active, wake the device
                        else if (Settings.System.getInt(mContext.contentResolver, "volumekey_wake_screen", 0) == 1)
                            XposedHelpers.callMethod(mPowerManager, "wakeUp", SystemClock.uptimeMillis())
                        if (mBroadcastWakeLock?.isHeld == true) mBroadcastWakeLock.release()
                    }
                    param.returnAndSkip(0)
                    isWaitingForVolumeLongPressed = false
                    sVolumeKeyCode = KeyEvent.KEYCODE_UNKNOWN
                }
            }
        })
    }

    @JvmStatic
    fun VolumeMediaPlayerHook(lpparam: PackageReadyParam) {
        val mediaPlayerCls = XposedHelpers.findClass("android.media.MediaPlayer", lpparam.classLoader)
        val getAudioStreamType = mediaPlayerCls.getDeclaredMethod("getAudioStreamType").apply {
            isAccessible = true
        }
        ModuleHelper.findAndHookMethod(mediaPlayerCls, "pause", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = ModuleHelper.findContext(lpparam) as? Context ?: return
                val mStreamType = (getAudioStreamType.invoke(param.getThisObject()) as? Number)?.toInt() ?: 0
                if (mStreamType == AudioManager.STREAM_MUSIC || mStreamType == 0x80000000.toInt()) {
                    val intent = Intent(GlobalActions.ACTION_PREFIX + "SaveLastMusicPausedTime")
                    intent.setPackage("android")
                    mContext.sendBroadcast(intent)
                }
            }
        })
    }

    @JvmStatic
    fun VolumeCursorHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("android.inputmethodservice.InputMethodService", lpparam.classLoader, "onKeyDown", Int::class.javaPrimitiveType, KeyEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val ims = param.getThisObject() as? InputMethodService ?: return
                val code = param.getArg(0) as? Int ?: return
                if ((code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN) && ims.isInputViewShown) {
                    val pkgName = Settings.Global.getString(ims.contentResolver, HookUtils.modulePkg + ".foreground.package")
                    val appsSet = MainModule.mPrefs.getStringSet("controls_volumecursor_apps")
                    if (pkgName != null && appsSet.contains(pkgName)) return
                    val swapDir = MainModule.mPrefs.getBoolean("controls_volumecursor_reverse")
                    ims.sendDownUpKeyEvents(if (code == (if (swapDir) KeyEvent.KEYCODE_VOLUME_DOWN else KeyEvent.KEYCODE_VOLUME_UP)) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
                    param.returnAndSkip(true)
                }
            }
        })

        ModuleHelper.findAndHookMethod("android.inputmethodservice.InputMethodService", lpparam.classLoader, "onKeyUp", Int::class.javaPrimitiveType, KeyEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val ims = param.getThisObject() as? InputMethodService ?: return
                val code = param.getArg(0) as? Int ?: return
                if ((code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN) && ims.isInputViewShown) {
                    val pkgName = Settings.Global.getString(ims.contentResolver, HookUtils.modulePkg + ".foreground.package")
                    val appsSet = MainModule.mPrefs.getStringSet("controls_volumecursor_apps")
                    if (pkgName == null || !appsSet.contains(pkgName))
                        param.returnAndSkip(true)
                }
            }
        })
    }

    private fun handleNavBarAction(context: Context, key: String): Boolean {
        val action = MainModule.mPrefs.getInt(key + "_action", 1)
        return if (action >= 85 && action <= 88) {
            if (GlobalActions.isMediaActionsAllowed(context)) {
                GlobalActions.sendDownUpKeyEvent(context, action, false)
            }
            true
        } else if (action == 1) {
            try {
                Toast.makeText(ModuleHelper.getModuleContext(context), R.string.controls_navbar_noaction, Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) throw t
            }
            false
        } else {
            GlobalActions.handleAction(context, key)
        }
    }

    private fun reposNavBarButtons(navbar: FrameLayout) {
        val mContext = navbar.context
        val displayRotation = navbar.context.display?.rotation ?: Surface.ROTATION_0
        val density = mContext.resources.displayMetrics.density
        val margin = Math.round(MainModule.mPrefs.getInt("controls_navbarmargin", 0) * density)
        if (displayRotation == Surface.ROTATION_0) {
            val hleft = navbar.findViewWithTag<ImageView>("custom_left_horiz")
            if (hleft != null) {
                val leftbtn = hleft.parent as? LinearLayout
                val lpl = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT)
                lpl.leftMargin += margin
                lpl.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                leftbtn?.layoutParams = lpl
            }

            val hright = navbar.findViewWithTag<ImageView>("custom_right_horiz")
            if (hright != null) {
                val rightbtn = hright.parent as? LinearLayout
                val lpr = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT)
                lpr.rightMargin += margin
                lpr.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                rightbtn?.layoutParams = lpr
            }
        } else {
            val vleft = navbar.findViewWithTag<ImageView>("custom_left_vert")
            val vright = navbar.findViewWithTag<ImageView>("custom_right_vert")

            val leftbtn = if (vleft != null) vleft.parent as? LinearLayout else null
            val lpl = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)

            val rightbtn = if (vright != null) vright.parent as? LinearLayout else null
            val lpr = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            if (displayRotation == Surface.ROTATION_270) {
                lpl.topMargin += margin
                lpl.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

                lpr.bottomMargin += margin
                lpr.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            } else if (displayRotation == Surface.ROTATION_90) {
                lpr.topMargin += margin
                lpr.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

                lpl.bottomMargin += margin
                lpl.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            if (leftbtn != null) leftbtn.layoutParams = lpl
            if (rightbtn != null) rightbtn.layoutParams = lpr
        }
    }

    private fun addCustomNavBarKeys(isVertical: Boolean, mContext: Context, navButtons: FrameLayout, kbrCls: Class<*>?) {
        val dot1: Drawable
        val dot2: Drawable
        try {
            val modCtx = ModuleHelper.getModuleContext(mContext)
            val modRes = ModuleHelper.getModuleRes(mContext)
            dot1 = modRes.getDrawable(R.drawable.ic_sysbar_dot_bottomleft, modCtx.theme)
            dot2 = modRes.getDrawable(R.drawable.ic_sysbar_dot_topright, modCtx.theme)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
            return
        }

        val leftbtn = LinearLayout(mContext)
        val left = ImageView(mContext)

        val lplc: LinearLayout.LayoutParams
        if (isVertical)
            lplc = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        else
            lplc = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
        left.layoutParams = lplc
        left.setImageDrawable(dot1)
        left.alpha = 0.9f
        left.tag = "custom_left" + if (isVertical) "_vert" else "_horiz"
        if (kbrCls != null) try {
            val lripple = kbrCls.getConstructor(Context::class.java, View::class.java).newInstance(mContext, leftbtn) as? Drawable
            leftbtn.background = lripple
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
        }
        leftbtn.isClickable = true
        leftbtn.isHapticFeedbackEnabled = true
        leftbtn.setOnClickListener {
            ModuleHelper.guarded("Controls.leftNavigationClick") {
                handleNavBarAction(it.context, "controls_navbarleft")
            }
        }
        leftbtn.setOnLongClickListener {
            ModuleHelper.guarded("Controls.leftNavigationLongClick", false) {
                handleNavBarAction(it.context, "controls_navbarleftlong")
            }
        }
        leftbtn.addView(left)

        val rightbtn = LinearLayout(mContext)
        val right = ImageView(mContext)
        val lprc: LinearLayout.LayoutParams
        if (isVertical)
            lprc = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        else
            lprc = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
        right.layoutParams = lprc
        right.setImageDrawable(dot2)
        right.alpha = 0.9f
        right.tag = "custom_right" + if (isVertical) "_vert" else "_horiz"
        if (kbrCls != null) try {
            val rripple = kbrCls.getConstructor(Context::class.java, View::class.java).newInstance(mContext, rightbtn) as? Drawable
            rightbtn.background = rripple
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
        }
        rightbtn.isClickable = true
        rightbtn.isHapticFeedbackEnabled = true
        rightbtn.setOnClickListener {
            ModuleHelper.guarded("Controls.rightNavigationClick") {
                handleNavBarAction(it.context, "controls_navbarright")
            }
        }
        rightbtn.setOnLongClickListener {
            ModuleHelper.guarded("Controls.rightNavigationLongClick", false) {
                handleNavBarAction(it.context, "controls_navbarrightlong")
            }
        }
        rightbtn.addView(right)

        val hasLeftAction = MainModule.mPrefs.getInt("controls_navbarleft_action", 1) > 1 || MainModule.mPrefs.getInt("controls_navbarleftlong_action", 1) > 1
        val hasRightAction = MainModule.mPrefs.getInt("controls_navbarright_action", 1) > 1 || MainModule.mPrefs.getInt("controls_navbarrightlong_action", 1) > 1

        if (isVertical) {
            if (hasRightAction) {
                navButtons.addView(rightbtn, 0)
            }
            if (hasLeftAction) {
                navButtons.addView(leftbtn, navButtons.childCount)
            }
        } else {
            if (hasLeftAction) {
                navButtons.addView(leftbtn, 0)
            }
            if (hasRightAction) {
                navButtons.addView(rightbtn, navButtons.childCount)
            }
        }
    }

    @JvmStatic
    fun NavBarButtonsHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.navigationbar.NavigationBarView", lpparam.classLoader, "updateOrientationViews", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val navBar = param.getThisObject() as? FrameLayout ?: return
                val mContext = navBar.context
                val mHorizontal = XposedHelpers.getObjectField(param.getThisObject(), "mHorizontal") as? ViewGroup ?: return
                val mVertical = XposedHelpers.getObjectField(param.getThisObject(), "mVertical") as? ViewGroup ?: return
                val navButtonsId = navBar.resources.getIdentifier("nav_buttons", "id", lpparam.packageName)
                val navButtons0 = mHorizontal.findViewById<FrameLayout>(navButtonsId)
                val navButtons90 = mVertical.findViewById<FrameLayout>(navButtonsId)

                val kbrCls = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.MiuiKeyButtonRipple", lpparam.classLoader)
                addCustomNavBarKeys(false, mContext, navButtons0, kbrCls)
                addCustomNavBarKeys(true, mContext, navButtons90, kbrCls)
                reposNavBarButtons(navBar)
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.navigationbar.NavigationBarTransitions", lpparam.classLoader, "applyDarkIntensity", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val navbar = XposedHelpers.getObjectField(param.getThisObject(), "mView") as? FrameLayout ?: return
                val isDark = (param.getArg(0) as? Float ?: 0f) > 0.5f
                val hleft = navbar.findViewWithTag<ImageView>("custom_left_horiz")
                val vleft = navbar.findViewWithTag<ImageView>("custom_left_vert")
                val hright = navbar.findViewWithTag<ImageView>("custom_right_horiz")
                val vright = navbar.findViewWithTag<ImageView>("custom_right_vert")

                val modCtx = ModuleHelper.getModuleContext(navbar.context)
                val modRes = ModuleHelper.getModuleRes(navbar.context)
                if (isDark) {
                    val darkImg1 = modRes.getDrawable(R.drawable.ic_sysbar_dot_bottomleft_dark, modCtx.theme)
                    val darkImg2 = modRes.getDrawable(R.drawable.ic_sysbar_dot_topright_dark, modCtx.theme)
                    if (hleft != null) hleft.setImageDrawable(darkImg1)
                    if (vleft != null) vleft.setImageDrawable(darkImg1)
                    if (hright != null) hright.setImageDrawable(darkImg2)
                    if (vright != null) vright.setImageDrawable(darkImg2)
                } else {
                    val lightImg1 = modRes.getDrawable(R.drawable.ic_sysbar_dot_bottomleft, modCtx.theme)
                    val lightImg2 = modRes.getDrawable(R.drawable.ic_sysbar_dot_topright, modCtx.theme)
                    if (hleft != null) hleft.setImageDrawable(lightImg1)
                    if (vleft != null) vleft.setImageDrawable(lightImg1)
                    if (hright != null) hright.setImageDrawable(lightImg2)
                    if (vright != null) vright.setImageDrawable(lightImg2)
                }
            }
        })
        ModuleHelper.hookAllMethods("com.android.systemui.navigationbar.NavigationBarView", lpparam.classLoader, "onConfigurationChanged", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val navbar = param.getThisObject() as? FrameLayout ?: return
                reposNavBarButtons(navbar)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun handleCallAction(action: Int): Boolean {
        val ctx = miuiPWMContext ?: return false
        val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        if (!tm.isInCall) return false
        val callState = (XposedHelpers.callMethod(tm, "getCallState") as? Int) ?: 0
        if (callState == TelephonyManager.CALL_STATE_RINGING) {
            val accept = MainModule.mPrefs.getStringAsInt("controls_fingerprint_accept", 1)
            val reject = MainModule.mPrefs.getStringAsInt("controls_fingerprint_reject", 1)
            return when (action) {
                accept -> {
                    XposedHelpers.callMethod(tm, "acceptRingingCall")
                    true
                }
                reject -> {
                    XposedHelpers.callMethod(tm, "endCall")
                    true
                }
                else -> false
            }
        } else if (callState == TelephonyManager.CALL_STATE_OFFHOOK) {
            val hangup = MainModule.mPrefs.getStringAsInt("controls_fingerprint_hangup", 1)
            if (action == hangup) {
                XposedHelpers.callMethod(tm, "endCall")
                return true
            }
        }
        return MainModule.mPrefs.getBoolean("controls_fingerprintskip2")
    }

    @SuppressLint("StaticFieldLeak")
    private var miuiPWMContext: Context? = null
    private var miuiPWMHandler: Handler? = null
    private var hasDoubleTap = false
    private var wasScreenOn = false
    private var wasFingerprintUsed = false
    private var isFingerprintPressed = false
    private var isFingerprintLongPressed = false
    private var isFingerprintLongPressHandled = false

    private val singlePressFingerprint = Runnable {
        ModuleHelper.guarded {
            val ctx = miuiPWMContext ?: return@guarded
            val hdl = miuiPWMHandler ?: return@guarded
            hdl.removeCallbacks(longPressFingerprint)
            if (!handleCallAction(2)) GlobalActions.handleAction(ctx, "controls_fingerprint1")
        }
    }

    private val longPressFingerprint = Runnable {
        ModuleHelper.guarded {
            if (isFingerprintPressed) {
                val ctx = miuiPWMContext ?: return@guarded
                isFingerprintLongPressed = true
                isFingerprintLongPressHandled = handleCallAction(4)
                HookUtils.performStrongVibration(ctx, true)
            }
        }
    }

    @JvmStatic
    fun FingerprintEventsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.server.policy.MiuiPhoneWindowManager", lpparam.classLoader, "processFingerprintNavigationEvent", KeyEvent::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            @SuppressLint("MissingPermission")
            override fun before(param: BeforeHookCallback) {
                if (MainModule.mPrefs.getBoolean("controls_fingerprintskip")) {
                    val mFocusedWindow = XposedHelpers.getObjectField(param.getThisObject(), "mFocusedWindow")
                    if ((param.getArg(1) as? Boolean ?: false) && mFocusedWindow != null) {
                        val ownPkg = XposedHelpers.callMethod(mFocusedWindow, "getOwningPackage") as? String
                        if ("com.android.camera" == ownPkg) return
                    }
                }

                miuiPWMContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context
                miuiPWMHandler = XposedHelpers.getObjectField(param.getThisObject(), "mHandler") as? Handler
                val ctx = miuiPWMContext ?: return
                val hdl = miuiPWMHandler ?: return

                val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val isInCall = tm?.isInCall ?: false

                val keyEvent = param.getArg(0) as? KeyEvent ?: return
                if (keyEvent.keyCode != KeyEvent.KEYCODE_DPAD_CENTER || keyEvent.action != KeyEvent.ACTION_DOWN) return

                isFingerprintPressed = true
                wasScreenOn = XposedHelpers.callMethod(param.getThisObject(), "isScreenOnInternal") as? Boolean ?: false
                wasFingerprintUsed = Settings.System.getInt(ctx.contentResolver, "is_fingerprint_active", 0) == 1

                hasDoubleTap = false
                if (wasScreenOn && !wasFingerprintUsed) {
                    val delay = MainModule.mPrefs.getInt("controls_fingerprintlong_delay", 0)
                    if (isInCall) {
                        val accept = MainModule.mPrefs.getStringAsInt("controls_fingerprint_accept", 1)
                        val reject = MainModule.mPrefs.getStringAsInt("controls_fingerprint_reject", 1)
                        val hangup = MainModule.mPrefs.getStringAsInt("controls_fingerprint_hangup", 1)
                        hasDoubleTap = accept == 3 || reject == 3 || hangup == 3
                        if (accept == 4 || reject == 4 || hangup == 4) {
                            hdl.postDelayed(longPressFingerprint, (if (delay < 200) ViewConfiguration.getLongPressTimeout() else delay).toLong())
                        }
                    } else {
                        val dtaction = MainModule.mPrefs.getInt("controls_fingerprint2_action", 1)
                        hasDoubleTap = dtaction > 1
                        if (MainModule.mPrefs.getInt("controls_fingerprintlong_action", 1) > 1) {
                            hdl.postDelayed(longPressFingerprint, (if (delay < 200) ViewConfiguration.getLongPressTimeout() else delay).toLong())
                        }
                    }
                }

                if (XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "touchTime") == null) {
                    XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "touchTime", 0L)
                }
            }

            @SuppressLint("MissingPermission")
            override fun after(param: AfterHookCallback) {
                if (MainModule.mPrefs.getBoolean("controls_fingerprintskip")) {
                    val mFocusedWindow = XposedHelpers.getObjectField(param.getThisObject(), "mFocusedWindow")
                    if ((param.getArg(1) as? Boolean ?: false) && mFocusedWindow != null) {
                        val ownPkg = XposedHelpers.callMethod(mFocusedWindow, "getOwningPackage") as? String
                        if ("com.android.camera" == ownPkg) return
                    }
                }

                val keyEvent = param.getArg(0) as? KeyEvent ?: return
                if (keyEvent.keyCode != KeyEvent.KEYCODE_DPAD_CENTER || keyEvent.action != KeyEvent.ACTION_UP) return

                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mHandler = XposedHelpers.getObjectField(param.getThisObject(), "mHandler") as? Handler ?: return

                val lastTouchTime = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "touchTime") as? Long ?: 0L
                val currentTouchTime = java.lang.System.currentTimeMillis()
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "touchTime", currentTouchTime)

                val delay = MainModule.mPrefs.getInt("controls_fingerprint2_delay", 50)
                val dtTimeout = if (delay < 200) ViewConfiguration.getDoubleTapTimeout() else delay
                if (wasScreenOn && !wasFingerprintUsed) {
                    if (hasDoubleTap && currentTouchTime - lastTouchTime < dtTimeout) {
                        mHandler.removeCallbacks(singlePressFingerprint)
                        mHandler.removeCallbacks(longPressFingerprint)
                        if (!handleCallAction(3)) GlobalActions.handleAction(mContext, "controls_fingerprint2")
                        wasScreenOn = false
                    } else if (isFingerprintLongPressed) {
                        if (!isFingerprintLongPressHandled) GlobalActions.handleAction(mContext, "controls_fingerprintlong")
                        isFingerprintLongPressHandled = false
                        wasScreenOn = false
                    } else {
                        mHandler.removeCallbacks(longPressFingerprint)
                        mHandler.removeCallbacks(singlePressFingerprint)
                        if (hasDoubleTap)
                            mHandler.postDelayed(singlePressFingerprint, dtTimeout.toLong())
                        else
                            mHandler.post(singlePressFingerprint)
                    }
                }

                isFingerprintLongPressed = false
                isFingerprintPressed = false
            }
        })

        val fpService = "com.android.server.biometrics.BiometricServiceBase"
        ModuleHelper.hookAllMethods(fpService, lpparam.classLoader, "startClient", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                Settings.System.putInt(mContext.contentResolver, "is_fingerprint_active", 1)
            }
        })

        ModuleHelper.hookAllMethods(fpService, lpparam.classLoader, "removeClient", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                Settings.System.putInt(mContext.contentResolver, "is_fingerprint_active", 0)
            }
        })
    }

    @SuppressLint("StaticFieldLeak")
    private var basePWMContext: Context? = null
    private var basePWMObject: Any? = null
    private var markShortcutTriggered: java.lang.reflect.Method? = null

    private val mBackLongPressAction = Runnable {
        try {
            val ctx = basePWMContext ?: return@Runnable
            val obj = basePWMObject ?: return@Runnable
            if (GlobalActions.handleAction(ctx, "controls_backlong")) HookUtils.performStrongVibration(ctx)
            if (MainModule.mPrefs.getInt("controls_backlong_action", 1) != 1) markShortcutTriggered?.invoke(obj)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
        }
    }

    private val mHomeLongPressAction = Runnable {
        try {
            val ctx = basePWMContext ?: return@Runnable
            val obj = basePWMObject ?: return@Runnable
            if (GlobalActions.handleAction(ctx, "controls_homelong")) HookUtils.performStrongVibration(ctx)
            if (MainModule.mPrefs.getInt("controls_homelong_action", 1) != 1) markShortcutTriggered?.invoke(obj)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
        }
    }

    private val mMenuLongPressAction = Runnable {
        try {
            val ctx = basePWMContext ?: return@Runnable
            val obj = basePWMObject ?: return@Runnable
            if (GlobalActions.handleAction(ctx, "controls_menulong")) HookUtils.performStrongVibration(ctx)
            if (MainModule.mPrefs.getInt("controls_menulong_action", 1) != 1) markShortcutTriggered?.invoke(obj)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
        }
    }

    @JvmStatic
    fun NavBarActionsHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.policy.BaseMiuiPhoneWindowManager", lpparam.classLoader, "postKeyLongPress", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (basePWMObject == null) basePWMObject = param.getThisObject()
                if (basePWMContext == null) basePWMContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context
                if (markShortcutTriggered == null) markShortcutTriggered = XposedHelpers.findMethodExact("com.android.server.policy.BaseMiuiPhoneWindowManager", lpparam.classLoader, "markShortcutTriggered")

                val key = param.getArg(0) as? Int ?: return
                val mHandler = XposedHelpers.getObjectField(param.getThisObject(), "mHandler") as? Handler ?: return
                if (key == KeyEvent.KEYCODE_BACK && MainModule.mPrefs.getInt("controls_backlong_action", 1) > 1) {
                    mHandler.postDelayed(mBackLongPressAction, ViewConfiguration.getLongPressTimeout().toLong())
                    param.returnAndSkip(null)
                } else if (key == KeyEvent.KEYCODE_HOME && MainModule.mPrefs.getInt("controls_homelong_action", 1) > 1) {
                    mHandler.postDelayed(mHomeLongPressAction, ViewConfiguration.getLongPressTimeout().toLong())
                    param.returnAndSkip(null)
                } else if (key == KeyEvent.KEYCODE_APP_SWITCH && MainModule.mPrefs.getInt("controls_menulong_action", 1) > 1) {
                    mHandler.postDelayed(mMenuLongPressAction, ViewConfiguration.getLongPressTimeout().toLong())
                    param.returnAndSkip(null)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.server.policy.BaseMiuiPhoneWindowManager", lpparam.classLoader, "removeKeyLongPress", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val key = param.getArg(0) as? Int ?: return
                val mHandler = XposedHelpers.getObjectField(param.getThisObject(), "mHandler") as? Handler ?: return
                when (key) {
                    KeyEvent.KEYCODE_BACK -> mHandler.removeCallbacks(mBackLongPressAction)
                    KeyEvent.KEYCODE_HOME -> mHandler.removeCallbacks(mHomeLongPressAction)
                    KeyEvent.KEYCODE_APP_SWITCH -> mHandler.removeCallbacks(mMenuLongPressAction)
                }
            }
        })
    }

    @JvmStatic
    fun NavbarHeightRes() {
        val opt = MainModule.mPrefs.getInt("controls_navbarheight", 19)
        val heightDpi = if (opt == 19) 47 else opt
        MainModule.getResHooks().setDensityReplacement("*", "dimen", "navigation_bar_height", heightDpi.toFloat())
        MainModule.getResHooks().setDensityReplacement("*", "dimen", "navigation_bar_height_landscape", heightDpi.toFloat())
        MainModule.getResHooks().setDensityReplacement("*", "dimen", "navigation_bar_frame_height", heightDpi.toFloat())
        MainModule.getResHooks().setDensityReplacement("*", "dimen", "navigation_bar_frame_height_landscape", heightDpi.toFloat())
        MainModule.getResHooks().setDensityReplacement("*", "dimen", "navigation_bar_gesture_height", heightDpi.toFloat())
        MainModule.getResHooks().setDensityReplacement("*", "dimen", "navigation_bar_width", heightDpi.toFloat())
        MainModule.getResHooks().setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_size", heightDpi.toFloat())
    }

    @JvmStatic
    fun FingerprintHapticSuccessHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.biometrics.sensors.AuthenticationClient", lpparam.classLoader, "onAuthenticated", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mAuthSuccess = XposedHelpers.getBooleanField(param.getThisObject(), "mAuthSuccess")
                if (!mAuthSuccess) return
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return

                val ignoreSystem = MainModule.mPrefs.getBoolean("controls_fingerprintsuccess_ignore")
                val opt = MainModule.mPrefs.getString("controls_fingerprintsuccess", "1").toIntOrNull() ?: 1
                when (opt) {
                    2 -> HookUtils.performLightVibration(mContext, ignoreSystem)
                    3 -> HookUtils.performStrongVibration(mContext, ignoreSystem)
                }
            }
        })
    }

    @JvmStatic
    fun FingerprintHapticFailureHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.biometrics.sensors.AcquisitionClient", lpparam.classLoader, "vibrateError", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun FingerprintScreenOnHook(lpparam: SystemServerStartingParam) {
        val authClient = "com.android.server.biometrics.sensors.AuthenticationClient"
        ModuleHelper.hookAllMethods(authClient, lpparam.classLoader, "onAuthenticated", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mAuthSuccess = XposedHelpers.getBooleanField(param.getThisObject(), "mAuthSuccess")
                if (mAuthSuccess) return
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mPowerManager = mContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
                if (mPowerManager.isInteractive) return
                if (!GlobalActions.commonSendAction(mContext, "WakeUp")) XposedHelpers.log("FingerprintScreenOnHook", "Failed to wake up device")
            }
        })
    }

    @JvmStatic
    fun BackGestureAreaHeightHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethodSilently("com.miui.home.recents.GestureStubView", lpparam.classLoader, "getGestureStubWindowParam", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val lp = param.getResult() as? WindowManager.LayoutParams ?: return
                val pct = MainModule.mPrefs.getInt("controls_fsg_coverage", 60)
                lp.height = Math.round(lp.height / 60.0f * pct)
                param.setResult(lp)
            }
        })
    }

    @JvmStatic
    fun BackGestureAreaWidthHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethodSilently("com.miui.home.recents.GestureStubView", lpparam.classLoader, "initScreenSizeAndDensity", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val pct = MainModule.mPrefs.getInt("controls_fsg_width", 100)
                if (pct == 100) return
                var mGestureStubDefaultSize = XposedHelpers.getIntField(param.getThisObject(), "mGestureStubDefaultSize")
                var mGestureStubSize = XposedHelpers.getIntField(param.getThisObject(), "mGestureStubSize")
                mGestureStubDefaultSize = Math.round(mGestureStubDefaultSize * pct / 100f)
                mGestureStubSize = Math.round(mGestureStubSize * pct / 100f)
                XposedHelpers.setIntField(param.getThisObject(), "mGestureStubDefaultSize", mGestureStubDefaultSize)
                XposedHelpers.setIntField(param.getThisObject(), "mGestureStubSize", mGestureStubSize)
            }
        })

        ModuleHelper.findAndHookMethodSilently("com.miui.home.recents.GestureStubView", lpparam.classLoader, "setSize", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pct = MainModule.mPrefs.getInt("controls_fsg_width", 100)
                if (pct == 100) return
                val mGestureStubDefaultSize = XposedHelpers.getIntField(param.getThisObject(), "mGestureStubDefaultSize")
                val arg = param.getArg(0) as? Int ?: return
                if (arg == mGestureStubDefaultSize) return
                param.getArgs()[0] = Math.round(arg * pct / 100f)
            }
        })
    }

    @JvmStatic
    fun HideNavBarHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.NavigationModeControllerExt", lpparam.classLoader, "hideNavigationBar", HookerClassHelper.returnConstant(true))
        ModuleHelper.hookAllMethods("com.android.systemui.navigationbar.NavigationBarController", lpparam.classLoader, "createNavigationBar", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.getArgsCount() >= 3) {
                    param.returnAndSkip(null)
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiDockIndicatorService", lpparam.classLoader, "onNavigationModeChanged", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                XposedHelpers.setObjectField(param.getThisObject(), "mNavMode", param.getArg(0))
                if (XposedHelpers.getObjectField(param.getThisObject(), "mNavigationBarView") != null) {
                    XposedHelpers.callMethod(param.getThisObject(), "setNavigationBarView", null)
                } else {
                    XposedHelpers.callMethod(param.getThisObject(), "checkAndApplyNavigationMode")
                }
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun ImeBackAltIconHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader, "setImeWindowStatus", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mNavigationBarView = XposedHelpers.getObjectField(param.getThisObject(), "mNavigationBarView")
                if (mNavigationBarView != null) XposedHelpers.callMethod(mNavigationBarView, "setNavigationIconHints", param.getArg(1), false)
            }
        })
    }

    @JvmStatic
    fun PowerDoubleTapActionHook(lpparam: SystemServerStartingParam) {
        val dtFromVolumeDown = MainModule.mPrefs.getBoolean("controls_volumedowndt_torch")
        ModuleHelper.findAndHookMethod("com.miui.server.input.util.ShortCutActionsUtils", lpparam.classLoader, "triggerFunction", String::class.java, String::class.java, Bundle::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val arg1 = param.getArg(1) as? String
                if (dtFromVolumeDown && arg1 == "double_click_volume_down") {
                    param.getArgs()[0] = "turn_on_torch"
                } else if (!dtFromVolumeDown && isPowerDoubleTapReason(arg1)) {
                    val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                    GlobalActions.handleAction(mContext, "controls_powerdt", true)
                    param.returnAndSkip(true)
                }
            }
        })

        if (dtFromVolumeDown) {
            ModuleHelper.findAndHookMethodSilently("com.android.server.policy.MiuiKeyShortcutManager", lpparam.classLoader, "getVolumeKeyLaunchCamera", HookerClassHelper.returnConstant(true))
        }
    }

    internal fun isPowerDoubleTapReason(reason: String?): Boolean {
        return reason == "double_click_power" ||
            reason == "power_double_tap" ||
            reason == "double_click_power_key"
    }

    @JvmStatic
    fun NoFingerprintWakeHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.policy.MiuiPhoneWindowManager", lpparam.classLoader, "processBackFingerprintDpcenterEvent", KeyEvent::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val isScreenOn = param.getArg(1) as? Boolean ?: return
                if (!isScreenOn) param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun AssistGestureActionHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.assist.AssistManager", lpparam.classLoader, "startAssist", Bundle::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val bundle = param.getArg(0) as? Bundle ?: return
                if (bundle.getInt("triggered_by", 0) != 83 || bundle.getInt("invocation_type", 0) != 1) return
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val pos = if (bundle.getInt("inDirection", 0) == 1) "right" else "left"
                if (GlobalActions.handleAction(mContext, "controls_fsg_assist_$pos", false, bundle)) {
                    HookUtils.performLightVibration(mContext)
                    param.returnAndSkip(null)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.assist.ui.DefaultUiController", lpparam.classLoader, "logInvocationProgressMetrics", Int::class.javaPrimitiveType, Float::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, HookerClassHelper.DO_NOTHING)
    }
}
