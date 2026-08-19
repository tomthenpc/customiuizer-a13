package tv.withaibuild.customiuizer.utils

/**
 * Maps A14 USB default modes onto the A13 [system_defaultusb] string functions.
 * A13 continues to use [UsbManager.setCurrentFunction]; A14 HAL bitmasks are not used.
 */
object UsbDefaultFunctionMapper {

    /**
     * Returns the A13 USB function name to apply, or null when the ROM default
     * (charging / follow-system) must be left alone.
     */
    @JvmStatic
    fun toA13Function(raw: String?): String? {
        return when (raw) {
            null, "", "none", "0", "1", "charging" -> null
            "2" -> "mtp"
            "3" -> "ptp"
            "mtp", "ptp", "rndis", "midi" -> raw
            else -> raw
        }
    }
}
