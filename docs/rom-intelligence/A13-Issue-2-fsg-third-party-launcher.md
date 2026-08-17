# A13 Issue #2 — Horizontal FSG / Third-Party Launcher Evidence

> A13 Stage F 静态证据文档。所有 ROM/APK 证据均来自本地缓存；实机验证未进行。

## Executive summary

`controls_fsg_horiz`（设置中显示为 *Horizontal gestures / 水平手势*，summary 为 *Enable both horizontal gestures and navigation bar / 同时启用水平手势和导航栏*）的 hook 安装路径在 `com.miui.home` 主进程内是完整的。然而，该功能最终能否在桌面/UI 层创建水平手势 stub view，取决于小米 Launcher 内部的 `mIsUseMiuiHomeAsDefaultHome` 字段，而当前模块并没有 override 这个字段或 `com.miui.home.launcher.common.Utilities.getDefaultHomePackageName()`。

当用户将第三方桌面设为默认桌面时，`BaseRecentsImpl` 判定 `mIsUseMiuiHomeAsDefaultHome = false`，导致 `addFsgGestureWindow()` 与 `updateFsgWindowState()` 都不会调用 `createAndAddNavStubView()` / `addBackStubWindow()` / `showBackStubWindow()`。因此水平手势条 / 侧滑返回 stub 不会被创建。这是 ROM 原生行为与模块未覆盖该分支共同作用的结果，而不是 hook 未安装或 `REAL_FORCE_FSG_NAV_BAR` 未初始化导致的崩溃。

| Gate field | Value |
|---|---|
| `STAGE_F` | `PASS` for static evidence closure |
| `PRODUCTION_AUTHORIZATION` | `NO` |
| `PRODUCTION_CHANGED` | `false` |
| `DEVICE_VERIFIED` | `NO` |
| `RUNTIME_RESOLUTION_CLAIM` | `NO` |
| `ISSUE_2_STATIC_STATE` | `UPSTREAM_DEFAULT_HOME_GATE_BLOCKS_FSG_WHEN_THIRD_PARTY_LAUNCHER` |
| `STATIC_CLASSIFICATION` | `UPSTREAM_LIFECYCLE_LIMITATION` |
| `RUNTIME_CLASSIFICATION` | `UNVERIFIED` |

本批不做 production 修改；任何 runtime 结论均需实机验证。

## 1. Issue 描述

- GitHub Issue #2：开启 `controls_fsg_horiz` 后，当默认桌面为第三方 launcher 时，水平手势 + 导航栏行为不生效。
- 复现场景（静态假设）：用户把系统默认桌面从 `com.miui.home` 改为其他桌面（如 `com.jeejen.family` 等）。
- 目标 ROM：基于小米 Launcher `com.miui.home` `RELEASE-4.39.14.8060-04191512`，Android 13 / API 33。

## 2. 涉及 preference 与调用链

### 2.1 `controls_fsg_horiz`

| 字段 | 值 |
|---|---|
| UI key | `pref_key_controls_fsg_horiz` |
| Runtime key | `controls_fsg_horiz` |
| App-selection key | `pref_key_controls_fsg_horiz_apps` / `controls_fsg_horiz_apps` |
| Default | `false` |
| Title | `Horizontal gestures` |
| Summary | `Enable both horizontal gestures and navigation bar` |

`prefs_controls.xml:325-349` 定义了该选项；`controls_fsg_horiz_apps` 依赖 `controls_fsg_horiz`。

### 2.2 Production call chain

```text
MainModule.onPackageReady()
  → ProcessScopes.resolve(pkg, processName) == ProcessScope.LAUNCHER
  → PreferenceLoadRegistry.shouldLoad(remote, pkg)  // true for com.miui.home
  → initPrefs()
  → LauncherInstaller.hasAnyLauncherPackageReadyFeature(mPrefs)  // true when controls_fsg_horiz == true
  → LauncherInstaller.installPackageReady(lpparam)
      → LauncherInstaller.handleLoadLauncher(lpparam)
  → LauncherInstaller.hasAnyLauncherApplicationFeature(mPrefs)   // true when controls_fsg_horiz == true
  → LauncherInstaller.installApplication(lpparam)
      → Application.attach() after-callback
      → handleLoadLauncher(lpparam)
          → if ("com.miui.home".equals(lpparam.getPackageName()))
              → if (MainModule.mPrefs.getBoolean("controls_fsg_horiz"))
                  → LauncherGestureHooks.FSGesturesHook(lpparam)
```

`ProcessScope.kt:92` 把 `com.miui.home` 和 `com.mi.android.globallauncher` 都映射为 `ProcessScope.LAUNCHER`。`MainModule:162-174` 对 launcher scope 调用 `installPackageReady` 和 `installApplication`。`LauncherInstaller:74-78` 只在 `lpparam.packageName == "com.miui.home"` 时才安装 FSG 相关 hook。因此 `com.mi.android.globallauncher` 被识别为 launcher scope，但**不会**执行 `FSGesturesHook`。

注意：默认桌面如果是 `com.miui.home` 之外的第三方应用（如 `com.jeejen.family`），它既不在 `ProcessScopes.isKnownPackage()` 中，也不会被 `PreferenceLoadRegistry` 判定为需要加载偏好，因此模块不会在该进程中初始化。该功能实际运行在 `com.miui.home` 进程内。

## 3. 精确 ROM/APK 证据

| 字段 | 值 |
|---|---|
| `PACKAGE` | `com.miui.home` |
| `VERSION_NAME` | `RELEASE-4.39.14.8060-04191512` |
| `VERSION_CODE` | `439148060` |
| `APK_SIZE` | `24782235` |
| `APK_SHA256` | `b507f1cbf2d8fbc445398a2402ff9dd3f22580265b0ef9de07b4b37889b3384b` |
| `CERT_SHA256` | `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025` |
| `SIGNATURE_VARIANT` | `7b6d` |
| `ARTIFACT_SOURCE` | MemeOS Updates public mirror (APKMemeOS download link) |
| `EXACT_VERSION` | `YES` |

APK 保存在外部证据缓存，未纳入仓库版本控制。

## 4. 当前 `FSGesturesHook` 实现

`app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt:251-305`：

```kotlin
fun FSGesturesHook(lpparam: PackageReadyParam) {
    val baseRecentsClass = XposedHelpers.findClass(
        "com.miui.home.recents.BaseRecentsImpl",
        lpparam.classLoader
    )

    ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "usingFsGesture", HookerClassHelper.returnConstant(true))

    ModuleHelper.findAndHookMethodSilently("com.miui.home.recents.BaseRecentsImpl", lpparam.classLoader, "createAndAddNavStubView", object : MethodHook() {
        override fun before(param: BeforeHookCallback) {
            val fsg = XposedHelpers.getAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR") as? Boolean ?: false
            if (!fsg) param.returnAndSkip(null)
        }
    })

    ModuleHelper.findAndHookMethodSilently("com.miui.home.recents.BaseRecentsImpl", lpparam.classLoader, "updateFsgWindowState", object : MethodHook() {
        override fun after(param: AfterHookCallback) {
            val fsg = XposedHelpers.getAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR") as? Boolean ?: false
            if (fsg) return
            val mNavStubView = XposedHelpers.getObjectField(param.getThisObject(), "mNavStubView")
            val mWindowManager = XposedHelpers.getObjectField(param.getThisObject(), "mWindowManager")
            if (mWindowManager != null && mNavStubView != null) {
                XposedHelpers.callMethod(mWindowManager, "removeView", mNavStubView)
                XposedHelpers.setObjectField(param.getThisObject(), "mNavStubView", null)
            }
        }
    })

    ModuleHelper.findAndHookMethodSilently("com.miui.launcher.utils.MiuiSettingsUtils", lpparam.classLoader, "getGlobalBoolean", android.content.ContentResolver::class.java, String::class.java, object : MethodHook() {
        override fun after(param: AfterHookCallback) {
            if (param.getArg(1) != "force_fsg_nav_bar") return
            for (el in Thread.currentThread().stackTrace) {
                if (el.className == "com.miui.home.recents.BaseRecentsImpl") {
                    XposedHelpers.setAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR", param.getResult())
                    param.setResult(true)
                    return
                }
            }
        }
    })

    ModuleHelper.findAndHookMethod("com.miui.home.recents.GestureStubView", lpparam.classLoader, "onTouchEvent", MotionEvent::class.java, object : MethodHook() {
        override fun before(param: BeforeHookCallback) {
            val event = param.getArg(0) as? MotionEvent ?: return
            if (event.action != MotionEvent.ACTION_DOWN) return
            val foregroundInfo = miui.process.ProcessManager.getForegroundInfo()
            if (foregroundInfo != null) {
                val pkgName = foregroundInfo.mForegroundPackageName
                if (MainModule.mPrefs.getStringSet("controls_fsg_horiz_apps").contains(pkgName)) param.returnAndSkip(false)
            }
        }
    })
}
```

### 4.1 关键状态：`REAL_FORCE_FSG_NAV_BAR`

- 首次发布点：`MiuiSettingsUtils.getGlobalBoolean("force_fsg_nav_bar")` 被 `BaseRecentsImpl` 调用时（`addFsgGestureWindow:24` 和 `updateFsgWindowState:5`）。
- 发布方式：`setAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR", param.getResult())`，然后 `param.setResult(true)`。
- 默认值：`getAdditionalStaticField(..., ...) as? Boolean ?: false`，未发布前为 `false`。
- 读取点：
  - `createAndAddNavStubView` before-callback；
  - `updateFsgWindowState` after-callback。

## 5. 精确 DEX 生命周期证据

### 5.1 `BaseRecentsImpl` 构造器

`com/miui/home/recents/BaseRecentsImpl; <init> (Landroid/content/Context;)V`，decompiled from `C:\Home\xiaomi\rom\A13\_evidence_cache\launcher\fsg_methods\com_miui_home_recents_BaseRecentsImpl__init_dad.java`：

```java
this.mContext = p10;
this.mWindowManager = ((android.view.WindowManager) this.mContext.getSystemService("window"));
this.mKM = ((android.app.KeyguardManager) p10.getSystemService("keyguard"));
this.mHandler = ...
this.initSFDeviceGestureHelper();
this.addFsgGestureWindow();  // 构造器内直接调用
```

构造器在初始化 `mWindowManager`、`mHandler` 后立即调用 `addFsgGestureWindow()`，这是 `getGlobalBoolean("force_fsg_nav_bar")` 的首次调用的最可能位置。

### 5.2 `addFsgGestureWindow()`

`com/miui/home/recents/BaseRecentsImpl; addFsgGestureWindow ()V`，decompiled：

```java
private void addFsgGestureWindow() {
    this.mHasNavigationBar = com.miui.home.launcher.DeviceConfig.isHasNavigationBar();
    if (this.mHasNavigationBar) {
        ...
        this.mIsFsgNavBar = com.miui.launcher.utils.MiuiSettingsUtils.getGlobalBoolean(
            this.mContext.getContentResolver(), "force_fsg_nav_bar");
        this.mHideGestureLine = ...
        ...
        String v0_3 = com.miui.home.launcher.common.Utilities.getDefaultHomePackageName(this.mContext);
        if ((android.text.TextUtils.isEmpty(v0_3)) || (!v0_3.equals(this.mContext.getPackageName()))) {
            v2_0 = 0;
        }
        this.mIsUseMiuiHomeAsDefaultHome = v2_0;
        ...
        this.updateUseLauncherRecentsAndFsGesture();
        if ((this.mIsFsgNavBar) && ((!this.mIsInAnotherPro) && (this.mIsUseMiuiHomeAsDefaultHome))) {
            this.createAndAddNavStubView();
            this.showBackStubWindow();
        }
    }
}
```

静态分析要点：

1. `mIsFsgNavBar` 从 `getGlobalBoolean("force_fsg_nav_bar")` 读取；模块会让其看到 `true`。
2. `mIsUseMiuiHomeAsDefaultHome` 取决于 `getDefaultHomePackageName(context)` 是否等于 `com.miui.home`。
3. 仅当 `mIsFsgNavBar && !mIsInAnotherPro && mIsUseMiuiHomeAsDefaultHome` 时才创建 nav stub 并显示 back stub。

### 5.3 `updateFsgWindowState()`

`com/miui/home/recents/BaseRecentsImpl; updateFsgWindowState ()V`，decompiled：

```java
private void updateFsgWindowState() {
    if (this.mHasNavigationBar) {
        boolean v0_7 = com.miui.launcher.utils.MiuiSettingsUtils.getGlobalBoolean(
            this.mContext.getContentResolver(), "force_fsg_nav_bar");
        if (v0_7 != this.mIsFsgNavBar) {
            this.mIsFsgNavBar = v0_7;
            ...
        }
        if (!com.miui.home.recents.SpecialFDeviceGestureHelper.getInstance().isInSFDeviceFoldedMode()) {
            if ((!v0_7) || (!this.mIsUseMiuiHomeAsDefaultHome)) {
                this.removeNavStubView();
                this.clearBackStubWindow();
            } else {
                if (this.mNavStubView == null) {
                    this.createAndAddNavStubView();
                }
                this.addBackStubWindow();
            }
        }
    }
}
```

要点：

- `v0_7` 被模块强制为 `true`。
- 第二个分支条件仍然是 `!this.mIsUseMiuiHomeAsDefaultHome`：若默认桌面不是 `com.miui.home`，调用 `removeNavStubView()` / `clearBackStubWindow()`。
- 若默认桌面是 `com.miui.home`，才调用 `createAndAddNavStubView()` / `addBackStubWindow()`。

### 5.4 `DeviceConfig.usingFsGesture()`

`com/miui/home/launcher/DeviceConfig; usingFsGesture ()Z`：

```java
public static boolean usingFsGesture() {
    return com.miui.home.launcher.DeviceConfig.sIsGestureEnable;
}
```

模块通过 `returnConstant(true)` 覆盖此方法的返回值，使 `sIsGestureEnable` 的静态值对 `DeviceConfig` 调用者不再重要。但 `BaseRecentsImpl` 的 stub 创建逻辑并不直接调用 `usingFsGesture()`。

### 5.5 目标 ABI 存在性

| 目标 | DEX 存在 | 签名/说明 |
|---|---|---|
| `Lcom/miui/home/launcher/DeviceConfig;` | 是 | `usingFsGesture()Z` |
| `Lcom/miui/home/recents/BaseRecentsImpl;` | 是 | `addFsgGestureWindow()V`, `createAndAddNavStubView()V`, `updateFsgWindowState()V` |
| `Lcom/miui/home/recents/GestureStubView;` | 是 | `onTouchEvent(Landroid/view/MotionEvent;)Z` |
| `Lcom/miui/launcher/utils/MiuiSettingsUtils;` | 是 | `getGlobalBoolean(Landroid/content/ContentResolver; Ljava/lang/String;)Z` |
| `Lcom/miui/home/launcher/common/Utilities;` | 是 | `getDefaultHomePackageName(Landroid/content/Context;)Ljava/lang/String;` |

## 6. `REAL_FORCE_FSG_NAV_BAR` 状态机重建

| 阶段 | 事件 | `REAL_FORCE_FSG_NAV_BAR` 行为 |
|---|---|---|
| 初始化 | `BaseRecentsImpl.<init>` 调用 `addFsgGestureWindow()` | 字段尚未存在，`getAdditionalStaticField` 会返回 `null`，模块在 `getGlobalBoolean` after-callback 中设置为真实系统值 |
| 第一次读取 | `addFsgGestureWindow:24` 调用 `getGlobalBoolean("force_fsg_nav_bar")` | 模块写入真实值，返回 `true`；字段已发布 |
| 第二次读取 | `updateFsgWindowState:5` 再次调用 | 模块覆写为最新真实值，仍返回 `true`；字段保持最新 |
| 消费 | `createAndAddNavStubView` before-callback | 读取字段；若真实值为 `false`（即用户实际使用 3-button nav），`returnAndSkip(null)` |
| 消费 | `updateFsgWindowState` after-callback | 读取字段；若真实值为 `false`，移除已存在的 `mNavStubView`；若为 `true`，保留 |

### 6.1 发布顺序结论

`addFsgGestureWindow()` 在构造器内调用，早于任何可能的 `updateFsgWindowState()` / `createAndAddNavStubView()` 调用，因此 `REAL_FORCE_FSG_NAV_BAR` 在 `createAndAddNavStubView` 被调用前已经发布。**不存在未发布导致的读取异常**。`getAdditionalStaticField` 对不存在的字段返回 `null`，代码显式 `as? Boolean ?: false`，不会抛异常。

### 6.2 fail-open / fail-closed 评估

| 场景 | 结果 | 风险 |
|---|---|---|
| `getGlobalBoolean` hook 未触发（例如在类初始化前直接调用） | 字段保持未发布，读取时为 `false` | `createAndAddNavStubView` 会被跳过，`updateFsgWindowState` 会移除 nav stub；这与真实 `force_fsg_nav_bar = false` 等价，fail-closed |
| 堆栈无 `BaseRecentsImpl` | 不发布字段，不修改返回值 | 对非 `BaseRecentsImpl` 调用者保持原生值，正确 |
| 字段已发布但真实值为 `true` | `createAndAddNavStubView` 执行原始，`updateFsgWindowState` 保留 | 期望行为 |
| 字段已发布但真实值为 `false` | `createAndAddNavStubView` 跳过，`updateFsgWindowState` 移除 | 对应 3-button + 水平手势的 "both" 场景 |

## 7. 根因假设与证据

### H1：第三方桌面导致 `mIsUseMiuiHomeAsDefaultHome = false`，原生分支不创建 stub

- **支持证据**：
  - `addFsgGestureWindow()` 与 `updateFsgWindowState()` 均把 `mIsUseMiuiHomeAsDefaultHome` 作为创建 stub 的必要条件。
  - 模块没有 hook `Utilities.getDefaultHomePackageName()` 或 `mIsUseMiuiHomeAsDefaultHome`。
  - 当默认桌面为第三方包时，`getDefaultHomePackageName()` 不等于 `com.miui.home`。
- **反对证据 / 反例**：
  - 无：这是 DEX 直接逻辑，没有已知的替代代码路径。
- **置信度**：**高**（纯静态分析可得）。

### H2：`REAL_FORCE_FSG_NAV_BAR` 未初始化导致 `createAndAddNavStubView` 错误跳过

- **支持证据**：
  - 字段没有显式初始赋值。
- **反对证据**：
  - `addFsgGestureWindow()` 在构造器中调用，且先调用 `getGlobalBoolean`，先发布字段；`createAndAddNavStubView` 的调用在条件分支中，发生在发布之后。
  - 即使字段未发布，`getAdditionalStaticField` 返回 `null`，代码转 `false`，行为等价于真实值 false，不会抛异常。
- **置信度**：低。静态证据不支持这是主因。

### H3：FSG hook 未在 `com.mi.android.globallauncher` 安装

- **支持证据**：
  - `ProcessScope.kt:92` 把 `com.mi.android.globallauncher` 识别为 `LAUNCHER`。
  - `LauncherInstaller:74` 显式限制 FSG hook 仅在 `com.miui.home`。
- **反对证据 / 限制**：
  - 即使安装到 `com.mi.android.globallauncher`，它也不一定是 MIUI 默认桌面的实际包名；用户报告的第三方桌面通常不是小米系包。
  - 该 issue 的核心是默认桌面改变后 `com.miui.home` 进程内的 `BaseRecentsImpl` 行为，而不是 hook 没装进 `com.mi.android.globallauncher`。
- **置信度**：中低。这是一个 routing gap，但不是当前 issue 的主因。

### H4：模块与 `usingFsGesture()` / `DeviceConfig.sIsGestureEnable` 语义不匹配

- **支持证据**：
  - `usingFsGesture()` 返回 `sIsGestureEnable`；模块覆盖为 `true`。
- **反对证据**：
  - `BaseRecentsImpl` 创建 stub 不依赖 `usingFsGesture()` 的返回值，而是依赖 `force_fsg_nav_bar` 和 `mIsUseMiuiHomeAsDefaultHome`。
- **置信度**：低。该 override 对 stub 创建路径的影响是间接的。

## 8. 测试覆盖审计

### 8.1 已有覆盖

`tools/tests/test_launcher_gesture_state_cache.py` 对 `LauncherGestureHooks.kt` 做静态契约检查：

- `test_fs_gestures_callbacks_use_captured_class`：确认 `BaseRecentsImpl` 只解析一次，并在 `createAndAddNavStubView` / `updateFsgWindowState` / `getGlobalBoolean` 回调中使用已捕获的 `baseRecentsClass`。
- `test_real_force_fsg_nav_bar_unchanged`：确认 `REAL_FORCE_FSG_NAV_BAR` 字符串存在。
- `test_stack_trace_call_preserved` / `test_stack_class_name_condition_preserved`：确认栈回溯逻辑。
- `test_global_force_fsg_not_always_true`：确认 `getGlobalBoolean` hook 不是对所有调用者都返回 `true`。
- `test_base_recents_class_parsed_once`：确认 `BaseRecentsImpl` findClass 只出现一次。

### 8.2 缺失覆盖

| 缺失项 | 说明 |
|---|---|
| 第三方包默认桌面场景 | 没有测试模拟 `getDefaultHomePackageName() != "com.miui.home"` 时的 `mIsUseMiuiHomeAsDefaultHome` 分支 |
| `mIsUseMiuiHomeAsDefaultHome` override | 当前源码没有 override，因此无对应测试 |
| `com.mi.android.globallauncher` FSG 路由 | 现有测试仅确认 `ProcessScope` 把该包识别为 LAUNCHER，未验证 `LauncherInstaller` 中 `com.miui.home` 的 guard |
| `REAL_FORCE_FSG_NAV_BAR` 初始化/发布顺序 | 静态测试未构造 `addFsgGestureWindow` → `getGlobalBoolean` → `createAndAddNavStubView` 的时序 |
| `GestureStubView.onTouchEvent` 对 `controls_fsg_horiz_apps` 的命中 | 测试仅检查栈回溯和字符串存在，未构造 foreground package 与 set 的匹配/不匹配 |
| 默认桌面从第三方切换回小米桌面 | 没有生命周期切换测试 |

本批不加新测试，因为生产变更未授权。

## 9. 关键结论

1. **Install chain 完整**：`controls_fsg_horiz` 从 preference 到 `LauncherInstaller` 再到 `FSGesturesHook` 的安装路径没有断点。
2. **ROM 原生 default-home gate 是主因**：`BaseRecentsImpl.addFsgGestureWindow()` 和 `updateFsgWindowState()` 仅在 `mIsUseMiuiHomeAsDefaultHome == true` 时创建/显示手势 stub；模块未 override 该条件。
3. **`REAL_FORCE_FSG_NAV_BAR` 发布顺序正确**：构造器内 `addFsgGestureWindow()` 首先调用 `getGlobalBoolean`，字段在 `createAndAddNavStubView` 消费前已发布；未初始化不会抛异常，fail-closed。
4. **第三方 launcher 进程内不加载模块**：`com.miui.home` 之外的默认桌面包不在 `ProcessScopes.KNOWN_PACKAGES` 中，模块不会在其进程初始化；但 FSG 功能本应由 `com.miui.home` 提供，因此问题不在第三方包本身。
5. **Production 缺口**：若要在第三方默认桌面下启用水平手势，需要额外 hook `Utilities.getDefaultHomePackageName()` 或 `BaseRecentsImpl.mIsUseMiuiHomeAsDefaultHome` / `addFsgGestureWindow()` / `updateFsgWindowState()`，使 stub 创建不再受默认桌面限制。这属于生产变更，本批不做。

## 10. 推荐结论

- **本批不做 production 修改**。
- **Issue #2 的静态分类**：`UPSTREAM_LIFECYCLE_LIMITATION`（ROM 原生 `mIsUseMiuiHomeAsDefaultHome` gate 阻止 stub 创建）。
- **Device 验证**：在实机上验证 `controls_fsg_horiz` 在第三方默认桌面 vs 小米默认桌面下的行为差异前，不声称已修复。
- **下阶段授权门**：若用户/PM 批准，可设计最小 production 变更（override default-home 判定或 stub 创建分支），并补充对应的默认桌面切换测试与 `com.mi.android.globallauncher` 路由测试。

## 11. 证据文件清单

| 文件 | 路径 | 用途 |
|---|---|---|
| 精确 Launcher APK | `C:\Home\xiaomi\rom\A13\_evidence_cache\launcher\com.miui.home_RELEASE-4.39.14.8060-04191512.apk` | 静态 ABI 来源 |
| `BaseRecentsImpl.<init>` | `C:\Home\xiaomi\rom\A13\_evidence_cache\launcher\fsg_methods\com_miui_home_recents_BaseRecentsImpl__init_dad.java` | 构造器与 `addFsgGestureWindow` 调用点 |
| `BaseRecentsImpl.addFsgGestureWindow` | `C:\Home\xiaomi\rom\A13\_evidence_cache\launcher\fsg_methods\com_miui_home_recents_BaseRecentsImpl__addFsgGestureWindow_dad.java` | stub 创建条件 |
| `BaseRecentsImpl.updateFsgWindowState` | `C:\Home\xiaomi\rom\A13\_evidence_cache\launcher\fsg_methods\com_miui_home_recents_BaseRecentsImpl__updateFsgWindowState_dad.java` | default-home 分支 |
| `DeviceConfig.usingFsGesture` | `C:\Home\xiaomi\rom\A13\_evidence_cache\launcher\fsg_methods\com_miui_home_launcher_DeviceConfig__usingFsGesture_dad.java` | 模块 override 目标 |

## 12. 验证（Stage F）

运行命令（仅文档变更；未修改生产源码）：

| 命令 | 结果 |
|---|---|
| `git status --short` | 干净工作区（仅新增 Stage F 文档文件） |
| `git diff --check` | `OK` |
| `python tools/verify.py fast --changed` | `OK` |
| `python tools/verify.py full` | `OK`（compileDebugKotlin / compileDebugJavaWithJavac / testDebugUnitTest-all / lintDebug） |
| `python -m compileall tools` | `OK` |
| `python -m unittest discover -s tools/tests -p "test_*.py"` | `OK (1267 tests, skipped=2)` |

完整验证结果见 `A13_STAGE_F_ISSUE_2_FSG_EVIDENCE_REPORT.txt` 和 `A13_STAGE_F1_FSG_TARGET_SELECTION_REPORT.txt`。

---

## A. Stage F1 — Target selection analysis

> 本附件记录中断处继续后的额外静态分析：字段读写面、`getDefaultHomePackageName` 调用点、后 stub 方法审计、默认桌面切换生命周期、候选设计排序与未来测试。

### A.1 `mIsUseMiuiHomeAsDefaultHome` 读写面

- 读点：`access$1200`、`addFsgGestureWindow()`、`isUseLauncherRecentsAndFsGesture()`、`updateFsgWindowState()`、`BaseRecentsImpl$6.onExpand()`。
- 写点：`addFsgGestureWindow()`（直接赋值）、`setIsUseMiuiHomeAsDefaultHome()`。
- `access$1200` 为 synthetic accessor，在 `BaseRecentsImpl$6.onExpand()` 被读取；该字段因此也影响折叠屏展开时的 stub 分支。

### A.2 `updateUseLauncherRecentsAndFsGesture` 副作用

```java
private void updateUseLauncherRecentsAndFsGesture() {
    DeviceConfig.setUseLauncherRecentsAndFsGesture(
        this.mContext,
        this.isUseLauncherRecentsAndFsGesture());  // 直接返回 mIsUseMiuiHomeAsDefaultHome
}
```

`DeviceConfig.setUseLauncherRecentsAndFsGesture` 会写入 Settings.Global：

```java
MiuiSettingsUtils.putBooleanToGlobal(
    contentResolver,
    "use_gesture_version_three",
    value);
```

因此任何对 `mIsUseMiuiHomeAsDefaultHome` 的 override 都会同步影响全局 `use_gesture_version_three`。

### A.3 `Utilities.getDefaultHomePackageName` 调用点

| # | 类 | 方法 |
|---|---|---|
| 1 | `FallbackHomeCompat` | `getDesiredHomePackage()` |
| 2 | `Utilities` | `isUseMiuiHomeAsDefaultHome(Context)` |
| 3 | `Utilities` | `isUsePocoHomeAsDefaultHome(Context)` |
| 4 | `ElderlyManModeChangedReceiver` | `onReceive$___twin___` |
| 5 | `BaseRecentsImpl$7` | `onChange(boolean)` |
| 6 | `BaseRecentsImpl` | `addFsgGestureWindow()` |
| 7 | `NavBarTypeContainerPreference` | 设置默认桌面对话框 lambda |
| 8 | `MiuiHomeSettings` | `onResume()` |

唯一进入 FSG 创建路径的调用是 `BaseRecentsImpl.addFsgGestureWindow()`；`BaseRecentsImpl$7.onChange` 通过 `setIsUseMiuiHomeAsDefaultHome` 影响状态。其余调用点属于设置 UI、兼容、老年模式等，不宜被全局 override 影响。

### A.4 默认桌面切换生命周期

- `BaseRecentsImpl$7` 是 `ContentObserver`，在 `registerSuperSavePowerObserver()` 中创建并注册给 `Settings.System "power_supersave_mode_open"`。
- `BaseRecentsImpl$7.onChange` 调用 `setIsUseMiuiHomeAsDefaultHome(...)` 并触发 `OverviewComponentObserver.updateOverviewTargetsPost()`。
- `OverviewComponentObserver` 注册 `ACTION_PREFERRED_ACTIVITY_CHANGED`；其构造器和 `TouchInteractionService.initWhenUserUnlocked()` 都会主动调用 `updateOverviewTargets()`。
- `OverviewComponentObserver.updateOverviewTargets()` 通过 `PackageManager.resolveActivity(CATEGORY_HOME, 786432)` 解析默认桌面，然后调用 `BaseRecentsImpl.setIsUseMiuiHomeAsDefaultHome(mIsHomeAndOverviewSame)`。

### A.5 后 stub 方法审计

`addBackStubWindow()`、`showBackStubWindow()`、`clearBackStubWindow()`、`removeNavStubView()`、`createAndAddNavStubView()` 自身不检查默认桌面，只执行窗口操作（通常提交到 `BACKGROUND_EXECUTOR` 或 `GESTURE_EXECUTOR`）。是否创建/移除由 `addFsgGestureWindow()` 和 `updateFsgWindowState()` 中的 `mIsUseMiuiHomeAsDefaultHome` 分支决定。

### A.6 候选设计排序

| ID | 设计 | 风险 | 推荐 |
|---|---|---|---|
| A | Hook `Utilities.getDefaultHomePackageName` 在 `BaseRecentsImpl` 上下文返回 `com.miui.home` | 高：影响 settings UI / 兼容 / Poco / 老年模式 | 否 |
| B | Hook `BaseRecentsImpl.addFsgGestureWindow` + `setIsUseMiuiHomeAsDefaultHome` 在 `controls_fsg_horiz` 且真实 FSG 为 true 时强制 `true` | 中：局部，不影响其他 `Utilities` 调用者，保留 `REAL_FORCE_FSG_NAV_BAR` fail-closed | **是** |
| C | 直接 Hook `addFsgGestureWindow` / `updateFsgWindowState` 分支逻辑，跳过 `mIsUseMiuiHomeAsDefaultHome` 检查 | 中高：`use_gesture_version_three` 仍可能为 false，字段状态仍不一致 | 否 |
| D | Hook `OverviewComponentObserver.updateOverviewTargets` 强制 `mIsHomeAndOverviewSame=true` | 高：改变 overview/home intent 构造 | 否 |

**推荐设计 B** 的核心思路：
1. `addFsgGestureWindow` after-hook 在原有逻辑完成后，若 `controls_fsg_horiz` 与真实 `force_fsg_nav_bar` 均为 true，则设置 `mIsUseMiuiHomeAsDefaultHome = true`，重新调用 `updateUseLauncherRecentsAndFsGesture()` 与 `updateFsgWindowState()`。
2. `setIsUseMiuiHomeAsDefaultHome` before-hook 在参数为 `false` 且满足同样条件时，覆盖为 `true`，从而阻止 `BaseRecentsImpl$7` 或 `OverviewComponentObserver` 把字段改回 `false` 并移除 stub。

### A.7 未来最小测试集合

- F1-T1：小米默认桌面 + FSG 开启，stub 正常创建，`use_gesture_version_three` 为 true。
- F1-T2：第三方默认桌面 + FSG 开启，设计 B 强制 `mIsUseMiuiHomeAsDefaultHome` 为 true，stub 创建。
- F1-T3：小米桌面与第三方桌面来回切换，`setIsUseMiuiHomeAsDefaultHome(false)` 被覆盖为 `true`。
- F1-T4：真实 `force_fsg_nav_bar == false`（三键导航）时不 override，stub 被移除/跳过。
- F1-T5：`controls_fsg_horiz_apps` set 命中/未命中时 `GestureStubView.onTouchEvent` 返回值。
- F1-T6：无默认桌面/空包时 hook 不 NPE 且按 `controls_fsg_horiz` 条件行为。
- F1-T7：`addFsgGestureWindow` / `updateFsgWindowState` / `setIsUseMiuiHomeAsDefaultHome` 回调无反射扫描、无重复栈遍历。
- F1-T8：override `mIsUseMiuiHomeAsDefaultHome` 为 true 时，确认 `use_gesture_version_three` 写入 true。

### A.8 F1 结论

- `STAGE_F1` 静态分析完成；生产变更未授权，未做任何 production 修改。
- Issue #2 在设备层面仍为 `UNVERIFIED`。
- 唯一推荐候选为设计 B；若用户/PM 批准，需先补充测试并在实机验证，方可进入 production 实现。
- 详细报告见 `A13_STAGE_F1_FSG_TARGET_SELECTION_REPORT.txt`。
