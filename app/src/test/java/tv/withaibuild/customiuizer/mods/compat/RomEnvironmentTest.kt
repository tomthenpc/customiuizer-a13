package tv.withaibuild.customiuizer.mods.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RomEnvironmentTest {

    private fun env(
        sdk: Int = 33,
        display: String = "",
        incremental: String = "",
        props: Map<String, String> = emptyMap()
    ) = RomEnvironmentDetector.detect(sdk, display, incremental, props)

    @Test
    fun miui14V13IsDetected() {
        val env = env(
            props = mapOf(
                "ro.miui.ui.version.name" to "V14.0.0",
                "ro.miui.ui.version.code" to "14"
            )
        )
        assertEquals(RomProfile.MIUI14_A13, env.profile)
        assertEquals("V14.0.0", env.miuiVersionName)
        assertNull(env.hyperOsVersionName)
        assertTrue(env.evidence.any { it.startsWith("miui=") })
    }

    @Test
    fun hyperOs1IsDetected() {
        val env = env(
            props = mapOf(
                "ro.mi.os.version.name" to "OS1.0.2.0",
                "ro.mi.os.version.code" to "1"
            )
        )
        assertEquals(RomProfile.HYPEROS1_A13, env.profile)
        assertEquals("OS1.0.2.0", env.hyperOsVersionName)
        assertNull(env.miuiVersionName)
        assertTrue(env.evidence.any { it.startsWith("hyperos=") })
    }

    @Test
    fun bothMiuiAndHyperOsIsUnknown() {
        val env = env(
            props = mapOf(
                "ro.miui.ui.version.name" to "V14.0.0",
                "ro.mi.os.version.name" to "OS1.0.2.0"
            )
        )
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
    }

    @Test
    fun noEvidenceOnA13IsUnknown() {
        val env = env()
        assertEquals(RomProfile.UNKNOWN_A13, env.profile)
        assertEquals(33, env.sdkInt)
    }

    @Test
    fun api34IsUnsupported() {
        val env = env(sdk = 34)
        assertEquals(RomProfile.UNSUPPORTED_ANDROID, env.profile)
    }

    @Test
    fun api32IsUnsupported() {
        val env = env(sdk = 32)
        assertEquals(RomProfile.UNSUPPORTED_ANDROID, env.profile)
    }

    @Test
    fun evidenceIncludesBuildValues() {
        val env = env(
            display = "TKQ1.220829.002",
            incremental = "V14.0.0.0.TKHCNXM",
            props = mapOf("ro.build.version.incremental" to "TKHCNXM")
        )
        assertTrue(env.evidence.any { it.startsWith("display=") })
        assertTrue(env.evidence.any { it.startsWith("buildIncremental=") })
        assertTrue(env.evidence.any { it.startsWith("roIncremental=") })
    }
}
