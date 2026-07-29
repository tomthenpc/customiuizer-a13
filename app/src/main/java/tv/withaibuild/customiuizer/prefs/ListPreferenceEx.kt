package tv.withaibuild.customiuizer.prefs

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.utils.Helpers

class ListPreferenceEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle
) : ListPreference(context, attrs, defStyleAttr), PreferenceState {

    private var sValue: CharSequence? = null
    private val res: Resources = context.resources
    private val childpadding = res.getDimensionPixelSize(R.dimen.preference_item_child_padding)
    private val secondary = res.getColor(R.color.preference_secondary_text, context.theme)
    private val disableColor = res.getColor(R.color.preference_primary_text_disable, context.theme)
    private val indentLevel: Int
    private val dynamic: Boolean
    private var newmod = false
    private var highlight = false
    private var unsupported = false
    private val valueAsSummary: Boolean

    init {
        val xmlAttrs: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ListPreferenceEx)
        indentLevel = xmlAttrs.getInt(R.styleable.ListPreferenceEx_indentLevel, 0)
        dynamic = xmlAttrs.getBoolean(R.styleable.ListPreferenceEx_dynamic, false)
        valueAsSummary = xmlAttrs.getBoolean(R.styleable.ListPreferenceEx_valueAsSummary, false)
        xmlAttrs.recycle()
        isIconSpaceReserved = false
    }

    override fun setValue(value: String) {
        super.setValue(value)
        val index = findIndexOfValue(value)
        val entries = getEntries()
        if (index < 0 || entries == null || index >= entries.size) return
        sValue = entries[index]
    }

    fun setUnsupported(value: Boolean) {
        unsupported = value
        isEnabled = !value
    }

    fun getView(finalView: View) {
        val title = finalView.findViewById<TextView>(android.R.id.title) ?: return
        val summary = finalView.findViewById<TextView>(android.R.id.summary) ?: return
        val valSummary = finalView.findViewById<TextView>(android.R.id.hint) ?: return

        val summaryText = getSummary()
        summary.visibility = if (valueAsSummary || summaryText.isNullOrEmpty()) View.GONE else View.VISIBLE
        valSummary.visibility = if (valueAsSummary) View.VISIBLE else View.GONE
        valSummary.text = if (valueAsSummary) sValue else ""
        if (valueAsSummary) {
            valSummary.setTextColor(if (isEnabled) secondary else disableColor)
        }
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
}
