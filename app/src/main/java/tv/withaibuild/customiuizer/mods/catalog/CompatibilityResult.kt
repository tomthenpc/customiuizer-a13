package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.HookInstallResult

/**
 * Outcome of a feature compatibility probe.
 *
 * This is a side-effect-free, read-only value. The caller (typically
 * [FeatureInstallRegistry]) is responsible for turning it into a
 * [tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder] snapshot.
 *
 * @param compatibility State used for install routing.
 * @param reasonCode Stable reason produced by the resolver.
 * @param detail Human-readable detail for diagnostics.
 * @param hookResult Full resolver result, including the selected variant and
 *                   target records; passed to the installer and
 *                   [tv.withaibuild.customiuizer.mods.utils.HookInstaller.withSession].
 */
data class CompatibilityResult(
    val compatibility: CompatibilityState,
    val reasonCode: ReasonCode,
    val detail: String?,
    val hookResult: HookInstallResult
)
