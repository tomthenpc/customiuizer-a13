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

class HookUtilsDiagnosticsTest {

    @Test
    fun copyFile_validSource_returnsTrueAndCopiesContent() {
        val tempDir = Files.createTempDirectory("hookutils_copy")
        val source = tempDir.resolve("source.txt")
        val target = tempDir.resolve("target.txt")
        source.writeText("hello hookutils")

        try {
            assertTrue("copyFile should return true for valid source", HookUtils.copyFile(source.toString(), target.toString()))
            assertEquals("hello hookutils", target.readText())
        } finally {
            source.deleteIfExists()
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun copyFile_missingSource_returnsFalse() {
        val tempDir = Files.createTempDirectory("hookutils_copy")
        val missing = tempDir.resolve("missing.txt")
        val target = tempDir.resolve("target.txt")

        try {
            assertFalse("copyFile should return false when source does not exist", HookUtils.copyFile(missing.toString(), target.toString()))
        } finally {
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun copyFile_failureDoesNotPropagate() {
        // This is mainly a contract test: the catch block should swallow the
        // normal exception and return false, not throw it to the caller.
        val tempDir = Files.createTempDirectory("hookutils_copy")
        val missing = tempDir.resolve("missing.txt")
        val target = tempDir.resolve("target.txt")

        try {
            val result = runCatching { HookUtils.copyFile(missing.toString(), target.toString()) }.getOrNull()
            assertEquals(false, result)
        } finally {
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun copyFile_existingTarget_isOverwritten() {
        val tempDir = Files.createTempDirectory("hookutils_copy")
        val source = tempDir.resolve("source.txt")
        val target = tempDir.resolve("target.txt")
        source.writeText("new content")
        target.createFile()
        target.writeText("old content")

        try {
            assertTrue(HookUtils.copyFile(source.toString(), target.toString()))
            assertEquals("new content", target.readText())
        } finally {
            source.deleteIfExists()
            target.deleteIfExists()
            tempDir.deleteIfExists()
        }
    }

    @Test
    fun getAnimationScale_returnsFallback_whenReflectionFails() {
        // In a JVM unit test the Android ServiceManager / WindowManagerService
        // classes are unavailable, so the method should fall back to 1.0f.
        assertEquals(1.0f, HookUtils.getAnimationScale(0), 0.0f)
    }
}
