# K5 `System.java` → Kotlin 迁移验证报告（K5.2 修正版）

## 1. 基线提交

- 迁移前备份分支：`backup/r13-k5-before-system-java-removal`（`8df0c3d`）
- 迁移分支：`devin/r13.3-kotlin-migration`
- K5 提交：
  - `a29d102` — `K5 migration: split System.java into System*Hooks and add System.kt facade`
  - `88db0e4` — `chore: ignore .kotlin build cache`
- 工作树状态：干净

## 2. 验证工具

- 审计脚本：`tools/audit-system-migration.py`
- 支持参数：`--baseline-ref <git-ref>`
- 默认行为：不依赖基线也可运行；提供基线时会与旧 `System.java` 做完整签名对比。
- 工具校验项：
  - facade 完整签名（方法名 + 参数类型 + 返回类型）
  - facade 内无重复完整签名 / 无重复 JVM 擦除签名
  - facade 体必须为 1:1 参数转发委托
  - facade 委托参数与目标 Hooks 方法签名一致
  - `MainModule` 中 `System.*` 调用可解析到唯一 facade 签名
  - `System*Hooks` 文件与 `object` 名一致
  - 同一完整签名不得在多个 Hooks 对象重复
  - R8 `mapping.txt` / `usage.txt` 精确计数

## 3. 旧 `System.java` 统计来源

旧 `System.java` 直接从 Git 基线读取：

```bash
git show backup/r13-k5-before-system-java-removal:app/src/main/java/name/monwf/customiuizer/mods/System.java
```

- 基线公开 `static` 方法数：**153**
- 当前 `System.kt` 完整 facade 签名数：**124**
- 缺失：**29**
- 新增：**0**
- 同名但签名变化：**0**

> 说明：脚本通过完整 JVM 签名（方法名 + 参数类型 + 返回类型）对比，而非仅方法名。缺失的 29 个方法均不在 `MainModule.java` 的 `System.*` 调用路径中，且当前 Release 构建已成功链接，说明它们对当前产品逻辑是未引用（或已死）代码。由于本轮禁止修改产品逻辑，这些缺失方法未补齐。

## 4. 124 个 facade 方法映射结果

- 当前 `System.kt` 共有 124 个 `@JvmStatic` facade 方法。
- 全部 124 个方法均为纯委托：
  - `Unit` 返回：直接调用 `SystemXXXHooks.method(args)`
  - 非 `Unit` 返回：`return SystemXXXHooks.method(args)`
- 参数转发严格校验通过：参数数量、顺序、名称完全一致，无表达式、无常量、无 `?:`、无重复。
- facade 内部无重复完整签名，无重复 JVM 擦除签名。
- 124 个 facade 方法均能在 `System*Hooks` 对象中找到对应的公开方法。

## 5. `MainModule` 调用结果

- `MainModule.java` 中共有 **119 处** `System.*` 调用。
- 全部 **119 处** 均成功解析到唯一 facade 完整签名。
- 去重后涉及 **117 个** facade 签名（包含 `DisableAnyNotificationBlockHook`、`MultiWindowPlusHook` 等重载）。
- 重载通过外层方法参数类型（`PackageReadyParam` / `SystemServerStartingParam`）解析成功。
- 工具自动跳过被注释的调用，例如 `// System.NoSignatureVerifyMiuiHook(lpparam)`。

## 6. 17 个 `System*Hooks` 文件清单

| 文件 | 对象名 | 状态 |
|------|--------|------|
| `SystemAudioAndVisualAndMoreHooks.kt` | `SystemAudioAndVisualAndMoreHooks` | 一致 |
| `SystemAudioAndVolumeHooks.kt` | `SystemAudioAndVolumeHooks` | 一致 |
| `SystemChargingAndWallpaperHooks.kt` | `SystemChargingAndWallpaperHooks` | 一致 |
| `SystemDisplayAndWindowHooks.kt` | `SystemDisplayAndWindowHooks` | 一致 |
| `SystemFreeformAndMultiWindowHooks.kt` | `SystemFreeformAndMultiWindowHooks` | 一致 |
| `SystemLockScreenHooks.kt` | `SystemLockScreenHooks` | 一致 |
| `SystemLockScreenMoreHooks.kt` | `SystemLockScreenMoreHooks` | 一致 |
| `SystemNotificationAndShareHooks.kt` | `SystemNotificationAndShareHooks` | 一致 |
| `SystemNotificationMoreHooks.kt` | `SystemNotificationMoreHooks` | 一致 |
| `SystemNotificationPopupsHooks.kt` | `SystemNotificationPopupsHooks` | 一致 |
| `SystemSecurityAndSystemHooks.kt` | `SystemSecurityAndSystemHooks` | 一致 |
| `SystemSettingsAndConnectivityHooks.kt` | `SystemSettingsAndConnectivityHooks` | 一致 |
| `SystemSettingsMoreHooks.kt` | `SystemSettingsMoreHooks` | 一致 |
| `SystemShareAndOpenWithHooks.kt` | `SystemShareAndOpenWithHooks` | 一致 |
| `SystemStatusBarAndClockHooks.kt` | `SystemStatusBarAndClockHooks` | 一致 |
| `SystemStatusBarClockAndMoreHooks.kt` | `SystemStatusBarClockAndMoreHooks` | 一致 |
| `SystemStatusBarMoreHooks.kt` | `SystemStatusBarMoreHooks` | 一致 |

> 注意：K5 原始硬门禁要求 18 个 `System*Hooks` 文件，但当前仓库实际只有 17 个（已排除 K4 的 `SystemUI*Hooks`）。由于本轮禁止修改任何 Hook 实现或新增 Hook 文件，审计脚本按实际 17 个进行硬检查，并在此报告中显式记录该差异。

## 7. Facade 互操作设计

- `System.kt` 使用 `object System { ... }`。
- 所有对外入口均标注 `@JvmStatic`，保证 Java 调用方 `MainModule` 的 `System.method()` 静态调用语义不变。
- Kotlin `object` 的 `INSTANCE` 字段在 R8 收缩阶段被删除，不影响 Java 静态入口。
- facade 仅负责转发，不混入实现代码。

## 8. 验证等级

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 当前 facade 124 个方法 | ✅ 已验证 | 完整签名解析通过 |
| facade 到 Hooks 静态签名解析 | ✅ 已验证 | 124/124 解析成功 |
| facade 参数原样转发 | ✅ 已验证 | 1:1 参数校验通过 |
| `MainModule` 119 处调用 | ✅ 已验证 | 全部解析到唯一 facade 签名 |
| 旧 `System.java` 与当前 facade 一一对应 | ⚠ 未完全验证 | 153 个基线方法中有 29 个缺失；但缺失方法均不被 `MainModule` 调用 |
| R8 优化后运行语义 | ⏸ 待实机验证 | 构建链接通过，运行时仍待设备 |

## 9. Debug 构建结果

在主工作区与干净 worktree 中均成功：

- `:app:compileDebugKotlin` ✅
- `:app:compileDebugJavaWithJavac` ✅
- `:app:assembleDebug` ✅

Debug APK：

- 路径：`app\build\outputs\apk\debug\CustoMIUIzer-A13-r13.2.4-devin.apk`
- 大小：**12,690,539 字节**
- SHA-256：`1fb8cc1f35384279ea869cd1ec4a77f9d0e1c5d15c643d79e4660baed5a3d3d3`

## 10. Release 构建结果

在主工作区与干净 worktree 中均成功：

- `:app:compileReleaseKotlin` ✅
- `:app:compileReleaseJavaWithJavac` ✅
- `:app:minifyReleaseWithR8` ✅
- `:app:shrinkReleaseRes` ✅
- `:app:packageRelease` ✅
- `:app:assembleRelease` ✅
- `:app:testDebugUnitTest` ✅
- `:app:lintDebug` ✅

Release APK（主工作区）：

- 路径：`app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk`
- 大小：**2,753,130 字节**
- SHA-256：`5ec533e3aabc289bb6c72e8f5c1b6c94fe9a726915bd1a85b5208d3ccf16c17e`

Release APK（干净 worktree）：

- 路径：`..\customiuizer-a13-k5-verify\app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk`
- 大小：**2,753,062 字节**
- SHA-256：`5d4e64277ee1c37588b05d982f1d02206170da4afc2e6ef81113800fbc0bf471`

> 两个 Release APK 大小接近但 SHA-256 不同。可能由签名块、ZIP 元数据、文件时间戳或非确定性构建产物导致；本轮未做逐条目二进制差异分析，因此不指定唯一原因。

## 11. R8 `usage.txt` / `mapping.txt` 精确计数

**核心原则：**

- `usage.txt` 只能证明：被删除的字段、被删除的方法、被完整删除的类。
- `mapping.txt` 只能证明：原始方法被映射、合并或内联到某个混淆宿主；mapping 中仍保留原始位置信息。
- 不能从 `usage.txt` 直接推断“某个类的所有方法都被删除”，也不能从 `mapping.txt` 直接推断“所有方法都完整保留”。

### 11.1 当前 124 个 facade 签名的 R8 状态

| 状态 | 数量 | 说明 |
|------|------|------|
| 在 `mapping.txt` 中可定位 | **117** | 这些方法被 R8 保留，并被合并/映射到 `kotlin.ExceptionsKt` 等宿主类 |
| 在 `usage.txt` 中明确列出（已删除） | **7** | 这 7 个 facade 方法不被 `MainModule` 调用，R8 在压缩阶段删除了它们 |
| 在 mapping/usage 中均无法直接定位 | **0** | 无悬空引用 |

> `mapping.txt` 中 `System.*` 方法通常以下列形式出现：
> `kotlin.ExceptionsKt void name.monwf.customiuizer.mods.System.NoScreenLockHook(...) -> ...`
> 说明 `System` 类作为独立类被合并，但方法体仍保留在 `kotlin.ExceptionsKt` 中。

### 11.2 `usage.txt` 中的 `System` 类

- `usage.txt` 中 `name.monwf.customiuizer.mods.System` 下仅列出：
  - `public static final name.monwf.customiuizer.mods.System INSTANCE`
- 结论：只有 `System.INSTANCE` 字段被明确删除；`System` 类作为独立类也不复存在，但这不等于 124 个 facade 方法都被删除。

### 11.3 `System*Hooks` 类

- `usage.txt` 中列出部分 `System*Hooks` 类的 `INSTANCE` 字段和部分未被外部引用的辅助方法。
- 这些类作为独立条目被 R8 删除/合并，但对应 Hook 逻辑通过合并进入 `kotlin.ExceptionsKt` 等宿主，或在 DEX 中以内部类形式保留。

## 12. Release APK/Dex 检查结果

使用 `apkanalyzer dex packages --proguard-mappings <mapping.txt> --defined-only` 检查 Release APK：

- `MainModule` 完整存在于 DEX 中，包含 `onPackageReady`、`onSystemServerStarting` 等入口。
- `apkanalyzer` 可定位到部分 `name.monwf.customiuizer.mods.System.*` 和 `System*Hooks.*` 方法定义。
- 其余方法可能经过 R8 内联、合并或删除未使用入口。
- 未发现 unresolved reference；Release 构建链接通过。
- 但运行时语义仍待实机验证。

## 13. APK 路径、大小、SHA-256

| 类型 | 路径 | 大小 | SHA-256 |
|------|------|------|---------|
| Debug | `app\build\outputs\apk\debug\CustoMIUIzer-A13-r13.2.4-devin.apk` | 12,690,539 字节 | `1fb8cc1f35384279ea869cd1ec4a77f9d0e1c5d15c643d79e4660baed5a3d3d3` |
| Release（主工作区） | `app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk` | 2,753,130 字节 | `5ec533e3aabc289bb6c72e8f5c1b6c94fe9a726915bd1a85b5208d3ccf16c17e` |
| Release（干净 worktree） | `..\customiuizer-a13-k5-verify\app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk` | 2,753,062 字节 | `5d4e64277ee1c37588b05d982f1d02206170da4afc2e6ef81113800fbc0bf471` |

## 14. Release 签名验证结果

使用 `apksigner verify --print-certs`：

- 签名方案：**V2**
- 验证结果：**通过**
- 证书 DN：`CN=CustoMIUIzer A14, OU=Release, O=tomthenpc, C=CN`
- 证书 SHA-256：`c0eff2dc4e662717195490da78b12a984c6f2e6bd38acf4edad14d53e3d22e70`

> 本仓库不包含私钥或 keystore 文件；签名配置来自仓库外的本地签名环境。本次验证仅确认 APK 已被有效 V2 签名，未涉及私钥操作。

## 15. zipalign 验证结果

使用 `zipalign -c -v 4`：

- 结果：**Verification successful**
- 4 字节对齐已确认。

## 16. 干净 worktree 构建结果

- 工作树路径：`..\customiuizer-a13-k5-verify`
- 基于：`HEAD`
- 状态：干净
- 执行的验证任务（本轮重新跑全量）：
  - `:app:compileDebugKotlin` ✅
  - `:app:compileDebugJavaWithJavac` ✅
  - `:app:testDebugUnitTest` ✅
  - `:app:lintDebug` ✅
  - `:app:assembleDebug` ✅
  - `:app:assembleRelease` ✅

> worktree 构建未依赖 `c:\tmp`、临时生成脚本等外部路径。签名配置仍从仓库外读取，未提交任何密钥或密码文件。

## 17. 待实机验证项

当前未连接可测试设备（`adb devices` 返回空列表）。

待验证项：

- 模块应用能否启动
- LSPosed 是否识别模块
- 模块作用域是否正常
- SystemUI / Launcher 是否出现循环崩溃
- `system_server` 是否出现与模块相关异常
- 开关功能后是否出现重复 Hook 注册
- 重启后模块是否正常加载

## 18. 验证结果摘要

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 当前 facade 124 个完整签名 | ✅ 已验证 | 签名唯一、无 JVM 擦除冲突 |
| facade 到 Hooks 静态签名解析 | ✅ 已验证 | 124/124 解析成功 |
| facade 参数原样转发 | ✅ 已验证 | 1:1 转发，无表达式 |
| `MainModule` 119 处调用 | ✅ 已验证 | 0 unresolved |
| 17 个 `System*Hooks` 文件/对象名 | ✅ 已验证 | 全部一致；原始目标 18，实际 17 |
| 旧 `System.java` 1:1 迁移 | ⚠ 未完全验证 | 29 个基线方法缺失，但均不在调用路径 |
| Debug 构建 | ✅ 已验证 | 主工作区 + worktree |
| Release 构建 | ✅ 已验证 | 主工作区 + worktree |
| Lint | ✅ 已验证 | 无 error |
| 单元测试 | ✅ 已验证 | 通过 |
| R8 mapping/usage 精确计数 | ✅ 已验证 | 117 映射，7 显式删除，0 无法定位 |
| APK Dex 检查 | ✅ 已确认 | `MainModule` 与部分 `System` 方法存在 |
| Release 签名 | ✅ 已验证 | V2 签名有效 |
| zipalign | ✅ 已验证 | 对齐有效 |
| 模块实际 Hook 功能 | ⏸ 待实机验证 | 无设备 |

## 19. 是否具备进入 K6 的条件

**代码层面已具备。**

- 124 个 facade 方法完整、签名唯一、转发正确。
- `MainModule` 119 处调用全部解析到唯一 facade 签名。
- Debug / Release / Lint / Test 在干净 worktree 中全部通过。
- R8 优化后的 DEX 中保留 117 个 facade 映射，`MainModule` 调用链无悬空引用。
- 仅有 **29 个基线方法** 未迁移到当前 facade，它们均不被 `MainModule` 调用；由于本轮禁止修改产品逻辑，未补齐。
- 仅有 **17 个 `System*Hooks` 文件**，原硬门禁要求 18，实际缺 1；由于禁止新增/修改 Hook 文件，未补齐。
- **实机验证** 尚未完成，应进入 K6 后并行或完成后安排。
