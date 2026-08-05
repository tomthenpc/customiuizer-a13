package tv.withaibuild.customiuizer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import tv.withaibuild.customiuizer.prefs.PreferenceCategoryEx
import tv.withaibuild.customiuizer.prefs.PreferenceState
import tv.withaibuild.customiuizer.prefs.SpinnerEx
import tv.withaibuild.customiuizer.prefs.SpinnerExFake
import tv.withaibuild.customiuizer.subs.AppSelector
import tv.withaibuild.customiuizer.subs.ColorSelector
import tv.withaibuild.customiuizer.subs.MultiAction
import tv.withaibuild.customiuizer.subs.SortableList
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.ColorCircle
import tv.withaibuild.customiuizer.utils.Helpers

@Suppress("DEPRECATION")
open class SubFragment : PreferenceFragmentBase() {

    private var contentResId = 0

    @JvmField
    var settingTitle: String = ""

    @JvmField
    protected var sub: String? = null

    @JvmField
    protected var catInfo: Bundle? = null

    @JvmField
    protected var isStandalone = false

    private var order = 100.0f
    private var highlightKey: String? = null

    @JvmField
    var padded = true

    @JvmField
    var settingsType = Helpers.SettingsType.Preference

    @JvmField
    var abType = Helpers.ActionBarType.Edit

    @JvmField
    val openAppsEdit: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openApps(preference.key)
        true
    }

    @JvmField
    val openAppsBWEdit: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openAppsBW(preference.key)
        true
    }

    @JvmField
    val openShareEdit: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openShare(preference.key)
        true
    }

    @JvmField
    val openOpenWithEdit: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openOpenWith(preference.key)
        true
    }

    @JvmField
    val openLauncherActions: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openMultiAction(preference, MultiAction.Actions.LAUNCHER)
        true
    }

    @JvmField
    val openControlsActions: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openMultiAction(preference, MultiAction.Actions.CONTROLS)
        true
    }

    @JvmField
    val openNavbarActions: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openMultiAction(preference, MultiAction.Actions.NAVBAR)
        true
    }

    @JvmField
    val openStatusbarActions: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openMultiAction(preference, MultiAction.Actions.STATUSBAR)
        true
    }

    @JvmField
    val openLockScreenActions: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openMultiAction(preference, MultiAction.Actions.LOCKSCREEN)
        true
    }

    @JvmField
    val openLaunchActions: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        openMultiAction(preference, MultiAction.Actions.LAUNCH)
        true
    }

    @JvmField
    val openActivitiesList: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        val act = activity as? AppCompatActivity
        if (act != null && !Helpers.checkPermAndRequest(act, Helpers.ACCESS_SECURITY_CENTER, Helpers.REQUEST_PERMISSIONS_SECURITY_CENTER)) {
            return@OnPreferenceClickListener false
        }
        openActivitiesItemList(preference)
        true
    }

    @JvmField
    val openColorSelector: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        doOpenColorSelector(preference)
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val args = requireArguments()
        settingsType = Helpers.SettingsType.values()[args.getInt("settingsType")]
        abType = Helpers.ActionBarType.values()[args.getInt("abType")]
        contentResId = args.getInt("contentResId")
        settingTitle = args.getString("titleResId") ?: ""
        order = args.getFloat("order") + 10.0f
        catInfo = args.getBundle("catInfo")
        sub = args.getString("sub")
        isStandalone = args.getBoolean("isStandalone")
        highlightKey = args.getString("mod")

        if (abType == Helpers.ActionBarType.Edit) {
            isCustomActionBar = true
        }
        toolbarMenu = toolbarMenu || isCustomActionBar

        if (contentResId == 0) {
            activity?.finish()
            return
        }

        if (settingsType == Helpers.SettingsType.Preference) {
            super.onCreate(savedInstanceState, contentResId)
        } else {
            super.onCreate(savedInstanceState)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadSharedPrefs()
        val actionBar: ActionBar? = getActionBar()
        if (actionBar != null) {
            if (isStandalone && catInfo?.getBoolean("isDynamic") == true) {
                actionBar.setTitle(settingTitle + " ⟳")
            } else if (!isStandalone && sub != null) {
                val screen = preferenceScreen ?: return
                val category = screen.getPreference(0) as? PreferenceCategoryEx
                if (category != null) {
                    val title = category.title?.toString() ?: ""
                    actionBar.setTitle(if (category.isDynamic()) title + " ⟳" else title)
                }
            } else {
                actionBar.setTitle(settingTitle)
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (settingsType == Helpers.SettingsType.Preference) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(contentResId, rootKey)
            val highlightPref = highlightKey?.let { findPreference<Preference>(it) } as? PreferenceState
            if (highlightPref != null) {
                highlightPref.applyHighlight()
            } else {
                highlightKey = null
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val crtInflator = inflater.cloneInContext(requireContext())
        return if (settingsType == Helpers.SettingsType.Preference) {
            super.onCreateView(crtInflator, container, savedInstanceState)
        } else {
            val view = crtInflator.inflate(
                if (padded) R.layout.prefs_common_padded else R.layout.prefs_common,
                container,
                false
            )
            crtInflator.inflate(contentResId, view as FrameLayout)
            view
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.translationZ = order
    }

    override fun onStart() {
        super.onStart()
        val key = highlightKey ?: return
        val mList = listView ?: return
        val adapter = mList.adapter as? PreferenceGroup.PreferencePositionCallback ?: return
        val position = adapter.getPreferenceAdapterPosition(key)
        highlightKey = null
        if (position < 9) return
        val smoothScroller = object : LinearSmoothScroller(mList.context) {
            override fun getVerticalSnapPreference(): Int {
                return LinearSmoothScroller.SNAP_TO_START
            }
        }
        smoothScroller.targetPosition = position
        view?.postDelayed({
            mList.layoutManager?.startSmoothScroll(smoothScroller)
        }, 380)
    }

    open fun saveSharedPrefs() {
        val container = view?.findViewById<View>(R.id.container)
        if (container == null) {
            Log.e("miuizer", "View not yet ready!")
            return
        }
        val prefs = AppHelper.appPrefs?.edit() ?: return
        val nViews = Helpers.getChildViewsRecursive(container, false)
        for (nView in nViews) {
            if (nView == null) continue
            try {
                val tag = nView.tag as? String ?: continue
                when (nView) {
                    is TextView -> prefs.putString(tag, nView.text.toString()).apply()
                    is SpinnerExFake -> {
                        prefs.putString(tag, nView.getValue()).apply()
                        nView.applyOthers()
                    }
                    is SpinnerEx -> prefs.putInt(tag, nView.getSelectedArrayValue()).apply()
                    is ColorCircle -> prefs.putInt(tag, nView.color).apply()
                }
            } catch (e: Throwable) {
                if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                Log.e("miuizer", "Cannot save sub preference!")
            }
        }
    }

    open fun loadSharedPrefs() {
        val container = view?.findViewById<View>(R.id.container)
        if (container == null) {
            Log.e("miuizer", "View not yet ready!")
            return
        }
        val nViews = Helpers.getChildViewsRecursive(container, false)
        for (nView in nViews) {
            if (nView == null) continue
            try {
                val tag = nView.tag as? String ?: continue
                when (nView) {
                    is TextView -> nView.text = AppHelper.getStringOfAppPrefs(tag, "")
                }
            } catch (e: Throwable) {
                if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                Log.e("miuizer", "Cannot load sub preference!")
            }
        }
    }

    private fun setTargetFragmentDeprecated(fragment: Fragment) {
        fragment.setTargetFragment(this, 0)
    }

    private fun setTargetFragmentDeprecated(fragment: Fragment, target: Fragment, requestCode: Int) {
        fragment.setTargetFragment(target, requestCode)
    }

    protected fun openApps(key: String?) {
        val args = Bundle().apply {
            putString("key", key)
            putBoolean("multi", true)
        }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_apps, R.layout.prefs_app_selector)
    }

    protected fun openAppsBW(key: String?) {
        val args = Bundle().apply {
            putString("key", key)
            putBoolean("multi", true)
            putBoolean("bw", true)
        }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_apps, R.layout.prefs_app_selector)
    }

    protected fun openShare(key: String?) {
        val args = Bundle().apply {
            putString("key", key)
            putBoolean("multi", true)
            putBoolean("share", true)
        }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_apps, R.layout.prefs_app_selector)
    }

    protected fun openOpenWith(key: String?) {
        val args = Bundle().apply {
            putString("key", key)
            putBoolean("multi", true)
            putBoolean("openwith", true)
        }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_apps, R.layout.prefs_app_selector)
    }

    protected fun openMultiAction(pref: Preference, actions: MultiAction.Actions) {
        val args = Bundle().apply {
            putString("key", pref.key)
            putInt("actions", actions.ordinal)
        }
        openSubFragment(MultiAction(), args, Helpers.SettingsType.Edit, Helpers.ActionBarType.Edit, pref.title?.toString() ?: "", R.layout.prefs_multiaction)
    }

    fun openStandaloneApp(pref: Preference, targetFrag: Fragment, resultId: Int) {
        val args = Bundle().apply {
            putString("key", pref.key)
            putBoolean("standalone", true)
        }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector, targetFrag, resultId)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_app, R.layout.prefs_app_selector)
    }

    fun openPrivacyAppEdit(targetFrag: Fragment, resultId: Int) {
        val args = Bundle().apply { putBoolean("privacy", true) }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector, targetFrag, resultId)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_apps, R.layout.prefs_app_selector)
    }

    fun openLockedAppEdit(targetFrag: Fragment, resultId: Int) {
        val args = Bundle().apply { putBoolean("applock", true) }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector, targetFrag, resultId)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_apps, R.layout.prefs_app_selector)
    }

    fun openLaunchableList(pref: Preference, targetFrag: Fragment, resultId: Int) {
        val args = Bundle().apply {
            putString("key", pref.key)
            putBoolean("custom_titles", true)
        }
        val appSelector = AppSelector()
        setTargetFragmentDeprecated(appSelector, targetFrag, resultId)
        openSubFragment(appSelector, args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.launcher_renameapps_list_title, R.layout.prefs_app_selector)
    }

    private fun doOpenColorSelector(pref: Preference) {
        val args = Bundle().apply { putString("key", pref.key) }
        openSubFragment(ColorSelector(), args, Helpers.SettingsType.Edit, Helpers.ActionBarType.Edit, pref.title?.toString() ?: "", R.layout.fragment_selectcolor)
    }

    fun openActivitiesItemList(pref: Preference) {
        val args = Bundle().apply {
            putBoolean("activities", true)
            putString("key", pref.key)
            putString("titleResId", pref.title?.toString() ?: "")
        }
        openSubFragment(SortableList(), args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, pref.title?.toString() ?: "", R.layout.prefs_sortable_list)
    }

    fun selectSub() {
        if (isStandalone) return
        val screen = preferenceScreen ?: return
        var i = screen.preferenceCount - 1
        while (i >= 0) {
            val pref = screen.getPreference(i)
            val key = pref.key
            if (key == null || key != sub) {
                screen.removePreference(pref)
            } else {
                val category = pref as? PreferenceCategoryEx
                val actionBar: ActionBar? = getActionBar()
                if (category != null && actionBar != null) {
                    val title = category.title?.toString() ?: ""
                    actionBar.setTitle(if (category.isDynamic()) title + " ⟳" else title)
                }
                category?.hide()
            }
            i--
        }
    }

    fun finish() {
        val act = activity as? AppCompatActivity
        Helpers.hideKeyboard(act, view)
        val fragmentManager: FragmentManager = parentFragmentManager
        if (!isResumed) {
            act?.supportFragmentManager?.popBackStack()
        } else {
            fragmentManager.popBackStackImmediate()
        }
    }

    override fun confirmEdit() {
        saveSharedPrefs()
        finish()
    }
}
