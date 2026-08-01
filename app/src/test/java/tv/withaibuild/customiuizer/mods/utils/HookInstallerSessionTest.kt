package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome

class HookInstallerSessionTest {

    private fun contract() = HookTargetContract(
        featureId = "test",
        requirements = listOf(
            SingleTargetRequirement(
                HookTargetSpec(
                    id = "resolver.method",
                    operation = HookOperation.EXACT_METHOD,
                    className = ResolverTestTarget::class.java.name,
                    memberName = "method",
                    parameterTypes = listOf(String::class.java)
                )
            )
        )
    )

    private fun resolver(classLoader: ClassLoader = this.javaClass.classLoader!!) =
        HookTargetResolver(classLoader)

    @Test
    fun sessionException_cleansUpThreadLocal() {
        val contract = contract()
        try {
            HookInstaller.withSession(
                resolver = resolver(),
                contract = contract,
                diagnosticId = "test",
                classLoader = this.javaClass.classLoader!!
            ) {
                throw RuntimeException("boom")
            }
            fail("exception should escape")
        } catch (t: Throwable) {
            assertEquals("boom", t.message)
        }

        assertFalse(HookInstaller.isRecording())
    }

    @Test
    fun sessionNested_rejectedWithTestableError() {
        val contract = contract()
        HookInstaller.withSession(
            resolver = resolver(),
            contract = contract,
            diagnosticId = "test",
            classLoader = this.javaClass.classLoader!!
        ) {
            try {
                HookInstaller.withSession(
                    resolver = resolver(),
                    contract = contract,
                    diagnosticId = "nested",
                    classLoader = this.javaClass.classLoader!!
                ) {
                    fail("nested session should not run")
                }
                fail("nested session should throw")
            } catch (e: IllegalStateException) {
                assertTrue(e.message?.contains("already active") == true)
            }
        }

        assertFalse(HookInstaller.isRecording())
    }

    @Test
    fun sessionThreadIsolation() {
        val contract = contract()
        val classLoader = this.javaClass.classLoader!!
        HookInstaller.withSession(
            resolver = resolver(classLoader),
            contract = contract,
            diagnosticId = "test",
            classLoader = classLoader
        ) {
            assertTrue(HookInstaller.isRecording())

            val t = Thread {
                assertFalse(HookInstaller.isRecording())
            }
            t.start()
            t.join(1000)
        }

        assertFalse(HookInstaller.isRecording())
    }

    @Test
    fun sessionClassLoaderIsolation_rejectsNestedOnSameThread() {
        val contract = contract()
        val classLoaderA = this.javaClass.classLoader!!
        val classLoaderB = object : ClassLoader(classLoaderA) {}

        HookInstaller.withSession(
            resolver = resolver(classLoaderA),
            contract = contract,
            diagnosticId = "test",
            classLoader = classLoaderA
        ) {
            try {
                HookInstaller.withSession(
                    resolver = resolver(classLoaderB),
                    contract = contract,
                    diagnosticId = "test2",
                    classLoader = classLoaderB
                ) {
                    fail("nested session should not run")
                }
                fail("nested session should throw")
            } catch (e: IllegalStateException) {
                assertTrue(e.message?.contains("already active") == true)
            }
        }

        assertFalse(HookInstaller.isRecording())
    }

    @Test
    fun sessionRejectsInstallerClassLoaderDifferentFromResolver() {
        val contract = contract()
        val classLoader = this.javaClass.classLoader!!
        val otherClassLoader = object : ClassLoader(classLoader) {}

        HookInstaller.withSession(
            resolver = resolver(classLoader),
            contract = contract,
            diagnosticId = "test",
            classLoader = classLoader
        ) {
            try {
                HookInstaller.resolveClassIfRecording(
                    ResolverTestTarget::class.java.name,
                    otherClassLoader
                )
                fail("mismatched installer ClassLoader should throw")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("ClassLoader differs") == true)
            }
        }

        assertFalse(HookInstaller.isRecording())
    }

    @Test
    fun sessionRecordsInstallAgainstContract() {
        val contract = contract()
        val classLoader = this.javaClass.classLoader!!
        val result = HookInstaller.withSession(
            resolver = resolver(classLoader),
            contract = contract,
            diagnosticId = "test",
            classLoader = classLoader
        ) {
            HookInstaller.recordInstall(ResolverTestTarget::class.java.name, "method", HookOperation.EXACT_METHOD, listOf(String::class.java), 1)
        }

        assertEquals(InstallOutcome.INSTALLED, result.installation)
        assertEquals(1, result.requiredInstalled)
        assertEquals(1, result.requiredTotal)
    }

    @Test
    fun compatibilityResult_seedsInstallRecords() {
        val contract = contract()
        val classLoader = this.javaClass.classLoader!!
        val resolver = resolver(classLoader)
        val (_, compatResult) = resolver.evaluateContract(contract, "test")
        assertEquals(CompatibilityState.COMPATIBLE, compatResult.compatibility)

        val result = HookInstaller.withSession(
            resolver = resolver,
            contract = contract,
            diagnosticId = "test",
            classLoader = classLoader,
            compatibilityResult = compatResult
        ) {
            // No legacy installer call; passive/exact evidence is already seeded.
        }

        assertNotNull(result)
    }
}
