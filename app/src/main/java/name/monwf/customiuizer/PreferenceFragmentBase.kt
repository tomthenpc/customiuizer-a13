package name.monwf.customiuizer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import name.monwf.customiuizer.mods.GlobalActions
import name.monwf.customiuizer.subs.WebPage
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.Helpers
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class PreferenceFragmentBase : PreferenceFragmentCompat() {

    companion object {
        const val PICK_BACKFILE = 11
        const val SAVE_BACKFILE = 12
    }

    private var actContext: Context? = null

    @JvmField
    protected var toolbarMenu = false

    @JvmField
    protected var animDur = 350

    @JvmField
    protected var activeMenus = ""

    @JvmField
    protected var isCustomActionBar = false

    @JvmField
    protected var headLayoutId = 0

    @JvmField
    protected var tailLayoutId = 0

    @JvmField
    protected var pageUrl: String? = null

    @JvmField
    protected val mapKeys = HashMap<Int, String>().apply {
        put(R.id.search_btn, "search")
        put(R.id.restartlauncher, "launcher")
        put(R.id.restartsystemui, "systemui")
        put(R.id.restartsecuritycenter, "securitycenter")
        put(R.id.edit_confirm, "edit")
        put(R.id.softreboot, "reboot")
        put(R.id.backuprestore, "settings")
        put(R.id.resetsettings, "reset")
        put(R.id.about, "about")
        put(R.id.openinweb, "openinweb")
    }

    protected fun getActionBar(): ActionBar? {
        val act = activity as? AppCompatActivity
        return act?.supportActionBar
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (toolbarMenu) {
            inflater.inflate(R.menu.menu_mods, menu)
        }
        if (isCustomActionBar) {
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                item.isVisible = item.itemId == R.id.edit_confirm
            }
        } else {
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                val menuId = item.itemId
                val menuKey = mapKeys[menuId]
                when {
                    activeMenus == "all" && (menuId == R.id.edit_confirm || menuId == R.id.openinweb) -> item.isVisible = false
                    activeMenus == "all" -> item.isVisible = true
                    menuKey != null && activeMenus.contains(menuKey) -> item.isVisible = true
                    else -> item.isVisible = false
                }
            }
        }
    }

    open fun confirmEdit() {}

    @Suppress("DEPRECATION")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = AppHelper.prefsName
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_confirm -> {
                confirmEdit()
                return true
            }
            R.id.restartlauncher -> {
                if (!AppHelper.moduleActive) {
                    showXposedDialog(activity as? AppCompatActivity)
                    return true
                }
                val actionIntent = Intent(GlobalActions.ACTION_PREFIX + "RestartLauncher")
                actionIntent.setPackage("com.android.systemui")
                getValidContext().sendBroadcast(actionIntent)
                return true
            }
            R.id.restartsystemui -> {
                if (!AppHelper.moduleActive) {
                    showXposedDialog(activity as? AppCompatActivity)
                    return true
                }
                val actionIntent = Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI")
                actionIntent.setPackage("com.android.systemui")
                getValidContext().sendBroadcast(actionIntent)
                return true
            }
            R.id.restartsecuritycenter -> {
                val actionIntent = Intent(GlobalActions.ACTION_PREFIX + "RestartSecurityCenter")
                actionIntent.setPackage("com.android.systemui")
                getValidContext().sendBroadcast(actionIntent)
                return true
            }
            R.id.backuprestore -> {
                showBackupRestoreDialog()
                return true
            }
            R.id.openinweb -> {
                Helpers.openURL(getValidContext(), pageUrl)
                return true
            }
            R.id.softreboot -> {
                if (!AppHelper.moduleActive) {
                    showXposedDialog(activity as? AppCompatActivity)
                    return true
                }
                AlertDialog.Builder(getValidContext())
                    .setTitle(R.string.soft_reboot)
                    .setMessage(R.string.soft_reboot_ask)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val intent = Intent(GlobalActions.ACTION_PREFIX + "FastReboot")
                        intent.setPackage("com.android.systemui")
                        getValidContext().sendBroadcast(intent)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                return true
            }
            R.id.about -> {
                openSubFragment(
                    AboutFragment(),
                    null,
                    Helpers.SettingsType.Preference,
                    Helpers.ActionBarType.HomeUp,
                    R.string.app_about,
                    R.xml.prefs_about
                )
                return true
            }
        }
        return false
    }

    fun showXposedDialog(act: AppCompatActivity?) {
        val context = act ?: getValidContext()
        AlertDialog.Builder(context)
            .setTitle(R.string.warning)
            .setMessage(R.string.module_not_active)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .show()
    }

    fun showBackupRestoreDialog() {
        val act = activity as? AppCompatActivity ?: return

        AlertDialog.Builder(act)
            .setTitle(R.string.backup_restore)
            .setMessage(R.string.backup_restore_choose)
            .setPositiveButton(R.string.do_restore) { _, _ ->
                restoreSettings(act)
            }
            .setNegativeButton(R.string.do_backup) { _, _ ->
                backupSettings(act)
            }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun initFragment() {
        setHasOptionsMenu(toolbarMenu)
        val actionBar = getActionBar()

        val showBack = if (this is MainFragment) {
            val act = activity as? AppCompatActivity
            act?.intent?.getBooleanExtra("from.settings", false) ?: false
        } else {
            true
        }

        actionBar?.setDisplayHomeAsUpEnabled(showBack)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    open fun onCreate(savedInstanceState: Bundle?, pref_defaults: Int) {
        super.onCreate(savedInstanceState)
        try {
            PreferenceManager.setDefaultValues(getValidContext(), pref_defaults, false)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    protected open fun fixStubLayout(view: View, position: Int) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (headLayoutId > 0) {
            val vs = view.findViewById<ViewStub>(R.id.head_stub)
            vs?.setLayoutResource(headLayoutId)
            val renderView = vs?.inflate()
            if (renderView != null) fixStubLayout(renderView, 1)
        }
        if (tailLayoutId > 0) {
            val vs = view.findViewById<ViewStub>(R.id.tail_stub)
            vs?.setLayoutResource(tailLayoutId)
            val renderView = vs?.inflate()
            if (renderView != null) fixStubLayout(renderView, 2)
        }
        initFragment()
    }

    fun openWebPage(url: String) {
        val args = Bundle().apply { putString("pageUrl", url) }
        openSubFragment(WebPage(), args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, "", R.layout.fragment_webpage)
    }

    fun openSubFragment(fragment: Fragment, args: Bundle?, settingsType: Helpers.SettingsType, abType: Helpers.ActionBarType, titleResId: Int, contentResId: Int) {
        openSubFragment(fragment, args, settingsType, abType, getString(titleResId), contentResId)
    }

    fun openSubFragment(fragment: Fragment, args: Bundle?, settingsType: Helpers.SettingsType, abType: Helpers.ActionBarType, title: String, contentResId: Int) {
        val bundle = args ?: Bundle()
        bundle.putInt("settingsType", settingsType.ordinal)
        bundle.putInt("abType", abType.ordinal)
        bundle.putString("titleResId", title)
        bundle.putInt("contentResId", contentResId)
        var order = 100.0f
        try {
            order = (view?.translationZ ?: 100.0f) + 0.0f
        } catch (_: Throwable) {}
        bundle.putFloat("order", order)
        if (fragment.arguments == null) {
            fragment.arguments = bundle
        } else {
            fragment.arguments?.clear()
            fragment.arguments?.putAll(bundle)
        }
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.animator.fragment_open_enter,
                R.animator.fragment_open_exit,
                R.animator.fragment_close_enter,
                R.animator.fragment_close_exit
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
        parentFragmentManager.executePendingTransactions()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actContext = context
    }

    override fun onDetach() {
        super.onDetach()
        actContext = null
    }

    fun getValidContext(): Context {
        return actContext ?: (activity?.applicationContext ?: requireContext())
    }

    @Suppress("DEPRECATION")
    fun backupSettings(act: AppCompatActivity?) {
        val context = act ?: getValidContext()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/octet-stream"
        intent.putExtra(
            Intent.EXTRA_TITLE,
            "customiuizer_backup_" + SimpleDateFormat("MMddHHmmss", Locale.US).format(Date())
        )
        startActivityForResult(intent, SAVE_BACKFILE)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == PICK_BACKFILE && resultCode == Activity.RESULT_OK) {
            val uri = resultData?.data
            if (uri != null) {
                doRestoreSettings(uri)
            }
        } else if (requestCode == SAVE_BACKFILE && resultCode == Activity.RESULT_OK) {
            val uri = resultData?.data ?: return
            var output: ObjectOutputStream? = null
            try {
                output = ObjectOutputStream(getValidContext().contentResolver.openOutputStream(uri))
                val prefs = AppHelper.appPrefs ?: return
                output.writeObject(prefs.all)

                AlertDialog.Builder(getValidContext())
                    .setTitle(R.string.do_backup)
                    .setMessage(R.string.backup_ok)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
            } catch (e: Throwable) {
                e.printStackTrace()
                AlertDialog.Builder(getValidContext())
                    .setTitle(R.string.warning)
                    .setMessage(getString(R.string.storage_cannot_backup) + "\n" + e.message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
            } finally {
                try {
                    output?.flush()
                    output?.close()
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun restoreSettings(act: AppCompatActivity?) {
        val activity = act ?: (activity as? AppCompatActivity)
        if (activity == null || !Helpers.checkStorageReadable(activity)) return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/octet-stream"
        startActivityForResult(intent, PICK_BACKFILE)
    }

    @Suppress("UNCHECKED_CAST")
    fun doRestoreSettings(uri: Uri?) {
        val act = activity as? AppCompatActivity ?: return
        if (uri == null) return
        var input: ObjectInputStream? = null
        try {
            input = ObjectInputStream(act.contentResolver.openInputStream(uri))
            val entries = input.readObject() as? Map<String, *>
            if (entries != null) {
                val appPrefs = AppHelper.appPrefs ?: return
                AppHelper.syncPrefsToAnother(entries, appPrefs, 1, null, false)
            }
            AlertDialog.Builder(act)
                .setTitle(R.string.do_restore)
                .setMessage(R.string.restore_ok)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    act.finish()
                    act.startActivity(act.intent)
                }
                .show()
        } catch (t: Throwable) {
            t.printStackTrace()
            AlertDialog.Builder(act)
                .setTitle(R.string.warning)
                .setMessage(R.string.storage_cannot_restore)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .show()
        } finally {
            try {
                input?.close()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
}
