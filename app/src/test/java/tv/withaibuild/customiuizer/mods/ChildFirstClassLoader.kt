package tv.withaibuild.customiuizer.mods

import java.io.File
import java.net.URL
import java.net.URLClassLoader

class ChildFirstClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {

    override fun loadClass(name: String?, resolve: Boolean): Class<*>? {
        val already = findLoadedClass(name)
        if (already != null) return already

        try {
            return findClass(name)
        } catch (ignored: ClassNotFoundException) {
        }

        return super.loadClass(name, resolve)
    }

    companion object {
        fun forTest(parent: ClassLoader): ChildFirstClassLoader {
            val root = File(System.getProperty("user.dir", "."))
            val candidates = listOf(
                File(root, "app/build/tmp/kotlin-classes/debugUnitTest"),
                File(root, "app/build/intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes"),
            )
            val urls = candidates.filter { it.exists() }.map { it.toURI().toURL() }.toTypedArray()
            return ChildFirstClassLoader(urls, parent)
        }
    }
}
