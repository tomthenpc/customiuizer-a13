package tv.withaibuild.customiuizer.mods.diagnostics

import android.os.SystemClock
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.util.concurrent.ConcurrentHashMap

/**
 * Privacy-safe, throttled diagnostic recorder.
 *
 * - Uses a monotonic clock for throttling ([SystemClock.elapsedRealtime] by
 *   default), overridable in tests via [clock].
 * - Logs the stable triple (id, state, reason) rather than full throwables.
 * - FAILED logs immediately on the first occurrence or when severity escalates
 *   from a lower state; it is not blocked by an earlier DEGRADED throttle.
 * - REQUESTED / COMPATIBLE / INSTALLED are logged only on state transition.
 * - [logger] and [clock] are `internal` so tests in the same module can inject
 *   them while production code cannot change them from the outside.
 */
object DiagnosticRecorder {
    private val entries = ConcurrentHashMap<String, DiagnosticSnapshot>()
    private val logThrottler = ConcurrentHashMap<String, Long>()
    private const val THROTTLE_MS = 60_000L

    /** Injected monotonic clock. Defaults to [SystemClock.elapsedRealtime]. */
    internal var clock: () -> Long = { SystemClock.elapsedRealtime() }

    /** Injected logger. Defaults to [XposedHelpers.log] when null. */
    internal var logger: ((String) -> Unit)? = null

    @JvmStatic
    @Synchronized
    fun record(
        id: String,
        state: DiagnosticState,
        throwable: Throwable? = null,
        reason: String? = null
    ) {
        val now = clock()
        val prev = entries[id]
        val snapshot = DiagnosticSnapshot(
            state = state,
            count = (prev?.count ?: 0L) + 1,
            firstSeenMs = prev?.firstSeenMs ?: now,
            lastSeenMs = now,
            reason = reason
        )
        entries[id] = snapshot

        val shouldLog = when (state) {
            DiagnosticState.FAILED -> shouldLogFailed(id, now, prev)
            DiagnosticState.DEGRADED -> shouldLogDegraded(id, now, prev)
            else -> prev?.state != state
        }

        if (shouldLog) {
            val logLine = buildLogLine(id, state, reason, throwable)
            val log = logger
            if (log != null) {
                log(logLine)
            } else {
                XposedHelpers.log(logLine)
            }
        }
    }

    private fun shouldLogFailed(id: String, now: Long, prev: DiagnosticSnapshot?): Boolean {
        if (prev == null || prev.state != DiagnosticState.FAILED) {
            // First FAILED or escalation from a lower severity: log immediately and
            // reset the throttle for this id.
            logThrottler[id] = now
            return true
        }
        return maybeThrottle(id, now)
    }

    private fun shouldLogDegraded(id: String, now: Long, prev: DiagnosticSnapshot?): Boolean {
        if (prev == null || prev.state != DiagnosticState.DEGRADED) {
            logThrottler[id] = now
            return true
        }
        return maybeThrottle(id, now)
    }

    private fun maybeThrottle(id: String, now: Long): Boolean {
        val prevLog = logThrottler[id]
        return if (prevLog == null) {
            logThrottler[id] = now
            true
        } else if (now - prevLog >= THROTTLE_MS) {
            logThrottler[id] = now
            true
        } else {
            false
        }
    }

    private fun buildLogLine(
        id: String,
        state: DiagnosticState,
        reason: String?,
        throwable: Throwable?
    ): String {
        return buildString {
            append("Diagnostic[").append(id).append("] ").append(state.name)
            if (!reason.isNullOrEmpty()) {
                append(": ").append(reason)
            }
            if (throwable != null) {
                append(" | ").append(throwableSummary(throwable))
            }
        }
    }

    /** Returns a desensitized throwable summary: class name only, no stack trace. */
    private fun throwableSummary(throwable: Throwable): String {
        val name = throwable.javaClass.name
        val message = throwable.message
        return if (message.isNullOrEmpty()) name else "$name: ${message.take(80)}"
    }

    @JvmStatic
    @Synchronized
    fun summarize(): Map<String, DiagnosticSnapshot> = entries.toMap()

    @JvmStatic
    @Synchronized
    fun reset() {
        entries.clear()
        logThrottler.clear()
        logger = null
        clock = { SystemClock.elapsedRealtime() }
    }
}
