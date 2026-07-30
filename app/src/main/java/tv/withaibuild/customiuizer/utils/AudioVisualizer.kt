package tv.withaibuild.customiuizer.utils

import android.animation.ObjectAnimator
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
import android.os.Handler
import android.os.Process
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.palette.graphics.Palette
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal fun audioVisualizerBandBinLimits(bands: FloatArray, fftSize: Int): IntArray {
    val half = fftSize / 2
    return IntArray(bands.size) { band ->
        ((bands[band] * half / 22050f).toInt() + 1)
            .coerceIn(1, half.coerceAtLeast(1))
    }
}

internal fun shouldDisplayAudioVisualizer(
    playing: Boolean,
    attached: Boolean,
    viewVisible: Boolean,
    windowVisible: Boolean
): Boolean = playing && attached && viewVisible && windowVisible

internal fun isCurrentAudioVisualizerGeneration(
    expected: Long,
    current: Long
): Boolean = expected == current

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
        private val workerNumber = AtomicInteger()

        private fun createWorker(role: String): ThreadPoolExecutor =
            ThreadPoolExecutor(
                1,
                1,
                15L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue(),
                { runnable ->
                    Thread(
                        {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                            runnable.run()
                        },
                        "Pengeek-AudioVisualizer-$role-${workerNumber.incrementAndGet()}"
                    )
                }
            ).apply {
                allowCoreThreadTimeOut(true)
            }

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
    @Volatile
    private var mVisualizer: Visualizer? = null
    private val visualizerLock = Any()
    private var mVisualizerColorAnimator: ObjectAnimator? = null
    private var mVisualizerGlowColorAnimator: ObjectAnimator? = null

    private val mFFTPoints: FloatArray
    private var mFftSize = 0
    private val mBandBinLimits = IntArray(mBandsNum) { Int.MAX_VALUE }
    private val mBandStarts = FloatArray(mBandsNum) { Float.MAX_VALUE }
    private val mBandTargets = FloatArray(mBandsNum) { Float.MAX_VALUE }
    private val mPendingTargets = FloatArray(mBandsNum) { Float.MAX_VALUE }
    private val mComputedTargets = FloatArray(mBandsNum) { Float.MAX_VALUE }
    private val mFrameLock = Any()
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
    @Volatile
    private var mDisplaying = false
    private var mOpaqueColor = 0
    private var mColor = 0
    private val mHandler: Handler
    private var mArt: Bitmap? = null
    private var mProcessedArt: Bitmap? = null
    private var detached = false
    private var viewAttached = false
    private var viewVisible = true
    private var windowVisible = true
    @Volatile
    private var visualizerGeneration = 0L
    @Volatile
    private var paletteGeneration = 0L
    private var paletteFuture: Future<*>? = null
    private val paletteHandlerToken = Any()
    private val visualizerExecutor = createWorker("Visualizer")
    private val paletteExecutor = createWorker("Palette")

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

    @Volatile
    private var mNewDataPending = false
    private var mFrameStartTime = 0L
    @Volatile
    private var mFrameCallbackScheduled = false
    private val mFrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            ModuleHelper.guarded("AudioVisualizer.frameCallback") {
                runFrame(frameTimeNanos)
            }
        }
    }

    private fun runFrame(frameTimeNanos: Long) {
        if (detached || !mDisplaying) {
            mFrameCallbackScheduled = false
            return
        }
        if (mNewDataPending) applyNewData(frameTimeNanos)

        val duration = animDur.coerceAtLeast(1)
        val elapsedMs = (frameTimeNanos - mFrameStartTime) / 1_000_000f
        val fraction = (elapsedMs / duration).coerceIn(0f, 1f)
        var changed = false
        for (band in 0 until mBandsNum) {
            val start = mBandStarts[band]
            val target = mBandTargets[band]
            if (start == Float.MAX_VALUE || target == Float.MAX_VALUE) continue
            val interpolated = if (target < start) {
                decel.getInterpolation(fraction)
            } else {
                accel.getInterpolation(fraction)
            }
            val value = start + (target - start) * interpolated
            val point = band * 4 + 3
            if (mFFTPoints[point] != value) {
                mFFTPoints[point] = value
                changed = true
            }
        }
        if (changed) postInvalidateOnAnimation()

        mFrameCallbackScheduled = false
        if (mNewDataPending || fraction < 1f) startFrameScheduler()
    }

    private fun applyNewData(frameTimeNanos: Long) {
        synchronized(mFrameLock) {
            if (!mNewDataPending) return
            System.arraycopy(mPendingTargets, 0, mBandTargets, 0, mBandsNum)
            mNewDataPending = false
        }
        for (band in 0 until mBandsNum) {
            mBandStarts[band] = mFFTPoints[band * 4 + 3]
        }
        mFrameStartTime = frameTimeNanos
    }

    private fun startFrameScheduler() {
        if (detached || !mDisplaying || mFrameCallbackScheduled) return
        mFrameCallbackScheduled = true
        Choreographer.getInstance().postFrameCallback(mFrameCallback)
    }

    private fun stopFrameScheduler() {
        mFrameCallbackScheduled = false
        Choreographer.getInstance().removeFrameCallback(mFrameCallback)
    }

    private val mVisualizerListener: Visualizer.OnDataCaptureListener
    private val randomizeColor: Runnable
    private val paletteResult: PaletteAsyncListener

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
        for (band in 0 until mBandsNum) {
            mBandStarts[band] = mHeight.toFloat()
            mBandTargets[band] = mHeight.toFloat()
            mPendingTargets[band] = mHeight.toFloat()
            mComputedTargets[band] = mHeight.toFloat()
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
                    if (detached || !mDisplaying) return
                    if (mFftSize != fft.size) computeBandBinLimits(fft.size)

                    val silentFrame = allZeros(fft)
                    var band = 0
                    var i = 1
                    val maxHeight = minOf(maxDp * mDensity, mHeight / 2.0f)

                    while (band < mBandsNum && i < mFftSize / 2) {
                        magnitude = 0f

                        if (!silentFrame) {
                            while (i < mBandBinLimits[band]) {
                                real = fft[i * 2]
                                imaginary = fft[i * 2 + 1]
                                magnitude = maxOf(magnitude, real * real + imaginary * imaginary + 0f)
                                i++
                            }
                        }

                        dbValue = if (magnitude > 0) (10 * Math.log10(magnitude.toDouble())).toInt() else 0
                        maxDb = maxOf(maxDb, dbValue.toFloat())
                        mComputedTargets[band] =
                            mFFTPoints[band * 4 + 1] - maxHeight * dbValue / maxDb
                        band++
                    }

                    synchronized(mFrameLock) {
                        System.arraycopy(mComputedTargets, 0, mPendingTargets, 0, band)
                        mNewDataPending = true
                    }
                    if (!mFrameCallbackScheduled) post { startFrameScheduler() }
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                }
            }
        }

        computeBandBinLimits(Visualizer.getCaptureSizeRange()[1])

        randomizeColor = Runnable {
            ModuleHelper.guarded("AudioVisualizer.randomizeColor") {
                if (colorMode == ColorMode.DYNAMIC && !detached && mDisplaying) {
                    setColor(getRandomColor())
                    mHandler.removeCallbacks(this@AudioVisualizer.randomizeColor)
                    mHandler.postDelayed(
                        this@AudioVisualizer.randomizeColor,
                        randomizeInterval.toLong()
                    )
                }
            }
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
                if (detached) return
                try {
                    when (key) {
                        "pref_key_system_visualizer_animdur" -> {
                            animDur = MainModule.mPrefs.getInt("system_visualizer_animdur", 65)
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

    private fun computeBandBinLimits(fftSize: Int) {
        mFftSize = fftSize
        val limits = audioVisualizerBandBinLimits(mBands, fftSize)
        System.arraycopy(limits, 0, mBandBinLimits, 0, mBandsNum)
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        detached = false
        viewAttached = true
        viewVisible = visibility == VISIBLE
        windowVisible = windowVisibility == VISIBLE
        checkStateChanged()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView !== this) return
        viewVisible = visibility == VISIBLE
        checkStateChanged()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        windowVisible = visibility == VISIBLE
        checkStateChanged()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detached = true
        viewAttached = false
        mDisplaying = false
        stopFrameScheduler()
        mHandler.removeCallbacks(randomizeColor)
        cancelPaletteWork()
        animate().cancel()
        mVisualizerColorAnimator?.cancel()
        mVisualizerGlowColorAnimator?.cancel()
        detachCurrentVisualizer()
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
            mBandStarts[i] = h.toFloat()
            mBandTargets[i] = h.toFloat()
            mPendingTargets[i] = h.toFloat()
            mComputedTargets[i] = h.toFloat()
        }
    }

    override fun hasOverlappingRendering(): Boolean {
        return mDisplaying
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!mDisplaying) return
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

    private fun scheduleVisualizerLink() {
        val generation = ++visualizerGeneration
        visualizerExecutor.execute {
            val candidate = createVisualizer()
            mHandler.post {
                ModuleHelper.guarded("AudioVisualizer.publishVisualizer") {
                    if (
                        candidate == null ||
                        detached ||
                        !mDisplaying ||
                        !isCurrentAudioVisualizerGeneration(generation, visualizerGeneration)
                    ) {
                        scheduleVisualizerRelease(candidate)
                    } else {
                        val previous = synchronized(visualizerLock) {
                            val current = mVisualizer
                            mVisualizer = candidate
                            current
                        }
                        if (previous !== candidate) scheduleVisualizerRelease(previous)
                    }
                }
            }
        }
    }

    private fun createVisualizer(): Visualizer? = try {
        Visualizer(0).apply {
            enabled = false
            captureSize = Visualizer.getCaptureSizeRange()[1]
            scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            setDataCaptureListener(
                mVisualizerListener,
                Visualizer.getMaxCaptureRate(),
                false,
                true
            )
            enabled = true
        }
    } catch (t: Throwable) {
        XposedHelpers.log(t)
        null
    }

    private fun detachCurrentVisualizer() {
        visualizerGeneration++
        val current = synchronized(visualizerLock) {
            val visualizer = mVisualizer
            mVisualizer = null
            visualizer
        }
        scheduleVisualizerRelease(current)
    }

    private fun scheduleVisualizerRelease(visualizer: Visualizer?) {
        if (visualizer == null) return
        visualizerExecutor.execute {
            try {
                visualizer.enabled = false
            } catch (t: Throwable) {
                XposedHelpers.log(t)
            }
            try {
                visualizer.release()
            } catch (t: Throwable) {
                XposedHelpers.log(t)
            }
        }
    }

    fun setBitmap() {
        try {
            if (mProcessedArt == mArt && mArt != null) return
            val art = mArt
            mProcessedArt = art
            cancelPaletteWork()
            if (art == null) {
                setColor(Color.TRANSPARENT)
                return
            }

            val generation = paletteGeneration
            paletteFuture = paletteExecutor.submit {
                val palette = try {
                    Palette.from(art).generate()
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                    null
                }
                if (palette == null) return@submit
                mHandler.postAtTime(
                    {
                        ModuleHelper.guarded("AudioVisualizer.paletteResult") {
                            if (
                                !detached &&
                                isCurrentAudioVisualizerGeneration(generation, paletteGeneration) &&
                                mArt === art &&
                                colorMode == ColorMode.MATCH
                            ) {
                                paletteResult.onGenerated(palette)
                            }
                        }
                    },
                    paletteHandlerToken,
                    SystemClock.uptimeMillis()
                )
            }
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }

    private fun cancelPaletteWork() {
        paletteGeneration++
        paletteFuture?.cancel(true)
        paletteFuture = null
        mHandler.removeCallbacksAndMessages(paletteHandlerToken)
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
        if (colorMode != ColorMode.MATCH) cancelPaletteWork()
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
        cancelPaletteWork()
        mArt = art
        updateColorMode()
    }

    private fun updatePlaying() {
        setPlaying(isScreenOn && isMusicPlaying && ((isOnKeyguard && (!isOnCustomLockScreen || showOnCustom)) || isExpandedPanel))
    }

    private fun checkStateChanged() {
        if (detached) return
        if (shouldDisplayAudioVisualizer(mPlaying, viewAttached, viewVisible, windowVisible)) {
            if (!mDisplaying) {
                mDisplaying = true
                scheduleVisualizerLink()
                mHandler.removeCallbacks(randomizeColor)
                mHandler.postDelayed(randomizeColor, randomizeInterval.toLong())
                animate().alpha(1.0f).withEndAction(null).setDuration(Math.round(800 * animDur / 65f).toLong())
            }
        } else {
            if (mDisplaying) {
                mDisplaying = false
                stopFrameScheduler()
                mHandler.removeCallbacks(randomizeColor)
                detachCurrentVisualizer()
                if (isOnKeyguard) {
                    animate().alpha(0.0f).withEndAction(null)
                        .setDuration(Math.round(600 * animDur / 65f).toLong())
                } else {
                    alpha = 0.0f
                }
            }
        }
    }
}
