package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome

class InstallerVariantBindingTest {

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

    @Test
    fun installRecords_onlyCountSelectedVariantTargets() {
        val abc = FeatureTargetVariant(
            id = "abc",
            requirements = listOf(
                SingleTargetRequirement(
                    target(
                        "clamp.abc",
                        "Abc",
                        "clamp",
                        HookOperation.EXACT_METHOD,
                        listOf(Float::class.javaPrimitiveType!!)
                    )
                ),
                SingleTargetRequirement(target("ctor.abc", "Abc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val dpc = FeatureTargetVariant(
            id = "dpc",
            requirements = listOf(
                SingleTargetRequirement(
                    target(
                        "clamp.dpc",
                        "Dpc",
                        "clamp",
                        HookOperation.EXACT_METHOD,
                        listOf(Float::class.javaPrimitiveType!!)
                    )
                ),
                SingleTargetRequirement(target("ctor.dpc", "Dpc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val contract = HookTargetContract("test", listOf(abc, dpc))

        // Resolver selects abc.
        val resolver = VariantTestResolver(
            resolvedMethods = setOf("Abc.clamp"),
            resolvedClasses = setOf("Abc")
        )
        val (_, compat) = resolver.evaluateContract(contract, "test")
        assertEquals("abc", compat.selectedVariantId)

        // Legacy installer only installs the abc clamp. The abc ctor is not actually hooked,
        // so the final result is FAILED for this synthetic test.
        val result = HookInstaller.withSession(
            resolver = resolver,
            contract = contract,
            diagnosticId = "test",
            classLoader = this.javaClass.classLoader!!,
            compatibilityResult = compat
        ) {
            HookInstaller.recordInstall(
                "Abc",
                "clamp",
                HookOperation.EXACT_METHOD,
                listOf(Float::class.javaPrimitiveType!!),
                1
            )
        }

        assertTrue(result.records.any { it.spec.id == "clamp.abc" })
        assertFalse(result.records.any { it.spec.id == "clamp.dpc" })
        assertEquals(2, result.records.size)
        assertEquals(InstallOutcome.FAILED, result.installation)
    }

    @Test
    fun fallbackVariant_installerRecordsDoNotMixWithPrimaryVariant() {
        val primary = FeatureTargetVariant(
            id = "primary",
            requirements = listOf(SingleTargetRequirement(target("p.a", "primary.Foo", "a")))
        )
        val fallback = FeatureTargetVariant(
            id = "fallback",
            requirements = listOf(SingleTargetRequirement(target("f.x", "fallback.Foo", "x")))
        )
        val contract = HookTargetContract("test", listOf(primary, fallback))

        val resolver = VariantTestResolver(resolvedMethods = setOf("fallback.Foo.x"))
        val (_, compat) = resolver.evaluateContract(contract, "test")
        assertEquals("fallback", compat.selectedVariantId)

        val result = HookInstaller.withSession(
            resolver = resolver,
            contract = contract,
            diagnosticId = "test",
            classLoader = this.javaClass.classLoader!!,
            compatibilityResult = compat
        ) {
            HookInstaller.recordInstall("fallback.Foo", "x", HookOperation.EXACT_METHOD, emptyList(), 1)
        }

        assertTrue(result.records.any { it.spec.id == "f.x" })
        assertFalse(result.records.any { it.spec.id == "p.a" })
        assertEquals(InstallOutcome.INSTALLED, result.installation)
    }
}
