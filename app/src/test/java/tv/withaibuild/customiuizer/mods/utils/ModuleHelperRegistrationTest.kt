package tv.withaibuild.customiuizer.mods.utils

import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class ModuleHelperRegistrationTest {

    @Before
    @After
    fun clearRegistries() {
        ModuleHelper.prefObservers.clear()
        clearCollection("keyedPrefObservers")
        clearCollection("ownedPrefObservers")
        clearCollection("moduleRegistrations")
    }

    @Test
    fun processObserverKeyReplacesPreviousObserver() {
        var oldCalls = 0
        var newCalls = 0
        ModuleHelper.observePreferenceChange("process.key") { oldCalls++ }
        ModuleHelper.observePreferenceChange("process.key") { newCalls++ }

        ModuleHelper.handlePreferenceChanged("changed")

        assertEquals(0, oldCalls)
        assertEquals(1, newCalls)
    }

    @Test
    fun ownerAndKeyReplaceOnlyTheSameOwnedObserver() {
        val owner = Any()
        var oldCalls = 0
        var newCalls = 0
        ModuleHelper.observePreferenceChange("owner.key", owner) { oldCalls++ }
        ModuleHelper.observePreferenceChange("owner.key", owner) { newCalls++ }

        ModuleHelper.handlePreferenceChanged("changed")

        assertEquals(0, oldCalls)
        assertEquals(1, newCalls)
        ModuleHelper.removePreferenceObserver("owner.key", owner)
    }

    @Test
    fun differentOwnersWithTheSameKeyRemainIndependent() {
        val firstOwner = Any()
        val secondOwner = Any()
        var firstCalls = 0
        var secondCalls = 0
        ModuleHelper.observePreferenceChange("owner.key", firstOwner) { firstCalls++ }
        ModuleHelper.observePreferenceChange("owner.key", secondOwner) { secondCalls++ }

        ModuleHelper.handlePreferenceChanged("changed")

        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)
        ModuleHelper.removePreferenceObserver("owner.key", firstOwner)
        ModuleHelper.removePreferenceObserver("owner.key", secondOwner)
    }

    @Test
    fun ownerAwareCallbackIsRetainedWithoutCapturingOwner() {
        val owner = Any()
        var callbackOwner: Any? = null
        var callbackKey: String? = null

        ModuleHelper.observeOwnedPreferenceChange("owner.callback", owner) { currentOwner, key ->
            callbackOwner = currentOwner
            callbackKey = key
        }

        val registrations = ownedPreferenceRegistrations()
        val registration = requireNotNull(registrations.single())
        val callbackField = registration.javaClass.getDeclaredField("callback").apply {
            isAccessible = true
        }
        val observerRefField = registration.javaClass.getDeclaredField("observerRef").apply {
            isAccessible = true
        }
        assertNull(observerRefField.get(registration))
        assertNotNull(callbackField.get(registration))

        ModuleHelper.handlePreferenceChanged("changed")

        assertSame(owner, callbackOwner)
        assertEquals("changed", callbackKey)
        ModuleHelper.removePreferenceObserver("owner.callback", owner)
    }

    @Test
    fun replacingModuleRegistrationCleansOnlyThePreviousValue() {
        var firstCleanups = 0
        var secondCleanups = 0
        ModuleHelper.replaceModuleRegistration("registration.key", Runnable { firstCleanups++ })

        ModuleHelper.replaceModuleRegistration("registration.key", Runnable { secondCleanups++ })

        assertEquals(1, firstCleanups)
        assertEquals(0, secondCleanups)
        ModuleHelper.clearModuleRegistration("registration.key")
        ModuleHelper.clearModuleRegistration("registration.key")
        assertEquals(1, secondCleanups)
    }

    @Test
    fun registrationKeysDoNotCleanEachOther() {
        var firstCleanups = 0
        var secondCleanups = 0
        ModuleHelper.replaceModuleRegistration("registration.first", Runnable { firstCleanups++ })
        ModuleHelper.replaceModuleRegistration("registration.second", Runnable { secondCleanups++ })

        ModuleHelper.clearModuleRegistration("registration.first")

        assertEquals(1, firstCleanups)
        assertEquals(0, secondCleanups)
        ModuleHelper.clearModuleRegistration("registration.second")
    }

    private fun clearCollection(fieldName: String) {
        val field = ModuleHelper::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        when (val value = field.get(null)) {
            is MutableMap<*, *> -> value.clear()
            is MutableCollection<*> -> value.clear()
            is CopyOnWriteArrayList<*> -> value.clear()
            else -> error("Unsupported registry: $fieldName")
        }
    }

    private fun ownedPreferenceRegistrations(): List<*> {
        val field = ModuleHelper::class.java.getDeclaredField("ownedPrefObservers")
        field.isAccessible = true
        return (field.get(null) as Collection<*>).toList()
    }
}
