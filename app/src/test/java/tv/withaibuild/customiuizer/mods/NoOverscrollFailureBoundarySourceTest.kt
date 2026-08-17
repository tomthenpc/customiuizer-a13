package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Narrow source-structure regression for B2A-D2 [NoOverscrollAppHook].
 *
 * Only the four catch(Throwable) sites inside that function are inspected.
 */
class NoOverscrollFailureBoundarySourceTest {

    @Test
    fun noOverscrollAppHook_fourCatchesUseRuntimeFatality_notOomOnly() {
        val fn = extractJvmStaticFun("NoOverscrollAppHook")
        val catches = extractThrowableCatches(fn)

        assertEquals("NoOverscrollAppHook must contain exactly four catch(Throwable) sites", 4, catches.size)

        val primary = catches.filter { it.parameter == "t" }
        val fallback = catches.filter { it.parameter == "fallback" }
        assertEquals(2, primary.size)
        assertEquals(2, fallback.size)

        for (block in catches) {
            assertTrue(
                "${block.parameter} catch must call RuntimeFatality.throwIfFatal(${block.parameter})",
                block.body.contains("RuntimeFatality.throwIfFatal(${block.parameter})")
            )
            assertFalse(
                "${block.parameter} catch must not special-case only OutOfMemoryError",
                block.body.contains("is OutOfMemoryError")
            )
            assertFalse(
                "NoOverscrollAppHook must not add a local fatal helper",
                block.body.contains("fun isFatal") || block.body.contains("isFatal(")
            )
        }

        assertTrue(
            "SpringBackLayout primary → fallback field write must remain",
            fn.contains("callMethod(param.thisObject, \"setSpringBackEnable\", false)") &&
                fn.contains("setBooleanField(param.thisObject, \"mSpringBackEnable\", false)")
        )
        assertTrue(
            "RemixRecyclerView primary → fallback field write must remain",
            fn.contains("callMethod(param.thisObject, \"setSpringEnabled\", false)") &&
                fn.contains("setBooleanField(param.thisObject, \"mSpringEnabled\", false)")
        )
        assertTrue(
            "AbsListView semantics must remain",
            fn.contains("android.widget.AbsListView") && fn.contains("initAbsListView")
        )
    }

    private data class CatchBlock(val parameter: String, val body: String)

    private fun extractJvmStaticFun(name: String): String {
        val source = readHooksSource()
        val marker = "fun $name("
        val start = source.indexOf(marker)
        if (start < 0) fail("missing fun $name in SystemAudioAndVisualAndMoreHooks.kt")
        val next = source.indexOf("\n    @JvmStatic", start + marker.length)
        val end = if (next >= 0) next else source.length
        return source.substring(start, end)
    }

    private fun extractThrowableCatches(functionSource: String): List<CatchBlock> {
        val regex = Regex("""catch \((t|fallback): Throwable\)""")
        val blocks = mutableListOf<CatchBlock>()
        for (match in regex.findAll(functionSource)) {
            val brace = functionSource.indexOf('{', match.range.last)
            if (brace < 0) fail("unterminated catch in NoOverscrollAppHook")
            blocks += CatchBlock(match.groupValues[1], extractBrace(functionSource, brace))
        }
        return blocks
    }

    private fun extractBrace(source: String, brace: Int): String {
        var depth = 0
        for (i in brace until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(brace, i + 1)
                }
            }
        }
        fail("unterminated catch brace in NoOverscrollAppHook")
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
