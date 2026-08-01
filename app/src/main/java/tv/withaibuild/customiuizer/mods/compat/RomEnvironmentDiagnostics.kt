package tv.withaibuild.customiuizer.mods.compat

import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

/**
 * Records the per-process ROM environment exactly once.
 *
 * This is intentionally separate from [RomEnvironmentDetector] so that classification can be
 * tested without side effects on the global diagnostic recorder.
 */
internal object RomEnvironmentDiagnostics {

    fun record(environment: RomEnvironment) {
        val compatibility = when (environment.profile) {
            RomProfile.MIUI14_A13, RomProfile.HYPEROS1_A13 -> CompatibilityState.COMPATIBLE
            RomProfile.UNKNOWN_A13 -> CompatibilityState.DEGRADED
            RomProfile.UNSUPPORTED_ANDROID -> CompatibilityState.INCOMPATIBLE
        }

        val reason = when (environment.profile) {
            RomProfile.MIUI14_A13, RomProfile.HYPEROS1_A13 -> ReasonCode.ROM_PROFILE_DETECTED
            RomProfile.UNKNOWN_A13 -> ReasonCode.ROM_PROFILE_UNKNOWN
            RomProfile.UNSUPPORTED_ANDROID -> ReasonCode.ANDROID_VERSION_UNSUPPORTED
        }

        DiagnosticRecorder.record(
            id = "rom.environment",
            compatibility = compatibility,
            reasonCode = reason,
            detail = buildString {
                append("profile=").append(environment.profile.name)
                append("; sdk=").append(environment.sdkInt)
                append("; evidence=[")
                append(formatEvidence(environment))
                append(']')
            }
        )
    }

    private fun formatEvidence(environment: RomEnvironment): String {
        val flags = environment.evidenceFlags
        val parts = ArrayList<String>(7)
        if (flags and RomEnvironmentDetector.EVIDENCE_DISPLAY != 0) {
            parts.add("display=${environment.buildDisplay}")
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_BUILD_INCREMENTAL != 0) {
            parts.add("buildIncremental=${environment.buildIncremental}")
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_RO_INCREMENTAL != 0) {
            parts.add("roIncremental=${environment.roIncremental}")
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_MIUI != 0) {
            parts.add("miui=${environment.miuiVersionName}")
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_MIUI_CODE != 0) {
            parts.add("miuiCode=${environment.miuiVersionCode}")
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_HYPEROS != 0) {
            parts.add("hyperos=${environment.hyperOsVersionName}")
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_HYPEROS_CODE != 0) {
            parts.add("hyperosCode=${environment.hyperOsVersionCode}")
        }
        return parts.joinToString(", ")
    }
}
