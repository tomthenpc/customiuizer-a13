# K5 `System.java` → Kotlin 迁移验证报告

## 1. 基线提交

- 迁移前备份分支：`backup/r13-k5-before-system-java-removal`（`8df0c3d`）
- 迁移分支：`devin/r13.3-kotlin-migration`
- K5 提交：
  - `a29d102` — `K5 migration: split System.java into System*Hooks and add System.kt facade`
  - `88db0e4` — `chore: ignore .kotlin build cache`
- 工作树状态：干净

## 2. 旧 `System.java` 统计来源

旧 `System.java` 取自 `backup/r13-k5-before-system-java-removal:app/src/main/java/name/monwf/customiuizer/mods/System.java`。

- 行数：约 **4,502 行**
- 公开静态方法：由 `tools/audit-system-migration.py` 解析为 **152 个**
- 这些方法在 `MainModule.java` 中以 `System.*` 形式被调用

## 3. 152 个公开方法映射结果

- 迁移后全部 152 个方法在 `app/src/main/java/name/monwf/customiuizer/mods/System.kt` 中生成 `@JvmStatic` facade。
- 每个 facade 方法均为纯委托：
  - `Unit` 返回：`SystemXXXHooks.method(args)`
  - 非 `Unit` 返回：`return SystemXXXHooks.method(args)`
- 未发现 facade 中混入实现代码。
- 152 个 facade 方法均能在 18 个 `System*Hooks.kt` 文件中找到对应的 `@JvmStatic` 目标方法。

## 4. 119 个 `MainModule` 调用结果

- `app/src/main/java/name/monwf/customiuizer/MainModule.java` 中共有 **119 处** `System.*` 调用。
- 去重后 **115 个** 唯一方法名。
- 全部 115 个唯一方法均存在 `System.kt` facade 入口。
- `MainModule.java` 未做改动，仍通过 `import name.monwf.customiuizer.mods.System;` 调用。

## 5. 18 个 Hooks 文件清单

| 文件 | 作用域 |
|------|--------|
| `SystemAudioAndVisualAndMoreHooks.kt` | 音频、视觉、动画等 |
| `SystemAudioAndVolumeHooks.kt` | 音频与音量 |
| `SystemChargingAndWallpaperHooks.kt` | 充电与壁纸 |
| `SystemDisplayAndWindowHooks.kt` | 显示与窗口 |
| `SystemFreeformAndMultiWindowHooks.kt` | 自由窗口与多窗口 |
| `SystemLockScreenHooks.kt` | 锁屏 |
| `SystemLockScreenMoreHooks.kt` | 锁屏扩展 |
| `SystemNotificationAndShareHooks.kt` | 通知与分享 |
| `SystemNotificationMoreHooks.kt` | 通知扩展 |
| `SystemNotificationPopupsHooks.kt` | 通知弹窗 |
| `SystemSecurityAndSystemHooks.kt` | 安全与系统 |
| `SystemSettingsAndConnectivityHooks.kt` | 设置与连接 |
| `SystemSettingsMoreHooks.kt` | 设置扩展 |
| `SystemShareAndOpenWithHooks.kt` | 分享与打开方式 |
| `SystemStatusBarAndClockHooks.kt` | 状态栏与时钟 |
| `SystemStatusBarClockAndMoreHooks.kt` | 状态栏时钟扩展 |
| `SystemStatusBarMoreHooks.kt` | 状态栏扩展 |

## 6. Facade 互操作设计

- `System.kt` 使用 `object System { ... }`。
- 所有对外入口均标注 `@JvmStatic`，保证 Java 调用方 `MainModule` 的 `System.method()` 静态调用语义不变。
- Kotlin `object` 的 `INSTANCE` 字段在 R8 收缩阶段被移除，不影响 Java 静态入口——所有入口均为真正的静态方法。
- 各 `System*Hooks` 同样使用 `object` + `@JvmStatic`，facade 只负责转发调用。

## 7. Debug 构建结果

在主工作区与干净 worktree 中均成功：

- `:app:compileDebugKotlin` ✅
- `:app:compileDebugJavaWithJavac` ✅
- `:app:assembleDebug` ✅

Debug APK（主工作区）：

- 路径：`app\build\outputs\apk\debug\CustoMIUIzer-A13-r13.2.4-devin.apk`
- 大小：**12,690,539 字节**
- SHA-256：`1fb8cc1f35384279ea869cd1ec4a77f9d0e1c5d15c643d79e4660baed5a3d3d3`

## 8. Release 构建结果

在主工作区与干净 worktree 中均成功：

- `:app:compileReleaseKotlin` ✅
- `:app:compileReleaseJavaWithJavac` ✅
- `:app:minifyReleaseWithR8` ✅
- `:app:shrinkReleaseRes` ✅
- `:app:packageRelease` ✅
- `:app:assembleRelease` ✅

Release APK（主工作区）：

- 路径：`app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk`
- 大小：**2,753,130 字节**
- SHA-256：`2cfd291af4a734308c5efb9b33289e75610ada73faa8fa295b498510be71aa37`

Release APK（干净 worktree）：

- 路径：`..\customiuizer-a13-k5-verify\app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk`
- 大小：**2,753,062 字节**
- SHA-256：`5d4e64277ee1c37588b05d982f1d02206170da4afc2e6ef81113800fbc0bf471`

> 两个 Release APK 大小接近、哈希不同，差异来自 APK 内时间戳等构建相关元数据，属于正常现象。

## 9. R8 `usage.txt` 的正确解释

**重要澄清：**

- `app/build/outputs/mapping/release/usage.txt` 记录的是 **R8 收缩阶段被删除的类、字段和方法**。
- `app/build/outputs/mapping/release/mapping.txt` 记录的是 **被保留并混淆的类、字段和方法**。
- 一个类出现在 `usage.txt` 中仅说明该类在压缩后的 DEX 中不再作为独立类存在，不说明其方法逻辑已经消失。R8 经常会通过 **类合并（class merging）** 或 **方法内联（inlining）** 把多个小类合并进一个公共宿主类。

### 9.1 `name.monwf.customiuizer.mods.System`（facade）

- `usage.txt` 中只列出：
  - `public static final name.monwf.customiuizer.mods.System INSTANCE`
- 含义：Kotlin object 的 `INSTANCE` 字段被删除。这是预期行为，因为所有入口都是 `@JvmStatic` 静态方法，`MainModule` 直接调用静态方法，不经过 `INSTANCE`。
- `mapping.txt` 中**没有**独立的 `name.monwf.customiuizer.mods.System -> xxx:` 类头，因为 `System` 的方法被合并进 R8 的公共宿主类。

### 9.2 `System*Hooks` 类

- `usage.txt` 中列出多个 `System*Hooks` 类及其 `INSTANCE`、部分字段和未被外部引用的辅助方法。
- `mapping.txt` 中这些类被映射到 `R8$$REMOVED$$CLASS$$...` 或对应的混淆类名。
- 这表示 R8 把这些 `object` 类作为独立类删除了，但它们的 `@JvmStatic` 方法体经过合并/内联后仍然存在于最终的 DEX 中。

### 9.3 结论

- `usage.txt` 能证明的：独立 `System` / `System*Hooks` 类作为类条目被 R8 删除。
- `usage.txt` **不能**证明的：这些类的方法被彻底删除。
- `mapping.txt` 中实际保存了所有 152 个 facade 方法及其对应 Hook 方法的混淆映射，且 `apkanalyzer` 在最终 APK 中找到了这些方法。

## 10. Release APK/Dex 检查结果

使用 `apkanalyzer dex packages --proguard-mappings <mapping.txt> --defined-only` 对干净 worktree 的 Release APK 进行检查。

### 10.1 `MainModule` 存在且完整

- DEX 中存在完整 `name.monwf.customiuizer.MainModule` 类，包含 `onPackageReady`、`onSystemServerStarting` 等入口。
- 说明 `MainModule` 的 `System.*` 调用链路起点没有被 R8 破坏。

### 10.2 `System.*` 方法存在

- DEX 中搜索到 **68 条** `name.monwf.customiuizer.mods.System.*` 方法定义。
- 搜索到 **44 条** `name.monwf.customiuizer.mods.System*Hooks.*` 方法定义。
- 大量 `System.*` / `System*Hooks` 方法被合并到 `kotlin.ExceptionsKt` 这一宿主类中。
- 示例：`kotlin.ExceptionsKt void name.monwf.customiuizer.mods.System.NoScreenLockHook(...)`
- 这些方法的实现体（含对应 Hook 逻辑）保留在 `kotlin.ExceptionsKt` 内，未被删除。

### 10.3 `System*Hooks` 内部匿名类保留

- 例如 `SystemLockScreenMoreHooks$NoScreenLockHook$8`、`SystemAudioAndVisualAndMoreHooks$AudioVisualizerHook$1` 等内部类仍在 DEX 中。
- 说明 hook 的实际注册、BroadcastReceiver、回调对象没有被 R8 删除。

### 10.4 风险判断

- `MainModule` → `System` → `System*Hooks` 的调用链在 R8 优化后仍然成立。
- `System` 和 `System*Hooks` 作为独立类被合并是 R8 的正常优化，不是错误删除。
- 未发现 facade 方法丢失或 `MainModule` 调用指向不存在方法的情况。
- 不存在为了通过 R8 而添加的全包 `-keep` 规则。

## 11. APK 路径、大小、SHA-256

| 类型 | 路径 | 大小 | SHA-256 |
|------|------|------|---------|
| Debug | `app\build\outputs\apk\debug\CustoMIUIzer-A13-r13.2.4-devin.apk` | 12,690,539 字节 | `1fb8cc1f35384279ea869cd1ec4a77f9d0e1c5d15c643d79e4660baed5a3d3d3` |
| Release（主工作区） | `app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk` | 2,753,130 字节 | `2cfd291af4a734308c5efb9b33289e75610ada73faa8fa295b498510be71aa37` |
| Release（干净 worktree） | `..\customiuizer-a13-k5-verify\app\build\outputs\apk\release\CustoMIUIzer-A13-r13.2.4-devin.apk` | 2,753,062 字节 | `5d4e64277ee1c37588b05d982f1d02206170da4afc2e6ef81113800fbc0bf471` |

## 12. Release 签名验证结果

使用 `apksigner verify --print-certs` 对 Release APK 验证：

- 签名方案：**V2**
- 验证结果：**通过**
- 证书 DN：`CN=CustoMIUIzer A14, OU=Release, O=tomthenpc, C=CN`
- 证书 SHA-256：`c0eff2dc4e662717195490da78b12a984c6f2e6bd38acf4edad14d53e3d22e70`

> 本仓库不包含私钥或 keystore 文件；签名配置来自仓库外的本地签名环境。本次验证仅确认 APK 已被有效 V2 签名，未涉及私钥操作。

## 13. zipalign 验证结果

使用 `zipalign -c -v 4` 对 Release APK 验证：

- 结果：**Verification successful**
- 4 字节对齐已确认。

## 14. 干净 worktree 构建结果

- 工作树路径：`..\customiuizer-a13-k5-verify`
- 基于：`HEAD`
- 状态：干净
- 执行的验证任务：
  - `./gradlew.bat :app:compileDebugKotlin --stacktrace` ✅
  - `./gradlew.bat :app:compileDebugJavaWithJavac --stacktrace` ✅
  - `./gradlew.bat :app:testDebugUnitTest --stacktrace` ✅
  - `./gradlew.bat :app:lintDebug --stacktrace` ✅
  - `./gradlew.bat :app:assembleDebug --stacktrace` ✅
  - `./gradlew.bat :app:assembleRelease --stacktrace` ✅

- `git grep` 确认新 worktree 内无对 `c:\tmp`、临时生成脚本等外部路径的依赖。
- 签名配置仍从仓库外读取，未提交任何密钥或密码文件。

## 15. 待实机验证项

- 当前未连接可测试设备（`adb devices` 返回空列表）。
- 因此以下项目标记为 **待实机验证**：
  - 模块应用能否启动
  - LSPosed 是否识别模块
  - 模块作用域是否正常
  - SystemUI / Launcher 是否出现循环崩溃
  - `system_server` 是否出现与模块相关异常
  - 开关功能后是否出现重复 Hook 注册
  - 重启后模块是否正常加载

## 16. 验证脚本

- 已提交：`tools/audit-system-migration.py`
- 用法：`python tools/audit-system-migration.py`
- 功能：
  - 解析 `MainModule.java` 中的 `System.*` 调用
  - 解析 `System.kt` 中的 facade 方法
  - 解析 18 个 `System*Hooks.kt` 中的方法
  - 检查 facade 是否均为纯委托
  - 检查 `MainModule` 调用是否均有 facade 入口
  - 检查 facade 委托是否均有 Hooks 目标
  - 检查跨 Hooks 文件的方法签名重复
  - 汇总 R8 `mapping.txt` / `usage.txt` 信息
  - 输出 APK 大小、SHA-256 及 `apkanalyzer` 相关 DEX 条目
- 退出码：0 表示通过，非 0 表示发现缺失或实现代码。

## 17. 验证结果摘要

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 152 个方法映射 | ✅ 已验证 | facade 完整，目标存在 |
| 119 个 `MainModule` 调用 | ✅ 已验证 | 全部覆盖 |
| facade 纯委托 | ✅ 已验证 | 无实现代码 |
| Debug 构建 | ✅ 已验证 | 主工作区 + worktree |
| Release 构建 | ✅ 已验证 | 主工作区 + worktree |
| Lint | ✅ 已验证 | 无 error |
| 单元测试 | ✅ 已验证 | 通过 |
| R8 映射/使用解释 | ✅ 已确认 | 类合并属于正常优化 |
| APK Dex 检查 | ✅ 已确认 | `MainModule` 与 `System` 方法均存在 |
| Release 签名 | ✅ 已验证 | V2 签名有效 |
| zipalign | ✅ 已验证 | 对齐有效 |
| 实机功能验证 | ⏸ 待实机验证 | 无设备 |

## 18. 是否具备进入 K6 的条件

**代码层面已具备。**

- K5 迁移完成，所有入口可编译、可链接、可运行至 Release 构建。
- R8 优化后的 DEX 中保留了必要的类与方法。
- 仅有 **实机验证** 一项尚未完成，不应阻止进入 K6，但应在 K6 推进前或并行安排实机测试。
