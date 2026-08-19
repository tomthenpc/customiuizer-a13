package tv.withaibuild.customiuizer.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.zip.CRC32
import tv.withaibuild.customiuizer.BuildConfig

/**
 * A13 V2 backup format (CUI2). Semantic contract matches the A14 M2 format so
 * backups stay portable; encoding is implemented here against API 33 / A13 types.
 *
 * Format:
 *   MAGIC          4 bytes  "CUI2"
 *   FORMAT_VERSION 4 bytes  big-endian Int32 = 1
 *   APP_REVISION   4 bytes  big-endian Int32 = BuildConfig.VERSION_CODE
 *   ENTRY_COUNT    4 bytes  big-endian Int32
 *   TYPED_ENTRIES  variable
 *   CRC32          4 bytes  big-endian UInt32 over [MAGIC .. payload-end]
 */
object BackupFormatV2 {

    const val MAGIC = 0x43_55_49_32 // "CUI2"

    const val FORMAT_VERSION = 1

    const val TYPE_BOOLEAN = 1
    const val TYPE_INT = 2
    const val TYPE_LONG = 3
    const val TYPE_FLOAT = 4
    const val TYPE_STRING = 5
    const val TYPE_STRING_SET = 6

    const val MAX_FILE_SIZE = 2L * 1024 * 1024
    const val MAX_ENTRY_COUNT = 4096
    const val MAX_KEY_BYTES = 128
    const val MAX_STRING_BYTES = 65535
    const val MAX_SET_ITEMS = 1024

    const val MIN_FILE_LENGTH = 20

    val LEGACY_MAGIC = byteArrayOf(0xAC.toByte(), 0xED.toByte(), 0x00, 0x05)

    class BackupFormatException(message: String) : Exception(message)

    @JvmStatic
    fun encode(entries: Map<String, Any?>): ByteArray {
        if (entries.size > MAX_ENTRY_COUNT) {
            throw BackupFormatException("Entry count ${entries.size} exceeds $MAX_ENTRY_COUNT")
        }

        var totalSize = 16L
        for (key in entries.keys) {
            val keyBytes = encodeStrictUtf8(key)
            if (keyBytes.size > MAX_KEY_BYTES) {
                throw BackupFormatException("Key too long: ${keyBytes.size} > $MAX_KEY_BYTES")
            }
            if (keyBytes.size > 65535) {
                throw BackupFormatException("Key length exceeds 16-bit limit")
            }
            totalSize += 2 + keyBytes.size
            totalSize += 1
            totalSize += measureValueSize(entries[key])
        }
        totalSize += 4

        if (totalSize > MAX_FILE_SIZE) {
            throw BackupFormatException("V2 encoded size $totalSize exceeds $MAX_FILE_SIZE")
        }

        val output = ByteArrayOutputStream(totalSize.toInt())
        val data = DataOutputStream(output)

        data.writeInt(MAGIC)
        data.writeInt(FORMAT_VERSION)
        data.writeInt(BuildConfig.VERSION_CODE)
        data.writeInt(entries.size)

        for (key in entries.keys.sorted()) {
            writeKey(data, key)
            writeValue(data, entries[key])
        }

        data.flush()

        val payload = output.toByteArray()
        val crc = CRC32()
        crc.update(payload, 0, payload.size)
        data.writeInt(crc.value.toInt())

        return output.toByteArray()
    }

    @JvmStatic
    fun decode(bytes: ByteArray): Map<String, Any?> {
        if (bytes.size < MIN_FILE_LENGTH) {
            throw BackupFormatException("V2 file too short: ${bytes.size}")
        }
        if (bytes.size > MAX_FILE_SIZE) {
            throw BackupFormatException("V2 file too large: ${bytes.size}")
        }

        val storedCrc = readUnsignedInt32(bytes, bytes.size - 4)
        val payload = bytes.copyOfRange(0, bytes.size - 4)

        val crc = CRC32()
        crc.update(payload, 0, payload.size)
        if (crc.value.toInt() != storedCrc) {
            throw BackupFormatException("V2 CRC mismatch")
        }

        val input = DataInputStream(ByteArrayInputStream(payload))

        val magic = input.readInt()
        if (magic != MAGIC) {
            throw BackupFormatException("V2 magic mismatch: $magic")
        }

        val version = input.readInt()
        if (version != FORMAT_VERSION) {
            throw BackupFormatException("Unsupported V2 format version: $version")
        }

        input.readInt() // app revision, metadata only

        val entryCount = input.readInt()
        if (entryCount < 0 || entryCount > MAX_ENTRY_COUNT) {
            throw BackupFormatException("V2 entry count out of bounds: $entryCount")
        }

        val result = LinkedHashMap<String, Any?>(entryCount)
        repeat(entryCount) {
            val key = readKey(input)
            val value = readValue(input)
            result[key] = value
        }

        if (input.available() != 0) {
            throw BackupFormatException("V2 trailing bytes after entries")
        }

        return result
    }

    @JvmStatic
    private fun writeKey(data: DataOutputStream, key: String) {
        val keyBytes = encodeStrictUtf8(key)
        if (keyBytes.size > MAX_KEY_BYTES) {
            throw BackupFormatException("Key too long: ${keyBytes.size} > $MAX_KEY_BYTES")
        }
        data.writeShort(keyBytes.size)
        data.write(keyBytes)
    }

    @JvmStatic
    private fun measureValueSize(value: Any?): Long {
        return when (value) {
            is Boolean -> 1L
            is Int -> 4L
            is Long -> 8L
            is Float -> 4L
            is String -> {
                val bytes = encodeStrictUtf8(value)
                if (bytes.size > MAX_STRING_BYTES) {
                    throw BackupFormatException("String too long: ${bytes.size} > $MAX_STRING_BYTES")
                }
                4L + bytes.size
            }
            is Set<*> -> {
                if (value.size > MAX_SET_ITEMS) {
                    throw BackupFormatException("StringSet too large: ${value.size} > $MAX_SET_ITEMS")
                }
                var size = 4L
                for (item in value) {
                    if (item !is String) {
                        throw BackupFormatException("StringSet contains non-String member: ${item?.javaClass}")
                    }
                    val itemBytes = encodeStrictUtf8(item)
                    if (itemBytes.size > MAX_STRING_BYTES) {
                        throw BackupFormatException("StringSet item too long: ${itemBytes.size} > $MAX_STRING_BYTES")
                    }
                    size += 4L + itemBytes.size
                }
                size
            }
            else -> throw BackupFormatException("Unsupported V2 value type: ${value?.javaClass}")
        }
    }

    @JvmStatic
    private fun writeValue(data: DataOutputStream, value: Any?) {
        when (value) {
            is Boolean -> {
                data.writeByte(TYPE_BOOLEAN)
                data.writeByte(if (value) 1 else 0)
            }
            is Int -> {
                data.writeByte(TYPE_INT)
                data.writeInt(value)
            }
            is Long -> {
                data.writeByte(TYPE_LONG)
                data.writeLong(value)
            }
            is Float -> {
                data.writeByte(TYPE_FLOAT)
                data.writeFloat(value)
            }
            is String -> {
                data.writeByte(TYPE_STRING)
                writeString(data, value)
            }
            is Set<*> -> {
                data.writeByte(TYPE_STRING_SET)
                writeStringSet(data, value)
            }
            else -> throw BackupFormatException("Unsupported V2 value type: ${value?.javaClass}")
        }
    }

    @JvmStatic
    private fun writeString(data: DataOutputStream, value: String) {
        val bytes = encodeStrictUtf8(value)
        if (bytes.size > MAX_STRING_BYTES) {
            throw BackupFormatException("String too long: ${bytes.size} > $MAX_STRING_BYTES")
        }
        data.writeInt(bytes.size)
        data.write(bytes)
    }

    @JvmStatic
    private fun writeStringSet(data: DataOutputStream, values: Set<*>) {
        if (values.size > MAX_SET_ITEMS) {
            throw BackupFormatException("StringSet too large: ${values.size} > $MAX_SET_ITEMS")
        }
        @Suppress("UNCHECKED_CAST")
        val items = (values as Set<String>).toList().sorted()
        data.writeInt(items.size)
        for (item in items) {
            writeString(data, item)
        }
    }

    @JvmStatic
    private fun readKey(input: DataInputStream): String {
        val length = input.readUnsignedShort()
        if (length > MAX_KEY_BYTES) {
            throw BackupFormatException("V2 key length exceeds $MAX_KEY_BYTES")
        }
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return decodeStrictUtf8(bytes)
    }

    @JvmStatic
    private fun readValue(input: DataInputStream): Any? {
        val type = input.readUnsignedByte()
        return when (type) {
            TYPE_BOOLEAN -> {
                val raw = input.readUnsignedByte()
                if (raw != 0 && raw != 1) {
                    throw BackupFormatException("V2 boolean tag must be 0 or 1")
                }
                raw == 1
            }
            TYPE_INT -> input.readInt()
            TYPE_LONG -> input.readLong()
            TYPE_FLOAT -> input.readFloat()
            TYPE_STRING -> readString(input)
            TYPE_STRING_SET -> readStringSet(input)
            else -> throw BackupFormatException("Unknown V2 type tag: $type")
        }
    }

    @JvmStatic
    private fun readString(input: DataInputStream): String {
        val length = input.readInt()
        if (length < 0) {
            throw BackupFormatException("V2 string length negative: $length")
        }
        if (length > MAX_STRING_BYTES) {
            throw BackupFormatException("V2 string length exceeds $MAX_STRING_BYTES")
        }
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return decodeStrictUtf8(bytes)
    }

    @JvmStatic
    private fun readStringSet(input: DataInputStream): Set<String> {
        val count = input.readInt()
        if (count < 0) {
            throw BackupFormatException("V2 StringSet count negative: $count")
        }
        if (count > MAX_SET_ITEMS) {
            throw BackupFormatException("V2 StringSet count exceeds $MAX_SET_ITEMS")
        }
        val set = LinkedHashSet<String>(count)
        repeat(count) {
            set.add(readString(input))
        }
        return set
    }

    @JvmStatic
    private fun decodeStrictUtf8(bytes: ByteArray): String {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            throw BackupFormatException("Malformed or unmappable UTF-8")
        }
    }

    @JvmStatic
    private fun encodeStrictUtf8(value: String): ByteArray {
        val encoder = StandardCharsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            val buffer = encoder.encode(CharBuffer.wrap(value))
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        } catch (e: CharacterCodingException) {
            throw BackupFormatException("Malformed or unmappable UTF-16 source")
        }
    }

    @JvmStatic
    private fun readUnsignedInt32(bytes: ByteArray, offset: Int): Int {
        return (
            (bytes[offset].toInt() and 0xFF shl 24) or
                (bytes[offset + 1].toInt() and 0xFF shl 16) or
                (bytes[offset + 2].toInt() and 0xFF shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
        )
    }
}
