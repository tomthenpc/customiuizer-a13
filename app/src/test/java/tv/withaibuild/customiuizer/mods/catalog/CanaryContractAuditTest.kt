package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.AnyOfRequirement

class CanaryContractAuditTest {

    /**
     * Fixture of the three SystemUI Canary contracts and their audited target ids.
     * The ids must stay in sync with the source-level audit in
     * `build/compat-audit/a13-a14-systemui-canary.md`.
     */
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
    fun allCanaryTargetIdsAreUniqueWithinContract() {
        val contracts = listOf(
            CanaryContracts.statusBarClockTweak,
            CanaryContracts.noMoreIcon,
            CanaryContracts.batteryIndicator
        )
        for (contract in contracts) {
            val ids = contract.allTargets.map { it.id }
            assertEquals(
                "contract ${contract.featureId} has duplicate target ids",
                ids.toSet().size,
                ids.size
            )
        }
    }
}
