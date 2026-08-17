package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.lang.reflect.Proxy

/**
 * Narrow B1-D1 behavioral coverage for [SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook].
 *
 * D2 callback injection is not attempted here. Fatal semantics of
 * [tv.withaibuild.customiuizer.mods.utils.RuntimeFatality] are covered by
 * [tv.withaibuild.customiuizer.mods.utils.RuntimeFatalityTest]; the
 * ScreenshotFloatTimeHook catch block is pinned by
 * [MediaHookFailureBoundarySourceTest].
 */
class MediaHookFailureBoundaryTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        MainModule.mPrefs["system_gallery_screenshots_path"] = 2
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    @Test
    fun galleryClassMissing_doesNotThrowOrdinaryFailure() {
        SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(
            lpparam(hidingGalleryClassLoader())
        )
    }

    @Test
    fun galleryFieldMissing_doesNotThrowOrdinaryFailure() {
        SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(
            lpparam(parentClassLoader)
        )
    }

    @Test
    fun galleryOrdinaryLookupFailure_failOpen() {
        SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(
            lpparam(throwingGalleryClassLoader(IllegalStateException("ordinary lookup")))
        )
    }

    @Test
    fun galleryClassLookupOutOfMemoryError_propagates() {
        val failure = OutOfMemoryError("gallery oom")
        assertSame(
            failure,
            thrownFatal {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(
                    lpparam(throwingGalleryClassLoader(failure))
                )
            }
        )
    }

    @Test
    fun galleryClassLookupThreadDeath_propagates() {
        val failure = ThreadDeath()
        assertSame(
            failure,
            thrownFatal {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(
                    lpparam(throwingGalleryClassLoader(failure))
                )
            }
        )
    }

    @Test
    fun galleryClassLookupInternalError_propagates() {
        val failure = InternalError("gallery vm")
        assertSame(
            failure,
            thrownFatal {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(
                    lpparam(throwingGalleryClassLoader(failure))
                )
            }
        )
    }

    @Test
    fun galleryClassLookupWrappedFatal_propagatesOriginal() {
        val failure = OutOfMemoryError("wrapped gallery oom")
        assertSame(
            failure,
            thrownFatal {
                SystemAudioAndVisualAndMoreHooks.GalleryScreenshotPathHook(
                    lpparam(throwingGalleryClassLoader(RuntimeException(failure)))
                )
            }
        )
    }

    private fun lpparam(classLoader: ClassLoader): PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.miui.gallery"
                "getProcessName" -> "com.miui.gallery"
                "getClassLoader" -> classLoader
                else -> null
            }
        } as PackageReadyParam
    }

    private fun hidingGalleryClassLoader(): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == GALLERY_CONSTANTS_CLASS) {
                    throw ClassNotFoundException(name)
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    private fun throwingGalleryClassLoader(failure: Throwable): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == GALLERY_CONSTANTS_CLASS) {
                    when (failure) {
                        is Error -> throw failure
                        is RuntimeException -> throw failure
                        is ClassNotFoundException -> throw failure
                        else -> throw RuntimeException(failure)
                    }
                }
                return super.loadClass(name, resolve)
            }
        }
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

    companion object {
        private const val GALLERY_CONSTANTS_CLASS =
            "com.miui.gallery.storage.constants.MIUIStorageConstants"
    }
}
