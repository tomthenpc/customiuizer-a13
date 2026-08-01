package tv.withaibuild.customiuizer.mods.diagnostics

import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState

enum class EnabledState { DISABLED, REQUESTED }

enum class InstallOutcome {
    INSTALLED,
    DISPATCHED,
    DEGRADED,
    FAILED
}

/**
 * Stable, machine-readable reason code used in logs and snapshots.
 *
 * Dynamic exception text or class names are kept in [DiagnosticSnapshot.detail];
 * the reason code itself is an enum constant and is safe to aggregate.
 */
enum class ReasonCode {
    PREFERENCE_DISABLED,
    REQUESTED,
    PRIMARY_TARGET_FOUND,
    FALLBACK_TARGET_FOUND,
    TARGET_NOT_FOUND,
    INSTALLER_DISPATCHED,
    INSTALLER_SUCCEEDED,
    INSTALLER_FAILED,
    ROM_PROFILE_DETECTED,
    ROM_PROFILE_UNKNOWN,
    ROM_EVIDENCE_CONFLICT,
    HYPEROS_FALLBACK_FOUND,
    HYPEROS_TARGET_NOT_FOUND,
    UNKNOWN
}

/**
 * Combined diagnostic state used for severity ordering and throttling.
 *
 * The numeric order (ordinal) reflects increasing severity. It is derived from
 * the per-dimension fields in [DiagnosticSnapshot] and is not persisted itself.
 */
enum class DiagnosticState {
    DISABLED,
    REQUESTED,
    COMPATIBLE,
    DEGRADED,
    INCOMPATIBLE,
    DISPATCHED,
    INSTALLED,
    FAILED
}

/**
 * Multi-dimensional diagnostic snapshot.
 *
 * The three dimensions (enabled, compatibility, installation) are stored
 * independently so a final installation state never overwrites the
 * compatibility conclusion.
 */
data class DiagnosticSnapshot(
    val enabled: EnabledState? = null,
    val compatibility: CompatibilityState? = null,
    val installation: InstallOutcome? = null,
    val reasonCode: ReasonCode,
    val detail: String? = null,
    val count: Long,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val installSummary: InstallSummary? = null
)
