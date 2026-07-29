package tv.withaibuild.customiuizer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import tv.withaibuild.customiuizer.R
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.Comparator
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class LockedAppAdapter @SuppressLint("WrongConstant") constructor(
    context: Context,
    arr: ArrayList<AppData>
) : BaseAdapter(), Filterable {

    private val ctx: Context = context
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val pool: ThreadPoolExecutor
    private val mFilter = ItemFilter()
    private val originalAppList: ArrayList<AppData> = arr
    private val filteredAppList = CopyOnWriteArrayList<AppData>()
    private var mSecurityManager: Any? = null
    private var getApplicationAccessControlEnabledAsUser: Method? = null

    init {
        filteredAppList.addAll(arr)
        val cpuCount = Runtime.getRuntime().availableProcessors()
        pool = ThreadPoolExecutor(
            cpuCount + 1,
            cpuCount * 2 + 1,
            2,
            TimeUnit.SECONDS,
            LinkedBlockingQueue()
        )

        try {
            mSecurityManager = context.getSystemService("security")
            val sm = mSecurityManager
            if (sm != null) {
                val method = sm::class.java.getDeclaredMethod(
                    "getApplicationAccessControlEnabledAsUser",
                    String::class.java,
                    Int::class.javaPrimitiveType
                )
                method.isAccessible = true
                getApplicationAccessControlEnabledAsUser = method
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        sortList()
    }

    private fun sortList() {
        filteredAppList.sortWith(Comparator { app1, app2 ->
            try {
                val sm = mSecurityManager
                val method = getApplicationAccessControlEnabledAsUser
                if (sm == null || method == null) return@Comparator 0

                val app1checked = method.invoke(
                    sm,
                    app1.pkgName ?: "",
                    app1.user
                ) as? Boolean ?: false
                val app2checked = method.invoke(
                    sm,
                    app2.pkgName ?: "",
                    app2.user
                ) as? Boolean ?: false

                when {
                    app1checked && app2checked -> 0
                    app1checked -> -1
                    app2checked -> 1
                    else -> 0
                }
            } catch (t: Throwable) {
                0
            }
        })
    }

    override fun isEnabled(position: Int): Boolean {
        val ad = getItem(position)
        return ad.pkgName != "com.miui.securitycenter"
    }

    override fun getCount(): Int {
        return filteredAppList.size
    }

    override fun getItem(position: Int): AppData {
        return filteredAppList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row: View = if (convertView != null) {
            convertView
        } else {
            mInflater.inflate(R.layout.applist_item11, parent, false)
        }

        val itemIsDis: ImageView = row.findViewById(R.id.icon_disable)
        val itemIsDual: ImageView = row.findViewById(R.id.icon_dual)
        val itemChecked: CheckBox = row.findViewById(android.R.id.checkbox)
        Helpers.setMiuiCheckbox(itemChecked)
        val itemTitle: TextView = row.findViewById(android.R.id.title)
        val itemIcon: ImageView = row.findViewById(android.R.id.icon)

        val ad = getItem(position)
        itemIcon.tag = position
        itemTitle.text = ad.label ?: ""
        itemIsDis.visibility = if (ad.enabled) View.GONE else View.VISIBLE
        itemIsDual.visibility = if (ad.user != 0) View.VISIBLE else View.GONE

        val icon: Bitmap? = Helpers.memoryCache.get(ad.pkgName + "|" + ad.actName)
        if (icon == null) {
            val dualIcon = arrayOf(ctx.resources.getDrawable(R.drawable.card_icon_default, ctx.theme))
            val crossfader = android.graphics.drawable.TransitionDrawable(dualIcon)
            crossfader.isCrossFadeEnabled = true
            itemIcon.setImageDrawable(crossfader)
            BitmapCachedLoader(itemIcon, ad, ctx).executeOnExecutor(pool)
        } else {
            itemIcon.setImageBitmap(icon)
        }

        try {
            val sm = mSecurityManager
            val method = getApplicationAccessControlEnabledAsUser
            itemChecked.visibility = View.VISIBLE
            itemChecked.isChecked = if (sm != null && method != null) {
                method.invoke(sm, ad.pkgName ?: "", ad.user) as? Boolean ?: false
            } else {
                false
            }
        } catch (t: Throwable) {
            itemChecked.visibility = View.GONE
        }

        val enabled = ad.pkgName != "com.miui.securitycenter"
        itemIcon.alpha = if (enabled) 1.0f else 0.5f
        itemTitle.alpha = if (enabled) 1.0f else 0.5f
        itemChecked.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        row.isEnabled = enabled

        return row
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
            val filterString = constraint?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val results = Filter.FilterResults()

            val count = originalAppList.size
            val nlist = ArrayList<AppData>()

            for (i in 0 until count) {
                val filterableData = originalAppList[i]
                if ((filterableData.label ?: "").lowercase(Locale.getDefault()).contains(filterString)) {
                    nlist.add(filterableData)
                }
            }

            results.values = nlist
            results.count = nlist.size
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) {
            filteredAppList.clear()
            if ((results?.count ?: 0) > 0 && results?.values != null) {
                filteredAppList.addAll(results.values as? ArrayList<AppData> ?: emptyList())
            }
            sortList()
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter {
        return mFilter
    }
}
