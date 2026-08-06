# A13 FSG BaseRecents Call-Scope Audit

## 审计元数据

| 字段 | 值 |
|------|-----|
| Base SHA | `6ea181a261082ad5ec8bf38a6e03737cfed116b0` |
| 审计日期 | 2026-08-06 |
| 审计任务 | `AUDIT-A13-FSG-BASE-RECENTS-CALL-SCOPE` |
| 最终状态 | `EVIDENCE_BLOCKED_NO_ROM_INPUT` |
| 审计员 | Devin (autonomous) |

## 当前源码合同

`LauncherGestureHooks.kt` 中相关代码（`FSGesturesHook`）：

```kotlin
val baseRecentsClass = XposedHelpers.findClass(
    "com.miui.home.recents.BaseRecentsImpl",
    lpparam.classLoader
)

// ... createAndAddNavStubView / updateFsgWindowState callbacks ...

ModuleHelper.findAndHookMethodSilently(
    "com.miui.launcher.utils.MiuiSettingsUtils",
    lpparam.classLoader,
    "getGlobalBoolean",
    android.content.ContentResolver::class.java,
    String::class.java,
    object : MethodHook() {
        override fun after(param: AfterHookCallback) {
            if (param.getArg(1) != "force_fsg_nav_bar") return

            for (el in Thread.currentThread().stackTrace) {
                if (el.className == "com.miui.home.recents.BaseRecentsImpl") {
                    XposedHelpers.setAdditionalStaticField(
                        baseRecentsClass,
                        "REAL_FORCE_FSG_NAV_BAR",
                        param.getResult()
                    )
                    param.setResult(true)
                    return
                }
            }
        }
    }
)
```

### 当前 Hook 符号

| 符号 | 位置 | 说明 |
|------|------|------|
| `com.miui.home.recents.BaseRecentsImpl` | `LauncherGestureHooks.kt:253` | 安装阶段 `XposedHelpers.findClass` 解析一次 |
| `createAndAddNavStubView` | `LauncherGestureHooks.kt:259` | 读取 `REAL_FORCE_FSG_NAV_BAR` |
| `updateFsgWindowState` | `LauncherGestureHooks.kt:266` | 读取 `REAL_FORCE_FSG_NAV_BAR` |
| `getGlobalBoolean` | `LauncherGestureHooks.kt:280` | Hook `MiuiSettingsUtils.getGlobalBoolean` |
| `force_fsg_nav_bar` | `LauncherGestureHooks.kt:282` | 参数 key gate |
| `REAL_FORCE_FSG_NAV_BAR` | `LauncherGestureHooks.kt:286, 261, 268` | 用作跨 callback 标志 |
| `Thread.currentThread().stackTrace` | `LauncherGestureHooks.kt:284` | 调用范围判定 |
| `com.miui.home.recents.BaseRecentsImpl` exact class name | `LauncherGestureHooks.kt:285` | 堆栈帧判定条件 |

仓库中不存在第二个相同逻辑实现。`app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:795-796` 也使用了 `Thread.currentThread().stackTrace`，但与 `force_fsg_nav_bar` 无关，不在本审计范围。

## 工具检查

```powershell
Get-Command jadx,jadx-cli,apktool,aapt2,apkanalyzer,java,python -ErrorAction SilentlyContinue
```

结果：

| 工具 | 可用 | 路径 |
|------|------|------|
| `java` | 是 | `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe` |
| `python` | 是 | `C:\Python314\python.exe` |
| `aapt2` | 否（在 PATH） | 本地副本：`C:\Users\tv\Downloads\Peengeek\.tools\android-sdk\build-tools\{34,35,36,37}.0.0\aapt2.exe` |
| `apkanalyzer` | 否（在 PATH） | 本地副本：`C:\Users\tv\Downloads\Peengeek\.tools\android-sdk\cmdline-tools\latest\bin\apkanalyzer.bat` |
| `jadx` / `jadx-cli` | 否 | 未找到 |
| `apktool` | 否 | 未找到 |

结论：即使获得 ROM launcher APK，也缺少 JADX / apktool / baksmali 进行可靠的 Java/DEX 级反编译。仅有 `aapt2`/`apkanalyzer` 可读取 manifest，无法分析方法体调用链。

## ROM 样本查找

### 仓库内

```powershell
git grep -n -i -E "miui.?14|hyperos.?1|miuihome|com\.miui\.home|launcher apk|rom input|rom intelligence" -- docs tools tasks .
```

结果仅包含文档/测试字符串，没有实际 APK 索引或路径。

### 指定父目录

```powershell
$repoRoot = (git rev-parse --show-toplevel).Trim()
$parent = Split-Path $repoRoot -Parent
Get-ChildItem $repoRoot,$parent -Recurse -File |
    Where-Object { $_.Name -match '^(MiuiHome|com\.miui\.home|Home).*\.apk$' } |
    Select-Object FullName,Length,LastWriteTime
```

结果：无匹配。

### 已知工作目录

```powershell
$knownRoot = "C:\Users\tv\Downloads\Peengeek"
Get-ChildItem $knownRoot -Recurse -File |
    Where-Object { $_.Name -match '^(MiuiHome|com\.miui\.home|Home).*\.apk$' } |
    Select-Object FullName,Length,LastWriteTime
```

结果：无匹配。

### 用户主目录 APK 扫描

扫描 `C:\Users\tv` 下的所有 `.apk` 文件（仅列出非 CustoMIUIzer 模块）：

- `C:\Users\tv\Documents\AyuGramDownloads\app-release.apk`
- `C:\Users\tv\Documents\AyuGramDownloads\base.apk`
- `C:\Users\tv\Documents\AyuGramDownloads\FkWeChat_1.2.6_sign.apk`
- 多个 `CustoMIUIzer-A13/A14` 模块 APK
- 部分测试/产物 APK

没有任何文件名匹配 `MiuiHome`、`com.miui.home`、`Home` 或 ROM launcher 包名，也没有 `globalauncher` 包 APK。

### 按名称扩展扫描

```powershell
Get-ChildItem "C:\Users\tv" -Recurse -File |
    Where-Object { $_.Name -match '(miui|hyperos).*home|globallauncher|launcher.*apk' }
```

结果：无匹配。

### 预解码目录

```powershell
Get-ChildItem "C:\Users\tv\Downloads\Peengeek" -Recurse -Directory |
    Where-Object { $_.Name -match 'MiuiHome|com\.miui\.home|Home.*jadx|Home.*smali|BaseRecents' }
```

结果：无匹配。

## ROM 样本目录

| ROM 目标 | APK 文件 | SHA-256 | versionName | versionCode | minSdk | targetSdk | 状态 |
|----------|----------|---------|-------------|-------------|--------|-----------|------|
| MIUI 14 / Android 13 | 未找到 | — | — | — | — | — | `MISSING` |
| HyperOS 1 / Android 13 | 未找到 | — | — | — | — | — | `MISSING` |

## 调用范围矩阵

由于缺少 ROM 输入，无法构建可验证矩阵。以下为根据源码假设的占位：

| 字段 | MIUI 14 | HyperOS 1 |
|------|---------|-----------|
| ROM | 未知 | 未知 |
| Launcher version | 未知 | 未知 |
| APK SHA-256 | 无 | 无 |
| `force_fsg_nav_bar` call-site count | 未知 | 未知 |
| Direct caller class | 未知 | 未知 |
| Direct caller method | 未知 | 未知 |
| Exact `BaseRecentsImpl` frame present | 无法验证 | 无法验证 |
| Subclass/helper frame present | 无法验证 | 无法验证 |
| Async boundary present | 无法验证 | 无法验证 |
| Reflection present | 无法验证 | 无法验证 |
| Estimated frequency | `UNKNOWN` | `UNKNOWN` |
| Candidate hook boundary | 无 | 无 |
| Confidence | 无 | 无 |

## 问题回答

### 问题 1：调用范围能否由有限、稳定、可 Hook 的方法边界表示？

**无法回答。**

缺少两个目标 ROM 的 launcher APK，无法验证：

- `MiuiSettingsUtils.getGlobalBoolean` 中 `"force_fsg_nav_bar"` 的实际调用点；
- 调用点是否全部位于 `BaseRecentsImpl` 本类，或其 subclass / 内部类 / lambda / helper；
- 是否经过 Handler、Executor、Coroutine 等异步边界；
- 当前 exact-class 条件 `com.miui.home.recents.BaseRecentsImpl` 在两 ROM 是否成立。

因此不能判定是否可用精确方法边界替代 `Thread.currentThread().stackTrace`。

### 问题 2：删除 `Thread.currentThread().stackTrace` 是否具有实际性能价值？

**无法回答。**

`Estimated frequency` 为 `UNKNOWN`。仅在以下证据支持下才可评估：

- 调用点位于 `GESTURE_START`、`TOUCH_EVENT` 或 `FRAME_OR_LAYOUT` 路径；
- 每事件均触发 `getGlobalBoolean("force_fsg_nav_bar")`。

当前没有任何 ROM 样本可用于测量或推断调用频率。

## 替代方案评估

由于缺少证据，不评估任何替代方案，也不建议进入实现任务。

### 禁止建议

即使证据不足，以下方案也不能无证据推荐：

- 全局对 `force_fsg_nav_bar` 返回 `true`；
- 仅保护 `createAndAddNavStubView` 或 `updateFsgWindowState` 而无调用点证据；
- 使用 `Throwable().stackTrace`、`StackWalker` 或 `VMStack`；
- 使用不支持嵌套的 `ThreadLocal Boolean`；
- 永久静态缓存系统设置结果。

## 风险

1. **无 ROM 输入**：无法验证当前 exact-class 条件是否在两个目标 ROM 上均成立。
2. **无反编译工具**：即使获得 APK，也缺少 JADX/apktool，无法完成 Java/DEX 级分析。
3. **当前实现风险**：`Thread.currentThread().stackTrace` 在每次 `getGlobalBoolean("force_fsg_nav_bar")` 时分配 `StackTraceElement[]`，若调用频率高则存在分配与性能成本；若 ROM 中调用点不经过 exact `BaseRecentsImpl` 帧，则当前逻辑存在 false negative 风险（`EXISTING_SCOPE_COMPATIBILITY_RISK` 无法在本次审计中证实或否认）。

## 后续建议

- 提供两个目标 ROM 的 launcher APK（`com.miui.home` 或 `com.mi.android.globallauncher`），MIUI 14 与 HyperOS 1 各一。
- 提供 JADX CLI 或 apktool + baksmali 环境。
- 在获得输入后重启审计，方可建议 `PERF-A13-FSG-CALL-SCOPE-GUARD` 或明确停止优化方向。

## 最终决策状态

```text
EVIDENCE_BLOCKED_NO_ROM_INPUT
```

由于同时缺少 JADX/apktool，工具侧 blocker 也可记录为 `EVIDENCE_BLOCKED_NO_DECOMPILER`。

## 输出清单

- `docs/audit/A13_FSG_BASE_RECENTS_CALL_SCOPE.md`：已创建。
- 未新增任何 APK、DEX、smali 或反编译源码目录。
- 未修改任何生产源码、Hook 文件或 contract。
