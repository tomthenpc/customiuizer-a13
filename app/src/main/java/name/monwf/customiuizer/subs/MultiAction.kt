package name.monwf.customiuizer.subs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import name.monwf.customiuizer.R
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.prefs.SpinnerEx
import name.monwf.customiuizer.prefs.SpinnerExFake
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.Helpers
import java.io.File

@Suppress("DEPRECATION")
class MultiAction : SubFragment() {

    enum class Actions {
        NAVBAR, LAUNCHER, CONTROLS, LOCKSCREEN, LAUNCH, STATUSBAR
    }

    private var appLaunch: SpinnerExFake? = null
    private var shortcutLaunch: SpinnerExFake? = null
    private var activityLaunch: SpinnerExFake? = null
    private var key: String? = null
    private var appValue: String? = null
    private var appUser: Int = -1
    private var activityValue: String? = null
    private var activityUser: Int = -1
    private var shortcutValue: String? = null
    private var shortcutName: String? = null
    private var shortcutIcon: String? = null
    private lateinit var shortcutIconPath: String
    private var shortcutIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        this.padded = false
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val args = arguments ?: return
        key = args.getString("key") ?: return
        val actions = Actions.values()[args.getInt("actions")]

        val (entriesResId, entryValuesResId) = when (actions) {
            Actions.NAVBAR -> Pair(R.array.global_actions_navbar, R.array.global_actions_navbar_val)
            Actions.LAUNCHER -> Pair(R.array.global_actions_launcher, R.array.global_actions_launcher_val)
            Actions.CONTROLS -> Pair(R.array.global_actions_controls, R.array.global_actions_controls_val)
            Actions.STATUSBAR -> Pair(R.array.global_actions_statusbar, R.array.global_actions_statusbar_val)
            Actions.LOCKSCREEN -> Pair(R.array.global_lockscreen_actions, R.array.global_lockscreen_actions_val)
            Actions.LAUNCH -> Pair(R.array.global_launch_actions, R.array.global_launch_actions_val)
        }

        val view = view ?: return

        val actionSpinner = view.findViewById<SpinnerEx>(R.id.action)
        @Suppress("UNCHECKED_CAST")
        actionSpinner.entries = resources.getStringArray(entriesResId) as Array<CharSequence>
        actionSpinner.entryValues = resources.getIntArray(entryValuesResId)
        actionSpinner.tag = "${key}_action"
        actionSpinner.init(AppHelper.getIntOfAppPrefs("${key}_action", 1))
        actionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateControls(parent as? SpinnerEx ?: return, position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateControls(parent as? SpinnerEx ?: return, 0)
            }
        }

        appLaunch = view.findViewById(R.id.app_to_launch)
        appLaunch?.apply {
            tag = "${key}_app"
            setValue(appValue ?: AppHelper.getStringOfAppPrefs("${key}_app", null) ?: "")
            setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (child is TextView && child.id == android.R.id.text1) {
                        val pkgAppName = getValue()
                        if (pkgAppName != null) {
                            val label = Helpers.getAppName(context, pkgAppName)
                            if (label != null) {
                                val user = if (appUser != -1) appUser else AppHelper.getIntOfAppPrefs("${key}_app_user", 0)
                                child.text = "$label${if (user != 0) " *" else ""}"
                                return
                            }
                        }
                        child.setText(R.string.notselected)
                        child.alpha = 0.5f
                    }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
            setOnTouchListener(View.OnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val appSelect = AppSelector()
                    appSelect.setTargetFragment(this@MultiAction, 0)
                    openSubFragment(appSelect, null, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_app, R.layout.prefs_app_selector)
                }
                false
            })
        }

        shortcutLaunch = view.findViewById(R.id.shortcut_to_launch)
        shortcutLaunch?.apply {
            tag = "${key}_shortcut"
            setValue(shortcutValue ?: AppHelper.getStringOfAppPrefs("${key}_shortcut", null) ?: "")
            addValue("${key}_shortcut_intent", shortcutIntent)
            addValue("${key}_shortcut_name", shortcutName)

            shortcutIconPath = context.filesDir.absolutePath + "/shortcuts/" + key + "_shortcut.png"
            val shortcutIconFile = shortcutIcon?.let { File(it) } ?: File(shortcutIconPath)
            if (shortcutIconFile.exists()) {
                val sIcon = view.findViewById<ImageView>(R.id.shortcut_icon)
                val sBmp = BitmapFactory.decodeFile(shortcutIconFile.absolutePath)
                sIcon.setImageBitmap(sBmp)
            }

            setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (child is TextView && child.id == android.R.id.text1) {
                        val pkgAppName = getValue()
                        if (pkgAppName != null) {
                            val label = Helpers.getAppName(context, pkgAppName)
                            if (label != null) {
                                child.text = label
                                return
                            }
                        }
                        child.setText(R.string.notselected)
                        child.alpha = 0.5f
                    }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
            setOnTouchListener(View.OnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val shortcutSelect = ShortcutSelector()
                    shortcutSelect.setTargetFragment(this@MultiAction, 1)
                    val bundle = Bundle().apply {
                        putString("key", "${key}_shortcut")
                    }
                    openSubFragment(shortcutSelect, bundle, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_shortcut, R.layout.prefs_app_selector)
                }
                false
            })
        }

        val toggleSpinner = view.findViewById<SpinnerEx>(R.id.toggle)
        toggleSpinner.tag = "${key}_toggle"
        toggleSpinner.init(AppHelper.getIntOfAppPrefs("${key}_toggle", 1))

        activityLaunch = view.findViewById(R.id.activity_to_launch)
        activityLaunch?.apply {
            tag = "${key}_activity"
            setValue(activityValue ?: AppHelper.getStringOfAppPrefs("${key}_activity", null) ?: "")
            val value = getValue()
            val textView = view.findViewById<TextView>(R.id.activity_class)
            textView.text = if (value != null && value != "") {
                value.replace("|", "/\u200B").replace(".", ".\u200B")
            } else ""
            setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (child is TextView && child.id == android.R.id.text1) {
                        val pkgAppName = getValue()
                        if (pkgAppName != null) {
                            val label = Helpers.getAppName(context, pkgAppName, true)
                            if (label != null) {
                                val user = if (activityUser != -1) activityUser else AppHelper.getIntOfAppPrefs("${key}_activity_user", 0)
                                child.text = "$label${if (user != 0) " *" else ""}"
                                return
                            }
                        }
                        child.setText(R.string.notselected)
                        child.alpha = 0.5f
                    }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
            setOnTouchListener(View.OnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val bundle = Bundle().apply {
                        putBoolean("activity", true)
                    }
                    val activitySelect = AppSelector()
                    activitySelect.setTargetFragment(this@MultiAction, 2)
                    openSubFragment(activitySelect, bundle, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.select_app, R.layout.prefs_app_selector)
                }
                false
            })
        }
    }

    private fun updateControls(spinner: SpinnerEx, position: Int) {
        val view = view ?: return
        val apps = view.findViewById<View>(R.id.apps_group)
        val shortcuts = view.findViewById<View>(R.id.shortcuts_group)
        val activities = view.findViewById<View>(R.id.activities_group)
        val toggles = view.findViewById<View>(R.id.toggles_group)

        apps.visibility = View.GONE
        shortcuts.visibility = View.GONE
        activities.visibility = View.GONE
        toggles.visibility = View.GONE
        when (spinner.entryValues?.get(position)) {
            8 -> apps.visibility = View.VISIBLE
            9 -> shortcuts.visibility = View.VISIBLE
            10 -> toggles.visibility = View.VISIBLE
            20 -> activities.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                0 -> {
                    appValue = data?.getStringExtra("app")
                    appUser = data?.getIntExtra("user", 0) ?: 0
                }
                1 -> {
                    shortcutValue = data?.getStringExtra("shortcut_contents")
                    shortcutName = data?.getStringExtra("shortcut_name")
                    shortcutIcon = data?.getStringExtra("shortcut_icon")
                    shortcutIntent = data?.getParcelableExtra("shortcut_intent")
                }
                2 -> {
                    activityValue = data?.getStringExtra("activity")
                    activityUser = data?.getIntExtra("user", 0) ?: 0
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun saveSharedPrefs() {
        val filesDir = context?.filesDir ?: return
        val tmpIconFile = File(filesDir, "shortcuts/tmp.png")
        if (tmpIconFile.exists()) {
            val prefIconFile = File(shortcutIconPath)
            prefIconFile?.delete()
            tmpIconFile.renameTo(prefIconFile)
        }
        if (appUser != -1) AppHelper.appPrefs?.edit()?.putInt("${key}_app_user", appUser)?.apply()
        if (activityUser != -1) AppHelper.appPrefs?.edit()?.putInt("${key}_activity_user", activityUser)?.apply()
        super.saveSharedPrefs()
    }

    override fun onDestroy() {
        val filesDir = context?.filesDir
        if (filesDir != null) {
            val tmpIconFile = File(filesDir, "shortcuts/tmp.png")
            if (tmpIconFile.exists()) tmpIconFile.delete()
        }
        super.onDestroy()
    }
}
