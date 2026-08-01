package tv.withaibuild.customiuizer.mods.utils

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger

class ModuleHelperReceiverTest {

    @org.junit.After
    fun clearRegistries() {
        clearCollection("ownedReceivers")
        clearCollection("staleOwnedReceivers")
        clearCollection("moduleReceivers")
        clearCollection("staleModuleReceivers")
    }

    private fun clearCollection(fieldName: String) {
        val field = ModuleHelper::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        when (val value = field.get(null)) {
            is MutableMap<*, *> -> value.clear()
            is MutableCollection<*> -> value.clear()
            else -> error("Unsupported registry: $fieldName")
        }
    }

    private class TrackableContext : Application() {
        val registeredReceivers = ArrayList<Pair<String, BroadcastReceiver>>()
        val unregisteredReceivers = ArrayList<BroadcastReceiver>()

        var registerDelayMs = 0L
        var failNextRegister = false
        var failNextUnregister = false
        var oomNextRegister = false

        override fun getApplicationContext(): Context = this

        override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {
            return registerReceiver(receiver, filter, null, null, Context.RECEIVER_NOT_EXPORTED)
        }

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            flags: Int
        ): Intent? {
            if (oomNextRegister) {
                oomNextRegister = false
                throw OutOfMemoryError("simulated register OOM")
            }
            if (failNextRegister) {
                failNextRegister = false
                throw IllegalStateException("simulated register failure")
            }
            if (registerDelayMs > 0L) {
                Thread.sleep(registerDelayMs)
            }
            if (receiver != null) registeredReceivers.add("" to receiver)
            return Intent("stub")
        }

        override fun unregisterReceiver(receiver: BroadcastReceiver?) {
            if (failNextUnregister) {
                failNextUnregister = false
                throw IllegalStateException("simulated unregister failure")
            }
            if (receiver != null) unregisteredReceivers.add(receiver)
        }
    }

    private class StubReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {}
    }

    private fun intentFilter(action: String) = IntentFilter(action).apply { addAction(action) }

    @Test
    fun moduleReceiver_singleThreadRegistersAndUnregisters() {
        val context = TrackableContext()
        val receiver = StubReceiver()

        val ok = ModuleHelper.registerModuleReceiver(
            context,
            "testKey",
            receiver,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        assertTrue(ok)
        assertEquals(1, context.registeredReceivers.size)

        ModuleHelper.unregisterModuleReceiver("testKey")
        assertEquals(1, context.unregisteredReceivers.size)
        assertTrue(context.unregisteredReceivers.contains(receiver))
    }

    @Test
    fun moduleReceiver_registrationFailureDoesNotLeak() {
        val context = TrackableContext()
        context.failNextRegister = true
        val receiver = StubReceiver()

        val ok = ModuleHelper.registerModuleReceiver(
            context,
            "testKey",
            receiver,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        assertFalse(ok)
        assertEquals(0, context.registeredReceivers.size)
    }

    @Test
    fun moduleReceiver_failedReplacementKeepsPreviousRegistration() {
        val context = TrackableContext()
        val first = StubReceiver()

        assertTrue(
            ModuleHelper.registerModuleReceiver(
                context,
                "testKey",
                first,
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
        )
        context.failNextRegister = true

        assertFalse(
            ModuleHelper.registerModuleReceiver(
                context,
                "testKey",
                StubReceiver(),
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
        )
        assertFalse(context.unregisteredReceivers.contains(first))

        ModuleHelper.unregisterModuleReceiver("testKey")
        assertTrue(context.unregisteredReceivers.contains(first))
    }

    @Test
    fun moduleReceiver_registrationRethrowsOutOfMemoryError() {
        val context = TrackableContext().apply { oomNextRegister = true }
        var thrown = false

        try {
            ModuleHelper.registerModuleReceiver(
                context,
                "testKey",
                StubReceiver(),
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } catch (_: OutOfMemoryError) {
            thrown = true
        }

        assertTrue(thrown)
        assertEquals(0, context.registeredReceivers.size)
    }

    @Test
    fun moduleReceiver_replaceOldReceiverOnSameKey() {
        val context = TrackableContext()
        val first = StubReceiver()
        val second = StubReceiver()

        ModuleHelper.registerModuleReceiver(
            context,
            "testKey",
            first,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )
        ModuleHelper.registerModuleReceiver(
            context,
            "testKey",
            second,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        assertEquals(2, context.registeredReceivers.size)
        assertTrue(context.unregisteredReceivers.contains(first))
        assertFalse(context.unregisteredReceivers.contains(second))

        ModuleHelper.unregisterModuleReceiver("testKey")
        assertTrue(context.unregisteredReceivers.contains(second))
    }

    @Test
    fun moduleReceiver_concurrentSameKeyReplacesLoser() {
        val context = TrackableContext()
        context.registerDelayMs = 50L
        val barrier = CountDownLatch(2)
        val winners = AtomicInteger(0)
        val losers = AtomicInteger(0)

        val t1 = Thread {
            val ok = ModuleHelper.registerModuleReceiver(
                context,
                "raceKey",
                StubReceiver(),
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
            if (ok) winners.incrementAndGet() else losers.incrementAndGet()
            barrier.countDown()
        }
        val t2 = Thread {
            val ok = ModuleHelper.registerModuleReceiver(
                context,
                "raceKey",
                StubReceiver(),
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
            if (ok) winners.incrementAndGet() else losers.incrementAndGet()
            barrier.countDown()
        }

        t1.start()
        t2.start()
        barrier.await()

        // Both threads successfully called registerReceiver, but only one registration may remain
        // current for the same key. The earlier one must have been unregistered.
        assertEquals("both concurrent registrations reach the framework", 2, context.registeredReceivers.size)
        assertEquals("only one concurrent registration is unregistered as the loser", 1, context.unregisteredReceivers.size)

        ModuleHelper.unregisterModuleReceiver("raceKey")
        assertEquals("final cleanup unregisters the winner too", 2, context.unregisteredReceivers.size)
    }

    @Test
    fun ownedReceiver_sameOwnerReplacesOldReceiver() {
        val context = TrackableContext()
        val owner = Any()
        val first = StubReceiver()
        val second = StubReceiver()

        ModuleHelper.registerOwnedReceiver(
            context,
            owner,
            "testKey",
            first,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )
        ModuleHelper.registerOwnedReceiver(
            context,
            owner,
            "testKey",
            second,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        assertEquals(2, context.registeredReceivers.size)
        assertTrue(context.unregisteredReceivers.contains(first))
        assertFalse(context.unregisteredReceivers.contains(second))
    }

    @Test
    fun ownedReceiver_differentOwnersCoexistForSameKey() {
        val context = TrackableContext()
        val owner1 = Any()
        val owner2 = Any()
        val first = StubReceiver()
        val second = StubReceiver()

        ModuleHelper.registerOwnedReceiver(
            context,
            owner1,
            "testKey",
            first,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )
        ModuleHelper.registerOwnedReceiver(
            context,
            owner2,
            "testKey",
            second,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        assertEquals(2, context.registeredReceivers.size)
        assertEquals(0, context.unregisteredReceivers.size)

        ModuleHelper.unregisterOwnedReceiver("testKey", owner1)
        assertEquals(1, context.unregisteredReceivers.size)
        assertTrue(context.unregisteredReceivers.contains(first))
    }

    @Test
    fun ownedReceiver_registrationFailureDoesNotLeak() {
        val context = TrackableContext()
        context.failNextRegister = true
        val owner = Any()
        val receiver = StubReceiver()

        val ok = ModuleHelper.registerOwnedReceiver(
            context,
            owner,
            "testKey",
            receiver,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        assertFalse(ok)
        assertEquals(0, context.registeredReceivers.size)
    }

    @Test
    fun ownedReceiver_failedReplacementKeepsPreviousRegistration() {
        val context = TrackableContext()
        val owner = Any()
        val first = StubReceiver()

        assertTrue(
            ModuleHelper.registerOwnedReceiver(
                context,
                owner,
                "testKey",
                first,
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
        )
        context.failNextRegister = true

        assertFalse(
            ModuleHelper.registerOwnedReceiver(
                context,
                owner,
                "testKey",
                StubReceiver(),
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
        )
        assertFalse(context.unregisteredReceivers.contains(first))

        ModuleHelper.unregisterOwnedReceiver("testKey", owner)
        assertTrue(context.unregisteredReceivers.contains(first))
    }

    @Test
    fun ownedReceiver_unregisterRemovesEmptyBucket() {
        val context = TrackableContext()
        val owner = Any()

        ModuleHelper.registerOwnedReceiver(
            context,
            owner,
            "detachKey",
            StubReceiver(),
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )
        ModuleHelper.unregisterOwnedReceiver("detachKey", owner)

        assertTrue(emptyOwnedBucket("detachKey"))
        assertEquals(1, context.unregisteredReceivers.size)
    }

    @Test
    fun ownedReceiver_concurrentRegisterAndUnregisterIsSafe() {
        val context = TrackableContext()
        val owner = Any()
        val start = CyclicBarrier(2)
        val done = CountDownLatch(2)

        val t1 = Thread {
            start.await()
            val ok = ModuleHelper.registerOwnedReceiver(
                context,
                owner,
                "raceKey",
                StubReceiver(),
                intentFilter("action"),
                Context.RECEIVER_NOT_EXPORTED
            )
            assertTrue("registration must succeed", ok)
            done.countDown()
        }

        val t2 = Thread {
            start.await()
            ModuleHelper.unregisterOwnedReceiver("raceKey", owner)
            done.countDown()
        }

        t1.start()
        t2.start()
        done.await()

        // Ensure a final unregister so the receiver is always cleaned up,
        // regardless of which thread won the initial race.
        ModuleHelper.unregisterOwnedReceiver("raceKey", owner)

        // No leaked framework receiver: whatever was registered is also unregistered.
        assertEquals(context.registeredReceivers.size, context.unregisteredReceivers.size)
        assertEquals(1, context.registeredReceivers.size)
        assertEquals(1, context.unregisteredReceivers.size)
    }

    @Test
    fun staleModuleReceiver_failedUnregisterIsDrainedByNextRegister() {
        val context = TrackableContext()
        val receiver = StubReceiver()

        ModuleHelper.registerModuleReceiver(
            context,
            "staleKey",
            receiver,
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        context.failNextUnregister = true
        ModuleHelper.unregisterModuleReceiver("staleKey")

        context.failNextUnregister = false
        ModuleHelper.registerModuleReceiver(
            context,
            "staleKey",
            StubReceiver(),
            intentFilter("action"),
            Context.RECEIVER_NOT_EXPORTED
        )

        assertEquals(1, context.unregisteredReceivers.size)
        assertEquals(2, context.registeredReceivers.size)
    }

    private fun emptyOwnedBucket(key: String): Boolean {
        val field = ModuleHelper::class.java.getDeclaredField("ownedReceivers")
        field.isAccessible = true
        val map = field.get(null) as MutableMap<*, *>
        return !map.containsKey(key)
    }

    private fun staleMapContains(fieldName: String, key: String): Boolean {
        val field = ModuleHelper::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        val map = field.get(null) as MutableMap<*, *>
        if (!map.containsKey(key)) return false
        val deque = map[key] as? Collection<*> ?: return false
        return !deque.isEmpty()
    }
}
