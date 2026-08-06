package tv.withaibuild.customiuizer.mods

import android.view.View
import android.widget.LinearLayout
import com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class NotificationRowMenuHookTest {

    private val parentClassLoader: ClassLoader
        get() = this.javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    private fun lpparam(): XposedModuleInterface.PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(XposedModuleInterface.PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.android.systemui"
                "getProcessName" -> "com.android.systemui"
                "getClassLoader" -> parentClassLoader
                else -> null
            }
        } as XposedModuleInterface.PackageReadyParam
    }

    private fun installHook() {
        SystemNotificationMoreHooks.NotificationRowMenuHook(lpparam())
    }

    private fun fakeBeforeCallback(thisObject: Any?, args: List<Any?> = emptyList()): HookerClassHelper.BeforeHookCallback {
        val chain = FakeChain(null, thisObject, args, null, null)
        val constructor = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredConstructor(XposedInterface.Chain::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(chain)
    }

    private fun fakeAfterCallback(thisObject: Any?, args: List<Any?> = emptyList(), result: Any? = null): HookerClassHelper.AfterHookCallback {
        val before = fakeBeforeCallback(thisObject, args)
        val constructor = HookerClassHelper.AfterHookCallback::class.java.getDeclaredConstructor(
            HookerClassHelper.BeforeHookCallback::class.java,
            Any::class.java,
            Throwable::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(before, result, null)
    }

    private fun getRecordedHook(targetClassName: String, targetMethodName: String): HookerClassHelper.MethodHook {
        val recorded = FakeXposedInterface.recordedHooks.filter {
            val clsName = it.executable.declaringClass.name
            val name = if (it.executable is java.lang.reflect.Constructor<*>) "<init>" else it.executable.name
            clsName == targetClassName && name == targetMethodName
        }
        assertTrue("expected hook for $targetClassName#$targetMethodName", recorded.isNotEmpty())
        return recorded.first().hook
    }

    private fun createRow(pkgName: String = "com.example.app", uid: Int = 1000): MiuiNotificationMenuRow {
        val row = MiuiNotificationMenuRow()
        val context = FakeContext()
        val container = RecordingMenuContainer(context)
        row.mContext = context
        row.mSbn = FakeStatusBarNotification(mPackageName = pkgName, mAppUid = uid)
        row.mParent = FakeExpandableNotificationRow(mMiniWindowTargetPkg = pkgName, mPendingIntent = null)
        row.mMenuMargin = 10
        row.mMenuContainer = container
        return row
    }

    @Test
    fun createMenuViews_after_addsThreeMenuItems() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        hook.afterHook(afterCb)

        assertEquals(3, row.mMenuItems.size)
        val container = row.mMenuContainer as RecordingMenuContainer
        assertEquals(3, container.addedChildren.size)
    }

    @Test
    fun createMenuViews_after_setsOnClickListeners() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        hook.afterHook(afterCb)

        val item = row.mMenuItems[0]
        val getMenuView = XposedHelpers.findMethodBestMatch(item.javaClass, "getMenuView")
        val view = getMenuView.invoke(item) as RecordingMenuItemView
        assertNotNull(view.storedOnClickListener)
    }

    @Test
    fun createMenuViews_after_setsListenersOnAllMenuItems() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        hook.afterHook(afterCb)

        for (item in row.mMenuItems) {
            val getMenuView = XposedHelpers.findMethodBestMatch(item.javaClass, "getMenuView")
            val view = getMenuView.invoke(item) as RecordingMenuItemView
            assertNotNull(view.storedOnClickListener)
        }
    }

    @Test
    fun createMenuViews_after_handlesMissingNotification() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        row.mSbn = null
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        hook.afterHook(afterCb)

        assertEquals(3, row.mMenuItems.size)
        val item = row.mMenuItems[0]
        val getMenuView = XposedHelpers.findMethodBestMatch(item.javaClass, "getMenuView")
        val view = getMenuView.invoke(item) as RecordingMenuItemView
        assertNotNull(view.storedOnClickListener)
    }

    @Test
    fun createMenuViews_after_doesNotRecreateMenuItemsOnSecondCall() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        hook.afterHook(afterCb)
        hook.afterHook(afterCb)

        assertEquals(6, row.mMenuItems.size)
    }
}
