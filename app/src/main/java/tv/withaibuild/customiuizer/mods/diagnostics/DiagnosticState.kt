package tv.withaibuild.customiuizer.mods.diagnostics

enum class DiagnosticState {
    REQUESTED, COMPATIBLE, INSTALLED, DEGRADED, FAILED
}

data class DiagnosticSnapshot(
    val state: DiagnosticState,
    val count: Long,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val message: String?
)
