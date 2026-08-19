package tv.withaibuild.customiuizer.mods

import androidx.recyclerview.widget.RemixRecyclerView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import miuix.springback.view.SpringBackLayout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Proxy

/**
 * Narrow B2A-D2 behavioral coverage for [SystemAudioAndVisualAndMoreHooks.NoOverscrollAppHook].
 *
 * Ordinary callMethod failure still falls back to the boolean field write.
 * Wrapped fatals propagate the original instance from both SpringBackLayout
 * and RemixRecyclerView constructor after-callbacks.
 */
class NoOverscrollFailureBoundaryTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        SpringBackLayout.setSpringBackEnableFailure = null
        RemixRecyclerView.setSpringEnabledFailure = null
        SystemAudioAndVisualAndMoreHooks.NoOverscrollAppHook(lpparam())
    }

    @After
    fun tearDown() {
        SpringBackLayout.setSpringBackEnableFailure = null
        RemixRecyclerView.setSpringEnabledFailure = null
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
    }

    @Test
    fun springPrimaryOrdinaryFailure_fallsBackToFieldWrite() {
        SpringBackLayout.setSpringBackEnableFailure = IllegalStateException("spring method missing")
        val target = SpringBackLayout()
        target.mSpringBackEnable = true

        FakeXposedInterface.executeAfter(constructorHook(SpringBackLayout::class.java), target)

        assertFalse("fallback must still write mSpringBackEnable", target.mSpringBackEnable)
    }

    @Test
    fun springPrimaryWrappedFatal_propagatesOriginal() {
        val failure = OutOfMemoryError("spring wrapped oom")
        SpringBackLayout.setSpringBackEnableFailure = RuntimeException(failure)
        assertSame(
            failure,
            thrownFatal {
                FakeXposedInterface.executeAfter(
                    constructorHook(SpringBackLayout::class.java),
                    SpringBackLayout()
                )
            }
        )
    }

    @Test
    fun remixPrimaryOrdinaryFailure_fallsBackToFieldWrite() {
        RemixRecyclerView.setSpringEnabledFailure = IllegalStateException("remix method missing")
        val target = RemixRecyclerView()
        target.mSpringEnabled = true

        FakeXposedInterface.executeAfter(constructorHook(RemixRecyclerView::class.java), target)

        assertFalse("fallback must still write mSpringEnabled", target.mSpringEnabled)
    }

    @Test
    fun remixPrimaryWrappedFatal_propagatesOriginal() {
        val failure = OutOfMemoryError("remix wrapped oom")
        RemixRecyclerView.setSpringEnabledFailure = RuntimeException(failure)
        assertSame(
            failure,
            thrownFatal {
                FakeXposedInterface.executeAfter(
                    constructorHook(RemixRecyclerView::class.java),
                    RemixRecyclerView()
                )
            }
        )
    }

    @Test
    fun springFallbackOrdinaryFailure_failOpen() {
        FakeXposedInterface.executeAfter(
            constructorHook(SpringBackLayout::class.java),
            FallbackOnlyTarget()
        )
    }

    @Test
    fun remixFallbackOrdinaryFailure_failOpen() {
        FakeXposedInterface.executeAfter(
            constructorHook(RemixRecyclerView::class.java),
            RemixFallbackOnlyTarget()
        )
    }

    private fun constructorHook(type: Class<*>): FakeXposedInterface.RecordedHook {
        val recorded = FakeXposedInterface.recordedHooks.find {
            it.executable is Constructor<*> && it.executable.declaringClass == type
        }
        assertTrue("constructor hook for ${type.name} must be recorded", recorded != null)
        return recorded!!
    }

    private fun lpparam(): PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.example.app"
                "getProcessName" -> "com.example.app"
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

    /** No mSpringBackEnable field: fallback setBooleanField fails ordinarily. */
    private class FallbackOnlyTarget

    /** No mSpringEnabled field: fallback setBooleanField fails ordinarily. */
    private class RemixFallbackOnlyTarget
}
