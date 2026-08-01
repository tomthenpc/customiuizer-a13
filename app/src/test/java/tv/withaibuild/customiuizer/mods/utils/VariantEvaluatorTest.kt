package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

class VariantEvaluatorTest {

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
    fun primaryVariantComplete_selectsPrimaryAndStops() {
        val primary = FeatureTargetVariant(
            id = "primary",
            requirements = listOf(SingleTargetRequirement(target("p.a", "primary.Foo", "a")))
        )
        val fallback = FeatureTargetVariant(
            id = "fallback",
            requirements = listOf(SingleTargetRequirement(target("f.x", "fallback.Foo", "x")))
        )
        val contract = HookTargetContract("test", listOf(primary, fallback))

        val resolver = VariantTestResolver(resolvedMethods = setOf("primary.Foo.a"))
        val result = resolver.evaluateContract(contract, "test")

        assertEquals(CompatibilityState.COMPATIBLE, result.first)
        assertEquals("primary", result.second.selectedVariantId)
        assertEquals(ReasonCode.PRIMARY_TARGET_FOUND, result.second.reasonCode)
        assertFalse(result.second.fallbackUsed)
    }

    @Test
    fun primaryFailsFallbackComplete_selectsFallback() {
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
        val result = resolver.evaluateContract(contract, "test")

        // Variant fallback is not the same as AnyOf fallback: a selected variant is
        // a complete, valid bundle, so it is COMPATIBLE. The selected variant id
        // is what identifies which bundle was chosen.
        assertEquals(CompatibilityState.COMPATIBLE, result.first)
        assertEquals("fallback", result.second.selectedVariantId)
        assertFalse(result.second.fallbackUsed)
    }

    @Test
    fun mixedPrimaryAndFallbackTargets_areNotCombined() {
        val abc = FeatureTargetVariant(
            id = "abc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.abc", "Abc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.abc", "Abc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val dpc = FeatureTargetVariant(
            id = "dpc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.dpc", "Dpc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.dpc", "Dpc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val contract = HookTargetContract("test", listOf(abc, dpc))

        // One target from each variant; neither variant is complete.
        val resolver = VariantTestResolver(
            resolvedMethods = setOf("Abc.clamp"),
            resolvedClasses = setOf("Dpc")
        )
        val result = resolver.evaluateContract(contract, "test")

        assertEquals(CompatibilityState.INCOMPATIBLE, result.first)
    }

    @Test
    fun abcVariantComplete_antiMixingForAutoBrightness() {
        val abc = FeatureTargetVariant(
            id = "abc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.abc", "Abc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.abc", "Abc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val dpc = FeatureTargetVariant(
            id = "dpc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.dpc", "Dpc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.dpc", "Dpc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val contract = HookTargetContract("test", listOf(abc, dpc))

        val resolver = VariantTestResolver(
            resolvedMethods = setOf("Abc.clamp"),
            resolvedClasses = setOf("Abc")
        )
        val result = resolver.evaluateContract(contract, "test")

        assertEquals(CompatibilityState.COMPATIBLE, result.first)
        assertEquals("abc", result.second.selectedVariantId)
        assertEquals(setOf("clamp.abc", "ctor.abc"), result.second.records.map { it.spec.id }.toSet())
    }

    @Test
    fun dpcVariantComplete_selectsDpcNotMixedWithAbc() {
        val abc = FeatureTargetVariant(
            id = "abc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.abc", "Abc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.abc", "Abc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val dpc = FeatureTargetVariant(
            id = "dpc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.dpc", "Dpc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.dpc", "Dpc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val contract = HookTargetContract("test", listOf(abc, dpc))

        val resolver = VariantTestResolver(
            resolvedMethods = setOf("Dpc.clamp"),
            resolvedClasses = setOf("Dpc")
        )
        val result = resolver.evaluateContract(contract, "test")

        assertEquals(CompatibilityState.COMPATIBLE, result.first)
        assertEquals("dpc", result.second.selectedVariantId)
    }

    @Test
    fun partialAbcPartialDpc_isIncompatible() {
        val abc = FeatureTargetVariant(
            id = "abc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.abc", "Abc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.abc", "Abc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val dpc = FeatureTargetVariant(
            id = "dpc",
            requirements = listOf(
                SingleTargetRequirement(target("clamp.dpc", "Dpc", "clamp", parameterTypes = listOf(Float::class.javaPrimitiveType!!))),
                SingleTargetRequirement(target("ctor.dpc", "Dpc", null, HookOperation.ALL_CONSTRUCTORS))
            )
        )
        val contract = HookTargetContract("test", listOf(abc, dpc))

        val resolver = VariantTestResolver(
            resolvedMethods = setOf("Abc.clamp"),
            resolvedClasses = setOf("Dpc")
        )
        val result = resolver.evaluateContract(contract, "test")

        assertEquals(CompatibilityState.INCOMPATIBLE, result.first)
    }

    @Test
    fun optionalMissingInSelectedVariant_degradedButSelected() {
        val primary = FeatureTargetVariant(
            id = "primary",
            requirements = listOf(
                SingleTargetRequirement(target("required", "Req", "required")),
                SingleTargetRequirement(
                    target("optional", "Opt", "optional"),
                    criticality = Criticality.OPTIONAL
                )
            )
        )
        val contract = HookTargetContract("test", listOf(primary))
        val resolver = VariantTestResolver(resolvedMethods = setOf("Req.required"))

        val result = resolver.evaluateContract(contract, "test")

        assertEquals(CompatibilityState.DEGRADED, result.first)
        assertEquals(InstallOutcome.DEGRADED, result.second.installation)
    }
}
