package name.monwf.customiuizer.subs

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import name.monwf.customiuizer.R
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.ColorCircle
import name.monwf.customiuizer.utils.Helpers
import java.util.Locale

class ColorSelector : SubFragment() {

    private var key: String? = null
    private var colorCircle: ColorCircle? = null
    private var white: TextView? = null
    private var black: TextView? = null
    private var auto: TextView? = null
    private var selectedColorHint: TextView? = null
    private var selectedColorView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        padded = false
        super.onCreate(savedInstanceState)
    }

    private fun updateSelColor(color: Int) {
        val view = selectedColorView ?: return
        val background = view.background as? GradientDrawable ?: return
        background.colors = if (color == Color.TRANSPARENT) {
            intArrayOf(Color.WHITE, Color.BLACK)
        } else {
            intArrayOf(color, color)
        }
        selectedColorHint?.text = String.format(Locale.US, "#%08X", color)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        key = arguments?.getString("key")
        val view = view ?: return

        selectedColorView = view.findViewById(R.id.selected_color)
        selectedColorView?.setBackgroundResource(R.drawable.rounded_corners)
        selectedColorHint = view.findViewById(R.id.selected_color_hint)
        colorCircle = view.findViewById(R.id.color_circle)
        colorCircle?.tag = key

        val prefColor = key?.let { AppHelper.getIntOfAppPrefs(it, Color.WHITE) } ?: Color.WHITE
        colorCircle?.init(prefColor)
        colorCircle?.setListener(object : ColorCircle.ColorListener {
            override fun onColorSelected(color: Int) = updateSelColor(color)
        })

        if (savedInstanceState != null) {
            val savedColor = savedInstanceState.getInt("colorCircleColor")
            colorCircle?.setColor(savedColor, true)
        }

        val currentColor = colorCircle?.color ?: prefColor
        updateSelColor(currentColor)

        val hsvBar = view.findViewById<SeekBar>(R.id.hsv_value)
        hsvBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                colorCircle?.setValue(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        val hsv = FloatArray(3)
        Color.RGBToHSV(Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor), hsv)
        hsvBar?.setProgress((hsv[2] * 100).toInt(), false)

        val alphaBar = view.findViewById<SeekBar>(R.id.alpha_value)
        alphaBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                colorCircle?.setAlphaVal(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        alphaBar?.setProgress(Color.alpha(currentColor), false)

        white = view.findViewById(R.id.white_color)
        black = view.findViewById(R.id.black_color)
        auto = view.findViewById(R.id.auto_color)

        // Original Java re-finds white before setting listeners; keep the same order.
        white = view.findViewById(R.id.white_color)
        white?.isSelected = currentColor == Color.WHITE
        white?.setOnClickListener {
            setSelected(1)
            colorCircle?.setColor(Color.WHITE)
            hsvBar?.setProgress(100, false)
        }

        black = view.findViewById(R.id.black_color)
        black?.isSelected = currentColor == Color.BLACK
        black?.setOnClickListener {
            setSelected(2)
            colorCircle?.setColor(Color.BLACK)
            hsvBar?.setProgress(0, false)
        }

        auto = view.findViewById(R.id.auto_color)
        if (key?.contains("pref_key_system_batteryindicator") == true) auto?.visibility = View.VISIBLE
        auto?.isSelected = currentColor == Color.TRANSPARENT
        auto?.setOnClickListener {
            setSelected(3)
            colorCircle?.setColor(Color.TRANSPARENT)
            hsvBar?.setProgress(0, false)
        }

        selectedColorHint?.setOnClickListener {
            AppHelper.showInputDialog(
                requireActivity(),
                selectedColorHint?.text?.toString().orEmpty(),
                R.string.array_static,
                0,
                1,
                object : Helpers.InputCallback {
                    override fun onInputFinished(key: String?, text: String?) {
                        if (key != null && !text.isNullOrBlank()) {
                            try {
                                text.let { colorCircle?.setColor(Color.parseColor(it), true) }
                            } catch (_: IllegalArgumentException) {}
                        }
                    }
                },
                false
            )
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        colorCircle?.let { savedInstanceState.putInt("colorCircleColor", it.color) }
        super.onSaveInstanceState(savedInstanceState)
    }

    private fun setSelected(btn: Int) {
        white?.isSelected = false
        black?.isSelected = false
        auto?.isSelected = false
        when (btn) {
            1 -> white?.isSelected = true
            2 -> black?.isSelected = true
            3 -> auto?.isSelected = true
        }
    }
}
