package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class B3_1_MigrationInteropTest {

    @Test
    fun `PackagePermissions is migrated to Kotlin object`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.PackagePermissions")
        val instanceField = clazz.getField("INSTANCE")
        assertNotNull(instanceField)
        assertEquals(clazz, instanceField.type)
    }

    @Test
    fun `PackagePermissions preserves hook method accepting SystemServerStartingParam`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.PackagePermissions")
        val method = clazz.declaredMethods.single { it.name == "hook" && it.parameterCount == 1 }
        assertNotNull(method)
        assertEquals("SystemServerStartingParam", method.parameterTypes[0].simpleName)
    }
}
