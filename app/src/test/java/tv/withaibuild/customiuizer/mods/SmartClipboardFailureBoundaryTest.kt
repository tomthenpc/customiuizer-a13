package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

/**
 * Narrow B2A-D1 behavioral coverage for [Various.SmartClipboardActionHook].
 *
 * Missing [SECURITY_PROMPT_HANDLER] must fail-open after the ClipboardTipDialog
 * sub-hook has already been installed. Fatal JVM errors still propagate.
 */
class SmartClipboardFailureBoundaryTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        MainModule.mPrefs["various_clipboard_defaultaction"] = 2
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    @Test
    fun securityPromptHandlerMissing_failOpen_andClipboardDialogHookPreserved() {
        Various.SmartClipboardActionHook(lpparam(hidingPromptHandlerClassLoader()))

        val dialogHook = FakeXposedInterface.recordedHooks.find {
            it.executable.declaringClass.name == CLIPBOARD_TIP_DIALOG &&
                it.executable.name == "customReadClipboardDialog"
        }
        assertNotNull(
            "ClipboardTipDialog hook must remain installed when SecurityPromptHandler is missing",
            dialogHook
        )
        assertTrue(
            "SecurityPromptHandler must not be hooked when the class is missing",
            FakeXposedInterface.recordedHooks.none {
                it.executable.declaringClass.name == SECURITY_PROMPT_HANDLER
            }
        )
    }

    @Test
    fun securityPromptHandlerOrdinaryLookupFailure_failOpen() {
        Various.SmartClipboardActionHook(
            lpparam(throwingPromptHandlerClassLoader(IllegalStateException("ordinary lookup")))
        )
    }

    @Test
    fun securityPromptHandlerOutOfMemoryError_propagates() {
        val failure = OutOfMemoryError("clipboard oom")
        assertSame(
            failure,
            thrownFatal {
                Various.SmartClipboardActionHook(lpparam(throwingPromptHandlerClassLoader(failure)))
            }
        )
    }

    @Test
    fun securityPromptHandlerThreadDeath_propagates() {
        val failure = ThreadDeath()
        assertSame(
            failure,
            thrownFatal {
                Various.SmartClipboardActionHook(lpparam(throwingPromptHandlerClassLoader(failure)))
            }
        )
    }

    @Test
    fun securityPromptHandlerInternalError_propagates() {
        val failure = InternalError("clipboard vm")
        assertSame(
            failure,
            thrownFatal {
                Various.SmartClipboardActionHook(lpparam(throwingPromptHandlerClassLoader(failure)))
            }
        )
    }

    @Test
    fun securityPromptHandlerWrappedFatal_propagatesOriginal() {
        val failure = OutOfMemoryError("wrapped clipboard oom")
        assertSame(
            failure,
            thrownFatal {
                Various.SmartClipboardActionHook(
                    lpparam(throwingPromptHandlerClassLoader(RuntimeException(failure)))
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
                "getPackageName" -> "com.lbe.security.miui"
                "getProcessName" -> "com.lbe.security.miui"
                "getClassLoader" -> classLoader
                else -> null
            }
        } as PackageReadyParam
    }

    private fun hidingPromptHandlerClassLoader(): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == SECURITY_PROMPT_HANDLER) {
                    throw ClassNotFoundException(name)
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    private fun throwingPromptHandlerClassLoader(failure: Throwable): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == SECURITY_PROMPT_HANDLER) {
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
        private const val SECURITY_PROMPT_HANDLER =
            "com.lbe.security.ui.SecurityPromptHandler"
        private const val CLIPBOARD_TIP_DIALOG =
            "com.lbe.security.ui.ClipboardTipDialog"
    }
}
