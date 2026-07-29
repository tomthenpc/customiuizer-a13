# Nightly Handoff — 2026-07-29

## 基线

- 分支：`devin/r13.6-runtime-backport`
- 起始：`d628d89`（K8.3.1 完成后）
- 备份：`backup/r13-before-night-run-20260729`（196fa9ab）
- 当前 HEAD：`0625fe8`
- A14 参考：`a1929885`（r14.13.7 运行时修复合并）

## 本轮完成工作

1. **K8.3.1 已确认完成**
   - 设备监控内容模式 1/2/3 只读取必要 sysfs 文件。
   - 主开关从动态快照移除，ticker 使用 hook 时捕获值。
   - 非法 content 选项回退到模式 1。
   - 添加 `SystemUIStatusBarHooksDeviceMonitorTest` / `PrefMapTest`。

2. **阶段 B：A14 r14.13.7 差距审计**
   - 创建 `docs/K9_NIGHTLY_RUNTIME_GAP_AUDIT.md`。
   - 比较 A14 运行时修复、A13 LSPosed 日志 P0/P1 候选，按 SAFE_BACKPORT / NEEDS_DEVICE_TEST / A14_ONLY 分类。

3. **阶段 C：SAFE_BACKPORT（两条已提交）**
   - `cedf412` `Various.kt`：加固 `AppsDisableServiceHook`，不覆盖异常路径，安全类型转换。
   - `a8e8a96` `XposedHelpers.java`：将 `additionalFields` 从 `WeakHashMap` 替换为基于身份键的 `ConcurrentHashMap` + `ReferenceQueue`，消除 `equals`/`hashCode` 冲突导致的字段丢失与内存泄漏。

4. **阶段 D：Java/Kotlin 收尾清单**
   - 创建 `docs/K9_JAVA_KOTLIN_REMAINDER.md`，记录 6 个剩余 Java 文件均为 RED/保留边界。

5. **阶段 E：性能审计**
   - 跳过：反射缓存已使用 `ConcurrentHashMap` + `MemberCacheKey`，与 A14 同构；搜索/参数分配等 P3 项未在本轮处理。

6. **阶段 F：完整验证**
   - `tools/check-invariants.py`：117 文件，0 违规。
   - `tools/audit-system-migration.py`：通过，无阻塞问题。
   - `gradlew clean :app:lintRelease :app:testDebugUnitTest :app:assembleRelease`：全部成功。
   - 单元测试：通过。

7. **阶段 G：APK 与签名验证**
   - APK：`app/build/outputs/apk/release/CustoMIUIzer-A13-r13.2.4-devin.apk`
   - 大小：约 2.76 MB
   - SHA-256：`B20BB851FA826C27DEF3DB9CDDAF7E14BF9D758158F40BA8D23F864DD2EB190D`
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
- `AppLocaleController` / 语言设置 no-op、`MainFragment` 搜索状态机等 UI 修复（A13 实体验证后再决定）。

## Git 状态

```text
0625fe8 (HEAD -> devin/r13.6-runtime-backport) docs(k9): list remaining Java boundaries and migration freeze
a8e8a96 fix(k9): use weak identity keys for additional instance fields
cedf412 fix(k9): guard AppsDisableServiceHook result override and avoid masking system exceptions
38a9da2 docs(k9): audit remaining r14.13.7 gaps after runtime backports
d628d89 (origin/devin/r13.6-runtime-backport) fix(k8): correct device monitor data dependencies
```

- 工作区干净：`git status --short` 无输出。
- 相对于远端 `origin/devin/r13.6-runtime-backport` 领先 4 个提交。

## 下一步

- 推送当前 4 个提交到 `origin/devin/r13.6-runtime-backport`。
- 若实机测试发现问题，从备份 `backup/r13-before-night-run-20260729` 回滚到 `196fa9ab`。
