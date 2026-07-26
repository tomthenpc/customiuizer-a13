package name.monwf.customiuizer.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import name.monwf.customiuizer.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class ColorCircle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var posXin: Float = 0f
    private var posYin: Float = 0f
    private var radius: Float = 0f
    private var innerRadius: Float = 0f
    private var offset: Int = 0
    private var alphaVal: Int = 0
    private val paint1 = Paint()
    private val paint1a = Paint()
    private val paint2 = Paint()
    private val paint3 = Paint()
    private var listener: ColorListener? = null
    private var mTransparent = false
    private val mColor = FloatArray(3)
    private var initialized = false

    init {
        isSaveEnabled = true
    }

    val color: Int
        get() = if (mTransparent) {
            Color.TRANSPARENT
        } else {
            val resColor = Color.HSVToColor(mColor)
            (alphaVal shl 24) or (resColor and 0x00FFFFFF)
        }

    fun setColor(color: Int) {
        setColor(color, false)
    }

    fun setColor(color: Int, setAlpha: Boolean) {
        mTransparent = color == Color.TRANSPARENT
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), mColor)
        if (setAlpha) {
            alphaVal = Color.alpha(color)
        }
        listener?.onColorSelected(color)
        val coords = getPointForColor()
        updatePickerPos(coords.x, coords.y)
        postInvalidate()
    }

    fun setAlphaVal(alphaV: Int) {
        alphaVal = alphaV
        listener?.onColorSelected(color)
    }

    private fun update() {
        val diameter = min(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        ) * if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.7f else 0.8f
        radius = diameter / 2.0f
        innerRadius = radius - offset * 2
        layoutParams.width = diameter.toInt()
        layoutParams.height = diameter.toInt()

        val steps = 6
        val colors = IntArray(steps + 1)
        val hsv = floatArrayOf(0f, 1f, 1f)
        for (i in 0 until steps) {
            hsv[0] = (360f / steps) * i
            colors[i] = Color.HSVToColor(hsv)
        }
        colors[steps] = colors[0]

        paint1.shader = SweepGradient(radius, radius, colors, null)
        paint1a.shader = RadialGradient(
            radius,
            radius,
            radius,
            0xFFFFFFFF.toInt(),
            0x00FFFFFF,
            android.graphics.Shader.TileMode.CLAMP
        )
    }

    fun getPointForColor(): PointF {
        val hue = mColor[0]
        val sat = mColor[1]
        val rad = Math.toRadians(hue.toDouble()).toFloat()
        return PointF(
            (radius + radius * sat * cos(rad)).toFloat(),
            (radius + radius * sat * sin(rad)).toFloat()
        )
    }

    fun getColorForPoint(x: Int, y: Int) {
        val dx = x - radius
        val dy = y - radius
        mColor[0] = ((Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f).toFloat()
        mColor[1] = (hypot(dx, dy) / radius).toFloat().coerceIn(0f, 1f)
        mTransparent = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        update()
        postInvalidate()
    }

    fun init(prefColor: Int) {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        paint1.isAntiAlias = true
        paint1a.isAntiAlias = true
        paint2.isAntiAlias = true
        paint2.color = Color.CYAN
        paint2.strokeWidth = 2.0f
        paint2.style = Paint.Style.STROKE
        paint3.isAntiAlias = true
        paint3.color = Color.WHITE
        paint3.style = Paint.Style.FILL_AND_STROKE
        offset = resources.getDimensionPixelSize(R.dimen.screen_color_preview_offset)
        mTransparent = prefColor == Color.TRANSPARENT
        alphaVal = Color.alpha(prefColor)
        Color.RGBToHSV(Color.red(prefColor), Color.green(prefColor), Color.blue(prefColor), mColor)
        update()
        val coords = getPointForColor()
        updatePickerPos(coords.x, coords.y)
        initialized = true
        postInvalidate()
    }

    fun setValue(value: Float) {
        mTransparent = false
        mColor[2] = value
        listener?.onColorSelected(color)
        postInvalidate()
    }

    interface ColorListener {
        fun onColorSelected(color: Int)
    }

    fun setListener(colorListener: ColorListener?) {
        listener = colorListener
    }

    private fun distanceToCenter(f: Float, f2: Float): Float {
        return hypot((radius - f).toDouble(), (radius - f2).toDouble()).toFloat()
    }

    private fun isInCircle(f: Float, f2: Float, radius: Float): Boolean {
        return distanceToCenter(f, f2) <= radius
    }

    private fun limitByCircle(f: Float, f2: Float, radius: Float) {
        val angle = atan2((f - radius).toDouble(), (f2 - radius).toDouble()).toFloat()
        posXin = radius + (radius * sin(angle)) + offset * 2
        posYin = radius + (radius * cos(angle)) + offset * 2
    }

    private fun updatePickerPos(x: Float, y: Float) {
        if (isInCircle(x, y, innerRadius)) {
            posXin = x
            posYin = y
        } else {
            limitByCircle(x, y, innerRadius)
        }
    }

    public override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (!initialized) return false

        parent.requestDisallowInterceptTouchEvent(true)
        if (!isEnabled) return true

        val x = motionEvent.x
        val y = motionEvent.y
        updatePickerPos(x, y)
        getColorForPoint(x.toInt(), y.toInt())
        listener?.onColorSelected(color)
        postInvalidate()

        return true
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!initialized) return
        canvas.drawCircle(radius, radius, radius - offset * 2, paint1)
        canvas.drawCircle(radius, radius, radius - offset * 2, paint1a)
        canvas.drawCircle(posXin, posYin, offset.toFloat(), paint2)
        canvas.drawCircle(posXin, posYin, (offset - 2).toFloat(), paint3)
    }
}
