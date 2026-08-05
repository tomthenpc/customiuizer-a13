package tv.withaibuild.customiuizer.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.ListPopupWindow
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics

@SuppressLint("ResourceType")
open class SpinnerEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatSpinner(context, attrs) {

    @JvmField
    var entries: Array<CharSequence>? = null
    @JvmField
    var entryValues: IntArray? = null
    private val disabledItems = ArrayList<Int>()
    private val res: Resources = context.resources
    private val childpadding = res.getDimensionPixelSize(R.dimen.preference_item_child_padding)

    init {
        val xmlAttrs = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.entries, R.attr.entryValues))
        entries = xmlAttrs.getTextArray(0)
        val entryValuesResId = xmlAttrs.getResourceId(1, 0)
        if (entryValuesResId != 0) {
            entryValues = resources.getIntArray(entryValuesResId)
        }
        xmlAttrs.recycle()
        setPadding(childpadding, 0, childpadding, 0)

        try {
            val mPopup = AppCompatSpinner::class.java.getDeclaredField("mPopup")
            mPopup.isAccessible = true
            val popupWindow = mPopup.get(this) as? ListPopupWindow
            val scale = resources.displayMetrics.density
            popupWindow?.setHeight((40 * 10 * scale).toInt())
        } catch (e: Throwable) {
            if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
            SettingsDiagnostics.failure("SpinnerEx.configurePopupHeight", e)
        }
    }

    private fun findIndex(valInt: Int, vals: IntArray?): Int {
        vals ?: return -1
        return vals.indexOf(valInt)
    }

    fun init(valInt: Int) {
        val currentEntries = entries ?: return
        val currentValues = entryValues ?: return
        val newAdapter = ArrayAdapterEx(context, android.R.layout.simple_spinner_item, currentEntries)
        adapter = newAdapter
        setSelection(findIndex(valInt, currentValues))
    }

    fun getSelectedArrayValue(): Int {
        return entryValues?.get(selectedItemPosition) ?: 0
    }

    private inner class ArrayAdapterEx(
        context: Context,
        resource: Int,
        objects: Array<CharSequence>
    ) : ArrayAdapter<CharSequence>(context, resource, objects) {

        override fun isEnabled(position: Int): Boolean {
            return !disabledItems.contains(position)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            view.isEnabled = isEnabled(position)
            return view
        }
    }
}
