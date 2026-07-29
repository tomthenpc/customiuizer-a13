@file:Suppress("DEPRECATION")
package tv.withaibuild.customiuizer.utils

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import tv.withaibuild.customiuizer.R
import java.util.ArrayList
import java.util.Comparator
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AppDataAdapter : BaseAdapter, Filterable {
    private val ctx: Context
    private val mInflater: LayoutInflater
    private val pool: ThreadPoolExecutor
    private val mFilter = ItemFilter()
    private val originalAppList = ArrayList<AppData>()
    private val filteredAppList = CopyOnWriteArrayList<AppData>()
    private var key: String? = null
    private var selectedApp: String? = null
    private var bwlist = false
    private var selectedUser = 0
    private var selectedApps: java.util.LinkedHashSet<String> = java.util.LinkedHashSet()
    private var selectedAppsBlack: java.util.LinkedHashSet<String> = java.util.LinkedHashSet()
    private var aType: Helpers.AppAdapterType = Helpers.AppAdapterType.Default
    private var multiUserSupport = false

    constructor(context: Context, arr: ArrayList<AppData>) {
        ctx = context
        mInflater = LayoutInflater.from(context)
        originalAppList.addAll(arr)
        filteredAppList.addAll(arr)
        val cpuCount = Runtime.getRuntime().availableProcessors()
        pool = ThreadPoolExecutor(
            cpuCount + 1,
            cpuCount * 2 + 1,
            2,
            TimeUnit.SECONDS,
            LinkedBlockingQueue()
        )
    }

    constructor(
        context: Context,
        arr: ArrayList<AppData>,
        adapterType: Helpers.AppAdapterType,
        prefKey: String?
    ) : this(context, arr, adapterType, prefKey, false)

    constructor(
        context: Context,
        arr: ArrayList<AppData>,
        adapterType: Helpers.AppAdapterType,
        prefKey: String?,
        isBW: Boolean
    ) : this(context, arr) {
        key = prefKey
        aType = adapterType
        bwlist = isBW
        var removeDual = false
        val currentKey = key ?: ""
        if (aType == Helpers.AppAdapterType.Mutli) {
            selectedApps = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(currentKey, emptySet()) ?: emptySet()
            )
            if (bwlist) selectedAppsBlack = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(currentKey + "_black", emptySet()) ?: emptySet()
            )
            val multiUserMods = ArrayList<String>().apply {
                add("pref_key_system_cleanshare_apps")
                add("pref_key_system_cleanopenwith_apps")
            }
            multiUserSupport = multiUserMods.contains(currentKey)
            if (multiUserSupport) {
                val selectedAppsAdd = java.util.HashSet<String>()
                val iter = selectedApps.iterator()
                while (iter.hasNext()) {
                    val item = iter.next()
                    if (!item.contains("|")) {
                        selectedAppsAdd.add(item + "|0")
                        iter.remove()
                    }
                }
                if (selectedAppsAdd.size > 0) selectedApps.addAll(selectedAppsAdd)
            } else {
                removeDual = true
            }
        } else if (aType == Helpers.AppAdapterType.Standalone) {
            selectedApp = AppHelper.getStringOfAppPrefs(currentKey, "")
            selectedUser = AppHelper.getIntOfAppPrefs(currentKey + "_user", 0)
            val noApp = AppData().apply {
                pkgName = ""
                actName = ""
                label = ctx.resources.getString(R.string.array_default)
                enabled = true
            }
            originalAppList.add(0, noApp)
            filteredAppList.add(0, noApp)
        } else if (aType == Helpers.AppAdapterType.Default) {
            removeDual = currentKey.contains("pref_key_system_applock_skip_activities")
        }
        if (removeDual) {
            originalAppList.removeAll { it.user != 0 }
            filteredAppList.clear()
            filteredAppList.addAll(originalAppList)
        }
        sortList()
    }

    fun updateSelectedApps() {
        val currentKey = key ?: ""
        if (aType == Helpers.AppAdapterType.Mutli) {
            selectedApps = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(currentKey, emptySet()) ?: emptySet()
            )
            if (bwlist) selectedAppsBlack = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(currentKey + "_black", emptySet()) ?: emptySet()
            )
        } else if (aType == Helpers.AppAdapterType.Standalone) {
            selectedApp = AppHelper.getStringOfAppPrefs(currentKey, "")
            selectedUser = AppHelper.getIntOfAppPrefs(currentKey + "_user", 0)
        }
        notifyDataSetChanged()
    }

    private fun shouldSelect(pkgName: String?, user: Int): Boolean {
        val name = pkgName ?: ""
        return (!multiUserSupport && (selectedApps.contains(name) || selectedApps.contains(name + "|0"))) ||
            (multiUserSupport && selectedApps.contains(name + "|" + user))
    }

    private fun shouldSelectBW(pkgName: String?): Boolean {
        return selectedApps.contains(pkgName ?: "") || selectedAppsBlack.contains(pkgName ?: "")
    }

    private fun sortList() {
        filteredAppList.sortWith(Comparator { app1, app2 ->
            when (aType) {
                Helpers.AppAdapterType.Mutli -> {
                    if (selectedApps.size == 0 && selectedAppsBlack.size == 0) return@Comparator 0
                    val app1checked = if (bwlist) shouldSelectBW(app1.pkgName) else shouldSelect(app1.pkgName, app1.user)
                    val app2checked = if (bwlist) shouldSelectBW(app2.pkgName) else shouldSelect(app2.pkgName, app2.user)
                    when {
                        app1checked && app2checked -> 0
                        app1checked -> -1
                        app2checked -> 1
                        else -> 0
                    }
                }
                Helpers.AppAdapterType.Standalone -> {
                    if (app1.pkgName == "" && app1.actName == "") return@Comparator -1
                    if (app2.pkgName == "" && app2.actName == "") return@Comparator 1
                    val app1Key = "${app1.pkgName ?: ""}|${app1.actName ?: ""}"
                    val app2Key = "${app2.pkgName ?: ""}|${app2.actName ?: ""}"
                    val app1checked = selectedApp == app1Key && selectedUser == app1.user
                    val app2checked = selectedApp == app2Key && selectedUser == app2.user
                    when {
                        app1checked && app2checked -> 0
                        app1checked -> -1
                        app2checked -> 1
                        else -> 0
                    }
                }
                Helpers.AppAdapterType.Activities -> {
                    (app1.actName ?: "").lowercase(Locale.getDefault())
                        .compareTo((app2.actName ?: "").lowercase(Locale.getDefault()))
                }
                else -> 0
            }
        })
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
        if (!bwlist && (itemChecked.tag == null || itemChecked.tag as? Boolean != true)) {
            itemChecked.tag = true
            Helpers.setMiuiCheckbox(itemChecked)
        }
        val itemStateIcon: ImageView = row.findViewById(android.R.id.selectedIcon)
        val itemTitle: TextView = row.findViewById(android.R.id.title)
        val itemSummary: TextView = row.findViewById(android.R.id.summary)
        val itemIcon: ImageView = row.findViewById(android.R.id.icon)

        val ad = getItem(position)
        itemTitle.text = ad.label ?: ""
        itemIsDis.visibility = if (ad.enabled) View.GONE else View.VISIBLE

        if (aType == Helpers.AppAdapterType.Activities) {
            itemIcon.visibility = View.GONE
            val container: View = row.findViewById(R.id.container)
            val lp = container.layoutParams as LinearLayout.LayoutParams
            lp.leftMargin = 0
            container.layoutParams = lp
        } else {
            itemIcon.tag = position
            val icon = Helpers.memoryCache.get(ad.pkgName + "|" + ad.actName)
            if (icon == null) {
                val dualIcon = arrayOf(ctx.resources.getDrawable(R.drawable.card_icon_default, ctx.theme))
                val crossfader = android.graphics.drawable.TransitionDrawable(dualIcon)
                crossfader.isCrossFadeEnabled = true
                itemIcon.setImageDrawable(crossfader)
                BitmapCachedLoader(itemIcon, ad, ctx).executeOnExecutor(pool)
            } else {
                itemIcon.setImageBitmap(icon)
            }
        }

        val currentKey = key ?: ""
        when (aType) {
            Helpers.AppAdapterType.Mutli -> {
                itemSummary.visibility = View.GONE
                if (bwlist) {
                    itemStateIcon.visibility = View.VISIBLE
                    itemStateIcon.setImageResource(
                        when {
                            selectedApps.contains(ad.pkgName ?: "") -> R.drawable.icon_action_allow
                            selectedAppsBlack.contains(ad.pkgName ?: "") -> R.drawable.icon_action_disallow
                            else -> R.drawable.icon_action_default
                        }
                    )
                } else {
                    itemChecked.visibility = View.VISIBLE
                    itemChecked.isChecked = shouldSelect(ad.pkgName, ad.user)
                }
                itemIsDual.visibility = if (ad.user != 0) View.VISIBLE else View.GONE
            }
            Helpers.AppAdapterType.CustomTitles -> {
                itemSummary.text = AppHelper.getStringOfAppPrefs(
                    currentKey + ":" + ad.pkgName + "|" + ad.actName + "|" + ad.user,
                    ""
                )
                itemSummary.visibility = if (TextUtils.isEmpty(itemSummary.text)) View.GONE else View.VISIBLE
                itemIsDual.visibility = if (ad.user != 0) View.VISIBLE else View.GONE
            }
            Helpers.AppAdapterType.Standalone -> {
                itemChecked.visibility = View.VISIBLE
                itemChecked.isChecked =
                    (selectedApp == "" && ad.pkgName == "" && ad.actName == "") ||
                        ("${ad.pkgName ?: ""}|${ad.actName ?: ""}" == selectedApp && ad.user == selectedUser)
                itemIsDual.visibility = if (ad.user != 0) View.VISIBLE else View.GONE
            }
            Helpers.AppAdapterType.Activities -> {
                itemSummary.text = (ad.actName ?: "").replace(".", ".\u200B")
                itemSummary.visibility = if (TextUtils.isEmpty(itemSummary.text)) View.GONE else View.VISIBLE
                itemSummary.setSingleLine(false)
                itemSummary.maxLines = Integer.MAX_VALUE
                itemSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                itemIsDual.visibility = if (ad.user != 0) View.VISIBLE else View.GONE
            }
            else -> {
                itemSummary.visibility = View.GONE
                itemIsDual.visibility = if (ad.user != 0) View.VISIBLE else View.GONE
            }
        }

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
                if (aType == Helpers.AppAdapterType.Activities &&
                    (filterableData.actName ?: "").lowercase(Locale.getDefault()).contains(filterString)
                ) {
                    nlist.add(filterableData)
                } else if (
                    (aType == Helpers.AppAdapterType.Standalone &&
                        filterableData.pkgName == "" && filterableData.actName == "") ||
                    (filterableData.label ?: "").lowercase(Locale.getDefault()).contains(filterString)
                ) {
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
