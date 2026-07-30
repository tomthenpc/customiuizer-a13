package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureCatalogTest {

    @Test
    fun catalogContainsRepresentativeFeatures() {
        val specs = FeatureCatalog.specs()
        assertTrue(specs.any { it.id == "packagePermissions" })
        assertTrue(specs.any { it.id == "statusBarClockTweak" })
    }

    @Test
    fun packagePermissionsIsSystemServerScoped() {
        val feature = FeatureCatalog.specs().find { it.id == "packagePermissions" }
        assertNotNull(feature)
        assertEquals(null, feature?.targetPackage)
        assertTrue(feature?.requiresReboot == true)
        assertFalse(feature?.requiresSystemUIRestart == true)
    }

    @Test
    fun statusBarClockTweakIsSystemUIScoped() {
        val feature = FeatureCatalog.specs().find { it.id == "statusBarClockTweak" }
        assertNotNull(feature)
        assertEquals("com.android.systemui", feature?.targetPackage)
        assertFalse(feature?.requiresReboot == true)
        assertTrue(feature?.requiresSystemUIRestart == true)
    }

    @Test
    fun statusBarClockTweakConditionMatchesOriginal() {
        val feature = FeatureCatalog.specs().find { it.id == "statusBarClockTweak" }!!
        val prefs = tv.withaibuild.customiuizer.utils.PrefMap<String, Any?>()

        assertFalse(feature.condition(prefs))

        prefs["pref_key_system_statusbar_clocktweak"] = true
        assertTrue(feature.condition(prefs))

        prefs.clear()
        prefs["pref_key_system_cc_clocktweak"] = true
        assertTrue(feature.condition(prefs))

        prefs.clear()
        prefs["pref_key_system_cc_hidedate"] = true
        assertTrue(feature.condition(prefs))

        prefs.clear()
        prefs["pref_key_system_cc_dateformat"] = "MM/dd EEEE"
        assertTrue(feature.condition(prefs))
    }

    @Test
    fun allFeatureIdsAreStableAndUnique() {
        val ids = FeatureCatalog.specs().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
