package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class PrefMapTest {

    private lateinit var map: PrefMap<String, Any>

    @Before
    fun setUp() {
        map = PrefMap()
    }

    @Test
    fun getBooleanReturnsTrueAndFalse() {
        map["pref_key_true"] = true
        map["pref_key_false"] = false
        assertTrue(map.getBoolean("true"))
        assertFalse(map.getBoolean("false"))
    }

    @Test
    fun getBooleanParsesStringTrueFalse() {
        map["pref_key_t"] = "true"
        map["pref_key_one"] = "1"
        map["pref_key_f"] = "false"
        map["pref_key_zero"] = "0"
        assertTrue(map.getBoolean("t"))
        assertTrue(map.getBoolean("one"))
        assertFalse(map.getBoolean("f"))
        assertFalse(map.getBoolean("zero"))
    }

    @Test
    fun getBooleanUnknownValueReturnsDefault() {
        map["pref_key_unknown"] = "notABoolean"
        map["pref_key_number"] = 42
        assertFalse(map.getBoolean("unknown"))
        assertFalse(map.getBoolean("number"))
        assertTrue(map.getBoolean("unknown", true))
        assertTrue(map.getBoolean("number", true))
    }

    @Test
    fun getBooleanMissingKeyReturnsDefault() {
        assertFalse(map.getBoolean("missing"))
        assertTrue(map.getBoolean("missing", true))
    }

    @Test
    fun getStringAsIntParsesNumericString() {
        map["pref_key_foo"] = "123"
        assertEquals(123, map.getStringAsInt("foo", 0))
        assertEquals(123, map.getStringAsInt("pref_key_foo", 0))
    }

    @Test
    fun getStringAsIntReturnsDefaultForDamagedString() {
        map["pref_key_foo"] = "not-a-number"
        assertEquals(7, map.getStringAsInt("foo", 7))
    }

    @Test
    fun getStringAsIntReturnsNumberAsInt() {
        map["pref_key_foo"] = 42
        assertEquals(42, map.getStringAsInt("foo", 0))
    }

    @Test
    fun getStringAsIntReturnsDefaultForUnknownType() {
        map["pref_key_foo"] = listOf("x")
        assertEquals(5, map.getStringAsInt("foo", 5))
    }

    @Test
    fun getStringAsIntCachesParseResult() {
        map["pref_key_foo"] = "123"
        val first = map.getStringAsInt("foo", 0)
        val second = map.getStringAsInt("foo", 0)
        assertEquals(first, second)
        // Key is still cached after the first successful parse.
        assertEquals(123, map.getStringAsInt("foo", 0))
    }

    @Test
    fun putInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map["pref_key_foo"] = "456"
        assertEquals(456, map.getStringAsInt("foo", 0))
    }

    @Test
    fun removeInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map.remove("pref_key_foo")
        assertEquals(9, map.getStringAsInt("foo", 9))
    }

    @Test
    fun clearInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map.clear()
        assertEquals(9, map.getStringAsInt("foo", 9))
    }

    @Test
    fun putAllInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map.putAll(mapOf("pref_key_foo" to "456"))
        assertEquals(456, map.getStringAsInt("foo", 0))
    }

    @Test
    fun getIntSafelyDowngradesNonIntegerValues() {
        map["pref_key_long"] = 42L
        map["pref_key_str"] = "7"
        map["pref_key_bool"] = true
        assertEquals(42, map.getInt("long", 0))
        assertEquals(7, map.getInt("str", 0))
        assertEquals(0, map.getInt("bool", 0))
    }

    @Test
    fun getLongSafelyDowngradesNonLongValues() {
        map["pref_key_int"] = 42
        map["pref_key_str"] = "123456789012"
        map["pref_key_bool"] = true
        assertEquals(42L, map.getLong("int", 0L))
        assertEquals(123456789012L, map.getLong("str", 0L))
        assertEquals(0L, map.getLong("bool", 0L))
    }

    @Test
    fun getStringSetFiltersNonStringEntries() {
        map["pref_key_set"] = setOf("a", 1, "b")
        val result = map.getStringSet("set")
        assertEquals(setOf("a", "b"), result)
    }

    @Test
    fun getObjectNormalizesBothShortAndFullKey() {
        map["pref_key_foo"] = "full"
        assertEquals("full", map.getObject("foo", null))
        assertEquals("full", map.getObject("pref_key_foo", null))
    }

    @Test
    fun getStringAsIntInvalidatesCacheOnRemove() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map.remove("foo")
        assertEquals(9, map.getStringAsInt("foo", 9))
    }

    @Test
    fun concurrentReadAndWriteDoesNotThrow() {
        val iterations = 1000
        val threads = 8
        val start = CountDownLatch(threads)
        val done = CountDownLatch(threads)
        val errors = AtomicInteger(0)

        for (i in 0 until threads) {
            Thread {
                start.countDown()
                try {
                    start.await()
                    for (j in 0 until iterations) {
                        val key = "pref_key_${j % 50}"
                        when (i % 4) {
                            0 -> map[key] = j
                            1 -> map.remove(key)
                            2 -> map.getStringAsInt(key, -1)
                            3 -> map.getBoolean(key)
                        }
                    }
                } catch (t: Throwable) {
                    errors.incrementAndGet()
                } finally {
                    done.countDown()
                }
            }.start()
        }

        done.await()
        assertEquals("concurrent access must not throw", 0, errors.get())
    }

    @Test
    fun concurrentPutAllAndClearDoesNotCorrupt() {
        val mapA = PrefMap<String, Any>()
        val mapB = PrefMap<String, Any>()

        val threads = 4
        val iterations = 500
        val done = CountDownLatch(threads)

        for (i in 0 until threads) {
            Thread {
                for (j in 0 until iterations) {
                    if (i % 2 == 0) {
                        mapA.putAll(mapOf("pref_key_a" to j, "pref_key_b" to j + 1))
                    } else {
                        mapA.clear()
                    }
                }
                done.countDown()
            }.start()
        }

        done.await()
        // The map must remain a valid ConcurrentHashMap and not throw on access.
        mapA["pref_key_a"] = 1
        assertEquals(1, mapA.getInt("a", 0))
    }

    @Test
    fun cacheDoesNotGrowUnbounded() {
        for (i in 0 until 100) {
            map["pref_key_$i"] = "$i"
            map.getStringAsInt("$i", 0)
        }
        map.clear()
        assertTrue(map.getStringAsInt("0", -1) == -1)
    }

    @Test
    fun mapStyleGetAndContainsNormalizeKeys() {
        map["pref_key_foo"] = "bar"
        assertEquals("bar", map["foo"])
        assertEquals("bar", map["pref_key_foo"])
        assertTrue("foo" in map)
        assertTrue("pref_key_foo" in map)
        assertFalse("bar" in map)
    }
}
