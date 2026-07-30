package tv.withaibuild.customiuizer.utils

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
import tv.withaibuild.customiuizer.R
import java.util.ArrayList
import java.util.Locale

class ModSearchAdapter(context: Context) : BaseAdapter(), Filterable {

    private val inflater = LayoutInflater.from(context)
    private val itemFilter = ItemFilter()
    private var modsList: List<ModData> = emptyList()
    private var filterString = ""

    override fun getCount(): Int = modsList.size

    override fun getItem(position: Int): ModData = modsList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = convertView ?: inflater.inflate(R.layout.pref_item, parent, false)
        val itemTitle = row.findViewById<TextView>(android.R.id.title)
        val itemSummary = row.findViewById<TextView>(android.R.id.summary)
        val mod = getItem(position)
        val title = mod.title.orEmpty()

        val start = mod.titleSearchKey.indexOf(filterString)
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
        itemSummary.text = mod.breadcrumbs
        return row
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString().orEmpty()
            val querySearchKey = query.lowercase(Locale.ROOT)
            val newModsOnly = query == Helpers.NEW_MODS_SEARCH_QUERY
            val source = Helpers.allModsList
            val matches = ArrayList<ModData>(
                if (querySearchKey.isEmpty()) source.size else 16
            )

            for (mod in source) {
                val matchesQuery =
                    if (newModsOnly) Helpers.newMods.contains(mod.key)
                    else mod.titleSearchKey.contains(querySearchKey)
                if (matchesQuery) matches.add(mod)
            }

            return FilterResults().apply {
                values = matches
                count = matches.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filterString = constraint?.toString()?.lowercase(Locale.ROOT).orEmpty()
            modsList = results?.values as? ArrayList<ModData> ?: emptyList()
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = itemFilter
}
