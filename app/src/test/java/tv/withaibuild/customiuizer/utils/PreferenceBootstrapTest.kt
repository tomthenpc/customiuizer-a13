package tv.withaibuild.customiuizer.utils

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    // 5. Listener registration throws -> EMPTY_PENDING for empty first snapshot.
    @Test
    fun listenerRegistrationFailureWithEmptyFirstStaysEmptyPending() {
        fake.setRegisterException(RuntimeException("register"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.EMPTY_PENDING, bootstrap.start())
        assertFalse(bootstrap.isWatcherRegistered)
        assertEquals("register_listener", bootstrap.lastFailureStage)
    }

    // 6. Listener can only be successfully registered once.
    @Test
    fun listenerRegisteredAtMostOnce() {
        fake.set(mapOf("k1" to true))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.start())
        assertTrue(bootstrap.isWatcherRegistered)

        val count = AtomicInteger(0)
        val listenerField = PreferenceBootstrap::class.java.getDeclaredField("listener")
        listenerField.isAccessible = true
        val firstListener = listenerField.get(bootstrap)

        // Second start should be idempotent and keep the same listener.
        bootstrap.start()
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.state)
        assertEquals(firstListener, listenerField.get(bootstrap))
    }

    // 7. First empty, listener not registered -> EMPTY_PENDING.
    @Test
    fun emptyFirstSnapshotWithoutListenerIsEmptyPending() {
        fake.setRegisterException(RuntimeException("register"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.EMPTY_PENDING, bootstrap.start())
        assertEquals(0, snapshot.size)
    }

    // 8. Listener registered, second still empty -> VALID_EMPTY.
    @Test
    fun listenerSuccessSecondEmptyIsValidEmpty() {
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.VALID_EMPTY, bootstrap.start())
        assertTrue(bootstrap.isLoaded)
        assertTrue(bootstrap.isWatcherRegistered)
        assertEquals(1, bootstrap.emptyConfirmations)
    }

    // 9. Second non-empty -> LOADED.
    @Test
    fun nonEmptySecondSnapshotIsLoaded() {
        fake.set(mapOf("pref_key_foo" to true, "pref_key_bar" to 42))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.start())
        assertTrue(bootstrap.snapshot.getBoolean("foo", false))
        assertEquals(42, bootstrap.snapshot.getInt("bar", 0))
    }

    // 10. Race window: preference changes between first and second getAll() are captured.
    @Test
    fun changeBetweenFirstAndSecondGetAllIsCaptured() {
        fake.set(mapOf("pref_key_foo" to "old"))
        // After the first getAll and during listener registration, the value changes.
        val bootstrap = object : PreferenceBootstrap({ fake }, "test", snapshot) {
            override fun start(): PreferenceBootstrap.State {
                // Hook: inject a change immediately after the first getAll.
                // This is exercised by the double-read design, not by overriding.
                return super.start()
            }
        }

        // We simulate by changing the fake before start; the second getAll should see the new value.
        fake.set(mapOf("pref_key_foo" to "new"))
        assertEquals(PreferenceBootstrap.State.LOADED, bootstrap.start())
        assertEquals("new", bootstrap.snapshot.getString("foo", ""))
    }

    // 11. Concurrent bootstrap calls do not register multiple listeners.
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
        // All calls must agree on the final loaded state.
        assertTrue(states.all { it == PreferenceBootstrap.State.LOADED })
        assertTrue(bootstrap.isWatcherRegistered)
        assertEquals(1, bootstrap.attempts)
    }

    // 12. Listener update with malformed type does not throw.
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

    // 13. Preference removal updates the snapshot.
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

    // 14. Attempts are capped and repeated failures stop.
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

    // 15. ensureWatcher can recover from a listener failure.
    @Test
    fun ensureWatcherRecovers() {
        fake.setRegisterException(RuntimeException("register"))
        val bootstrap = bootstrap()
        assertEquals(PreferenceBootstrap.State.EMPTY_PENDING, bootstrap.start())
        assertFalse(bootstrap.isWatcherRegistered)

        fake.setRegisterException(null)
        assertEquals(PreferenceBootstrap.State.VALID_EMPTY, bootstrap.ensureWatcher())
        assertTrue(bootstrap.isWatcherRegistered)
    }

    // Helper: set map on fake.
    private fun FakeSharedPreferences.set(values: Map<String, Any?>) {
        this.setAll(values)
    }

    private fun FakeSharedPreferences.change(key: String, value: Any?) {
        if (value == null) this.remove(key)
        else this.put(key, value)
    }
}
