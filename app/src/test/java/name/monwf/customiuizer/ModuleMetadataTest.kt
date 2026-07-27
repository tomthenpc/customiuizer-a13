package name.monwf.customiuizer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Properties

class ModuleMetadataTest {

    @Test
    fun modulePropDeclaresApi101WithTarget102() {
        val stream = javaClass.classLoader?.getResourceAsStream("META-INF/xposed/module.prop")
            ?: error("META-INF/xposed/module.prop not found on test classpath")
        val props = Properties()
        stream.use { props.load(it) }
        assertEquals("101", props.getProperty("minApiVersion"))
        assertEquals("102", props.getProperty("targetApiVersion"))
        assertEquals("false", props.getProperty("staticScope"))
    }

    @Test
    fun javaInitListDeclaresMainModule() {
        val stream = javaClass.classLoader?.getResourceAsStream("META-INF/xposed/java_init.list")
            ?: error("META-INF/xposed/java_init.list not found on test classpath")
        val text = stream.bufferedReader().use { it.readText().trim() }
        assertEquals("name.monwf.customiuizer.MainModule", text)
    }
}
