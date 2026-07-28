package name.monwf.customiuizer.subs

import android.content.Intent
import name.monwf.customiuizer.SubFragment
import org.junit.Assert.*
import org.junit.Test

class B1_4_MigrationInteropTest {

    @Test
    fun `System can be constructed and extends SubFragment`() {
        val clazz = System::class.java
        assertTrue(SubFragment::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `System preserves lifecycle methods`() {
        val clazz = System::class.java

        val onCreatePreferences = clazz.getDeclaredMethod(
            "onCreatePreferences",
            android.os.Bundle::class.java,
            String::class.java
        )
        assertNotNull(onCreatePreferences)

        val onCreate = clazz.getDeclaredMethod(
            "onCreate",
            android.os.Bundle::class.java
        )
        assertNotNull(onCreate)

        val onActivityCreated = clazz.getDeclaredMethod(
            "onActivityCreated",
            android.os.Bundle::class.java
        )
        assertNotNull(onActivityCreated)

        val onActivityResult = clazz.getDeclaredMethod(
            "onActivityResult",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Intent::class.java
        )
        assertNotNull(onActivityResult)

        val checkUSBPermission = clazz.getDeclaredMethod("checkUSBPermission")
        assertNotNull(checkUSBPermission)

        val checkAnimationPermission = clazz.getDeclaredMethod("checkAnimationPermission")
        assertNotNull(checkAnimationPermission)
    }
}
