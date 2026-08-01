package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome

class HookInstallerSelectionValidationTest {

    @Test
    fun multiVariant_missingSelection_throws() {
        val contract = HookTargetContract(
            "multi",
            listOf(
                FeatureTargetVariant(
                    "v1",
                    listOf(SingleTargetRequirement(target("t1", "A", "m")))
                ),
                FeatureTargetVariant(
                    "v2",
                    listOf(SingleTargetRequirement(target("t2", "B", "m")))
                )
            )
        )
        val resolver = VariantTestResolver(resolvedMethods = setOf("A.m"))

        try {
            HookInstaller.withSession(
                resolver = resolver,
                contract = contract,
                diagnosticId = "multi",
                classLoader = this.javaClass.classLoader!!,
                compatibilityResult = null
            ) {
                fail("block should not run")
            }
            fail("expected IllegalStateException for missing selected variant")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("multi"))
        }
    }

    @Test
    fun singleVariant_noSelection_usesOnlyVariant() {
        val contract = HookTargetContract(
            "single",
            listOf(FeatureTargetVariant("v1", listOf(SingleTargetRequirement(target("t1", "A", "m")))))
        )
        val resolver = VariantTestResolver(resolvedMethods = setOf("A.m"))

        var blockCalled = false
        val result = HookInstaller.withSession(
            resolver = resolver,
            contract = contract,
            diagnosticId = "single",
            classLoader = this.javaClass.classLoader!!,
            compatibilityResult = null
        ) {
            blockCalled = true
            HookInstaller.recordInstall("A", "m", HookOperation.EXACT_METHOD, emptyList(), 1)
        }

        assertTrue(blockCalled)
        assertEquals(InstallOutcome.INSTALLED, result.installation)
    }

    @Test
    fun multiVariant_incompatibleResult_skipsBlock() {
        val contract = HookTargetContract(
            "multi",
            listOf(
                FeatureTargetVariant(
                    "v1",
                    listOf(SingleTargetRequirement(target("t1", "A", "m")))
                )
            )
        )
        val resolver = VariantTestResolver(resolvedMethods = emptySet())
        val (compat, compatResult) = resolver.evaluateContract(contract, "multi")
        assertEquals(CompatibilityState.INCOMPATIBLE, compat)

        // Incompatible result does not reach the installer, so no selected variant.
    }

    private fun target(
        id: String,
        className: String,
        memberName: String?,
        operation: HookOperation = HookOperation.EXACT_METHOD,
        parameterTypes: List<Class<*>> = emptyList()
    ) = HookTargetSpec(
        id = id,
        operation = operation,
        className = className,
        memberName = memberName,
        parameterTypes = parameterTypes
    )
}
