package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Narrow source-structure regression for B1-D1 / B1-D2.
 *
 * Only [GalleryScreenshotPathHook] and [ScreenshotFloatTimeHook] are inspected.
 * This is not a whole-file catch(Throwable) audit.
 */
class MediaHookFailureBoundarySourceTest {

    @Test
    fun galleryScreenshotPathHook_usesFindClassIfExists_andRuntimeFatality() {
        val fn = extractJvmStaticFun("GalleryScreenshotPathHook")

        assertTrue(
            "GalleryScreenshotPathHook must look up MIUIStorageConstants with findClassIfExists",
            fn.contains("findClassIfExists(") &&
                fn.contains("com.miui.gallery.storage.constants.MIUIStorageConstants")
        )
        assertFalse(
            "GalleryScreenshotPathHook must not use throwing findClass() for MIUIStorageConstants",
            fn.contains("findClass(\"com.miui.gallery.storage.constants.MIUIStorageConstants\"")
        )
        assertTrue(
            "GalleryScreenshotPathHook must reuse RuntimeFatality.throwIfFatal",
            fn.contains("RuntimeFatality.throwIfFatal(t)")
        )
        assertFalse(
            "GalleryScreenshotPathHook must not add a local fatal helper",
            fn.contains("fun isFatal") || fn.contains("private fun isFatal")
        )
    }

    @Test
    fun screenshotFloatTimeHook_catchUsesRuntimeFatality_andOrdinaryFailOpen() {
        val fn = extractJvmStaticFun("ScreenshotFloatTimeHook")
        val catchBlock = extractFirstThrowableCatch(fn)

        assertTrue(
            "ScreenshotFloatTimeHook catch must call RuntimeFatality.throwIfFatal(t)",
            catchBlock.contains("RuntimeFatality.throwIfFatal(t)")
        )
        assertTrue(
            "ordinary reflection failure must still fail-open to false",
            catchBlock.contains("false")
        )
        assertFalse(
            "ScreenshotFloatTimeHook catch must not special-case only OutOfMemoryError",
            catchBlock.contains("t is OutOfMemoryError")
        )
        assertFalse(
            "ScreenshotFloatTimeHook must not add a local fatal helper",
            fn.contains("fun isFatal") || fn.contains("private fun isFatal")
        )
    }

    private fun extractJvmStaticFun(name: String): String {
        val source = readHooksSource()
        val marker = "fun $name("
        val start = source.indexOf(marker)
        if (start < 0) fail("missing fun $name in SystemAudioAndVisualAndMoreHooks.kt")
        val next = source.indexOf("\n    @JvmStatic", start + marker.length)
        val end = if (next >= 0) next else source.length
        return source.substring(start, end)
    }

    private fun extractFirstThrowableCatch(functionSource: String): String {
        val catchIdx = functionSource.indexOf("catch (t: Throwable)")
        if (catchIdx < 0) fail("ScreenshotFloatTimeHook must contain catch (t: Throwable)")
        val brace = functionSource.indexOf('{', catchIdx)
        if (brace < 0) fail("unterminated catch in ScreenshotFloatTimeHook")
        var depth = 0
        for (i in brace until functionSource.length) {
            when (functionSource[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return functionSource.substring(brace, i + 1)
                }
            }
        }
        fail("unterminated catch brace in ScreenshotFloatTimeHook")
        return ""
    }

    private fun readHooksSource(): String {
        val sourceFile = File(
            "src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt"
        )
        if (!sourceFile.isFile) fail("missing source file: ${sourceFile.absolutePath}")
        return sourceFile.readText()
    }
}
