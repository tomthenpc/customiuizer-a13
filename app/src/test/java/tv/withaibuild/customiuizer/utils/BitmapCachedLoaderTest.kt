package tv.withaibuild.customiuizer.utils

import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitmapCachedLoaderTest {

    @Test
    fun loaderThreadCountStaysWithinSharedPoolBounds() {
        assertEquals(2, BitmapCachedLoader.clampLoaderThreadCount(1))
        assertEquals(2, BitmapCachedLoader.clampLoaderThreadCount(4))
        assertEquals(3, BitmapCachedLoader.clampLoaderThreadCount(6))
        assertEquals(4, BitmapCachedLoader.clampLoaderThreadCount(8))
        assertEquals(4, BitmapCachedLoader.clampLoaderThreadCount(32))
    }

    @Test
    fun sameIconKeyHasOneLeaderAndMultipleWaiters() {
        val registry = IconLoadRegistry<String>()

        assertTrue(registry.join("pkg|Activity", "first"))
        assertFalse(registry.join("pkg|Activity", "second"))
        assertEquals(1, registry.pendingKeyCount())
        assertEquals(listOf("first", "second"), registry.release("pkg|Activity"))
        assertEquals(0, registry.pendingKeyCount())
    }

    @Test
    fun rejectionCleanupAllowsTheKeyToBeQueuedAgain() {
        val registry = IconLoadRegistry<String>()

        assertTrue(registry.join("pkg|-", "rejected"))
        registry.release("pkg|-")

        assertTrue(registry.join("pkg|-", "retry"))
        assertEquals(listOf("retry"), registry.release("pkg|-"))
    }

    @Test
    fun iconKeyUsesPrecomputedValueAndKeepsPackageFallback() {
        val app = AppData().apply {
            pkgName = "example.package"
            actName = "-"
        }
        assertEquals("example.package|-", appIconCacheKey(app))

        app.iconKey = "prepared-key"
        assertEquals("prepared-key", appIconCacheKey(app))
    }

    @Test
    fun appAdapterUsesReusableHolderAndOwnedReplacementList() {
        val adapterClass = AppDataAdapter::class.java
        assertTrue(adapterClass.declaredClasses.any { it.simpleName == "ViewHolder" })

        val filteredList = adapterClass.getDeclaredField("filteredAppList")
        assertTrue(List::class.java.isAssignableFrom(filteredList.type))
        assertFalse(CopyOnWriteArrayList::class.java.isAssignableFrom(filteredList.type))
    }
}
