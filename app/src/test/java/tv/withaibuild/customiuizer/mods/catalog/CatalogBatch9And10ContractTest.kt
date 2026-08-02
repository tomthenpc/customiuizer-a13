package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.utils.AnyOfRequirement
import tv.withaibuild.customiuizer.mods.utils.Criticality
import tv.withaibuild.customiuizer.mods.utils.SingleTargetRequirement

/**
 * Contract parity invariants for catalog expansion batches 9 and 10.
 *
 * These tests verify that every production hook call in the legacy installer
 * maps to a contract target with the correct criticality, and that contract
 * metadata (feature id, diagnostic id, target ids) is internally consistent.
 */
class CatalogBatch9And10ContractTest {

    private fun contractFor(id: String) =
        FeatureCatalog.specs().find { it.id == id }?.contract
            ?: throw AssertionError("No contract for feature $id")

    private fun requiredIds(contract: tv.withaibuild.customiuizer.mods.utils.HookTargetContract): Set<String> =
        contract.requirements
            .filter { it.criticality == Criticality.REQUIRED }
            .flatMap { req ->
                when (req) {
                    is SingleTargetRequirement -> listOf(req.target.id)
                    is AnyOfRequirement -> req.candidates.map { it.id }
                }
            }
            .toSet()

    private fun optionalIds(contract: tv.withaibuild.customiuizer.mods.utils.HookTargetContract): Set<String> =
        contract.requirements
            .filter { it.criticality == Criticality.OPTIONAL }
            .flatMap { req ->
                when (req) {
                    is SingleTargetRequirement -> listOf(req.target.id)
                    is AnyOfRequirement -> req.candidates.map { it.id }
                }
            }
            .toSet()

    @Test
    fun batch9ContractsMatchProductionHooks() {
        // EnhancedSecurityHook installs 4 silent hooks on PhoneWindowManager.
        val enhancedSecurity = contractFor("enhancedSecurity")
        assertEquals(
            setOf(
                "PhoneWindowManager.interceptPowerKeyDown",
                "PhoneWindowManager.powerLongPress",
                "PhoneWindowManager.showGlobalActions",
                "PhoneWindowManager.showGlobalActionsInternal"
            ),
            optionalIds(enhancedSecurity)
        )
        assertTrue("enhancedSecurity has no required targets", requiredIds(enhancedSecurity).isEmpty())

        // AppLockHook and SkipAppLockHook each install one hard hook.
        val appLock = contractFor("appLock")
        assertEquals(
            setOf("SecurityManagerService.removeAccessControlPassLocked"),
            requiredIds(appLock)
        )
        assertTrue("appLock has no optional targets", optionalIds(appLock).isEmpty())

        val skipAppLock = contractFor("skipAppLock")
        assertEquals(
            setOf("AccessController.skipActivity"),
            requiredIds(skipAppLock)
        )
        assertTrue("skipAppLock has no optional targets", optionalIds(skipAppLock).isEmpty())

        // NoCallInterruptionHook installs 3 hard hooks.
        val noCallInterruption = contractFor("noCallInterruption")
        assertEquals(
            setOf(
                "AudioService.requestAudioFocus",
                "TelephonyRegistry.notifyCallState",
                "TelephonyRegistry.notifyCallStateForPhoneId"
            ),
            requiredIds(noCallInterruption)
        )
        assertTrue("noCallInterruption has no optional targets", optionalIds(noCallInterruption).isEmpty())
    }

    @Test
    fun noSignatureVerifyContainsExactlyEightRequiredTargets() {
        val contract = contractFor("noSignatureVerify")
        val expected = setOf(
            "SigningDetails.checkCapability",
            "StrictJarVerifier.constructors",
            "StrictJarVerifier.verifyMessageDigest",
            "StrictJarVerifier.verify",
            "PackageManagerServiceUtils.verifySignatures",
            "InstallPackageHelper.doesSignatureMatchForPermissions",
            "InstallPackageHelper.cannotInstallWithBadPermissionGroups",
            "PermissionManagerServiceImpl.shouldGrantPermissionBySignature"
        )
        assertEquals(expected, requiredIds(contract))
        assertTrue("noSignatureVerify has no optional targets", optionalIds(contract).isEmpty())
        assertEquals("noSignatureVerify target count", 8, contract.allTargets.size)
    }

    @Test
    fun removeSecureThreeTargetsAllRequired() {
        val contract = contractFor("removeSecure")
        val expected = setOf(
            "WindowState.isSecureLocked",
            "WindowSurfaceController.setSecure",
            "WindowSurfaceController.constructors"
        )
        assertEquals(expected, requiredIds(contract))
        assertTrue("removeSecure has no optional targets", optionalIds(contract).isEmpty())
    }

    @Test
    fun noDarkForceOnlySetForceDarkIsOptional() {
        val contract = contractFor("noDarkForce")
        assertEquals(
            setOf("UiModeManagerService.setForceDark"),
            optionalIds(contract)
        )
        assertEquals(
            setOf(
                "SecurityManagerService.getAppDarkModeForUser",
                "DarkModeAppSettingsInfo.getOverrideEnableValue"
            ),
            requiredIds(contract)
        )
    }

    @Test
    fun stickyFloatingWindowsMatchesProductionTarget() {
        val contract = contractFor("stickyFloatingWindows")
        assertEquals(
            setOf(
                "ActivityStarterInjector.modifyLaunchActivityOptionIfNeed",
                "ActivityTaskSupervisor.startActivityFromRecents",
                "MiuiFreeFormGestureController\$FreeFormReceiver.onReceive",
                "MiuiFreeFormGestureController.notifyFullScreenWidnowModeStart",
                "ActivityTaskManagerService.launchSmallFreeFormWindow",
                "ActivityTaskManagerService.onSystemReady",
                "ActivityTaskManagerService.resizeTask"
            ),
            requiredIds(contract)
        )
        assertTrue("stickyFloatingWindows has no optional targets", optionalIds(contract).isEmpty())
    }

    @Test
    fun batch9And10ContractFeatureIdsMatchSpecIds() {
        val batchIds = setOf(
            "enhancedSecurity", "appLock", "skipAppLock", "noCallInterruption",
            "removeSecure", "noSignatureVerify", "noDarkForce", "stickyFloatingWindows"
        )
        for (id in batchIds) {
            val spec = FeatureCatalog.specs().find { it.id == id }
                ?: throw AssertionError("Missing spec for $id")
            assertTrue("$id contract must not be null", spec.contract != null)
            assertEquals("$id contract.featureId must match spec.id", id, spec.contract!!.featureId)
        }
    }

    @Test
    fun batch9And10TargetIdsAreUniqueWithinEachContract() {
        val batchIds = setOf(
            "enhancedSecurity", "appLock", "skipAppLock", "noCallInterruption",
            "removeSecure", "noSignatureVerify", "noDarkForce", "stickyFloatingWindows"
        )
        for (id in batchIds) {
            val contract = contractFor(id)
            val targetIds = contract.allTargets.map { it.id }
            assertEquals(
                "$id has duplicate target ids: ${targetIds.groupBy { it }.filter { it.value.size > 1 }.keys}",
                targetIds.size,
                targetIds.toSet().size
            )
        }
    }

    @Test
    fun batch9And10DiagnosticIdsAreUnique() {
        val batchIds = setOf(
            "enhancedSecurity", "appLock", "skipAppLock", "noCallInterruption",
            "removeSecure", "noSignatureVerify", "noDarkForce", "stickyFloatingWindows"
        )
        val specs = FeatureCatalog.specs().filter { it.id in batchIds }
        val diagnosticIds = specs.map { it.diagnosticId }
        assertEquals(
            "Batch 9/10 diagnostic ids must be unique",
            diagnosticIds.size,
            diagnosticIds.toSet().size
        )

        // Every diagnostic id must be declared in DiagnosticIds.
        val declared = DiagnosticIds::class.java.declaredFields
            .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.get(null) as String }
            .toSet()
        for (id in diagnosticIds) {
            assertTrue("Diagnostic id $id not declared in DiagnosticIds", id in declared)
        }
    }
}
