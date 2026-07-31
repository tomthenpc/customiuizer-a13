package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.utils.HookTargetResolver
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Runtime context passed to a [FeatureSpec] condition, compatibility probe and
 * installer.
 *
 * A single [FeatureRuntime] is created per host process and reused for every
 * [FeatureDispatcher.installById] call so the [HookTargetResolver] cache is shared
 * across features in the same process.
 *
 * The [HookTargetResolver] is created lazily: if no catalog feature is requested
 * in a process, the resolver (and its reflection cache) is never allocated,
 * reducing disabled-feature overhead in scope processes.
 */
class FeatureRuntime(
    val processName: String,
    val lpparam: Any,
    val classLoader: ClassLoader,
    val prefs: PrefMap<String, Any?>
) {
    private val resolverLazy = lazy(LazyThreadSafetyMode.NONE) { HookTargetResolver(classLoader) }
    val resolver: HookTargetResolver by resolverLazy

    internal fun isResolverInitialized(): Boolean = resolverLazy.isInitialized()
}
