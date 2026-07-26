package name.monwf.customiuizer.prefs

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import name.monwf.customiuizer.R
import name.monwf.customiuizer.utils.Helpers

class CheckBoxPreferenceEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SwitchPreference(context, attrs, defStyleAttr), PreferenceState {

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
