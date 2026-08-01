package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

class HookEvidenceEvaluatorTest {

    private fun target(
        id: String,
        operation: HookOperation = HookOperation.EXACT_METHOD,
        className: String = "test.Foo",
        memberName: String? = "bar",
        parameterTypes: List<Class<*>> = emptyList()
    ) = HookTargetSpec(id, operation, className, memberName, parameterTypes)

    private fun record(
        spec: HookTargetSpec,
        requirementId: String = spec.id,
        resolved: Boolean = false,
        installed: Boolean = false
    ) = HookTargetRecord(spec, requirementId, resolved, installed)

    private fun contract(vararg requirements: HookRequirement) =
        HookTargetContract("test", requirements.toList())

    @Test
    fun primarySucceedsFallbackMissing_compatibilityInstalled_noFallbackUsed() {
        val primary = target("primary")
        val fallback = target("fallback")
        val req = AnyOfRequirement(
            id = "group",
            criticality = Criticality.REQUIRED,
            candidates = listOf(primary, fallback)
        )
        val c = contract(req)
        val records = listOf(
            record(primary, resolved = true, installed = true),
            record(fallback, resolved = true, installed = false)
        )

        val compat = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.COMPATIBILITY)
        assertEquals(CompatibilityState.COMPATIBLE, compat.compatibility)
        assertFalse(compat.fallbackUsed)
        assertEquals(1, compat.requiredInstalled)
        assertEquals(1, compat.requiredTotal)

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.INSTALLED, install.installation)
        assertFalse(install.fallbackUsed)
        assertEquals(0, install.requiredFailures.size)
    }

    @Test
    fun fallbackSucceedsPrimaryMissing_degradedAndFallbackUsed() {
        val primary = target("primary")
        val fallback = target("fallback")
        val req = AnyOfRequirement(
            id = "group",
            criticality = Criticality.REQUIRED,
            candidates = listOf(primary, fallback)
        )
        val c = contract(req)
        val records = listOf(
            record(primary, resolved = false, installed = false),
            record(fallback, resolved = true, installed = true)
        )

        val compat = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.COMPATIBILITY)
        assertEquals(CompatibilityState.DEGRADED, compat.compatibility)
        assertTrue(compat.fallbackUsed)
        assertEquals(ReasonCode.FALLBACK_TARGET_FOUND, compat.reasonCode)

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.DEGRADED, install.installation)
        assertTrue(install.fallbackUsed)
    }

    @Test
    fun primaryAndFallbackBothFail_requiredFails_incompatible_failed() {
        val primary = target("primary")
        val fallback = target("fallback")
        val req = AnyOfRequirement(
            id = "group",
            criticality = Criticality.REQUIRED,
            candidates = listOf(primary, fallback)
        )
        val c = contract(req)
        val records = listOf(
            record(primary, resolved = false, installed = false),
            record(fallback, resolved = false, installed = false)
        )

        val compat = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.COMPATIBILITY)
        assertEquals(CompatibilityState.INCOMPATIBLE, compat.compatibility)
        assertEquals(ReasonCode.TARGET_NOT_FOUND, compat.reasonCode)

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.FAILED, install.installation)
        assertEquals(2, install.requiredFailures.size)
    }

    @Test
    fun installationFailureDetailIncludesOriginalExceptionType() {
        val spec = target("clock")
        val c = contract(SingleTargetRequirement(spec))
        val records = listOf(
            HookTargetRecord(
                spec = spec,
                requirementId = spec.id,
                resolved = true,
                installed = false,
                failureReason = HookFailureReason.HOOK_FAILED,
                failureType = "java.lang.IllegalStateException"
            )
        )

        val install = HookEvidenceEvaluator.evaluate(
            c,
            records,
            HookEvidenceEvaluator.EvidencePhase.INSTALLATION
        )

        assertEquals(InstallOutcome.FAILED, install.installation)
        assertTrue(install.detail?.contains("clock:IllegalStateException") == true)
    }

    @Test
    fun twoRequiredAnyOfGroups_eachGroupNeedsOneSuccess_onlyOnePerGroup() {
        val c = contract(
            AnyOfRequirement(
                id = "g1",
                criticality = Criticality.REQUIRED,
                candidates = listOf(target("g1a"), target("g1b"))
            ),
            AnyOfRequirement(
                id = "g2",
                criticality = Criticality.REQUIRED,
                candidates = listOf(target("g2a"), target("g2b"))
            )
        )
        val records = listOf(
            record(target("g1a"), resolved = true, installed = true),
            record(target("g1b"), resolved = false, installed = false),
            record(target("g2a"), resolved = false, installed = false),
            record(target("g2b"), resolved = true, installed = true)
        )

        val compat = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.COMPATIBILITY)
        assertEquals(CompatibilityState.DEGRADED, compat.compatibility)
        assertEquals(2, compat.requiredInstalled)
        assertEquals(2, compat.requiredTotal)
        assertTrue(compat.fallbackUsed)
        assertEquals(0, compat.requiredFailures.size)
    }

    @Test
    fun optionalAnyOfAllFail_doesNotMakeWholeFeatureFailed() {
        val c = contract(
            SingleTargetRequirement(target("required"), criticality = Criticality.REQUIRED),
            AnyOfRequirement(
                id = "opt",
                criticality = Criticality.OPTIONAL,
                candidates = listOf(target("opt1"), target("opt2"))
            )
        )
        val records = listOf(
            record(target("required"), resolved = true, installed = true),
            record(target("opt1"), resolved = false, installed = false),
            record(target("opt2"), resolved = false, installed = false)
        )

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.DEGRADED, install.installation)
        assertEquals(1, install.requiredInstalled)
        assertEquals(1, install.requiredTotal)
        assertEquals(0, install.optionalInstalled)
        assertEquals(1, install.optionalTotal)
        assertEquals(0, install.requiredFailures.size)
        assertEquals(2, install.optionalFailures.size)
    }

    @Test
    fun sameNameDifferentParameterOverloads_areIsolated() {
        val c = contract(
            SingleTargetRequirement(
                target("int", parameterTypes = listOf(Int::class.javaPrimitiveType!!))
            ),
            SingleTargetRequirement(
                target("string", parameterTypes = listOf(String::class.java))
            )
        )
        val records = listOf(
            record(
                target("int", parameterTypes = listOf(Int::class.javaPrimitiveType!!)),
                resolved = true,
                installed = true
            ),
            record(
                target("string", parameterTypes = listOf(String::class.java)),
                resolved = false,
                installed = false
            )
        )

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.FAILED, install.installation)
        assertEquals(1, install.requiredInstalled)
        assertEquals(2, install.requiredTotal)
        assertEquals(1, install.requiredFailures.size)
    }

    @Test
    fun exactMethodIsolatedFromAllMethods() {
        val c = contract(
            SingleTargetRequirement(
                target("exact", parameterTypes = listOf(Int::class.javaPrimitiveType!!))
            ),
            SingleTargetRequirement(
                target("all", operation = HookOperation.ALL_METHODS_BY_NAME)
            )
        )
        val records = listOf(
            record(
                target("exact", parameterTypes = listOf(Int::class.javaPrimitiveType!!)),
                resolved = true,
                installed = true
            ),
            record(
                target("all", operation = HookOperation.ALL_METHODS_BY_NAME),
                resolved = false,
                installed = false
            )
        )

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.FAILED, install.installation)
        assertEquals(1, install.requiredInstalled)
        assertEquals(2, install.requiredTotal)
        assertEquals(1, install.requiredFailures.size)
    }

    @Test
    fun noArgExactMethod_isRealNoArgMethod() {
        val c = contract(
            SingleTargetRequirement(
                target("noArg", parameterTypes = emptyList())
            )
        )
        val records = listOf(
            record(
                target("noArg", parameterTypes = emptyList()),
                resolved = true,
                installed = true
            )
        )

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.INSTALLED, install.installation)
    }

    @Test
    fun allConstructorsIsolatedFromNoArgConstructor() {
        val all = target("allC", operation = HookOperation.ALL_CONSTRUCTORS, memberName = null)
        val exact = target("exactC", operation = HookOperation.EXACT_CONSTRUCTOR, memberName = null, parameterTypes = emptyList())
        val c = contract(
            SingleTargetRequirement(all),
            SingleTargetRequirement(exact)
        )
        val records = listOf(
            record(all, resolved = true, installed = true),
            record(exact, resolved = false, installed = false)
        )

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.FAILED, install.installation)
        assertEquals(1, install.requiredInstalled)
        assertEquals(2, install.requiredTotal)
        assertEquals(1, install.requiredFailures.size)
    }

    @Test
    fun regression_5de2606_requiredTotal_isByRequirement_notCandidateCount() {
        val c = contract(
            AnyOfRequirement(
                id = "clamp",
                criticality = Criticality.REQUIRED,
                candidates = listOf(
                    target("primary"),
                    target("fallback")
                )
            )
        )
        val records = listOf(
            record(target("primary"), resolved = true, installed = true),
            record(target("fallback"), resolved = true, installed = false)
        )

        val install = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(1, install.requiredInstalled)
        assertEquals(1, install.requiredTotal)
        assertFalse(install.fallbackUsed)
        assertEquals(InstallOutcome.INSTALLED, install.installation)
    }

    @Test
    fun hookInstallResult_isImmutableValueObject_andDoesNotRecompute() {
        val primary = target("primary")
        val fallback = target("fallback")
        val c = contract(
            AnyOfRequirement(
                id = "group",
                criticality = Criticality.REQUIRED,
                candidates = listOf(primary, fallback)
            )
        )
        val records = listOf(
            record(primary, resolved = true, installed = true),
            record(fallback, resolved = true, installed = false)
        )

        val result = HookEvidenceEvaluator.evaluate(c, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
        assertEquals(InstallOutcome.INSTALLED, result.installation)

        // Mutating the original records must not change the pre-computed outcome.
        val modifiedRecords = records.map { it.copy(installed = false) }
        val resultCopy = result.copy(records = modifiedRecords)

        assertEquals(InstallOutcome.INSTALLED, resultCopy.installation)
        assertEquals(1, resultCopy.requiredInstalled)
        assertEquals(1, resultCopy.requiredTotal)
    }
}
