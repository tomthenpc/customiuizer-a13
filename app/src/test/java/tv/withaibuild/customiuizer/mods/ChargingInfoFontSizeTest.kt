package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingInfoFontSizeTest {

    @Test
    fun defaultKeepsSystemSize() {
        assertNull(SystemChargingAndWallpaperHooks.resolveChargingInfoFontSizeSp(16))
    }

    @Test
    fun customValueMapsToSp() {
        assertEquals(10f, SystemChargingAndWallpaperHooks.resolveChargingInfoFontSizeSp(20)!!, 0f)
        assertEquals(20f, SystemChargingAndWallpaperHooks.resolveChargingInfoFontSizeSp(40)!!, 0f)
    }
}
