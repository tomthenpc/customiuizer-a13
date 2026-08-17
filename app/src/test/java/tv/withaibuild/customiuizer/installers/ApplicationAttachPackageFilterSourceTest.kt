package tv.withaibuild.customiuizer.installers

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Source invariant test for A3: verify both Application.attach after callbacks
 * call the package identity gate before any legacy hook installation.
 *
 * This is intentionally not a broad grep: it only checks the two affected
 * installers and the exact guard-to-installation ordering.
 */
class ApplicationAttachPackageFilterSourceTest {

    @Test
    fun launcherInstaller_packageFilter_precedes_handleLoadLauncher() {
        val source = readSource("LauncherInstaller.java")

        val guardIndex = source.indexOf("isTargetPackage(param.getThisObject(), lpparam)")
        val hookIndex = source.indexOf("handleLoadLauncher(lpparam);")

        assertTrue("LauncherInstaller after callback must contain package gate", guardIndex >= 0)
        assertTrue(
            "package gate must appear before handleLoadLauncher",
            guardIndex < hookIndex
        )
    }

    @Test
    fun genericAppInstaller_packageFilter_precedes_hookInstallations() {
        val source = readSource("GenericAppInstaller.java")

        val guardIndex = source.indexOf("isTargetPackage(param.getThisObject(), lpparam)")
        val hookIndex = source.indexOf("StatusBarBackgroundCompatHook(lpparam)")

        assertTrue("GenericAppInstaller after callback must contain package gate", guardIndex >= 0)
        assertTrue(
            "package gate must appear before the first direct hook installation",
            guardIndex < hookIndex
        )
    }

    private fun readSource(fileName: String): String {
        val sourceFile = File("src/main/java/tv/withaibuild/customiuizer/installers/$fileName")
        if (!sourceFile.isFile) fail("missing source file: ${sourceFile.absolutePath}")
        return sourceFile.readText()
    }
}
