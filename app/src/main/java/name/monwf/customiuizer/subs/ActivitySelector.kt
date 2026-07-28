package name.monwf.customiuizer.subs

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import name.monwf.customiuizer.R
import name.monwf.customiuizer.SubFragmentWithSearch
import name.monwf.customiuizer.mods.GlobalActions
import name.monwf.customiuizer.utils.AppData
import name.monwf.customiuizer.utils.AppDataAdapter
import name.monwf.customiuizer.utils.Helpers
import java.util.ArrayList

class ActivitySelector : SubFragmentWithSearch() {

    private var pkg: String? = null
    private var key: String? = null
    private var user = 0
    private val activities = ArrayList<AppData>()

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

        val process = Runnable {
            val context = activity ?: return@Runnable
            if (activities.size == 0) {
                Toast.makeText(context, R.string.no_activities_found, Toast.LENGTH_SHORT).show()
                finish()
                return@Runnable
            }
            listView?.adapter = AppDataAdapter(
                context.applicationContext,
                activities,
                Helpers.AppAdapterType.Activities,
                null
            )
            listView?.setOnItemClickListener { parent, _, position, _ ->
                val appData = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@setOnItemClickListener
                val intent = Intent(activity, this.javaClass).apply {
                    putExtra("activity", "${appData.pkgName}|${appData.actName}")
                    putExtra("user", user)
                }
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                finish()
            }
            listView?.setOnItemLongClickListener { parent, _, position, _ ->
                val appData = (parent.adapter as? AppDataAdapter)?.getItem(position) ?: return@setOnItemLongClickListener true
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

        Thread {
            try {
                Thread.sleep(animDur.toLong())
            } catch (_: Throwable) {}
            try {
                val act = activity ?: return@Thread
                activities.clear()
                val pm = act.packageManager
                val pi = pm.getPackageInfo(pkg ?: "", PackageManager.GET_ACTIVITIES)
                pi.activities?.forEach { info ->
                    val appData = AppData().apply {
                        pkgName = pkg
                        actName = info.name ?: ""
                        label = info.loadLabel(pm).toString()
                        enabled = info.enabled
                    }
                    activities.add(appData)
                }
                act.runOnUiThread(process)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }.start()
    }
}
