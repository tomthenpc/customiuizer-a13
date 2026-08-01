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
import tv.withaibuild.customiuizer.MainModule
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

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
    internal class QueryTicket(val generation: Long, val queryId: Long)

    internal class Lifecycle {
        private val lock = Any()
        private var screenOnState = true
        private var hasViewsState = false
        private var timeTickRegisteredState = false
        private var generationState = 0L
        private var nextQueryIdState = 0L
        private var activeTicketState: QueryTicket? = null
        private var latestValidQueryIdState = 0L

        val screenOn: Boolean get() = synchronized(lock) { screenOnState }
        val hasViews: Boolean get() = synchronized(lock) { hasViewsState }
        val timeTickRegistered: Boolean get() = synchronized(lock) { timeTickRegisteredState }
        val isQuerying: Boolean get() = synchronized(lock) { activeTicketState != null }
        val generation: Long get() = synchronized(lock) { generationState }

        fun onScreenOn() = synchronized(lock) {
            screenOnState = true
        }

        fun onScreenOff() = synchronized(lock) {
            screenOnState = false
            timeTickRegisteredState = false
            invalidateLocked()
        }

        fun setHasViews(value: Boolean) = synchronized(lock) {
            hasViewsState = value
            if (!value) {
                timeTickRegisteredState = false
                invalidateLocked()
            }
        }

        /**
         * Records that the time-tick receiver should be considered registered.
         * Returns `true` if the state changed; repeated calls are idempotent.
         */
        fun registerTimeTick(): Boolean = synchronized(lock) {
            if (timeTickRegisteredState) return true
            if (!screenOnState || !hasViewsState) return false
            timeTickRegisteredState = true
            return true
        }

        fun unregisterTimeTick() = synchronized(lock) {
            timeTickRegisteredState = false
        }

        fun canSchedule(): Boolean = synchronized(lock) {
            screenOnState && hasViewsState && activeTicketState == null
        }

        fun tryStartQuery(): QueryTicket? = synchronized(lock) {
            if (!screenOnState || !hasViewsState || activeTicketState != null) return null
            val ticket = QueryTicket(generationState, ++nextQueryIdState)
            activeTicketState = ticket
            latestValidQueryIdState = ticket.queryId
            ticket
        }

        fun finishQuery(ticket: QueryTicket): Boolean = synchronized(lock) {
            if (activeTicketState === ticket) {
                activeTicketState = null
                true
            } else {
                false
            }
        }

        fun isCurrent(ticket: QueryTicket): Boolean = synchronized(lock) {
            ticket.queryId == latestValidQueryIdState && ticket.generation == generationState
        }

        fun isCurrent(gen: Long): Boolean = synchronized(lock) { generationState == gen }

        fun consumeResult(ticket: QueryTicket): Boolean = synchronized(lock) {
            if (ticket.queryId == latestValidQueryIdState && ticket.generation == generationState) {
                latestValidQueryIdState = 0L
                true
            } else {
                false
            }
        }

        fun invalidate() = synchronized(lock) { invalidateLocked() }

        fun bumpGeneration(): Long = synchronized(lock) { ++generationState }

        fun reset() = synchronized(lock) {
            screenOnState = true
            hasViewsState = false
            timeTickRegisteredState = false
            activeTicketState = null
            latestValidQueryIdState = 0L
            ++generationState
        }

        private fun invalidateLocked() {
            activeTicketState = null
            latestValidQueryIdState = 0L
            ++generationState
        }
    }

    private data class StepViewRef(
        val ref: WeakReference<TextView>,
        val tag: String?
    )

    private class QueryRunnable(
        val ticket: QueryTicket,
        val context: Context
    ) : Runnable {
        override fun run() = runQuery(this)
    }

    private val stepViews = mutableListOf<StepViewRef>()
    private val viewLock = Any()
    private var sContext: Context? = null
    private var queryHandler: Handler? = null
    private var uiHandler: Handler? = null
    private var queryThread: HandlerThread? = null
    private var stepsWithGoal: String? = null

    private var timeTickReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var disableObserver: ModuleHelper.PreferenceObserver? = null
    internal val pendingQueryRunnable = AtomicReference<Runnable?>(null)

    @JvmField
    internal val lifecycle = Lifecycle()

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
        val registered = ModuleHelper.registerModuleReceiver(
            appContext,
            "StepCounterController.screenReceiver",
            screenReceiver!!,
            screenFilter,
            Context.RECEIVER_NOT_EXPORTED
        )
        if (!registered) {
            screenReceiver = null
            lifecycle.onScreenOff()
            XposedHelpers.log("StepCounterController", "Screen receiver registration failed.")
        }

        val observer = ModuleHelper.PreferenceObserver {
            if (sContext != null && !MainModule.mPrefs.getBoolean("system_cc_show_stepcount")) {
                destroy()
            }
        }
        disableObserver = observer
        ModuleHelper.observePreferenceChange("system_cc_show_stepcount", StepCounterController, observer)
    }

    @JvmStatic
    fun destroy() {
        stopTimeTick()

        screenReceiver?.let {
            ModuleHelper.unregisterModuleReceiver("StepCounterController.screenReceiver")
            screenReceiver = null
        }

        queryHandler?.removeCallbacksAndMessages(null)
        queryHandler = null

        uiHandler?.removeCallbacksAndMessages(null)
        uiHandler = null

        queryThread?.quitSafely()
        queryThread = null

        sContext = null
        synchronized(viewLock) { stepViews.clear() }
        stepsWithGoal = null
        lifecycle.reset()

        disableObserver?.let {
            ModuleHelper.removePreferenceObserver("system_cc_show_stepcount", StepCounterController)
            disableObserver = null
        }
    }

    @JvmStatic
    fun removeStepViewByTag(tag: String?) {
        synchronized(viewLock) {
            val iterator = stepViews.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (tag == entry.tag || entry.ref.get() == null) {
                    iterator.remove()
                }
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

        synchronized(viewLock) {
            cleanExpiredViewsLocked()
            if (stepViews.any { it.ref.get() === sv }) return
            stepViews.add(StepViewRef(WeakReference(sv), sv.tag as? String))
        }

        if (hasLiveViews()) {
            lifecycle.setHasViews(true)
            if (lifecycle.screenOn) {
                registerTimeTickAndSchedule()
            }
        }
    }

    private fun hasLiveViews(): Boolean = synchronized(viewLock) {
        cleanExpiredViewsLocked()
        stepViews.isNotEmpty()
    }

    private fun cleanExpiredViewsLocked() {
        val iterator = stepViews.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().ref.get() == null) {
                iterator.remove()
            }
        }
    }

    private fun liveViews(): List<TextView> = synchronized(viewLock) {
        val iterator = stepViews.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().ref.get() == null) {
                iterator.remove()
            }
        }
        val result = ArrayList<TextView>(stepViews.size)
        for (entry in stepViews) {
            entry.ref.get()?.let { result.add(it) }
        }
        result
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
            val registered = ModuleHelper.registerModuleReceiver(
                ctx,
                "StepCounterController.timeTick",
                timeTickReceiver!!,
                IntentFilter(Intent.ACTION_TIME_TICK),
                Context.RECEIVER_NOT_EXPORTED
            )
            if (!registered) {
                timeTickReceiver = null
                lifecycle.unregisterTimeTick()
                XposedHelpers.log("StepCounterController", "TIME_TICK receiver registration failed.")
                return
            }
        }

        if (lifecycle.registerTimeTick()) {
            scheduleUpdate()
        }
    }

    private fun stopTimeTick() {
        val old = pendingQueryRunnable.getAndSet(null)
        old?.let { queryHandler?.removeCallbacks(it) }
        lifecycle.unregisterTimeTick()
        timeTickReceiver?.let {
            ModuleHelper.unregisterModuleReceiver("StepCounterController.timeTick")
            timeTickReceiver = null
        }
    }

    private fun clearPendingQuery(runnable: Runnable) {
        pendingQueryRunnable.compareAndSet(runnable, null)
    }

    private fun scheduleUpdate() {
        if (!lifecycle.screenOn || sContext == null) return
        if (!hasLiveViews()) {
            lifecycle.setHasViews(false)
            stopTimeTick()
            return
        }
        val capturedContext = sContext!!
        val ticket = lifecycle.tryStartQuery() ?: return

        val newRunnable = QueryRunnable(ticket, capturedContext)
        val old = pendingQueryRunnable.getAndSet(newRunnable)
        old?.let { queryHandler?.removeCallbacks(it) }
        val posted = try {
            queryHandler?.post(newRunnable) ?: false
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
            false
        }
        if (!posted) {
            pendingQueryRunnable.compareAndSet(newRunnable, null)
            lifecycle.finishQuery(ticket)
        }
    }

    private fun runQuery(thisRunnable: QueryRunnable) {
        val ticket = thisRunnable.ticket
        if (!lifecycle.isCurrent(ticket) || !lifecycle.hasViews) {
            lifecycle.finishQuery(ticket)
            return
        }

        val newText: String? = try {
            querySteps(thisRunnable.context)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            XposedHelpers.log(t)
            null
        } finally {
            lifecycle.finishQuery(ticket)
            clearPendingQuery(thisRunnable)
        }

        if (newText != null && lifecycle.isCurrent(ticket) && lifecycle.hasViews && lifecycle.screenOn) {
            val posted = uiHandler?.post {
                ModuleHelper.guarded("StepCounterController.updateViews") {
                    updateViews(ticket, newText)
                }
            } ?: false
            if (!posted) {
                lifecycle.consumeResult(ticket)
            }
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

    private fun updateViews(ticket: QueryTicket, newText: String) {
        if (!lifecycle.screenOn) {
            lifecycle.consumeResult(ticket)
            return
        }

        val views = liveViews()
        if (views.isEmpty()) {
            lifecycle.setHasViews(false)
            stopTimeTick()
            lifecycle.consumeResult(ticket)
            return
        }

        if (newText == stepsWithGoal) {
            lifecycle.consumeResult(ticket)
            return
        }
        stepsWithGoal = newText

        for (view in views) {
            view.text = newText
        }
        lifecycle.consumeResult(ticket)
    }
}
