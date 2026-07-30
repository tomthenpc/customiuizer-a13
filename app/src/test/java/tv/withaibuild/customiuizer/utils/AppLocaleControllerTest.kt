package tv.withaibuild.customiuizer.utils

import androidx.core.os.LocaleListCompat
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLocaleControllerTest {

    private lateinit var prefs: FakeSharedPreferences
    private val appliedTags = ArrayList<String>()
    private var providerCalls = 0

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        appliedTags.clear()
        providerCalls = 0
        AppLocaleController.applicationLocaleApplier = {
            appliedTags.add(it.toLanguageTags())
        }
        AppLocaleController.applicationLocaleProvider = {
            providerCalls++
            LocaleListCompat.getEmptyLocaleList()
        }
    }

    @After
    fun tearDown() {
        AppLocaleController.applicationLocaleApplier = null
        AppLocaleController.applicationLocaleProvider = null
        AppHelper.moduleActive = false
        AppHelper.moduleConnectionObserved = false
    }

    @Test
    fun normalizeLocaleTagKeepsSupportedValuesAndMapsUnknownToAuto() {
        assertEquals("auto", AppLocaleController.normalizeLocaleTag(null))
        assertEquals("auto", AppLocaleController.normalizeLocaleTag(""))
        assertEquals("auto", AppLocaleController.normalizeLocaleTag("1"))
        assertEquals("auto", AppLocaleController.normalizeLocaleTag("unknown"))
        assertEquals("en", AppLocaleController.normalizeLocaleTag("en"))
        assertEquals("zh-CN", AppLocaleController.normalizeLocaleTag("zh-CN"))
        assertEquals("zh-TW", AppLocaleController.normalizeLocaleTag("zh-TW"))
        assertEquals("pt-BR", AppLocaleController.normalizeLocaleTag("pt-BR"))
    }

    @Test
    fun userLocaleReadNormalizesLegacyAndInvalidStoredValues() {
        assertEquals("auto", AppLocaleController.getUserLocale(null))
        prefs.put(AppLocaleController.LOCALE_PREF_KEY, "1")
        assertEquals("auto", AppLocaleController.getUserLocale(prefs))
        prefs.put(AppLocaleController.LOCALE_PREF_KEY, "invalid")
        assertEquals("auto", AppLocaleController.getUserLocale(prefs))
    }

    @Test
    fun untouchedAutoStartupDoesNotReadOrWriteFrameworkLocales() {
        assertFalse(AppLocaleController.apply(prefs))
        assertEquals(0, providerCalls)
        assertTrue(appliedTags.isEmpty())
        assertNull(
            prefs.getString(AppLocaleController.APPLIED_LOCALE_PREF_KEY, null)
        )
    }

    @Test
    fun explicitLocaleIsPersistedThenAppliedThroughFrameworkList() {
        assertTrue(AppLocaleController.setUserLocale(prefs, "zh-CN"))
        assertTrue(AppLocaleController.apply(prefs))

        assertEquals(listOf("zh-CN"), appliedTags)
        assertEquals(
            "zh-CN",
            prefs.getString(AppLocaleController.APPLIED_LOCALE_PREF_KEY, null)
        )
    }

    @Test
    fun savingLocaleIsDeferredUntilTheNextApply() {
        assertTrue(AppLocaleController.setUserLocale(prefs, "es-ES"))
        assertEquals("es-ES", AppLocaleController.getUserLocale(prefs))
        assertTrue(appliedTags.isEmpty())
        assertEquals(0, providerCalls)
    }

    @Test
    fun invalidSavedLocaleIsNormalizedToAuto() {
        assertTrue(AppLocaleController.setUserLocale(prefs, "garbage"))
        assertEquals("auto", AppLocaleController.getUserLocale(prefs))
    }

    @Test
    fun matchingExplicitLocaleIsNotAppliedAgain() {
        prefs.put(AppLocaleController.LOCALE_PREF_KEY, "en")
        AppLocaleController.applicationLocaleProvider = {
            providerCalls++
            LocaleListCompat.forLanguageTags("en")
        }

        assertFalse(AppLocaleController.apply(prefs))
        assertTrue(appliedTags.isEmpty())
        assertEquals(
            "en",
            prefs.getString(AppLocaleController.APPLIED_LOCALE_PREF_KEY, null)
        )
    }

    @Test
    fun returningToAutoClearsFrameworkLocaleAndMarker() {
        prefs.put(AppLocaleController.LOCALE_PREF_KEY, "auto")
        prefs.put(AppLocaleController.APPLIED_LOCALE_PREF_KEY, "en")
        AppLocaleController.applicationLocaleProvider = {
            LocaleListCompat.forLanguageTags("en")
        }

        assertTrue(AppLocaleController.apply(prefs))
        assertEquals(listOf(""), appliedTags)
        assertNull(
            prefs.getString(AppLocaleController.APPLIED_LOCALE_PREF_KEY, null)
        )
    }

    @Test
    fun backupRestoreForcesOneFullReconciliation() {
        AppLocaleController.invalidateFastPath(prefs)

        assertEquals(
            "",
            prefs.getString(AppLocaleController.APPLIED_LOCALE_PREF_KEY, null)
        )
        assertFalse(AppLocaleController.apply(prefs))
        assertEquals(1, providerCalls)
        assertNull(
            prefs.getString(AppLocaleController.APPLIED_LOCALE_PREF_KEY, null)
        )
    }

    @Test
    fun explicitLocaleWithoutContextOrTestApplierDoesNotClaimSuccess() {
        prefs.put(AppLocaleController.LOCALE_PREF_KEY, "en")
        AppLocaleController.applicationLocaleApplier = null

        assertFalse(AppLocaleController.apply(prefs))
        assertNull(
            prefs.getString(AppLocaleController.APPLIED_LOCALE_PREF_KEY, null)
        )
    }

    @Test
    fun localeListsPreserveRegionTagsAndAutoIsEmpty() {
        assertEquals(
            "pt-BR",
            AppLocaleController.toLocaleListCompat("pt-BR").toLanguageTags()
        )
        assertEquals(
            "zh-TW",
            AppLocaleController.toLocaleListCompat("zh-TW").toLanguageTags()
        )
        assertEquals(
            "",
            AppLocaleController.toLocaleListCompat("auto").toLanguageTags()
        )
    }

    @Test
    fun localeDisplayEntriesRemainParallelAndComplete() {
        val (entries, values) =
            AppLocaleController.buildLocaleDisplayData("System default")

        assertEquals(entries.size, values.size)
        assertArrayEquals(
            arrayOf(
                "auto",
                "en",
                "zh-CN",
                "zh-TW",
                "ru-RU",
                "ja-JP",
                "vi-VN",
                "cs-CZ",
                "pt-BR",
                "tr-TR",
                "es-ES"
            ),
            values
        )
        assertEquals("System default", entries.first().toString())
        assertEquals("繁體中文（台灣）", entries[values.indexOf("zh-TW")].toString())
        assertTrue(entries[values.indexOf("pt-BR")].toString().contains("(Brasil)"))
        assertNotNull(entries.last())
    }

    @Test
    fun localeContextCompatibilityBoundaryKeepsItsJvmSignature() {
        assertNotNull(
            AppLocaleController::class.java.getDeclaredMethod(
                "getLocaleContext",
                android.content.Context::class.java,
                android.content.SharedPreferences::class.java
            )
        )
    }

    @Test
    fun effectiveLocaleHelperStillResolvesAutoAndExplicitTags() {
        val system = Locale.forLanguageTag("ru-RU")
        assertEquals(system, AppLocaleController.getEffectiveLocale("auto") { system })
        assertEquals(
            Locale.forLanguageTag("es-ES"),
            AppLocaleController.getEffectiveLocale("es-ES") { system }
        )
    }

    @Test
    fun unobservedBinderStateNeverReportsModuleInactive() {
        AppHelper.moduleConnectionObserved = false
        AppHelper.moduleActive = false
        assertFalse(AppHelper.shouldReportModuleInactive())

        AppHelper.moduleConnectionObserved = true
        assertTrue(AppHelper.shouldReportModuleInactive())

        AppHelper.moduleActive = true
        assertFalse(AppHelper.shouldReportModuleInactive())
    }
}
