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

/**
 * Owns the Control Center step counter view lifecycle and the background query.
 *
 * The controller deliberately splits scheduling policy into the testable
 * [Lifecycle] class while keeping Android-specific registration and ContentResolver
 * access in the object. This keeps the scheduling rules unit-testable without a
 * full SystemUI environment.
 */
object StepCounterController {

    /**
     * Pure scheduling state for the step counter. Visible to unit tests in the same
     * module so the screen-on/off, view add/remove, and query-at-most-once logic
     * can be exercised without an Android runtime.
     */
    internal class Lifecycle {
        private val _screenOn = AtomicBoolean(true)
        private val _hasViews = AtomicBoolean(false)
        private val _timeTickRegistered = AtomicBoolean(false)
        private val _isQuerying = AtomicBoolean(false)

        val screenOn: Boolean get() = _screenOn.get()
        val hasViews: Boolean get() = _hasViews.get()
        val timeTickRegistered: Boolean get() = _timeTickRegistered.get()
        val isQuerying: Boolean get() = _isQuerying.get()

        fun onScreenOn() {
            _screenOn.set(true)
        }

        fun onScreenOff() {
            _screenOn.set(false)
            _timeTickRegistered.set(false)
        }

        fun setHasViews(value: Boolean) {
            _hasViews.set(value)
            if (!value) {
                _timeTickRegistered.set(false)
            }
        }

        /**
         * Records that the time-tick receiver should be considered registered.
         * Returns `true` if the state changed; repeated calls are idempotent.
         */
        fun registerTimeTick(): Boolean {
            if (_timeTickRegistered.get()) return true
            if (!_screenOn.get() || !_hasViews.get()) return false
            return _timeTickRegistered.compareAndSet(false, true)
        }

        fun unregisterTimeTick() {
            _timeTickRegistered.set(false)
        }

        fun canSchedule(): Boolean = _screenOn.get() && _hasViews.get() && !_isQuerying.get()

        fun tryStartQuery(): Boolean {
            if (!_screenOn.get() || !_hasViews.get()) return false
            return _isQuerying.compareAndSet(false, true)
        }

        fun finishQuery() {
            _isQuerying.set(false)
        }

        fun stopQuery() {
            _isQuerying.set(false)
        }

        fun reset() {
            _screenOn.set(true)
            _hasViews.set(false)
            _timeTickRegistered.set(false)
            _isQuerying.set(false)
        }
    }

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

    private var timeTickReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null

    @JvmField
    internal val lifecycle = Lifecycle()

    private val queryRunnable = Runnable {
        ModuleHelper.guarded("StepCounterController.queryRunnable") { runQuery() }
    }

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

        lifecycle.reset()
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (pm?.isInteractive == true) {
            lifecycle.onScreenOn()
        } else {
            lifecycle.onScreenOff()
        }

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                ModuleHelper.guarded("StepCounterController.screenReceiver") {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_ON -> {
                            lifecycle.onScreenOn()
                            if (hasLiveViews()) registerTimeTickAndSchedule()
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            lifecycle.onScreenOff()
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
            lifecycle.setHasViews(false)
            stopTimeTick()
        } else {
            lifecycle.setHasViews(true)
        }
    }

    @JvmStatic
    fun addStepView(sv: TextView?) {
        if (sContext == null || sv == null) return

        cleanExpiredViews()
        if (stepViews.any { it.ref.get() === sv }) return

        stepViews.add(StepViewRef(WeakReference(sv), sv.tag as? String))

        if (hasLiveViews()) {
            lifecycle.setHasViews(true)
            if (lifecycle.screenOn) {
                registerTimeTickAndSchedule()
            }
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
            lifecycle.setHasViews(false)
            stopTimeTick()
            return
        }

        if (timeTickReceiver == null) {
            timeTickReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    ModuleHelper.guarded("StepCounterController.timeTickReceiver") {
                        if (lifecycle.screenOn) scheduleUpdate()
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

        if (lifecycle.registerTimeTick()) {
            scheduleUpdate()
        }
    }

    private fun stopTimeTick() {
        queryHandler?.removeCallbacks(queryRunnable)
        lifecycle.unregisterTimeTick()
        timeTickReceiver?.let {
            ModuleHelper.unregisterModuleReceiver("StepCounterController.timeTick")
            timeTickReceiver = null
        }
    }

    private fun scheduleUpdate() {
        if (!lifecycle.screenOn || sContext == null) return
        if (!hasLiveViews()) {
            lifecycle.setHasViews(false)
            stopTimeTick()
            return
        }
        if (!lifecycle.tryStartQuery()) return

        queryHandler?.removeCallbacks(queryRunnable)
        queryHandler?.post(queryRunnable)
    }

    private fun runQuery() {
        val context = sContext
        if (context == null) {
            lifecycle.finishQuery()
            return
        }

        val newText = try {
            querySteps(context)
        } catch (t: Throwable) {
            XposedHelpers.log(t)
            null
        } finally {
            lifecycle.finishQuery()
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
        if (!lifecycle.screenOn) return

        val views = liveViews()
        if (views.isEmpty()) {
            lifecycle.setHasViews(false)
            stopTimeTick()
            return
        }

        if (newText == stepsWithGoal) return
        stepsWithGoal = newText

        for (view in views) {
            view.text = newText
        }
    }
}
