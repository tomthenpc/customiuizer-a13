package tv.withaibuild.customiuizer.utils

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModSearchIndexTest {

    private fun mod(breadcrumbs: String, title: String): ModData =
        ModData().apply {
            this.breadcrumbs = breadcrumbs
            this.title = title
            prepareSearchKeys()
        }

    private fun filter(source: List<ModData>, query: String): List<ModData> {
        val searchKey = query.lowercase(Locale.ROOT)
        return source.filter { it.titleSearchKey.contains(searchKey) }
    }

    @Test
    fun searchKeysArePreparedOnceWithRootLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val entry = mod("System", "IMEI")
            assertEquals("imei", entry.titleSearchKey)
            assertEquals("system", entry.breadcrumbsSortKey)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun filteringTheSortedIndexPreservesDisplayOrder() {
        val unsorted = listOf(
            mod("System/Status bar", "Zen mode"),
            mod("Launcher", "Animation speed"),
            mod("System/Status bar", "Alarm icon"),
            mod("Launcher", "Zoom animation"),
            mod("System", "Alarm sound")
        )
        val sorted = unsorted.sortedWith(Helpers.MOD_DISPLAY_ORDER)

        for (query in listOf("", "a", "an", "zo", "alarm", "none")) {
            assertEquals(
                filter(unsorted, query)
                    .sortedWith(Helpers.MOD_DISPLAY_ORDER)
                    .map { it.title },
                filter(sorted, query).map { it.title }
            )
        }
    }

    @Test
    fun displayOrderIsCaseInsensitiveBreadcrumbThenTitle() {
        val sorted = listOf(
            mod("system", "beta"),
            mod("System", "Alpha"),
            mod("Launcher", "Gamma")
        ).sortedWith(Helpers.MOD_DISPLAY_ORDER)

        assertEquals(listOf("Gamma", "Alpha", "beta"), sorted.map { it.title })
    }

    @Test
    fun comparatorIsAntisymmetric() {
        val entries = listOf(
            mod("A", "x"),
            mod("A", "y"),
            mod("B", "x"),
            mod("a", "X"),
            mod("B", "y")
        )
        for (first in entries) for (second in entries) {
            val forward = Helpers.MOD_DISPLAY_ORDER.compare(first, second)
            val reverse = Helpers.MOD_DISPLAY_ORDER.compare(second, first)
            assertTrue(
                (forward == 0 && reverse == 0) ||
                    (forward < 0 && reverse > 0) ||
                    (forward > 0 && reverse < 0)
            )
        }
    }
}
