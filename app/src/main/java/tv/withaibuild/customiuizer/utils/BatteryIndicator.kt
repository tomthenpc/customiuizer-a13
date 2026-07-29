package tv.withaibuild.customiuizer.utils

import android.animation.ArgbEvaluator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

class BatteryIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    protected var mDisplayWidth = 0
    protected var mIsBeingCharged = false
    protected var mIsExtremePowerSave = false
    protected var mIsPowerSave = false
    protected val mLowLevelSystem = resources.getInteger(
        resources.getIdentifier("config_lowBatteryWarningLevel", "integer", "android")
    )
    protected var mPowerLevel = 0
    protected var mTestPowerLevel = 0

    private val mFullColor = Color.GREEN
    private val mLowColor = Color.RED
    private val mPowerSaveColor = Color.rgb(245, 166, 35)
    private val mChargingColor = Color.YELLOW
    private var mLowLevel = mLowLevelSystem
    private var mHeight = 5
    private var mGlow = 0
    private var mTransparency = 0
    private var mPadding = 0
    private var mVisibility = View.VISIBLE
    private var mColorMode = ColorMode.DISCRETE
    private var mTesting = false
    private var mRounded = false
    private var mCentered = false
    private var mExpanded = false
    private var mOnKeyguard = false

    private var mScreenshot = false
    private var mBottom = false
    private var mLimited = false
    private var mTintColor = Color.argb(153, 0, 0, 0)
    private var mStatusBar: Any? = null

    enum class ColorMode {
        DUMMY, DISCRETE, GRADUAL, RAINBOW
    }

    init {
        updateDisplaySize()
    }

    fun init(statusBar: Any?) {
        mStatusBar = statusBar

        try {
            val shape = ShapeDrawable()
            val paint = shape.paint
            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true
            shape.setIntrinsicWidth(9999)
            setImageDrawable(shape)
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }

        updateParameters()
        ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
            override fun onChange(key: String?) {
                try {
                    if (!mTesting && key != null && key.contains("pref_key_system_batteryindicator")) {
                        Handler(Looper.getMainLooper()).post {
                            updateParameters()
                            update()
                        }
                    }
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                }
            }
        })

        val intentFilter = IntentFilter()
        intentFilter.addAction("tv.withaibuild.customiuizer.mods.BatteryIndicatorTest")
        if (MainModule.mPrefs.getBoolean("system_hidestatusbar_whenscreenshot")) {
            intentFilter.addAction("miui.intent.TAKE_SCREENSHOT")
        }
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if ("miui.intent.TAKE_SCREENSHOT" == intent.action) {
                        val finished = intent.getBooleanExtra("IsFinished", true)
                        updateScreenShotState(!finished)
                    } else {
                        removeCallbacks(step)
                        startTest()
                    }
                }
            },
            intentFilter,
            Context.RECEIVER_EXPORTED
        )
    }

    private val step = StepRunnable()

    private inner class StepRunnable : Runnable {
        override fun run() {
            mTestPowerLevel--
            if (mTestPowerLevel >= 0) {
                update()
                postDelayed(this, if (mTestPowerLevel == mLowLevel - 1) 300L else 20L)
            } else {
                removeCallbacks(this)
                mTesting = false
                postDelayed({
                    updateParameters()
                    update()
                }, 1000L)
            }
        }
    }

    private fun startTest() {
        mTesting = true
        mTestPowerLevel = 100
        post(step)
    }

    private fun postUpdate() {
        post { update() }
    }

    fun updateScreenShotState(screenshot: Boolean) {
        if (mScreenshot == screenshot) return
        mScreenshot = screenshot
        if (!mScreenshot && !mLimited) {
            visibility = mVisibility
        }
        update()
    }

    fun onExpandingChanged(expanded: Boolean) {
        if (mExpanded == expanded) return
        mExpanded = expanded
        update()
    }

    fun onKeyguardStateChanged(showing: Boolean) {
        if (mOnKeyguard == showing) return
        mOnKeyguard = showing
        update()
    }

    fun onDarkModeChanged(intensity: Float, tintColor: Int) {
        // if (intensity != 0.0f && intensity != 1.0f) return;
        if (mTintColor == tintColor) return
        mTintColor = tintColor
        update()
    }

    fun onBatteryLevelChanged(powerLevel: Int, isCharging: Boolean, isCharged: Boolean) {
        if (this.mPowerLevel == powerLevel && this.mIsBeingCharged == isCharging && !isCharged) return
        this.mPowerLevel = powerLevel
        this.mIsBeingCharged = isCharging && !isCharged
        update()
    }

    fun onPowerSaveChanged(isPowerSave: Boolean) {
        if (this.mIsPowerSave == isPowerSave) return
        this.mIsPowerSave = isPowerSave
        update()
    }

    fun onExtremePowerSaveChanged(isExtremePowerSave: Boolean) {
        if (this.mIsExtremePowerSave == isExtremePowerSave) return
        this.mIsExtremePowerSave = isExtremePowerSave
        update()
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        super.onConfigurationChanged(configuration)
        updateDisplaySize()
        postUpdate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            updateDisplaySize()
            postUpdate()
        }
    }

    fun update() {
        if (mScreenshot) {
            visibility = View.GONE
        } else {
            if (mLimited) visibility = if (mExpanded || mOnKeyguard) mVisibility else View.GONE
        }
        clearAnimation()
        updateDrawable()
    }

    fun updateDisplaySize() {
        this.mDisplayWidth = measuredWidth
    }

    protected fun updateParameters() {
        mColorMode = ColorMode.values()[MainModule.mPrefs.getStringAsInt("system_batteryindicator_color", 1)]
        mLowLevel = MainModule.mPrefs.getInt("system_batteryindicator_lowlevel", mLowLevelSystem)
        mHeight = MainModule.mPrefs.getInt("system_batteryindicator_height", 5)
        mGlow = MainModule.mPrefs.getInt("system_batteryindicator_glow", 0)
        mRounded = MainModule.mPrefs.getBoolean("system_batteryindicator_rounded")
        mBottom = MainModule.mPrefs.getStringAsInt("system_batteryindicator_align", 1) == 2
        mCentered = MainModule.mPrefs.getBoolean("system_batteryindicator_centered")
        mLimited = MainModule.mPrefs.getBoolean("system_batteryindicator_limitvis")
        mTransparency = MainModule.mPrefs.getInt("system_batteryindicator_transp", 0)
        mPadding = MainModule.mPrefs.getInt("system_batteryindicator_padding", 0)
        mVisibility = if (MainModule.mPrefs.getBoolean("system_batteryindicator")) View.VISIBLE else View.GONE

        val lp = layoutParams as? FrameLayout.LayoutParams ?: return
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        lp.gravity = if (mBottom) Gravity.BOTTOM else Gravity.TOP
        this.layoutParams = lp

        try {
            imageAlpha = 255 - Math.round(255 * mTransparency / 100f)
        } catch (ignored: Throwable) {
        }
        visibility = mVisibility
        scaleType = if (mCentered) ScaleType.CENTER else ScaleType.MATRIX
        imageMatrix = Matrix()
    }

    protected fun updateDrawable() {
        try {
            val level = if (this.mTesting) this.mTestPowerLevel else this.mPowerLevel
            var color = this.mFullColor
            when {
                !this.mTesting && this.mIsBeingCharged -> color = this.mChargingColor
                !this.mTesting && (this.mIsPowerSave || this.mIsExtremePowerSave) -> color = this.mPowerSaveColor
                level <= this.mLowLevel -> color = this.mLowColor
            }

            val shape = drawable as? ShapeDrawable ?: return
            shape.shaderFactory = null
            val paint = shape.paint
            paint.shader = null

            if (color == Color.TRANSPARENT && mStatusBar != null) {
                try {
                    color = if (mExpanded) {
                        Color.WHITE
                    } else {
                        if (mOnKeyguard) {
                            val isLightWallpaperStatusBar = XposedHelpers.getBooleanField(
                                XposedHelpers.getObjectField(mStatusBar, "mKeyguardIndicationController"),
                                "mDarkStyle"
                            )
                            if (isLightWallpaperStatusBar) Color.argb(153, 0, 0, 0) else Color.WHITE
                        } else {
                            mTintColor
                        }
                    }
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                }
            }

            val mDisplayPadding = Math.round(mPadding / 100f * this.mDisplayWidth)

            if (mColorMode == ColorMode.GRADUAL) {
                color = ArgbEvaluator().evaluate(
                    1f - (level - this.mLowLevel) / (100f - this.mLowLevel),
                    color,
                    mLowColor
                ) as Int
            } else if (mColorMode == ColorMode.RAINBOW) {
                val steps = 15
                val jump = 300f / steps
                val pos = FloatArray(steps)
                val rainbow = IntArray(steps)
                for (i in 0 until steps) {
                    pos[i] = i / (steps - 1).toFloat()
                    var c = (if (mCentered) 240 else 0) + jump * i
                    if (c > 360) c -= 360
                    rainbow[i] = Color.HSVToColor(255, floatArrayOf(c, 1.0f, 1.0f))
                }
                shape.shaderFactory = object : ShapeDrawable.ShaderFactory() {
                    override fun resize(width: Int, height: Int): Shader {
                        return if (mCentered) {
                            LinearGradient(
                                width / 2f - (mDisplayWidth - mDisplayPadding * 2) / 2f,
                                height / 2f,
                                (mDisplayWidth - mDisplayPadding * 2).toFloat(),
                                height / 2f,
                                rainbow,
                                pos,
                                Shader.TileMode.CLAMP
                            )
                        } else {
                            LinearGradient(
                                0f,
                                height / 2f,
                                (mDisplayWidth - mDisplayPadding * 2).toFloat(),
                                height / 2f,
                                rainbow,
                                pos,
                                Shader.TileMode.CLAMP
                            )
                        }
                    }
                }
            }

            paint.color = color
            shape.shape = if (mRounded) {
                RoundRectShape(FloatArray(8) { mHeight.toFloat() }, null, null)
            } else {
                RectShape()
            }

            val mWidth = Math.round((this.mDisplayWidth - mDisplayPadding * 2) * level / 100f)
            val mDensity = resources.displayMetrics.density
            val sbHeight = resources.getDimensionPixelSize(
                resources.getIdentifier("status_bar_height", "dimen", "android")
            )

            if (mGlow == 0) {
                paint.clearShadowLayer()
                if (mBottom) {
                    setPadding(mDisplayPadding, 0, mDisplayPadding, -mHeight)
                } else {
                    setPadding(mDisplayPadding, -mHeight, mDisplayPadding, 0)
                }
                shape.intrinsicHeight = mHeight * 2
                shape.intrinsicWidth = mWidth
            } else {
                val shadowPadding = sbHeight - mHeight
                paint.setShadowLayer(
                    (mGlow / 100f) * (sbHeight - 9 * mDensity),
                    if (mCentered || mDisplayPadding > 0) 0f else shadowPadding / 2f,
                    if (mBottom) mHeight - 10f else 10f - mHeight,
                    Color.argb(
                        Math.min(
                            Math.round(mGlow / 100f * 255),
                            Math.round(255 - mTransparency / 100f * 255)
                        ),
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                    )
                )
                if (mDisplayPadding == 0) {
                    setPadding(
                        if (mCentered) 0 else -shadowPadding,
                        if (mBottom) shadowPadding else -shadowPadding,
                        if (mCentered) 0 else Math.min(mDisplayWidth - mWidth, shadowPadding),
                        if (mBottom) -shadowPadding else shadowPadding
                    )
                } else {
                    setPadding(
                        mDisplayPadding,
                        if (mBottom) shadowPadding else -shadowPadding,
                        mDisplayPadding,
                        if (mBottom) -shadowPadding else shadowPadding
                    )
                }
                shape.intrinsicHeight = sbHeight
                shape.intrinsicWidth = mWidth + if (mCentered) 0 else if (mDisplayPadding == 0) shadowPadding else 0
            }

            invalidate()
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }
}
