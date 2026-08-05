package tv.withaibuild.customiuizer.subs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.Helpers
import tv.withaibuild.customiuizer.utils.PreferenceAdapter
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics
import tv.withaibuild.customiuizer.utils.SortableListView
import java.util.Locale
import java.util.UUID

class SortableList : SubFragment() {

    private lateinit var key: String
    private var titleResId: String? = null
    private var activities = false
    private var listView: SortableListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        this.padded = false
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        toolbarMenu = true
        super.onActivityCreated(savedInstanceState)

        val args = arguments ?: return
        key = args.getString("key") ?: return
        titleResId = args.getString("titleResId")
        activities = args.getBoolean("activities", false)

        val ctx = context ?: return
        val view = view ?: return
        listView = view.findViewById(android.R.id.list)

        if (!activities) {
            try {
                val ssField = SortableListView::class.java.getDeclaredField("mSnapshotShadow")
                ssField.isAccessible = true
                val lightShadow = resources.getIdentifier(
                    "dynamic_listview_dragging_item_shadow_light",
                    "drawable",
                    "miui"
                )
                val darkShadow = resources.getIdentifier(
                    "dynamic_listview_dragging_item_shadow_dark",
                    "drawable",
                    "miui"
                )
                val shadow = resources.getDrawable(
                    if (Helpers.isNightMode(ctx)) darkShadow else lightShadow,
                    ctx.theme
                )
                ssField.set(listView, shadow)
            } catch (e: Throwable) {
                if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                SettingsDiagnostics.failure("SortableList.loadDragShadow", e)
            }
        }

        listView?.adapter = PreferenceAdapter(ctx, key, activities)
        if (activities) {
            listView?.setOnOrderChangedListener(null)
        } else {
            listView?.setOnOrderChangedListener { oldPos, newPos ->
                if (oldPos == newPos) return@setOnOrderChangedListener
                val itemStr = AppHelper.getStringOfAppPrefs(key, "") ?: ""
                if (itemStr.isEmpty()) return@setOnOrderChangedListener
                val itemList = itemStr.trim().split('|').toMutableList()
                val uuid = itemList[oldPos]
                itemList.removeAt(oldPos)
                itemList.add(newPos, uuid)
                AppHelper.appPrefs?.edit()?.putString(key, itemList.joinToString("|"))?.apply()
                val adapter = listView?.adapter as? PreferenceAdapter ?: return@setOnOrderChangedListener
                adapter.updateItems()
                adapter.notifyDataSetChanged()
            }
        }

        if (!activities) {
            listView?.setOnItemClickListener { parent, _, position, _ ->
                val uuid = (parent.adapter as? PreferenceAdapter)?.getItem(position) ?: return@setOnItemClickListener
                val bundle = Bundle().apply {
                    putString("key", "${key}_$uuid")
                    putInt("actions", MultiAction.Actions.LOCKSCREEN.ordinal)
                }
                openSubFragment(MultiAction(), bundle, Helpers.SettingsType.Edit, Helpers.ActionBarType.Edit, titleResId ?: "", R.layout.prefs_multiaction)
            }
        }

        listView?.setOnItemLongClickListener { _, _, position, _ ->
            deleteItem(position)
            true
        }
    }

    private fun createNewUUID(): String {
        return UUID.randomUUID().toString().replace("-", "").lowercase(Locale.ROOT)
    }

    private fun createNewItem(uuid: String) {
        val items = AppHelper.getStringOfAppPrefs(key, "") ?: ""
        AppHelper.appPrefs?.edit()?.putString(key, if (items.isEmpty()) uuid else "$items|$uuid")?.apply()
        val adapter = listView?.adapter as? PreferenceAdapter ?: return
        adapter.updateItems()
        adapter.notifyDataSetChanged()
    }

    private fun deleteItem(position: Int) {
        val adapter = listView?.adapter as? PreferenceAdapter ?: return
        val items = AppHelper.getStringOfAppPrefs(key, "") ?: ""
        val uuid = adapter.getItem(position)
        val newItems = if (items.isEmpty()) {
            ""
        } else {
            items.replace(uuid, "")
                .replace("||", "|")
                .replace(Regex("^\\|"), "")
                .replace(Regex("\\|$"), "")
        }
        AppHelper.appPrefs?.edit()?.putString(key, newItems)?.apply()
        adapter.updateItems()
        adapter.notifyDataSetChanged()
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.deleteitem -> {
                Toast.makeText(context, R.string.delete_item_info, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.additem -> {
                if (activities) {
                    val bundle = Bundle().apply {
                        putBoolean("activity", true)
                        putString("key", key)
                    }
                    val activitySelect = AppSelector()
                    activitySelect.setTargetFragment(this, 2)
                    openSubFragment(activitySelect, bundle, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_app, R.layout.prefs_app_selector)
                } else {
                    createNewItem(createNewUUID())
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "UNUSED_PARAMETER")
    override fun onPrepareOptionsMenu(menu: Menu) {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == 2) {
            val activityValue = data?.getStringExtra("activity") ?: return
            val activityUser = data.getIntExtra("user", 0)
            if (activityUser < 0) return

            val uuid = createNewUUID()
            AppHelper.appPrefs?.edit()
                ?.putInt("${key}_${uuid}_action", 20)
                ?.putString("${key}_${uuid}_activity", activityValue)
                ?.putInt("${key}_${uuid}_activity_user", activityUser)
                ?.apply()
            createNewItem(uuid)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
