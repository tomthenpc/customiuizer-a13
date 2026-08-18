package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import java.lang.reflect.Proxy

class PackagePermissionsB3CTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
    }

    @Test
    fun missingDefaultGrantPolicy_stillInstallsPermissionHooks() {
        PackagePermissions.hook(lpparam(hidingClassLoader(DEFAULT_GRANT)))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == PERMISSION_IMPL &&
                    it.executable.name == "shouldGrantPermissionBySignature"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.none {
                it.executable.declaringClass.name == DEFAULT_GRANT
            }
        )
    }

    @Test
    fun wrappedOomOnDefaultGrantPolicy_propagatesOriginal() {
        val oom = OutOfMemoryError("wrapped-grant")
        try {
            PackagePermissions.hook(
                lpparam(throwingClassLoader(DEFAULT_GRANT, RuntimeException(oom)))
            )
            fail("expected wrapped OutOfMemoryError")
        } catch (t: OutOfMemoryError) {
            assertSame(oom, t)
        }
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == PERMISSION_IMPL &&
                    it.executable.name == "shouldGrantPermissionBySignature"
            }
        )
    }

    private fun lpparam(classLoader: ClassLoader): SystemServerStartingParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(SystemServerStartingParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getClassLoader" -> classLoader
                else -> null
            }
        } as SystemServerStartingParam
    }

    private fun hidingClassLoader(hidden: String): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == hidden) {
                    throw ClassNotFoundException(name)
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    private fun throwingClassLoader(target: String, failure: Throwable): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == target) {
                    throw failure
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    companion object {
        private const val PERMISSION_IMPL =
            "com.android.server.pm.permission.PermissionManagerServiceImpl"
        private const val DEFAULT_GRANT =
            "com.android.server.pm.MiuiDefaultPermissionGrantPolicy"
    }
}
