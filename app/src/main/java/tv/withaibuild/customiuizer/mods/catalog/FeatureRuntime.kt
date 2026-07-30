package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.utils.HookTargetResolver
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Runtime context passed to a [FeatureSpec] condition, compatibility probe and
 * installer.
 *
 * It groups the process name, libxposed parameter, ClassLoader, preference map
 * and a per-runtime [HookTargetResolver] so each feature performs exactly one
 * round of target resolution per install attempt.
 */
class FeatureRuntime(
    val processName: String,
    val lpparam: Any,
    val classLoader: ClassLoader,
    val resolver: HookTargetResolver,
    val prefs: PrefMap<String, Any?>
)
