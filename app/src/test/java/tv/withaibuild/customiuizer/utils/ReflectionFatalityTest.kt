package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertSame
import org.junit.Test
import java.lang.reflect.InvocationTargetException

class ReflectionFatalityTest {

    @Test(expected = OutOfMemoryError::class)
    fun directOutOfMemoryErrorIsRethrown() {
        val error = OutOfMemoryError("oom")
        try {
            ReflectionFatality.rethrowIfFatal(error)
        } catch (thrown: Throwable) {
            assertSame(error, thrown)
            throw thrown
        }
    }

    @Test(expected = ThreadDeath::class)
    fun directThreadDeathIsRethrown() {
        val error = ThreadDeath()
        try {
            ReflectionFatality.rethrowIfFatal(error)
        } catch (thrown: Throwable) {
            assertSame(error, thrown)
            throw thrown
        }
    }

    @Test(expected = InternalError::class)
    fun directVirtualMachineErrorIsRethrown() {
        val error = InternalError("vm error")
        try {
            ReflectionFatality.rethrowIfFatal(error)
        } catch (thrown: Throwable) {
            assertSame(error, thrown)
            throw thrown
        }
    }

    @Test(expected = OutOfMemoryError::class)
    fun wrappedOutOfMemoryErrorCauseIsRethrown() {
        val cause = OutOfMemoryError("oom")
        val wrapped = InvocationTargetException(cause)
        try {
            ReflectionFatality.rethrowIfFatal(wrapped)
        } catch (thrown: Throwable) {
            assertSame(cause, thrown)
            throw thrown
        }
    }

    @Test(expected = ThreadDeath::class)
    fun wrappedThreadDeathCauseIsRethrown() {
        val cause = ThreadDeath()
        val wrapped = InvocationTargetException(cause)
        try {
            ReflectionFatality.rethrowIfFatal(wrapped)
        } catch (thrown: Throwable) {
            assertSame(cause, thrown)
            throw thrown
        }
    }

    @Test(expected = InternalError::class)
    fun wrappedVirtualMachineErrorCauseIsRethrown() {
        val cause = InternalError("vm error")
        val wrapped = InvocationTargetException(cause)
        try {
            ReflectionFatality.rethrowIfFatal(wrapped)
        } catch (thrown: Throwable) {
            assertSame(cause, thrown)
            throw thrown
        }
    }

    @Test
    fun directOrdinaryExceptionReturnsNormally() {
        ReflectionFatality.rethrowIfFatal(IllegalStateException("ordinary"))
    }

    @Test
    fun wrappedOrdinaryExceptionReturnsNormally() {
        val wrapped = InvocationTargetException(IllegalStateException("ordinary"))
        ReflectionFatality.rethrowIfFatal(wrapped)
    }

    @Test
    fun invocationTargetExceptionWithNullCauseReturnsNormally() {
        val wrapped = InvocationTargetException(null)
        ReflectionFatality.rethrowIfFatal(wrapped)
    }
}
