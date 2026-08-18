package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SystemServerB3CSourceTest {

    @Test
    fun installerUsesCanonicalRuntimeFatality() {
        val installer = File("src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        if (!installer.isFile) fail("missing ${installer.absolutePath}")
        val text = installer.readText()
        assertTrue(text.contains("RuntimeFatality.throwIfFatal"))
        assertFalse(text.contains("private static void rethrowIfFatal"))
    }

    @Test
    fun packagePermissionsUsesRuntimeFatality() {
        val source = File("src/main/java/tv/withaibuild/customiuizer/mods/PackagePermissions.kt")
        if (!source.isFile) fail("missing ${source.absolutePath}")
        val text = source.readText()
        assertTrue(text.contains("RuntimeFatality.throwIfFatal"))
        assertFalse(text.contains("if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError)"))
    }
}
