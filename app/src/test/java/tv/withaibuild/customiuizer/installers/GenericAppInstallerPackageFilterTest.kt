package tv.withaibuild.customiuizer.installers

import android.app.Application
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Regression coverage for A3: GenericAppInstaller.install's
 * Application.attach after callback must reject a foreign package's
 * Application before any of the four direct hook installations.
 */
class GenericAppInstallerPackageFilterTest {

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
        val lpparam = fakePackageReadyParam("com.example.app")
        val app = fakeApplication("com.example.app")

        assertTrue(
            "matching package must be accepted",
            GenericAppInstaller.isTargetPackage(app, lpparam)
        )
    }

    @Test
    fun isTargetPackage_foreignPackage_returnsFalse() {
        val lpparam = fakePackageReadyParam("com.example.app")
        val app = fakeApplication("com.foreign.app")

        assertFalse(
            "foreign package must be rejected",
            GenericAppInstaller.isTargetPackage(app, lpparam)
        )
    }

    @Test
    fun isTargetPackage_defaultApplication_supported() {
        val lpparam = fakePackageReadyParam("com.example.app")
        val app = fakeApplication("com.example.app")

        assertTrue(
            "default Application must be supported via getPackageName()",
            GenericAppInstaller.isTargetPackage(app, lpparam)
        )
    }

    @Test
    fun isTargetPackage_nonApplicationThisObject_returnsFalse() {
        val lpparam = fakePackageReadyParam("com.example.app")

        assertFalse(
            "non-Application thisObject must be rejected",
            GenericAppInstaller.isTargetPackage(42, lpparam)
        )
    }

    @Test
    fun install_afterCallback_blocksForeignPackage() {
        // ClassLoader must not be accessed if the package filter rejects.
        val guardLpparam = Proxy.newProxyInstance(
            PackageReadyParam::class.java.classLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.example.app"
                "getClassLoader" -> throw AssertionError("getClassLoader must not be called for foreign package")
                else -> null
            }
        } as PackageReadyParam

        val foreignApp = fakeApplication("com.foreign.app")

        assertFalse(
            "foreign Application must be rejected before GenericApp hooks run",
            GenericAppInstaller.isTargetPackage(foreignApp, guardLpparam)
        )
    }
}
