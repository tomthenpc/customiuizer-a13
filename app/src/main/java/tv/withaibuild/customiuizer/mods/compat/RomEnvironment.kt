package tv.withaibuild.customiuizer.mods.compat

import android.os.Build
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import java.lang.reflect.Method
import java.util.Locale

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

internal data class RomEnvironment(
    val sdkInt: Int,
    val profile: RomProfile,
    val buildDisplay: String,
    val buildIncremental: String,
    val miuiVersionName: String?,
    val hyperOsVersionName: String?,
    val evidence: List<String>
)

internal object RomEnvironmentDetector {

    /** Test hook to suppress per-process diagnostic output when not relevant to the test. */
    @JvmField
    internal var recordDiagnostics: Boolean = true

    private const val MIUI_VERSION_NAME = "ro.miui.ui.version.name"
    private const val MIUI_VERSION_CODE = "ro.miui.ui.version.code"
    private const val HYPER_OS_VERSION_NAME = "ro.mi.os.version.name"
    private const val HYPER_OS_VERSION_CODE = "ro.mi.os.version.code"
    private const val BUILD_VERSION_INCREMENTAL = "ro.build.version.incremental"

    private val systemPropertiesGet: Method? by lazy {
        try {
            Class.forName("android.os.SystemProperties").getDeclaredMethod("get", String::class.java)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            null
        }
    }

    /** Production entry: reads [Build] and cached [android.os.SystemProperties] once. */
    fun detect(): RomEnvironment = detect(
        Build.VERSION.SDK_INT,
        Build.DISPLAY ?: "",
        Build.VERSION.INCREMENTAL ?: "",
        readSystemProperties()
    )

    /** Testable entry: all inputs are explicit. */
    fun detect(
        sdkInt: Int,
        buildDisplay: String,
        buildIncremental: String,
        properties: Map<String, String>
    ): RomEnvironment {
        val rawMiui = properties[MIUI_VERSION_NAME]?.takeIf { it.isNotEmpty() }
        val rawHyper = properties[HYPER_OS_VERSION_NAME]?.takeIf { it.isNotEmpty() }
        val miuiVersionName = rawMiui?.takeIf { isValidMiuiVersion(it) }
        val hyperOsVersionName = rawHyper?.takeIf { isValidHyperOsVersion(it) }

        val evidence = buildList {
            if (buildDisplay.isNotEmpty()) add("display=$buildDisplay")
            if (buildIncremental.isNotEmpty()) add("buildIncremental=$buildIncremental")
            properties[BUILD_VERSION_INCREMENTAL]?.let { if (it.isNotEmpty()) add("roIncremental=$it") }
            miuiVersionName?.let { add("miui=$it") }
            properties[MIUI_VERSION_CODE]?.let { if (it.isNotEmpty()) add("miuiCode=$it") }
            hyperOsVersionName?.let { add("hyperos=$it") }
            properties[HYPER_OS_VERSION_CODE]?.let { if (it.isNotEmpty()) add("hyperosCode=$it") }
        }

        val profile = when {
            sdkInt != 33 -> RomProfile.UNSUPPORTED_ANDROID
            hyperOsVersionName != null -> RomProfile.HYPEROS1_A13
            miuiVersionName != null -> RomProfile.MIUI14_A13
            else -> RomProfile.UNKNOWN_A13
        }

        val environment = RomEnvironment(
            sdkInt = sdkInt,
            profile = profile,
            buildDisplay = buildDisplay,
            buildIncremental = buildIncremental,
            miuiVersionName = miuiVersionName,
            hyperOsVersionName = hyperOsVersionName,
            evidence = evidence
        )

        recordEnvironment(environment)
        return environment
    }

    /** HyperOS 1 candidate: "OS1" or "OS1.x.y.z" after normalization. */
    internal fun isValidHyperOsVersion(value: String): Boolean {
        val v = value.trim().uppercase(Locale.ROOT)
        if (v.isEmpty()) return false
        if (v == "OS1") return true
        if (v.startsWith("OS1")) {
            val next = v.getOrNull(3)
            return next == null || next == '.' || next.isDigit()
        }
        return false
    }

    /** MIUI 14 candidate: "V14" or "V14.x.y.z" after normalization. */
    internal fun isValidMiuiVersion(value: String): Boolean {
        val v = value.trim().uppercase(Locale.ROOT)
        if (v.isEmpty()) return false
        if (v == "V14") return true
        if (v.startsWith("V14")) {
            val next = v.getOrNull(3)
            return next == null || next == '.' || next.isDigit()
        }
        return false
    }

    private fun readSystemProperties(): Map<String, String> {
        val keys = listOf(
            MIUI_VERSION_NAME,
            MIUI_VERSION_CODE,
            HYPER_OS_VERSION_NAME,
            HYPER_OS_VERSION_CODE,
            BUILD_VERSION_INCREMENTAL
        )
        return keys.associateWith { getSystemProperty(it) ?: "" }
    }

    private fun getSystemProperty(key: String): String? {
        val method = systemPropertiesGet ?: return null
        return try {
            method.invoke(null, key) as? String
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) throw t
            null
        }
    }

    private fun recordEnvironment(environment: RomEnvironment) {
        if (!recordDiagnostics) return
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
            detail = "${environment.profile.name}; sdk=${environment.sdkInt}; evidence=[${environment.evidence.joinToString(", ")}]"
        )
    }
}
