package tv.withaibuild.customiuizer.mods.catalog

/**
 * Result of a one-time compatibility probe for a feature.
 *
 * [COMPATIBLE]   - all required targets are present; normal install.
 * [DEGRADED]     - some targets are missing but a degraded install is still
 *                  possible (caller decides whether to install).
 * [INCOMPATIBLE] - a required target is missing; the feature cannot run.
 */
enum class CompatibilityState {
    COMPATIBLE,
    DEGRADED,
    INCOMPATIBLE
}
