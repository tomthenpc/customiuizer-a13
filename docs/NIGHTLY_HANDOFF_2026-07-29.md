# Nightly Handoff — 2026-07-29

## 基线

- 分支：`devin/r13.6-runtime-backport`
- 合并前源分支最终 HEAD：`docs(k9): finalize verification before main merge`
- 备份：待 `backup/main-before-r13.6-merge-20260729` 创建（`origin/main` 合并前）
- A14 参考：`a1929885`（r14.13.7 运行时修复合并）

## 从 `d628d89` 到源分支最终 HEAD 的提交

共 8 个提交：

```text
38a9da2 docs(k9): audit remaining r14.13.7 gaps after runtime backports
cedf412 fix(k9): guard AppsDisableServiceHook result override and avoid masking system exceptions
a8e8a96 fix(k9): use weak identity keys for additional instance fields
0625fe8 docs(k9): list remaining Java boundaries and migration freeze
9eba020 docs(k9): nightly handoff and verification summary
d5aa71f test(k9): cover weak identity additional fields
c91f155 docs(k9): finalize nightly verification
docs(k9): finalize verification before main merge
```

## 本轮完成工作

1. **K8.3.1 已确认完成**
   - 设备监控内容模式 1/2/3 只读取必要 sysfs 文件。
   - 主开关从动态快照移除，ticker 使用 hook 时捕获值。
   - 非法 content 选项回退到模式 1。
   - 添加 `SystemUIStatusBarHooksDeviceMonitorTest` / `PrefMapTest`。

2. **阶段 B：A14 r14.13.7 差距审计**
   - 创建 `docs/K9_NIGHTLY_RUNTIME_GAP_AUDIT.md`。
   - 比较 A14 运行时修复、A13 LSPosed 日志 P0/P1 候选，按 SAFE_BACKPORT / NEEDS_DEVICE_TEST / A14_ONLY 分类。

3. **阶段 C：SAFE_BACKPORT**
   - `cedf412` `Various.kt`：加固 `AppsDisableServiceHook`，不覆盖异常路径，安全类型转换。
   - `a8e8a96` `XposedHelpers.java`：将 `additionalFields` 从 `WeakHashMap` 替换为基于身份键的 `ConcurrentHashMap` + `ReferenceQueue`，消除 `equals`/`hashCode` 冲突导致的字段丢失与内存泄漏。

4. **阶段 D：Java/Kotlin 收尾清单**
   - 创建 `docs/K9_JAVA_KOTLIN_REMAINDER.md`，记录 6 个剩余 Java 文件均为 RED/保留边界。

5. **阶段 E：性能审计**
   - 跳过：反射缓存已使用 `ConcurrentHashMap` + `MemberCacheKey`，与 A14 同构；搜索/参数分配等 P3 项未在本轮处理。

## 最终门禁

- `git diff --check`：无空白错误。
- `tools/check-invariants.py`：117 文件，0 违规。
- `gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease --stacktrace`：BUILD SUCCESSFUL。
- `gradlew.bat :app:lintVitalRelease --stacktrace`：任务存在，BUILD SUCCESSFUL。

## 单元测试

- `testDebugUnitTest`：184 tests，0 failures，0 errors，0 skipped。

### `AdditionalInstanceFieldTest` 覆盖

- 测试数量：10
- 并发测试：`concurrentReadersAndWritersStaySane`（8 线程 × 5000 轮 set/get）
- GC 测试：`ownersAreNotKeptAlive`、`theProbeDoesNotPinTheLastOwnerItLookedUp`（有限 20 次循环，配合 `System.gc()` 与 `setAdditionalInstanceField(Any(), ...)` 搅动）
- 覆盖点：
  - 两个 `equals()` 相等但实例不同的对象不共享字段
  - owner 的 `hashCode()` 相关字段变化后，附加字段仍可读取
  - 不同 owner 隔离
  - 同一 owner 多个 key 隔离
  - `set` 返回旧值
  - `remove` 返回旧值
  - `null` 值与不存在值的语义
  - `null` owner / `null` key 参数拒绝
  - WeakReference owner 可被 GC
  - ThreadLocal probe 不持有最后查询对象
  - 多线程并发 set/get/remove 不串数据、不崩溃
  - 静态 additional field 行为不受影响
- 说明：A14 的 `readsAreAllocationFree` 测试依赖 `com.sun.management.ThreadMXBean`，A13 单元测试 classpath 不包含该包，因此未回移；热路径零分配仍由 `LookupInstanceKey` ThreadLocal 设计保证。

## APK 与签名

- Debug APK：`app/build/outputs/apk/debug/CustoMIUIzer-A13-r13.2.4-devin.apk`
  - 大小：约 11.28 MB
  - SHA-256：`09ED8BE927E768EBDD112272AA47574C49BE3712FE1F1325B99FCFE81FCC96C8`
- Release APK：`app/build/outputs/apk/release/CustoMIUIzer-A13-r13.2.4-devin.apk`
  - 大小：约 2.76 MB
  - SHA-256：`FAD09FC9240F7D862ABA1179B3487A629F7D46BF34D7EA2C0AA3D7A1AA6974B2`
  - 签名方案：v2（`Verified using v2 scheme: true`）
  - 证书 CN：`CustoMIUIzer A14, OU=Release, O=tomthenpc, C=CN`
  - 证书 SHA-256：`c0eff2dc4e662717195490da78b12a984c6f2e6bd38acf4edad14d53e3d22e70`
  - `java_init.list`：`tv.withaibuild.customiuizer.MainModule`
  - `module.prop`：`minApiVersion=101, targetApiVersion=102, staticScope=false`
  - `scope.list`：包含 systemui/launcher/settings 等目标。

## 尚未处理的 SAFE_BACKPORT（需要后续实机或专门批次）

- `ModuleHelper.guarded` 未覆盖的 23 个 system_server/SystemUI 回调（需要逐一对照 A14）。
- 图标加载队列 `DiscardOldestPolicy` → `AbortPolicy` 与占位图释放（`LauncherIconHooks` 等）。
- 锁屏 editor 的并发 blur 缓存（`SystemLockScreenMoreHooks`）。
- `AppLocaleController` / 语言设置 no-op、`MainFragment` 搜索状态机等 UI 修复。

## 剩余

- K7 实机冒烟验证：待实机验证
- 上述 NEEDS_DEVICE_TEST 与 A14_ONLY 项：待后续夜间批次评估。
