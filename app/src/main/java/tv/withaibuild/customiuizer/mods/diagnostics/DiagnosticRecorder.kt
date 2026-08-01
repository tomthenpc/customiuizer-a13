package tv.withaibuild.customiuizer.mods.diagnostics

import android.os.SystemClock
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

/**
 * Privacy-safe, throttled, multi-dimensional diagnostic recorder.
 *
 * - Uses a monotonic clock ([SystemClock.elapsedRealtime]) by default, overridable
 *   for tests via [clock].
 * - Splits state into three dimensions: [EnabledState], [CompatibilityState] and
 *   [InstallOutcome]. The snapshot never lets an install result overwrite the
 *   compatibility conclusion.
 * - Logs a stable feature id, [DiagnosticState] and [ReasonCode]. Dynamic detail
 *   (class names, etc.) is attached to [DiagnosticSnapshot.detail] but never used
 *   as the throttling key.
 * - FAILED logs immediately on first occurrence or when severity escalates from
 *   a lower state; it is not blocked by an earlier DEGRADED throttle.
 * - REQUESTED / COMPATIBLE / INSTALLED are logged only on state transition.
 * - Retained snapshots and throttle state are bounded so long-lived processes do
 *   not accumulate unbounded diagnostic maps.
 */
object DiagnosticRecorder {

    private const val MAX_SNAPSHOTS = 32
    private const val MAX_DETAIL_LENGTH = 512
    private const val THROTTLE_MS = 60_000L

    private val snapshots =
        LinkedHashMap<String, DiagnosticSnapshot>(MAX_SNAPSHOTS, 0.75f, true)

    private val logThrottler =
        HashMap<String, Long>(MAX_SNAPSHOTS)

    /** Injected monotonic clock. Defaults to [SystemClock.elapsedRealtime]. */
    internal var clock: () -> Long = { SystemClock.elapsedRealtime() }

    /** Injected logger. Defaults to [XposedHelpers.log] when null. */
    internal var logger: ((String) -> Unit)? = null

    @JvmStatic
    @Synchronized
    fun record(
        id: String,
        enabled: EnabledState? = null,
        compatibility: CompatibilityState? = null,
        installation: InstallOutcome? = null,
        reasonCode: ReasonCode,
        detail: String? = null,
        throwable: Throwable? = null,
        installSummary: InstallSummary? = null
    ) {
        val now = clock()
        val prev = snapshots[id]

        val safeDetail = detail?.take(MAX_DETAIL_LENGTH)

        val snapshot = DiagnosticSnapshot(
            enabled = enabled ?: prev?.enabled,
            compatibility = compatibility ?: prev?.compatibility,
            installation = installation ?: prev?.installation,
            reasonCode = reasonCode,
            detail = safeDetail,
            count = (prev?.count ?: 0L) + 1,
            firstSeenMs = prev?.firstSeenMs ?: now,
            lastSeenMs = now,
            installSummary = installSummary ?: prev?.installSummary
        )
        snapshots[id] = snapshot
        trimToLimit()

        val newState = overallState(snapshot)
        val prevState = prev?.let { overallState(it) }

        val shouldLog = when {
            newState == DiagnosticState.FAILED ->
                prevState != DiagnosticState.FAILED || throttleExpired(id, now)
            newState == DiagnosticState.DEGRADED ->
                prevState != DiagnosticState.DEGRADED || throttleExpired(id, now)
            else ->
                prevState != newState
        }

        if (shouldLog) {
            val logLine = buildString {
                append("Diagnostic[").append(id).append("] ")
                append(newState.name)
                if (snapshot.enabled != null) append(" enabled=").append(snapshot.enabled.name)
                if (snapshot.compatibility != null) append(" compat=").append(snapshot.compatibility.name)
                if (snapshot.installation != null) append(" install=").append(snapshot.installation.name)
                val summary = snapshot.installSummary
                if (summary != null) {
                    append(" required=").append(summary.requiredInstalled).append("/").append(summary.requiredTotal)
                    append(" optional=").append(summary.optionalInstalled).append("/").append(summary.optionalTotal)
                    if (summary.fallbackUsed) append(" fallback=true")
                }
                append(" reason=").append(reasonCode.name)
                if (!safeDetail.isNullOrEmpty()) append(" detail=").append(safeDetail)
                if (throwable != null) append(" | ").append(throwableSummary(throwable))
            }
            logThrottler[id] = now
            val log = logger
            if (log != null) {
                try {
                    log(logLine)
                } catch (oom: OutOfMemoryError) {
                    throw oom
                } catch (t: Throwable) {
                    try {
                        XposedHelpers.log("Diagnostic logger failed: ${t.message}")
                    } catch (oom: OutOfMemoryError) {
                        throw oom
                    } catch (_: Throwable) {
                        // fallback logger must not block the installation path
                    }
                }
            } else {
                try {
                    XposedHelpers.log(logLine)
                } catch (oom: OutOfMemoryError) {
                    throw oom
                } catch (_: Throwable) {
                    // logger failure must not block the installation path
                }
            }
        }
    }

    private fun throttleExpired(id: String, now: Long): Boolean {
        val last = logThrottler[id] ?: return true
        return now - last >= THROTTLE_MS
    }

    /** Evict the oldest entries until we are within the hard size bound. */
    @Synchronized
    private fun trimToLimit() {
        while (snapshots.size > MAX_SNAPSHOTS) {
            val oldestId = snapshots.entries.iterator().next().key
            snapshots.remove(oldestId)
            logThrottler.remove(oldestId)
        }
    }

    /** Computes the single overall severity state from the three dimensions. */
    private fun overallState(snapshot: DiagnosticSnapshot): DiagnosticState {
        val install = snapshot.installation
        if (install == InstallOutcome.FAILED) return DiagnosticState.FAILED
        if (install == InstallOutcome.INSTALLED) return DiagnosticState.INSTALLED
        if (install == InstallOutcome.DISPATCHED) return DiagnosticState.DISPATCHED
        if (install == InstallOutcome.DEGRADED) return DiagnosticState.DEGRADED

        val compat = snapshot.compatibility
        if (compat == CompatibilityState.INCOMPATIBLE) return DiagnosticState.INCOMPATIBLE
        if (compat == CompatibilityState.DEGRADED) return DiagnosticState.DEGRADED
        if (compat == CompatibilityState.COMPATIBLE) return DiagnosticState.COMPATIBLE

        val enabled = snapshot.enabled
        if (enabled == EnabledState.REQUESTED) return DiagnosticState.REQUESTED
        if (enabled == EnabledState.DISABLED) return DiagnosticState.DISABLED

        return DiagnosticState.REQUESTED
    }

    /** Returns a desensitized throwable summary: class name only, no stack trace. */
    private fun throwableSummary(throwable: Throwable): String {
        val name = throwable.javaClass.name
        val message = throwable.message
        return if (message.isNullOrEmpty()) name else "$name: ${message.take(80)}"
    }

    @JvmStatic
    @Synchronized
    fun summarize(): Map<String, DiagnosticSnapshot> = snapshots.toMap()

    @JvmStatic
    @Synchronized
    fun reset() {
        snapshots.clear()
        logThrottler.clear()
        logger = null
        clock = { SystemClock.elapsedRealtime() }
    }
}
