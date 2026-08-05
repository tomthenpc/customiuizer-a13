package tv.withaibuild.customiuizer.mods.utils

internal object DeviceInfoSysfsParser {
    const val BATTERY_BUFFER_BYTES = 8 * 1024
    const val CPU_BUFFER_BYTES = 64

    internal class BatteryValues {
        var temperature: Int = 0
        var currentNow: Int = 0
        var voltageNow: Int = 0

        fun reset() {
            temperature = 0
            currentNow = 0
            voltageNow = 0
        }
    }

    private const val KEY_TEMP = "POWER_SUPPLY_TEMP"
    private const val KEY_CURRENT = "POWER_SUPPLY_CURRENT_NOW"
    private const val KEY_VOLTAGE = "POWER_SUPPLY_VOLTAGE_NOW"

    fun parseBatteryUevent(buffer: ByteArray, length: Int, output: BatteryValues): Boolean {
        if (length < 0 || length > buffer.size) return false
        output.reset()
        if (length == 0) return true

        var pos = 0
        while (pos < length) {
            while (pos < length && isSpace(buffer[pos])) pos++
            if (pos >= length) break

            val lineStart = pos
            val lineEnd = findLineEnd(buffer, length, pos)
            val eqPos = findChar(buffer, length, '=', lineStart, lineEnd)

            if (eqPos != -1) {
                val keyLen = eqPos - lineStart
                val valueStart = eqPos + 1
                val valueEnd = lineEnd
                if (keyLen == KEY_TEMP.length &&
                    matchKey(buffer, lineStart, KEY_TEMP, length)
                ) {
                    output.temperature = parseInt(buffer, valueStart, valueEnd)
                } else if (keyLen == KEY_CURRENT.length &&
                    matchKey(buffer, lineStart, KEY_CURRENT, length)
                ) {
                    output.currentNow = parseInt(buffer, valueStart, valueEnd)
                } else if (keyLen == KEY_VOLTAGE.length &&
                    matchKey(buffer, lineStart, KEY_VOLTAGE, length)
                ) {
                    output.voltageNow = parseInt(buffer, valueStart, valueEnd)
                }
            }

            pos = lineEnd
            if (pos < length && byteAt(buffer, pos) == '\r'.code) pos++
            if (pos < length && byteAt(buffer, pos) == '\n'.code) pos++
        }

        return true
    }

    fun parseCpuTemperature(buffer: ByteArray, length: Int): Int {
        if (length < 0 || length > buffer.size) return 0
        if (length == 0) return 0
        val end = findLineEnd(buffer, length, 0)
        return parseInt(buffer, 0, end)
    }

    private fun parseInt(buffer: ByteArray, start: Int, end: Int): Int {
        var pos = start
        while (pos < end && isSpace(buffer[pos])) pos++
        if (pos >= end) return 0

        val negative = when (byteAt(buffer, pos)) {
            '-'.code -> {
                pos++
                true
            }
            '+'.code -> {
                pos++
                false
            }
            else -> false
        }

        if (pos >= end) return 0

        val firstDigit = pos
        var magnitude: Long = 0
        val limit = if (negative) Int.MAX_VALUE.toLong() + 1 else Int.MAX_VALUE.toLong()

        while (pos < end) {
            val b = byteAt(buffer, pos)
            if (b < '0'.code || b > '9'.code) break
            magnitude = magnitude * 10 + (b - '0'.code)
            if (magnitude > limit) return 0
            pos++
        }

        if (pos == firstDigit) return 0

        while (pos < end && isSpace(buffer[pos])) pos++
        if (pos != end) return 0

        val result = if (negative) -magnitude else magnitude
        return result.toInt()
    }

    private fun matchKey(buffer: ByteArray, start: Int, key: String, length: Int): Boolean {
        if (start + key.length > length) return false
        for (i in key.indices) {
            if (byteAt(buffer, start + i) != key[i].code) return false
        }
        return true
    }

    private fun findLineEnd(buffer: ByteArray, length: Int, start: Int): Int {
        var pos = start
        while (pos < length) {
            val b = byteAt(buffer, pos)
            if (b == '\r'.code || b == '\n'.code) break
            pos++
        }
        return pos
    }

    private fun findChar(buffer: ByteArray, length: Int, ch: Char, start: Int, end: Int): Int {
        var pos = start
        while (pos < end) {
            if (byteAt(buffer, pos) == ch.code) return pos
            pos++
        }
        return -1
    }

    private fun isSpace(b: Byte): Boolean {
        val v = byteAt(b)
        return v == ' '.code || v == '\t'.code
    }

    private fun byteAt(buffer: ByteArray, pos: Int): Int = buffer[pos].toInt() and 0xFF
    private fun byteAt(b: Byte): Int = b.toInt() and 0xFF
}
