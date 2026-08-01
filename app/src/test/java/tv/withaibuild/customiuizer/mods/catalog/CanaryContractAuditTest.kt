package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.AnyOfRequirement
import tv.withaibuild.customiuizer.mods.utils.HookEvidenceEvaluator

class CanaryContractAuditTest {

    private val allCanaries = listOf(
        CanaryContracts.packagePermissions,
        CanaryContracts.autoBrightnessRange,
        CanaryContracts.muffledVibration,
        CanaryContracts.statusBarClockTweak,
        CanaryContracts.noMoreIcon,
        CanaryContracts.batteryIndicator,
        CanaryContracts.noClockHide,
        CanaryContracts.noWidgetOnly
    )

    @Test
    fun statusBarClockTweakContractHasAuditedTargets() {
        val contract = CanaryContracts.statusBarClockTweak
        val expected = setOf(
            "MiuiStatusBarClockController.constructors",
            "MiuiStatusBarClockController.fireTimeChange",
            "MiuiClock.constructors",
            "MiuiClock.updateTime",
            "MiuiClock.setClockVisibility",
            "MiuiPhoneStatusBarView.onAttachedToWindow"
        )
        assertEquals(expected, contract.allTargets.map { it.id }.toSet())
        assertTrue(contract.requirements.none { it is AnyOfRequirement })
    }

    @Test
    fun noMoreIconContractHasAuditedTarget() {
        val contract = CanaryContracts.noMoreIcon
        val expected = setOf("NotificationIconAreaController.setIconsVisibility")
        assertEquals(expected, contract.allTargets.map { it.id }.toSet())
    }

    @Test
    fun batteryIndicatorContractHasAuditedTargets() {
        val contract = CanaryContracts.batteryIndicator
        val expected = setOf(
            "CentralSurfacesImpl.createAndAddWindows",
            "CentralSurfacesImpl.setPanelExpanded",
            "CentralSurfacesImpl.setQsExpanded",
            "CentralSurfacesImpl.updateIsKeyguard",
            "NotificationIconAreaController.onDarkChanged",
            "MiuiBatteryControllerImpl.fireBatteryLevelChanged",
            "BatteryControllerImpl.firePowerSaveChanged"
        )
        assertEquals(expected, contract.allTargets.map { it.id }.toSet())
        assertTrue(contract.requirements.none { it is AnyOfRequirement })
    }

    @Test
    fun autoBrightnessRangeHasTwoAtomicVariants() {
        val contract = CanaryContracts.autoBrightnessRange
        assertEquals(2, contract.variants.size)

        val abc = contract.variants[0]
        assertEquals("automatic_brightness_controller", abc.id)
        assertEquals(
            setOf(
                "AutomaticBrightnessController.clampScreenBrightness",
                "AutomaticBrightnessController.constructors"
            ),
            abc.allTargets.map { it.id }.toSet()
        )

        val dpc = contract.variants[1]
        assertEquals("display_power_controller", dpc.id)
        assertEquals(
            setOf(
                "DisplayPowerController.clampScreenBrightness",
                "DisplayPowerController.constructors"
            ),
            dpc.allTargets.map { it.id }.toSet()
        )

        assertTrue(contract.allTargets.isNotEmpty())
    }

    @Test
    fun allCanaryTargetIdsAreUniqueWithinContract() {
        for (contract in allCanaries) {
            val ids = contract.allTargets.map { it.id }
            assertEquals(
                "contract ${contract.featureId} has duplicate target ids",
                ids.toSet().size,
                ids.size
            )
        }
    }

    @Test
    fun allCanaryVariantIdsAreUniqueWithinContract() {
        for (contract in allCanaries) {
            val variantIds = contract.variants.map { it.id }
            assertEquals(
                "contract ${contract.featureId} has duplicate variant ids",
                variantIds.toSet().size,
                variantIds.size
            )
        }
    }

    @Test
    fun allCanariesHaveAtLeastOneVariantAndOneRequirement() {
        for (contract in allCanaries) {
            assertTrue("${contract.featureId} has no variants", contract.variants.isNotEmpty())
            for (variant in contract.variants) {
                assertNotNull(variant)
                assertTrue(
                    "${contract.featureId} variant ${variant.id} has no requirements",
                    variant.requirements.isNotEmpty()
                )
            }
        }
    }

    @Test
    fun singleVariantCanaryEvaluatesAgainstDefaultPrimary() {
        val contract = CanaryContracts.noClockHide
        assertEquals(1, contract.variants.size)
        assertEquals("primary", contract.variants.single().id)
        val result = HookEvidenceEvaluator.evaluate(
            contract.variants.single(),
            emptyList(),
            HookEvidenceEvaluator.EvidencePhase.COMPATIBILITY
        )
        assertEquals(CompatibilityState.INCOMPATIBLE, result.compatibility)
    }
}
