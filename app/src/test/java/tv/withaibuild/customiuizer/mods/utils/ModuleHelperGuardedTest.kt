package tv.withaibuild.customiuizer.mods.utils

import java.util.concurrent.Callable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleHelperGuardedTest {

    @After
    fun clearObservers() {
        ModuleHelper.prefObservers.clear()
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
        assertTrue(loggedFailure === failure)
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
}
