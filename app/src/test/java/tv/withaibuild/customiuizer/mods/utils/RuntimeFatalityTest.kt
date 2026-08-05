package tv.withaibuild.customiuizer.mods.utils

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class RuntimeFatalityTest {

    @Test
    fun directOutOfMemoryErrorIsRethrown() {
        val failure = OutOfMemoryError("oom")

        val thrown = try {
            RuntimeFatality.throwIfFatal(failure)
            fail("expected OutOfMemoryError")
            null
        } catch (oom: OutOfMemoryError) {
            oom
        }

        assertSame(failure, thrown)
    }

    @Test
    fun directThreadDeathIsRethrown() {
        val failure = ThreadDeath()

        val thrown = try {
            RuntimeFatality.throwIfFatal(failure)
            fail("expected ThreadDeath")
            null
        } catch (td: ThreadDeath) {
            td
        }

        assertSame(failure, thrown)
    }

    @Test
    fun directInternalErrorIsRethrown() {
        val failure = InternalError("internal")

        val thrown = try {
            RuntimeFatality.throwIfFatal(failure)
            fail("expected InternalError")
            null
        } catch (ie: InternalError) {
            ie
        }

        assertSame(failure, thrown)
    }

    @Test
    fun wrappedOutOfMemoryErrorRethrowsOriginalInstance() {
        val failure = OutOfMemoryError("oom")
        val wrapper = RuntimeException(failure)

        val thrown = try {
            RuntimeFatality.throwIfFatal(wrapper)
            fail("expected OutOfMemoryError")
            null
        } catch (oom: OutOfMemoryError) {
            oom
        }

        assertSame(failure, thrown)
    }

    @Test
    fun invocationTargetExceptionWithThreadDeathRethrowsOriginalInstance() {
        val failure = ThreadDeath()
        val wrapper = InvocationTargetException(failure)

        val thrown = try {
            RuntimeFatality.throwIfFatal(wrapper)
            fail("expected ThreadDeath")
            null
        } catch (td: ThreadDeath) {
            td
        }

        assertSame(failure, thrown)
    }

    @Test
    fun doubleWrappedInternalErrorRethrowsOriginalInstance() {
        val failure = InternalError("internal")
        val wrapper = RuntimeException(RuntimeException(failure))

        val thrown = try {
            RuntimeFatality.throwIfFatal(wrapper)
            fail("expected InternalError")
            null
        } catch (ie: InternalError) {
            ie
        }

        assertSame(failure, thrown)
    }

    @Test
    fun directOrdinaryExceptionReturnsNormally() {
        RuntimeFatality.throwIfFatal(IllegalStateException("ordinary"))
    }

    @Test
    fun wrappedOrdinaryExceptionReturnsNormally() {
        val cause = IllegalStateException("ordinary")
        val wrapper = RuntimeException(cause)
        RuntimeFatality.throwIfFatal(wrapper)
    }

    @Test
    fun directAssertionErrorReturnsNormally() {
        RuntimeFatality.throwIfFatal(AssertionError("assertion"))
    }

    @Test
    fun nullReturnsNormally() {
        RuntimeFatality.throwIfFatal(null)
    }

    @Test
    fun fatalAtDepthEightIsRethrown() {
        val failure = OutOfMemoryError("oom at depth 8")

        var current: Throwable = failure
        repeat(7) { depth ->
            current = RuntimeException("wrapper $depth", current)
        }

        val thrown = try {
            RuntimeFatality.throwIfFatal(current)
            fail("expected OutOfMemoryError")
            null
        } catch (oom: OutOfMemoryError) {
            oom
        }

        assertSame(failure, thrown)
    }

    @Test
    fun fatalAtDepthNineIsNotSearched() {
        val failure = OutOfMemoryError("oom at depth 9")

        var current: Throwable = failure
        repeat(8) { depth ->
            current = RuntimeException("wrapper $depth", current)
        }

        RuntimeFatality.throwIfFatal(current)
    }
}
