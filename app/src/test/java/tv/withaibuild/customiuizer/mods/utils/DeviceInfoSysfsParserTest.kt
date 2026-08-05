package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoSysfsParserTest {

    private val batteryValues = DeviceInfoSysfsParser.BatteryValues()

    private fun ascii(input: String): ByteArray = input.toByteArray(Charsets.US_ASCII)

    @Test
    fun parseBatteryUeventExtractsAllThreeKeys() {
        val buffer = ascii(
            "POWER_SUPPLY_STATUS=Charging\n" +
            "POWER_SUPPLY_TEMP=350\n" +
            "POWER_SUPPLY_CURRENT_NOW=-1250000\n" +
            "POWER_SUPPLY_VOLTAGE_NOW=4200000\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(350, batteryValues.temperature)
        assertEquals(-1_250_000, batteryValues.currentNow)
        assertEquals(4_200_000, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventAcceptsArbitraryKeyOrder() {
        val buffer = ascii(
            "POWER_SUPPLY_VOLTAGE_NOW=4100000\n" +
            "POWER_SUPPLY_STATUS=Charging\n" +
            "POWER_SUPPLY_TEMP=340\n" +
            "POWER_SUPPLY_CURRENT_NOW=500000\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(340, batteryValues.temperature)
        assertEquals(500_000, batteryValues.currentNow)
        assertEquals(4_100_000, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventIgnoresUnknownKeys() {
        val buffer = ascii(
            "POWER_SUPPLY_NAME=battery\n" +
            "POWER_SUPPLY_TECHNOLOGY=Li-poly\n" +
            "POWER_SUPPLY_TEMP=350\n" +
            "POWER_SUPPLY_PRESENT=1\n" +
            "POWER_SUPPLY_CURRENT_NOW=-1000\n" +
            "POWER_SUPPLY_VOLTAGE_NOW=3000000\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(350, batteryValues.temperature)
        assertEquals(-1_000, batteryValues.currentNow)
        assertEquals(3_000_000, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventSupportsCrLf() {
        val buffer = ascii(
            "POWER_SUPPLY_STATUS=Charging\r\n" +
            "POWER_SUPPLY_TEMP=123\r\n" +
            "POWER_SUPPLY_CURRENT_NOW=-456\r\n" +
            "POWER_SUPPLY_VOLTAGE_NOW=789\r\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(123, batteryValues.temperature)
        assertEquals(-456, batteryValues.currentNow)
        assertEquals(789, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventSupportsNegativeCurrent() {
        val buffer = ascii(
            "POWER_SUPPLY_TEMP=350\n" +
            "POWER_SUPPLY_CURRENT_NOW=-987654\n" +
            "POWER_SUPPLY_VOLTAGE_NOW=4000000\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(-987_654, batteryValues.currentNow)
    }

    @Test
    fun parseBatteryUeventSupportsLeadingPlusSign() {
        val buffer = ascii(
            "POWER_SUPPLY_TEMP=+350\n" +
            "POWER_SUPPLY_CURRENT_NOW=+1250000\n" +
            "POWER_SUPPLY_VOLTAGE_NOW=+4200000\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(350, batteryValues.temperature)
        assertEquals(1_250_000, batteryValues.currentNow)
        assertEquals(4_200_000, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventIgnoresSpacesAndTabsAroundValue() {
        val buffer = ascii(
            "POWER_SUPPLY_TEMP=\t 350 \t\n" +
            "POWER_SUPPLY_CURRENT_NOW=\t -1000 \n" +
            "POWER_SUPPLY_VOLTAGE_NOW= 4200000 \t\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(350, batteryValues.temperature)
        assertEquals(-1_000, batteryValues.currentNow)
        assertEquals(4_200_000, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventReturnsZeroForIllegalNumbers() {
        val buffer = ascii(
            "POWER_SUPPLY_TEMP=not_a_number\n" +
            "POWER_SUPPLY_CURRENT_NOW=--123\n" +
            "POWER_SUPPLY_VOLTAGE_NOW=12abc\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(0, batteryValues.temperature)
        assertEquals(0, batteryValues.currentNow)
        assertEquals(0, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventReturnsZeroForPositiveOverflow() {
        val buffer = ascii(
            "POWER_SUPPLY_TEMP=2147483648\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(0, batteryValues.temperature)
    }

    @Test
    fun parseBatteryUeventReturnsZeroForNegativeOverflow() {
        val buffer = ascii(
            "POWER_SUPPLY_CURRENT_NOW=-2147483649\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(0, batteryValues.currentNow)
    }

    @Test
    fun parseBatteryUeventKeepsMissingKeysAtZero() {
        val buffer = ascii(
            "POWER_SUPPLY_STATUS=Charging\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(0, batteryValues.temperature)
        assertEquals(0, batteryValues.currentNow)
        assertEquals(0, batteryValues.voltageNow)
    }

    @Test
    fun parseBatteryUeventLastDuplicateWins() {
        val buffer = ascii(
            "POWER_SUPPLY_TEMP=100\n" +
            "POWER_SUPPLY_TEMP=200\n" +
            "POWER_SUPPLY_TEMP=300\n"
        )
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(300, batteryValues.temperature)
    }

    @Test
    fun parseBatteryUeventHonorsLengthParameter() {
        val buffer = ascii(
            "POWER_SUPPLY_TEMP=111\n" +
            "POWER_SUPPLY_TEMP=999\n"
        )
        // Only parse the first line.
        val firstLineEnd = buffer.indexOf('\n'.code.toByte()) + 1
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, firstLineEnd, batteryValues))
        assertEquals(111, batteryValues.temperature)
    }

    @Test
    fun parseBatteryUeventRejectsNegativeLength() {
        assertFalse(DeviceInfoSysfsParser.parseBatteryUevent(ascii(""), -1, batteryValues))
    }

    @Test
    fun parseBatteryUeventRejectsLengthExceedingBuffer() {
        val buffer = ascii("POWER_SUPPLY_TEMP=350\n")
        assertFalse(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size + 1, batteryValues))
    }

    @Test
    fun parseBatteryUeventResetsOutputOnEachParse() {
        val buffer1 = ascii(
            "POWER_SUPPLY_TEMP=350\n" +
            "POWER_SUPPLY_CURRENT_NOW=-1250000\n" +
            "POWER_SUPPLY_VOLTAGE_NOW=4200000\n"
        )
        val buffer2 = ascii(
            "POWER_SUPPLY_CURRENT_NOW=500\n"
        )

        DeviceInfoSysfsParser.parseBatteryUevent(buffer1, buffer1.size, batteryValues)
        assertEquals(350, batteryValues.temperature)

        DeviceInfoSysfsParser.parseBatteryUevent(buffer2, buffer2.size, batteryValues)
        assertEquals(0, batteryValues.temperature)
        assertEquals(500, batteryValues.currentNow)
        assertEquals(0, batteryValues.voltageNow)
    }

    @Test
    fun parseCpuTemperaturePositive() {
        val buffer = ascii("45000\n")
        assertEquals(45_000, DeviceInfoSysfsParser.parseCpuTemperature(buffer, buffer.size))
    }

    @Test
    fun parseCpuTemperatureNegative() {
        val buffer = ascii("-5000\n")
        assertEquals(-5_000, DeviceInfoSysfsParser.parseCpuTemperature(buffer, buffer.size))
    }

    @Test
    fun parseCpuTemperatureCrLf() {
        val buffer = ascii("12345\r\n")
        assertEquals(12_345, DeviceInfoSysfsParser.parseCpuTemperature(buffer, buffer.size))
    }

    @Test
    fun parseCpuTemperatureIllegalOrEmptyInputIsZero() {
        assertEquals(0, DeviceInfoSysfsParser.parseCpuTemperature(ascii(""), 0))
        assertEquals(0, DeviceInfoSysfsParser.parseCpuTemperature(ascii("abc"), 3))
        assertEquals(0, DeviceInfoSysfsParser.parseCpuTemperature(ascii("--1"), 3))
        assertEquals(0, DeviceInfoSysfsParser.parseCpuTemperature(ascii("12 34"), 5))
    }

    @Test
    fun parseCpuTemperatureMaxValue() {
        val buffer = ascii("2147483647")
        assertEquals(Int.MAX_VALUE, DeviceInfoSysfsParser.parseCpuTemperature(buffer, buffer.size))
    }

    @Test
    fun parseCpuTemperatureMinValue() {
        val buffer = ascii("-2147483648")
        assertEquals(Int.MIN_VALUE, DeviceInfoSysfsParser.parseCpuTemperature(buffer, buffer.size))
    }

    @Test
    fun parseBatteryUeventSupportsIntMaxValue() {
        val buffer = ascii("POWER_SUPPLY_TEMP=2147483647\n")
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(Int.MAX_VALUE, batteryValues.temperature)
    }

    @Test
    fun parseBatteryUeventSupportsIntMinValue() {
        val buffer = ascii("POWER_SUPPLY_CURRENT_NOW=-2147483648\n")
        assertTrue(DeviceInfoSysfsParser.parseBatteryUevent(buffer, buffer.size, batteryValues))
        assertEquals(Int.MIN_VALUE, batteryValues.currentNow)
    }
}
