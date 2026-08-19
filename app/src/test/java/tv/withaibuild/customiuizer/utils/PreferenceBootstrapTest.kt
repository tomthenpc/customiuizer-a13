package tv.withaibuild.customiuizer.utils

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

class PreferenceBootstrapTest {

    private lateinit var snapshot: PrefMap<String, Any>
    private lateinit var fake: FakeSharedPreferences

    @Before
    fun setup() {
        snapshot = PrefMap()
        fake = FakeSharedPreferences()
    }

    private fun bootstrap(): PreferenceBootstrap {
        return PreferenceBootstrap({ fake }, "test_remote", snapshot)
    }

    // 1. Initial state is UNINITIALIZED.
    @Test
    fun initialStateIsUninitialized() {
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.UNINITIALIZED, bootstrap.state)
        assertNull(bootstrap.remotePreferences)
        assertFalse(bootstrap.isLoaded)
    }

    // 2. RemotePreferences provider returns null -> UNAVAILABLE.
    @Test
    fun nullRemoteGoesUnavailable() {
        val bootstrap = PreferenceBootstrap({ null }, "test", snapshot)
        assertEquals(PreferenceBootstrap.State.UNAVAILABLE, bootstrap.start())
        assertEquals("resolve_remote_null", bootstrap.lastFailureStage)
        assertEquals(PreferenceBootstrap.State.UNAVAILABLE, bootstrap.state)
    }

    // 3. Provider throws -> UNAVAILABLE.
    @Test
    fun throwingProviderGoesUnavailable() {
        val error = RuntimeException("provider")
        val bootstrap = PreferenceBootstrap({ throw error }, "test", snapshot)
        assertEquals(PreferenceBootstrap.State.UNAVAILABLE, bootstrap.start())
        assertEquals("resolve_remote", bootstrap.lastFailureStage)
        assertNotNull(bootstrap.lastError)
    }

    // 4. First getAll() throws -> UNAVAILABLE.
    @Test
    fun firstGetAllThrowsGoesUnavailable() {
        fake.setGetAllException(RuntimeException("getAll"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.UNAVAILABLE, bootstrap.start())
        assertEquals("first_getAll", bootstrap.lastFailureStage)
    }

    // 5. Listener registration fails with non-empty second snapshot -> SNAPSHOT_PENDING_LISTENER, not LOADED.
    @Test
    fun listenerRegistrationFailureWithNonEmptyStaysSnapshotPendingListener() {
        fake.set(mapOf("pref_key_foo" to true, "pref_key_bar" to 42))
        fake.setRegisterException(RuntimeException("register"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.SNAPSHOT_PENDING_LISTENER, bootstrap.start())
        assertFalse(bootstrap.isLoaded)
        assertFalse(bootstrap.isWatcherRegistered)
        assertEquals("register_listener", bootstrap.lastFailureStage)
    }

    // 6. Listener registration fails with empty second snapshot -> SNAPSHOT_PENDING_LISTENER.
    @Test
    fun listenerRegistrationFailureWithEmptyStaysSnapshotPendingListener() {
        fake.setRegisterException(RuntimeException("register"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.SNAPSHOT_PENDING_LISTENER, bootstrap.start())
        assertFalse(bootstrap.isLoaded)
    }

    // 7. Listener can only be successfully registered once.
    @Test
    fun listenerRegisteredAtMostOnce() {
        fake.set(mapOf("k1" to true))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.start())
        assertTrue(bootstrap.isWatcherRegistered)

        val listenerField = PreferenceBootstrap::class.java.getDeclaredField("listener")
        listenerField.isAccessible = true
        val firstListener = listenerField.get(bootstrap)

        // Second start should be idempotent and keep the same listener.
        bootstrap.start()
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.state)
        assertEquals(firstListener, listenerField.get(bootstrap))
    }

    // 8. First empty, listener not registered -> SNAPSHOT_PENDING_LISTENER.
    @Test
    fun emptyFirstSnapshotWithoutListenerIsSnapshotPendingListener() {
        fake.setRegisterException(RuntimeException("register"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.SNAPSHOT_PENDING_LISTENER, bootstrap.start())
        assertEquals(0, snapshot.size)
    }

    // 9. Listener registered, second still empty -> VALID_EMPTY.
    @Test
    fun listenerSuccessSecondEmptyIsValidEmpty() {
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.VALID_EMPTY, bootstrap.start())
        assertTrue(bootstrap.isLoaded)
        assertTrue(bootstrap.isWatcherRegistered)
        assertEquals(1, bootstrap.emptyConfirmations)
    }

    // 10. Second non-empty -> LOADED.
    @Test
    fun nonEmptySecondSnapshotIsLoaded() {
        fake.set(mapOf("pref_key_foo" to true, "pref_key_bar" to 42))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.start())
        assertTrue(bootstrap.snapshot.getBoolean("foo", false))
        assertEquals(42, bootstrap.snapshot.getInt("bar", 0))
    }

    // 11. Race window: preference changes between first and second getAll() are captured.
    @Test
    fun changeBetweenFirstAndSecondGetAllIsCaptured() {
        fake.set(mapOf("pref_key_foo" to "old"))
        fake.set(mapOf("pref_key_foo" to "new"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.start())
        assertEquals("new", bootstrap.snapshot.getString("foo", ""))
    }

    // 12. Concurrent bootstrap calls do not register multiple listeners.
    @Test
    fun concurrentStartIsIdempotent() {
        fake.set(mapOf("pref_key_foo" to true))
        val bootstrap = bootstrap()
        val barrier = CyclicBarrier(4)
        val done = CountDownLatch(4)
        val states = mutableListOf<PreferenceBootstrap.State>()

        repeat(4) {
            Thread {
                barrier.await()
                val s = bootstrap.start()
                synchronized(states) { states.add(s) }
                done.countDown()
            }.start()
        }

        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertTrue(states.all { it == PreferenceBootstrap.State.LOADED })
        assertTrue(bootstrap.isWatcherRegistered)
        assertEquals(1, bootstrap.attempts)
    }

    // 13. Listener update with malformed type does not throw.
    @Test
    fun malformedListenerUpdateDoesNotThrow() {
        fake.set(mapOf("pref_key_foo" to 42))
        val bootstrap = bootstrap()
        bootstrap.start()

        // Simulate remote storing a String under a key previously holding an Int.
        fake.set(mapOf("pref_key_foo" to "now string"))
        fake.change("pref_key_foo", "now string")

        assertEquals("now string", snapshot.getString("foo", ""))
    }

    // 14. Listener getAll() throwing is guarded and does not propagate.
    @Test
    fun listenerGetAllExceptionDoesNotPropagate() {
        fake.set(mapOf("pref_key_foo" to true))
        val bootstrap = bootstrap()
        bootstrap.start()

        fake.setGetAllException(RuntimeException("listener_getAll"))
        fake.set(mapOf("pref_key_foo" to false))
        fake.change("pref_key_foo", false)

        // Snapshot must not have been corrupted or updated with a stale value.
        assertTrue(snapshot.getBoolean("foo", false))
    }

    // 15. Preference removal updates the snapshot.
    @Test
    fun preferenceRemovalUpdatesSnapshot() {
        fake.set(mapOf("pref_key_foo" to true, "pref_key_bar" to 1))
        val bootstrap = bootstrap()
        bootstrap.start()

        fake.set(mapOf("pref_key_bar" to 1))
        fake.change("pref_key_foo", null)

        assertFalse("foo should be removed; keys=${snapshot.keys}", snapshot.getBoolean("foo", false))
        assertEquals("bar should remain; keys=${snapshot.keys}", 1, snapshot.getInt("bar", 0))
    }

    // 16. Removing the last key transitions to VALID_EMPTY.
    @Test
    fun removingLastKeyTransitionsToValidEmpty() {
        fake.set(mapOf("pref_key_foo" to true))
        val bootstrap = bootstrap()
        bootstrap.start()

        fake.set(emptyMap())
        fake.remove("pref_key_foo")

        assertEquals(PreferenceBootstrap.State.VALID_EMPTY, bootstrap.state)
    }

    // 17. Adding the first key transitions to LOADED.
    @Test
    fun addingFirstKeyTransitionsToLoaded() {
        val bootstrap = bootstrap()
        bootstrap.start()
        assertEquals(PreferenceBootstrap.State.VALID_EMPTY, bootstrap.state)

        fake.set(mapOf("pref_key_foo" to true))
        fake.put("pref_key_foo", true)

        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.state)
    }

    // 18. Attempts are capped and repeated failures stop.
    @Test
    fun attemptsAreCapped() {
        fake.setGetAllException(RuntimeException("broken"))
        val bootstrap = bootstrap()
        bootstrap.start()
        bootstrap.start()
        bootstrap.start()
        bootstrap.start()
        assertEquals(3, bootstrap.attempts)
    }

    // 19. ensureWatcher can recover from a listener failure and publish a stable snapshot.
    @Test
    fun ensureWatcherRecovers() {
        fake.setRegisterException(RuntimeException("register"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.SNAPSHOT_PENDING_LISTENER, bootstrap.start())
        assertFalse(bootstrap.isWatcherRegistered)

        fake.setRegisterException(null)
        assertEquals(PreferenceBootstrap.State.VALID_EMPTY, bootstrap.ensureWatcher())
        assertTrue(bootstrap.isWatcherRegistered)
    }

    // 20. Type change from Int to String is safe.
    @Test
    fun typeChangeFromIntToStringIsSafe() {
        fake.set(mapOf("pref_key_foo" to 42))
        val bootstrap = bootstrap()
        bootstrap.start()
        assertEquals(42, snapshot.getInt("foo", 0))

        fake.set(mapOf("pref_key_foo" to "string"))
        fake.change("pref_key_foo", "string")

        assertEquals("string", snapshot.getString("foo", ""))
    }

    // 21. Type change from String to Boolean is safe.
    @Test
    fun typeChangeFromStringToBooleanIsSafe() {
        fake.set(mapOf("pref_key_foo" to "yes"))
        val bootstrap = bootstrap()
        bootstrap.start()
        assertEquals("yes", snapshot.getString("foo", ""))

        fake.set(mapOf("pref_key_foo" to true))
        fake.change("pref_key_foo", true)

        assertTrue(snapshot.getBoolean("foo", false))
    }

    // 22. OutOfMemoryError from provider must escape (canonical fatal contract).
    @Test
    fun providerOutOfMemoryErrorEscapes() {
        val bootstrap = PreferenceBootstrap({ throw OutOfMemoryError("provider oom") }, "test", snapshot)
        try {
            bootstrap.start()
            fail("OutOfMemoryError should have been rethrown")
        } catch (e: OutOfMemoryError) {
            assertEquals("provider oom", e.message)
        }
        assertEquals(PreferenceBootstrap.State.UNINITIALIZED, bootstrap.state)
    }

    // 23. ThreadDeath from first getAll must escape.
    @Test
    fun firstGetAllThreadDeathEscapes() {
        fake.setGetAllException(ThreadDeath())
        val bootstrap = bootstrap()
        try {
            bootstrap.start()
            fail("ThreadDeath should have been rethrown")
        } catch (e: ThreadDeath) {
            // expected
        }
    }

    // 24. VirtualMachineError subtype from listener registration must escape.
    @Test
    fun listenerRegistrationVirtualMachineErrorEscapes() {
        fake.set(mapOf("pref_key_foo" to true))
        fake.setRegisterException(InternalError("registration vm error"))
        val bootstrap = bootstrap()
        try {
            bootstrap.start()
            fail("VirtualMachineError should have been rethrown")
        } catch (e: InternalError) {
            assertEquals("registration vm error", e.message)
        }
    }

    // 25. Wrapped fatal cause must still escape through bounded cause-chain traversal.
    @Test
    fun wrappedFatalCauseEscapes() {
        val root = OutOfMemoryError("root oom")
        val wrapped = RuntimeException("wrapper", root)
        val bootstrap = PreferenceBootstrap({ throw wrapped }, "test", snapshot)
        try {
            bootstrap.start()
            fail("wrapped OutOfMemoryError cause should have been rethrown")
        } catch (e: OutOfMemoryError) {
            assertEquals("root oom", e.message)
        }
    }

    // 26. Ordinary RuntimeException still fails open with existing state and failure record.
    @Test
    fun ordinaryRuntimeExceptionStillFailOpen() {
        val error = RuntimeException("ordinary provider failure")
        val bootstrap = PreferenceBootstrap({ throw error }, "test", snapshot)
        assertEquals(PreferenceBootstrap.State.UNAVAILABLE, bootstrap.start())
        assertEquals("resolve_remote", bootstrap.lastFailureStage)
        assertEquals(error, bootstrap.lastError)
    }
}
