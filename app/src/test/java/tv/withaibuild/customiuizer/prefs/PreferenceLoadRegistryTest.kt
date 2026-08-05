package tv.withaibuild.customiuizer.prefs

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.utils.FakeSharedPreferences

class PreferenceLoadRegistryTest {

    private fun prefs(vararg pairs: Pair<String, Any?>): SharedPreferences {
        val fake = FakeSharedPreferences()
        for ((key, value) in pairs) {
            if (value != null) fake.put(key, value)
        }
        return fake
    }

    private val selectedPackage = "com.spotify.music"
    private val unrelatedPackage = "com.example.unrelated"
    private val knownPackage = "com.android.systemui"

    @Test
    fun knownPackage_loadsRegardlessOfSwitches() {
        val fake = prefs()
        assertTrue(
            "known package should always load prefs",
            PreferenceLoadRegistry.shouldLoad(fake, knownPackage)
        )
    }

    @Test
    fun alarmCompat_enabledAndSelected_loads() {
        val fake = prefs(
            "pref_key_various_alarmcompat" to true,
            "pref_key_various_alarmcompat_apps" to setOf(selectedPackage)
        )
        assertTrue(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun alarmCompat_disabled_doesNotLoad() {
        val fake = prefs(
            "pref_key_various_alarmcompat" to false,
            "pref_key_various_alarmcompat_apps" to setOf(selectedPackage)
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun alarmCompat_packageAbsent_doesNotLoad() {
        val fake = prefs(
            "pref_key_various_alarmcompat" to true,
            "pref_key_various_alarmcompat_apps" to setOf("com.other.app")
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun alarmCompat_appSetMissing_doesNotLoad() {
        val fake = prefs("pref_key_various_alarmcompat" to true)
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun statusBarColor_enabledAndSelected_loads() {
        val fake = prefs(
            "pref_key_system_statusbarcolor" to true,
            "pref_key_system_statusbarcolor_apps" to setOf(selectedPackage)
        )
        assertTrue(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun statusBarColor_disabled_doesNotLoad() {
        val fake = prefs(
            "pref_key_system_statusbarcolor" to false,
            "pref_key_system_statusbarcolor_apps" to setOf(selectedPackage)
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun statusBarColor_packageAbsent_doesNotLoad() {
        val fake = prefs(
            "pref_key_system_statusbarcolor" to true,
            "pref_key_system_statusbarcolor_apps" to setOf("com.other.app")
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun noOverscroll_enabledAndSelected_loads() {
        val fake = prefs(
            "pref_key_system_nooverscroll" to true,
            "pref_key_system_nooverscroll_apps" to setOf(selectedPackage)
        )
        assertTrue(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun noOverscroll_disabled_doesNotLoad() {
        val fake = prefs(
            "pref_key_system_nooverscroll" to false,
            "pref_key_system_nooverscroll_apps" to setOf(selectedPackage)
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun noOverscroll_packageAbsent_doesNotLoad() {
        val fake = prefs(
            "pref_key_system_nooverscroll" to true,
            "pref_key_system_nooverscroll_apps" to setOf("com.other.app")
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_upGreaterThanZeroAndSelected_loads() {
        val fake = prefs(
            "pref_key_controls_volumemedia_up" to "1",
            "pref_key_controls_mediaplayer_apps" to setOf(selectedPackage)
        )
        assertTrue(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_downGreaterThanZeroAndSelected_loads() {
        val fake = prefs(
            "pref_key_controls_volumemedia_down" to "1",
            "pref_key_controls_mediaplayer_apps" to setOf(selectedPackage)
        )
        assertTrue(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_bothZeroAndSelected_doesNotLoad() {
        val fake = prefs(
            "pref_key_controls_volumemedia_up" to "0",
            "pref_key_controls_volumemedia_down" to "0",
            "pref_key_controls_mediaplayer_apps" to setOf(selectedPackage)
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_enabledButPackageAbsent_doesNotLoad() {
        val fake = prefs(
            "pref_key_controls_volumemedia_up" to "1",
            "pref_key_controls_mediaplayer_apps" to setOf("com.other.app")
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_upOne_downBad_shortCircuitTrue() {
        val fake = prefs(
            "pref_key_controls_volumemedia_up" to "1",
            "pref_key_controls_volumemedia_down" to "bad",
            "pref_key_controls_mediaplayer_apps" to setOf(selectedPackage)
        )
        assertTrue(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_upZero_downBad_catchesAndFalse() {
        val fake = prefs(
            "pref_key_controls_volumemedia_up" to "0",
            "pref_key_controls_volumemedia_down" to "bad",
            "pref_key_controls_mediaplayer_apps" to setOf(selectedPackage)
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_upBad_downOne_leftThrowsAndFalse() {
        val fake = prefs(
            "pref_key_controls_volumemedia_up" to "bad",
            "pref_key_controls_volumemedia_down" to "1",
            "pref_key_controls_mediaplayer_apps" to setOf(selectedPackage)
        )
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun volumeMedia_appSetMissing_doesNotLoad() {
        val fake = prefs("pref_key_controls_volumemedia_up" to "1")
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, selectedPackage))
    }

    @Test
    fun unrelatedPackage_allSwitchesOff_doesNotLoad() {
        val fake = prefs()
        assertFalse(PreferenceLoadRegistry.shouldLoad(fake, unrelatedPackage))
    }

    @Test
    fun registryRulesHaveUniqueIds() {
        val ids = PreferenceLoadRegistry.rules.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun registryHasExactlyFourFeatureRules() {
        assertEquals(4, PreferenceLoadRegistry.rules.size)
    }

    @Test
    fun eachRuleHasNonEmptyPreferenceKeys() {
        for (rule in PreferenceLoadRegistry.rules) {
            assertTrue("rule ${rule.id} has empty preferenceKeys", rule.preferenceKeys.isNotEmpty())
        }
    }

    @Test
    fun registryDoesNotMutateStringSet() {
        val appSet = mutableSetOf(selectedPackage)
        val fake = prefs(
            "pref_key_various_alarmcompat" to true,
            "pref_key_various_alarmcompat_apps" to appSet
        )
        PreferenceLoadRegistry.shouldLoad(fake, selectedPackage)
        assertEquals(setOf(selectedPackage), appSet)
        assertNotEquals(emptySet<String>(), appSet)
    }
}
