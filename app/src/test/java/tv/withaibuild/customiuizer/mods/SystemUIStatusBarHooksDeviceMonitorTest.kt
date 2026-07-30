package tv.withaibuild.customiuizer.mods

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.DeviceInfoMonitor
import tv.withaibuild.customiuizer.utils.PrefMap
import java.util.Locale
import java.util.Properties

class SystemUIStatusBarHooksDeviceMonitorTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ROOT)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    private fun p(key: String): String =
        if (key.startsWith("pref_key_")) key else "pref_key_$key"

    private fun prefs(vararg entries: Pair<String, Any?>): PrefMap<String, Any> {
        val map = PrefMap<String, Any>()
        for ((k, v) in entries) if (v != null) map[p(k)] = v
        return map
    }

    @Test
    fun snapshotReadsAllDynamicValues() {
        val map = prefs(
            "system_statusbar_batterytempandcurrent_incharge" to true,
            "system_statusbar_batterytempandcurrent_content" to "4",
            "system_statusbar_batterytempandcurrent_temp_decimal" to true,
            "system_statusbar_batterytempandcurrent_fixcurrentratio" to true,
            "system_statusbar_batterytempandcurrent_positive" to true,
            "system_statusbar_batterytempandcurrent_singlerow" to true,
            "system_statusbar_batterytempandcurrent_reverseorder" to true,
            "system_statusbar_batterytempandcurrent_hideunit" to "2",
            "system_statusbar_showdevicetemperature_content" to "2",
            "system_statusbar_showdevicetemperature_hideunit" to true,
            "system_statusbar_showdevicetemperature_singlerow" to true,
            "system_statusbar_showdevicetemperature_reverseorder" to true
        )

        val snap = DeviceInfoMonitor.readSnapshot(map)

        assertTrue(snap.batteryInCharge)
        assertEquals(4, snap.batteryContentOpt)
        assertTrue(snap.batteryTempDecimal)
        assertTrue(snap.batteryFixCurrentRatio)
        assertTrue(snap.batteryPositive)
        assertTrue(snap.batterySingleRow)
        assertTrue(snap.batteryReverseOrder)
        assertEquals(2, snap.batteryHideUnit)
        assertEquals(2, snap.deviceTempContentOpt)
        assertTrue(snap.deviceTempHideUnit)
        assertTrue(snap.deviceTempSingleRow)
        assertTrue(snap.deviceTempReverseOrder)
    }

    @Test
    fun snapshotDoesNotIncludeFixedSlotSettings() {
        val map = prefs(
            "system_statusbar_batterytempandcurrent" to true,
            "system_statusbar_showdevicetemperature" to true,
            "system_statusbar_batterytempandcurrent_atright" to true,
            "system_statusbar_showdevicetemperature_atright" to true,
            "system_statusbar_batterytempandcurrent_fontsize" to 30,
            "system_statusbar_batterytempandcurrent_leftmargin" to 4
        )

        val snap = DeviceInfoMonitor.readSnapshot(map)

        // Slot placement and style remain fixed, while master toggles join the controller snapshot.
        assertEquals(1, snap.batteryContentOpt)
    }

    @Test
    fun snapshotContainsMasterTogglesForTickerShutdown() {
        val map = prefs(
            "system_statusbar_batterytempandcurrent" to true,
            "system_statusbar_showdevicetemperature" to true
        )

        val snap = DeviceInfoMonitor.readSnapshot(map)

        assertTrue(snap.showBatteryDetail)
        assertTrue(snap.showDeviceTemp)
        assertTrue(snap.enabled)
    }

    @Test
    fun snapshotUsesDefaultsForMissingAndMalformedValues() {
        val map = prefs(
            "system_statusbar_batterytempandcurrent_incharge" to "notABoolean",
            "system_statusbar_batterytempandcurrent_content" to "bad",
            "system_statusbar_batterytempandcurrent_hideunit" to "alsoBad"
        )

        val snap = DeviceInfoMonitor.readSnapshot(map)

        assertFalse(snap.batteryInCharge)
        assertEquals(1, snap.batteryContentOpt)
        assertEquals(0, snap.batteryHideUnit)
    }

    @Test
    fun snapshotIsConsistentAndImmutablePerTick() {
        val map = prefs(
            "system_statusbar_batterytempandcurrent_content" to "1",
            "system_statusbar_batterytempandcurrent_hideunit" to "0"
        )

        val snap = DeviceInfoMonitor.readSnapshot(map)

        // Mutating the map after the snapshot is taken must not change the snapshot.
        map[p("system_statusbar_batterytempandcurrent_content")] = 5
        map[p("system_statusbar_batterytempandcurrent_hideunit")] = 3

        assertEquals(1, snap.batteryContentOpt)
        assertEquals(0, snap.batteryHideUnit)
    }

    @Test
    fun nextTickSeesUpdatedPreferences() {
        val map = prefs(
            "system_statusbar_batterytempandcurrent_content" to "1",
            "system_statusbar_batterytempandcurrent_hideunit" to "0"
        )

        val first = DeviceInfoMonitor.readSnapshot(map)
        assertEquals(1, first.batteryContentOpt)

        map[p("system_statusbar_batterytempandcurrent_content")] = 4
        val second = DeviceInfoMonitor.readSnapshot(map)
        assertEquals(4, second.batteryContentOpt)
    }

    @Test
    fun buildBatteryInfoUsesSnapshotConsistently() {
        val snap = DeviceInfoMonitor.Snapshot(
            batteryInCharge = false,
            batteryContentOpt = 1,
            batteryTempDecimal = false,
            batteryFixCurrentRatio = false,
            batteryPositive = false,
            batterySingleRow = true,
            batteryReverseOrder = false,
            batteryHideUnit = 0,
            deviceTempContentOpt = 1,
            deviceTempHideUnit = false,
            deviceTempSingleRow = false,
            deviceTempReverseOrder = false
        )

        val props = Properties().apply {
            setProperty("POWER_SUPPLY_TEMP", "350")
            setProperty("POWER_SUPPLY_CURRENT_NOW", "-1250000")
            setProperty("POWER_SUPPLY_VOLTAGE_NOW", "4200000")
        }

        val text = DeviceInfoMonitor.buildBatteryInfo(snap, props)

        // Temp = 35 (350/10, no decimal because 350 % 10 == 0)
        // Current = -1 * round(-1250000 / 1000) = 1250 mA -> 1.25 A
        assertEquals("35\u2103 1.25A", text)
    }

    @Test
    fun buildBatteryInfoReactsToSnapshotChanges() {
        val snap = DeviceInfoMonitor.Snapshot(
            batteryInCharge = false,
            batteryContentOpt = 1,
            batteryTempDecimal = false,
            batteryFixCurrentRatio = false,
            batteryPositive = true,
            batterySingleRow = true,
            batteryReverseOrder = false,
            batteryHideUnit = 0,
            deviceTempContentOpt = 1,
            deviceTempHideUnit = false,
            deviceTempSingleRow = false,
            deviceTempReverseOrder = false
        )

        val props = Properties().apply {
            setProperty("POWER_SUPPLY_TEMP", "351")
            setProperty("POWER_SUPPLY_CURRENT_NOW", "-500000")
            setProperty("POWER_SUPPLY_VOLTAGE_NOW", "4200000")
        }

        val text = DeviceInfoMonitor.buildBatteryInfo(snap, props)

        // positive => raw current made absolute; 500 mA displayed as 500 mA.
        // temp 351 -> 35.1 (not divisible by 10)
        assertEquals("35.1\u2103 500mA", text)
    }

    @Test
    fun buildBatteryInfoHandlesMissingSysfsKeys() {
        val snap = DeviceInfoMonitor.Snapshot(
            batteryInCharge = false,
            batteryContentOpt = 1,
            batteryTempDecimal = false,
            batteryFixCurrentRatio = false,
            batteryPositive = false,
            batterySingleRow = true,
            batteryReverseOrder = false,
            batteryHideUnit = 0,
            deviceTempContentOpt = 1,
            deviceTempHideUnit = false,
            deviceTempSingleRow = false,
            deviceTempReverseOrder = false
        )

        val props = Properties()

        val text = DeviceInfoMonitor.buildBatteryInfo(snap, props)

        // Missing values fall back to 0 and produce a harmless string instead of crashing.
        assertEquals("0\u2103 0mA", text)
    }

    @Test
    fun buildDeviceInfoDualTemperatureReadsBoth() {
        val snap = DeviceInfoMonitor.Snapshot(
            batteryInCharge = false,
            batteryContentOpt = 1,
            batteryTempDecimal = false,
            batteryFixCurrentRatio = false,
            batteryPositive = false,
            batterySingleRow = false,
            batteryReverseOrder = false,
            batteryHideUnit = 0,
            deviceTempContentOpt = 1,
            deviceTempHideUnit = false,
            deviceTempSingleRow = true,
            deviceTempReverseOrder = true
        )

        val text = DeviceInfoMonitor.buildDeviceInfo(snap, "360", "45000")

        // reverse order with single row: CPU 45.0℃ before battery 36.0℃
        assertEquals("45.0\u2103 36.0\u2103", text)
    }

    @Test
    fun buildDeviceInfoBatteryOnlyDoesNotNeedCpu() {
        val snap = DeviceInfoMonitor.Snapshot(
            batteryInCharge = false,
            batteryContentOpt = 1,
            batteryTempDecimal = false,
            batteryFixCurrentRatio = false,
            batteryPositive = false,
            batterySingleRow = false,
            batteryReverseOrder = false,
            batteryHideUnit = 0,
            deviceTempContentOpt = 2,
            deviceTempHideUnit = false,
            deviceTempSingleRow = true,
            deviceTempReverseOrder = false
        )

        val text = DeviceInfoMonitor.buildDeviceInfo(snap, "360", null)

        // content=2: only battery temperature, CPU may be null
        assertEquals("36.0\u2103", text)
    }

    @Test
    fun buildDeviceInfoCpuOnlyDoesNotNeedBattery() {
        val snap = DeviceInfoMonitor.Snapshot(
            batteryInCharge = false,
            batteryContentOpt = 1,
            batteryTempDecimal = false,
            batteryFixCurrentRatio = false,
            batteryPositive = false,
            batterySingleRow = false,
            batteryReverseOrder = false,
            batteryHideUnit = 0,
            deviceTempContentOpt = 3,
            deviceTempHideUnit = false,
            deviceTempSingleRow = false,
            deviceTempReverseOrder = false
        )

        val text = DeviceInfoMonitor.buildDeviceInfo(snap, null, "45000")

        // content=3: only CPU temperature, battery may be null
        assertEquals("45.0\u2103", text)
    }

    @Test
    fun buildDeviceInfoIllegalContentFallsBackToDual() {
        val snap = DeviceInfoMonitor.Snapshot(
            batteryInCharge = false,
            batteryContentOpt = 1,
            batteryTempDecimal = false,
            batteryFixCurrentRatio = false,
            batteryPositive = false,
            batterySingleRow = false,
            batteryReverseOrder = false,
            batteryHideUnit = 0,
            deviceTempContentOpt = 42,
            deviceTempHideUnit = false,
            deviceTempSingleRow = true,
            deviceTempReverseOrder = false
        )

        val text = DeviceInfoMonitor.buildDeviceInfo(snap, "360", "45000")

        // Illegal content value falls back to mode 1 (battery + CPU)
        assertEquals("36.0\u2103 45.0\u2103", text)
    }
}
