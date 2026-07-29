package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test

class PrefMapTest {

    private lateinit var map: PrefMap<String, Any>

    @Before
    fun setUp() {
        map = PrefMap()
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
}
