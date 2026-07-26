package name.monwf.customiuizer.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.TransitionDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import name.monwf.customiuizer.R
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ResolveInfoAdapter(context: Context, arr: ArrayList<ResolveInfo>) : BaseAdapter(), Filterable {

    private val ctx: Context = context
    private val pm: PackageManager = ctx.packageManager
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val pool: ThreadPoolExecutor
    private val mFilter = ItemFilter()
    private val originalAppList = CopyOnWriteArrayList<ResolveInfo>()
    private val filteredAppList = CopyOnWriteArrayList<ResolveInfo>()

    init {
        originalAppList.addAll(arr)
        filteredAppList.addAll(arr)
        val cpuCount = Runtime.getRuntime().availableProcessors()
        pool = ThreadPoolExecutor(
            cpuCount + 1,
            cpuCount * 2 + 1,
            2L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue()
        )
    }

    override fun getCount(): Int = filteredAppList.size

    override fun getItem(position: Int): ResolveInfo = filteredAppList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = convertView ?: mInflater.inflate(R.layout.applist_item11, parent, false)

        val itemIsDis = row.findViewById<ImageView>(R.id.icon_disable)
        val itemTitle = row.findViewById<TextView>(android.R.id.title)
        val itemIcon = row.findViewById<ImageView>(android.R.id.icon)

        val ri = getItem(position)
        itemIcon.tag = position

        val ad = AppData().apply {
            pkgName = ri.activityInfo.applicationInfo.packageName
            actName = ri.activityInfo.name
            enabled = ri.activityInfo.enabled
            label = ri.loadLabel(pm).toString()
        }

        itemTitle.text = ad.label
        itemIsDis.visibility = if (ad.enabled) View.INVISIBLE else View.VISIBLE
        val icon = Helpers.memoryCache.get(ad.pkgName + "|" + ad.actName)

        if (icon == null) {
            val dualIcon = arrayOf(ctx.resources.getDrawable(R.drawable.card_icon_default, ctx.theme))
            val crossfader = TransitionDrawable(dualIcon)
            crossfader.isCrossFadeEnabled = true
            itemIcon.setImageDrawable(crossfader)
            BitmapCachedLoader(itemIcon, ad, ctx).executeOnExecutor(pool)
        } else {
            itemIcon.setImageBitmap(icon)
        }

        return row
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterString = constraint?.toString()?.lowercase() ?: ""
            val results = FilterResults()

            val nlist = ArrayList<ResolveInfo>()
            for (filterableData in originalAppList) {
                if (filterableData.loadLabel(pm).toString().lowercase().contains(filterString)) {
                    nlist.add(filterableData)
                }
            }

            results.values = nlist
            results.count = nlist.size
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredAppList.clear()
            if (results != null && results.count > 0 && results.values != null) {
                filteredAppList.addAll(results.values as ArrayList<ResolveInfo>)
            }
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = mFilter
}
