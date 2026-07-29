package tv.withaibuild.customiuizer.mods

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Interpolator
import tv.withaibuild.customiuizer.utils.Helpers
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

object SystemDisplayAndWindowHooks {

    @JvmStatic
    fun ScreenAnimHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.display.DisplayPowerController", lpparam.classLoader, "initialize", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                try {
                    XposedHelpers.setObjectField(param.thisObject, "mColorFadeEnabled", true)
                    XposedHelpers.setObjectField(param.thisObject, "mColorFadeFadesConfig", true)
                } catch (ignore: Throwable) {}
            }

            override fun after(param: AfterHookCallback) {
                val mColorFadeOffAnimator = XposedHelpers.getObjectField(param.thisObject, "mColorFadeOffAnimator") as? ObjectAnimator
                if (mColorFadeOffAnimator != null) {
                    var duration = MainModule.mPrefs.getInt("system_screenanim_duration", 0)
                    if (duration == 0) duration = 250
                    mColorFadeOffAnimator.duration = duration.toLong()
                }
                ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
                    override fun onChange(key: String) {
                        if (key.contains("system_screenanim_duration")) {
                            if (mColorFadeOffAnimator == null) return
                            var duration = MainModule.mPrefs.getInt("system_screenanim_duration", 0)
                            if (duration == 0) duration = 250
                            mColorFadeOffAnimator.duration = duration.toLong()
                        }
                    }
                })
            }
        })
    }

    @JvmStatic
    fun NoAccessDeviceLogsRequest(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.logcat.LogcatManagerService", lpparam.classLoader, "onLogAccessRequested", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                XposedHelpers.callMethod(param.thisObject, "declineRequest", param.args[0])
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun NoLightUpOnChargeHook(lpparam: SystemServerStartingParam) {
        val methodName = "wakePowerGroupLocked"
        ModuleHelper.hookAllMethods("com.android.server.power.PowerManagerService", lpparam.classLoader, methodName, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val reason = param.args[3] as? String ?: return
                val option = MainModule.mPrefs.getString("system_nolightuponcharges", "1").toInt()
                if (option == 3 && (reason == "android.server.power:POWER" || reason.startsWith("android.server.power:PLUGGED"))) {
                    param.returnAndSkip(false)
                    return
                }
                if (option == 2 && (
                    reason == "android.server.power:POWER" ||
                    reason.startsWith("android.server.power:PLUGGED") ||
                    reason == "com.android.systemui:RAPID_CHARGE" ||
                    reason == "com.android.systemui:WIRELESS_CHARGE" ||
                    reason == "com.android.systemui:WIRELESS_RAPID_CHARGE"
                )) {
                    param.returnAndSkip(false)
                }
            }
        })
    }

    @JvmStatic
    fun DoubleTapToSleepHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val view = param.thisObject as? android.view.View ?: return
                XposedHelpers.setAdditionalInstanceField(view, "currentTouchTime", 0L)
                XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", 0F)
                XposedHelpers.setAdditionalInstanceField(view, "currentTouchY", 0F)

                view.setOnTouchListener { v, event ->
                    ModuleHelper.guarded(false) {
                        if (event.action != android.view.MotionEvent.ACTION_DOWN) return@guarded false

                        val lastTouchTime = XposedHelpers.getAdditionalInstanceField(view, "currentTouchTime") as? Long ?: 0L
                        val lastTouchX = XposedHelpers.getAdditionalInstanceField(view, "currentTouchX") as? Float ?: 0F
                        val lastTouchY = XposedHelpers.getAdditionalInstanceField(view, "currentTouchY") as? Float ?: 0F

                        var currentTouchTime = java.lang.System.currentTimeMillis()
                        val currentTouchX = event.x
                        val currentTouchY = event.y

                        if (currentTouchTime - lastTouchTime < 250L &&
                            kotlin.math.abs(currentTouchX - lastTouchX) < 100F &&
                            kotlin.math.abs(currentTouchY - lastTouchY) < 100F
                        ) {
                            val keyguardMgr = v.context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                            if (keyguardMgr?.isKeyguardLocked == true) {
                                GlobalActions.commonSendAction(v.context, "GoToSleep")
                            }
                            currentTouchTime = 0L
                        }

                        XposedHelpers.setAdditionalInstanceField(view, "currentTouchTime", currentTouchTime)
                        XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", currentTouchX)
                        XposedHelpers.setAdditionalInstanceField(view, "currentTouchY", currentTouchY)

                        false
                    }
                }
            }
        })
    }

    @JvmStatic
    fun DrawerBlurRatioHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mView = param.args[0] as? android.view.View ?: return
                val mContext = mView.context

                val mControlPanelWindowManager = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader),
                    "get",
                    XposedHelpers.findClass("com.android.systemui.controlcenter.phone.ControlPanelWindowManager", lpparam.classLoader)
                )
                val notificationShadeDepthController = XposedHelpers.getObjectField(param.thisObject, "notificationShadeDepthController")
                val initBlurRatio = MainModule.mPrefs.getInt("system_drawer_blur", 100)
                XposedHelpers.setAdditionalInstanceField(notificationShadeDepthController, "mCustomBlurModifier", initBlurRatio)
                XposedHelpers.setAdditionalInstanceField(mControlPanelWindowManager, "mCustomBlurModifier", initBlurRatio)
                ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
                    override fun onChange(key: String) {
                        if (key.contains("system_drawer_blur")) {
                            val opt = MainModule.mPrefs.getInt("system_drawer_blur", 100)
                            XposedHelpers.setAdditionalInstanceField(notificationShadeDepthController, "mCustomBlurModifier", opt)
                            XposedHelpers.setAdditionalInstanceField(mControlPanelWindowManager, "mCustomBlurModifier", opt)
                        }
                    }
                })
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.NotificationShadeDepthController\$updateBlurCallback\$1", lpparam.classLoader, "doFrame", Long::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val parentCtrl = XposedHelpers.getSurroundingThis(param.thisObject)
                val blurRatio = XposedHelpers.getAdditionalInstanceField(parentCtrl, "mCustomBlurModifier")
                val mBlurUtils = XposedHelpers.getObjectField(parentCtrl, "blurUtilsExt")
                XposedHelpers.setAdditionalInstanceField(mBlurUtils, "mCustomBlurModifier", blurRatio)
            }

            override fun after(param: AfterHookCallback) {
                val parentCtrl = XposedHelpers.getSurroundingThis(param.thisObject)
                val mBlurUtils = XposedHelpers.getObjectField(parentCtrl, "blurUtilsExt")
                XposedHelpers.removeAdditionalInstanceField(mBlurUtils, "mCustomBlurModifier")
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.BlurUtilsExt", lpparam.classLoader, "applyBlurByRadius", android.view.View::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val multiplier = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCustomBlurModifier") as? Int ?: return
                val blurUtils = XposedHelpers.getObjectField(param.thisObject, "blurUtils")
                val ratio = XposedHelpers.callMethod(blurUtils, "ratioOfBlurRadius", 1.0f * (param.args[1] as Int)) as? Float ?: return
                val newRatio = ratio * multiplier / 100f
                param.args[1] = Math.round((XposedHelpers.callMethod(blurUtils, "blurRadiusOfRatio", newRatio) as? Float ?: 0f))
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.controlcenter.phone.ControlPanelWindowManager", lpparam.classLoader, "setBlurRatio", Float::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val modifier = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCustomBlurModifier") as? Int ?: 100
                param.args[0] = (param.args[0] as Float) * modifier / 100f
            }
        })
    }

    @JvmStatic
    fun ChargeAnimationHook(lpparam: PackageReadyParam) {
        val ccCls = try {
            XposedHelpers.findClass("com.android.keyguard.charge.MiuiWirelessChargeController", lpparam.classLoader)
        } catch (t1: Throwable) {
            try {
                XposedHelpers.findClass("com.android.keyguard.charge.MiuiChargeController", lpparam.classLoader)
            } catch (t2: Throwable) {
                XposedHelpers.log(t1)
                XposedHelpers.log(t2)
                return
            }
        }

        val hook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler") as? Handler ?: return
                val mScreenOffRunnable = XposedHelpers.getObjectField(param.thisObject, "mScreenOffRunnable") as? Runnable ?: return
                val mScreenOnWakeLock = XposedHelpers.getObjectField(param.thisObject, "mScreenOnWakeLock") as? android.os.PowerManager.WakeLock ?: return

                if (mScreenOnWakeLock.isHeld) {
                    val timeout = MainModule.mPrefs.getInt("system_chargeanimtime", 20) * 1000
                    val oldReleaseRunnable = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mScreenOnWakeLockReleaseRunnable") as? Runnable
                    if (oldReleaseRunnable != null) mHandler.removeCallbacks(oldReleaseRunnable)
                    val releaseRunnable = Runnable { mScreenOnWakeLock.release() }
                    mHandler.postDelayed(releaseRunnable, timeout.toLong())
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mScreenOnWakeLockReleaseRunnable", releaseRunnable)
                    mHandler.removeCallbacks(mScreenOffRunnable)
                    mHandler.postDelayed(mScreenOffRunnable, timeout.toLong())
                }
            }
        }

        ModuleHelper.findAndHookMethodSilently(ccCls, "showWirelessChargeAnimation", hook)
        ModuleHelper.findAndHookMethodSilently(ccCls, "showRapidChargeAnimation", hook)
        ModuleHelper.findAndHookMethodSilently(ccCls, "showWirelessRapidChargeAnimation", hook)
    }

    private var mMaximumBacklight = 0f
    private var mMinimumBacklight = 0f
    private var backlightMaxLevel = 0

    private fun constrainValue(value: Float): Float {
        var v = value
        if (v < 0) v = 0f
        if (v > 1) v = 1f

        val limitmin = MainModule.mPrefs.getBoolean("system_autobrightness_limitmin")
        val limitmax = MainModule.mPrefs.getBoolean("system_autobrightness_limitmax")
        val minPct = MainModule.mPrefs.getInt("system_autobrightness_min", 25)
        val maxPct = MainModule.mPrefs.getInt("system_autobrightness_max", 75)

        val min = Helpers.convertGammaToLinearFloat(minPct / 100f * backlightMaxLevel, backlightMaxLevel, mMinimumBacklight, mMaximumBacklight)
        val max = Helpers.convertGammaToLinearFloat(maxPct / 100f * backlightMaxLevel, backlightMaxLevel, mMinimumBacklight, mMaximumBacklight)

        if (limitmin && v < min) v = min
        if (limitmax && v > max) v = max
        return v
    }

    @JvmStatic
    fun AutoBrightnessRangeHook(lpparam: SystemServerStartingParam) {
        val clampHook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val result = param.result as? Float ?: return
                if (result >= 0) {
                    param.setResult(constrainValue(result))
                }
            }
        }
        ModuleHelper.findAndHookMethod("com.android.server.display.AutomaticBrightnessController", lpparam.classLoader, "clampScreenBrightness", Float::class.javaPrimitiveType, clampHook)

        ModuleHelper.hookAllConstructors("com.android.server.display.AutomaticBrightnessController", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setLongField(param.thisObject, "mBrighteningLightDebounceConfig", 1000L)
                XposedHelpers.setLongField(param.thisObject, "mDarkeningLightDebounceConfig", 1200L)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.display.DisplayPowerController", lpparam.classLoader, "clampScreenBrightness", Float::class.javaPrimitiveType, clampHook)

        ModuleHelper.hookAllConstructors("com.android.server.display.DisplayPowerController", lpparam.classLoader, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val res = android.content.res.Resources.getSystem()
                val minBrightnessLevel = res.getInteger(res.getIdentifier("config_screenBrightnessSettingMinimum", "integer", "android"))
                val maxBrightnessLevel = res.getInteger(res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android"))
                val backlightBit = res.getInteger(res.getIdentifier("config_backlightBit", "integer", "android.miui"))
                backlightMaxLevel = (1 shl backlightBit) - 1
                mMinimumBacklight = (minBrightnessLevel - 1) * 1.0f / (backlightMaxLevel - 1)
                mMaximumBacklight = (maxBrightnessLevel - 1) * 1.0f / (backlightMaxLevel - 1)
            }
        })
    }

    @JvmStatic
    fun HideProximityWarningHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.policy.MiuiScreenOnProximityLock", lpparam.classLoader, "showHint", HookerClassHelper.DO_NOTHING)
        ModuleHelper.findAndHookMethod("com.android.server.policy.MiuiScreenOnProximityLock", lpparam.classLoader, "prepareHintWindow", HookerClassHelper.DO_NOTHING)
    }

    @JvmStatic
    fun RotationAnimationRes() {
        val opt = MainModule.mPrefs.getString("system_rotateanim", "1").toInt()
        val enter: Int
        val exit: Int
        if (opt == 2) {
            enter = R.anim.no_enter
            exit = R.anim.no_exit
        } else if (opt == 3) {
            enter = R.anim.xfade_enter
            exit = R.anim.xfade_exit
        } else {
            enter = 0
            exit = 0
        }

        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_0_enter", enter)
        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_0_exit", exit)
        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_180_enter", enter)
        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_180_exit", exit)
        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_minus_90_enter", enter)
        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_minus_90_exit", exit)
        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_plus_90_enter", enter)
        MainModule.resHooks.setResReplacement("android", "anim", "screen_rotate_plus_90_exit", exit)
    }

    @JvmStatic
    fun RotationAnimationHook(lpparam: SystemServerStartingParam) {
        val animEnter = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val opt = MainModule.mPrefs.getString("system_rotateanim", "1").toInt()
                val anim = param.result as? Animation ?: return
                if (opt == 2) {
                    anim.duration = 0
                    param.setResult(anim)
                } else if (opt == 3) {
                    val alphaAnim = AlphaAnimation(1.0f, 1.0f)
                    alphaAnim.interpolator = XposedHelpers.getStaticObjectField(
                        XposedHelpers.findClass("com.android.server.wm.AppTransitionInjector", lpparam.classLoader),
                        "QUART_EASE_OUT_INTERPOLATOR"
                    ) as? Interpolator
                    alphaAnim.duration = 300
                    alphaAnim.fillAfter = true
                    alphaAnim.fillBefore = true
                    alphaAnim.isFillEnabled = true
                    param.setResult(alphaAnim)
                }
            }
        }

        val animExit = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val opt = MainModule.mPrefs.getString("system_rotateanim", "1").toInt()
                if (opt == 2) {
                    val anim = param.result as? Animation ?: return
                    anim.duration = 0
                } else if (opt == 3) {
                    val alphaAnim = AlphaAnimation(1.0f, 0.0f)
                    alphaAnim.interpolator = XposedHelpers.getStaticObjectField(
                        XposedHelpers.findClass("com.android.server.wm.AppTransitionInjector", lpparam.classLoader),
                        "QUART_EASE_OUT_INTERPOLATOR"
                    ) as? Interpolator
                    alphaAnim.duration = 300
                    alphaAnim.fillAfter = true
                    alphaAnim.fillBefore = true
                    alphaAnim.isFillEnabled = true
                    param.setResult(alphaAnim)
                }
            }
        }

        ModuleHelper.findAndHookMethod("com.android.server.wm.ScreenRotationAnimationImpl", lpparam.classLoader, "createRotation180Enter", animEnter)
        ModuleHelper.findAndHookMethod("com.android.server.wm.ScreenRotationAnimationImpl", lpparam.classLoader, "createRotation180Exit", animExit)
        ModuleHelper.hookAllMethods("com.android.server.wm.ScreenRotationAnimationImpl", lpparam.classLoader, "createRotationEnter", animEnter)
        ModuleHelper.hookAllMethods("com.android.server.wm.ScreenRotationAnimationImpl", lpparam.classLoader, "createRotationEnterWithBackColor", animEnter)
        ModuleHelper.hookAllMethods("com.android.server.wm.ScreenRotationAnimationImpl", lpparam.classLoader, "createRotationExit", animExit)
    }
}
