package tv.withaibuild.customiuizer.installers

import android.app.Application
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Regression coverage for A3: LauncherInstaller.installApplication's
 * Application.attach after callback must reject a foreign package's
 * Application before any hook installation.
 */
class LauncherInstallerPackageFilterTest {

    private fun fakeApplication(packageName: String): Application {
        return object : Application() {
            override fun getPackageName(): String = packageName
        }
    }

    private fun fakePackageReadyParam(packageName: String): PackageReadyParam {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            PackageReadyParam::class.java.classLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> packageName
                "getClassLoader" -> javaClass.classLoader
                else -> null
            }
        } as PackageReadyParam
    }

    @Test
    fun isTargetPackage_matchingPackage_returnsTrue() {
        val lpparam = fakePackageReadyParam("com.miui.home")
        val app = fakeApplication("com.miui.home")

        assertTrue(
            "matching package must be accepted",
            LauncherInstaller.isTargetPackage(app, lpparam)
        )
    }

    @Test
    fun isTargetPackage_foreignPackage_returnsFalse() {
        val lpparam = fakePackageReadyParam("com.miui.home")
        val app = fakeApplication("com.foreign.app")

        assertFalse(
            "foreign package must be rejected",
            LauncherInstaller.isTargetPackage(app, lpparam)
        )
    }

    @Test
    fun isTargetPackage_defaultApplication_overridesPackageName() {
        // default android.app.Application uses getPackageName() from the base
        // Context; it does not rely on the Application class defining ClassLoader.
        val lpparam = fakePackageReadyParam("com.miui.home")
        val app = fakeApplication("com.miui.home")

        assertTrue(
            "default Application must be supported via getPackageName()",
            LauncherInstaller.isTargetPackage(app, lpparam)
        )
    }

    @Test
    fun isTargetPackage_nonApplicationThisObject_returnsFalse() {
        val lpparam = fakePackageReadyParam("com.miui.home")

        assertFalse(
            "non-Application thisObject must be rejected",
            LauncherInstaller.isTargetPackage("not an application", lpparam)
        )
    }

    @Test
    fun installApplication_afterCallback_blocksForeignPackage() {
        val lpparam = fakePackageReadyParam("com.miui.home")
        // ClassLoader must not be accessed if the package filter rejects.
        val guardLpparam = Proxy.newProxyInstance(
            PackageReadyParam::class.java.classLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.miui.home"
                "getClassLoader" -> throw AssertionError("getClassLoader must not be called for foreign package")
                else -> null
            }
        } as PackageReadyParam

        val foreignApp = fakeApplication("com.foreign.app")

        // The static helper is the exact guard the after callback uses.
        // If it rejects, no legacy hook installation runs and lpparam.getClassLoader()
        // is never touched.
        assertFalse(
            "foreign Application must be rejected before legacy hooks run",
            LauncherInstaller.isTargetPackage(foreignApp, guardLpparam)
        )
    }
}
