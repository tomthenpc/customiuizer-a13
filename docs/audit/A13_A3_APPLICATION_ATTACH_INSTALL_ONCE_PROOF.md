# A13 A3 — Application.attach install-once / idempotence Static Proof

## 元信息

| 项目 | 值 |
|---|---|
| BASE_SHA | 6689ebe7719d5bb474f569648de8deea81ec003c |
| 分支 | `devin/a13-foundation-parity-r13.11.1` |
| 范围 | LauncherInstaller / GenericAppInstaller 的 `Application.attach` after callback |
| 性质 | 静态证明 / 文档证据；无生产代码改动 |

## 1. MainModule.onPackageReady 的 `lpparam.isFirstPackage()` 语义

源码：`app/src/main/java/tv/withaibuild/customiuizer/MainModule.java:89-95`

```java
public void onPackageReady(final PackageReadyParam lpparam) {
    if (!isSupportedAndroidVersion()) return;
    if (!lpparam.isFirstPackage()) return;

    String pkg = lpparam.getPackageName();
    ProcessScope scope = ProcessScopes.resolve(pkg, processName);
```

libxposed 官方文档对 `PackageReadyParam` 的说明：

- `onPackageReady` 对每个**加载进当前进程的包名**调用一次。
- `isFirstPackage()` 返回当前包是不是**当前进程中第一个/主包**。
- 一个进程可能加载多个包，例如 `sharedUserId` 或 `Context.createPackageContext(..., CONTEXT_INCLUDE_CODE)`。

因此 `lpparam.isFirstPackage()` 的实际保证是：

- 保证 `MainModule.onPackageReady` 对当前进程的**第一个包**只进入一次。
- 保证 `LauncherInstaller.installApplication()` / `GenericAppInstaller.install()` 在当前进程的**第一个包**上只被调用一次。
- **不保证**已注册的 `Application.attach` after callback 在整个进程中只触发一次，因为 `Application.class` 是框架基类，所有包的 `Application` 实例都会调用 `attach(Context)`。

## 2. Android 13 framework 中 `Application.attach` 的正常调用次数

AOSP `android-13.0.0_r1` 源码证据：

- `Application.attach(Context)` 是 `package-private final`（`core/java/android/app/Application.java:347-350`）：

  ```java
  /* package */ final void attach(Context context) {
      attachBaseContext(context);
      mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
  }
  ```

  这意味着它不能被 Application 子类覆盖，且只能由同包代码（Instrumentation）调用。

- `Instrumentation.newApplication(Class<?>, Context)` 内部**每个新 Application 实例调用一次 `attach`**（`core/java/android/app/Instrumentation.java`）。

- `LoadedApk.makeApplicationInner(..., boolean allowDuplicateInstances)`（`core/java/android/app/LoadedApk.java:1401-1424`）有缓存机制：

  ```java
  private Application makeApplicationInner(boolean forceDefaultAppClass,
          Instrumentation instrumentation, boolean allowDuplicateInstances) {
      if (mApplication != null) {
          return mApplication;
      }
      ...
      synchronized (sApplications) {
          final Application cached = sApplications.get(mPackageName);
          if (cached != null) {
              ...
              if (!allowDuplicateInstances) {
                  mApplication = cached;
                  return cached;
              }
              // Some apps intentionally call makeApplication() to create a new Application
              // instance... Sigh...
          }
      }
      ...
  }
  ```

- `ActivityThread.handleBindApplication(...)` 中只调用一次 `data.info.makeApplicationInner(...)` 和一次 `callApplicationOnCreate(app)`（`core/java/android/app/ActivityThread.java:6478` 附近）。

结论：在**一个包一个进程**的正常启动路径上，Android 13 framework 保证只创建一个 `Application` 实例，`Application.attach` 只调用一次。

## 3. 一个 Android app 进程生命周期内是否可能出现多个 Application instance

| 场景 | 是否可能多个 `Application` | 说明 |
|---|---|---|
| 正常单包启动 | 否 | `LoadedApk` 与 `sApplications` 缓存保证一个实例。 |
| 多进程 manifest (`android:process`) | 否（每个进程一个） | 是**不同进程**，不是同一进程内多个实例。 |
| `sharedUserId` + 同 `android:process` 的多个包 | **是** | 同一 UID/进程可加载多个 `LoadedApk`，每个包各自的 `Application` 会分别 `attach`。 |
| `Context.createPackageContext(..., CONTEXT_INCLUDE_CODE)` | **是** | libxposed 文档明确提到这种多包加载。 |
| Instrumentation / 测试 | 是 | 可通过 `newApplication` 或 `makeApplication(true, ...)` 创建额外实例，但不属于生产环境。 |
| framework 热加载 / package update | 通常否 | 会创建新的 `LoadedApk` 与 `ClassLoader`，`Application` 通常跟随进程重启而重建。 |

**重要区分**：
- `package` 是 Manifest 中的包名。
- `processName` 是当前进程名（由 AMS 指定）。
- 多个包可以在同一 `processName` 中运行，但每个包有自己的 `LoadedApk`、`ClassLoader` 和 `Application`。

## 4. `Application.class / attach(Context)` 的 libxposed hook 范围

`LauncherInstaller.installApplication` 与 `GenericAppInstaller.install` 中的注册方式：

```java
ModuleHelper.findAndHookMethod(Application.class, "attach", Context.class, new MethodHook() {
    @Override
    protected void after(AfterHookCallback param) throws Throwable {
        handleLoadLauncher(lpparam);   // 或 GenericApp 的 direct hooks
    }
});
```

`XposedHelpers.doHookMethod` / `ModuleHelper.doHookMethod` 只是调用 `moduleInst.hook(m).intercept(hook)`（`app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:853-859`、`app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:173-192`），**没有**按 `Application` 子类或包名过滤，也不会检测同一方法上是否已有该 hook。

`Application` 是框架基类，被子类继承。hook 注册在 `Application.class` 的 `attach(Context)` 上，因此：

- 在当前进程内，**任何** `Application` 子类的 `attach` 被调用都会触发同一个 after callback。
- callback 中使用的 `lpparam` 是**注册时**传入的对象（第一个包的 `PackageReadyParam`），不会随触发实例自动切换。
- 没有 package / process / ClassLoader 校验逻辑来过滤非目标 `Application` 实例。

## 5. LauncherInstaller `handleLoadLauncher` 安装路径分类

源码：`app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java:47-111`

### 5.1 catalog / FeatureDispatcher 路径

`handleLoadLauncher` 中调用的 `FeatureDispatcher.installById(...)` 包括：

- `folderColumns`
- `titleTopMargin`
- `noClockHide`
- `fixAppInfoLaunch`
- `hideLauncherTitles`
- `noWidgetOnly`

它们最终进入 `FeatureInstallRegistry.installById`（`app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:111-227`）。

- 状态键：`FeatureStateKey(runtime.processName, spec.id)`。
- `runtime.processName` 来自 `FeatureDispatcher.createRuntime(processName, ...)`，而 `LauncherInstaller` 传入的是 `lpparam.getPackageName()`（`LauncherInstaller.java:49`）。
- 状态机保证 `INSTALLED` 后再次调用返回 `AlreadyInstalled`，不会重复执行 installer。

因此 **catalog 路径具备 install-once 能力**，但该能力只能保护 `FeatureDispatcher.installById` 调用内部；它不会阻止 `handleLoadLauncher` 先创建 `FeatureRuntime`、再走一遍 `if` 条件判断，也不会阻止 catalog 路径**之前/之后**的直接 Hook 调用。

### 5.2 legacy direct hook 路径

`handleLoadLauncher` 其余调用为直接 `ModuleHelper.findAndHookMethod(...)` 或 `FeatureDispatcher` 之外的资源/Hook 函数，例如：

- `LauncherGestureHooks.HomescreenSwipesHook` / `HotSeatSwipesHook` / `ShakeHook` / `LauncherDoubleTapHook` / `LauncherPinchHook`
- `LauncherIconHooks.IconScaleHook` / `TitleFontSizeHook` / `RenameShortcutsHook` / `TitleShadowHook`
- `LauncherLayoutHooks.HideNavBarHook` / `InfiniteScrollHook` / `HorizontalSpacingRes` / `IndicatorHeightRes` / `IndicatorMarginTopHook` / `UnlockGridsRes` / `UnlockGridsHook` / `WorkspaceCellPaddingTopHook` / `DockMarginTopHook` / `DockMarginBottomHook` / `ResizableWidgetsHook`
- `LauncherSystemHooks.DisableLauncherLogHook`
- `LauncherAnimationHooks.RecentsBlurRatioHook` / `DisableLauncherWallpaperScale` / `FixAnimHook` / `NoZoomAnimationHook` / `UseOldLaunchAnimationHook`
- `LauncherSystemHooks.StickyFloatingWindowsLauncherHook` / `HideStatusBarInRecentsHook` / `HideFromRecentsHook` / `CloseDrawerOnLaunchHook`
- `LauncherFolderHooks.CloseFolderOnLaunchHook` / `PrivacyFolderHook` / `FolderBlurHook` / `CloseFolderOrDrawerOnLaunchShortcutMenuHook`
- `LauncherGestureHooks.AssistGestureActionHook` / `SwipeAndStopActionHook`
- `Controls.BackGestureAreaHeightHook` / `BackGestureAreaWidthHook`
- `SystemStatusBarAndClockHooks.HideMemoryCleanHook`
- `SystemFreeformAndMultiWindowHooks.MultiWindowPlusHook`

对 `Launcher*.kt`、`Controls.kt`、`SystemStatusBarAndClockHooks.kt`、`SystemFreeformAndMultiWindowHooks.kt` 的源码搜索表明：

- **没有任何一个相关 hook 函数内部使用 `isHooked` / `hooked` 类实例守卫**。
- `ModuleHelper.findAndHookMethod` / `XposedHelpers.doHookMethod` 内部也没有按 method 去重或只注册一次的逻辑。

因此，**如果 `handleLoadLauncher` 被回调多次，legacy direct hook 路径会在同一目标方法上再次添加 hook 回调，产生重复 hook / 重复执行**。

### 5.3 重复执行后果

- `findAndHookMethod` 第二次调用时，`XposedInterface.hook(m).intercept(hook)` 会添加第二个 interceptor。
- 目标方法被调用时，before/after 回调会执行多次。
- 对于修改参数/返回值/状态的 hook，多次执行会导致**数值被重复修改、条件判断被重复触发、listener/observer 被重复注册**等不可预期行为。
- 这不是 harmless no-op。

## 6. GenericAppInstaller `Application.attach` after 回调分析

源码：`app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java:40-52`

```java
if (isStatusBarColor || isNoOverscroll || controlMedia) {
    ModuleHelper.findAndHookMethod(Application.class, "attach", Context.class, new MethodHook() {
        @Override
        protected void after(AfterHookCallback param) throws Throwable {
            if (isStatusBarColor) {
                SystemStatusBarAndClockHooks.StatusBarBackgroundCompatHook(lpparam);
                SystemStatusBarAndClockHooks.StatusBarBackgroundHook(lpparam);
            }
            if (isNoOverscroll) SystemAudioAndVisualAndMoreHooks.NoOverscrollAppHook(lpparam);
            if (controlMedia) Controls.VolumeMediaPlayerHook(lpparam);
        }
    });
}
```

+ 回调中调用的 `StatusBarBackgroundCompatHook`、`StatusBarBackgroundHook`、`NoOverscrollAppHook`、`VolumeMediaPlayerHook` 均为直接 `findAndHookMethod` 调用。
+ 这四个函数体中没有局部 `isHooked` / `hooked` 守卫。
+ 同 Launcher，若 `Application.attach` after 回调被触发多次，这些函数会被再次调用，导致对应方法上被添加多个 hook 回调。

`Various.SmartClipboardActionHook(lpparam)` 与 `Various.AlarmCompatHook()` 在 `GenericAppInstaller.install` 中也属于直接调用；`AlarmCompatHook()` 直接 hook `Settings.System`（boot classloader），没有局部守卫。如果回调被再次触发，会重复注册该 hook。

## 7. Secondary process / package / processName / ProcessScope / ClassLoader

- `ProcessScopes.resolve`（`app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt:89-118`）对 `com.miui.home` / `com.mi.android.globallauncher` 直接返回 `ProcessScope.LAUNCHER`，不区分主/副进程。
- 副进程（如 `com.miui.home:remote`）的包名仍然是 `com.miui.home`，但 `MainModule.processName`（`app/src/main/java/tv/withaibuild/customiuizer/MainModule.java:55`）是实际进程名 `com.miui.home:remote`。
- libxposed 模块在每个进程中独立加载，因此每个进程有自己的 `MainModule` / `LauncherInstaller` / `GenericAppInstaller` 实例。
- `isFirstPackage()` 是**每个进程内**的判断，所以副进程的 `onPackageReady` 的 `isFirstPackage()` 仍为 `true`（对副进程而言它是第一个包），副进程会独立注册自己的 `Application.attach` hook。

这是合法行为，**不是**同一进程内的重复安装。真正需要关注的是：

- 同一进程内加载了**多个不同包**（如 `sharedUserId` / 同 `android:process`），导致 `Application.attach` 被调用多次。

## 8. ClassLoader 分析

- `Application.class` 由 boot classloader 加载。
- `lpparam.getClassLoader()` 是当前包的 `LoadedApk` 创建的 app classloader。
- `Application` 的子类（如 `com.miui.home.MiuiHomeApplication`）由该 app classloader 加载。
- 因此 `param.thisObject.getClass().getClassLoader()` 与 `lpparam.getClassLoader()` 在**目标包**的 `Application` 实例上是相同的；对于其它包加载进同一进程的 `Application` 实例，这个 ClassLoader 不同。

当前 `Application.attach` after 回调**没有**比较 `param.thisObject.getClass().getClassLoader()` 与 `lpparam.getClassLoader()`，也没有按 ClassLoader 记录已处理状态。

## 9. callback 可多次执行 → legacy hook 是否实际不具备 install-once

已知条件：

1. libxposed 文档确认一个进程可加载多个包（`sharedUserId` / `createPackageContext`）。
2. `Application.attach` 是 `Application` 基类方法，所有包的 `Application` 都会调用。
3. `MainModule.onPackageReady` 只对第一个包进入，但已注册的 `Application.attach` after callback 对所有包的 `attach` 都生效。
4. callback 体中的 `handleLoadLauncher` / GenericApp direct hooks 全部使用 `lpparam`（第一个包的参数），并直接调用 `ModuleHelper.findAndHookMethod`。
5. `findAndHookMethod` / `doHookMethod` 没有内置去重或 `isHooked` 检查。
6. Launcher / GenericApp 被调用的所有相关 hook 函数均没有局部 `isHooked` 守卫。

因此：**在“同一进程加载多个包”的场景下，`Application.attach` after callback 确实会多次执行，且 legacy direct hook 路径没有 install-once 能力，会产生重复安装**。

这个结论**不依赖 ROM 猎取**，只依赖 AOSP/framework/libxposed 文档 + 项目源码静态分析。

## 10. FeatureInstallRegistry 能保护什么 / 不能保护什么

能保护：

- 所有经过 `FeatureDispatcher.installById` 的 catalog feature，在 `FeatureRuntime.processName + canonicalId` 维度上保证 install-once（`FeatureInstallRegistry.kt:142-152, 199-220`）。
- 如果 `Application.attach` after 回调触发多次，只要 `handleLoadLauncher` 内调用 `FeatureDispatcher.installById`，registry 会返回 `AlreadyInstalled` 并跳过真正安装。

不能保护：

- `FeatureDispatcher.installById` **之前**的 `FeatureRuntime` 创建和 `if (prefs...)` 判断仍会每次执行（无副作用）。
- `handleLoadLauncher` 中**非 catalog 的直接 Hook 调用**不在 registry 管辖范围内，会重复执行。
- `GenericAppInstaller` 完全是 legacy direct hook，不走 `FeatureInstallRegistry`。

## 11. 分类结论

```text
A3_STATIC_RESULT = CONFIRMED_DUPLICATE_INSTALL_RISK
```

说明：

- 在“一个进程只加载一个包”的常见路径上，AOSP/framework 保证 `Application.attach` 只调用一次，风险不触发。
- 但在 libxposed 明确支持的“同一进程加载多个包”场景（`sharedUserId`、`Context.createPackageContext(..., CONTEXT_INCLUDE_CODE)`、同 `android:process` 的多个包）下，已注册的 `Application.attach` after callback 会被多个包的 `Application` 实例触发，`handleLoadLauncher` / GenericApp direct hooks 中没有 install-once 守卫，会在同一目标方法上重复添加 hook 回调。
- 该风险不是基于猜测，而是基于框架行为与项目源码结构确认的实际可能。

```text
A3_PRODUCTION_CHANGE_REQUIRED = YES (deferred to ChatGPT A3 corrective gate)
```

本轮任务仅提交 static proof 与最小设计，不实施生产改动。

## 12. 最小 corrective design（仅设计，未实现）

### 12.1 目标

- 防止 `Application.attach` after callback 对非目标包 / 已处理过的 `Application` 实例重复执行 legacy direct hooks。
- 不改变正常单包/单进程 hook 时机。
- 不阻止合法副进程的安装。
- 不阻止目标包在 ClassLoader 变化后的重新安装（例如 package reload）。

### 12.2 推荐方案

在 `LauncherInstaller.installApplication()` 与 `GenericAppInstaller.install()` 的 `Application.attach` after callback 入口处加入双重 guard：

```text
1. ClassLoader 身份校验：
   param.thisObject.getClass().getClassLoader() == lpparam.getClassLoader()
   目的：跳过非目标包的 Application 实例（同一进程加载的其它包）。

2. ClassLoader 已处理记录：
   static Set<ClassLoader> sHandledLoaders
   if (!sHandledLoaders.add(param.thisObject.getClass().getClassLoader())) return;
   目的：确保同一个 ClassLoader（即同一个包在同一进程中的 Application 类加载器）只触发一次。
```

### 12.3 关键设计点

- **guard identity**：app classloader（`param.thisObject.getClass().getClassLoader()`） + `lpparam.getClassLoader()` 一致性。
- **owner**：每个 installer 各自持有 `static final Set<ClassLoader>`（如 `LauncherInstaller.sHandledLoaders` / `GenericAppInstaller.sHandledLoaders`）。
- **process scope**：static 字段是每个进程内唯一的（模块在每个进程独立加载），因此不会跨进程泄漏。
- **ClassLoader scope**：使用 ClassLoader 而不是 package name 作为键，可以区分同一 package 的不同 ClassLoader 实例（例如 package reload），也能区分不同 package 在同一进程中的 ClassLoader。
- **Launcher 与 GenericApp 是否需要不同 guard**：是，两个 installer 注册独立的 `Application.attach` after callback，应使用各自的 `Set<ClassLoader>`，避免互相影响。
- **guard 放在 registration side 还是 callback side**：放在 callback side。`MainModule` 已经保证 `installApplication` / `install` 每个进程只注册一次；需要过滤的是 callback 触发时的非目标 / 重复实例。
- **为什么不阻止合法 secondary process**：每个进程有独立的模块 ClassLoader，static set 不共享；副进程的 `lpparam.getClassLoader()` 与 `param.thisObject.getClass().getClassLoader()` 匹配，因此首次触发可以正常通过。
- **为什么不改变 hook timing**：guard 在 after callback 开始处即时返回；正常单包场景下第一次 `Application.attach` 仍会完整执行，hook 时机不变。
- **为什么不阻止 retry after partial failure**：
  - 正常 Android 生命周期下，一个包的 `Application.attach` 对同一个 ClassLoader 不会调用第二次；没有“retry by re-attach”机制。
  - 如果因为 ClassLoader 重建（如 package reload）导致新的 `Application` 实例出现，`param.thisObject.getClass().getClassLoader()` 会变成新的对象，guard set 中没有该对象，允许重新执行。但旧 hook 仍附着在旧 ClassLoader 加载的类上，这需要由 A4/A5 阶段的 hook 生命周期策略处理，不在 A3 范围内。

### 12.4 替代方案（次选）

若不希望依赖 `param.thisObject.getClass().getClassLoader()`，可使用 `WeakHashMap<Application, Boolean>` 记录已处理的 `Application` 实例，并额外校验 `Application.mLoadedApk.mPackageName`（需要反射）与 `lpparam.getPackageName()` 一致。

但这种方案：
- 需要访问 package-private 的 `mLoadedApk` 字段，反射成本与稳定性不如 ClassLoader 校验。
- 不能自然处理 ClassLoader 变化。

因此 **ClassLoader 双重 guard 是更小的、更与 AOSP 语义一致的设计**。

## 13. 验证

本次只改动 `docs/audit/A13_A3_APPLICATION_ATTACH_INSTALL_ONCE_PROOF.md`，未修改生产或测试代码。

```text
python tools/verify.py fast --changed    OK
git diff --check                         OK
```

未运行 full gate。

## 14. 参考

- `app/src/main/java/tv/withaibuild/customiuizer/MainModule.java:89-95, 143-146, 162-174`
- `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java:47-111`
- `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java:40-52`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:111-227`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureRuntime.kt:22-27`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:173-192`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:853-859`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt:89-118`
- AOSP `core/java/android/app/Application.java:347-350` (`attach` final)
- AOSP `core/java/android/app/Instrumentation.java` (`newApplication` calls `attach`)
- AOSP `core/java/android/app/LoadedApk.java:1401-1424` (`makeApplicationInner` + `sApplications`)
- AOSP `core/java/android/app/ActivityThread.java:6478` (`handleBindApplication`)
- libxposed API 文档：`PackageReadyParam` / `isFirstPackage()` / `onPackageReady` 多包说明

---

```text
PRODUCTION_CHANGED = NO
B_STARTED = NO
FULL_GATE_RUN = NO
READY_FOR_CHATGPT_A3_STATIC_AUDIT = YES
```
