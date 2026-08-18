package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SystemUiB3BCompatibilitySourceTest {

    @Test
    fun b3bLookupsFailOpenWithoutThrowingFindClass() {
        val tiles = extractFun("SystemUIMonitorAndTileHooks.kt", "AddCustomTileHook")
        assertTrue(tiles.contains("findClassIfExists("))
        assertFalse(
            tiles.contains("findClass(\"com.android.systemui.qs.tileimpl.QSTileImpl\$ResourceIcon\"")
        )
        assertTrue(tiles.indexOf("createTileInternal") < tiles.indexOf("ResourceIconClass != null"))

        val monitor = extractFun("SystemUIStatusBarHooks.kt", "MonitorDeviceInfoHook")
        assertTrue(monitor.contains("findClassIfExists("))
        assertFalse(
            monitor.contains("findClass(\"com.android.systemui.plugins.DarkIconDispatcher\"")
        )
        assertTrue(monitor.contains("NetworkSpeedViewClass != null"))
        assertTrue(monitor.indexOf("DeviceInfoMonitor.hook") > monitor.indexOf("findClassIfExists"))

        val notif = extractFun("SystemNotificationMoreHooks.kt", "DisableAnyNotificationHook")
        assertTrue(notif.contains("findClassIfExists("))
        assertTrue(notif.contains("RuntimeFatality.throwIfFatal"))
        assertTrue(
            notif.indexOf("NotificationFilterHelper") > notif.indexOf("findClassIfExists")
        )

        val charge = extractFun("SystemDisplayAndWindowHooks.kt", "ChargeAnimationHook")
        assertTrue(charge.contains("RuntimeFatality.throwIfFatal"))
        assertFalse(charge.contains("if (t1 is OutOfMemoryError"))
        assertFalse(charge.contains("if (t2 is OutOfMemoryError"))
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
