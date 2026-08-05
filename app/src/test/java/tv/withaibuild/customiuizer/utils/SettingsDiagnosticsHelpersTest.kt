package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SettingsDiagnosticsHelpersTest {

    @Test
    fun copyFile_validSource_returnsTrueAndCopiesContent() {
        val tempDir = Files.createTempDirectory("helpers_copy")
        val source = tempDir.resolve("source.txt")
        val target = tempDir.resolve("target.txt")
        source.writeText("hello helpers")

        try {
            assertTrue("copyFile should return true for valid source", Helpers.copyFile(source.toString(), target.toString()))
            assertEquals("hello helpers", target.readText())
        } finally {
            source.deleteIfExists()
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun copyFile_existingTarget_isOverwritten() {
        val tempDir = Files.createTempDirectory("helpers_copy")
        val source = tempDir.resolve("source.txt")
        val target = tempDir.resolve("target.txt")
        source.writeText("new content")
        target.createFile()
        target.writeText("old content")

        try {
            assertTrue(Helpers.copyFile(source.toString(), target.toString()))
            assertEquals("new content", target.readText())
        } finally {
            source.deleteIfExists()
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun copyFile_missingSource_returnsFalse() {
        val tempDir = Files.createTempDirectory("helpers_copy")
        val missing = tempDir.resolve("missing.txt")
        val target = tempDir.resolve("target.txt")

        try {
            assertFalse("copyFile should return false when source does not exist", Helpers.copyFile(missing.toString(), target.toString()))
        } finally {
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun copyFile_failureDoesNotPropagate() {
        val tempDir = Files.createTempDirectory("helpers_copy")
        val missing = tempDir.resolve("missing.txt")
        val target = tempDir.resolve("target.txt")

        try {
            val result = runCatching { Helpers.copyFile(missing.toString(), target.toString()) }.getOrNull()
            assertEquals(false, result)
        } finally {
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun getAnimationScale_returnsFallback_whenReflectionFails() {
        // In a JVM unit test the Android ServiceManager / WindowManagerService
        // classes are unavailable, so the method should fall back to 1.0f.
        assertEquals(1.0f, Helpers.getAnimationScale(0), 0.0f)
    }

    @Test
    fun settingsDiagnostics_failure_runsWithoutError() {
        // Smoke test that the logger can be invoked with a plain Throwable.
        // android.util.Log is a no-op in the unit-test environment, so the
        // assertion is simply that no exception escapes.
        SettingsDiagnostics.failure("test.operation", RuntimeException("expected"))
    }
}
