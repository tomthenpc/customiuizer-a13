package tv.withaibuild.customiuizer.mods.compat

import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

/**
 * Records the per-process ROM environment exactly once.
 *
 * This is intentionally separate from [RomEnvironmentDetector] so that classification can be
 * tested without side effects on the global diagnostic recorder. A failure here is non-fatal
 * and must not block target resolution or Hook installation.
 */
internal object RomEnvironmentDiagnostics {

    fun recordSafely(environment: RomEnvironment) {
        try {
            record(environment)
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (_: Throwable) {
            // Diagnostic recording is best-effort metadata. A recording failure must never
            // prevent the feature installer from continuing.
        }
    }

    internal fun record(environment: RomEnvironment) {
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

        val detail = buildString {
            append("profile=").append(environment.profile.name)
            append("; sdk=").append(environment.sdkInt)
            append("; evidence=[")
            appendEvidence(this, environment)
            append(']')
        }

        DiagnosticRecorder.record(
            id = "rom.environment",
            compatibility = compatibility,
            reasonCode = reason,
            detail = detail
        )
    }

    private fun appendEvidence(builder: StringBuilder, environment: RomEnvironment) {
        val flags = environment.evidenceFlags
        var first = true

        fun addPart(key: String, value: String?) {
            if (value.isNullOrEmpty()) return
            if (!first) builder.append(", ")
            first = false
            builder.append(key).append('=').append(value)
        }

        if (flags and RomEnvironmentDetector.EVIDENCE_DISPLAY != 0) {
            addPart("display", environment.buildDisplay)
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_BUILD_INCREMENTAL != 0) {
            addPart("buildIncremental", environment.buildIncremental)
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_RO_INCREMENTAL != 0) {
            addPart("roIncremental", environment.roIncremental)
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_MIUI != 0) {
            addPart("miui", environment.miuiVersionName)
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_MIUI_CODE != 0) {
            addPart("miuiCode", environment.miuiVersionCode)
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_HYPEROS != 0) {
            addPart("hyperos", environment.hyperOsVersionName)
        }
        if (flags and RomEnvironmentDetector.EVIDENCE_HYPEROS_CODE != 0) {
            addPart("hyperosCode", environment.hyperOsVersionCode)
        }
    }
}
