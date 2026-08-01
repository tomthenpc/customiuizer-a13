package tv.withaibuild.customiuizer.mods

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.PowerManager
import android.text.format.DateFormat
import java.lang.ref.WeakReference
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

object SystemStatusBarClockAndMoreHooks {

    // Pre-compiled regex for clock format hour replacement (hot path, called each tick).
    private val CLOCK_HOUR_PATTERN = Regex("h+:")

    @JvmStatic
    fun StatusBarClockTweakHook(lpparam: PackageReadyParam) {
        val statusbarClockTweak = MainModule.mPrefs.getBoolean("system_statusbar_clocktweak")
        val ccClockTweak = MainModule.mPrefs.getBoolean("system_cc_clocktweak")
        val scheduleHook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val clockController = param.thisObject
                val mContext = XposedHelpers.getObjectField(clockController, "mContext") as? Context ?: return
                if (isScreenOn(mContext)) startOrRestartSecondTicker(clockController)

                val controllerRef = WeakReference(clockController)
                val screenAndTimeReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        ModuleHelper.guarded("SystemStatusBarClockAndMoreHooks.clockScreenAndTimeReceiver") {
                            val controller = controllerRef.get() ?: return@guarded
                            val controllerContext = XposedHelpers.getObjectField(controller, "mContext") as? Context ?: return@guarded
                            val state = secondTickerState(controller)
                            when (intent.action) {
                                Intent.ACTION_SCREEN_OFF -> {
                                    state.setScreen(false)
                                    stopSecondTimer(controller)
                                }
                                Intent.ACTION_SCREEN_ON,
                                "android.intent.action.TIME_SET" -> {
                                    state.setScreen(true)
                                    if (isScreenOn(controllerContext)) startOrRestartSecondTicker(controller)
                                }
                            }
                        }
                    }
                }

                val filter = IntentFilter().apply {
                    addAction("android.intent.action.TIME_SET")
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
                ModuleHelper.registerOwnedReceiver(
                    mContext,
                    clockController,
                    "systemui.clockScreenAndTimeReceiver",
                    screenAndTimeReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            }
        }
        if (ccClockTweak || statusbarClockTweak) {
            ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.policy.MiuiStatusBarClockController", lpparam.classLoader, scheduleHook)
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.MiuiStatusBarClockController", lpparam.classLoader, "fireTimeChange", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val clockController = param.thisObject
                    val mClockListeners = XposedHelpers.getObjectField(clockController, "mClockListeners") as? ArrayList<Any> ?: return
                    for (clock in mClockListeners) {
                        val showSeconds = XposedHelpers.getAdditionalInstanceField(clock, "showSeconds")
                        if (showSeconds == null) {
                            XposedHelpers.callMethod(clock, "onTimeChange")
                        }
                    }
                    param.returnAndSkip(null)
                }
            })
        }
        val ccDateFormat = MainModule.mPrefs.getString("system_cc_dateformat", "")
        val ccDateCustom = ccDateFormat.isNotEmpty()
        val hideDateView = MainModule.mPrefs.getBoolean("system_cc_hidedate")
        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.views.MiuiClock", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val clock = param.thisObject as? TextView ?: return
                if (param.getArgsCount() != 3) return
                val clockId = clock.resources.getIdentifier("clock", "id", "com.android.systemui")
                val bigClockId = clock.resources.getIdentifier("big_time", "id", "com.android.systemui")
                val dateClockId = clock.resources.getIdentifier("date_time", "id", "com.android.systemui")
                val horizDateClockId = clock.resources.getIdentifier("horizontal_date_time", "id", "com.android.systemui")
                val thisClockId = clock.id
                if (clockId == thisClockId && statusbarClockTweak) {
                    XposedHelpers.setAdditionalInstanceField(clock, "clockName", "clock")
                    if (getShowSeconds()) {
                        XposedHelpers.setAdditionalInstanceField(clock, "showSeconds", true)
                    }
                } else if (bigClockId == thisClockId && ccClockTweak) {
                    XposedHelpers.setAdditionalInstanceField(clock, "clockName", "ccClock")
                    if (getCCShowSeconds()) {
                        XposedHelpers.setAdditionalInstanceField(clock, "showSeconds", true)
                    }
                    initClockStyle(clock)
                } else if ((dateClockId == thisClockId || thisClockId == horizDateClockId) && (ccDateCustom || hideDateView)) {
                    XposedHelpers.setAdditionalInstanceField(clock, "clockName", "ccDate")
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.views.MiuiClock", lpparam.classLoader, "updateTime", object : MethodHook(XposedInterface.PRIORITY_HIGHEST) {
            override fun before(param: BeforeHookCallback) {
                val clock = param.thisObject as? TextView ?: return
                val clockName = XposedHelpers.getAdditionalInstanceField(clock, "clockName") as? String ?: return
                val mContext = clock.context
                val mMiuiStatusBarClockController = XposedHelpers.getObjectField(clock, "mMiuiStatusBarClockController")
                val mCalendar = XposedHelpers.getObjectField(mMiuiStatusBarClockController, "mCalendar")
                var timeFmt: String? = null
                if ("ccClock" == clockName && ccClockTweak) {
                    val customFormat = MainModule.mPrefs.getString("system_cc_clock_customformat", "")
                    if (customFormat.isNotEmpty()) {
                        timeFmt = customFormat
                    }
                } else if ("ccDate" == clockName && (!hideDateView && ccDateCustom)) {
                    timeFmt = ccDateFormat
                } else if ("clock" == clockName && statusbarClockTweak) {
                    val customFormat = MainModule.mPrefs.getString("system_statusbar_clock_customformat", "")
                    var enableCustomFormat = MainModule.mPrefs.getBoolean("system_statusbar_clock_customformat_enable")
                    enableCustomFormat = enableCustomFormat && customFormat.isNotEmpty()
                    if (enableCustomFormat) {
                        timeFmt = customFormat
                    } else {
                        val showSeconds = MainModule.mPrefs.getBoolean("system_statusbar_clock_show_seconds")
                        val is24 = MainModule.mPrefs.getBoolean("system_statusbar_clock_24hour_format")
                        val showAmpm = MainModule.mPrefs.getBoolean("system_statusbar_clock_show_ampm")
                        val hourIn2d = MainModule.mPrefs.getBoolean("system_statusbar_clock_leadingzero")
                        val fmt = if (showAmpm) "fmt_time_12hour_minute_pm" else "fmt_time_12hour_minute"
                        val fmtResId = mContext.resources.getIdentifier(fmt, "string", "com.android.systemui")
                        var fmtString = mContext.getString(fmtResId)
                        if (showSeconds) {
                            fmtString = fmtString.replaceFirst(":mm", ":mm:ss")
                        }
                        var hourStr = "h"
                        if (is24) {
                            hourStr = "H"
                        }
                        if (hourIn2d) {
                            hourStr += hourStr
                        }
                        timeFmt = CLOCK_HOUR_PATTERN.replaceFirst(fmtString, "$hourStr:")
                    }
                }
                if (timeFmt != null) {
                    val formatSb = StringBuilder(timeFmt)
                    val textSb = StringBuilder()
                    XposedHelpers.callMethod(mCalendar, "format", mContext, textSb, formatSb)
                    clock.text = textSb.toString()
                    param.returnAndSkip(null)
                }
            }
        })
        if (hideDateView) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.views.MiuiClock", lpparam.classLoader, "setClockVisibility", Int::class.javaPrimitiveType, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val clock = param.thisObject as? TextView ?: return
                    val clockName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "clockName") as? String
                    if ("ccDate" == clockName) {
                        XposedHelpers.setObjectField(param.thisObject, "mVisibility", 8)
                        clock.visibility = View.GONE
                        param.returnAndSkip(null)
                    }
                }
            })
        }
        if (statusbarClockTweak) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "onAttachedToWindow", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val clock = XposedHelpers.getObjectField(param.thisObject, "mMiuiClock") as? TextView ?: return
                    initClockStyle(clock)
                }
            })
        }
        if (ccClockTweak) {
            val ccClockFontSize = MainModule.mPrefs.getInt("system_cc_clock_fontsize", 9)
            val clockMarginTop = MainModule.mPrefs.getInt("system_cc_clock_topmargin_indrawer", 0)
            val defaultVerticalOffset = 10
            val verticalOffset = MainModule.mPrefs.getInt("system_cc_clock_verticaloffset", defaultVerticalOffset)
            if (ccClockFontSize > 9 || clockMarginTop > 0 || verticalOffset != defaultVerticalOffset) {
                val setSizeHook = object : MethodHook() {
                    override fun after(param: AfterHookCallback) {
                        val clock = XposedHelpers.getObjectField(param.thisObject, "mBigTime") as? TextView ?: return
                        if (ccClockFontSize > 9) {
                            clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, ccClockFontSize.toFloat())
                        }
                        val clsName = param.thisObject.javaClass.simpleName
                        if (clockMarginTop > 0 && "MiuiNotificationHeaderView" == clsName) {
                            val lp = clock.layoutParams as? LinearLayout.LayoutParams ?: return
                            val marginTop = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                clockMarginTop.toFloat(),
                                clock.resources.displayMetrics
                            )
                            lp.topMargin = marginTop.toInt()
                            clock.layoutParams = lp
                        }
                        if (verticalOffset != defaultVerticalOffset) {
                            val marginTop = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                (verticalOffset - defaultVerticalOffset).toFloat(),
                                clock.resources.displayMetrics
                            )
                            clock.translationY = marginTop
                        }
                    }
                }
                ModuleHelper.findAndHookMethod("com.android.systemui.qs.MiuiNotificationHeaderView", lpparam.classLoader, "updateResources", setSizeHook)
                ModuleHelper.findAndHookMethod("com.android.systemui.qs.MiuiQSHeaderView", lpparam.classLoader, "updateResources", setSizeHook)
            }
        }
    }

    private fun initClockStyle(mClock: TextView) {
        val res = mClock.resources
        val clockName = XposedHelpers.getAdditionalInstanceField(mClock, "clockName") as? String ?: return
        val subKey = if (clockName == "clock") "statusbar" else "cc"
        val statusBarClock = clockName == "clock"
        val enableCustomFormat = !statusBarClock || MainModule.mPrefs.getBoolean("system_${subKey}_clock_customformat_enable")
        val customFormat = MainModule.mPrefs.getString("system_${subKey}_clock_customformat", "")
        val dualRows = enableCustomFormat && customFormat.contains("\n")
        if (statusBarClock) {
            val dimStep = 0.5f
            val fontSize = MainModule.mPrefs.getInt("system_statusbar_clock_fontsize", 13)
            if (fontSize > 13) {
                mClock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * dimStep)
            }
            if (dualRows) {
                mClock.setLineSpacing(0f, if (0.5 * fontSize > 8.5f) 0.85f else 0.9f)
            }
            val align = MainModule.mPrefs.getStringAsInt("system_${subKey}_clock_align", 1)
            if (align == 2) {
                mClock.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            } else if (align == 3) {
                mClock.textAlignment = View.TEXT_ALIGNMENT_CENTER
            } else if (align == 4) {
                mClock.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            }
            if (MainModule.mPrefs.getBoolean("system_${subKey}_clock_bold")) {
                mClock.typeface = Typeface.DEFAULT_BOLD
            }
            val leftMargin = MainModule.mPrefs.getInt("system_statusbar_clock_leftmargin", 0)
            val leftMarginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                leftMargin * dimStep,
                res.displayMetrics
            ).toInt()
            val rightMargin = MainModule.mPrefs.getInt("system_statusbar_clock_rightmargin", 0)
            val rightMarginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                rightMargin * dimStep,
                res.displayMetrics
            ).toInt()
            val defaultVerticalOffset = 8
            val verticalOffset = MainModule.mPrefs.getInt("system_statusbar_clock_verticaloffset", defaultVerticalOffset)
            if (verticalOffset != defaultVerticalOffset) {
                val marginTop = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (verticalOffset - defaultVerticalOffset) * dimStep,
                    res.displayMetrics
                )
                mClock.translationY = marginTop
            }

            if (MainModule.mPrefs.getBoolean("system_statusbar_clock_chip")) {
                val lp = mClock.layoutParams as? LinearLayout.LayoutParams ?: return
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                if (leftMarginPx > 0) {
                    lp.leftMargin = leftMarginPx
                }
                if (rightMarginPx > 0) {
                    lp.rightMargin = rightMarginPx
                }
                mClock.layoutParams = lp

                val useMonet = MainModule.mPrefs.getBoolean("system_statusbar_clock_chip_usemonet")
                val enableCustomText = MainModule.mPrefs.getBoolean("system_statusbar_clock_chip_customtextcolor")
                if (useMonet || enableCustomText) {
                    XposedHelpers.setObjectField(mClock, "mUseWallpaperTextColor", true)
                }

                var startColor = MainModule.mPrefs.getInt("system_statusbar_clock_chip_startcolor", 0x8F7C4DFF.toInt())
                var endColor = MainModule.mPrefs.getInt("system_statusbar_clock_chip_endcolor", 0x2FA7FFEB.toInt())
                if (useMonet) {
                    mClock.setTextColor(mClock.resources.getColor(android.R.color.system_accent1_0, null))
                    startColor = mClock.resources.getColor(android.R.color.system_accent1_600, null)
                    endColor = startColor
                } else if (enableCustomText) {
                    val textcolor = MainModule.mPrefs.getInt("system_statusbar_clock_chip_textcolor", 0xFFFFFFFF.toInt())
                    mClock.setTextColor(textcolor)
                }
                val chipDrawable = GradientDrawable()
                val verticalOrientation = MainModule.mPrefs.getBoolean("system_statusbar_clock_chip_orientation_vertical")
                chipDrawable.orientation = if (verticalOrientation) GradientDrawable.Orientation.TOP_BOTTOM else GradientDrawable.Orientation.LEFT_RIGHT
                chipDrawable.colors = intArrayOf(startColor, endColor)
                chipDrawable.shape = GradientDrawable.RECTANGLE
                var horizPadding = MainModule.mPrefs.getInt("system_statusbar_clock_chip_horizpadding", 0)
                var vertPadding = MainModule.mPrefs.getInt("system_statusbar_clock_chip_verticalpadding", 0)
                if (horizPadding > 0) {
                    horizPadding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        horizPadding.toFloat(),
                        res.displayMetrics
                    ).toInt()
                }
                if (vertPadding > 0 || horizPadding > 0) {
                    chipDrawable.setPadding(horizPadding, vertPadding, horizPadding, vertPadding)
                }
                var radiusPx = MainModule.mPrefs.getInt("system_statusbar_clock_chip_radius", 0)
                if (radiusPx > 0) {
                    radiusPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        radiusPx.toFloat(),
                        res.displayMetrics
                    ).toInt()
                    chipDrawable.cornerRadius = radiusPx.toFloat()
                }
                mClock.background = chipDrawable
            } else {
                if (leftMarginPx > 0 || rightMarginPx > 0) {
                    val lp = mClock.layoutParams as? LinearLayout.LayoutParams ?: return
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    lp.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    if (leftMarginPx > 0) {
                        lp.leftMargin = leftMarginPx
                    }
                    if (rightMarginPx > 0) {
                        lp.rightMargin = rightMarginPx
                    }
                    mClock.layoutParams = lp
                }
            }
        }
        if (dualRows) {
            mClock.setSingleLine(false)
            mClock.maxLines = 2
        }

        val fixedWidth = MainModule.mPrefs.getInt("system_${subKey}_clock_fixedcontent_width", 10)
        if (fixedWidth > 10) {
            val lp = mClock.layoutParams
            lp.width = (mClock.resources.displayMetrics.density * fixedWidth).toInt()
            mClock.layoutParams = lp
        }
    }

    private fun getShowSeconds(): Boolean {
        val sbShowSeconds = MainModule.mPrefs.getBoolean("system_statusbar_clock_show_seconds")
        val customFormat = MainModule.mPrefs.getString("system_statusbar_clock_customformat", "")
        val enableCustomFormat = MainModule.mPrefs.getBoolean("system_statusbar_clock_customformat_enable")
        return (enableCustomFormat && customFormat.contains("ss")) || (!enableCustomFormat && sbShowSeconds)
    }

    private fun getCCShowSeconds(): Boolean {
        val customFormat = MainModule.mPrefs.getString("system_cc_clock_customformat", "")
        return customFormat.contains("ss")
    }

    private fun isScreenOn(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isInteractive ?: true
    }

    /**
     * Scheduler abstraction so the second-ticker lifecycle can be unit tested without a
     * real [Handler]. Production uses [HandlerScheduler]; tests use [FakeTickerScheduler].
     *
     * Returns true if the runnable was actually enqueued, matching Android
     * [Handler.postDelayed] semantics.
     */
    internal interface TickerScheduler {
        fun postDelayed(runnable: Runnable, delayMillis: Long): Boolean
        fun removeCallbacks(runnable: Runnable)
    }

    internal class HandlerScheduler(private val handler: Handler) : TickerScheduler {
        override fun postDelayed(runnable: Runnable, delayMillis: Long): Boolean {
            return handler.postDelayed(runnable, delayMillis)
        }
        override fun removeCallbacks(runnable: Runnable) {
            handler.removeCallbacks(runnable)
        }
    }

    /**
     * Pure per-controller state for the status-bar second ticker. Visible to unit tests
     * so the screen-on/off, generation and stop/start semantics can be exercised without
     * a real MiuiStatusBarClockController.
     */
    internal class SecondTickerState {
        var screenOn: Boolean = true
            private set
        var running: Boolean = false
            private set
        var generation: Long = 0L
            private set
        var scheduledGeneration: Long = 0L
            private set
        var callbackPending: Boolean = false
            private set

        fun setScreen(on: Boolean) {
            screenOn = on
            if (!on) {
                running = false
                callbackPending = false
            }
        }

        fun start(newGen: Long) {
            generation = newGen
            scheduledGeneration = newGen
            running = true
            callbackPending = false
        }

        fun stop() {
            generation = 0L
            scheduledGeneration = 0L
            running = false
            callbackPending = false
        }

        fun markCallbackPending(pending: Boolean) {
            callbackPending = pending
        }

        fun canRePost(myGen: Long): Boolean =
            screenOn && running && !callbackPending && generation == myGen
    }

    /**
     * Single per-controller second-ticker runnable. The [scheduledGen] is fixed when the
     * callback is posted, so re-initialising the timer with a new generation cannot make
     * this callback continue into a new loop.
     */
    internal open class ClockRunnable(
        private val scheduledGen: Long,
        private val controllerRef: WeakReference<Any>,
        private val stateRef: WeakReference<SecondTickerState>,
        private val scheduler: TickerScheduler
    ) : Runnable {

        override fun run() {
            val state = stateRef.get() ?: return
            synchronized(state) {
                if (state.scheduledGeneration != scheduledGen || !state.running || !state.screenOn || !state.callbackPending) return
                state.markCallbackPending(false)
            }

            ModuleHelper.guarded("SystemStatusBarClockAndMoreHooks.secondTicker") {
                doTick()
            }

            val after = stateRef.get() ?: return
            scheduleTicker(after, scheduler, this, scheduledGen, 1000L)
        }

        protected open fun doTick() {
            val controller = controllerRef.get() ?: return
            val context = XposedHelpers.getObjectField(controller, "mContext") as? Context ?: return
            val mCalendar = XposedHelpers.getObjectField(controller, "mCalendar")
            XposedHelpers.callMethod(mCalendar, "setTimeInMillis", java.lang.System.currentTimeMillis())
            XposedHelpers.setObjectField(controller, "mIs24", DateFormat.is24HourFormat(context))
            val mClockListeners = XposedHelpers.getObjectField(controller, "mClockListeners") as? ArrayList<Any> ?: return
            for (clock in mClockListeners) {
                val showSeconds = XposedHelpers.getAdditionalInstanceField(clock, "showSeconds")
                if (showSeconds != null) {
                    XposedHelpers.callMethod(clock, "onTimeChange")
                }
            }
        }
    }

    /**
     * Posts [runnable] if the [state] generation still matches, the screen is on and no
     * callback is already pending. The post is performed inside [state]'s monitor so the
     * [SecondTickerState.callbackPending] flag always reflects the real scheduler state.
     */
    internal fun scheduleTicker(
        state: SecondTickerState,
        scheduler: TickerScheduler,
        runnable: Runnable,
        generation: Long,
        delayMillis: Long
    ): Boolean {
        synchronized(state) {
            if (!state.canRePost(generation)) return false
            state.markCallbackPending(true)
            try {
                if (!scheduler.postDelayed(runnable, delayMillis)) {
                    state.markCallbackPending(false)
                    return false
                }
            } catch (t: Throwable) {
                state.markCallbackPending(false)
                if (t is OutOfMemoryError) throw t
                return false
            }
            return true
        }
    }

    private fun secondTickerState(clockController: Any): SecondTickerState {
        var state = XposedHelpers.getAdditionalInstanceField(clockController, "secondTickerState") as? SecondTickerState
        if (state == null) {
            state = SecondTickerState()
            XposedHelpers.setAdditionalInstanceField(clockController, "secondTickerState", state)
        }
        return state
    }

    private fun nextGeneration(): Long = java.lang.System.nanoTime()

    private fun stopSecondTimer(clockController: Any) {
        val state = secondTickerState(clockController)

        synchronized(state) {
            state.stop()
        }

        val clockHandler = XposedHelpers.getAdditionalInstanceField(clockController, "clockHandler") as? Handler
        val clockRunnable = XposedHelpers.getAdditionalInstanceField(clockController, "clockRunnable") as? Runnable
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable)
        }
    }

    private fun startOrRestartSecondTicker(clockController: Any) {
        val ccShowSeconds = getCCShowSeconds()
        val finalSbShowSeconds = getShowSeconds()
        val state = secondTickerState(clockController)
        var clockHandler = XposedHelpers.getAdditionalInstanceField(clockController, "clockHandler") as? Handler
        var clockRunnable = XposedHelpers.getAdditionalInstanceField(clockController, "clockRunnable") as? ClockRunnable
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable)
        }
        if (!ccShowSeconds && !finalSbShowSeconds) {
            state.stop()
            return
        }
        val mContext = XposedHelpers.getObjectField(clockController, "mContext") as? Context ?: return
        if (clockHandler == null) {
            clockHandler = Handler(mContext.mainLooper)
            XposedHelpers.setAdditionalInstanceField(clockController, "clockHandler", clockHandler)
        }

        val newGen = nextGeneration()
        state.start(newGen)

        val scheduler = HandlerScheduler(clockHandler)
        clockRunnable = ClockRunnable(newGen, WeakReference(clockController), WeakReference(state), scheduler)
        XposedHelpers.setAdditionalInstanceField(clockController, "clockRunnable", clockRunnable)
        scheduleTicker(state, scheduler, clockRunnable, newGen, 1000 - java.lang.System.currentTimeMillis() % 1000)
    }

    @JvmStatic
    fun ExpandNotificationsHook(lpparam: PackageReadyParam) {
        val feedbackMethod = "setFeedbackIcon"
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader, feedbackMethod, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mOnKeyguard = XposedHelpers.callMethod(param.thisObject, "isOnKeyguard") as? Boolean ?: false
                if (!mOnKeyguard) {
                    val notification = XposedHelpers.getObjectField(XposedHelpers.callMethod(param.thisObject, "getEntry"), "mSbn")
                    val pkgName = XposedHelpers.callMethod(notification, "getPackageName") as? String ?: return
                    val opt = MainModule.mPrefs.getString("system_expandnotifs", "1").toInt()
                    val isSelected = MainModule.mPrefs.getStringSet("system_expandnotifs_apps").contains(pkgName)
                    if (opt == 2 && !isSelected || opt == 3 && isSelected)
                        XposedHelpers.callMethod(param.thisObject, "setSystemExpanded", true)
                }
            }
        })
    }

    @JvmStatic
    fun ExpandHeadsUpHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader, "setHeadsUp", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mOnKeyguard = XposedHelpers.callMethod(param.thisObject, "isOnKeyguard") as? Boolean ?: false
                val showHeadsUp = param.getArg(0) as? Boolean ?: false
                if (!mOnKeyguard && showHeadsUp) {
                    val notifyRow = param.thisObject as? View ?: return
                    val notification = XposedHelpers.getObjectField(XposedHelpers.callMethod(param.thisObject, "getEntry"), "mSbn")
                    val pkgName = XposedHelpers.callMethod(notification, "getPackageName") as? String ?: return
                    val opt = MainModule.mPrefs.getString("system_expandheadups", "1").toInt()
                    val isSelected = MainModule.mPrefs.getStringSet("system_expandheadups_apps").contains(pkgName)
                    if (opt == 2 && !isSelected || opt == 3 && isSelected) {
                        notifyRow.postDelayed({
                            ModuleHelper.guarded { XposedHelpers.callMethod(param.thisObject, "expandNotification") }
                        }, 60)
                    }
                }
            }
        })
    }
}
