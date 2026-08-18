package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsbDefaultFunctionMapperTest {

    @Test
    fun followAndChargeLeaveRomDefault() {
        assertNull(UsbDefaultFunctionMapper.toA13Function("none"))
        assertNull(UsbDefaultFunctionMapper.toA13Function("0"))
        assertNull(UsbDefaultFunctionMapper.toA13Function("1"))
        assertNull(UsbDefaultFunctionMapper.toA13Function("charging"))
    }

    @Test
    fun a14NumericModesMapToA13Names() {
        assertEquals("mtp", UsbDefaultFunctionMapper.toA13Function("2"))
        assertEquals("ptp", UsbDefaultFunctionMapper.toA13Function("3"))
    }

    @Test
    fun a13NamesPassThrough() {
        assertEquals("mtp", UsbDefaultFunctionMapper.toA13Function("mtp"))
        assertEquals("rndis", UsbDefaultFunctionMapper.toA13Function("rndis"))
    }
}
