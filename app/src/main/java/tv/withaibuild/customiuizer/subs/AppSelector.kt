@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
package tv.withaibuild.customiuizer.subs

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragmentWithSearch
import tv.withaibuild.customiuizer.utils.AppData
import tv.withaibuild.customiuizer.utils.AppDataAdapter
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.Helpers
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics
import tv.withaibuild.customiuizer.utils.LockedAppAdapter
import tv.withaibuild.customiuizer.utils.PrivacyAppAdapter
import java.lang.ref.WeakReference

class AppSelector : SubFragmentWithSearch() {

    private var initialized = false
    private var standalone = false
    private var multi = false
    private var bwlist = false
    private var privacy = false
    private var applock = false
    private var customTitles = false
    private var share = false
    private var openwith = false
    private var isActivity = false
    private var key: String? = null
    private var process: Runnable? = null
    private var pendingAppLoadStart: Runnable? = null
    private var appLoadInFlight = false
    private var retryAppLoadAfterInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        this.padded = false
        super.onCreate(savedInstanceState)

        val args = arguments ?: Bundle()
        standalone = args.getBoolean("standalone")
        multi = args.getBoolean("multi")
        bwlist = args.getBoolean("bw")
        privacy = args.getBoolean("privacy")
        applock = args.getBoolean("applock")
        customTitles = args.getBoolean("custom_titles")
        share = args.getBoolean("share")
        openwith = args.getBoolean("openwith")
        isActivity = args.getBoolean("activity")
        key = args.getString("key")

        process = Runnable {
            val context = getValidContext()
            if (!isAdded) return@Runnable
            when {
                multi && key != null -> {
                    val list = when {
                        openwith -> Helpers.openWithAppsList
                        share -> Helpers.shareAppsList
                        else -> Helpers.installedAppsList
                    } ?: return@Runnable
                    listView?.adapter = AppDataAdapter(
                        context,
                        list,
                        Helpers.AppAdapterType.Mutli,
                        key,
                        bwlist
                    )
                }
                privacy -> {
                    val list = Helpers.installedAppsList ?: return@Runnable
                    listView?.adapter = PrivacyAppAdapter(context, list)
                }
                applock -> {
                    val list = Helpers.installedAppsList ?: return@Runnable
                    listView?.adapter = LockedAppAdapter(context, list)
                }
                customTitles -> {
                    val list = Helpers.launchableAppsList ?: return@Runnable
                    listView?.adapter = AppDataAdapter(context, list, Helpers.AppAdapterType.CustomTitles, key)
                }
                standalone && key != null -> {
                    val list = Helpers.launchableAppsList ?: return@Runnable
                    listView?.adapter = AppDataAdapter(context, list, Helpers.AppAdapterType.Standalone, key)
                }
                isActivity -> {
                    val list = Helpers.installedAppsList ?: return@Runnable
                    listView?.adapter = AppDataAdapter(context, list, Helpers.AppAdapterType.Default, key)
                }
                else -> {
                    val list = Helpers.launchableAppsList ?: return@Runnable
                    listView?.adapter = AppDataAdapter(context, list)
                }
            }
            listView?.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val key = this@AppSelector.key ?: ""
                if (multi && key.isNotEmpty()) {
                    val app = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@OnItemClickListener
                    val selectedApps = java.util.LinkedHashSet(
                        AppHelper.getStringSetOfAppPrefs(key, emptySet()) ?: emptySet()
                    )
                    var selectedAppsBlack: java.util.LinkedHashSet<String>? = null
                    if (bwlist) {
                        selectedAppsBlack = java.util.LinkedHashSet(
                            AppHelper.getStringSetOfAppPrefs(key + "_black", emptySet()) ?: emptySet()
                        )
                        val pkg = app.pkgName.orEmpty()
                        when {
                            selectedApps.contains(pkg) -> {
                                selectedApps.remove(pkg)
                                selectedAppsBlack?.add(pkg)
                            }
                            (selectedAppsBlack?.contains(pkg) ?: false) -> {
                                selectedApps.remove(pkg)
                                selectedAppsBlack?.remove(pkg)
                            }
                            else -> {
                                selectedApps.add(pkg)
                                selectedAppsBlack?.remove(pkg)
                            }
                        }
                    } else if (selectedApps.contains(if (share || openwith) "${app.pkgName}|${app.user}" else app.pkgName)) {
                        selectedApps.remove(if (share || openwith) "${app.pkgName}|${app.user}" else app.pkgName)
                    } else {
                        selectedApps.add(if (share || openwith) "${app.pkgName}|${app.user}" else app.pkgName)
                        if (openwith) {
                            val mimeKey = key + "_" + app.pkgName + "|" + app.user
                            val mimeFlags = AppHelper.getIntOfAppPrefs(mimeKey, Helpers.MimeType.ALL)
                            val checkedTypes = booleanArrayOf(
                                (mimeFlags and Helpers.MimeType.IMAGE) == Helpers.MimeType.IMAGE,
                                (mimeFlags and Helpers.MimeType.AUDIO) == Helpers.MimeType.AUDIO,
                                (mimeFlags and Helpers.MimeType.VIDEO) == Helpers.MimeType.VIDEO,
                                (mimeFlags and Helpers.MimeType.DOCUMENT) == Helpers.MimeType.DOCUMENT,
                                (mimeFlags and Helpers.MimeType.ARCHIVE) == Helpers.MimeType.ARCHIVE,
                                (mimeFlags and Helpers.MimeType.LINK) == Helpers.MimeType.LINK,
                                (mimeFlags and Helpers.MimeType.OTHERS) == Helpers.MimeType.OTHERS
                            )
                            val builder = AlertDialog.Builder(activity ?: return@OnItemClickListener)
                            builder.setTitle(R.string.system_cleanopenwith_datatype)
                            builder.setMultiChoiceItems(R.array.mimetypes, checkedTypes) { _, which, isChecked ->
                                checkedTypes[which] = isChecked
                            }
                            builder.setCancelable(true)
                            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                                var sum = 0
                                for (i in checkedTypes.indices) {
                                    if (checkedTypes[i]) sum += 1 shl i
                                }
                                AppHelper.appPrefs?.edit()?.putInt(mimeKey, sum)?.apply()
                            }
                            builder.show()
                        }
                    }

                    val adapter = parent.adapter as? AppDataAdapter
                    val editor = AppHelper.appPrefs?.edit()
                    if (bwlist) {
                        editor?.putStringSet(key + "_black", selectedAppsBlack ?: emptySet())
                    }
                    editor?.putStringSet(key, selectedApps)?.apply()
                    adapter?.updateSelectedApps(selectedApps, selectedAppsBlack)
                } else if (isActivity) {
                    val app = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@OnItemClickListener
                    val args2 = Bundle().apply {
                        putString("key", key)
                        putString("package", app.pkgName)
                        putInt("user", app.user)
                    }
                    val activitySelect = ActivitySelector()
                    @Suppress("DEPRECATION")
                    activitySelect.setTargetFragment(this@AppSelector, targetRequestCode)
                    openSubFragment(
                        activitySelect,
                        args2,
                        Helpers.SettingsType.Edit,
                        Helpers.ActionBarType.HomeUp,
                        R.string.select_activity,
                        R.layout.prefs_app_selector
                    )
                } else if (privacy) {
                    val app = (parent.adapter as? PrivacyAppAdapter)?.getItem(position) ?: return@OnItemClickListener
                    try {
                        @Suppress("WrongConstant")
                        val mSecurityManager = activity?.getSystemService("security") ?: return@OnItemClickListener
                        val isPrivacyApp = mSecurityManager::class.java.getDeclaredMethod("isPrivacyApp", String::class.java, Int::class.javaPrimitiveType)
                        isPrivacyApp.isAccessible = true
                        val setPrivacyApp = mSecurityManager::class.java.getDeclaredMethod("setPrivacyApp", String::class.java, Int::class.javaPrimitiveType, java.lang.Boolean.TYPE)
                        setPrivacyApp.isAccessible = true
                        setPrivacyApp.invoke(mSecurityManager, app.pkgName, app.user, !(isPrivacyApp.invoke(mSecurityManager, app.pkgName, app.user) as? Boolean ?: false))
                        (parent.adapter as? PrivacyAppAdapter)?.refresh(app)
                        activity?.contentResolver?.notifyChange(
                            Uri.parse("content://com.miui.securitycenter.provider/update_privacyapps_icon"),
                            null
                        )
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                        SettingsDiagnostics.failure("AppSelector.privacy.toggle", t)
                    }
                } else if (applock) {
                    val app = (parent.adapter as? LockedAppAdapter)?.getItem(position) ?: return@OnItemClickListener
                    try {
                        @Suppress("WrongConstant")
                        val mSecurityManager = activity?.getSystemService("security") ?: return@OnItemClickListener
                        val getApplicationAccessControlEnabledAsUser = mSecurityManager::class.java.getDeclaredMethod(
                            "getApplicationAccessControlEnabledAsUser",
                            String::class.java,
                            Int::class.javaPrimitiveType
                        )
                        getApplicationAccessControlEnabledAsUser.isAccessible = true
                        val setApplicationAccessControlEnabledForUser = mSecurityManager::class.java.getDeclaredMethod(
                            "setApplicationAccessControlEnabledForUser",
                            String::class.java,
                            java.lang.Boolean.TYPE,
                            Int::class.javaPrimitiveType
                        )
                        setApplicationAccessControlEnabledForUser.isAccessible = true
                        val currentEnabled = getApplicationAccessControlEnabledAsUser.invoke(mSecurityManager, app.pkgName, app.user) as? Boolean ?: false
                        setApplicationAccessControlEnabledForUser.invoke(mSecurityManager, app.pkgName, !currentEnabled, app.user)
                        (parent.adapter as? LockedAppAdapter)?.refresh(app)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                        SettingsDiagnostics.failure("AppSelector.applock.toggle", t)
                    }
                } else if (customTitles) {
                    val app = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@OnItemClickListener
                    AppHelper.showInputDialog(
                        activity ?: return@OnItemClickListener,
                        key + ":" + app.pkgName + "|" + app.actName + "|" + app.user,
                        R.string.launcher_renameapps_modified,
                        0,
                        1,
                        object : Helpers.InputCallback {
                            override fun onInputFinished(key: String, text: String) {
                                if (TextUtils.isEmpty(text)) {
                                    AppHelper.appPrefs?.edit()?.remove(key)?.apply()
                                } else {
                                    AppHelper.appPrefs?.edit()?.putString(key, text)?.apply()
                                }
                                (parent.adapter as? AppDataAdapter)?.notifyDataSetChanged()
                            }
                        }
                    )
                } else {
                    val intent = Intent(activity ?: return@OnItemClickListener, this@AppSelector::class.java)
                    val app = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@OnItemClickListener
                    if (app.pkgName == "" && app.actName == "") {
                        intent.putExtra("app", "")
                    } else {
                        intent.putExtra("app", app.pkgName + "|" + app.actName)
                    }
                    intent.putExtra("user", app.user)
                    @Suppress("DEPRECATION")
                    if (SelectorResultDelivery.canDeliverFromSource(isAdded, targetFragment != null)) {
                        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                        finish()
                    }
                }
            }
            view?.findViewById<View>(R.id.am_progressBar)?.visibility = View.GONE
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (initialized) {
            process?.run()
        } else if (appLoadInFlight) {
            retryAppLoadAfterInFlight = true
        } else {
            scheduleAppLoad()
        }
    }

    private fun scheduleAppLoad() {
        val postView = view ?: return
        pendingAppLoadStart?.let { previous ->
            postView.removeCallbacks(previous)
        }
        val appContext = requireContext().applicationContext
        val fragmentRef = WeakReference(this)
        val loadIsActivity = isActivity
        val loadPrivacy = privacy
        val loadApplock = applock
        val loadMulti = multi
        val loadKey = key
        val loadOpenwith = openwith
        val loadShare = share
        val runnable = object : Runnable {
            override fun run() {
                try {
                    if (pendingAppLoadStart === this) {
                        pendingAppLoadStart = null
                    }
                    appLoadInFlight = true
                    startAppLoadWorker(
                        appContext, fragmentRef,
                        loadIsActivity, loadPrivacy, loadApplock,
                        loadMulti, loadKey, loadOpenwith, loadShare
                    )
                } catch (e: Throwable) {
                    if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                    SettingsDiagnostics.failure("AppSelector.loadApps.start", e)
                    appLoadInFlight = false
                    retryAppLoadAfterInFlight = false
                }
            }
        }
        pendingAppLoadStart = runnable
        if (!postView.postDelayed(runnable, animDur.toLong())) {
            if (pendingAppLoadStart === runnable) {
                pendingAppLoadStart = null
            }
        }
    }

    private fun onAppLoadFinished(success: Boolean) {
        val retry = retryAppLoadAfterInFlight
        appLoadInFlight = false
        retryAppLoadAfterInFlight = false
        if (success) {
            initialized = true
            if (isAdded && view != null) {
                process?.run()
            }
        } else if (retry && isAdded && view != null) {
            scheduleAppLoad()
        }
    }

    override fun onDestroyView() {
        pendingAppLoadStart?.let { pending ->
            view?.removeCallbacks(pending)
        }
        pendingAppLoadStart = null
        retryAppLoadAfterInFlight = false
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == targetRequestCode) {
            @Suppress("DEPRECATION")
            if (SelectorResultDelivery.canAcceptAtBackStackTarget(targetFragment != null, isAdded)) {
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, data)
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private fun startAppLoadWorker(
            appContext: Context,
            fragmentRef: WeakReference<AppSelector>,
            loadIsActivity: Boolean,
            loadPrivacy: Boolean,
            loadApplock: Boolean,
            loadMulti: Boolean,
            loadKey: String?,
            loadOpenwith: Boolean,
            loadShare: Boolean
        ) {
            Thread {
                var success = false
                try {
                    if (loadIsActivity || loadPrivacy || loadApplock || (loadMulti && loadKey != null)) {
                        if (loadOpenwith) {
                            if (Helpers.openWithAppsList == null) Helpers.getOpenWithApps(appContext)
                        } else if (loadShare) {
                            if (Helpers.shareAppsList == null) Helpers.getShareApps(appContext)
                        } else {
                            if (Helpers.installedAppsList == null) Helpers.getInstalledApps(appContext)
                        }
                    } else {
                        if (Helpers.launchableAppsList == null) Helpers.getLaunchableApps(appContext)
                    }
                    success = true
                } catch (e: Throwable) {
                    if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                    SettingsDiagnostics.failure("AppSelector.loadApps", e)
                }
                appContext.mainExecutor.execute {
                    fragmentRef.get()?.onAppLoadFinished(success)
                }
            }.start()
        }
    }
}
