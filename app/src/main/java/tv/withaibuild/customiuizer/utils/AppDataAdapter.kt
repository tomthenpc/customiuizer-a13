@file:Suppress("DEPRECATION")
package tv.withaibuild.customiuizer.utils

import android.content.Context
import android.graphics.drawable.TransitionDrawable
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

class AppDataAdapter : BaseAdapter, Filterable {
    private val ctx: Context
    private val inflater: LayoutInflater
    private val itemFilter = ItemFilter()
    private val originalAppList = ArrayList<AppData>()
    private var filteredAppList: List<AppData> = emptyList()
    private var key: String? = null
    private var selectedApp: String? = null
    private var bwlist = false
    private var selectedUser = 0
    private var selectedApps: LinkedHashSet<String> = LinkedHashSet()
    private var selectedAppsBlack: LinkedHashSet<String> = LinkedHashSet()
    private var aType: Helpers.AppAdapterType = Helpers.AppAdapterType.Default
    private var multiUserSupport = false

    constructor(context: Context, arr: ArrayList<AppData>) {
        ctx = context
        inflater = LayoutInflater.from(context)
        originalAppList.addAll(arr)
        prepareRows()
        replaceFiltered(originalAppList)
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
        val currentKey = key.orEmpty()
        if (aType == Helpers.AppAdapterType.Mutli) {
            selectedApps = LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(currentKey, emptySet()) ?: emptySet()
            )
            if (bwlist) {
                selectedAppsBlack = LinkedHashSet(
                    AppHelper.getStringSetOfAppPrefs(
                        currentKey + "_black",
                        emptySet()
                    ) ?: emptySet()
                )
            }
            multiUserSupport =
                currentKey == "pref_key_system_cleanshare_apps" ||
                    currentKey == "pref_key_system_cleanopenwith_apps"
            if (multiUserSupport) {
                val selectedAppsAdd = HashSet<String>()
                val iterator = selectedApps.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (!item.contains("|")) {
                        selectedAppsAdd.add("$item|0")
                        iterator.remove()
                    }
                }
                if (selectedAppsAdd.isNotEmpty()) selectedApps.addAll(selectedAppsAdd)
            } else {
                removeDual = true
            }
        } else if (aType == Helpers.AppAdapterType.Standalone) {
            selectedApp = AppHelper.getStringOfAppPrefs(currentKey, "")
            selectedUser = AppHelper.getIntOfAppPrefs(currentKey + "_user", 0)
            originalAppList.add(
                0,
                AppData().apply {
                    pkgName = ""
                    actName = ""
                    label = ctx.resources.getString(R.string.array_default)
                    enabled = true
                }
            )
        } else if (aType == Helpers.AppAdapterType.Default) {
            removeDual = currentKey.contains("pref_key_system_applock_skip_activities")
        }
        if (removeDual) originalAppList.removeAll { it.user != 0 }
        prepareRows()
        replaceFiltered(originalAppList)
    }

    private fun prepareRows() {
        val locale = Locale.getDefault()
        val currentKey = key.orEmpty()
        for (app in originalAppList) {
            val packageName = app.pkgName.orEmpty()
            val activityName = app.actName.orEmpty()
            app.labelSearchKey = app.label.orEmpty().lowercase(locale)
            app.activitySearchKey = activityName.lowercase(locale)
            app.selectionKey = "$packageName|$activityName"
            app.primaryUserSelectionKey = "$packageName|0"
            app.userSelectionKey = "$packageName|${app.user}"
            app.customTitlePrefKey = "$currentKey:${app.selectionKey}|${app.user}"
            app.activitySummary = activityName.replace(".", ".\u200B")
            app.iconKey = if (activityName.isEmpty()) packageName else app.selectionKey
        }
    }

    fun updateSelectedApps() {
        val currentKey = key.orEmpty()
        if (aType == Helpers.AppAdapterType.Mutli) {
            selectedApps = LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(currentKey, emptySet()) ?: emptySet()
            )
            if (bwlist) {
                selectedAppsBlack = LinkedHashSet(
                    AppHelper.getStringSetOfAppPrefs(
                        currentKey + "_black",
                        emptySet()
                    ) ?: emptySet()
                )
            }
        } else if (aType == Helpers.AppAdapterType.Standalone) {
            selectedApp = AppHelper.getStringOfAppPrefs(currentKey, "")
            selectedUser = AppHelper.getIntOfAppPrefs(currentKey + "_user", 0)
        }
        replaceFiltered(filteredAppList)
        notifyDataSetChanged()
    }

    private fun shouldSelect(app: AppData): Boolean {
        val packageName = app.pkgName.orEmpty()
        return if (multiUserSupport) {
            selectedApps.contains(app.userSelectionKey)
        } else {
            selectedApps.contains(packageName) || selectedApps.contains(app.primaryUserSelectionKey)
        }
    }

    private fun shouldSelectBW(app: AppData): Boolean {
        val packageName = app.pkgName.orEmpty()
        return selectedApps.contains(packageName) || selectedAppsBlack.contains(packageName)
    }

    private fun replaceFiltered(source: Collection<AppData>) {
        val replacement = ArrayList(source)
        replacement.sortWith(rowComparator)
        filteredAppList = replacement
    }

    private val rowComparator = Comparator<AppData> { app1, app2 ->
        when (aType) {
            Helpers.AppAdapterType.Mutli -> {
                if (selectedApps.isEmpty() && selectedAppsBlack.isEmpty()) {
                    0
                } else {
                    val app1checked = if (bwlist) shouldSelectBW(app1) else shouldSelect(app1)
                    val app2checked = if (bwlist) shouldSelectBW(app2) else shouldSelect(app2)
                    when {
                        app1checked && app2checked -> 0
                        app1checked -> -1
                        app2checked -> 1
                        else -> 0
                    }
                }
            }
            Helpers.AppAdapterType.Standalone -> {
                when {
                    app1.pkgName == "" && app1.actName == "" -> -1
                    app2.pkgName == "" && app2.actName == "" -> 1
                    else -> {
                        val app1checked =
                            selectedApp == app1.selectionKey && selectedUser == app1.user
                        val app2checked =
                            selectedApp == app2.selectionKey && selectedUser == app2.user
                        when {
                            app1checked && app2checked -> 0
                            app1checked -> -1
                            app2checked -> 1
                            else -> 0
                        }
                    }
                }
            }
            Helpers.AppAdapterType.Activities ->
                app1.activitySearchKey.compareTo(app2.activitySearchKey)
            else -> 0
        }
    }

    override fun getCount(): Int = filteredAppList.size

    override fun getItem(position: Int): AppData = filteredAppList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = if (convertView == null) {
            val row = inflater.inflate(R.layout.applist_item11, parent, false)
            ViewHolder(row).also { row.tag = it }
        } else {
            convertView.tag as ViewHolder
        }

        if (!bwlist && holder.checked.tag != true) {
            holder.checked.tag = true
            Helpers.setMiuiCheckbox(holder.checked)
        }

        val app = getItem(position)
        holder.title.text = app.label.orEmpty()
        holder.disableIcon.visibility = if (app.enabled) View.GONE else View.VISIBLE

        if (aType == Helpers.AppAdapterType.Activities) {
            holder.icon.visibility = View.GONE
            val params = holder.container.layoutParams as LinearLayout.LayoutParams
            params.leftMargin = 0
            holder.container.layoutParams = params
        } else {
            holder.icon.tag = app.iconKey
            val icon = Helpers.memoryCache.get(app.iconKey)
            if (icon == null) {
                val placeholder =
                    arrayOf(ctx.resources.getDrawable(R.drawable.card_icon_default, ctx.theme))
                val crossfader = TransitionDrawable(placeholder)
                crossfader.isCrossFadeEnabled = true
                holder.icon.setImageDrawable(crossfader)
                BitmapCachedLoader(holder.icon, app, ctx).execute()
            } else {
                holder.icon.setImageBitmap(icon)
            }
        }

        when (aType) {
            Helpers.AppAdapterType.Mutli -> {
                holder.summary.visibility = View.GONE
                if (bwlist) {
                    holder.stateIcon.visibility = View.VISIBLE
                    holder.stateIcon.setImageResource(
                        when {
                            selectedApps.contains(app.pkgName.orEmpty()) ->
                                R.drawable.icon_action_allow
                            selectedAppsBlack.contains(app.pkgName.orEmpty()) ->
                                R.drawable.icon_action_disallow
                            else -> R.drawable.icon_action_default
                        }
                    )
                } else {
                    holder.checked.visibility = View.VISIBLE
                    holder.checked.isChecked = shouldSelect(app)
                }
                holder.dualIcon.visibility = if (app.user != 0) View.VISIBLE else View.GONE
            }
            Helpers.AppAdapterType.CustomTitles -> {
                holder.summary.text =
                    AppHelper.getStringOfAppPrefs(app.customTitlePrefKey, "")
                holder.summary.visibility =
                    if (TextUtils.isEmpty(holder.summary.text)) View.GONE else View.VISIBLE
                holder.dualIcon.visibility = if (app.user != 0) View.VISIBLE else View.GONE
            }
            Helpers.AppAdapterType.Standalone -> {
                holder.checked.visibility = View.VISIBLE
                holder.checked.isChecked =
                    (selectedApp == "" && app.pkgName == "" && app.actName == "") ||
                        (app.selectionKey == selectedApp && app.user == selectedUser)
                holder.dualIcon.visibility = if (app.user != 0) View.VISIBLE else View.GONE
            }
            Helpers.AppAdapterType.Activities -> {
                holder.summary.text = app.activitySummary
                holder.summary.visibility =
                    if (TextUtils.isEmpty(holder.summary.text)) View.GONE else View.VISIBLE
                holder.summary.setSingleLine(false)
                holder.summary.maxLines = Integer.MAX_VALUE
                holder.summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                holder.dualIcon.visibility = if (app.user != 0) View.VISIBLE else View.GONE
            }
            else -> {
                holder.summary.visibility = View.GONE
                holder.dualIcon.visibility = if (app.user != 0) View.VISIBLE else View.GONE
            }
        }

        return holder.root
    }

    private class ViewHolder(val root: View) {
        val disableIcon: ImageView = root.findViewById(R.id.icon_disable)
        val dualIcon: ImageView = root.findViewById(R.id.icon_dual)
        val checked: CheckBox = root.findViewById(android.R.id.checkbox)
        val stateIcon: ImageView = root.findViewById(android.R.id.selectedIcon)
        val title: TextView = root.findViewById(android.R.id.title)
        val summary: TextView = root.findViewById(android.R.id.summary)
        val icon: ImageView = root.findViewById(android.R.id.icon)
        val container: View = root.findViewById(R.id.container)
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterString = constraint?.toString()?.lowercase(Locale.getDefault()).orEmpty()
            val matches = ArrayList<AppData>()
            for (app in originalAppList) {
                if (
                    aType == Helpers.AppAdapterType.Activities &&
                    app.activitySearchKey.contains(filterString)
                ) {
                    matches.add(app)
                } else if (
                    (aType == Helpers.AppAdapterType.Standalone &&
                        app.pkgName == "" &&
                        app.actName == "") ||
                    app.labelSearchKey.contains(filterString)
                ) {
                    matches.add(app)
                }
            }
            return FilterResults().apply {
                values = matches
                count = matches.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            val matches = results?.values as? ArrayList<AppData> ?: ArrayList()
            replaceFiltered(matches)
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = itemFilter
}
