package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds

/**
 * Identity completeness invariants for the typed Feature catalog.
 *
 * This is the mechanical gate for P1.1: every FeatureId enum has exactly one
 * FeatureSpec, canonical/normalized/alias IDs are collision-free, diagnostic IDs
 * are unique, and the FeatureInstallRegistry can register the whole catalog
 * without conflicts.
 */
class FeatureIdentityCompletenessTest {

    @Test
    fun featureIdsAndSpecsAreMutuallyComplete() {
        val specs = FeatureCatalog.specs()
        val specIds = specs.map { it.id }.toSet()
        val featureIds = FeatureId.values().map { it.canonicalId }.toSet()

        assertEquals(
            "FeatureCatalog spec count must equal FeatureId enum count",
            FeatureId.values().size,
            specs.size
        )

        for (featureId in FeatureId.values()) {
            assertTrue(
                "FeatureId ${featureId.name} canonical id ${featureId.canonicalId} missing from FeatureCatalog",
                featureId.canonicalId in specIds
            )
        }

        for (spec in specs) {
            assertTrue(
                "FeatureCatalog spec id ${spec.id} has no matching FeatureId enum entry",
                spec.id in featureIds
            )
        }
    }

    @Test
    fun canonicalIdsAreUnique() {
        val ids = FeatureCatalog.specs().map { it.id }
        assertEquals(
            "Canonical ids must be unique (size ${ids.size}, unique ${ids.toSet().size})",
            ids.size,
            ids.toSet().size
        )
    }

    @Test
    fun normalizedIdsAreUnique() {
        val normalized = FeatureCatalog.specs().map { FeatureIdentity.normalizeLookupId(it.id) }
        assertEquals(
            "Normalized ids must be unique",
            normalized.size,
            normalized.toSet().size
        )
    }

    @Test
    fun aliasesDoNotCollideWithCanonicalOrOtherAliases() {
        val specs = FeatureCatalog.specs()
        val canonicalByNormalized = mutableMapOf<String, String>()
        for (spec in specs) {
            canonicalByNormalized[FeatureIdentity.normalizeLookupId(spec.id)] = spec.id
        }

        for (spec in specs) {
            for (alias in spec.aliases) {
                val norm = FeatureIdentity.normalizeLookupId(alias)
                val existing = canonicalByNormalized[norm]
                assertTrue(
                    "Alias '$alias' of ${spec.id} collides with canonical '$existing'",
                    existing == null || existing == spec.id
                )
            }
        }
    }

    @Test
    fun diagnosticIdsAreUniqueAndNonEmpty() {
        val specs = FeatureCatalog.specs()
        val diagnosticIds = specs.map { it.diagnosticId }
        for (id in diagnosticIds) {
            assertTrue("Diagnostic id must not be empty", id.isNotEmpty())
        }
        assertEquals(
            "Diagnostic ids must be unique",
            diagnosticIds.size,
            diagnosticIds.toSet().size
        )
    }

    @Test
    fun diagnosticIdsAreDeclaredInDiagnosticIdsObject() {
        val specs = FeatureCatalog.specs()
        val declared = DiagnosticIds::class.java.declaredFields
            .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.get(null) as String }
            .toSet()

        for (spec in specs) {
            assertTrue(
                "Spec ${spec.id} diagnostic id ${spec.diagnosticId} not declared in DiagnosticIds",
                spec.diagnosticId in declared
            )
        }
    }

    @Test
    fun registrySpecsDoNotCollideWhenRegistered() {
        FeatureInstallRegistry.resetRegistryForTesting()
        FeatureInstallRegistry.registerAll(FeatureCatalog.registrySpecs())

        val registrySpecs = FeatureCatalog.registrySpecs()
        val legacySpecs = FeatureCatalog.specs() - registrySpecs.toSet()

        assertEquals(
            "Catalog specs should be the union of registry and legacy specs with no overlap",
            FeatureCatalog.specs().size,
            registrySpecs.size + legacySpecs.size
        )
    }

    @Test
    fun wholeCatalogCanBeRegisteredWithoutConflict() {
        FeatureInstallRegistry.resetRegistryForTesting()
        FeatureInstallRegistry.registerAll(FeatureCatalog.specs())
    }

    @Test
    fun allRequiredFieldsArePopulated() {
        for (spec in FeatureCatalog.specs()) {
            assertTrue("Spec id must not be empty", spec.id.isNotEmpty())
            assertTrue("Spec diagnosticId must not be empty", spec.diagnosticId.isNotEmpty())
            assertNotNull("Spec processTarget must be set", spec.processTarget)
            assertNotNull("Spec compatibilityPolicy must be set", spec.compatibilityPolicy)
            assertNotNull("Spec installer must be set", spec.installer)
            assertNotNull("Spec activationRestartTarget must be set", spec.activationRestartTarget)
            assertNotNull("Spec configReloadMode must be set", spec.configReloadMode)

            if (spec.compatibilityPolicy == CompatibilityPolicy.CONTRACT_REQUIRED) {
                assertNotNull(
                    "CONTRACT_REQUIRED spec ${spec.id} must provide a contract",
                    spec.contract
                )
            }
        }
    }

    @Test
    fun featureIdFromStringResolvesAllCanonicalAndAliasForms() {
        for (spec in FeatureCatalog.specs()) {
            assertEquals(
                "Canonical lookup failed for ${spec.id}",
                spec.id,
                FeatureId.fromString(spec.id)?.canonicalId
            )

            for (alias in spec.aliases) {
                assertEquals(
                    "Alias lookup failed for '$alias' of ${spec.id}",
                    spec.id,
                    FeatureId.fromString(alias)?.canonicalId
                )
            }
        }
    }
}
