package tv.withaibuild.customiuizer.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceSchemaTest {

    @Test
    fun entriesHaveNoDuplicateKeys() {
        val keys = PreferenceSchema.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(PreferenceSchema.entries.size, PreferenceSchema.byKey.size)
    }

    @Test
    fun eachEntryHasNonEmptyMetadata() {
        for (entry in PreferenceSchema.entries) {
            assertNotNull(entry.key)
            assertTrue("key should not be empty", entry.key.isNotEmpty())
            assertNotNull(entry.ownerFeature)
            assertTrue("ownerFeature should not be empty", entry.ownerFeature.isNotEmpty())
            assertNotNull(entry.requiresRestart)
            assertTrue("requiresRestart should not be empty", entry.requiresRestart.isNotEmpty())
        }
    }

    @Test
    fun eachDefaultValueMatchesDeclaredType() {
        for (entry in PreferenceSchema.entries) {
            val valid = when (entry.type) {
                PreferenceType.BOOLEAN -> entry.defaultValue is Boolean
                PreferenceType.INT -> entry.defaultValue is Int
                PreferenceType.STRING -> entry.defaultValue is String
                PreferenceType.STRING_SET -> entry.defaultValue is Set<*>
            }
            assertTrue(
                "${entry.key}: defaultValue ${entry.defaultValue} does not match type ${entry.type}",
                valid
            )
        }
    }

    @Test
    fun byKeyMapsEachEntry() {
        for (entry in PreferenceSchema.entries) {
            assertEquals(entry, PreferenceSchema.byKey[entry.key])
        }
    }
}
