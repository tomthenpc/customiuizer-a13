package name.monwf.customiuizer.prefs

import android.content.Context
import android.content.res.TypedArray
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import name.monwf.customiuizer.R
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.Helpers
import java.util.IllegalFormatException

class SeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr), PreferenceState {

    private var dynamic = false
    private var newmod = false
    private var highlight = false
    private var unsupported = false
    private val childpadding = context.resources.getDimensionPixelSize(R.dimen.preference_item_child_padding)

    private var mDefaultValue: Int
    private var mMinValue: Int
    private var mMaxValue: Int
    private var mStepValue: Int
    private var mNegativeShift: Int
    private val indentLevel: Int

    private var mDisplayDividerValue: Int = 1
    private var mUseDisplayDividerValue = false
    private var mShowPlus = false

    private var mFormat: String? = null
    private var mNote: String? = null
    private var mOffText: String? = null

    private var mSteppedMinValue: Int
    private var mSteppedMaxValue: Int

    private var mValue: TextView? = null
    private var mSeekBar: SeekBar? = null
    private var mListener: SeekBar.OnSeekBarChangeListener? = null

    init {
        if (attrs != null) {
            val xmlAttrs: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference)

            indentLevel = xmlAttrs.getInt(R.styleable.SeekBarPreference_indentLevel, 0)
            dynamic = xmlAttrs.getBoolean(R.styleable.SeekBarPreference_dynamic, false)
            mMinValue = xmlAttrs.getInt(R.styleable.SeekBarPreference_minValue, 0)
            mMaxValue = xmlAttrs.getInt(R.styleable.SeekBarPreference_maxValue, 10)
            mStepValue = xmlAttrs.getInt(R.styleable.SeekBarPreference_stepValue, 1)
            mDefaultValue = xmlAttrs.getInt(R.styleable.SeekBarPreference_android_defaultValue, 0)
            mNegativeShift = xmlAttrs.getInt(R.styleable.SeekBarPreference_negativeShift, 0)
            mShowPlus = xmlAttrs.getBoolean(R.styleable.SeekBarPreference_showplus, false)

            if (xmlAttrs.hasValue(R.styleable.SeekBarPreference_displayDividerValue)) {
                mUseDisplayDividerValue = true
                mDisplayDividerValue = xmlAttrs.getInt(R.styleable.SeekBarPreference_displayDividerValue, 1)
            } else {
                mUseDisplayDividerValue = false
                mDisplayDividerValue = 1
            }

            if (mMinValue < 0) mMinValue = 0
            if (mMaxValue <= mMinValue) mMaxValue = mMinValue + 1

            if (mDefaultValue < mMinValue)
                mDefaultValue = mMinValue
            else if (mDefaultValue > mMaxValue)
                mDefaultValue = mMaxValue

            if (mStepValue <= 0) mStepValue = 1

            mFormat = xmlAttrs.getString(R.styleable.SeekBarPreference_format)
            mNote = xmlAttrs.getString(R.styleable.SeekBarPreference_note)
            mOffText = xmlAttrs.getString(R.styleable.SeekBarPreference_offtext)

            xmlAttrs.recycle()
        } else {
            indentLevel = 0
            mMinValue = 0
            mMaxValue = 10
            mStepValue = 1
            mDefaultValue = 0
            mNegativeShift = 0
        }

        mSteppedMinValue = Math.round(mMinValue.toFloat() / mStepValue)
        mSteppedMaxValue = Math.round(mMaxValue.toFloat() / mStepValue)
        layoutResource = R.layout.preference_seekbar12
    }

    fun getView(finalView: View) {
        val title = finalView.findViewById<TextView>(android.R.id.title) ?: return
        mSeekBar?.alpha = if (isEnabled) 1.0f else 0.75f
        title.text = (getTitle()?.toString() ?: "") + if (unsupported) " ⨯" else if (dynamic) " ⟲" else ""
        if (newmod) Helpers.applyNewMod(title)
        if (highlight) {
            Helpers.applySearchItemHighlight(finalView)
        }

        val hrzPadding = (indentLevel + 1) * childpadding
        finalView.setPadding(hrzPadding, 0, childpadding, 0)
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)

        val summaryView = view.findViewById(android.R.id.summary) as? TextView
        if (TextUtils.isEmpty(getSummary())) {
            summaryView?.visibility = View.GONE
        } else {
            summaryView?.text = getSummary()
        }

        val noteView = view.findViewById(android.R.id.text1) as? TextView
        if (mNote.isNullOrEmpty()) {
            noteView?.visibility = View.GONE
        } else {
            noteView?.text = mNote
        }

        mValue = view.findViewById(R.id.seekbar_value) as? TextView
        mSeekBar = view.findViewById(R.id.seekbar) as? SeekBar
        mSeekBar?.max = mSteppedMaxValue - mSteppedMinValue

        val key = getKey()
        if (key != null) {
            setValue(AppHelper.getIntOfAppPrefs(key, mDefaultValue))
        } else {
            setValue(mDefaultValue)
        }

        mSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mListener?.onStopTrackingTouch(seekBar)
                saveValue()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mListener?.onStartTrackingTouch(seekBar)
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mListener?.onProgressChanged(seekBar, getValue(), fromUser)
                updateDisplay(progress)
            }
        })

        getView(view.itemView)
        view.setDividerAllowedAbove(false)
    }

    fun setOnSeekBarChangeListener(listener: SeekBar.OnSeekBarChangeListener?) {
        mListener = listener
    }

    fun getMinValue(): Int = mMinValue

    fun setMinValue(value: Int) {
        mMinValue = value
        updateAllValues()
    }

    fun getMaxValue(): Int = mMaxValue

    fun setMaxValue(value: Int) {
        mMaxValue = value
        updateAllValues()
    }

    fun getStepValue(): Int = mStepValue

    fun setStepValue(value: Int) {
        mStepValue = value
        updateAllValues()
    }

    fun getFormat(): String? = mFormat

    private fun setFormat(format: String?) {
        mFormat = format
        updateDisplay()
    }

    fun setFormat(formatResId: Int) {
        setFormat(context.getString(formatResId))
    }

    fun getValue(): Int {
        return mSeekBar?.let { (it.progress + mSteppedMinValue) * mStepValue } ?: mDefaultValue
    }

    fun setValue(value: Int) {
        setValue(value, false)
    }

    fun setValue(value: Int, save: Boolean) {
        val bounded = getBoundedValue(value) - mSteppedMinValue
        mSeekBar?.progress = bounded
        updateDisplay(bounded)
        if (save) {
            saveValue()
        }
    }

    fun setDefaultValue(value: Int) {
        mDefaultValue = value
    }

    private fun updateAllValues() {
        var currentValue = getValue()
        if (mMaxValue <= mMinValue) mMaxValue = mMinValue + 1
        mSteppedMinValue = Math.round(mMinValue.toFloat() / mStepValue)
        mSteppedMaxValue = Math.round(mMaxValue.toFloat() / mStepValue)

        mSeekBar?.max = mSteppedMaxValue - mSteppedMinValue

        currentValue = getBoundedValue(currentValue) - mSteppedMinValue

        mSeekBar?.let {
            it.progress = currentValue
            updateDisplay(currentValue)
        }
    }

    private fun getBoundedValue(value: Int): Int {
        var v = Math.round(value.toFloat() / mStepValue)
        if (v < mSteppedMinValue) v = mSteppedMinValue
        if (v > mSteppedMaxValue) v = mSteppedMaxValue
        return v
    }

    private fun updateDisplay() {
        mSeekBar?.progress?.let { updateDisplay(it) }
    }

    private fun updateDisplay(value: Int) {
        val valueView = mValue ?: return
        if (!mFormat.isNullOrEmpty()) {
            valueView.visibility = View.VISIBLE
            var display = (value + mSteppedMinValue) * mStepValue

            if (display == mDefaultValue && mOffText != null) {
                valueView.text = mOffText
                return
            }

            if (mNegativeShift > 0) display -= mNegativeShift

            val text = try {
                if (mUseDisplayDividerValue) {
                    String.format(mFormat!!, display.toFloat() / mDisplayDividerValue)
                } else {
                    String.format(mFormat!!, display)
                }
            } catch (e: IllegalFormatException) {
                e.printStackTrace()
                display.toString()
            }
            valueView.text = if (mShowPlus && display > 0) "+$text" else text
        } else {
            valueView.visibility = View.GONE
        }
    }

    private fun saveValue() {
        val key = getKey() ?: return
        AppHelper.appPrefs!!.edit().putInt(key, getValue()).apply()
    }

    fun setUnsupported(value: Boolean) {
        unsupported = value
        isEnabled = !value
    }

    override fun markAsNew() {
        newmod = true
    }

    override fun applyHighlight() {
        highlight = true
    }
}
