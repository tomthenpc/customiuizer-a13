package tv.withaibuild.customiuizer.mods.catalog
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
import tv.withaibuild.customiuizer.utils.PrefMap
/**
 * Runtime feature dispatcher.
 *
 * [FeatureDispatcher] is the hot-path entry point for installing typed catalog
 * features. It holds the smallest possible public surface:
 *
 * - [createRuntime] builds the per-process runtime context.
 * - [install] installs a typed [FeatureId] via the production registry.
 * - [installById] parses a string id and forwards to [install].
 *
 * All catalog spec lookup is delegated to [FeatureCatalog.specByCanonicalId],
 * and all install routing is delegated to [FeatureInstallRegistry].
 */
object FeatureDispatcher {
    init {
        FeatureInstallRegistry.registerAll(FeatureCatalog.specs())
    }
    @JvmStatic
    fun createRuntime(
        processName: String,
        lpparam: Any,
        classLoader: ClassLoader,
        prefs: PrefMap<String, Any>
    ): FeatureRuntime = FeatureRuntime(processName, lpparam, classLoader, prefs)
    @JvmStatic
    fun installById(featureId: String, runtime: FeatureRuntime): Boolean = try {
        val feature = FeatureId.fromString(featureId)
        if (feature == null) {
            DiagnosticRecorder.record(
                id = DiagnosticIds.UNKNOWN_FEATURE_ID,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = featureId
            )
            false
        } else {
            install(feature, runtime)
        }
    } catch (fatal: OutOfMemoryError) {
        throw fatal
    } catch (fatal: VirtualMachineError) {
        throw fatal
    } catch (fatal: ThreadDeath) {
        throw fatal
    }
    @JvmStatic
    fun install(feature: FeatureId, runtime: FeatureRuntime): Boolean {
        val spec = FeatureCatalog.specByCanonicalId(feature.canonicalId)
        if (spec == null) {
            DiagnosticRecorder.record(
                id = DiagnosticIds.UNKNOWN_FEATURE_ID,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = feature.canonicalId
            )
            return false
        }
        val scope = spec.processScope
        val phase = spec.installPhase
        if (scope == null || phase == null) {
            DiagnosticRecorder.record(
                id = spec.diagnosticId,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = "missing processScope or installPhase for ${feature.canonicalId}"
            )
            return false
        }
        return try {
            FeatureInstallRegistry.installById(
                feature.canonicalId,
                scope,
                phase,
                runtime
            ).isActive
        } catch (fatal: OutOfMemoryError) {
            throw fatal
        } catch (fatal: VirtualMachineError) {
            throw fatal
        } catch (fatal: ThreadDeath) {
            throw fatal
        }
    }
}
