package tv.withaibuild.customiuizer.mods.utils

import android.util.SparseArray
import android.util.SparseIntArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Contract tests for the test-only [android.util.SparseArray] and
 * [android.util.SparseIntArray] shadows.
 *
 * These shadows replace the no-op Android framework stubs so that
 * [ResourceHooks] state can be asserted in unit tests.
 */
class AndroidSparseArrayShadowTest {

    @Test
    fun sparseArray_putAndGet() {
        val array = SparseArray<String>()
        array.put(10, "ten")
        array.put(20, "twenty")
        assertEquals("ten", array.get(10))
        assertEquals("twenty", array.get(20))
    }

    @Test
    fun sparseArray_overwriteSameKey() {
        val array = SparseArray<String>()
        array.put(5, "old")
        array.put(5, "new")
        assertEquals("new", array.get(5))
        assertEquals(1, array.size())
    }

    @Test
    fun sparseArray_sortedKeyAtValueAt() {
        val array = SparseArray<String>()
        array.put(30, "thirty")
        array.put(10, "ten")
        array.put(20, "twenty")
        assertEquals(3, array.size())
        assertEquals(10, array.keyAt(0))
        assertEquals(20, array.keyAt(1))
        assertEquals(30, array.keyAt(2))
        assertEquals("ten", array.valueAt(0))
        assertEquals("twenty", array.valueAt(1))
        assertEquals("thirty", array.valueAt(2))
    }

    @Test
    fun sparseArray_deleteAndRemove() {
        val array = SparseArray<String>()
        array.put(1, "one")
        array.put(2, "two")
        array.remove(1)
        assertEquals(1, array.size())
        assertNull(array.get(1))
        assertEquals("two", array.get(2))
    }

    @Test
    fun sparseArray_removeAt() {
        val array = SparseArray<String>()
        array.put(1, "one")
        array.put(2, "two")
        array.put(3, "three")
        array.removeAt(1)
        assertEquals(2, array.size())
        assertEquals(1, array.keyAt(0))
        assertEquals(3, array.keyAt(1))
    }

    @Test
    fun sparseArray_clear() {
        val array = SparseArray<String>()
        array.put(1, "one")
        array.put(2, "two")
        array.clear()
        assertEquals(0, array.size())
        assertNull(array.get(1))
    }

    @Test
    fun sparseArray_cloneIsIndependent() {
        val array = SparseArray<String>()
        array.put(1, "one")
        val clone = array.clone()
        assertEquals(1, clone.size())
        assertEquals("one", clone.get(1))
        array.put(2, "two")
        assertEquals(2, array.size())
        assertEquals(1, clone.size())
        assertNull(clone.get(2))
    }

    @Test
    fun sparseArray_append() {
        val array = SparseArray<String>()
        array.append(7, "seven")
        assertEquals("seven", array.get(7))
        assertEquals(1, array.size())
    }

    @Test
    fun sparseArray_getWithDefault() {
        val array = SparseArray<String>()
        array.put(1, "one")
        assertEquals("one", array.get(1, "default"))
        assertEquals("default", array.get(99, "default"))
    }

    @Test
    fun sparseArray_nullValueIsNotReplacedByDefault() {
        val array = SparseArray<String?>()
        array.put(1, null)
        assertNull(array.get(1, "default"))
        assertSame(null, array.get(1, "default"))
    }

    @Test
    fun sparseIntArray_putAndGet() {
        val array = SparseIntArray()
        array.put(10, 100)
        array.put(20, 200)
        assertEquals(100, array.get(10))
        assertEquals(200, array.get(20))
    }

    @Test
    fun sparseIntArray_overwriteSameKey() {
        val array = SparseIntArray()
        array.put(5, 1)
        array.put(5, 2)
        assertEquals(2, array.get(5))
        assertEquals(1, array.size())
    }

    @Test
    fun sparseIntArray_sortedKeyAtValueAt() {
        val array = SparseIntArray()
        array.put(30, 300)
        array.put(10, 100)
        array.put(20, 200)
        assertEquals(3, array.size())
        assertEquals(10, array.keyAt(0))
        assertEquals(20, array.keyAt(1))
        assertEquals(30, array.keyAt(2))
        assertEquals(100, array.valueAt(0))
        assertEquals(200, array.valueAt(1))
        assertEquals(300, array.valueAt(2))
    }

    @Test
    fun sparseIntArray_deleteAndRemove() {
        val array = SparseIntArray()
        array.put(1, 10)
        array.put(2, 20)
        array.remove(1)
        assertEquals(1, array.size())
        assertEquals(0, array.get(1))
        assertEquals(20, array.get(2))
    }

    @Test
    fun sparseIntArray_removeAt() {
        val array = SparseIntArray()
        array.put(1, 10)
        array.put(2, 20)
        array.put(3, 30)
        array.removeAt(1)
        assertEquals(2, array.size())
        assertEquals(1, array.keyAt(0))
        assertEquals(3, array.keyAt(1))
    }

    @Test
    fun sparseIntArray_clear() {
        val array = SparseIntArray()
        array.put(1, 10)
        array.put(2, 20)
        array.clear()
        assertEquals(0, array.size())
        assertEquals(0, array.get(1))
    }

    @Test
    fun sparseIntArray_cloneIsIndependent() {
        val array = SparseIntArray()
        array.put(1, 10)
        val clone = array.clone()
        assertEquals(1, clone.size())
        assertEquals(10, clone.get(1))
        array.put(2, 20)
        assertEquals(2, array.size())
        assertEquals(1, clone.size())
        assertEquals(0, clone.get(2))
    }

    @Test
    fun sparseIntArray_append() {
        val array = SparseIntArray()
        array.append(7, 70)
        assertEquals(70, array.get(7))
        assertEquals(1, array.size())
    }

    @Test
    fun sparseIntArray_missingDefault() {
        val array = SparseIntArray()
        assertEquals(42, array.get(99, 42))
    }
}
