package name.monwf.customiuizer

import android.content.ContentProvider
import name.monwf.customiuizer.subs.ShortcutSelector
import org.junit.Assert.*
import org.junit.Test

class B1_2_MigrationInteropTest {

    @Test
    fun `PrefsProvider is a ContentProvider with public static AUTHORITY`() {
        val clazz = PrefsProvider::class.java
        assertTrue(ContentProvider::class.java.isAssignableFrom(clazz))

        val authorityField = clazz.getField("AUTHORITY")
        assertTrue(java.lang.reflect.Modifier.isPublic(authorityField.modifiers))
        assertTrue(java.lang.reflect.Modifier.isStatic(authorityField.modifiers))
        assertTrue(java.lang.reflect.Modifier.isFinal(authorityField.modifiers))

        val authority = authorityField.get(null) as String
        assertEquals(BuildConfig.APPLICATION_ID + ".provider.sharedprefs", authority)

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
    }

    @Test
    fun `ShortcutSelector can be constructed and extends SubFragmentWithSearch`() {
        val clazz = ShortcutSelector::class.java
        assertTrue(SubFragmentWithSearch::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        val instance = constructor.newInstance()
        assertNotNull(instance)

        val onActivityCreated = clazz.getDeclaredMethod(
            "onActivityCreated",
            android.os.Bundle::class.java
        )
        assertNotNull(onActivityCreated)

        val onActivityResult = clazz.getDeclaredMethod(
            "onActivityResult",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            android.content.Intent::class.java
        )
        assertNotNull(onActivityResult)
    }
}
