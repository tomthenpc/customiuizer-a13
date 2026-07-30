package tv.withaibuild.customiuizer.mods.diagnostics

enum class DiagnosticState {
    REQUESTED, COMPATIBLE, INSTALLED, DEGRADED, FAILED
}

/**
 * Immutable diagnostic snapshot for a single feature / component.
 *
 * The actual [Throwable] is **not** stored here; only a stable, desensitized
 * reason string and the cumulative count are kept.
 */
data class DiagnosticSnapshot(
    val state: DiagnosticState,
    val count: Long,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val reason: String?
)
