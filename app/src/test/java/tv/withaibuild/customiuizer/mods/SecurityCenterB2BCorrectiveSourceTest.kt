package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Narrow source invariants for B2B-D1 / D3 / D4 SecurityCenter paths in Various.kt.
 */
class SecurityCenterB2BCorrectiveSourceTest {

    @Test
    fun installer_combinesAppsortAndSkip_withoutDeletingSkip() {
        val installer = File("src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java")
            .readText()
        assertTrue(installer.contains("various_skip"))
        assertTrue(
            installer.contains("getStringAsInt(\"various_appsort\", 1) > 1") &&
                installer.contains("getStringAsInt(\"various_skip\", 0) > 0")
        )
        assertFalse(
            Regex(
                """if \(MainModule\.mPrefs\.getStringAsInt\("various_appsort", 1\) > 1\) Various\.AppsDefaultSortHook"""
            ).containsMatchIn(installer)
        )
    }

    @Test
    fun d1_threeLookupsUseFindClassIfExists_andAppsRestrictDoesNotWholeReturn() {
        val restrict = extractFun("AppsRestrictHook")
        assertTrue(
            restrict.contains("findClassIfExists(") &&
                restrict.contains("com.miui.appmanager.AppManageUtils")
        )
        assertFalse(restrict.contains("findClass(\"com.miui.appmanager.AppManageUtils\""))
        assertTrue(
            "missing AppManageUtils must not skip independent networkassistant hooks",
            restrict.contains("ShowAppDetailFragment") &&
                restrict.contains("FirewallService") &&
                restrict.indexOf("ShowAppDetailFragment") >
                restrict.indexOf("Cannot find AppManageUtils class!")
        )

        val intercept = extractFun("InterceptPermHook")
        assertTrue(
            intercept.contains("findClassIfExists(") &&
                intercept.contains("com.miui.permcenter.privacymanager.InterceptBaseFragment")
        )
        assertFalse(
            intercept.contains("findClass(\"com.miui.permcenter.privacymanager.InterceptBaseFragment\"")
        )

        val battery = extractFun("ShowTempInBatteryHook")
        assertTrue(
            battery.contains("findClassIfExists(") &&
                battery.contains("com.miui.powercenter.BatteryFragment")
        )
        assertFalse(battery.contains("findClass(\"com.miui.powercenter.BatteryFragment\""))
    }

    @Test
    fun d3_clickHookIsNotOnCreateLocalActivityCapture() {
        val appInfo = extractFun("AppInfoHook")
        assertFalse(
            "onCreate must not register onPreferenceTreeClick with a nested MethodHook",
            appInfo.contains("hookAllMethods(frag.javaClass, \"onPreferenceTreeClick\"")
        )
        assertTrue(
            appInfo.contains("tryInstallAppInfoPreferenceClickHook(frag.javaClass, clickHookInstalled)")
        )

        val click = extractPropertyOrFun("appInfoPreferenceClickHook")
        assertTrue(click.contains("activityFromFragment(frag)"))
        assertTrue(click.contains("packageInfoFromFragment(frag)"))
        assertFalse(click.contains("mLastPackageInfo"))
        assertFalse(click.contains("param.thisObject as? Activity"))
    }

    @Test
    fun d4_authorizedCatchesUseRuntimeFatality_notDirectTypeChecks() {
        val bodies = listOf(
            extractFun("AppInfoHook"),
            extractPropertyOrFun("appInfoPreferenceClickHook"),
            extractFun("tryInstallAppInfoPreferenceClickHook"),
            extractFun("activityFromFragment"),
            extractFun("packageInfoFromFragment"),
            extractFun("AppsDefaultSortHook"),
            extractFun("tryInstallSortOnActivityCreated"),
            extractFun("setAppState"),
            extractFun("AppsDisableHook"),
            extractFun("AddSideBarExpandReceiverHook"),
            extractFun("UnlockClipboardAndLocationHook")
        )
        for (body in bodies) {
            assertFalse(
                "authorized SecurityCenter catch must not use direct OOM/ThreadDeath/VME checks:\n$body",
                DIRECT_FATAL_CHECK.containsMatchIn(body)
            )
        }
        assertTrue(
            extractFun("AppInfoHook").contains("RuntimeFatality.throwIfFatal")
        )
        assertTrue(
            extractFun("UnlockClipboardAndLocationHook").contains("RuntimeFatality.throwIfFatal")
        )
        assertTrue(
            extractFun("AddSideBarExpandReceiverHook").contains("RuntimeFatality.throwIfFatal")
        )
        assertFalse(
            "must not add a local fatal helper",
            readSource().contains("fun isFatal")
        )
    }

    private fun extractFun(name: String): String {
        val source = readSource()
        val marker = Regex("""\n    (?:private )?fun $name\(""")
        val match = marker.find(source) ?: fail("missing fun $name in Various.kt").let { return "" }
        val start = match.range.first + 1
        return source.substring(start, nextMemberStart(source, match.range.last))
    }

    private fun extractPropertyOrFun(name: String): String {
        val source = readSource()
        val marker = Regex("""\n    private val $name """)
        val match = marker.find(source)
        if (match != null) {
            return source.substring(match.range.first + 1, nextMemberStart(source, match.range.last))
        }
        return extractFun(name)
    }

    private fun nextMemberStart(source: String, from: Int): Int {
        val nextJvm = source.indexOf("\n    @JvmStatic", from)
        val nextPrivateFun = source.indexOf("\n    private fun ", from)
        val nextPrivateVal = source.indexOf("\n    private val ", from)
        val candidates = listOf(nextJvm, nextPrivateFun, nextPrivateVal).filter { it >= 0 }
        return candidates.minOrNull() ?: source.length
    }

    private fun readSource(): String {
        val sourceFile = File("src/main/java/tv/withaibuild/customiuizer/mods/Various.kt")
        if (!sourceFile.isFile) fail("missing source file: ${sourceFile.absolutePath}")
        return sourceFile.readText()
    }

    companion object {
        private val DIRECT_FATAL_CHECK = Regex(
            """is OutOfMemoryError \|\|[\s\S]{0,80}is ThreadDeath \|\|[\s\S]{0,80}is VirtualMachineError"""
        )
    }
}
