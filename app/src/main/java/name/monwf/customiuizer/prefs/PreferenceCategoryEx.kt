package name.monwf.customiuizer.prefs

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import name.monwf.customiuizer.R

class PreferenceCategoryEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceCategoryStyle
) : PreferenceCategory(context, attrs, defStyleAttr) {

    private val dynamic: Boolean
    private var state: Int = 0 // 0-正常 1-纯区块 2-顶层隐藏
    private var unsupported = false
    private val res: Resources = context.resources
    private val childpadding = res.getDimensionPixelSize(R.dimen.preference_item_child_padding)

    init {
        val xmlAttrs: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.PreferenceCategoryEx)
        dynamic = xmlAttrs.getBoolean(R.styleable.PreferenceCategoryEx_dynamic, false)
        state = xmlAttrs.getInt(R.styleable.PreferenceCategoryEx_state, 0)
        xmlAttrs.recycle()
        layoutResource = R.layout.preference_category
    }

    override fun onPrepareAddPreference(preference: Preference): Boolean {
        preference.onParentChanged(this, shouldDisableDependents())
        return true
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)
        val title = view.findViewById(android.R.id.title) as? TextView ?: return
        title.text = (getTitle()?.toString() ?: "") + if (unsupported) " ⨯" else if (dynamic) " ⟲" else ""
        title.visibility = if (state == 2 || state == 1) View.GONE else View.VISIBLE
        val finalView = view.itemView
        if (state == 2) {
            finalView.setPadding(childpadding, 0, childpadding, 0)
        } else {
            val verticalPadding = context.resources.getDimensionPixelSize(R.dimen.preference_item_padding_top)
            finalView.setPadding(childpadding, verticalPadding, childpadding, verticalPadding)
        }
    }

    fun setUnsupported(value: Boolean) {
        unsupported = value
        isEnabled = !value
    }

    fun isDynamic(): Boolean = dynamic

    fun hide() {
        state = 2
        notifyChanged()
    }
}
