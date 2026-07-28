package name.monwf.customiuizer.utils

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.audiofx.Visualizer
import android.os.AsyncTask
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.palette.graphics.Palette
import name.monwf.customiuizer.MainModule
import name.monwf.customiuizer.mods.utils.ModuleHelper
import name.monwf.customiuizer.mods.utils.XposedHelpers
import java.io.File

@Suppress("DEPRECATION")
class AudioVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    enum class BarStyle {
        DUMMY, SOLID, SOLID_ROUNDED, DASHED, CIRCLES, LINE
    }

    enum class ColorMode {
        DUMMY, MATCH, STATIC, RAINBOW_H, RAINBOW_V, DYNAMIC
    }

    enum class RenderType {
        AUTO, LINES, PATH
    }

    interface PaletteAsyncListener {
        fun onGenerated(palette: Palette?)
    }

    companion object {
        @JvmStatic
        fun allZeros(array: ByteArray?): Boolean {
            if (array == null) return true
            for (item in array) if (item != 0.toByte()) return false
            return true
        }
    }

    private val mBandsNum = 31
    private val maxDp = 280

    private val mBands = floatArrayOf(
        50f, 90f, 130f, 180f, 220f, 260f, 320f, 380f, 430f, 520f, 610f, 700f, 770f, 920f,
        1080f, 1270f, 1480f, 1720f, 2000f, 2320f, 2700f, 3135f, 3700f, 4400f, 5300f, 6400f,
        7700f, 9500f, 10500f, 12000f, 16000f
    )

    private var mHeight = 0
    private var mWidth = 0
    private var maxDb = 50f
    private val mDensity: Float
    private val mPaint: Paint
    private lateinit var mGlowPaint: Paint
    private var mVisualizer: Visualizer? = null
    private var mVisualizerColorAnimator: ObjectAnimator? = null
    private var mVisualizerGlowColorAnimator: ObjectAnimator? = null

    private val mValueAnimators: Array<ValueAnimator>
    private val mFFTPoints: FloatArray
    private val mRainbow: IntArray
    private val mRainbowVertical: IntArray
    private val mPositions: FloatArray
    private val mLinePath = Path()

    private var isMusicPlaying = false
    @JvmField
    var isScreenOn = false
    private var isOnKeyguard = false
    private var isExpandedPanel = false
    private var isOnCustomLockScreen = false
    private var mPlaying = false
    private var mDisplaying = false
    private var mOpaqueColor = 0
    private var mColor = 0
    private val mHandler: Handler
    private var mArt: Bitmap? = null
    private var mProcessedArt: Bitmap? = null

    @JvmField
    var showOnCustom = false
    private var animDur = 0
    private var transparency = 0
    @JvmField
    var colorMode = ColorMode.MATCH
    @JvmField
    var barStyle = BarStyle.SOLID
    @JvmField
    var renderType = RenderType.AUTO
    @JvmField
    var glowLevel = 0
    @JvmField
    var customColor = 0
    private var randomizeInterval = 0
    @JvmField
    var showInDrawer = false
    @JvmField
    var showWithControllerOnly = false

    private val accel = AccelerateInterpolator()
    private val decel = DecelerateInterpolator()

    private val mVisualizerListener: Visualizer.OnDataCaptureListener
    private val mLinkVisualizer: Runnable
    private val mUnlinkVisualizer: Runnable
    private val randomizeColor: Runnable
    private val paletteResult: PaletteAsyncListener

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private inner class PaletteTask(private val resultListener: PaletteAsyncListener) : AsyncTask<Bitmap, Void, Palette?>() {
        override fun doInBackground(vararg bitmaps: Bitmap?): Palette? {
            return try {
                val bitmap = bitmaps.getOrNull(0) ?: return null
                Palette.from(bitmap).generate()
            } catch (t: Throwable) {
                XposedHelpers.log(t)
                null
            }
        }

        override fun onPostExecute(result: Palette?) {
            result?.let { resultListener.onGenerated(it) }
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)

        val res = context.resources
        mHeight = if (res.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) res.displayMetrics.heightPixels else res.displayMetrics.widthPixels
        mWidth = if (res.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) res.displayMetrics.widthPixels else res.displayMetrics.heightPixels
        mDensity = res.displayMetrics.density
        mColor = Color.TRANSPARENT
        mOpaqueColor = Color.TRANSPARENT

        mPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.MITER
            color = mColor
        }

        animDur = MainModule.mPrefs.getInt("system_visualizer_animdur", 65)
        mFFTPoints = FloatArray(128)
        mRainbow = IntArray(mBandsNum)
        mRainbowVertical = IntArray(mBandsNum)
        mPositions = FloatArray(mBandsNum)
        mValueAnimators = Array(mBandsNum) { i ->
            val j = i * 4 + 3
            ValueAnimator().apply {
                duration = animDur.toLong()
                addUpdateListener { animation ->
                    mFFTPoints[j] = animation.animatedValue as Float
                    postInvalidate()
                }
            }
        }

        showOnCustom = MainModule.mPrefs.getBoolean("system_visualizer_custom")
        transparency = Math.round(255f - 255f * MainModule.mPrefs.getInt("system_visualizer_transp", 40) / 100f)
        colorMode = ColorMode.values()[MainModule.mPrefs.getStringAsInt("system_visualizer_color", 1)]
        barStyle = BarStyle.values()[MainModule.mPrefs.getStringAsInt("system_visualizer_style", 1)]
        renderType = RenderType.values()[MainModule.mPrefs.getStringAsInt("system_visualizer_render", 0)]
        glowLevel = MainModule.mPrefs.getInt("system_visualizer_glowlevel", 50)
        customColor = MainModule.mPrefs.getInt("system_visualizer_colorval", Color.WHITE)
        randomizeInterval = MainModule.mPrefs.getInt("system_visualizer_dyntime", 10) * 1000
        showInDrawer = MainModule.mPrefs.getBoolean("system_visualizer_drawer")
        showWithControllerOnly = MainModule.mPrefs.getBoolean("system_visualizer_controller")

        mHandler = Handler(context.mainLooper)

        mVisualizerListener = object : Visualizer.OnDataCaptureListener {
            private var real: Byte = 0
            private var imaginary: Byte = 0
            private var dbValue = 0
            private var magnitude = 0f

            override fun onWaveFormDataCapture(visualizer: Visualizer, bytes: ByteArray, samplingRate: Int) {}

            override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
                try {
                    val bandWidth = samplingRate.toFloat() / fft.size
                    var band = 0
                    var i = 1
                    val maxHeight = minOf(maxDp * mDensity, mHeight / 2.0f)

                    while (band < mBandsNum && i < fft.size / 2) {
                        magnitude = 0f

                        if (!allZeros(fft)) {
                            while (i < fft.size / 2 && i * bandWidth <= mBands[band] * samplingRate / 44100f) {
                                real = fft[i * 2]
                                imaginary = fft[i * 2 + 1]
                                magnitude = maxOf(magnitude, real * real + imaginary * imaginary + 0f)
                                i++
                            }
                        }

                        dbValue = if (magnitude > 0) (10 * Math.log10(magnitude.toDouble())).toInt() else 0
                        maxDb = maxOf(maxDb, dbValue.toFloat())
                        val oldVal = mFFTPoints[band * 4 + 3]
                        val newVal = mFFTPoints[band * 4 + 1] - maxHeight * dbValue / maxDb

                        mValueAnimators[band].cancel()
                        mValueAnimators[band].interpolator = if (newVal < oldVal) decel else accel
                        mValueAnimators[band].setFloatValues(oldVal, newVal)
                        mValueAnimators[band].start()

                        band++
                    }
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                }
            }
        }

        mLinkVisualizer = Runnable {
            try {
                mVisualizer = Visualizer(0).apply {
                    enabled = false
                    captureSize = Visualizer.getCaptureSizeRange()[1]
                    scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                    setDataCaptureListener(mVisualizerListener, Visualizer.getMaxCaptureRate(), false, true)
                    enabled = true
                }
            } catch (t: Throwable) {
                XposedHelpers.log(t)
            }
        }

        mUnlinkVisualizer = Runnable {
            mVisualizer?.let {
                try {
                    it.enabled = false
                    it.release()
                    mVisualizer = null
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                }
            }
        }

        randomizeColor = Runnable {
            if (colorMode != ColorMode.DYNAMIC) return@Runnable
            setColor(getRandomColor())
            mHandler.removeCallbacks(this@AudioVisualizer.randomizeColor)
            mHandler.postDelayed(this@AudioVisualizer.randomizeColor, randomizeInterval.toLong())
        }

        paletteResult = object : PaletteAsyncListener {
            override fun onGenerated(palette: Palette?) {
                try {
                    var color = Color.TRANSPARENT
                    palette?.let {
                        color = it.getLightVibrantColor(color)
                        if (color == Color.TRANSPARENT) color = it.getVibrantColor(color)
                        if (color == Color.TRANSPARENT) color = it.getDarkVibrantColor(color)
                    }
                    setColor(color)
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                }
            }
        }

        updateBarStyle()
        updateGlowPaint()
        updateRainbowColors()

        ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
            override fun onChange(key: String) {
                try {
                    when (key) {
                        "pref_key_system_visualizer_animdur" -> {
                            animDur = MainModule.mPrefs.getInt("system_visualizer_animdur", 65)
                            for (valueAnimator in mValueAnimators) valueAnimator.duration = animDur.toLong()
                        }
                        "pref_key_system_visualizer_transp" -> {
                            transparency = Math.round(255f - 255f * MainModule.mPrefs.getInt("system_visualizer_transp", 40) / 100f)
                            setColor(mOpaqueColor)
                            updateRainbowColors()
                        }
                        "pref_key_system_visualizer_color" -> {
                            colorMode = ColorMode.values()[MainModule.mPrefs.getStringAsInt("system_visualizer_color", 1)]
                            updateBarStyle()
                            updateColorMode()
                        }
                        "pref_key_system_visualizer_style" -> {
                            barStyle = BarStyle.values()[MainModule.mPrefs.getStringAsInt("system_visualizer_style", 1)]
                            updateBarStyle()
                        }
                        "pref_key_system_visualizer_render" -> {
                            renderType = RenderType.values()[MainModule.mPrefs.getStringAsInt("system_visualizer_render", 0)]
                            updateBarStyle()
                        }
                        "pref_key_system_visualizer_glowlevel" -> {
                            glowLevel = MainModule.mPrefs.getInt("system_visualizer_glowlevel", 50)
                            updateGlowPaint()
                        }
                        "pref_key_system_visualizer_colorval" -> {
                            customColor = MainModule.mPrefs.getInt("system_visualizer_colorval", Color.WHITE)
                            setColor(customColor)
                        }
                        "pref_key_system_visualizer_dyntime" -> {
                            randomizeInterval = MainModule.mPrefs.getInt("system_visualizer_dyntime", 10) * 1000
                            mHandler.removeCallbacks(randomizeColor)
                            mHandler.post(randomizeColor)
                        }
                        "pref_key_system_visualizer_drawer" -> {
                            showInDrawer = MainModule.mPrefs.getBoolean("system_visualizer_drawer", false)
                        }
                        "pref_key_system_visualizer_controller" -> {
                            showWithControllerOnly = MainModule.mPrefs.getBoolean("system_visualizer_controller", false)
                        }
                    }
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                }
            }
        })
    }

    private fun getRandomColor(): Int {
        return Color.HSVToColor(
            floatArrayOf(
                (Math.random() * 360).toFloat(),
                (0.5 + Math.random() * 0.5).toFloat(),
                (0.75 + Math.random() * 0.25).toFloat()
            )
        )
    }

    private fun updateGlowPaint() {
        mGlowPaint = Paint(mPaint)
        if (glowLevel == 0) return
        val scale = glowLevel / 100f
        mGlowPaint.pathEffect = null
        mGlowPaint.maskFilter = BlurMaskFilter(
            15 * mDensity * (1.25f + 0.25f * scale),
            BlurMaskFilter.Blur.NORMAL
        )
        mGlowPaint.alpha = minOf(transparency, 180)
        mGlowPaint.strokeWidth = (0.5f + 1.25f * scale) * mPaint.strokeWidth *
            if (barStyle == BarStyle.LINE) 4f else if (colorMode == ColorMode.RAINBOW_H) 1.15f else 1.3f
        if (barStyle == BarStyle.SOLID || barStyle == BarStyle.DASHED || mGlowPaint.strokeCap == Paint.Cap.ROUND) {
            mGlowPaint.strokeCap = Paint.Cap.SQUARE
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mArt = null
        mProcessedArt = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val barUnit = w / mBandsNum.toFloat()
        val barWidth = barUnit * 0.80f
        mHeight = h
        mWidth = w
        mPaint.strokeWidth = barWidth
        updateBarStyle()

        for (i in 0 until mBandsNum) {
            mFFTPoints[i * 4] = i * barUnit + (barWidth / 2)
            mFFTPoints[i * 4 + 2] = mFFTPoints[i * 4]
            mFFTPoints[i * 4 + 1] = h.toFloat()
            mFFTPoints[i * 4 + 3] = h.toFloat()
        }
    }

    override fun hasOverlappingRendering(): Boolean {
        return mDisplaying
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        try {
            if (mVisualizer?.enabled != true) return
        } catch (_: Throwable) {
            return
        }

        if (barStyle == BarStyle.LINE) {
            mLinePath.reset()
            mLinePath.moveTo(0f, mFFTPoints[3])
            for (i in 1 until mBandsNum) {
                mLinePath.lineTo(
                    if (i == mBandsNum - 1) mWidth.toFloat() else mFFTPoints[i * 4 + 2],
                    mFFTPoints[i * 4 + 3]
                )
            }
            if (glowLevel > 0) canvas.drawPath(mLinePath, mGlowPaint)
            canvas.drawPath(mLinePath, mPaint)
            return
        }

        val drawAsLines = when (renderType) {
            RenderType.LINES -> true
            RenderType.PATH -> false
            else -> glowLevel == 0
        }

        if (drawAsLines) {
            if (glowLevel > 0) canvas.drawLines(mFFTPoints, mGlowPaint)
            canvas.drawLines(mFFTPoints, mPaint)
        } else {
            mLinePath.reset()
            for (i in 0 until mBandsNum) {
                mLinePath.moveTo(mFFTPoints[i * 4], mFFTPoints[i * 4 + 1])
                mLinePath.lineTo(mFFTPoints[i * 4], mFFTPoints[i * 4 + 3])
            }
            if (glowLevel > 0) canvas.drawPath(mLinePath, mGlowPaint)
            canvas.drawPath(mLinePath, mPaint)
        }
    }

    fun setPlaying(playing: Boolean) {
        if (mPlaying != playing) {
            mPlaying = playing
            checkStateChanged()
        }
    }

    fun setBitmap() {
        try {
            if (mProcessedArt == mArt && mArt != null) return
            mProcessedArt = mArt
            if (mProcessedArt != null) {
                PaletteTask(paletteResult).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mProcessedArt)
            } else {
                setColor(Color.TRANSPARENT)
            }
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }

    fun setColor(color: Int) {
        var c = color
        if (c == Color.TRANSPARENT) c = Color.WHITE
        val newColor = Color.argb(transparency, Color.red(c), Color.green(c), Color.blue(c))
        if (mColor == newColor) return
        mColor = newColor
        mOpaqueColor = c
        if (mVisualizer != null) {
            mVisualizerColorAnimator?.cancel()
            mVisualizerColorAnimator = ObjectAnimator.ofArgb(mPaint, "color", mPaint.color, mColor).apply {
                startDelay = Math.round(600 * animDur / 65f).toLong()
                duration = Math.round(1200 * animDur / 65f).toLong()
                start()
            }

            if (glowLevel > 0) {
                mVisualizerGlowColorAnimator?.cancel()
                mVisualizerGlowColorAnimator = ObjectAnimator.ofArgb(mGlowPaint, "color", mGlowPaint.color, mColor).apply {
                    startDelay = Math.round(600 * animDur / 65f).toLong()
                    duration = Math.round(1200 * animDur / 65f).toLong()
                    start()
                }
            }
        } else {
            mPaint.color = mColor
            if (glowLevel > 0) mGlowPaint.color = mColor
        }
    }

    private fun updateColorMode() {
        if (!isMusicPlaying) return
        when (colorMode) {
            ColorMode.MATCH -> setBitmap()
            ColorMode.DYNAMIC -> setColor(getRandomColor())
            ColorMode.STATIC -> setColor(customColor)
            else -> setColor(Color.WHITE)
        }
    }

    private fun updateRainbowColors() {
        val jump = 300f / mBandsNum
        for (i in 0 until mRainbow.size) {
            mRainbow[i] = Color.HSVToColor(transparency, floatArrayOf(jump * i, 1.0f, 1.0f))
        }

        for (i in 0 until mRainbowVertical.size) {
            var h = 140 + jump * i
            if (h > 360) h -= 360
            mRainbowVertical[i] = Color.HSVToColor(transparency, floatArrayOf(h, 1.0f, 1.0f))
        }
    }

    private fun updateBarStyle() {
        when (colorMode) {
            ColorMode.RAINBOW_H -> mPaint.shader = LinearGradient(
                0f, 0f, mWidth.toFloat(), 0f, mRainbow, mPositions, Shader.TileMode.MIRROR
            )
            ColorMode.RAINBOW_V -> {
                val maxHeight = minOf(0.85f * maxDp * mDensity, mHeight / 2.0f)
                mPaint.shader = LinearGradient(
                    0f, mHeight.toFloat(), 0f, mHeight - maxHeight, mRainbowVertical, mPositions, Shader.TileMode.CLAMP
                )
            }
            else -> mPaint.shader = null
        }

        when (barStyle) {
            BarStyle.SOLID -> {
                mPaint.pathEffect = null
                mPaint.strokeCap = Paint.Cap.BUTT
            }
            BarStyle.SOLID_ROUNDED -> {
                mPaint.pathEffect = null
                mPaint.strokeCap = Paint.Cap.ROUND
            }
            BarStyle.DASHED -> {
                mPaint.pathEffect = DashPathEffect(floatArrayOf(4 * mDensity, 2 * mDensity), 0f)
                mPaint.strokeCap = Paint.Cap.BUTT
            }
            BarStyle.CIRCLES -> {
                mPaint.pathEffect = DashPathEffect(floatArrayOf(1.0f, mPaint.strokeWidth + mDensity), 0f)
                mPaint.strokeCap = Paint.Cap.ROUND
            }
            BarStyle.LINE -> {
                mPaint.pathEffect = CornerPathEffect(18 * mDensity)
                mPaint.strokeCap = Paint.Cap.ROUND
                mPaint.strokeWidth = 3 * mDensity
            }
            else -> {}
        }

        updateGlowPaint()
    }

    fun updateViewState(isPlaying: Boolean, isKeyguard: Boolean, isExpanded: Boolean) {
        isMusicPlaying = isPlaying
        isOnKeyguard = isKeyguard
        isExpandedPanel = showInDrawer && !isOnKeyguard && isExpanded
        isOnCustomLockScreen = File("/data/system/theme/lockscreen").exists()
        updatePlaying()
    }

    fun updateScreenOn(isOn: Boolean) {
        isScreenOn = isOn
        updatePlaying()
    }

    fun updateMusicArt(art: Bitmap?) {
        mArt = art
        updateColorMode()
    }

    private fun updatePlaying() {
        setPlaying(isScreenOn && isMusicPlaying && ((isOnKeyguard && (!isOnCustomLockScreen || showOnCustom)) || isExpandedPanel))
    }

    private fun checkStateChanged() {
        if (mPlaying) {
            if (!mDisplaying) {
                mDisplaying = true
                AsyncTask.execute(mLinkVisualizer)
                mHandler.removeCallbacks(randomizeColor)
                mHandler.postDelayed(randomizeColor, randomizeInterval.toLong())
                animate().alpha(1.0f).withEndAction(null).setDuration(Math.round(800 * animDur / 65f).toLong())
            }
        } else {
            if (mDisplaying) {
                mDisplaying = false
                mHandler.removeCallbacks(randomizeColor)
                if (isOnKeyguard) {
                    animate().alpha(0.0f).withEndAction { AsyncTask.execute(mUnlinkVisualizer) }
                        .setDuration(Math.round(600 * animDur / 65f).toLong())
                } else {
                    alpha = 0.0f
                    AsyncTask.execute(mUnlinkVisualizer)
                }
            }
        }
    }
}
