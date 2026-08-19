package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Narrow source-structure regression for B2A-D1 [Various.SmartClipboardActionHook].
 */
class SmartClipboardFailureBoundarySourceTest {

    @Test
    fun smartClipboardActionHook_usesFindClassIfExists_andDoesNotThrowFindClass() {
        val fn = extractJvmStaticFun("SmartClipboardActionHook")

        assertTrue(
            "opt==3 must keep ClipboardTipDialog returnConstant(false)",
            fn.contains("returnConstant(false)")
        )
        assertTrue(
            "opt!=3 must keep ClipboardTipDialog returnConstant(true)",
            fn.contains("returnConstant(true)")
        )
        assertTrue(
            "SecurityPromptHandler must use findClassIfExists",
            fn.contains("findClassIfExists(") &&
                fn.contains("com.lbe.security.ui.SecurityPromptHandler")
        )
        assertFalse(
            "SecurityPromptHandler must not use throwing findClass()",
            fn.contains("findClass(\"com.lbe.security.ui.SecurityPromptHandler\"")
        )
        assertTrue(
            "ordinary SecurityPromptHandler lookup failure must reuse RuntimeFatality.throwIfFatal",
            fn.contains("RuntimeFatality.throwIfFatal(t)")
        )
        assertFalse(
            "SmartClipboardActionHook must not add a local fatal helper",
            fn.contains("fun isFatal") || fn.contains("private fun isFatal")
        )
    }

    private fun extractJvmStaticFun(name: String): String {
        val source = readSource()
        val marker = "fun $name("
        val start = source.indexOf(marker)
        if (start < 0) fail("missing fun $name in Various.kt")
        val next = source.indexOf("\n    @JvmStatic", start + marker.length)
        val end = if (next >= 0) next else source.length
        return source.substring(start, end)
    }

    private fun readSource(): String {
        val sourceFile = File("src/main/java/tv/withaibuild/customiuizer/mods/Various.kt")
        if (!sourceFile.isFile) fail("missing source file: ${sourceFile.absolutePath}")
        return sourceFile.readText()
    }
}
