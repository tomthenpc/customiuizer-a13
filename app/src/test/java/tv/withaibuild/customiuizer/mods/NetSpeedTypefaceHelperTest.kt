package tv.withaibuild.customiuizer.mods

import android.graphics.Typeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetSpeedTypefaceHelperTest {

    @Test
    fun normalBaseIsPreservedWhenUnbolded() {
        assertEquals(Typeface.NORMAL, NetSpeedTypefaceHelper.resolveBaseStyle(Typeface.NORMAL))
    }

    @Test
    fun boldIsStrippedToRecoverBaseFamily() {
        assertEquals(Typeface.NORMAL, NetSpeedTypefaceHelper.resolveBaseStyle(Typeface.BOLD))
    }

    @Test
    fun boldItalicIsStrippedToItalicBase() {
        assertEquals(Typeface.ITALIC, NetSpeedTypefaceHelper.resolveBaseStyle(Typeface.BOLD_ITALIC))
    }

    @Test
    fun unboldedDesiredStyleMatchesBaseStyle() {
        assertEquals(Typeface.NORMAL, NetSpeedTypefaceHelper.resolveDesiredStyle(Typeface.NORMAL, false))
        assertEquals(Typeface.ITALIC, NetSpeedTypefaceHelper.resolveDesiredStyle(Typeface.ITALIC, false))
    }

    @Test
    fun boldDesiredStyleAddsBoldToBase() {
        assertEquals(Typeface.BOLD, NetSpeedTypefaceHelper.resolveDesiredStyle(Typeface.NORMAL, true))
        assertEquals(Typeface.BOLD_ITALIC, NetSpeedTypefaceHelper.resolveDesiredStyle(Typeface.ITALIC, true))
    }

    @Test
    fun boldToggleDoesNotAccumulateStyleBits() {
        // Simulating applying bold to a view that is already bold from a previous apply.
        // The base must be derived from the stripped style, and re-applying bold must not
        // create a new style beyond Bold or Bold Italic.
        var style = Typeface.NORMAL
        repeat(3) {
            style = NetSpeedTypefaceHelper.resolveDesiredStyle(
                NetSpeedTypefaceHelper.resolveBaseStyle(style),
                true
            )
        }
        assertEquals(Typeface.BOLD, style)
    }

    @Test
    fun fakeBoldFallbackIsUsedWhenNoTypefaceIsSet() {
        assertTrue(NetSpeedTypefaceHelper.shouldUseFakeBold(null))
    }

    @Test
    fun boldDesiredStyleDoesNotTurnOffItalic() {
        val base = Typeface.ITALIC
        val desired = NetSpeedTypefaceHelper.resolveDesiredStyle(base, true)
        assertTrue((desired and Typeface.ITALIC) != 0)
        assertTrue((desired and Typeface.BOLD) != 0)
    }
}
