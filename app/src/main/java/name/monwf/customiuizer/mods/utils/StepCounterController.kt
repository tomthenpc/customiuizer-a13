package name.monwf.customiuizer.mods.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.util.ArrayList

object StepCounterController {

    private val stepViewList = ArrayList<TextView>()
    private var mHandler: Handler? = null
    private var updateStepsRunnable: Runnable? = null
    private var stepsWithGoal: String? = null
    private var sContext: Context? = null
    private var sTimeTickReceiver: BroadcastReceiver? = null

    @JvmStatic
    fun updateSteps(context: Context?) {
        if (stepViewList.isEmpty() || context == null) return

        val uri = Uri.parse("content://com.mi.health.provider.main/activity/steps/brief")
        try {
            context.contentResolver.query(
                uri,
                arrayOf("steps", "goal"),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val stepCount = cursor.getString(0)
                    val stepGoal = cursor.getString(1)
                    val newText = "$stepCount/$stepGoal"
                    if (newText == stepsWithGoal) return
                    stepsWithGoal = newText
                    for (tv in stepViewList) {
                        tv.text = newText
                    }
                }
            }
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }

    @JvmStatic
    fun initContext(context: Context) {
        if (sContext != null && sTimeTickReceiver != null) {
            try {
                sContext?.unregisterReceiver(sTimeTickReceiver)
            } catch (_: Throwable) {
            }
            updateStepsRunnable?.let { mHandler?.removeCallbacks(it) }
        }

        sContext = context.applicationContext
        sTimeTickReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateSteps(sContext)
            }
        }
        sContext?.registerReceiver(sTimeTickReceiver, IntentFilter("android.intent.action.TIME_TICK"))

        val looper = Looper.myLooper() ?: Looper.getMainLooper()
        mHandler = Handler(looper)
        updateStepsRunnable = Runnable { updateSteps(sContext) }
    }

    @JvmStatic
    fun removeStepViewByTag(tag: String?) {
        val iterator = stepViewList.iterator()
        while (iterator.hasNext()) {
            if (tag == iterator.next().tag as? String) {
                iterator.remove()
                break
            }
        }
        if (stepViewList.isEmpty()) {
            updateStepsRunnable?.let { mHandler?.removeCallbacks(it) }
        }
    }

    @JvmStatic
    fun addStepView(sv: TextView?) {
        if (sContext == null || sv == null) return
        if (stepViewList.any { it === sv }) return
        stepViewList.add(sv)
        updateStepsRunnable?.let { runnable ->
            mHandler?.removeCallbacks(runnable)
            mHandler?.postDelayed(runnable, 3000L)
        }
    }
}
