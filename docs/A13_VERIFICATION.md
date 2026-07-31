# A13 验证体系

> 本文定义 A13 仓库的四个验证层级：
> - `已验证`：构建/测试/实机证据完整且可复现；
> - `代码层面确认`：静态可判定，无需设备；
> - `待实机验证`：需真机或 LSPosed 日志；
> - `无法确认`：受设备/ROM/签名条件限制，必须明确声明。

---

## 一、验证分级

| 级别 | 含义 | 可接受的证据 |
| --- | --- | --- |
| 已验证 | 已完成并可复现 | 命令退出码 0 + 产物或日志 |
| 代码层面确认 | 静态可判定 | 源码/AST/工具输出 |
| 待实机验证 | 需要真机 | 安装后的 LSPosed 日志、功能行为记录 |
| 无法确认 | 当前无法验证 | 必须明确写“未验证”及原因 |

禁止把旧版本、A14 版本或其他仓库的验证结果直接套用为 A13 当前源码的验证结果。

---

## 二、标准验证命令

A13 当前 Gradle 实际可用任务（以 `gradlew.bat tasks` 为准）：

```powershell
.\gradlew.bat :app:compileDebugKotlin --stacktrace
.\gradlew.bat :app:testDebugUnitTest --stacktrace
.\gradlew.bat :app:lintDebug --stacktrace
.\gradlew.bat :app:lintRelease --stacktrace
.\gradlew.bat :app:lintVitalRelease --stacktrace
.\gradlew.bat :app:assembleDebug --stacktrace
.\gradlew.bat :app:assembleRelease --stacktrace
```

若 A13 当前没有某项任务，记录真实可用替代项，禁止伪造已执行结果。

---

## 三、静态门禁

工具就位后优先执行：

```powershell
python tools/check-invariants.py
```

覆盖规则（见 `A13_RUNTIME_INVARIANTS.md`）：

1. `guard-framework-callbacks`
2. `guard-deferred-callbacks`
3. `no-raw-register-receiver`
4. `no-redundant-arg-marshalling`
5. `no-looperless-handler`
6. `no-legacy-xposed`
7. `no-regex-split-on-literal`

每次源码改动后必须保持退出码 0。

---

## 四、Release 产物审计

执行 `assembleRelease` 后检查：

| 项目 | 命令/方法 |
| --- | --- |
| APK 路径 | `app/build/outputs/apk/release/` |
| 文件大小 | `Get-Item` / `ls -l` |
| SHA-256 | `Get-FileHash -Algorithm SHA256` |
| zipalign | 在 R8 流程后检查 `zipalign -c -v 4 <apk>` |
| 签名 | `apksigner verify -v <apk>` |
| 签名证书 SHA-256 | `apksigner verify --print-certs <apk>` |
| applicationId | `aapt2 dump badging <apk>` |
| versionName/versionCode | `aapt2 dump badging <apk>` |
| ABI | `aapt2 dump badging <apk>` 或 APK 中 `lib/arm64-v8a/` |
| xposed 元数据 | 解压 `META-INF/xposed/module.prop` |
| scope | 解压 `META-INF/xposed/scope.list` |
| R8 mapping | `app/build/outputs/mapping/release/mapping.txt` |
| resource shrink | `app/build/outputs/mapping/release/resources.txt` |
| Legacy Xposed API 扫描 | DEX 扫描 `de.robv.android.xposed` |

APK、keystore、签名备份、构建缓存、本地日志均不得提交。

---

## 五、R8 与动态入口核对

涉及删除、重命名、私有化、移动代码前，检查：

1. `META-INF/xposed`
2. `AndroidManifest.xml`
3. 反射和字符串类名
4. DexKit
5. XML 和动态资源
6. JNI/native
7. `app/proguard-rules.pro`
8. Java/Kotlin 静态入口
9. preference key
10. resource shrink 输出

`proguard-rules.pro` 应保持：

```proguard
-keep class name.monwf.customiuizer.MainModule { *; }
-keepclassmembers class name.monwf.customiuizer.mods.** { *; }
-keep class name.monwf.customiuizer.mods.** { *; }
```

Release 构建后核对：
- R8 保留方法数与预期一致。
- `-repackageclasses` 后入口类名写入 `META-INF/xposed/java_init.list`。
- `module.prop` 中 `minApiVersion` / `targetApiVersion` / `staticScope` 正确。

---

## 六、单元测试

A13 当前已有测试覆盖：

- `AppLocaleControllerTest`
- `SearchStateMachineTest`
- `SearchRouteResolverTest`
- `ModuleMetadataTest`
- `PrefPairTest`

后续按风险补充：

- `ResourceHooks` 热路径测试
- `XposedHelpers` 反射缓存测试
- 迁移后的控制流对照测试

执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest --stacktrace
```

---

## 七、LSPosed 日志审计

拿到日志后先跑脚本，不直接读 `full.log`：

```powershell
python tools/analyze_lsposed_log.py `
    "C:\path\full.log" `
    --profile a13 `
    --repo-root "." `
    --output "build\log-analysis\<标签>"
```

输出产物：

| 文件 | 说明 |
| --- | --- |
| `summary.md` | P0/P1/P2 数量、模块加载情况、最终结论 |
| `candidates.tsv` | 归并后的候选问题 |
| `contexts.log` | P0/P1 及必要 P2 的上下文片段 |

分诊原则见 `A13_EXECUTION_PLAYBOOK.md`。

---

## 八、内存基线

建立对照实验：

```powershell
.\tools\capture-memory-baseline.ps1 -Scenario "T0_boot_1min" -Samples 3 -DelaySeconds 5
```

目标进程：

- `tv.withaibuild.customiuizer.r13`（设置应用）
- `system_server`
- `com.android.systemui`
- `com.miui.home`

比较：

```powershell
python tools/compare-memory-baseline.py `
    --baseline .devin/memory-audit/summary_baseline_disabled.json `
    --current .devin/memory-audit/summary_current_user_config.json `
    --output .devin/memory-audit/comparison.md
```

---

## 九、本阶段验证状态（release/r13.8.5）

| 项目 | 状态 | 证据 |
| --- | --- | --- |
| `compileDebugKotlin` / `compileReleaseKotlin` | 已验证 | 代码层面成功编译 |
| `System.java` 完整 | 代码层面确认 | 迁移后无 `System.java` 修改 |
| `System.kt` facade | 代码层面确认 | 17 个 `System*Hooks` 文件已替换 facade |
| 新 `System*.kt` 跨域调用 | 待实机验证 | 需按 `A13_SPLIT_AND_MIGRATION_METHOD.md` 复核 |
| 测试 | 已验证 | `gradlew :app:test`（含 debug/develop/release 单元测试）通过 |
| Lint | 已验证 | `lintDebug`、`lintRelease` 通过；`lintVitalRelease` 当前配置跳过 |
| R8 / Release | 已验证 | `assembleDebug`、`minifyReleaseWithR8`、`assembleRelease` 通过 |
| APK 元数据 | 已验证 | `versionName=r13.8.5`，`versionCode=130`，applicationId 正确，v2 签名有效 |
| 性能优化 | 代码层面确认 | 图标缓存限制、MainApplication 内存回调、Catalog/Resolver 懒加载、热路径 getArg/DecimalFormat/Regex 预编译已落地；PSS 基线/对比需实机测量 |
| 实机/LSPosed | 待实机验证 | 安装、作用域、日志与功能行为需真机确认 |

---

## 十、禁止事项

- 不伪造未执行的验证结果。
- 不拿 A14 构建产物冒充 A13 验证。
- 不拿 Debug 构建冒充 Release 验证。
- 不拿 API 101 验证冒充 API 102 验证。
- 不声称“应该没问题”等无法复现的断言。
