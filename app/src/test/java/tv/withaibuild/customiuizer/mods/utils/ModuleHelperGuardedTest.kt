package tv.withaibuild.customiuizer.mods.utils

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Callable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ModuleHelperGuardedTest {

    @After
    fun clearObservers() {
        ModuleHelper.prefObservers.clear()
        try {
            ModuleHelper::class.java.getDeclaredField("keyedPrefObservers").apply {
                isAccessible = true
            }.get(null).let {
                (it as? MutableMap<*, *>)?.clear()
            }
            ModuleHelper::class.java.getDeclaredField("ownedPrefObservers").apply {
                isAccessible = true
            }.get(null).let {
                (it as? MutableList<*>)?.clear()
            }
        } catch (_: Throwable) {
            // Best-effort cleanup for private static observer collections.
        }
    }

    @Test
    fun normalVoidCallbackRunsExactlyOnce() {
        var calls = 0

        ModuleHelper.guarded(Runnable { calls++ })

        assertEquals(1, calls)
    }

    @Test
    fun normalReturningCallbackKeepsItsResult() {
        val result = ModuleHelper.guarded("fallback", Callable { "result" })

        assertEquals("result", result)
    }

    @Test
    fun runtimeExceptionDoesNotEscape() {
        var reachedAfterGuard = false

        ModuleHelper.guarded(Runnable { throw IllegalStateException("runtime") })
        reachedAfterGuard = true

        assertTrue(reachedAfterGuard)
    }

    @Test
    fun nonExceptionThrowableDoesNotEscape() {
        var reachedAfterGuard = false

        ModuleHelper.guarded(Runnable { throw AssertionError("error") })
        reachedAfterGuard = true

        assertTrue(reachedAfterGuard)
    }

    @Test
    fun failureReturnsChosenFallbackWithoutRepeatingCallback() {
        var calls = 0

        val result = ModuleHelper.guarded("host-result", Callable<String> {
            calls++
            throw IllegalStateException("failure")
        })

        assertEquals("host-result", result)
        assertEquals(1, calls)
    }

    @Test
    fun returningCallbackDoesNotForceOneDefaultValue() {
        val consumed = ModuleHelper.guarded(false, Callable { true })
        val hostResult = ModuleHelper.guarded("host-result", Callable<String> {
            throw IllegalStateException("failure")
        })

        assertTrue(consumed)
        assertEquals("host-result", hostResult)
        assertFalse(ModuleHelper.guarded(false, Callable<Boolean> {
            throw IllegalStateException("failure")
        }))
    }

    @Test
    fun namedGuardLogsFailureExactlyOnce() {
        var logs = 0
        var loggedName: String? = null
        var loggedFailure: Throwable? = null
        val failure = IllegalStateException("failure")

        ModuleHelper.guarded(
            "SystemUI.testCallback",
            Runnable { throw failure },
            ModuleHelper.CallbackFailureLogger { callbackName, throwable ->
                logs++
                loggedName = callbackName
                loggedFailure = throwable
            },
        )

        assertEquals(1, logs)
        assertEquals("SystemUI.testCallback", loggedName)
        assertSame(failure, loggedFailure)
    }

    @Test
    fun repeatedNamedFailuresAreLoggedOnlyOncePerProcess() {
        var logs = 0
        val callbackName = "SystemUI.repeatedFailure.${System.nanoTime()}"
        val logger = ModuleHelper.CallbackFailureLogger { _, _ -> logs++ }

        repeat(3) {
            ModuleHelper.guarded(
                callbackName,
                Runnable { throw IllegalStateException("failure-$it") },
                logger,
            )
        }

        assertEquals(1, logs)
    }

    @Test
    fun failingPreferenceObserverDoesNotBlockLaterObserver() {
        var firstCalls = 0
        var secondCalls = 0
        ModuleHelper.observePreferenceChange {
            firstCalls++
            throw IllegalStateException("observer")
        }
        ModuleHelper.observePreferenceChange { secondCalls++ }

        ModuleHelper.handlePreferenceChanged("key")

        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test
    fun preferenceObserverOutOfMemoryIsRethrown() {
        val failure = OutOfMemoryError("observer oom")
        var laterCalls = 0
        ModuleHelper.observePreferenceChange {
            throw failure
        }
        ModuleHelper.observePreferenceChange { laterCalls++ }

        val thrown = assertThrows(OutOfMemoryError::class.java) {
            ModuleHelper.handlePreferenceChanged("key")
        }

        assertSame(failure, thrown)
        assertEquals(0, laterCalls)
    }

    @Test
    fun preferenceObserverThreadDeathIsRethrown() {
        val failure = ThreadDeath()
        var laterCalls = 0
        ModuleHelper.observePreferenceChange {
            throw failure
        }
        ModuleHelper.observePreferenceChange { laterCalls++ }

        val thrown = assertThrows(ThreadDeath::class.java) {
            ModuleHelper.handlePreferenceChanged("key")
        }

        assertSame(failure, thrown)
        assertEquals(0, laterCalls)
    }

    @Test
    fun preferenceObserverVirtualMachineErrorIsRethrown() {
        val failure = InternalError("observer vme")
        var laterCalls = 0
        ModuleHelper.observePreferenceChange {
            throw failure
        }
        ModuleHelper.observePreferenceChange { laterCalls++ }

        val thrown = assertThrows(VirtualMachineError::class.java) {
            ModuleHelper.handlePreferenceChanged("key")
        }

        assertSame(failure, thrown)
        assertEquals(0, laterCalls)
    }

    @Test
    fun unkeyedObserverWrappedOutOfMemoryIsRethrownOriginal() {
        val failure = OutOfMemoryError("wrapped oom")
        val wrapper = RuntimeException(failure)
        var laterCalls = 0

        ModuleHelper.observePreferenceChange {
            throw wrapper
        }
        ModuleHelper.observePreferenceChange { laterCalls++ }

        val thrown = assertThrows(OutOfMemoryError::class.java) {
            ModuleHelper.handlePreferenceChanged("key")
        }

        assertSame(failure, thrown)
        assertEquals(0, laterCalls)
    }

    @Test
    fun keyedObserverWrappedThreadDeathIsRethrownOriginal() {
        val failure = ThreadDeath()
        val wrapper = InvocationTargetException(failure)
        val key = "keyed-wrapped-td-${System.nanoTime()}"

        ModuleHelper.observePreferenceChange(key) {
            throw wrapper
        }

        val thrown = assertThrows(ThreadDeath::class.java) {
            ModuleHelper.handlePreferenceChanged("trigger")
        }

        assertSame(failure, thrown)

        ModuleHelper.observePreferenceChange(key, null)
    }

    @Test
    fun ownedObserverDoubleWrappedInternalErrorIsRethrownOriginal() {
        val failure = InternalError("double wrapped internal error")
        val wrapper = RuntimeException(RuntimeException(failure))
        var laterCalls = 0
        val key = "owned-wrapped-ie-${System.nanoTime()}"
        val owner1 = Any()
        val owner2 = Any()

        ModuleHelper.observeOwnedPreferenceChange(key, owner1) { _, _ ->
            throw wrapper
        }
        ModuleHelper.observeOwnedPreferenceChange(key, owner2) { _, _ -> laterCalls++ }

        val thrown = assertThrows(VirtualMachineError::class.java) {
            ModuleHelper.handlePreferenceChanged(key)
        }

        assertSame(failure, thrown)
        assertEquals(0, laterCalls)

        ModuleHelper.removePreferenceObserver(key, owner1)
        ModuleHelper.removePreferenceObserver(key, owner2)
    }

    @Test
    fun wrappedOrdinaryUnkeyedObserverFailureDoesNotEscape() {
        val cause = IllegalStateException("cause")
        val wrapper = RuntimeException(cause)
        var firstCalls = 0
        var secondCalls = 0

        ModuleHelper.observePreferenceChange {
            firstCalls++
            throw wrapper
        }
        ModuleHelper.observePreferenceChange { secondCalls++ }

        ModuleHelper.handlePreferenceChanged("key")

        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test
    fun wrappedAssertionErrorUnkeyedObserverFailureDoesNotEscape() {
        val cause = AssertionError("assertion")
        val wrapper = RuntimeException(cause)
        var firstCalls = 0
        var secondCalls = 0

        ModuleHelper.observePreferenceChange {
            firstCalls++
            throw wrapper
        }
        ModuleHelper.observePreferenceChange { secondCalls++ }

        ModuleHelper.handlePreferenceChanged("key")

        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test
    fun guardedRunnableWrappedOutOfMemoryIsRethrownOriginal() {
        val failure = OutOfMemoryError("guarded runnable oom")
        val wrapper = RuntimeException(failure)

        val thrown = assertThrows(OutOfMemoryError::class.java) {
            ModuleHelper.guarded(Runnable { throw wrapper })
        }

        assertSame(failure, thrown)
    }

    @Test
    fun namedGuardedWrappedThreadDeathIsRethrownOriginalAndLoggerNotCalled() {
        val failure = ThreadDeath()
        val wrapper = InvocationTargetException(failure)
        var logs = 0

        val thrown = assertThrows(ThreadDeath::class.java) {
            ModuleHelper.guarded(
                "SystemUI.wrappedTd.${System.nanoTime()}" ,
                Runnable { throw wrapper },
                ModuleHelper.CallbackFailureLogger { _, _ -> logs++ },
            )
        }

        assertSame(failure, thrown)
        assertEquals(0, logs)
    }

    @Test
    fun returningGuardedDoubleWrappedInternalErrorIsRethrownOriginal() {
        val failure = InternalError("returning guarded internal error")
        val wrapper = RuntimeException(RuntimeException(failure))

        val thrown = assertThrows(VirtualMachineError::class.java) {
            ModuleHelper.guarded(
                "fallback",
                Callable<String> { throw wrapper },
            )
        }

        assertSame(failure, thrown)
    }

    @Test
    fun namedReturningGuardedWrappedOutOfMemoryIsRethrownOriginalAndLoggerNotCalled() {
        val failure = OutOfMemoryError("named returning oom")
        val wrapper = RuntimeException(failure)
        var logs = 0

        val thrown = assertThrows(OutOfMemoryError::class.java) {
            ModuleHelper.guarded(
                "SystemUI.wrappedOom.${System.nanoTime()}" ,
                "fallback",
                Callable<String> { throw wrapper },
                ModuleHelper.CallbackFailureLogger { _, _ -> logs++ },
            )
        }

        assertSame(failure, thrown)
        assertEquals(0, logs)
    }

    @Test
    fun guardedRunnableWrappedOrdinaryExceptionDoesNotEscape() {
        var reachedAfterGuard = false

        ModuleHelper.guarded(Runnable { throw RuntimeException(IllegalStateException("wrapped runtime")) })
        reachedAfterGuard = true

        assertTrue(reachedAfterGuard)
    }

    @Test
    fun returningGuardedWrappedOrdinaryExceptionReturnsFallback() {
        val fallback = "fallback"

        val result = ModuleHelper.guarded(
            fallback,
            Callable<String> { throw RuntimeException(IllegalStateException("wrapped runtime")) },
        )

        assertSame(fallback, result)
    }

    @Test
    fun namedGuardedOrdinaryWrappedFailureLoggerReceivesWrapperAndOnlyOnce() {
        val cause = IllegalStateException("cause")
        val wrapper = RuntimeException(cause)
        var logs = 0
        var loggedFailure: Throwable? = null
        val callbackName = "SystemUI.wrapperLogger.${System.nanoTime()}"

        repeat(3) {
            ModuleHelper.guarded(
                callbackName,
                Runnable { throw wrapper },
                ModuleHelper.CallbackFailureLogger { _, failure ->
                    logs++
                    loggedFailure = failure
                },
            )
        }

        assertEquals(1, logs)
        assertSame(wrapper, loggedFailure)
        assertSame(cause, loggedFailure?.cause)
    }

    @Test
    fun returningGuardedWrappedAssertionErrorReturnsFallback() {
        val fallback = "fallback"

        val result = ModuleHelper.guarded(
            fallback,
            Callable<String> { throw RuntimeException(AssertionError("wrapped assertion")) },
        )

        assertSame(fallback, result)
    }
}
