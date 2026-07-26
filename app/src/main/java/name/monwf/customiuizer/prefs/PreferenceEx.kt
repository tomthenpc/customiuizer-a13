package name.monwf.customiuizer.prefs

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import name.monwf.customiuizer.R
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.Helpers

class PreferenceEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr), PreferenceState {

    private val res: Resources = context.resources
    private val secondary = res.getColor(R.color.preference_secondary_text, context.theme)
    private val disableColor = res.getColor(R.color.preference_primary_text_disable, context.theme)
    private val childpadding = res.getDimensionPixelSize(R.dimen.preference_item_child_padding)

    private val indentLevel: Int
    private val dynamic: Boolean
    private val warning: Boolean
    private val countAsSummary: Boolean
    private val longClickable: Boolean
    private var customSummary: String? = null
    private var newmod = false
    private var highlight = false
    private var unsupported = false
    private var longPressListener: View.OnLongClickListener? = null

    init {
        val xmlAttrs: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.PreferenceEx)
        dynamic = xmlAttrs.getBoolean(R.styleable.PreferenceEx_dynamic, false)
        indentLevel = xmlAttrs.getInt(R.styleable.PreferenceEx_indentLevel, 0)
        warning = xmlAttrs.getBoolean(R.styleable.PreferenceEx_warning, false)
        countAsSummary = xmlAttrs.getBoolean(R.styleable.PreferenceEx_countAsSummary, false)
        longClickable = xmlAttrs.getBoolean(R.styleable.PreferenceEx_longClickable, false)
        xmlAttrs.recycle()
        isIconSpaceReserved = false
    }

    fun getView(finalView: View) {
        val title = finalView.findViewById<TextView>(android.R.id.title) ?: return
        val summaryView = finalView.findViewById<TextView>(android.R.id.summary) ?: return
        val valSummary = finalView.findViewById<TextView>(android.R.id.hint) ?: return

        val summaryText = getSummary()
        summaryView.visibility = if (customSummary != null || countAsSummary || summaryText.isNullOrEmpty()) View.GONE else View.VISIBLE
        valSummary.visibility = if (customSummary != null || countAsSummary) View.VISIBLE else View.GONE
        if (customSummary != null || countAsSummary) {
            valSummary.setTextColor(if (isEnabled) secondary else disableColor)
        }
        when {
            customSummary != null -> valSummary.text = customSummary
            countAsSummary -> {
                val count = AppHelper.getStringSetOfAppPrefs(key, LinkedHashSet()).orEmpty().size +
                    AppHelper.getStringSetOfAppPrefs("${key}_black", LinkedHashSet()).orEmpty().size
                valSummary.text = count.toString()
            }
            else -> valSummary.text = null
        }
        if (warning) {
            title.setTextColor(Helpers.markColor)
        }
        title.text = (getTitle()?.toString() ?: "") + if (unsupported) " ⨯" else if (dynamic) " ⟲" else ""
        if (newmod) Helpers.applyNewMod(title)
        if (highlight) {
            Helpers.applySearchItemHighlight(finalView)
        }

        val hrzPadding = (indentLevel + 1) * childpadding
        finalView.setPadding(hrzPadding, 0, childpadding, 0)
        if (longClickable) {
            finalView.setOnLongClickListener {
                longPressListener?.onLongClick(finalView) ?: false
            }
        }
    }

    fun setLongPressListener(ll: View.OnLongClickListener?) {
        longPressListener = ll
    }

    fun setCustomSummary(text: String?) {
        customSummary = text
        notifyChanged()
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)

        val title = view.findViewById(android.R.id.title) as? TextView ?: return
        title.maxLines = 3

        val summary = view.findViewById(android.R.id.summary) as? TextView ?: return

        val valSummary = view.itemView.findViewById<TextView>(android.R.id.hint)
            ?: TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, summary.textSize)
                setPadding(summary.paddingLeft, summary.paddingTop, res.getDimensionPixelSize(R.dimen.preference_summary_padding_right), summary.paddingBottom)
                id = android.R.id.hint
                (view.itemView as ViewGroup).addView(this, 2)
            }

        getView(view.itemView)
    }

    override fun markAsNew() {
        newmod = true
    }

    override fun applyHighlight() {
        highlight = true
    }

    fun setUnsupported(value: Boolean) {
        unsupported = value
        isEnabled = !value
    }
}
