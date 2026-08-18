package tv.withaibuild.customiuizer.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import tv.withaibuild.customiuizer.BuildConfig

class BackupFormatV2Test {

    @Test
    fun encodeDecodeRoundTripAllSupportedTypes() {
        val entries = linkedMapOf(
            "pref_key_bool" to true,
            "pref_key_int" to 42,
            "pref_key_long" to 1234567890123L,
            "pref_key_float" to 3.14f,
            "pref_key_string" to "hello world",
            "pref_key_set" to LinkedHashSet(listOf("com.a", "com.b")),
        )
        val decoded = BackupFormatV2.decode(BackupFormatV2.encode(entries))
        assertEquals(true, decoded["pref_key_bool"])
        assertEquals(42, decoded["pref_key_int"])
        assertEquals(1234567890123L, decoded["pref_key_long"])
        assertEquals(3.14f, decoded["pref_key_float"])
        assertEquals("hello world", decoded["pref_key_string"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(setOf("com.a", "com.b"), decoded["pref_key_set"] as Set<String>)
    }

    @Test
    fun decodeRejectsCrcMutation() {
        val encoded = BackupFormatV2.encode(linkedMapOf("pref_key_test" to "value")).copyOf()
        encoded[8] = (encoded[8].toInt() xor 0x01).toByte()
        try {
            BackupFormatV2.decode(encoded)
            fail("CRC mismatch must be rejected")
        } catch (e: BackupFormatV2.BackupFormatException) {
            assertTrue(e.message?.contains("CRC") == true)
        }
    }

    @Test
    fun headerUsesCui2MagicAndCurrentVersionCode() {
        val encoded = BackupFormatV2.encode(linkedMapOf("pref_key_bool" to true))
        val magic = ((encoded[0].toInt() and 0xFF) shl 24) or
            ((encoded[1].toInt() and 0xFF) shl 16) or
            ((encoded[2].toInt() and 0xFF) shl 8) or
            (encoded[3].toInt() and 0xFF)
        assertEquals(BackupFormatV2.MAGIC, magic)
        val revision = ((encoded[8].toInt() and 0xFF) shl 24) or
            ((encoded[9].toInt() and 0xFF) shl 16) or
            ((encoded[10].toInt() and 0xFF) shl 8) or
            (encoded[11].toInt() and 0xFF)
        assertEquals(BuildConfig.VERSION_CODE, revision)
    }
}

class BackupRestoreTest {

    @Test
    fun generateBackupFilenameHasR13Prefix() {
        val filename = BackupRestore.generateBackupFilename()
        assertTrue(filename.startsWith("r13bak_"))
        assertEquals("r13bak_".length + 10, filename.length)
    }

    @Test
    fun decodeLegacyBackupAcceptsHashMap() {
        val map = HashMap<String, Any?>()
        map["pref_key_enabled"] = true
        map["pref_key_count"] = 7
        val decoded = BackupRestore.decodeLegacyBackup(serialize(map))
        assertEquals(true, decoded["pref_key_enabled"])
        assertEquals(7, decoded["pref_key_count"])
    }

    @Test
    fun restoreV2MigratesUsbAliasAndRollsBackOnCommitFailure() {
        val source = FakeSharedPreferences()
        source.put("pref_key_system_usb_default_function", "mtp")
        source.put("pref_key_system_netspeed_boldfont", true)
        val encoded = ByteArrayOutputStream()
        BackupRestore.performBackup(source, encoded)

        val dest = FakeSharedPreferences()
        dest.put("pref_key_keep", "old")
        dest.commitResult = false
        val result = BackupRestore.performRestore(
            ByteArrayInputStream(encoded.toByteArray()),
            dest,
            emptySet(),
            null,
        )
        assertEquals(BackupRestore.Status.FAILURE, result.status)
        assertTrue(result.rollbackAttempted)
        assertEquals("old", dest.getString("pref_key_keep", null))
    }

    @Test
    fun restoreV2WritesMigratedKeysAndSanitizesApps() {
        val source = FakeSharedPreferences()
        source.put("pref_key_system_usb_default_function", "charging")
        source.put("pref_key_demo_apps", HashSet(listOf("com.installed", "com.missing")))
        val encoded = ByteArrayOutputStream()
        BackupRestore.performBackup(source, encoded)

        val dest = FakeSharedPreferences()
        dest.put("pref_key_keep", "gone")
        val result = BackupRestore.performRestore(
            ByteArrayInputStream(encoded.toByteArray()),
            dest,
            setOf("com.installed"),
            { true },
        )
        assertEquals(BackupRestore.Status.SUCCESS, result.status)
        assertEquals("charging", dest.getString("pref_key_system_defaultusb", null))
        assertFalse(dest.contains("pref_key_keep"))
        assertEquals(setOf("com.installed"), dest.getStringSet("pref_key_demo_apps", emptySet()))
        assertTrue(result.migrated >= 1)
        assertTrue(result.appSelectionsSanitized >= 1)
    }

    @Test
    fun restoreLegacyObjectOutputStreamBackup() {
        val map = HashMap<String, Any?>()
        map["pref_key_foo"] = true
        map["pref_key_name"] = "a13"
        val dest = FakeSharedPreferences()
        dest.put("pref_key_old", 1)
        val result = BackupRestore.performRestore(
            ByteArrayInputStream(serialize(map)),
            dest,
            emptySet(),
            { true },
        )
        assertEquals(BackupRestore.Status.SUCCESS, result.status)
        assertEquals(true, dest.getBoolean("pref_key_foo", false))
        assertEquals("a13", dest.getString("pref_key_name", null))
        assertFalse(dest.contains("pref_key_old"))
    }

    @Test
    fun filterBackupEntriesOmitsRuntimeMarkers() {
        val prefs = FakeSharedPreferences()
        prefs.put("pref_key_foo", true)
        prefs.put("pref_key_miuizer_locale_applied", "en")
        prefs.put("pref_key_miuizer_synced_from_lsposed", true)
        val filtered = BackupRestore.filterBackupEntries(prefs)
        assertTrue(filtered.containsKey("pref_key_foo"))
        assertFalse(filtered.containsKey("pref_key_miuizer_locale_applied"))
        assertFalse(filtered.containsKey("pref_key_miuizer_synced_from_lsposed"))
    }

    private fun serialize(value: Any): ByteArray {
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use { it.writeObject(value) }
        return output.toByteArray()
    }
}

class AppSelectionSanitizerTest {

    @Test
    fun sanitizeRestoredEntriesDropsMissingPackages() {
        val entries = mapOf<String, Any?>(
            "pref_key_demo_apps" to setOf("com.keep", "com.gone"),
            "pref_key_demo_app" to "com.gone|Main",
        )
        val result = AppSelectionSanitizer.sanitizeRestoredEntries(entries, setOf("com.keep"))
        assertEquals(setOf("com.keep"), result.entries["pref_key_demo_apps"])
        assertFalse(result.entries.containsKey("pref_key_demo_app"))
        assertTrue(result.changedPrimaryCount >= 1)
    }
}
