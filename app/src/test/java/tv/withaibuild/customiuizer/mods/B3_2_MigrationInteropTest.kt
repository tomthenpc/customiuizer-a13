package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class B3_2_MigrationInteropTest {

    @Test
    fun `Various is migrated to Kotlin object`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val instanceField = clazz.getField("INSTANCE")
        assertNotNull(instanceField)
        assertEquals(clazz, instanceField.type)
    }

    @Test
    fun `Various preserves static fields for last package info and support fragment`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val lastPackageInfo = clazz.getField("mLastPackageInfo")
        assertNotNull(lastPackageInfo)
        assertEquals("android.content.pm.PackageInfo", lastPackageInfo.type.name)

        val supportFragment = clazz.getField("mSupportFragment")
        assertNotNull(supportFragment)
        assertEquals(Any::class.java, supportFragment.type)
    }

    @Test
    fun `Various checkBundle preserves signature`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val method = clazz.getMethod("checkBundle", Class.forName("android.content.Context"), Class.forName("android.os.Bundle"))
        assertNotNull(method)
        assertEquals("android.os.Bundle", method.returnType.name)
    }

    @Test
    fun `Various AppInfoHook accepts PackageReadyParam`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val paramClass = Class.forName("io.github.libxposed.api.XposedModuleInterface\$PackageReadyParam")
        val method = clazz.getMethod("AppInfoHook", paramClass)
        assertNotNull(method)
    }

    @Test
    fun `Various AppsDisableHook accepts PackageReadyParam`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val paramClass = Class.forName("io.github.libxposed.api.XposedModuleInterface\$PackageReadyParam")
        val method = clazz.getMethod("AppsDisableHook", paramClass)
        assertNotNull(method)
    }

    @Test
    fun `Various AlarmCompatServiceHook accepts SystemServerStartingParam`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val paramClass = Class.forName("io.github.libxposed.api.XposedModuleInterface\$SystemServerStartingParam")
        val method = clazz.getMethod("AlarmCompatServiceHook", paramClass)
        assertNotNull(method)
    }

    @Test
    fun `Various exposes hook methods with PackageReadyParam`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val paramClass = Class.forName("io.github.libxposed.api.XposedModuleInterface\$PackageReadyParam")
        val methods = arrayOf(
            "AppsDefaultSortHook",
            "AppsRestrictHook",
            "AppsRestrictPowerHook",
            "PersistBatteryOptimizationHook",
            "AddSideBarExpandReceiverHook",
            "InterceptPermHook",
            "PrivacyAppsLayoutHook",
            "OpenByDefaultHook",
            "SkipSecurityScanHook",
            "SmartClipboardActionHook",
            "ShowTempInBatteryHook",
            "DisableDockSuggestHook",
            "UnlockClipboardAndLocationHook",
            "AnswerCallInHeadUpHook",
            "ShowCallUIHook",
            "InCallBrightnessHook",
            "AppInfoDuringMiuiInstallHook",
            "MiuiPackageInstallerHook",
            "GboardPaddingHook",
            "FixInputMethodBottomMarginHook"
        )
        for (name in methods) {
            val method = clazz.getMethod(name, paramClass)
            assertNotNull("$name missing", method)
        }
    }

    @Test
    fun `Various exposes zero-arg hook methods`() {
        val clazz = Class.forName("tv.withaibuild.customiuizer.mods.Various")
        val methods = arrayOf("NoLowBatteryWarningHook", "AlarmCompatHook")
        for (name in methods) {
            val method = clazz.getMethod(name)
            assertNotNull("$name missing", method)
        }
    }
}
