package tv.withaibuild.customiuizer.mods.catalog

/**
 * How a [FeatureSpec] determines runtime compatibility.
 *
 * This must be declared explicitly for any feature that enters the
 * [FeatureInstallRegistry] production path.
 */
enum class CompatibilityPolicy {
    /**
     * Compatibility is decided by evaluating [FeatureSpec.contract] with the
     * process ClassLoader. A missing contract means incompatible.
     */
    CONTRACT_REQUIRED,

    /**
     * Compatibility is supplied by a custom [FeatureSpec.compatibilityCheck]
     * lambda. This is intended for features that need explicit fallback logic
     * or cross-process probes not captured by a single contract.
     */
    CUSTOM,

    /**
     * The feature is trusted to install on the declared process/phase without
     * a contract. This must only be used for legacy features while they are
     * being converted to contract-based probing.
     */
    LEGACY_TRUSTED
}
