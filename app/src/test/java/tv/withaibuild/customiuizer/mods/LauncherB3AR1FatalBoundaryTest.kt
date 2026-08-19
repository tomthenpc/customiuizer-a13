package tv.withaibuild.customiuizer.mods

import android.content.ComponentName
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import java.lang.reflect.Proxy

/**
 * B3A-R1 D1: FixAppInfoLaunchHook fallback remains for ordinary ABI failure,
 * and wrapped fatals propagate the original instance without running fallback.
 */
class LauncherB3AR1FatalBoundaryTest {

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
    fun ordinaryPrimaryFailureStillRunsFallback() {
        LauncherSystemHooks.FixAppInfoLaunchHook(lpparam())
        val hook = requireNotNull(FakeXposedInterface.findHook(UTILITIES, "startDetailsActivityForInfo"))
        val item = ItemInfo(primaryFailure = IllegalStateException("ordinary abi"))

        FakeXposedInterface.executeBefore(hook, Any(), item, null)

        assertTrue("ordinary primary failure must still invoke intent.getComponent fallback", item.fallbackInvoked)
    }

    @Test
    fun wrappedOomPropagatesOriginalAndSkipsFallback() {
        LauncherSystemHooks.FixAppInfoLaunchHook(lpparam())
        val hook = requireNotNull(FakeXposedInterface.findHook(UTILITIES, "startDetailsActivityForInfo"))
        val failure = OutOfMemoryError("fixappinfo wrapped oom")
        val item = ItemInfo(primaryFailure = RuntimeException(failure))

        assertSame(failure, thrownFatal {
            FakeXposedInterface.executeBefore(hook, Any(), item, null)
        })
        assertFalse("wrapped OOM must not run the fallback lookup", item.fallbackInvoked)
    }

    @Test
    fun wrappedThreadDeathPropagatesOriginalAndSkipsFallback() {
        LauncherSystemHooks.FixAppInfoLaunchHook(lpparam())
        val hook = requireNotNull(FakeXposedInterface.findHook(UTILITIES, "startDetailsActivityForInfo"))
        val failure = ThreadDeath()
        val item = ItemInfo(primaryFailure = RuntimeException(failure))

        assertSame(failure, thrownFatal {
            FakeXposedInterface.executeBefore(hook, Any(), item, null)
        })
        assertFalse(item.fallbackInvoked)
    }

    @Test
    fun wrappedInternalErrorPropagatesOriginalAndSkipsFallback() {
        LauncherSystemHooks.FixAppInfoLaunchHook(lpparam())
        val hook = requireNotNull(FakeXposedInterface.findHook(UTILITIES, "startDetailsActivityForInfo"))
        val failure = InternalError("fixappinfo wrapped vm")
        val item = ItemInfo(primaryFailure = RuntimeException(failure))

        assertSame(failure, thrownFatal {
            FakeXposedInterface.executeBefore(hook, Any(), item, null)
        })
        assertFalse(item.fallbackInvoked)
    }

    private fun lpparam(): PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.mi.android.globallauncher"
                "getProcessName" -> "com.mi.android.globallauncher"
                "getClassLoader" -> parentClassLoader
                else -> null
            }
        } as PackageReadyParam
    }

    private inline fun thrownFatal(block: () -> Unit): Throwable {
        try {
            block()
            fail("expected fatal throwable")
            throw AssertionError("unreachable")
        } catch (oom: OutOfMemoryError) {
            return oom
        } catch (td: ThreadDeath) {
            return td
        } catch (vm: VirtualMachineError) {
            return vm
        }
    }

    class ItemInfo(private val primaryFailure: Throwable?) {
        var fallbackInvoked = false

        @JvmField
        val intent = IntentHolder()

        fun getComponentName(): ComponentName {
            primaryFailure?.let { throw it }
            return ComponentName("pkg.primary", "Act")
        }

        inner class IntentHolder {
            fun getComponent(): ComponentName {
                fallbackInvoked = true
                return ComponentName("pkg.fallback", "Act")
            }
        }
    }

    companion object {
        private const val UTILITIES = "com.miui.home.launcher.util.Utilities"
    }
}
