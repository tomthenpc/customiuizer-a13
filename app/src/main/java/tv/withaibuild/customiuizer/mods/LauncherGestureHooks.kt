package tv.withaibuild.customiuizer.mods

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.ShakeManager
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

@Suppress("UNUSED_PARAMETER")
object LauncherGestureHooks {

    private const val HOTSEAT_GESTURE_STATE_KEY =
        "customiuizer.hotseatGestureState"

    internal class HotSeatGestureState {
        var densityDpi: Int = Int.MIN_VALUE
            private set
        var minDistance: Int = 0
            private set
        var velocityThreshold: Int = 0
            private set
        var touchSlop: Int = 0
            private set

        var downX: Float = 0f
        var downY: Float = 0f
        var downTime: Long = 0L

        fun updateThresholdsIfNeeded(
            newDensityDpi: Int,
            density: Float,
            newTouchSlop: Int
        ): Boolean {
            if (densityDpi == newDensityDpi) return false

            densityDpi = newDensityDpi
            minDistance = Math.round(75f * density)
            velocityThreshold = Math.round(33f * density)
            touchSlop = newTouchSlop
            return true
        }
    }

    private fun hotSeatGestureState(hotSeat: ViewGroup): HotSeatGestureState {
        var state = XposedHelpers.getAdditionalInstanceField(hotSeat, HOTSEAT_GESTURE_STATE_KEY) as? HotSeatGestureState
        if (state == null) {
            state = HotSeatGestureState()
            XposedHelpers.setAdditionalInstanceField(hotSeat, HOTSEAT_GESTURE_STATE_KEY, state)
        }
        return state
    }

    @JvmStatic
    fun HomescreenSwipesHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Workspace", lpparam.classLoader, "onVerticalGesture", Int::class.javaPrimitiveType, MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (XposedHelpers.callMethod(param.getThisObject(), "isInNormalEditingMode") as? Boolean == true) return
                val key: String?
                val helperContext = (param.getThisObject() as? ViewGroup)?.context ?: return
                var numOfFingers = 1
                if (param.getArg(1) != null) numOfFingers = (param.getArg(1) as? MotionEvent)?.pointerCount ?: 1
                when (param.getArg(0) as? Int) {
                    11 -> {
                        key = when (numOfFingers) {
                            1 -> "launcher_swipedown"
                            2 -> "launcher_swipedown2"
                            else -> null
                        }
                        if (key != null && GlobalActions.handleAction(helperContext, key)) param.returnAndSkip(true)
                    }
                    10 -> {
                        key = when (numOfFingers) {
                            1 -> "launcher_swipeup"
                            2 -> "launcher_swipeup2"
                            else -> null
                        }
                        if (key != null && GlobalActions.handleAction(helperContext, key)) param.returnAndSkip(true)
                    }
                }
            }
        })

        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.uioverrides.StatusBarSwipeController", lpparam.classLoader, "canInterceptTouch", MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (MainModule.mPrefs.getInt("launcher_swipedown_action", 1) > 1) param.returnAndSkip(false)
            }
        })

        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.uioverrides.AllAppsSwipeController", lpparam.classLoader, "canInterceptTouch", MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (MainModule.mPrefs.getInt("launcher_swipeup_action", 1) > 1) param.returnAndSkip(false)
            }
        })

        // content_center, global_search, notification_bar
        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.allapps.LauncherMode", lpparam.classLoader, "getPullDownGesture", Context::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (MainModule.mPrefs.getInt("launcher_swipedown_action", 1) > 1) param.setResult("no_action")
            }
        })

        // content_center, global_search
        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.allapps.LauncherMode", lpparam.classLoader, "getSlideUpGesture", Context::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (MainModule.mPrefs.getInt("launcher_swipeup_action", 1) > 1) param.returnAndSkip("no_action")
            }
        })

        if (ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "isGlobalSearchEnable", Context::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (MainModule.mPrefs.getInt("launcher_swipeup_action", 1) > 1) param.returnAndSkip(false)
            }
        })) {
            ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.search.SearchEdgeLayout", lpparam.classLoader, "isTopSearchEnable", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    if (MainModule.mPrefs.getInt("launcher_swipedown_action", 1) > 1) param.returnAndSkip(false)
                }
            })
            ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.search.SearchEdgeLayout", lpparam.classLoader, "isBottomGlobalSearchEnable", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    if (MainModule.mPrefs.getInt("launcher_swipeup_action", 1) > 1) param.returnAndSkip(false)
                }
            })
            ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "isGlobalSearchBottomEffectEnable", Context::class.java, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    if (MainModule.mPrefs.getInt("launcher_swipeup_action", 1) > 1) param.returnAndSkip(false)
                }
            })
        } else if (!ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "allowedSlidingUpToStartGolbalSearch", Context::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (MainModule.mPrefs.getInt("launcher_swipeup_action", 1) > 1) param.returnAndSkip(false)
            }
        })) {
            if (lpparam.packageName == "com.miui.home") XposedHelpers.log("HomescreenSwipesHook", "Cannot disable swipe up search")
        }
    }

    @JvmStatic
    fun HotSeatSwipesHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.overlay.assistant.AssistantOverlaySwipeController", lpparam.classLoader, "canInterceptTouch", MotionEvent::class.java, object : MethodHook() {
            private var mHotHeatTouchRect: android.graphics.Rect? = null
            override fun after(param: AfterHookCallback) {
                val canInterceptTouch = param.getResult() as? Boolean ?: false
                if (canInterceptTouch) {
                    if (mHotHeatTouchRect == null) {
                        val mLauncher = XposedHelpers.getObjectField(param.getThisObject(), "mLauncher")
                        val mHotSeats = XposedHelpers.callMethod(mLauncher, "getHotSeats") as? FrameLayout
                        if (mHotSeats != null) {
                            mHotHeatTouchRect = android.graphics.Rect()
                            mHotSeats.getHitRect(mHotHeatTouchRect)
                        }
                    }
                    val motionEvent = param.getArg(0) as? MotionEvent ?: return
                    val rect = mHotHeatTouchRect ?: return
                    if (rect.contains(motionEvent.x.toInt(), motionEvent.y.toInt())) {
                        param.setResult(false)
                    }
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.hotseats.HotSeats", lpparam.classLoader, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val ev = param.getArg(0) as? MotionEvent ?: return
                val hotSeat = param.getThisObject() as? ViewGroup ?: return
                if (hotSeat.context == null) return
                val state = hotSeatGestureState(hotSeat)

                val resources = hotSeat.context.resources
                val densityDpi = resources.configuration.densityDpi
                val density: Float
                val touchSlop: Int
                if (state.densityDpi != densityDpi) {
                    density = resources.displayMetrics.density
                    touchSlop = ViewConfiguration.get(hotSeat.context).scaledTouchSlop
                } else {
                    density = 0f
                    touchSlop = 0
                }
                state.updateThresholdsIfNeeded(densityDpi, density, touchSlop)

                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        state.downX = ev.x
                        state.downY = ev.y
                        state.downTime = SystemClock.uptimeMillis()
                    }
                    MotionEvent.ACTION_UP -> {
                        val ctx = hotSeat.context
                        val dx = ev.x - state.downX
                        val dy = ev.y - state.downY
                        val dt = SystemClock.uptimeMillis() - state.downTime
                        if (dt == 0L) return
                        val velocity = kotlin.math.abs(dx) * 1000 / dt
                        if (kotlin.math.abs(dy) <= state.touchSlop && velocity > state.velocityThreshold) {
                            if (dx > state.minDistance) {
                                GlobalActions.handleAction(ctx, "launcher_swiperight")
                            } else if (-dx > state.minDistance) {
                                GlobalActions.handleAction(ctx, "launcher_swipeleft")
                            }
                        }
                    }
                }
            }
        })
    }

    @JvmStatic
    fun ShakeHook(lpparam: PackageReadyParam) {
        val shakeMgrKey = "MIUIZER_SHAKE_MGR"

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "onResume", object : MethodHook() {
            @SuppressLint("MissingPermission")
            override fun after(param: AfterHookCallback) {
                var shakeMgr = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey) as? ShakeManager
                if (shakeMgr == null) {
                    shakeMgr = ShakeManager(param.getThisObject() as? Context ?: return)
                    XposedHelpers.setAdditionalInstanceField(param.getThisObject(), shakeMgrKey, shakeMgr)
                }
                val launcherActivity = param.getThisObject() as? android.app.Activity ?: return
                val sensorMgr = launcherActivity.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager ?: return
                shakeMgr.reset()
                sensorMgr.registerListener(shakeMgr, sensorMgr.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER), android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "onPause", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val shakeMgr = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey) as? ShakeManager ?: return
                val launcherActivity = param.getThisObject() as? android.app.Activity ?: return
                val sensorMgr = launcherActivity.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager ?: return
                sensorMgr.unregisterListener(shakeMgr)
            }
        })
    }

    @JvmStatic
    fun FSGesturesHook(lpparam: PackageReadyParam) {
        val baseRecentsClass = XposedHelpers.findClassIfExists(
            "com.miui.home.recents.BaseRecentsImpl",
            lpparam.classLoader
        )

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "usingFsGesture", HookerClassHelper.returnConstant(true))

        if (baseRecentsClass != null) {
            ModuleHelper.findAndHookMethodSilently("com.miui.home.recents.BaseRecentsImpl", lpparam.classLoader, "createAndAddNavStubView", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val fsg = XposedHelpers.getAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR") as? Boolean ?: false
                    if (!fsg) param.returnAndSkip(null)
                }
            })

            ModuleHelper.findAndHookMethodSilently("com.miui.home.recents.BaseRecentsImpl", lpparam.classLoader, "updateFsgWindowState", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val fsg = XposedHelpers.getAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR") as? Boolean ?: false
                    if (fsg) return

                    val mNavStubView = XposedHelpers.getObjectField(param.getThisObject(), "mNavStubView")
                    val mWindowManager = XposedHelpers.getObjectField(param.getThisObject(), "mWindowManager")
                    if (mWindowManager != null && mNavStubView != null) {
                        XposedHelpers.callMethod(mWindowManager, "removeView", mNavStubView)
                        XposedHelpers.setObjectField(param.getThisObject(), "mNavStubView", null)
                    }
                }
            })

            ModuleHelper.findAndHookMethodSilently("com.miui.launcher.utils.MiuiSettingsUtils", lpparam.classLoader, "getGlobalBoolean", android.content.ContentResolver::class.java, String::class.java, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    if (param.getArg(1) != "force_fsg_nav_bar") return

                    for (el in Thread.currentThread().stackTrace) {
                        if (el.className == "com.miui.home.recents.BaseRecentsImpl") {
                            XposedHelpers.setAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR", param.getResult())
                            param.setResult(true)
                            return
                        }
                    }
                }
            })
        }

        ModuleHelper.findAndHookMethod("com.miui.home.recents.GestureStubView", lpparam.classLoader, "onTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val event = param.getArg(0) as? MotionEvent ?: return
                if (event.action != MotionEvent.ACTION_DOWN) return
                val foregroundInfo = miui.process.ProcessManager.getForegroundInfo()
                if (foregroundInfo != null) {
                    val pkgName = foregroundInfo.mForegroundPackageName
                    if (MainModule.mPrefs.getStringSet("controls_fsg_horiz_apps").contains(pkgName)) param.returnAndSkip(false)
                }
            }
        })
    }

    @SuppressLint("StaticFieldLeak")
    private class DoubleTapController(context: Context, private val mActionKey: String) {
        private val MAX_DURATION = 500L
        private var mActionDownRawX = 0f
        private var mActionDownRawY = 0f
        private var mClickCount = 0
        val mContext: Context = context
        private var mFirstClickRawX = 0f
        private var mFirstClickRawY = 0f
        private var mLastClickTime = 0L
        private val mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop * 2

        fun isDoubleTapEvent(motionEvent: MotionEvent): Boolean {
            val action = motionEvent.actionMasked
            return if (action == MotionEvent.ACTION_DOWN) {
                mActionDownRawX = motionEvent.rawX
                mActionDownRawY = motionEvent.rawY
                false
            } else if (action != MotionEvent.ACTION_UP) {
                false
            } else {
                val rawX = motionEvent.rawX
                val rawY = motionEvent.rawY
                if (Math.abs(rawX - mActionDownRawX) <= mTouchSlop.toFloat() && Math.abs(rawY - mActionDownRawY) <= mTouchSlop.toFloat()) {
                    if (SystemClock.elapsedRealtime() - mLastClickTime > MAX_DURATION || rawY - mFirstClickRawY > mTouchSlop.toFloat() || rawX - mFirstClickRawX > mTouchSlop.toFloat()) {
                        mClickCount = 0
                    }
                    mClickCount++
                    if (mClickCount == 1) {
                        mFirstClickRawX = rawX
                        mFirstClickRawY = rawY
                        mLastClickTime = SystemClock.elapsedRealtime()
                        false
                    } else if (Math.abs(rawY - mFirstClickRawY) <= mTouchSlop.toFloat() && Math.abs(rawX - mFirstClickRawX) <= mTouchSlop.toFloat() && SystemClock.elapsedRealtime() - mLastClickTime <= MAX_DURATION) {
                        mClickCount = 0
                        true
                    } else {
                        mClickCount = 0
                        false
                    }
                } else {
                    mClickCount = 0
                    false
                }
            }
        }

        fun onDoubleTapEvent() {
            GlobalActions.handleAction(mContext, mActionKey)
        }
    }

    @JvmStatic
    fun LauncherDoubleTapHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllConstructors("com.miui.home.launcher.Workspace", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArgsCount() != 3) return
                var mDoubleTapControllerEx = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mDoubleTapControllerEx")
                if (mDoubleTapControllerEx != null) return
                mDoubleTapControllerEx = DoubleTapController(param.getArg(0) as? Context ?: return, "launcher_doubletap")
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "mDoubleTapControllerEx", mDoubleTapControllerEx)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Workspace", lpparam.classLoader, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mDoubleTapControllerEx = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mDoubleTapControllerEx") as? DoubleTapController ?: return
                if (!mDoubleTapControllerEx.isDoubleTapEvent(param.getArg(0) as? MotionEvent ?: return)) return
                val mCurrentScreenIndex = XposedHelpers.getIntField(param.getThisObject(), if (lpparam.packageName == "com.miui.home") "mCurrentScreenIndex" else "mCurrentScreen")
                val cellLayout = XposedHelpers.callMethod(param.getThisObject(), "getCellLayout", mCurrentScreenIndex)
                if (XposedHelpers.callMethod(cellLayout, "lastDownOnOccupiedCell") as? Boolean == true) return
                if (XposedHelpers.callMethod(param.getThisObject(), "isInNormalEditingMode") as? Boolean == true) return
                mDoubleTapControllerEx.onDoubleTapEvent()
            }
        })
    }

    @JvmStatic
    fun LauncherPinchHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Workspace", lpparam.classLoader, "onPinching", Float::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val dampingScale = XposedHelpers.callMethod(param.getThisObject(), "getDampingScale", param.getArg(0)) as? Float ?: 0f
                val screenScaleRatio = XposedHelpers.callMethod(param.getThisObject(), "getScreenScaleRatio") as? Float ?: 0f
                if (dampingScale < screenScaleRatio)
                    if (MainModule.mPrefs.getInt("launcher_pinch_action", 1) > 1) param.returnAndSkip(false)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Workspace", lpparam.classLoader, "onPinchingEnd", Float::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val dampingScale = XposedHelpers.callMethod(param.getThisObject(), "getDampingScale", param.getArg(0)) as? Float ?: 0f
                val screenScaleRatio = XposedHelpers.callMethod(param.getThisObject(), "getScreenScaleRatio") as? Float ?: 0f
                if (dampingScale < screenScaleRatio)
                    if (GlobalActions.handleAction((param.getThisObject() as? View)?.context, "launcher_pinch")) {
                        XposedHelpers.callMethod(param.getThisObject(), "finishCurrentGesture")

                        val pinchingStateEnum = XposedHelpers.findClass("com.miui.home.launcher.Workspace\$PinchingState", lpparam.classLoader)
                        val stateFollow = XposedHelpers.getStaticObjectField(pinchingStateEnum, "FOLLOW")
                        val stateReadyToEdit = XposedHelpers.getStaticObjectField(pinchingStateEnum, "READY_TO_EDIT")

                        val mState = XposedHelpers.getObjectField(param.getThisObject(), "mState")
                        XposedHelpers.setObjectField(param.getThisObject(), "mState", stateFollow)
                        if (mState == stateReadyToEdit)
                            XposedHelpers.callMethod(XposedHelpers.getObjectField(param.getThisObject(), "mLauncher"), "changeEditingEntryViewToHotseats")
                        XposedHelpers.callMethod(param.getThisObject(), "resetCellScreenScale", param.getArg(0))

                        param.returnAndSkip(null)
                    }
            }
        })
    }

    @JvmStatic
    fun AssistGestureActionHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.shared.recents.system.AssistManager", lpparam.classLoader, "isSupportGoogleAssist", Int::class.javaPrimitiveType, HookerClassHelper.returnConstant(true))
        val fsGestureHelper = XposedHelpers.findClassIfExists("com.miui.home.recents.FsGestureAssistHelper", lpparam.classLoader)
        if (fsGestureHelper != null) {
            ModuleHelper.findAndHookMethod(fsGestureHelper, "canTriggerAssistantAction", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val isDisabled = XposedHelpers.callStaticMethod(fsGestureHelper, "isAssistantGestureDisabled", param.getArg(2)) as? Boolean ?: true
                    if (!isDisabled) {
                        val mAssistantWidth = XposedHelpers.getIntField(param.getThisObject(), "mAssistantWidth")
                        val f = param.getArg(0) as? Float ?: 0f
                        val f2 = param.getArg(1) as? Float ?: 0f
                        if (f < mAssistantWidth || f > f2 - mAssistantWidth) {
                            param.returnAndSkip(true)
                            return
                        }
                    }
                    param.returnAndSkip(false)
                }
            })

            val inDirection = intArrayOf(0)

            ModuleHelper.hookAllMethods(fsGestureHelper, "handleTouchEvent", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val motionEvent = param.getArg(0) as? MotionEvent ?: return
                    if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                        val mDownX = XposedHelpers.getFloatField(param.getThisObject(), "mDownX")
                        val mAssistantWidth = XposedHelpers.getIntField(param.getThisObject(), "mAssistantWidth")
                        inDirection[0] = if (mDownX < mAssistantWidth) 0 else 1
                    }
                }
            })

            ModuleHelper.findAndHookMethod("com.miui.home.recents.SystemUiProxyWrapper", lpparam.classLoader, "startAssistant", Bundle::class.java, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val bundle = param.getArg(0) as? Bundle ?: return
                    bundle.putInt("inDirection", inDirection[0])
                }
            })
        }
    }

    @JvmStatic
    fun SwipeAndStopActionHook(lpparam: PackageReadyParam) {
        if (MainModule.mPrefs.getBoolean("controls_fsg_swipeandstop_disablevibrate")) {
            val vibratorCls = XposedHelpers.findClassIfExists("android.os.Vibrator", lpparam.classLoader)
            if (vibratorCls != null) {
                ModuleHelper.hookAllMethods("com.miui.home.recents.GestureBackArrowView", lpparam.classLoader, "setReadyFinish", object : MethodHook() {
                    private var vibratorHook: HookerClassHelper.CustomMethodUnhooker? = null
                    override fun before(param: BeforeHookCallback) {
                        vibratorHook = ModuleHelper.findAndHookMethod(vibratorCls, "vibrate", Long::class.javaPrimitiveType, HookerClassHelper.DO_NOTHING)
                    }
                    override fun after(param: AfterHookCallback) {
                        vibratorHook?.unhook()
                    }
                })
            }
        }
        ModuleHelper.findAndHookMethod("com.miui.home.recents.GestureStubView", lpparam.classLoader, "disableQuickSwitch", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.getArgs()[0] = false
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.home.recents.GestureStubView", lpparam.classLoader, "isDisableQuickSwitch", HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("com.miui.home.recents.GestureStubView", lpparam.classLoader, "getNextTask", Context::class.java, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val switchApp = param.getArg(1) as? Boolean ?: false
                if (switchApp) {
                    val mContext = param.getArg(0) as? Context ?: return
                    val bundle = Bundle()
                    bundle.putInt("inDirection", param.getArg(2) as? Int ?: 0)
                    if (GlobalActions.handleAction(mContext, "controls_fsg_swipeandstop", false, bundle)) {
                        val task = XposedHelpers.findClassIfExists("com.android.systemui.shared.recents.model.Task", lpparam.classLoader)
                        param.returnAndSkip(XposedHelpers.newInstance(task))
                        return
                    }
                }
                param.returnAndSkip(null)
            }
        })
    }
}
