package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.graphics.drawable.Drawable
import com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow
import com.android.systemui.statusbar.notification.row.NotificationGuts
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.lang.reflect.Constructor

/**
 * Tests for the bounded notification-menu item constructor resolver.
 *
 * The resolver is intentionally cold-path: it runs once when
 * [SystemNotificationMoreHooks.NotificationRowMenuHook] installs, and the
 * `createMenuViews` after-callback only reuses the resolved [Constructor].
 */
class NotificationMenuItemConstructorResolverTest {

    private val parentClassLoader: ClassLoader
        get() = this.javaClass.classLoader!!

    private fun resolve(
        menuItemClass: Class<*>,
        menuRowClass: Class<*>,
        classLoader: ClassLoader = parentClassLoader
    ): Constructor<*>? {
        val instanceField = SystemNotificationMoreHooks::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        val instance = instanceField.get(null)!!
        val method = SystemNotificationMoreHooks::class.java.getDeclaredMethod(
            "resolveNotificationMenuItemConstructor",
            Class::class.java,
            Class::class.java,
            ClassLoader::class.java
        ).apply { isAccessible = true }
        return method.invoke(instance, menuItemClass, menuRowClass, classLoader) as? Constructor<*>
    }

    @Test
    fun hyperOS_abi_gutsContent_resolved() {
        val ctor = resolve(
            MiuiNotificationMenuRow.MiuiNotificationMenuItem::class.java,
            MiuiNotificationMenuRow::class.java
        )
        assertNotNull(ctor)
        val types = ctor!!.parameterTypes
        assertSame(MiuiNotificationMenuRow::class.java, types[0])
        assertSame(Context::class.java, types[1])
        assertSame(Int::class.javaPrimitiveType, types[2])
        assertSame(NotificationGuts.GutsContent::class.java, types[3])
        assertSame(Int::class.javaPrimitiveType, types[4])
    }

    @Test
    fun legacy_abi_drawable_resolved() {
        val ctor = resolve(
            LegacyMenuRow.LegacyMenuItem::class.java,
            LegacyMenuRow::class.java
        )
        assertNotNull(ctor)
        val types = ctor!!.parameterTypes
        assertSame(LegacyMenuRow::class.java, types[0])
        assertSame(Context::class.java, types[1])
        assertSame(Int::class.javaPrimitiveType, types[2])
        assertSame(Drawable::class.java, types[3])
        assertSame(Int::class.javaPrimitiveType, types[4])
    }

    @Test
    fun zeroMatch_returnsNull() {
        val ctor = resolve(
            NoMatchMenuRow.NoMatchMenuItem::class.java,
            NoMatchMenuRow::class.java
        )
        assertNull(ctor)
    }

    @Test
    fun ambiguousMatch_returnsNull() {
        val ctor = resolve(
            AmbiguousMenuRow.AmbiguousMenuItem::class.java,
            AmbiguousMenuRow::class.java
        )
        assertNull(ctor)
    }

    @Test
    fun constructor_enumeration_stays_in_resolver() {
        // Source-structure regression: constructor lookup helpers must only be
        // invoked from the cold-path resolver, never from the createMenuViews
        // after-callback. If the source file cannot be located, skip.
        val source = java.io.File(
            System.getProperty("user.dir"),
            "app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt"
        )
        assumeTrue("source file not found", source.exists())

        val text = source.readText()
        val resolverStart = text.indexOf("private fun resolveNotificationMenuItemConstructor")
        assumeTrue("resolver not found in source", resolverStart >= 0)
        val resolverEnd = text.indexOf("\n    private fun ", resolverStart + 1)
            .let { if (it >= 0) it else text.length }

        val resolverBody = text.substring(resolverStart, resolverEnd)
        val remainder = text.substring(0, resolverStart) + text.substring(resolverEnd)

        val resolverTokens = listOf("findConstructorBestMatch", "declaredConstructors")
        for (token in resolverTokens) {
            assertTrue(
                "constructor lookup token '$token' must appear inside resolveNotificationMenuItemConstructor",
                token in resolverBody
            )
        }
        val forbidden = listOf("findConstructorBestMatch", "declaredConstructors", "getDeclaredConstructors")
        for (token in forbidden) {
            assertFalse(
                "constructor lookup token '$token' must not appear outside resolveNotificationMenuItemConstructor",
                token in remainder
            )
        }
    }

    // ---- synthetic fixtures ----

    open class LegacyMenuRow {
        inner class LegacyMenuItem(
            context: Context,
            val titleResId: Int,
            val icon: Drawable?,
            val iconResId: Int
        )
    }

    open class NoMatchMenuRow {
        inner class NoMatchMenuItem(
            context: Context,
            val a: Int,
            val b: Int,
            val c: Int,
            val d: Int
        )
    }

}
