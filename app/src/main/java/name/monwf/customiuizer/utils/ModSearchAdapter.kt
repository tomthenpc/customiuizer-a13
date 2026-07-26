package name.monwf.customiuizer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import name.monwf.customiuizer.R
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList

class ModSearchAdapter(context: Context) : BaseAdapter(), Filterable {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val mFilter = ItemFilter()
    private val modsList = CopyOnWriteArrayList<ModData>()
    private var filterString: String = ""

    @SuppressLint("WrongConstant")
    private fun sortList() {
        modsList.sortWith(
            compareBy<ModData>(
                { it.breadcrumbs?.lowercase() ?: "" },
                { it.title?.lowercase() ?: "" }
            )
        )
    }

    override fun getCount(): Int = modsList.size

    override fun getItem(position: Int): ModData = modsList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = convertView ?: mInflater.inflate(R.layout.pref_item, parent, false)

        val itemTitle = row.findViewById<TextView>(android.R.id.title)
        val itemSummary = row.findViewById<TextView>(android.R.id.summary)

        val ad = getItem(position)
        val title = ad.title ?: ""

        val start = title.lowercase().indexOf(filterString)
        if (start >= 0) {
            val spannable = SpannableString(title)
            spannable.setSpan(
                ForegroundColorSpan(Helpers.markColorVibrant),
                start,
                start + filterString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            itemTitle.setText(spannable, TextView.BufferType.SPANNABLE)
        } else {
            itemTitle.text = title
        }
        itemSummary.text = ad.breadcrumbs

        return row
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            filterString = constraint?.toString()?.lowercase() ?: ""
            val nlist = ArrayList<ModData>()

            for (filterableData in Helpers.allModsList) {
                if (constraint?.toString() == Helpers.NEW_MODS_SEARCH_QUERY) {
                    if (Helpers.newMods.contains(filterableData.key)) nlist.add(filterableData)
                } else if (filterableData.title?.lowercase()?.contains(filterString) == true) {
                    nlist.add(filterableData)
                }
            }

            val results = FilterResults()
            results.values = nlist
            results.count = nlist.size
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            modsList.clear()
            if (results != null && results.count > 0 && results.values != null) {
                modsList.addAll(results.values as ArrayList<ModData>)
            }
            sortList()
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = mFilter
}
