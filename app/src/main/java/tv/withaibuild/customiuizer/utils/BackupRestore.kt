package tv.withaibuild.customiuizer.utils

import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

/**
 * A13 backup / restore owner.
 *
 * Cold-path only. Writes V2 (CUI2 / typed entries / CRC32). Restore auto-detects
 * V2 or historical Java serialization and uses the restricted legacy decoder.
 * A13 ObjectOutputStream backups remain readable. New backups are V2.
 */
object BackupRestore {

    private val BACKUP_FILENAME_FORMATTER =
        DateTimeFormatter.ofPattern("MMddHHmmss", Locale.US)

    val NON_EXPORTABLE_KEYS = setOf(
        "pref_key_miuizer_locale_applied",
        "pref_key_miuizer_synced_from_lsposed",
    )

    /** A14 keys restored onto the existing A13 preference names. */
    private val LEGACY_KEY_ALIASES = mapOf(
        "pref_key_system_usb_default_function" to "pref_key_system_defaultusb",
        "system_usb_default_function" to "system_defaultusb",
        "pref_key_system_netspeed_boldfont" to "pref_key_system_netspeed_bold",
        "system_netspeed_boldfont" to "system_netspeed_bold",
    )

    private val LEGACY_MAGIC = byteArrayOf(0xAC.toByte(), 0xED.toByte(), 0x00, 0x05)

    const val LEGACY_MAX_DEPTH = 16L
    const val LEGACY_MAX_DESCRIPTOR_DEPTH = 8L
    const val LEGACY_MAX_REFERENCES = 100_000L
    const val LEGACY_MAX_ARRAY_LENGTH = 16_384L

    enum class Status { SUCCESS, PARTIAL_FAILURE, FAILURE }

    data class RestoreResult(
        val status: Status,
        val restored: Int = 0,
        val deprecatedIgnored: Int = 0,
        val invalidSkipped: Int = 0,
        val unknownIgnored: Int = 0,
        val appSelectionsSanitized: Int = 0,
        val migrated: Int = 0,
        val commitSucceeded: Boolean = false,
        val commitConfirmedDurable: Boolean = false,
        val deviceReconciled: Boolean = false,
        val rollbackAttempted: Boolean = false,
        val rollbackSucceeded: Boolean = false,
    ) {
        val isSuccess: Boolean get() = status == Status.SUCCESS
    }

    @JvmStatic
    fun generateBackupFilename(): String =
        "r13bak_" + BACKUP_FILENAME_FORMATTER.format(LocalDateTime.now())

    @JvmStatic
    fun filterBackupEntries(prefs: SharedPreferences): Map<String, Any?> {
        val source = prefs.all
        val filtered = LinkedHashMap<String, Any?>(source.size)
        for ((key, value) in source) {
            if (key in NON_EXPORTABLE_KEYS) continue
            if (value != null && !isSupportedValue(value)) {
                throw BackupFormatV2.BackupFormatException(
                    "Unsupported backup value type for key '$key': ${value.javaClass}"
                )
            }
            filtered[key] = when (value) {
                is Set<*> -> HashSet<String>(value.size).apply {
                    @Suppress("UNCHECKED_CAST")
                    addAll(value as Set<String>)
                }
                else -> value
            }
        }
        return filtered
    }

    @JvmStatic
    private fun isSupportedValue(value: Any?): Boolean {
        return value == null ||
            value is Boolean ||
            value is Int ||
            value is Long ||
            value is Float ||
            value is String ||
            (value is Set<*> && value.all { it is String })
    }

    @JvmStatic
    fun performBackup(
        prefs: SharedPreferences,
        output: OutputStream,
    ): Boolean {
        val entries = filterBackupEntries(prefs)
        val encoded = BackupFormatV2.encode(entries)
        output.write(encoded)
        output.flush()
        return true
    }

    @JvmStatic
    fun capturePreRestoreSnapshot(prefs: SharedPreferences): Map<String, Any?> {
        val snapshot = HashMap<String, Any?>(prefs.all.size * 4 / 3 + 1)
        for ((key, value) in prefs.all) {
            snapshot[key] = when (value) {
                is Set<*> -> HashSet<String>(value.size).apply {
                    @Suppress("UNCHECKED_CAST")
                    addAll(value as Set<String>)
                }
                else -> value
            }
        }
        return snapshot
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readBoundedInputStream(input: InputStream, maxBytes: Long = BackupFormatV2.MAX_FILE_SIZE): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    @JvmStatic
    fun decodeBackup(bytes: ByteArray): Map<*, *> {
        if (bytes.size < 4) {
            throw BackupRestoreException("Backup file too short")
        }

        val firstFour = ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)

        val decoded = when {
            firstFour == BackupFormatV2.MAGIC -> BackupFormatV2.decode(bytes)
            bytes.size >= LEGACY_MAGIC.size &&
                bytes[0] == LEGACY_MAGIC[0] &&
                bytes[1] == LEGACY_MAGIC[1] &&
                bytes[2] == LEGACY_MAGIC[2] &&
                bytes[3] == LEGACY_MAGIC[3] -> decodeLegacyBackup(bytes)
            else -> throw BackupRestoreException("Unrecognized backup format")
        }

        postDecodeBoundsCheck(decoded)
        return decoded
    }

    @JvmStatic
    fun decodeLegacyBackup(bytes: ByteArray): Map<*, *> {
        try {
            return LegacyBackupDecoder.decode(bytes)
        } catch (e: BackupRestoreException) {
            throw e
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            rethrowIfFatal(t)
            throw BackupRestoreException("Legacy decode failed", t)
        }
    }

    @JvmStatic
    private fun postDecodeBoundsCheck(raw: Map<*, *>) {
        if (raw.size > BackupFormatV2.MAX_ENTRY_COUNT) {
            throw BackupRestoreException("Decoded entry count ${raw.size} exceeds ${BackupFormatV2.MAX_ENTRY_COUNT}")
        }
        for ((rawKey, rawValue) in raw) {
            if (rawKey !is String) continue
            val keyBytes = rawKey.toByteArray(Charsets.UTF_8)
            if (keyBytes.size > BackupFormatV2.MAX_KEY_BYTES) {
                throw BackupRestoreException("Decoded key length ${keyBytes.size} exceeds ${BackupFormatV2.MAX_KEY_BYTES}")
            }
            when (rawValue) {
                is String -> {
                    val stringBytes = rawValue.toByteArray(Charsets.UTF_8)
                    if (stringBytes.size > BackupFormatV2.MAX_STRING_BYTES) {
                        throw BackupRestoreException("Decoded string length ${stringBytes.size} exceeds ${BackupFormatV2.MAX_STRING_BYTES}")
                    }
                }
                is Set<*> -> {
                    if (rawValue.size > BackupFormatV2.MAX_SET_ITEMS) {
                        throw BackupRestoreException("Decoded StringSet size ${rawValue.size} exceeds ${BackupFormatV2.MAX_SET_ITEMS}")
                    }
                    for (item in rawValue) {
                        if (item !is String) continue
                        val itemBytes = item.toByteArray(Charsets.UTF_8)
                        if (itemBytes.size > BackupFormatV2.MAX_STRING_BYTES) {
                            throw BackupRestoreException("Decoded set item length ${itemBytes.size} exceeds ${BackupFormatV2.MAX_STRING_BYTES}")
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun validateAndNormalizeEntries(
        raw: Map<*, *>,
    ): Pair<Map<String, Any?>, RestoreResult> {
        val normalized = LinkedHashMap<String, Any?>(raw.size)
        var invalidSkipped = 0
        var deprecatedIgnored = 0
        var migrated = 0

        for ((rawKey, rawValue) in raw) {
            if (rawKey !is String) {
                invalidSkipped++
                continue
            }
            val value = normalizeValue(rawValue)
            if (value == null) {
                invalidSkipped++
                continue
            }
            val mapped = LEGACY_KEY_ALIASES[rawKey] ?: rawKey
            if (mapped != rawKey) migrated++
            if (mapped in NON_EXPORTABLE_KEYS) {
                deprecatedIgnored++
                continue
            }
            normalized[mapped] = value
        }

        val counts = RestoreResult(
            status = Status.FAILURE,
            restored = normalized.size,
            deprecatedIgnored = deprecatedIgnored,
            invalidSkipped = invalidSkipped,
            migrated = migrated,
        )
        return Pair(normalized, counts)
    }

    @JvmStatic
    private fun normalizeValue(rawValue: Any?): Any? {
        return when (rawValue) {
            is Boolean, is Int, is Long, is Float, is String -> rawValue
            is Set<*> -> {
                val set = LinkedHashSet<String>(rawValue.size)
                for (element in rawValue) {
                    if (element !is String) return null
                    set.add(element)
                }
                set
            }
            else -> null
        }
    }

    @JvmStatic
    fun putSupportedPreferenceEntries(
        editor: SharedPreferences.Editor,
        entries: Map<String, Any?>,
    ) {
        for ((key, value) in entries) {
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> {
                    val set = LinkedHashSet<String>(value.size)
                    @Suppress("UNCHECKED_CAST")
                    for (element in value as Set<String>) {
                        set.add(element)
                    }
                    editor.putStringSet(key, set)
                }
            }
        }
    }

    @JvmStatic
    fun reconcileLauncherIcon(
        packageManager: PackageManager,
        componentName: ComponentName,
        enabled: Boolean?,
    ): Boolean {
        return try {
            val target = when (enabled) {
                null, true -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                componentName,
                target,
                PackageManager.DONT_KILL_APP,
            )
            true
        } catch (t: Throwable) {
            rethrowIfFatal(t)
            false
        }
    }

    @JvmStatic
    fun performRestore(
        inputStream: InputStream,
        prefs: SharedPreferences,
        installedPackages: Set<String>,
        launcherReconciler: ((Boolean?) -> Boolean)? = null,
    ): RestoreResult = inputStream.use { stream ->
        val bytes = try {
            readBoundedInputStream(stream) ?: return@use RestoreResult(
                status = Status.FAILURE,
                commitSucceeded = false,
                commitConfirmedDurable = false,
            )
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            rethrowIfFatal(t)
            return@use RestoreResult(
                status = Status.FAILURE,
                commitSucceeded = false,
                commitConfirmedDurable = false,
            )
        }

        val rawRoot = try {
            decodeBackup(bytes)
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            rethrowIfFatal(t)
            return@use RestoreResult(
                status = Status.FAILURE,
                commitSucceeded = false,
                commitConfirmedDurable = false,
            )
        }

        val (entriesAfterValidation, validationCounts) = validateAndNormalizeEntries(rawRoot)
        val invalidSkipped = validationCounts.invalidSkipped
        val deprecatedIgnored = validationCounts.deprecatedIgnored
        val migrated = validationCounts.migrated
        var restored = validationCounts.restored

        val (sanitizedEntries, appSelectionsSanitized) = try {
            val sanitized = AppSelectionSanitizer.sanitizeRestoredEntries(
                entriesAfterValidation,
                installedPackages,
            )
            Pair(sanitized.entries, sanitized.changedPrimaryCount)
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            rethrowIfFatal(t)
            return@use RestoreResult(
                status = Status.FAILURE,
                invalidSkipped = invalidSkipped,
                deprecatedIgnored = deprecatedIgnored,
                migrated = migrated,
                appSelectionsSanitized = 0,
                restored = restored,
                commitSucceeded = false,
                commitConfirmedDurable = false,
            )
        }

        restored = sanitizedEntries.size
        val snapshot = capturePreRestoreSnapshot(prefs)

        val primaryEditor = prefs.edit()
        primaryEditor.clear()
        putSupportedPreferenceEntries(primaryEditor, sanitizedEntries)
        AppLocaleController.stageReconcileMarker(primaryEditor)
        val primaryCommit = primaryEditor.commit()

        if (!primaryCommit) {
            val rollbackEditor = prefs.edit()
            rollbackEditor.clear()
            putSupportedPreferenceEntries(rollbackEditor, snapshot)
            val rollbackCommit = rollbackEditor.commit()
            return@use RestoreResult(
                status = Status.FAILURE,
                restored = restored,
                deprecatedIgnored = deprecatedIgnored,
                invalidSkipped = invalidSkipped,
                migrated = migrated,
                appSelectionsSanitized = appSelectionsSanitized,
                commitSucceeded = false,
                commitConfirmedDurable = false,
                deviceReconciled = false,
                rollbackAttempted = true,
                rollbackSucceeded = rollbackCommit,
            )
        }

        val launcherEnabled = prefs.getBoolean("pref_key_miuizer_launchericon", true)
        val launcherReconciled = launcherReconciler?.invoke(launcherEnabled) ?: true
        val status = if (launcherReconciled) Status.SUCCESS else Status.PARTIAL_FAILURE

        RestoreResult(
            status = status,
            restored = restored,
            deprecatedIgnored = deprecatedIgnored,
            invalidSkipped = invalidSkipped,
            migrated = migrated,
            appSelectionsSanitized = appSelectionsSanitized,
            commitSucceeded = true,
            commitConfirmedDurable = true,
            deviceReconciled = launcherReconciled,
            rollbackAttempted = false,
            rollbackSucceeded = false,
        )
    }

    @JvmStatic
    fun performRestore(
        inputStream: InputStream,
        packageManager: PackageManager,
        prefs: SharedPreferences,
        componentName: ComponentName? = null,
    ): RestoreResult {
        val installedPackages = try {
            AppSelectionSanitizer.queryInstalledPackageNames(packageManager)
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            rethrowIfFatal(t)
            return RestoreResult(
                status = Status.FAILURE,
                commitSucceeded = false,
                commitConfirmedDurable = false,
            )
        }

        val reconciler: ((Boolean?) -> Boolean)? = if (componentName != null) { enabled ->
            reconcileLauncherIcon(packageManager, componentName, enabled)
        } else null

        return performRestore(inputStream, prefs, installedPackages, reconciler)
    }

    private fun rethrowIfFatal(throwable: Throwable?) {
        var current = throwable
        for (depth in 0 until 8) {
            if (current == null) return
            when (current) {
                is OutOfMemoryError -> throw current
                is ThreadDeath -> throw current
                is VirtualMachineError -> throw current
            }
            val next = current.cause
            if (next === current) return
            current = next
        }
    }

    class BackupRestoreException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
