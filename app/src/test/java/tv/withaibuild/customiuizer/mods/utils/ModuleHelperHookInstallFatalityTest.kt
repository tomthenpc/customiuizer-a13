package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertSame
import org.junit.Test
import java.lang.reflect.InvocationTargetException

class ModuleHelperHookInstallFatalityTest {

    @Test
    fun directOOMIsRethrownAsOriginal() {
        val failure = OutOfMemoryError("oom")
        val thrown = assertFatality(failure)
        assertSame(failure, thrown)
    }

    @Test
    fun directThreadDeathIsRethrownAsOriginal() {
        val failure = ThreadDeath()
        val thrown = assertFatality(failure)
        assertSame(failure, thrown)
    }

    @Test
    fun directInternalErrorIsRethrownAsOriginal() {
        val failure = InternalError("internal")
        val thrown = assertFatality(failure)
        assertSame(failure, thrown)
    }

    @Test
    fun wrappedOOMRethrowsOriginal() {
        val failure = OutOfMemoryError("oom")
        val wrapped = RuntimeException(failure)
        val thrown = assertFatality(wrapped)
        assertSame(failure, thrown)
    }

    @Test
    fun wrappedThreadDeathRethrowsOriginal() {
        val failure = ThreadDeath()
        val wrapped = InvocationTargetException(failure)
        val thrown = assertFatality(wrapped)
        assertSame(failure, thrown)
    }

    @Test
    fun doubleWrappedInternalErrorRethrowsOriginal() {
        val failure = InternalError("internal")
        val once = RuntimeException(failure)
        val twice = RuntimeException(once)
        val thrown = assertFatality(twice)
        assertSame(failure, thrown)
    }

    @Test
    fun directOrdinaryThrowableReturnsNormally() {
        ModuleHelper.throwIfFatal(IllegalStateException("ordinary"))
    }

    @Test
    fun wrappedOrdinaryThrowableReturnsNormally() {
        val wrapped = RuntimeException(IllegalStateException("ordinary"))
        ModuleHelper.throwIfFatal(wrapped)
    }

    @Test
    fun assertionErrorReturnsNormally() {
        ModuleHelper.throwIfFatal(AssertionError("assert"))
    }

    @Test
    fun nullThrowableReturnsNormally() {
        ModuleHelper.throwIfFatal(null)
    }

    @Test
    fun fatalAtDepth8IsRethrown() {
        val failure = OutOfMemoryError("deep")
        val wrapped = wrap(failure, 7)
        val thrown = assertFatality(wrapped)
        assertSame(failure, thrown)
    }

    @Test
    fun fatalAtDepth9IsNotReached() {
        val failure = OutOfMemoryError("too deep")
        val wrapped = wrap(failure, 8)
        ModuleHelper.throwIfFatal(wrapped)
    }

    private fun assertFatality(throwable: Throwable): Throwable {
        try {
            ModuleHelper.throwIfFatal(throwable)
        } catch (t: Throwable) {
            return t
        }
        throw AssertionError("Expected fatal to be rethrown")
    }

    private fun wrap(cause: Throwable, times: Int): Throwable {
        var current = cause
        repeat(times) {
            current = RuntimeException(current)
        }
        return current
    }
}
