package tv.withaibuild.customiuizer.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.FeatureCatalog
import tv.withaibuild.customiuizer.mods.catalog.RestartTarget

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
            assertTrue("key should not be empty", entry.key.isNotEmpty())
            assertTrue("ownerFeature should not be empty", entry.ownerFeature.isNotEmpty())
            assertNotNull(entry.restartTarget)
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
    fun ownerFeatureReferencesExistingCatalogFeature() {
        val catalogIds = FeatureCatalog.specs().map { it.id }.toSet()
        for (entry in PreferenceSchema.entries) {
            assertTrue(
                "${entry.key}: ownerFeature ${entry.ownerFeature} not in catalog",
                catalogIds.contains(entry.ownerFeature)
            )
        }
    }

    @Test
    fun byKeyMapsEachEntry() {
        for (entry in PreferenceSchema.entries) {
            assertEquals(entry, PreferenceSchema.byKey[entry.key])
        }
    }

    @Test
    fun intConstraintsAreSatisfiedByDefaults() {
        for (entry in PreferenceSchema.entries) {
            val constraint = entry.constraint
            if (constraint is PreferenceConstraint.IntRange && entry.defaultValue is Int) {
                val value = entry.defaultValue as Int
                assertTrue(
                    "${entry.key}: $value not in [${constraint.min}, ${constraint.max}]",
                    value in constraint.min..constraint.max
                )
            }
        }
    }

    @Test
    fun restartTargetIsNotNoneForUiVisibleFeatures() {
        for (entry in PreferenceSchema.entries) {
            // All current representative features need at least a SystemUI restart.
            assertTrue(
                "${entry.key}: restart target should not be NONE",
                entry.restartTarget != RestartTarget.NONE
            )
        }
    }
}
