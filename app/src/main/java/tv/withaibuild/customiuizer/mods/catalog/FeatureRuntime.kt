package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.compat.RomEnvironment
import tv.withaibuild.customiuizer.mods.compat.RomEnvironmentDetector
import tv.withaibuild.customiuizer.mods.compat.RomEnvironmentDiagnostics
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
 * The [HookTargetResolver] and [RomEnvironment] are created lazily: if no catalog
 * feature is requested in a process, the resolver, its reflection cache and the
 * ROM environment object are never allocated, reducing disabled-feature overhead
 * in scope processes.
 */
class FeatureRuntime(
    val processName: String,
    val lpparam: Any,
    val classLoader: ClassLoader,
    val prefs: PrefMap<String, Any>
) {
    private val environmentLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val environment = RomEnvironmentDetector.detect()
        RomEnvironmentDiagnostics.recordSafely(environment)
        environment
    }
    internal val environment: RomEnvironment by environmentLazy

    private val resolverLazy = lazy(LazyThreadSafetyMode.NONE) { resolverForTest ?: HookTargetResolver(classLoader) }
    val resolver: HookTargetResolver by resolverLazy

    /** Test-only override to avoid lazy reflection. Never used in production. */
    internal var resolverForTest: HookTargetResolver? = null

    internal fun isEnvironmentInitialized(): Boolean = environmentLazy.isInitialized()
    internal fun isResolverInitialized(): Boolean = resolverLazy.isInitialized() || resolverForTest != null

    companion object {
        /** Test-only creation counter. Never used by production logic. */
        @JvmField
        var testCreationCount: Int = 0

        /** Test-only record of processes that created a runtime. */
        val testCreatedProcessNames = mutableListOf<String>()

        @JvmStatic
        fun resetForTest() {
            testCreationCount = 0
            testCreatedProcessNames.clear()
        }
    }

    init {
        testCreationCount++
        testCreatedProcessNames.add(processName)
    }
}
