package tv.withaibuild.customiuizer.mods.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

private class MapPropertyReader(private val values: Map<String, String>) : SystemPropertyReader {
    override fun get(key: String): String? = values[key]
}

private class ThrowingPropertyReader(
    private val exception: Throwable,
    private val throwKeys: Set<String>
) : SystemPropertyReader {
    override fun get(key: String): String? {
        if (throwKeys.contains(key)) throw exception
        return null
    }
}

class RomEnvironmentTest {

    private fun env(
        sdk: Int = 33,
        display: String = "",
        incremental: String = "",
        props: Map<String, String> = emptyMap()
    ): RomEnvironment = RomEnvironmentDetector.detect(sdk, display, incremental, MapPropertyReader(props))

    @Test
    fun sdkMismatchIsUnsupported() {
        val env = env(sdk = 32)
        assertEquals(RomProfile.UNSUPPORTED_ANDROID, env.profile)
        assertEquals(ReasonCode.ANDROID_VERSION_UNSUPPORTED, reasonFor(env.profile))
    }

    @Test
    fun api34IsUnsupported() {
        val env = env(sdk = 34)
        assertEquals(RomProfile.UNSUPPORTED_ANDROID, env.profile)
    }

    @Test
    fun noEvidenceIsUnknown() {
        val env = env()
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun validMiuiV14() {
        val env = env(props = mapOf("ro.miui.ui.version.name" to "V14"))
        assertEquals(RomProfile.MIUI14_A13, env.profile)
        assertEquals("V14", env.miuiVersionName)
    }

    @Test
    fun validMiuiV140() {
        val env = env(props = mapOf("ro.miui.ui.version.name" to "V14.0.10.0"))
        assertEquals(RomProfile.MIUI14_A13, env.profile)
    }

    @Test
    fun validHyperOsOS1() {
        val env = env(props = mapOf("ro.mi.os.version.name" to "OS1"))
        assertEquals(RomProfile.HYPEROS1_A13, env.profile)
    }

    @Test
    fun validHyperOsOS10100() {
        val env = env(props = mapOf("ro.mi.os.version.name" to "OS1.0.10.0"))
        assertEquals(RomProfile.HYPEROS1_A13, env.profile)
    }

    @Test
    fun hyperOsHasPriorityWhenBothPresent() {
        val env = env(
            props = mapOf(
                "ro.mi.os.version.name" to "OS1.0.10.0",
                "ro.miui.ui.version.name" to "V14"
            )
        )
        assertEquals(RomProfile.HYPEROS1_A13, env.profile)
    }

    @Test
    fun invalidHyperOsFallsBackToMiui() {
        val env = env(
            props = mapOf(
                "ro.mi.os.version.name" to "OS2.0",
                "ro.miui.ui.version.name" to "V14.0.10.0"
            )
        )
        assertEquals(RomProfile.MIUI14_A13, env.profile)
    }

    @Test
    fun invalidMiuiFallsBackToUnknown() {
        val env = env(props = mapOf("ro.miui.ui.version.name" to "V15.0.0.0"))
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun os10IsRejected() {
        val env = env(props = mapOf("ro.mi.os.version.name" to "OS10"))
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun osBarePrefixIsRejected() {
        val env = env(props = mapOf("ro.mi.os.version.name" to "OS"))
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun v140IsRejected() {
        val env = env(props = mapOf("ro.miui.ui.version.name" to "V140"))
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun v13IsRejected() {
        val env = env(props = mapOf("ro.miui.ui.version.name" to "V13"))
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun versionParsingHandlesCaseAndWhitespace() {
        assertTrue(RomEnvironmentDetector.isValidHyperOsVersion("  os1.0  "))
        assertTrue(RomEnvironmentDetector.isValidMiuiVersion("v14.0"))
        assertFalse(RomEnvironmentDetector.isValidHyperOsVersion("OS1x"))
        assertFalse(RomEnvironmentDetector.isValidMiuiVersion("V14x"))
    }

    @Test
    fun whitespaceOnlyIsInvalid() {
        assertFalse(RomEnvironmentDetector.isValidHyperOsVersion("   "))
        assertFalse(RomEnvironmentDetector.isValidMiuiVersion("   "))
    }

    @Test
    fun evidenceIncludesBuildValues() {
        val env = env(
            display = "TKQ1.220829.002",
            incremental = "V14.0.0.0.TKHCNXM",
            props = mapOf("ro.build.version.incremental" to "TKHCNXM")
        )
        assertTrue(env.evidenceFlags and RomEnvironmentDetector.EVIDENCE_DISPLAY != 0)
        assertTrue(env.evidenceFlags and RomEnvironmentDetector.EVIDENCE_BUILD_INCREMENTAL != 0)
        assertTrue(env.evidenceFlags and RomEnvironmentDetector.EVIDENCE_RO_INCREMENTAL != 0)
    }

    @Test
    fun evidenceIsSanitized() {
        val env = env(
            display = "\nline1\r\nline2",
            incremental = "A".repeat(300)
        )
        assertFalse(env.buildDisplay.contains("\n"))
        assertTrue(env.buildIncremental.length <= 128)
    }

    @Test
    fun readerReturningNullIsUnknown() {
        val env = RomEnvironmentDetector.detect(33, "", "", MapPropertyReader(emptyMap()))
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun runtimeExceptionDoesNotCrashDetection() {
        val reader = ThrowingPropertyReader(RuntimeException("test"), setOf("ro.mi.os.version.name"))
        val env = RomEnvironmentDetector.detect(33, "", "", reader)
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun securityExceptionDoesNotCrashDetection() {
        val reader = ThrowingPropertyReader(SecurityException("denied"), setOf("ro.miui.ui.version.name"))
        val env = RomEnvironmentDetector.detect(33, "", "", reader)
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun directOutOfMemoryErrorIsRethrown() {
        val reader = ThrowingPropertyReader(OutOfMemoryError(), setOf("ro.mi.os.version.name"))
        try {
            RomEnvironmentDetector.detect(33, "", "", reader)
            assertTrue("expected OOM", false)
        } catch (oom: OutOfMemoryError) {
            // expected
        }
    }

    @Test
    fun invocationTargetExceptionWrappingOomIsRethrown() {
        val cause = OutOfMemoryError()
        val reader = ThrowingPropertyReader(java.lang.reflect.InvocationTargetException(cause), setOf("ro.mi.os.version.name"))
        try {
            RomEnvironmentDetector.detect(33, "", "", reader)
            assertTrue("expected OOM", false)
        } catch (oom: OutOfMemoryError) {
            // expected
        }
    }

    @Test
    fun exceptionInInitializerErrorWrappingOomIsRethrown() {
        val cause = OutOfMemoryError()
        val reader = ThrowingPropertyReader(ExceptionInInitializerError(cause), setOf("ro.mi.os.version.name"))
        try {
            RomEnvironmentDetector.detect(33, "", "", reader)
            assertTrue("expected OOM", false)
        } catch (oom: OutOfMemoryError) {
            // expected
        }
    }

    @Test
    fun wrappedRuntimeExceptionDoesNotCrash() {
        val reader = ThrowingPropertyReader(java.lang.reflect.InvocationTargetException(RuntimeException("test")), setOf("ro.mi.os.version.name"))
        val env = RomEnvironmentDetector.detect(33, "", "", reader)
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun readsEachPropertyAtMostOnce() {
        val props = mutableMapOf<String, Int>()
        val reader = SystemPropertyReader { key ->
            props[key] = (props[key] ?: 0) + 1
            null
        }
        RomEnvironmentDetector.detect(33, "", "", reader)
        assertEquals(5, props.size)
        for (count in props.values) {
            assertEquals(1, count)
        }
    }

    @Test
    fun doesNotReadUnknownProperties() {
        val requestedKeys = mutableListOf<String>()
        val reader = SystemPropertyReader { key ->
            requestedKeys += key
            null
        }
        RomEnvironmentDetector.detect(33, "", "", reader)
        assertTrue(requestedKeys.contains("ro.miui.ui.version.name"))
        assertTrue(requestedKeys.contains("ro.mi.os.version.name"))
        assertTrue(requestedKeys.contains("ro.build.version.incremental"))
        assertFalse(requestedKeys.contains("ro.unknown.prop"))
    }

    private fun reasonFor(profile: RomProfile): ReasonCode = when (profile) {
        RomProfile.MIUI14_A13, RomProfile.HYPEROS1_A13 -> ReasonCode.ROM_PROFILE_DETECTED
        RomProfile.UNKNOWN_A13 -> ReasonCode.ROM_PROFILE_UNKNOWN
        RomProfile.UNSUPPORTED_ANDROID -> ReasonCode.ANDROID_VERSION_UNSUPPORTED
    }
}
