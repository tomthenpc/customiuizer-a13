package name.monwf.customiuizer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.recyclerview.widget.RecyclerView
import name.monwf.customiuizer.prefs.ListPreferenceEx
import name.monwf.customiuizer.subs.CategorySelector
import name.monwf.customiuizer.subs.Controls
import name.monwf.customiuizer.subs.Launcher
import name.monwf.customiuizer.subs.System
import name.monwf.customiuizer.subs.Various
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.AppLocaleController
import name.monwf.customiuizer.utils.Helpers
import name.monwf.customiuizer.utils.ModData
import name.monwf.customiuizer.utils.ModSearchAdapter
import name.monwf.customiuizer.utils.SearchRoute
import name.monwf.customiuizer.utils.SearchRouteResolver
import name.monwf.customiuizer.utils.SearchStateMachine

class MainFragment : PreferenceFragmentBase() {

    private val catSelector = CategorySelector()
    @JvmField
    var prefSystem: System = System()
    @JvmField
    var prefLauncher: Launcher = Launcher()
    @JvmField
    var prefControls: Controls = Controls()
    @JvmField
    var prefVarious: Various = Various()

    private var mActionMenu: Menu? = null
    private var listView: RecyclerView? = null
    private var resultView: ListView? = null
    private var mMainHandler: Handler? = null
    private var mCheckActiveRunnable: Runnable? = null
    private var mHideKeyboardRunnable: Runnable? = null

    @JvmField
    var isSearchFocused = false
    private var isRestoringSearch = false
    @JvmField
    var inSearchView = SearchStateMachine.STATE_IDLE
    @JvmField
    var lastFilter: String? = null

    private val showUpdateNotification = Runnable {
        if (view != null) try {
            val alert = view?.findViewById<ImageView>(R.id.update_alert)
            alert?.visibility = View.VISIBLE
        } catch (_: Throwable) {}
    }

    private val hideUpdateNotification = Runnable {
        if (view != null) try {
            val alert = view?.findViewById<ImageView>(R.id.update_alert)
            alert?.visibility = View.GONE
        } catch (_: Throwable) {}
    }

    private fun isFragmentReady(act: AppCompatActivity?): Boolean {
        return act != null && !act.isFinishing && isAdded
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        toolbarMenu = true
        activeMenus = "all"
        if (savedInstanceState != null) {
            inSearchView = savedInstanceState.getInt("inSearchView", SearchStateMachine.STATE_IDLE)
            lastFilter = savedInstanceState.getString("lastFilter")
            isSearchFocused = savedInstanceState.getBoolean("isSearchFocused", false)
        }
        super.onCreate(savedInstanceState, R.xml.prefs_main)
        tailLayoutId = R.layout.prefs_main12
        val act = activity as? AppCompatActivity

        Thread { Helpers.getAllMods(act, savedInstanceState != null) }.start()

        checkModuleIsActive()
    }

    private fun checkModuleIsActive() {
        if (mMainHandler == null) mMainHandler = Handler(Looper.getMainLooper())
        mCheckActiveRunnable?.let { mMainHandler?.removeCallbacks(it) }
        val runnable = Runnable {
            val act = activity as? AppCompatActivity
            if (isFragmentReady(act) && !AppHelper.moduleActive) {
                act?.runOnUiThread { showXposedDialog(act) }
            }
        }
        mCheckActiveRunnable = runnable
        mMainHandler?.postDelayed(runnable, 800)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_main, rootKey)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mActionMenu = menu
        val searchMenuItem = mActionMenu?.findItem(R.id.search_btn)
        val searchView = searchMenuItem?.actionView as? SearchView

        searchMenuItem?.let { item ->
            MenuItemCompat.setOnActionExpandListener(item, object : MenuItemCompat.OnActionExpandListener {
                override fun onMenuItemActionCollapse(searchItem: MenuItem): Boolean {
                    for (i in 0 until menu.size()) {
                        val it = menu.getItem(i)
                        it.isVisible = it.itemId != R.id.edit_confirm && it.itemId != R.id.openinweb
                    }
                    return true
                }

                override fun onMenuItemActionExpand(searchItem: MenuItem): Boolean {
                    for (i in 0 until menu.size()) {
                        val it = menu.getItem(i)
                        it.isVisible = it.itemId == R.id.search_btn
                    }
                    return true
                }
            })
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean = false

            override fun onQueryTextChange(newText: String): Boolean {
                if (isRestoringSearch || !SearchStateMachine.canFilter(inSearchView)) return false
                inSearchView = SearchStateMachine.transitionOnQuery(inSearchView, newText)
                findMod(newText)
                return false
            }
        })

        searchView?.setOnQueryTextFocusChangeListener { _, hasFocus ->
            isSearchFocused = hasFocus
        }

        if (SearchStateMachine.shouldClearOnReturn(inSearchView)) {
            resetSearchUi(searchMenuItem, searchView)
        } else if (inSearchView != SearchStateMachine.STATE_IDLE && !TextUtils.isEmpty(lastFilter)) {
            isRestoringSearch = true
            if (!MenuItemCompat.isActionViewExpanded(searchMenuItem)) {
                searchMenuItem?.let { MenuItemCompat.expandActionView(it) }
            }
            if (lastFilter != searchView?.query?.toString()) {
                searchView?.setQuery(lastFilter, false)
                if (!isSearchFocused) searchView?.clearFocus()
            }
            isRestoringSearch = false
            if (resultView != null && listView != null) findMod(lastFilter ?: "")
        }
    }

    override fun fixStubLayout(view: View, position: Int) {
        if (position == 2) {
            val lp = view.layoutParams
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            view.layoutParams = lp
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val actionBar: ActionBar? = actionBar
        actionBar?.setTitle(R.string.app_name)

        val view = view ?: return
        val act = activity ?: return

        resultView = view.findViewById(R.id.custom)
        resultView?.setDivider(null)
        resultView?.setDividerHeight(0)
        resultView?.adapter = ModSearchAdapter(act)
        resultView?.setOnItemClickListener { parent, _, position, _ ->
            val mod = parent.adapter?.getItem(position) as? ModData ?: return@setOnItemClickListener
            val cat = mod.cat?.name ?: return@setOnItemClickListener
            val modKey = mod.key ?: return@setOnItemClickListener
            if (openModCat(cat, mod.sub, modKey)) {
                inSearchView = SearchStateMachine.STATE_NAVIGATED
                isSearchFocused = false
                val a = activity as? AppCompatActivity
                if (a != null) Helpers.hideKeyboard(a, view)
            }
        }
        resultView?.setOnTouchListener { v, event ->
            if (isSearchFocused) {
                isSearchFocused = false
                if (mMainHandler == null) mMainHandler = Handler(v.context.mainLooper)
                mHideKeyboardRunnable?.let { mMainHandler?.removeCallbacks(it) }
                val hideRunnable = Runnable {
                    val act = activity as? AppCompatActivity
                    if (act != null) Helpers.hideKeyboard(act, view)
                }
                mHideKeyboardRunnable = hideRunnable
                mMainHandler?.postDelayed(hideRunnable, resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                resultView?.requestFocus()
            }
            false
        }

        listView = getListView()

        if (SearchStateMachine.shouldClearOnReturn(inSearchView)) {
            resetSearchUi(null, null)
        } else if (inSearchView != SearchStateMachine.STATE_IDLE && !TextUtils.isEmpty(lastFilter)) {
            findMod(lastFilter ?: "")
        }

        findPreference<Preference>("pref_key_miuizer_launchericon")?.setOnPreferenceChangeListener { _, newValue ->
            val pm = act.packageManager
            if (newValue as? Boolean == true) {
                pm.setComponentEnabledSetting(
                    ComponentName(act, GateWayLauncher::class.java),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    ComponentName(act, GateWayLauncher::class.java),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            true
        }

        val locale = findPreference<ListPreferenceEx>("pref_key_miuizer_locale")
        AppLocaleController.setupLocalePreference(locale, AppHelper.appPrefs)
    }

    protected fun findMod(filter: String) {
        if (isRestoringSearch || !SearchStateMachine.canFilter(inSearchView)) return
        if (resultView == null || listView == null) return
        lastFilter = filter
        resultView?.visibility = if (filter.isEmpty()) View.GONE else View.VISIBLE
        listView?.isEnabled = filter.isEmpty()
        val adapter = resultView?.adapter ?: return
        (adapter as? ModSearchAdapter)?.filter?.filter(filter)
    }

    @Suppress("DEPRECATION")
    private fun resetSearchUi(searchMenuItem: MenuItem?, searchView: SearchView?) {
        if (!SearchStateMachine.shouldClearOnReturn(inSearchView)) return
        isRestoringSearch = true
        try {
            searchMenuItem?.let { MenuItemCompat.collapseActionView(it) }
            searchView?.setQuery("", false)
            searchView?.clearFocus()
            resultView?.visibility = View.GONE
            listView?.isEnabled = true
            isSearchFocused = false
        } finally {
            isRestoringSearch = false
        }
        inSearchView = SearchStateMachine.STATE_IDLE
        lastFilter = null
    }

    @Suppress("DEPRECATION")
    private fun openModCat(cat: String, sub: String?, mod: String): Boolean {
        val route = SearchRouteResolver.resolve(cat, sub, mod)
        if (route == null) return false
        if (!isAdded) return false

        val bundle = Bundle().apply {
            putString("cat", route.category)
            if (route.sub != null) putString("sub", route.sub)
            putString("mod", route.key)
        }
        catSelector.setTargetFragment(this, 0)
        return when (route.category) {
            "pref_key_system" -> {
                if (route.isCategorySelector()) {
                    openSubFragment(catSelector, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_mods, R.xml.prefs_system_cat)
                } else {
                    openSubFragment(prefSystem, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_mods, R.xml.prefs_system)
                }
                true
            }
            "pref_key_launcher" -> {
                if (route.isCategorySelector()) {
                    openSubFragment(catSelector, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.launcher_title, R.xml.prefs_launcher_cat)
                } else {
                    openSubFragment(prefLauncher, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.launcher_title, R.xml.prefs_launcher)
                }
                true
            }
            "pref_key_controls" -> {
                if (route.isCategorySelector()) {
                    openSubFragment(catSelector, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.controls_mods, R.xml.prefs_controls_cat)
                } else {
                    openSubFragment(prefControls, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.controls_mods, R.xml.prefs_controls)
                }
                true
            }
            "pref_key_various" -> {
                openSubFragment(prefVarious, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.various_mods, R.xml.prefs_various)
                true
            }
            else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("inSearchView", inSearchView)
        outState.putString("lastFilter", lastFilter)
        outState.putBoolean("isSearchFocused", isSearchFocused)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCheckActiveRunnable?.let { mMainHandler?.removeCallbacks(it) }
        mHideKeyboardRunnable?.let { mMainHandler?.removeCallbacks(it) }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key ?: return super.onPreferenceTreeClick(preference)
        val modsCat = findPreference<PreferenceCategory>("prefs_cat")
        if (modsCat != null && modsCat.findPreference<Preference>(key) != null && openModCat(key, null, key)) {
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }
}
