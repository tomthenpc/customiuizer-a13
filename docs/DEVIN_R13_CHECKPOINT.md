# CustoMIUIzer A13 维护检查点

本文件记录当前分支 `devin/r13.2-kotlin-api102` 的基线、已完成工作、验证状态和后续待办。

## 1. 项目身份

- **仓库**：`customiuizer-a13-forDevin`
- **上游基线**：`MonwF/customiuizer v23.11.26`
- **A14 关系**：仅作为工程方法参考，不作为 A13 ROM 事实来源
- **应用 ID**：`tv.withaibuild.customiuizer.r13`
- **版本**：`versionCode=118`，`versionName=r13.2.0-devin`
- **分支**：`devin/r13.2-kotlin-api102`

## 2. 签名基线

- **证书 CN**：`CustoMIUIzer A13`
- **证书算法**：RSA 4096 / SHA256withRSA
- **证书有效期**：10950 天
- **证书 SHA-256**：`15ce32f03e4d8e62df9390f77431862e59bf2cf95cd5a72f0c7330cdfcca2934`
- **证书 SHA-1**：`f784faf95c96cc3691114a0193eab767c4e92d82`
- **私钥路径**（仅本地）：`C:/Users/tv/Documents/buildkey/r13/customiuizer-a13-release.p12`
- **本地配置**（未提交）：`../keystore.properties`
- **旧签名**：已遗失，旧版本无法覆盖安装

## 3. 构建系统

- **AGP**：8.7.2
- **Gradle**：8.9
- **Kotlin**：2.0.21
- **JDK**：17
- **compileSdk**：36（平台构建版本号显示为 `16`，SDK 平台目录存在异常，但构建通过）
- **minSdk**：33
- **targetSdk**：34
- **构建脚本**：Kotlin DSL（`settings.gradle.kts`、`build.gradle.kts`、`app/build.gradle.kts`）
- **依赖管理**：`gradle/libs.versions.toml`
- **Kotlin BOM**：`enforcedPlatform(libs.kotlin.bom)` 强制统一 Kotlin 标准库版本，避免 `libxposed` 传递 `kotlin-stdlib` 2.2.10 与 Kotlin 2.0.21 元数据冲突

## 4. Xposed 元数据

- `module.prop`：
  - `minApiVersion=101`
  - `targetApiVersion=102`
  - `staticScope=false`
- `java_init.list`：`name.monwf.customiuizer.MainModule`
- `scope.list`：保持原有 14 个作用域包名，未从 A14 复制

## 5. 本地验证记录

| 验证项 | 命令 | 结果 |
|--------|------|------|
| 单元测试 | `./gradlew :app:test` | 通过 |
| Lint Release | `./gradlew :app:lintRelease` | 通过 |
| Debug 构建 | `./gradlew :app:assembleDebug` | 通过 |
| Release 构建 | `./gradlew :app:assembleRelease` | 通过 |
| R8 / resource shrink | 包含在 assembleRelease 中 | 通过 |
| zipalign | `zipalign -c -v 4 app-release.apk` | 通过 |
| 签名证书 | `apksigner verify --print-certs` | `CN=CustoMIUIzer A13` |
| aapt2 badging | `aapt2 dump badging app-release.apk` | `package: name='tv.withaibuild.customiuizer.r13' versionCode='118' versionName='r13.2.0-devin' minSdkVersion:'33' targetSdkVersion:'34'` |
| Xposed 元数据 | 解压 `META-INF/xposed/module.prop` | `minApiVersion=101`、`targetApiVersion=102`、`staticScope=false` |
| 入口类 | 解压 `META-INF/xposed/java_init.list` | `name.monwf.customiuizer.MainModule` |
| 缺失签名失败 | 临时移除 `../keystore.properties` 后 `assembleRelease` | 明确失败：`Keystore file ... not found for signing config 'v2'` |

## 6. 已知未决

- **输出 APK 命名**：AGP 8.7 `VariantOutput` 未暴露 `outputFileName` 属性，当前使用默认 `app-<variant>.apk`，待 AGP API 可用后恢复 `CustoMIUIzer-A13-r13.x.x.apk`。
- **生命周期与性能治理**：`MainModule`/`ModuleHelper` 已有 `processHooked` 改进，但 Receiver、Observer、Handler、Coroutine、线程和 Hook 生命周期的系统性治理需按功能组继续推进。
- **Kotlin-first 迁移**：已完成 `GateWayLauncher` 和 `CredentialsLauncher`，后续按功能组、低风险边界继续迁移。
- **实机验证**：当前仅在构建层验证，尚未在 `V14.0.10.0.TLOINXM` / `V14.0.7.0.TLOCNXM` 实机回归。
- **R8 后入口类**：当前通过 `MainModule` 继承 `XposedModule` 和 `proguard-rules.pro` 保留，需进一步验证 R8 后 `name/monwf/customiuizer/MainModule.class` 仍存在。

## 7. 禁止项

- 不得恢复 `de.robv.android.xposed` Legacy API。
- 不得直接复制 A14 的 MIUI 类名、方法/字段签名、Hook target、资源 ID、SystemUI/Launcher 结构、ROM 判断、preference key、Manifest 组件。
- 不得提交 `keystore.properties`、APK、keystore、密码、token 或实机日志。
- CI 没有本地正式私钥时，不得伪装成正式签名构建或正式 Release。

## 8. 后续推荐优先级

1. 实机回归目标 ROM，确认 System UI、桌面、system_server 等核心 Hook。
2. 按功能组审查生命周期：避免匿名内部类长期持有 Activity / Preference 引用，统一 `Handler`/`Observer` 的 remove/destroy 路径。
3. 高频 Hook 优化：缓存反射结果、减少 `XposedHelpers` 全量查找、避免在主线程重复读取 SharedPreferences。
4. 逐步将边界简单、行为稳定的 Java 类迁移到 Kotlin，保持与现有 Hook 逻辑一致。
5. 恢复 APK 输出命名并补充 CI 中的 R8 入口类校验。
