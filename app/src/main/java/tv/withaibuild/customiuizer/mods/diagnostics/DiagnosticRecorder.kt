package tv.withaibuild.customiuizer.mods.diagnostics

import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.util.concurrent.ConcurrentHashMap

object DiagnosticRecorder {
    private val entries = ConcurrentHashMap<String, DiagnosticSnapshot>()
    private val logThrottler = ConcurrentHashMap<String, Long>()
    private const val THROTTLE_MS = 60_000L

    @JvmField
    var logger: ((String) -> Unit)? = null

    @JvmStatic
    @Synchronized
    fun record(
        id: String,
        state: DiagnosticState,
        throwable: Throwable? = null,
        message: String? = null
    ) {
        val now = System.currentTimeMillis()
        val prev = entries[id]
        val snapshot = DiagnosticSnapshot(
            state = state,
            count = (prev?.count ?: 0L) + 1,
            firstSeenMs = prev?.firstSeenMs ?: now,
            lastSeenMs = now,
            message = message
        )
        entries[id] = snapshot

        val shouldLog = when (state) {
            DiagnosticState.FAILED, DiagnosticState.DEGRADED -> shouldLogThrottled(id, now)
            else -> prev?.state != state
        }

        if (shouldLog) {
            val logLine = buildString {
                append("Diagnostic [").append(id).append("] ").append(state.name)
                if (!message.isNullOrEmpty()) append(" - ").append(message)
                if (throwable != null) append('\n').append(throwable.stackTraceToString())
            }
            val log = logger
            if (log != null) log(logLine) else XposedHelpers.log(logLine)
        }
    }

    private fun shouldLogThrottled(id: String, now: Long): Boolean {
        val prev = logThrottler[id]
        return if (prev == null) {
            logThrottler.putIfAbsent(id, now) == null
        } else if (now - prev >= THROTTLE_MS) {
            logThrottler.replace(id, prev, now)
        } else {
            false
        }
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
    }
}
