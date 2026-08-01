package tv.withaibuild.customiuizer.prefs

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.utils.Helpers

class CheckBoxPreferenceEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceStyle
) : SwitchPreference(context, attrs, defStyleAttr), PreferenceState {

    companion object {
        private const val PRESSED_ALPHA = 0.72f
        private const val PRESS_IN_DURATION_MS = 60L
        private const val PRESS_OUT_DURATION_MS = 120L

        private val pressFeedbackListener = View.OnTouchListener { itemView, event ->
            when {
                event.actionMasked == MotionEvent.ACTION_DOWN && itemView.isEnabled -> {
                    itemView.animate().cancel()
                    itemView.animate().alpha(PRESSED_ALPHA).setDuration(PRESS_IN_DURATION_MS).start()
                }
                event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL -> {
                    itemView.animate().cancel()
                    itemView.animate().alpha(1f).setDuration(PRESS_OUT_DURATION_MS).start()
                }
            }
            false
        }
    }

    private val res: Resources = context.resources
    private val childpadding = res.getDimensionPixelSize(R.dimen.preference_item_child_padding)
    private val indentLevel: Int
    private val dynamic: Boolean
    private var newmod = false
    private var highlight = false
    private var unsupported = false

    init {
        val xmlAttrs: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CheckBoxPreferenceEx)
        dynamic = xmlAttrs.getBoolean(R.styleable.CheckBoxPreferenceEx_dynamic, false)
        indentLevel = xmlAttrs.getInt(R.styleable.CheckBoxPreferenceEx_indentLevel, 0)
        xmlAttrs.recycle()
        isIconSpaceReserved = false
    }

    fun getView(finalView: View) {
        val title = finalView.findViewById<TextView>(android.R.id.title) ?: return
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
        view.itemView.animate().cancel()
        view.itemView.alpha = 1f
        view.itemView.setOnTouchListener(pressFeedbackListener)
        val title = view.findViewById(android.R.id.title) as? TextView ?: return
        title.maxLines = 3
        getView(view.itemView)
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
