package tv.withaibuild.customiuizer.mods.diagnostics

/**
 * A stable, privacy-safe summary of one feature install attempt.
 *
 * Does not contain class names, method names or throwables; only counts and
 * outcome flags. It is attached to [DiagnosticSnapshot] for downstream audit.
 */
data class InstallSummary(
    val requiredInstalled: Int,
    val requiredTotal: Int,
    val optionalInstalled: Int,
    val optionalTotal: Int,
    val fallbackUsed: Boolean,
    val installation: InstallOutcome,
    val reasonCode: ReasonCode
)
