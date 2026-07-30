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
