package tv.withaibuild.customiuizer.mods

import android.app.PendingIntent
import android.app.PendingIntentFactory
import android.app.RecordingActivityManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import com.android.systemui.Dependency
import com.android.systemui.statusbar.notification.modal.ModalController
import com.android.systemui.statusbar.notification.policy.AppMiniWindowManager
import com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
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
        Dependency.clear()
        AppMiniWindowManager.getInstance().calls.clear()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
        Dependency.clear()
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

    private fun createRow(
        pkgName: String = "com.example.app",
        uid: Int = 1000,
        miniWindowPkg: Any? = pkgName,
        pendingIntent: PendingIntent? = null
    ): MiuiNotificationMenuRow {
        val row = MiuiNotificationMenuRow()
        val context = FakeContext()
        val container = RecordingMenuContainer(context)
        val activityManager = RecordingActivityManager()
        context.fakeActivityManager = activityManager
        row.mContext = context
        row.mSbn = FakeStatusBarNotification(mPackageName = pkgName, mAppUid = uid)
        row.mParent = FakeExpandableNotificationRow(mMiniWindowTargetPkg = miniWindowPkg, mPendingIntent = pendingIntent)
        row.mMenuMargin = 10
        row.mMenuContainer = container
        return row
    }

    private fun moduleItems(row: MiuiNotificationMenuRow): List<Any> {
        return row.mMenuItems.filter { item ->
            val title = item.javaClass.getDeclaredField("titleResId").apply { isAccessible = true }.get(item) as Int
            title != 0
        }
    }

    private fun moduleView(row: MiuiNotificationMenuRow, index: Int): View {
        val items = moduleItems(row)
        val item = items.getOrNull(index) ?: throw AssertionError("module item at index $index not found (only ${items.size})")
        val getMenuView = XposedHelpers.findMethodBestMatch(item.javaClass, "getMenuView")
        return getMenuView.invoke(item) as View
    }

    private fun clickModule(row: MiuiNotificationMenuRow, index: Int) {
        val view = moduleView(row, index) as RecordingMenuItemView
        view.storedOnClickListener?.onClick(view)
    }

    private fun clickAppInfo(row: MiuiNotificationMenuRow) = clickModule(row, 0)
    private fun clickForceClose(row: MiuiNotificationMenuRow) = clickModule(row, 1)
    private fun clickOpenFloatingWindow(row: MiuiNotificationMenuRow) = clickModule(row, 2)

    @Test
    fun createMenuViews_after_addsThreeModuleItems() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        assertEquals(5, row.mMenuItems.size)
        assertEquals(3, moduleItems(row).size)
        val container = row.mMenuContainer as RecordingMenuContainer
        assertEquals(5, container.addedChildren.size)
    }

    @Test
    fun createMenuViews_after_setsOnClickListenersOnModuleItems() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        for (item in moduleItems(row)) {
            val getMenuView = XposedHelpers.findMethodBestMatch(item.javaClass, "getMenuView")
            val view = getMenuView.invoke(item) as RecordingMenuItemView
            assertNotNull(view.storedOnClickListener)
        }
    }

    @Test
    fun createMenuViews_after_doesNotSetOnClickListenersOnSystemItems() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        val systemItems = row.mMenuItems - moduleItems(row).toSet()
        assertEquals(2, systemItems.size)
        for (item in systemItems) {
            val getMenuView = XposedHelpers.findMethodBestMatch(item.javaClass, "getMenuView")
            val view = getMenuView.invoke(item) as RecordingMenuItemView
            assertNull(view.storedOnClickListener)
        }
    }

    @Test
    fun createMenuViews_after_handlesMissingNotification() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        row.mSbn = null
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        assertEquals(5, row.mMenuItems.size)
        assertEquals(3, moduleItems(row).size)
    }

    @Test
    fun createMenuViews_originalClearsBeforeAfter_doesNotAccumulate() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        assertEquals(5, row.mMenuItems.size)
        val container = row.mMenuContainer as RecordingMenuContainer
        assertEquals(5, container.addedChildren.size)
    }

    @Test
    fun createMenuViews_originalPreservesBeforeAfter_accumulates() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        row.originalClearsAndAddsSystemItems = false
        val preserveItem = Any()
        row.mMenuItems.add(preserveItem)
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)
        hook.afterHook(afterCb)

        assertEquals(7, row.mMenuItems.size)
    }

    @Test
    fun click_appInfo_startsActivityAndClosesDialogs() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow(pkgName = "com.example.target")
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        clickAppInfo(row)

        val context = row.mContext as FakeContext
        assertEquals(1, context.startedActivities.size)
        assertEquals(1, context.sentBroadcasts.size)
    }

    @Test
    fun click_forceClose_primaryUser_stopsPackage() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow(pkgName = "com.example.target", uid = 1000)
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        clickForceClose(row)

        val am = (row.mContext as FakeContext).fakeActivityManager as RecordingActivityManager
        assertEquals(listOf("com.example.target" to null), am.forceStopCalls)
    }

    @Test
    fun click_forceClose_secondaryUser_stopsPackageAsUser() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow(pkgName = "com.example.target", uid = 1010000)
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        clickForceClose(row)

        val am = (row.mContext as FakeContext).fakeActivityManager as RecordingActivityManager
        assertEquals(listOf("com.example.target" to 10), am.forceStopCalls)
    }

    @Test
    fun click_openFloatingWindow_launchesMiniWindowAndCollapsesModel() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val pi = PendingIntentFactory.newPendingIntent()
        val row = createRow(miniWindowPkg = "com.example.floating", pendingIntent = pi)
        val modalController = ModalController()
        Dependency.setMock(ModalController::class.java, modalController)

        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        clickOpenFloatingWindow(row)

        assertTrue(modalController.animExitCalled)
        val calls = AppMiniWindowManager.getInstance().calls
        assertEquals(1, calls.size)
        assertEquals("com.example.floating", calls[0].first)
        assertNotNull(calls[0].second)
    }

    @Test
    fun click_usesCurrentNotificationBindingNotCreationSnapshot() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow(pkgName = "com.example.old", uid = 1000, miniWindowPkg = "com.example.old.float")
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        row.mSbn = RuntimeStatusBarNotification(packageName = "com.example.new", appUid = 1010000)
        row.mParent = RuntimeExpandableNotificationRow(
            miniWindowTargetPkg = "com.example.new.float",
            pendingIntent = PendingIntentFactory.newPendingIntent()
        )

        clickForceClose(row)

        val am = (row.mContext as FakeContext).fakeActivityManager as RecordingActivityManager
        assertEquals(listOf("com.example.new" to 10), am.forceStopCalls)

        val modalController = ModalController()
        Dependency.setMock(ModalController::class.java, modalController)
        AppMiniWindowManager.getInstance().calls.clear()

        clickOpenFloatingWindow(row)

        assertTrue(modalController.animExitCalled)
        assertEquals("com.example.new.float", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun click_runtimeSubtypeFallback_whenDeclaredBaseLacksMethods() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = MiuiNotificationMenuRow()
        val context = FakeContext()
        context.fakeActivityManager = RecordingActivityManager()
        row.mContext = context
        row.mMenuContainer = RecordingMenuContainer(context)
        row.mSbn = RuntimeStatusBarNotification(packageName = "com.example.subtype", appUid = 1000)
        row.mParent = RuntimeExpandableNotificationRow(
            miniWindowTargetPkg = "com.example.subtype.float",
            pendingIntent = PendingIntentFactory.newPendingIntent()
        )

        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        clickForceClose(row)

        val am = (row.mContext as FakeContext).fakeActivityManager as RecordingActivityManager
        assertEquals(listOf("com.example.subtype" to null), am.forceStopCalls)

        val modalController = ModalController()
        Dependency.setMock(ModalController::class.java, modalController)

        clickOpenFloatingWindow(row)

        assertTrue(modalController.animExitCalled)
        assertEquals("com.example.subtype.float", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun click_appInfo_missingNotification_doesNothing() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        row.mSbn = null
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        clickAppInfo(row)

        assertEquals(0, (row.mContext as FakeContext).startedActivities.size)
    }

    @Test
    fun click_openFloatingWindow_missingParent_doesNothing() {
        installHook()
        val hook = getRecordedHook("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews")

        val row = createRow()
        row.mParent = null
        val afterCb = fakeAfterCallback(row, listOf(false, false))
        row.createMenuViews(false, false)
        hook.afterHook(afterCb)

        Dependency.setMock(ModalController::class.java, ModalController())
        clickOpenFloatingWindow(row)

        assertEquals(0, AppMiniWindowManager.getInstance().calls.size)
    }
}
