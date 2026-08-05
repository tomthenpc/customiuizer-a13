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

class PrivacyAppAdapter @SuppressLint("WrongConstant") constructor(
    context: Context,
    arr: ArrayList<AppData>
) : BaseAdapter(), Filterable {

    private val ctx: Context = context
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val mFilter = ItemFilter()
    private val originalAppList: ArrayList<AppData> = arr
    private var filteredAppList: List<AppData> = emptyList()
    private val checkedApps = HashSet<String>()
    private val searchLocale = Locale.getDefault()
    private var mSecurityManager: Any? = null
    private var isPrivacyApp: Method? = null
    private var readCheckedFailureLogged = false

    init {
        prepareRows()
        try {
            mSecurityManager = context.getSystemService("security")
            val sm = mSecurityManager
            if (sm != null) {
                val method = sm::class.java.getDeclaredMethod(
                    "isPrivacyApp",
                    String::class.java,
                    Int::class.javaPrimitiveType
                )
                method.isAccessible = true
                isPrivacyApp = method
            }
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            SettingsDiagnostics.failure("PrivacyAppAdapter.initializeSecurityManager", t)
        }

        for (app in originalAppList) {
            if (readChecked(app)) checkedApps.add(app.userSelectionKey)
        }
        replaceFiltered(originalAppList)
    }

    private fun prepareRows() {
        for (app in originalAppList) {
            val packageName = app.pkgName.orEmpty()
            app.labelSearchKey = app.label.orEmpty().lowercase(searchLocale)
            app.userSelectionKey = "$packageName|${app.user}"
            app.iconKey = appIconCacheKey(app)
        }
    }

    private fun readChecked(app: AppData): Boolean {
        val sm = mSecurityManager ?: return false
        val method = isPrivacyApp ?: return false
        return try {
            method.invoke(sm, app.pkgName.orEmpty(), app.user) as? Boolean ?: false
        } catch (error: Throwable) {
            ReflectionFatality.rethrowIfFatal(error)

            if (!readCheckedFailureLogged) {
                readCheckedFailureLogged = true
                SettingsDiagnostics.failure("PrivacyAppAdapter.readChecked", error)
            }

            false
        }
    }

    fun refresh(app: AppData) {
        if (readChecked(app)) checkedApps.add(app.userSelectionKey)
        else checkedApps.remove(app.userSelectionKey)
        notifyDataSetChanged()
    }

    private fun replaceFiltered(source: Collection<AppData>) {
        val replacement = ArrayList(source)
        replacement.sortWith(Comparator { app1, app2 ->
            val app1checked = checkedApps.contains(app1.userSelectionKey)
            val app2checked = checkedApps.contains(app2.userSelectionKey)
            when {
                app1checked == app2checked -> 0
                app1checked -> -1
                else -> 1
            }
        })
        filteredAppList = replacement
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
        val holder = if (convertView == null) {
            val row = mInflater.inflate(R.layout.applist_item11, parent, false)
            ViewHolder(row).also {
                Helpers.setMiuiCheckbox(it.checked)
                row.tag = it
            }
        } else {
            convertView.tag as ViewHolder
        }

        val app = getItem(position)
        holder.icon.tag = app.iconKey
        holder.title.text = app.label.orEmpty()
        holder.disableIcon.visibility = if (app.enabled) View.GONE else View.VISIBLE
        holder.dualIcon.visibility = if (app.user != 0) View.VISIBLE else View.GONE

        val icon: Bitmap? = Helpers.memoryCache.get(app.iconKey)
        if (icon == null) {
            val placeholder = arrayOf(ctx.resources.getDrawable(R.drawable.card_icon_default, ctx.theme))
            val crossfader = android.graphics.drawable.TransitionDrawable(placeholder)
            crossfader.isCrossFadeEnabled = true
            holder.icon.setImageDrawable(crossfader)
            BitmapCachedLoader(holder.icon, app, ctx).execute()
        } else {
            holder.icon.setImageBitmap(icon)
        }

        holder.checked.visibility = View.VISIBLE
        holder.checked.isChecked = checkedApps.contains(app.userSelectionKey)
        return holder.root
    }

    private class ViewHolder(val root: View) {
        val disableIcon: ImageView = root.findViewById(R.id.icon_disable)
        val dualIcon: ImageView = root.findViewById(R.id.icon_dual)
        val checked: CheckBox = root.findViewById(android.R.id.checkbox)
        val title: TextView = root.findViewById(android.R.id.title)
        val icon: ImageView = root.findViewById(android.R.id.icon)
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
            val filterString = constraint?.toString()?.lowercase(searchLocale).orEmpty()
            val nlist = ArrayList<AppData>(if (filterString.isEmpty()) originalAppList.size else 16)
            for (app in originalAppList) {
                if (app.labelSearchKey.contains(filterString)) nlist.add(app)
            }
            return Filter.FilterResults().apply {
                values = nlist
                count = nlist.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) {
            replaceFiltered(results?.values as? ArrayList<AppData> ?: emptyList())
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter {
        return mFilter
    }
}
