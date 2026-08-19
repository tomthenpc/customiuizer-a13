package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Narrow source invariant for B3A-R1 D1 authorized Launcher function bodies.
 */
class LauncherB3AR1FatalSourceTest {

    @Test
    fun authorizedLauncherCatchesUseRuntimeFatality_notOomOnly() {
        val bodies = listOf(
            extractFun("LauncherSystemHooks.kt", "FixAppInfoLaunchHook"),
            extractFun("LauncherSystemHooks.kt", "StickyFloatingWindowsLauncherHook"),
            extractFun("LauncherIconHooks.kt", "RenameShortcutsHook"),
            extractFun("LauncherIconHooks.kt", "IconScaleHook"),
            extractFun("LauncherAnimationHooks.kt", "FixAnimHook"),
            extractFun("LauncherFolderHooks.kt", "PrivacyFolderHook")
        )
        for (body in bodies) {
            assertFalse(
                "authorized Launcher catch must not use OOM-only fatal checks:\n$body",
                OOM_ONLY.containsMatchIn(body)
            )
            assertTrue(
                "authorized Launcher catch must reuse RuntimeFatality:\n$body",
                body.contains("RuntimeFatality.throwIfFatal")
            )
        }
        assertFalse(
            "must not add a local fatal helper",
            authorizedSources().any { it.contains("fun isFatal") || it.contains("fun throwIfFatal") }
        )
    }

    @Test
    fun registryRemovesLocalDirectOnlyIsFatal() {
        val registry = File("src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt")
            .readText()
        assertFalse(registry.contains("private fun isFatal"))
        assertTrue(registry.contains("RuntimeFatality.throwIfFatal"))
        assertFalse(
            DIRECT_ONLY.containsMatchIn(
                extractFunFromText(registry, "runCondition")
            )
        )
        assertFalse(
            DIRECT_ONLY.containsMatchIn(
                extractFunFromText(registry, "runCompatibilityAndInstaller")
            )
        )
    }

    private fun extractFun(fileName: String, name: String): String {
        val source = File("src/main/java/tv/withaibuild/customiuizer/mods/$fileName")
        if (!source.isFile) fail("missing source file: ${source.absolutePath}")
        return extractFunFromText(source.readText(), name)
    }

    private fun extractFunFromText(source: String, name: String): String {
        val marker = Regex("""\n    (?:private )?fun $name\(""")
        val match = marker.find(source) ?: fail("missing fun $name").let { return "" }
        val start = match.range.first + 1
        return source.substring(start, nextMemberStart(source, match.range.last))
    }

    private fun nextMemberStart(source: String, from: Int): Int {
        val nextJvm = source.indexOf("\n    @JvmStatic", from)
        val nextFun = source.indexOf("\n    fun ", from)
        val nextPrivateFun = source.indexOf("\n    private fun ", from)
        val candidates = listOf(nextJvm, nextFun, nextPrivateFun).filter { it >= 0 }
        return candidates.minOrNull() ?: source.length
    }

    private fun authorizedSources(): List<String> {
        return listOf(
            "LauncherSystemHooks.kt",
            "LauncherIconHooks.kt",
            "LauncherAnimationHooks.kt",
            "LauncherFolderHooks.kt"
        ).map { File("src/main/java/tv/withaibuild/customiuizer/mods/$it").readText() }
    }

    companion object {
        private val OOM_ONLY = Regex("""if \(t is OutOfMemoryError\) throw t""")
        private val DIRECT_ONLY = Regex(
            """t is OutOfMemoryError \|\|[\s\S]{0,80}t is ThreadDeath \|\|[\s\S]{0,80}t is VirtualMachineError"""
        )
    }
}
