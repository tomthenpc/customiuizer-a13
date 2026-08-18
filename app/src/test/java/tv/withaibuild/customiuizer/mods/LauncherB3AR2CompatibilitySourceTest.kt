package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class LauncherB3AR2CompatibilitySourceTest {

    @Test
    fun r2LookupsFailOpenWithoutThrowingFindClass() {
        val unlock = extractFun("LauncherLayoutHooks.kt", "UnlockGridsHook")
        assertTrue(unlock.contains("findClassIfExists("))
        assertFalse(unlock.contains("findClass(\"com.miui.home.launcher.DeviceConfig\""))
        assertTrue(
            unlock.indexOf("ScreenUtils") > unlock.indexOf("findClassIfExists")
        )

        val log = extractFun("LauncherSystemHooks.kt", "DisableLauncherLogHook")
        assertTrue(log.contains("RuntimeFatality.throwIfFatal"))
        assertTrue(log.contains("IS_ENABLE"))

        val fsg = extractFun("LauncherGestureHooks.kt", "FSGesturesHook")
        assertTrue(fsg.contains("findClassIfExists("))
        assertFalse(fsg.contains("findClass(\n            \"com.miui.home.recents.BaseRecentsImpl\""))
        assertFalse(fsg.contains("findClass(\"com.miui.home.recents.BaseRecentsImpl\""))
        assertTrue(fsg.indexOf("GestureStubView") > fsg.indexOf("findClassIfExists"))

        val scale = extractFun("LauncherAnimationHooks.kt", "DisableLauncherWallpaperScale")
        assertTrue(scale.contains("wallpaperZoomManagerKtClass != null"))
        assertTrue(scale.contains("RuntimeFatality.throwIfFatal"))
        assertTrue(scale.indexOf("DimLayer") > scale.indexOf("wallpaperZoomManagerKtClass != null"))
    }

    private fun extractFun(fileName: String, name: String): String {
        val source = File("src/main/java/tv/withaibuild/customiuizer/mods/$fileName")
        if (!source.isFile) fail("missing ${source.absolutePath}")
        val text = source.readText()
        val marker = Regex("""\n    fun $name\(""")
        val match = marker.find(text) ?: fail("missing fun $name").let { return "" }
        val start = match.range.first + 1
        val nextJvm = text.indexOf("\n    @JvmStatic", match.range.last)
        val nextFun = text.indexOf("\n    fun ", match.range.last)
        val end = listOf(nextJvm, nextFun).filter { it >= 0 }.minOrNull() ?: text.length
        return text.substring(start, end)
    }
}
