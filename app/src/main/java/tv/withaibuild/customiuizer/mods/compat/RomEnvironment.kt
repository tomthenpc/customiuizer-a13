package tv.withaibuild.customiuizer.mods.compat

import android.os.Build
import java.util.Locale

/**
 * Minimal abstraction for reading Android system properties.
 *
 * The production implementation caches `android.os.SystemProperties.get(String)` once and
 * rethrows any `OutOfMemoryError` that reaches it. Fakes can be supplied for unit tests.
 */
internal fun interface SystemPropertyReader {
    fun get(key: String): String?
}

/**
 * Production reader backed by `android.os.SystemProperties`.
 *
 * The `get` Method is resolved once per class loader. Ordinary reflection failures are
 * treated as `null` so that `RomEnvironmentDetector` can classify the device safely.
 * `OutOfMemoryError` is rethrown immediately, even when wrapped by
 * `InvocationTargetException` or `ExceptionInInitializerError`.
 */
private fun unwrapFatal(t: Throwable): Throwable = when (t) {
    is java.lang.reflect.InvocationTargetException -> t.targetException ?: t
    is ExceptionInInitializerError -> t.exception ?: t
    else -> t
}

internal object AndroidSystemPropertyReader : SystemPropertyReader {

    private const val SYSTEM_PROPERTIES_CLASS = "android.os.SystemProperties"
    private const val GET_METHOD = "get"

    private val getMethod by lazy {
        try {
            Class.forName(SYSTEM_PROPERTIES_CLASS).getDeclaredMethod(GET_METHOD, String::class.java)
        } catch (t: Throwable) {
            val fatal = unwrapFatal(t)
            if (fatal is OutOfMemoryError) throw fatal
            null
        }
    }

    override fun get(key: String): String? {
        val method = getMethod ?: return null
        return try {
            method.invoke(null, key) as? String
        } catch (t: Throwable) {
            val fatal = unwrapFatal(t)
            if (fatal is OutOfMemoryError) throw fatal
            null
        }
    }

}

/**
 * ROM profile for the current host process.
 *
 * Detection is performed once per process and is stored in [FeatureRuntime] lazily.
 * The profile is advisory only: feature install decisions remain the responsibility
 * of the concrete [HookTargetContract]/[HookTargetResolver] machinery.
 */
internal enum class RomProfile {
    MIUI14_A13,
    HYPEROS1_A13,
    UNKNOWN_A13,
    UNSUPPORTED_ANDROID
}

/**
 * @param evidenceFlags bit mask of which evidence fields are non-empty (see [RomEnvironmentDetector]).
 */
internal data class RomEnvironment(
    val sdkInt: Int,
    val profile: RomProfile,
    val buildDisplay: String,
    val buildIncremental: String,
    val miuiVersionName: String?,
    val miuiVersionCode: String?,
    val hyperOsVersionName: String?,
    val hyperOsVersionCode: String?,
    val roIncremental: String?,
    val evidenceFlags: Int
)

/**
 * Pure classifier. It reads a fixed set of properties, validates version strings and
 * returns a [RomEnvironment]. It does not record diagnostics or mutate any global state.
 */
internal object RomEnvironmentDetector {

    internal const val EVIDENCE_DISPLAY = 1
    internal const val EVIDENCE_BUILD_INCREMENTAL = 1 shl 1
    internal const val EVIDENCE_RO_INCREMENTAL = 1 shl 2
    internal const val EVIDENCE_MIUI = 1 shl 3
    internal const val EVIDENCE_MIUI_CODE = 1 shl 4
    internal const val EVIDENCE_HYPEROS = 1 shl 5
    internal const val EVIDENCE_HYPEROS_CODE = 1 shl 6

    private const val MIUI_VERSION_NAME = "ro.miui.ui.version.name"
    private const val MIUI_VERSION_CODE = "ro.miui.ui.version.code"
    private const val HYPER_OS_VERSION_NAME = "ro.mi.os.version.name"
    private const val HYPER_OS_VERSION_CODE = "ro.mi.os.version.code"
    private const val BUILD_VERSION_INCREMENTAL = "ro.build.version.incremental"
    private const val MAX_EVIDENCE_LENGTH = 128

    /** Production entry: reads [Build] and cached [android.os.SystemProperties] once. */
    fun detect(): RomEnvironment = detect(
        Build.VERSION.SDK_INT,
        Build.DISPLAY ?: "",
        Build.VERSION.INCREMENTAL ?: "",
        AndroidSystemPropertyReader
    )

    /** Testable entry: all inputs are explicit. */
    internal fun detect(
        sdkInt: Int,
        buildDisplay: String,
        buildIncremental: String,
        reader: SystemPropertyReader
    ): RomEnvironment {
        val miuiNameRaw = readProperty(reader, MIUI_VERSION_NAME)
        val miuiCode = readProperty(reader, MIUI_VERSION_CODE)
        val hyperNameRaw = readProperty(reader, HYPER_OS_VERSION_NAME)
        val hyperCode = readProperty(reader, HYPER_OS_VERSION_CODE)
        val roIncremental = readProperty(reader, BUILD_VERSION_INCREMENTAL)

        val miuiVersionName = miuiNameRaw?.takeIf { isValidMiuiVersion(it) }
        val hyperOsVersionName = hyperNameRaw?.takeIf { isValidHyperOsVersion(it) }

        val profile = when {
            sdkInt != 33 -> RomProfile.UNSUPPORTED_ANDROID
            hyperOsVersionName != null -> RomProfile.HYPEROS1_A13
            miuiVersionName != null -> RomProfile.MIUI14_A13
            else -> RomProfile.UNKNOWN_A13
        }

        val cleanBuildDisplay = sanitizeEvidence(buildDisplay)
        val cleanBuildIncremental = sanitizeEvidence(buildIncremental)
        val cleanMiuiVersionName = miuiVersionName?.let { sanitizeEvidence(it) }
        val cleanMiuiCode = miuiCode?.let { sanitizeEvidence(it) }
        val cleanHyperVersionName = hyperOsVersionName?.let { sanitizeEvidence(it) }
        val cleanHyperCode = hyperCode?.let { sanitizeEvidence(it) }
        val cleanRoIncremental = roIncremental?.let { sanitizeEvidence(it) }

        val evidenceFlags = computeEvidenceFlags(
            cleanBuildDisplay,
            cleanBuildIncremental,
            cleanMiuiVersionName,
            cleanMiuiCode,
            cleanHyperVersionName,
            cleanHyperCode,
            cleanRoIncremental
        )

        return RomEnvironment(
            sdkInt = sdkInt,
            profile = profile,
            buildDisplay = cleanBuildDisplay,
            buildIncremental = cleanBuildIncremental,
            miuiVersionName = cleanMiuiVersionName,
            miuiVersionCode = cleanMiuiCode,
            hyperOsVersionName = cleanHyperVersionName,
            hyperOsVersionCode = cleanHyperCode,
            roIncremental = cleanRoIncremental,
            evidenceFlags = evidenceFlags
        )
    }

    private fun readProperty(reader: SystemPropertyReader, key: String): String? =
        try {
            reader.get(key)
        } catch (t: Throwable) {
            val fatal = unwrapFatal(t)
            if (fatal is OutOfMemoryError) throw fatal
            null
        }

    /** HyperOS 1 candidate: exactly "OS1" or "OS1.x.y.z". */
    internal fun isValidHyperOsVersion(value: String): Boolean {
        val v = value.trim().uppercase(Locale.ROOT)
        return v == "OS1" || v.startsWith("OS1.")
    }

    /** MIUI 14 candidate: exactly "V14" or "V14.x.y.z". */
    internal fun isValidMiuiVersion(value: String): Boolean {
        val v = value.trim().uppercase(Locale.ROOT)
        return v == "V14" || v.startsWith("V14.")
    }

    internal fun sanitizeEvidence(value: String): String =
        value.trim().replace(Regex("[\r\n]"), " ").take(MAX_EVIDENCE_LENGTH)

    private fun computeEvidenceFlags(
        buildDisplay: String,
        buildIncremental: String,
        miuiVersionName: String?,
        miuiVersionCode: String?,
        hyperOsVersionName: String?,
        hyperOsVersionCode: String?,
        roIncremental: String?
    ): Int {
        var flags = 0
        if (buildDisplay.isNotEmpty()) flags = flags or EVIDENCE_DISPLAY
        if (buildIncremental.isNotEmpty()) flags = flags or EVIDENCE_BUILD_INCREMENTAL
        if (roIncremental?.isNotEmpty() == true) flags = flags or EVIDENCE_RO_INCREMENTAL
        if (miuiVersionName?.isNotEmpty() == true) flags = flags or EVIDENCE_MIUI
        if (miuiVersionCode?.isNotEmpty() == true) flags = flags or EVIDENCE_MIUI_CODE
        if (hyperOsVersionName?.isNotEmpty() == true) flags = flags or EVIDENCE_HYPEROS
        if (hyperOsVersionCode?.isNotEmpty() == true) flags = flags or EVIDENCE_HYPEROS_CODE
        return flags
    }
}
