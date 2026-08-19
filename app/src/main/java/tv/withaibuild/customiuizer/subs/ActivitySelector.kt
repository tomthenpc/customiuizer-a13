package tv.withaibuild.customiuizer.subs

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragmentWithSearch
import tv.withaibuild.customiuizer.mods.GlobalActions
import tv.withaibuild.customiuizer.utils.AppData
import tv.withaibuild.customiuizer.utils.AppDataAdapter
import tv.withaibuild.customiuizer.utils.Helpers
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics
import java.lang.ref.WeakReference
import java.util.ArrayList

class ActivitySelector : SubFragmentWithSearch() {

    private var pkg: String? = null
    private var key: String? = null
    private var user = 0
    private val activities = ArrayList<AppData>()
    private var pendingActivityLoadStart: Runnable? = null
    private var activityLoadInFlight = false
    private var retryActivityLoadAfterInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        this.padded = false
        super.onCreate(savedInstanceState)
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val args = arguments ?: return
        key = args.getString("key")
        pkg = args.getString("package")
        user = args.getInt("user")

        if (activityLoadInFlight) {
            retryActivityLoadAfterInFlight = true
        } else {
            scheduleActivityLoad()
        }
    }

    private fun scheduleActivityLoad() {
        val postView = view ?: return
        pendingActivityLoadStart?.let { previous ->
            postView.removeCallbacks(previous)
        }
        val appContext = requireContext().applicationContext
        val packageName = pkg ?: ""
        val fragmentRef = WeakReference(this)
        val runnable = object : Runnable {
            override fun run() {
                try {
                    if (pendingActivityLoadStart === this) {
                        pendingActivityLoadStart = null
                    }
                    activityLoadInFlight = true
                    startActivityLoadWorker(appContext, fragmentRef, packageName)
                } catch (e: Throwable) {
                    if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                    SettingsDiagnostics.failure("ActivitySelector.loadActivities.start", e)
                    activityLoadInFlight = false
                    retryActivityLoadAfterInFlight = false
                }
            }
        }
        pendingActivityLoadStart = runnable
        if (!postView.postDelayed(runnable, animDur.toLong())) {
            if (pendingActivityLoadStart === runnable) {
                pendingActivityLoadStart = null
            }
        }
    }

    private fun onActivityLoadFinished(success: Boolean, loadedActivities: ArrayList<AppData>) {
        val retry = retryActivityLoadAfterInFlight
        activityLoadInFlight = false
        retryActivityLoadAfterInFlight = false
        if (success) {
            if (isAdded && view != null) {
                activities.clear()
                activities.addAll(loadedActivities)
                renderActivities()
            }
        } else if (retry && isAdded && view != null) {
            scheduleActivityLoad()
        }
    }

    private fun renderActivities() {
        val context = activity ?: return
        val list = listView ?: return
        if (view == null) return
        if (activities.size == 0) {
            Toast.makeText(context, R.string.no_activities_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        list.adapter = AppDataAdapter(
            context.applicationContext,
            activities,
            Helpers.AppAdapterType.Activities,
            null
        )
        list.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val appData = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@OnItemClickListener
            val intent = Intent(activity, this.javaClass).apply {
                putExtra("activity", "${appData.pkgName}|${appData.actName}")
                putExtra("user", user)
            }
            if (SelectorResultDelivery.canDeliverFromSource(isAdded, targetFragment != null)) {
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                finish()
            }
        }
        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, _, position, _ ->
            val appData = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@OnItemLongClickListener true
            val componentPkg = appData.pkgName ?: ""
            val componentAct = appData.actName ?: ""
            val intent = Intent(activity, this.javaClass).apply {
                component = ComponentName(componentPkg, componentAct)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                putExtra("user", user)
            }
            val bIntent = Intent(GlobalActions.ACTION_PREFIX + "LaunchIntent").apply {
                putExtra("intent", intent)
            }
            activity?.sendBroadcast(bIntent)
            true
        }
        view?.findViewById<View>(R.id.am_progressBar)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        pendingActivityLoadStart?.let { pending ->
            view?.removeCallbacks(pending)
        }
        pendingActivityLoadStart = null
        retryActivityLoadAfterInFlight = false
        activities.clear()
        super.onDestroyView()
    }

    companion object {
        private fun startActivityLoadWorker(
            appContext: Context,
            fragmentRef: WeakReference<ActivitySelector>,
            packageName: String
        ) {
            Thread {
                var success = false
                val loadedActivities = ArrayList<AppData>()
                try {
                    val pm = appContext.packageManager
                    val pi = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                    pi.activities?.forEach { info ->
                        val appData = AppData().apply {
                            pkgName = packageName
                            actName = info.name ?: ""
                            label = info.loadLabel(pm).toString()
                            enabled = info.enabled
                        }
                        loadedActivities.add(appData)
                    }
                    success = true
                } catch (e: Throwable) {
                    if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                    SettingsDiagnostics.failure("ActivitySelector.loadActivities", e)
                }
                appContext.mainExecutor.execute {
                    fragmentRef.get()?.onActivityLoadFinished(success, loadedActivities)
                }
            }.start()
        }
    }
}
