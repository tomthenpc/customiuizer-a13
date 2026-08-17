# A13 A3 — Application.attach install-once / idempotence Static Proof

## 元信息

| 项目 | 值 |
|---|---|
| BASE_SHA | 8bdb901ce6f1a4640da952b10de7557f30b0506b |
| 分支 | `devin/a13-foundation-parity-r13.11.1` |
| 范围 | LauncherInstaller / GenericAppInstaller 的 `Application.attach` after callback |
| 性质 | 静态证明 / 文档证据；无生产代码改动 |

## 1. 重要区分：PACKAGE_LOAD_EVIDENCE != APPLICATION_CREATION_EVIDENCE

- `PackageReadyParam` / `onPackageReady` 是 **package-loaded** 事件。
- `createPackageContext(..., CONTEXT_INCLUDE_CODE)` 可以加载另一个包的 `LoadedApk`，但**不保证**会创建该包的 `Application`。
- 真正会在同一进程中创建第二个 `Application` 并调用 `Application.attach` 的生产路径是 **Activity 组件启动**：

  ```text
  ActivityThread.performLaunchActivity
    → ActivityThread.getPackageInfo(applicationInfo, ..., CONTEXT_INCLUDE_CODE)
    → LoadedApk.makeApplicationInner(false, mInstrumentation)
    → Instrumentation.newApplication(Class, Context)
    → Application.attach(Context)
  ```

  AOSP `android-13.0.0_r1` 源码：

  - `ActivityThread.performLaunchActivity`（`core/java/android/app/ActivityThread.java:3548`）

    ```java
    if (r.packageInfo == null) {
        r.packageInfo = getPackageInfo(aInfo.applicationInfo, r.compatInfo,
                Context.CONTEXT_INCLUDE_CODE);
    }
    ...
    ContextImpl appContext = createBaseContextForActivity(r);
    ...
    Application app = r.packageInfo.makeApplicationInner(false, mInstrumentation);
    ```

  - `ActivityThread.getPackageInfo(..., int flags)`（`ActivityThread.java:2534`）

    ```java
    boolean includeCode = (flags&Context.CONTEXT_INCLUDE_CODE) != 0;
    ...
    return getPackageInfo(ai, compatInfo, null, securityViolation, includeCode, registerPackage);
    ```

    当 `includeCode=true` 时，会按 `packageName` 从 `mPackages` 缓存取 `LoadedApk`；若不存在则新建一个 `LoadedApk`（`ActivityThread.java:2637`）。

  - `LoadedApk.makeApplicationInner(false, ...)`（`LoadedApk.java:1395-1420`）

    - 若 `mApplication != null` 返回已有实例。
    - 否则通过 `sApplications`（per-process、key 为 `packageName`）缓存取/创建。
    - `performLaunchActivity` 调用的是 `makeApplicationInner(..., allowDuplicateInstances=false)`。

  因此，**当同一进程启动了属于另一个包的 Activity**（需要 `android:sharedUserId` + 同 `android:process`，或系统路径等条件），framework 会为该包创建新的 `LoadedApk`、新的 `Application`，并调用新的 `Application.attach`。

## 2. `lpparam.isFirstPackage()` 实际保证什么

源码：`app/src/main/java/tv/withaibuild/customiuizer/MainModule.java:89-95`

```java
public void onPackageReady(final PackageReadyParam lpparam) {
    if (!isSupportedAndroidVersion()) return;
    if (!lpparam.isFirstPackage()) return;
    ...
```

libxposed 文档：

- `onPackageReady` 对加载进当前进程的每个包调用一次。
- `isFirstPackage()` 只说明当前包是不是当前进程中的第一个/主包。
- 一个进程可能加载多个包。

结论：

- `lpparam.isFirstPackage()` 保证 `MainModule.onPackageReady` 以及由它触发的 `LauncherInstaller.installApplication()` / `GenericAppInstaller.install()` **只进入一次**。
- 它**不保证**已注册的 `Application.attach` after callback 在进程内只触发一次，因为 `Application.class` 是 framework 基类，任何包的 `Application` 实例调用 `attach` 都会触发该 hook。

## 3. Android 13 framework 中 `Application.attach` 的正常调用次数

- `Application.attach(Context)` 是 `package-private final`（`Application.java:347-350`），子类无法覆盖。
- `Instrumentation.newApplication(Class<?>, Context)` 会在**每个新创建的 `Application` 实例**上调用一次 `attach`。
- `LoadedApk.makeApplicationInner(false, ...)` 会缓存并返回同一 `Application` 实例，因此同一 `LoadedApk` 不会重复 `attach`。
- `ActivityThread.handleBindApplication` 在单包进程启动时只调用一次 `makeApplicationInner` 与一次 `callApplicationOnCreate`。

所以，在**一个包独占一个进程**的常见路径上，`Application.attach` 只调用一次。

## 4. 一个 Android app 进程生命周期内是否可能出现多个 `Application` 实例

| 场景 | 同一进程内是否会有多个 `Application` 实例 | 说明 |
|---|---|---|
| 正常单包启动 | 否 | `LoadedApk` 缓存保证。 |
| 多进程 manifest (`android:process`) | 否 | 每个进程一个，属于不同进程。 |
| `sharedUserId` + 同 `android:process` 的多个包 | **是** | 同一 UID/同一 process 可加载多个 `LoadedApk`，各自 `Application`。 |
| 系统 / 内容提供方启动另一包的组件 | **是** | 例如 `ActivityThread.performLaunchActivity` 为 `aInfo.applicationInfo` 对应包新建 `LoadedApk` 与 `Application`。 |
| Instrumentation / 测试 | 是 | 不属于生产环境。 |

核心区分：

- `package` = Manifest 包名。
- `processName` = 当前进程名。
- 多个包可以在同一 `processName` 中运行，每个包有自己的 `LoadedApk`、`ClassLoader`、`Application`。

## 5. `Application.class / attach(Context)` 的 libxposed hook 范围

`LauncherInstaller.installApplication` 与 `GenericAppInstaller.install` 中的注册方式：

```java
ModuleHelper.findAndHookMethod(Application.class, "attach", Context.class, new MethodHook() {
    @Override
    protected void after(AfterHookCallback param) throws Throwable {
        ...
    }
});
```

`XposedHelpers.doHookMethod` / `ModuleHelper.doHookMethod` 只是调用
`moduleInst.hook(m).intercept(hook)`（`XposedHelpers.java:853-859`），**没有**按包名或 `Application` 实例过滤，也不会检测同一方法上是否已经存在该 hook。

因此：

- `Application.attach` hook 注册在 `Application` 基类上，继承到所有 `Application` 子类。
- 当前进程内**任何** `Application` 实例的 `attach` 被调用都会触发同一个 after callback。
- callback 中使用的 `lpparam` 是**注册时**传入的对象（第一个包），不会随触发实例自动切换。
- callback 内没有 package / process / ClassLoader 校验逻辑来过滤非目标 `Application` 实例。

## 6. Application 子类的 ClassLoader：不得使用 `param.thisObject.getClass().getClassLoader()`

- `Application` 的子类可能由 app classloader 加载（自定义 Application）。
- 若应用没有自定义 `Application`，framework 使用 `android.app.Application`，该类由 boot/system classloader 加载。
- 因此 `param.thisObject.getClass().getClassLoader()` **不能**作为通用 package identity：
  - 默认 `Application` 的 defining ClassLoader 是 boot loader。
  - 自定义 `Application` 的 defining ClassLoader 是 app loader。
  - 同一 package 在不同进程中的 defining ClassLoader 也是不同对象。

正确的 package identity 应使用 **Application 的 base Context 提供的 package name**：

```text
((Application) param.thisObject).getPackageName()
```

- `Application` 继承 `ContextWrapper.getPackageName()`，delegate 到已 attach 的 `ContextImpl`。
- 在 after callback 中，base Context 已经完成 attach，因此可用。
- 该方法对 `android.app.Application` 和自定义 `Application` 均有效。

如需辅助校验 ClassLoader，只能使用 `Application` 作为 `ContextWrapper` 提供的 ClassLoader：

```text
((Application) param.thisObject).getClassLoader()
```

这等同于 `ContextImpl.mPackageInfo.getClassLoader()`（即该 package 的 `LoadedApk` ClassLoader）。

但在本场景中，**package identity 校验已足够关闭 cross-package duplicate risk**；额外 ClassLoader 校验只在需要区分同一 package 的 ClassLoader 重新加载（reload）时才有收益，而 reload 会导致 callback 持有的 `lpparam` 已过期，属于不同问题域。因此优先保持 package identity 方案简单。

## 7. LauncherInstaller `handleLoadLauncher` 安装路径分类

源码：`app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java:47-111`

### 7.1 catalog / FeatureDispatcher 路径

`handleLoadLauncher` 中调用的 `FeatureDispatcher.installById(...)` 包括：

- `folderColumns`
- `titleTopMargin`
- `noClockHide`
- `fixAppInfoLaunch`
- `hideLauncherTitles`
- `noWidgetOnly`

这些最终进入 `FeatureInstallRegistry.installById`（`FeatureInstallRegistry.kt:111-227`）：

- 状态键：`FeatureStateKey(runtime.processName, spec.id)`。
- `runtime.processName` 来自 `FeatureDispatcher.createRuntime(processName, ...)`，而 `LauncherInstaller` 传入的是 `lpparam.getPackageName()`。
- 一旦状态为 `INSTALLED`，再次调用返回 `AlreadyInstalled`，不重新执行 installer。

因此 **catalog 路径有 install-once 能力**。

### 7.2 legacy direct hook 路径

`handleLoadLauncher` 其余调用为直接 `ModuleHelper.findAndHookMethod(...)`，包括大量 `Launcher*.kt`、`Controls.kt`、`SystemStatusBarAndClockHooks.kt`、`SystemFreeformAndMultiWindowHooks.kt` 中的函数。

对 `Launcher*.kt`、`Controls.kt`、`SystemStatusBarAndClockHooks.kt`、`SystemFreeformAndMultiWindowHooks.kt` 的源码搜索表明：

- **这些函数内部没有使用 `isHooked` / `hooked` 类实例守卫**。
- `ModuleHelper.findAndHookMethod` / `XposedHelpers.doHookMethod` 内部也没有按 method 去重或只注册一次的逻辑。

因此，**如果 `handleLoadLauncher` 被回调多次，legacy direct hook 路径会在同一目标方法上再次添加 hook 回调，产生重复执行**。

### 7.3 重复执行后果

- `findAndHookMethod` 第二次调用时，libxposed `hook(m).intercept(hook)` 会添加第二个 interceptor。
- 目标方法被调用时，before/after 回调会执行多次。
- 对于修改参数/返回值/状态的 hook，多次执行会导致重复修改、重复触发、重复注册 listener 等不可预期行为。
- 这不是 harmless no-op。

## 8. GenericAppInstaller `Application.attach` after 回调分析

源码：`app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java:40-52`

真正位于 `Application.attach` after callback 内部的只有四条路径：

1. `SystemStatusBarAndClockHooks.StatusBarBackgroundCompatHook(lpparam)`
2. `SystemStatusBarAndClockHooks.StatusBarBackgroundHook(lpparam)`
3. `SystemAudioAndVisualAndMoreHooks.NoOverscrollAppHook(lpparam)`
4. `Controls.VolumeMediaPlayerHook(lpparam)`

```java
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
```

这四条 hook 函数：

- 直接调用 `ModuleHelper.findAndHookMethod` / `ModuleHelper.hookMethod`。
- 没有局部 `isHooked` / `hooked` 守卫。
- 若 after callback 被触发多次，会在同一目标方法上重复注册 hook。

`Various.SmartClipboardActionHook(lpparam)` 与 `Various.AlarmCompatHook()` 在 `GenericAppInstaller.install` 中**不位于 `Application.attach` callback 内**，它们只会在 `install()` 被调用时执行一次（由 `isFirstPackage()` 保证）。因此它们不能作为 `Application.attach` 重复回调的证据。

## 9. Secondary process / package / processName / ProcessScope / ClassLoader

- `ProcessScopes.resolve`（`ProcessScope.kt:89-118`）对 `com.miui.home` / `com.mi.android.globallauncher` 直接返回 `ProcessScope.LAUNCHER`，不区分主/副进程。
- 副进程（如 `com.miui.home:remote`）的包名仍是 `com.miui.home`，`MainModule.processName` 是实际进程名。
- libxposed 模块在每个进程中独立加载，因此每个进程有自己的 `MainModule` / `LauncherInstaller` / `GenericAppInstaller` 实例。
- `isFirstPackage()` 是**每个进程内**的判断，所以副进程的 `onPackageReady` 的 `isFirstPackage()` 仍为 `true`（对副进程而言它是第一个包），副进程会独立注册自己的 `Application.attach` hook。

这是合法行为，不是同一进程内的重复安装。真正需要关注的是：

- 同一进程内启动了**属于另一个包的组件**，导致第二个 `Application.attach` 被调用。

## 10. ClassLoader 分析

- `Application.class` 由 boot classloader 加载。
- `lpparam.getClassLoader()` 是当前包的 `LoadedApk` 创建的 app classloader。
- `Application` 子类（如有）由该 app classloader 加载；默认 `android.app.Application` 由 boot classloader 定义，但实例化时携带的 base Context 仍属于该 package 的 `LoadedApk`。
- `((Application) param.thisObject).getClassLoader()` 返回的是 `ContextImpl` 中的 package ClassLoader（即 `LoadedApk` 的 ClassLoader），与 `lpparam.getClassLoader()` 对应。

当前 `Application.attach` after callback 没有：
- package name 校验；
- Application ClassLoader 校验；
- 已处理状态记录。

## 11. `FeatureInstallRegistry` 能保护什么 / 不能保护什么

能保护：

- 所有经过 `FeatureDispatcher.installById` 的 catalog feature，在 `FeatureRuntime.processName + canonicalId` 维度上保证 install-once。
- 如果 `Application.attach` after callback 触发多次，只要 `handleLoadLauncher` 内调用 `FeatureDispatcher.installById`，registry 会返回 `AlreadyInstalled` 并跳过真正安装。

不能保护：

- `FeatureDispatcher.installById` 之前的直接 `if (prefs...)` 判断和 `FeatureRuntime` 创建仍会每次执行（无副作用）。
- `handleLoadLauncher` 中**非 catalog 的直接 Hook 调用**不在 registry 管辖范围内，会重复执行。
- `GenericAppInstaller` 完全是 legacy direct hook，不走 `FeatureInstallRegistry`。

## 12. 重新评估 install-once guard

### A. package identity filter 是否足以关闭 cross-package duplicate risk？

**是。**

在 after callback 中加入：

```text
if (!lpparam.getPackageName().equals(((Application) param.thisObject).getPackageName())) return;
```

即可过滤掉同一进程中由其它包触发的 `Application.attach`。callback 只会在目标包的 `Application` 上继续执行。

### B. 是否还需要 process-local state 处理同一 package 的 duplicate `Application`？

**建议增加一个最小状态作为 defense in depth。**

- AOSP 正常路径保证同一 package 在同一进程内只有一个 `Application`。
- 但 `LoadedApk` 提供 `makeApplication(true, ...)`（`allowDuplicateInstances=true`）的公开/隐藏路径，存在创建第二个 `Application` 实例的理论可能。
- 因此使用一个 process-local `Set<String>` 记录已处理过的 package name，可在同一 package 的 `Application.attach` 被异常地再次触发时避免重复执行。

### C. guard 细节

- **执行前 claim 还是成功后 publish？**  执行前 claim。
  - `Application.attach` 在主线程同步调用，不存在并发问题；claim-before-execute 可以避免同一线程内的重复进入。
- **ordinary partial failure 怎么处理？**
  - 由于 `Application.attach` 在正常生命周期中不会被同一 package 调用第二次，callback 不应被设计为在同一 Application 生命周期内 retry。
  - 若某个 hook 因 `ClassNotFoundException` 等失败，应继续执行其它 hook；整体 callback 仍被标记为已处理。
  - 真正的“ClassLoader 变化后重新安装”需要新的 `PackageReadyParam` / 新的 installer 调用，不在 A3 范围内。
- **fatal failure 怎么处理？**
  - callback 体应保持 A1/A2 的 fatal 边界（`RuntimeFatality.throwIfFatal` 优先）。
  - 在 guard 之前的 `getPackageName()` 调用没有 fatal 风险；若 fatal 出现在 hook 内部，会按现有 contract 向上传播。
- **是否存在合法 retry？**
  - 同一 Application 实例：不存在。
  - 新的 `Application` 实例 / 新的 `LoadedApk`（package reload）：package name 相同，若使用 package name 状态会被跳过；但此时原 callback 持有的 `lpparam` 已过期，重新执行将使用过期 `ClassLoader`，因此跳过是正确的。
- **secondary process 为什么不受影响？**
  - static `Set<String>` 在每个进程有独立实例（module class 每进程加载）。
  - 每个进程的 `lpparam.getPackageName()` 独立，因此 package identity filter 在每个进程第一次触发时都会通过。

## 13. 最小 corrective design（仅设计，未实现）

### 13.1 目标

- 防止 `Application.attach` after callback 对非目标包 / 已处理过的 package 重复执行 legacy direct hooks。
- 不改变正常单包/单进程 hook 时机。
- 不阻止合法副进程的安装。
- 不使用 `param.thisObject.getClass().getClassLoader()` 作为 identity。

### 13.2 推荐方案

在 `LauncherInstaller.installApplication()` 与 `GenericAppInstaller.install()` 的 `Application.attach` after callback 入口处加入两层 guard：

```java
// 1. package identity 过滤：跳过非目标包的 Application 实例
String currentPkg = ((Application) param.thisObject).getPackageName();
if (!currentPkg.equals(lpparam.getPackageName())) {
    return;
}

// 2. process-local 已处理记录：确保同一 package 在本进程只执行一次
if (!sProcessedPackages.add(currentPkg)) {
    return;
}
```

- `sProcessedPackages` 是 installer 持有的 `Set<String>`，例如 `ConcurrentHashMap.newKeySet()`。
- 由于 module 在每个进程独立加载，该集合是 per-process 的。
- package identity filter 优先；`Set` 作为同一 package 重复 `Application` 的二次保护。

### 13.3 关键设计点

- **guard identity**：`Application.getPackageName()`（base Context 提供的 package name）。
- **owner**：`LauncherInstaller` 与 `GenericAppInstaller` 各自持有独立的 `static Set<String>`。
- **process scope**：static 字段 per-process。
- **ClassLoader scope**：不使用 defining ClassLoader；需要时只能使用 `Application.getClassLoader()`（package ClassLoader）作为辅助校验，但本设计不依赖它。
- **Launcher 与 GenericApp 是否需要不同 guard**：是，两个 installer 各自注册独立的 `Application.attach` after callback，应使用各自的 `Set`，避免互相影响。
- **guard 放在 registration side 还是 callback side**：callback side。`MainModule` 已保证 `installApplication` / `install` 每个进程只注册一次；需要过滤的是 callback 触发时的非目标 / 重复实例。
- **为什么不会阻止合法 secondary process**：每个进程有独立的模块 Class 与 `lpparam`，package identity filter 会放行每个进程自己的第一次 `Application.attach`。
- **为什么不会改变 hook timing**：guard 在 after callback 开始处即时返回；正常单包场景第一次 `Application.attach` 仍完整执行。
- **为什么不会阻止合法 retry**：
  - 同一 `Application` 实例不会调用两次 `attach`。
  - 新 `Application` 实例但同一 package name 的情况，原 `lpparam` 已过期，跳过是正确的。
  - 真正的 package reload / ClassLoader 替换需要新的 `PackageReadyParam`，应通过重新进入 `onPackageReady` 处理，不在 A3 范围内。

### 13.4 不推荐的方案

- `param.thisObject.getClass().getClassLoader()`：默认 `Application` 与自定义 `Application` 的 defining ClassLoader 不一致，不能作为 package identity。
- `Collections.newSetFromMap(WeakHashMap<Application, Boolean>)`：比 package name 更重，且 `Application` 实例在单包单进程内唯一，没有额外收益。

## 14. 结论

```text
A3_STATIC_RESULT = CONFIRMED_DUPLICATE_INSTALL_RISK
```

条件：

```text
MULTI_PACKAGE_SAME_PROCESS
+ SECOND_PACKAGE_APPLICATION_CREATION
(via ActivityThread.performLaunchActivity -> LoadedApk.makeApplicationInner)
```

在常见单包单进程路径上风险不触发；但在“同一进程启动另一个包的组件”这一 production 路径上，第二个 `Application.attach` 会触发已注册的 after callback，导致 Launcher / GenericApp 的 legacy direct hook 被重复安装。

```text
A3_PRODUCTION_CHANGE_REQUIRED = YES (deferred to ChatGPT A3 corrective gate)
```

本轮仅修正文档，不实施生产改动。

## 15. 验证

本次只改动 `docs/audit/A13_A3_APPLICATION_ATTACH_INSTALL_ONCE_PROOF.md`，未修改生产或测试代码。

```text
python tools/verify.py fast --changed    OK
git diff --check                         OK
```

未运行 full gate。

## 16. 参考

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
- AOSP `core/java/android/app/LoadedApk.java:1395-1424` (`makeApplicationInner` + `sApplications`)
- AOSP `core/java/android/app/ActivityThread.java:2534, 2637` (`getPackageInfo` 新建 `LoadedApk`)
- AOSP `core/java/android/app/ActivityThread.java:3548` (`performLaunchActivity` 调用 `makeApplicationInner`)
- AOSP `core/java/android/app/ActivityThread.java:6478` (`handleBindApplication`)
- libxposed API 文档：`PackageReadyParam` / `isFirstPackage()` / `onPackageReady` 多包说明

---

```text
PRODUCTION_CHANGED = NO
B_STARTED = NO
FULL_GATE_RUN = NO
READY_FOR_CHATGPT_A3_STATIC_CORRECTIVE_AUDIT = YES
```
