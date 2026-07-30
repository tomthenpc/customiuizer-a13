package tv.withaibuild.customiuizer.mods.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object StepCounterController {

    private data class StepViewRef(
        val ref: WeakReference<TextView>,
        val tag: String?
    )

    private val stepViews = mutableListOf<StepViewRef>()
    private var sContext: Context? = null
    private var queryHandler: Handler? = null
    private var uiHandler: Handler? = null
    private var queryThread: HandlerThread? = null
    private var stepsWithGoal: String? = null
    private val isQuerying = AtomicBoolean(false)

    private var isScreenOn = true
    private var timeTickReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null

    private val queryRunnable = Runnable { runQuery() }

    @JvmStatic
    fun updateSteps(context: Context?) {
        if (context == null) return
        scheduleUpdate()
    }

    @JvmStatic
    fun initContext(context: Context) {
        if (sContext != null) return

        val appContext = context.applicationContext
        sContext = appContext

        queryThread = HandlerThread("StepCounterQuery").apply { start() }
        queryHandler = Handler(queryThread!!.looper)
        uiHandler = Handler(Looper.getMainLooper())

        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isScreenOn = pm?.isInteractive ?: true

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                ModuleHelper.guarded("StepCounterController.screenReceiver") {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_ON -> {
                            isScreenOn = true
                            if (hasLiveViews()) registerTimeTickAndSchedule()
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            isScreenOn = false
                            stopTimeTick()
                        }
                    }
                }
            }
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ModuleHelper.registerModuleReceiver(
            appContext,
            "StepCounterController.screenReceiver",
            screenReceiver!!,
            screenFilter,
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    @JvmStatic
    fun removeStepViewByTag(tag: String?) {
        val iterator = stepViews.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (tag == entry.tag || entry.ref.get() == null) {
                iterator.remove()
            }
        }
        if (!hasLiveViews()) {
            stopTimeTick()
        }
    }

    @JvmStatic
    fun addStepView(sv: TextView?) {
        if (sContext == null || sv == null) return

        cleanExpiredViews()
        if (stepViews.any { it.ref.get() === sv }) return

        stepViews.add(StepViewRef(WeakReference(sv), sv.tag as? String))

        if (isScreenOn) {
            registerTimeTickAndSchedule()
        }
    }

    private fun hasLiveViews(): Boolean {
        cleanExpiredViews()
        return stepViews.isNotEmpty()
    }

    private fun cleanExpiredViews() {
        val iterator = stepViews.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().ref.get() == null) {
                iterator.remove()
            }
        }
    }

    private fun liveViews(): List<TextView> {
        val result = mutableListOf<TextView>()
        val iterator = stepViews.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val view = entry.ref.get()
            if (view == null) {
                iterator.remove()
            } else {
                result.add(view)
            }
        }
        return result
    }

    private fun registerTimeTickAndSchedule() {
        val ctx = sContext ?: return
        if (!hasLiveViews()) {
            stopTimeTick()
            return
        }

        if (timeTickReceiver == null) {
            timeTickReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    ModuleHelper.guarded("StepCounterController.timeTickReceiver") {
                        if (isScreenOn) scheduleUpdate()
                    }
                }
            }
            ModuleHelper.registerModuleReceiver(
                ctx,
                "StepCounterController.timeTick",
                timeTickReceiver!!,
                IntentFilter(Intent.ACTION_TIME_TICK),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
        scheduleUpdate()
    }

    private fun stopTimeTick() {
        queryHandler?.removeCallbacks(queryRunnable)
        timeTickReceiver?.let {
            ModuleHelper.unregisterModuleReceiver("StepCounterController.timeTick")
            timeTickReceiver = null
        }
    }

    private fun scheduleUpdate() {
        if (!isScreenOn || sContext == null) return
        if (!hasLiveViews()) {
            stopTimeTick()
            return
        }
        if (!isQuerying.compareAndSet(false, true)) return

        queryHandler?.removeCallbacks(queryRunnable)
        queryHandler?.post(queryRunnable)
    }

    private fun runQuery() {
        val context = sContext
        if (context == null) {
            isQuerying.set(false)
            return
        }

        val newText = try {
            querySteps(context)
        } catch (t: Throwable) {
            XposedHelpers.log(t)
            null
        } finally {
            isQuerying.set(false)
        }

        if (newText != null) {
            uiHandler?.post { updateViews(newText) }
        }
    }

    private fun querySteps(context: Context): String? {
        val uri = Uri.parse("content://com.mi.health.provider.main/activity/steps/brief")
        return context.contentResolver.query(
            uri,
            arrayOf("steps", "goal"),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val stepCount = cursor.getString(0)
                val stepGoal = cursor.getString(1)
                "$stepCount/$stepGoal"
            } else null
        }
    }

    private fun updateViews(newText: String) {
        if (!isScreenOn) return

        val views = liveViews()
        if (views.isEmpty()) return

        if (newText == stepsWithGoal) return
        stepsWithGoal = newText

        for (view in views) {
            view.text = newText
        }
    }
}
