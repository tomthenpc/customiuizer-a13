# UPGRADE-a13-build-toolchain-security-baseline

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

在保持应用运行逻辑不变的前提下，升级 A13 构建工具链并建立 Gradle 供应链安全基线。

目标版本：

```text
Gradle 8.14.5
Android Gradle Plugin 8.13.2
Kotlin 2.3.21
JDK 17
compileSdk 36
targetSdk 34
```

## 当前问题

构建工具链版本较旧，缺乏 Gradle 依赖验证、官方仓库优先和 mavenLocal 显式 opt-in 等供应链安全基线。

## 允许修改

- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradlew`
- `gradlew.bat`
- `gradle/libs.versions.toml`
- `gradle.properties`
- `settings.gradle.kts`
- `gradle/verification-metadata.xml`（新增）
- 任务文件

## 必须保持

- 不修改 `app/src/main/java` 或 `app/src/test` 等生产/测试 Java/Kotlin 源码；
- `compileSdk 36`、`targetSdk 34`、`minSdk 33`、JDK 17；
- `org.gradle.configuration-cache=true`、 `org.gradle.caching=true`；
- `applicationId` 不变；
- `applicationVariants` APK 文件名逻辑不变；
- libxposed API/service 坐标不变；
- 不迁移到 AGP 9 built-in Kotlin。

## 非目标

- 不解决 AGP 8.13 的 `applicationVariants` 弃用警告（仅记录）；
- 不迁移到 AGP 9 built-in Kotlin；
- 不修改 A14 仓库；
- 不修改生产功能源码。

## 验收标准

- [x] Gradle 为 8.14.5
- [x] AGP 为 8.13.2
- [x] Kotlin 为 2.3.21
- [x] JDK 仍为 17
- [x] compileSdk 36 不再需要 `suppressUnsupportedCompileSdk`
- [x] Wrapper 四个文件由官方 Wrapper 任务生成
- [x] `distributionSha256Sum` 存在且匹配官方值
- [x] 新增 `gradle/verification-metadata.xml`
- [x] 默认 strict dependency verification 通过
- [x] 官方 repository 优先
- [x] JitPack 在无实际依赖时删除
- [x] mavenLocal 默认关闭，只能显式 opt-in
- [x] 无动态版本、SNAPSHOT 或意外传递依赖变化
- [x] 无用 coroutines catalog 项删除
- [x] `applicationVariants` 功能仍能配置和构建
- [x] source hazard 保持 26 reviewed、0 new
- [x] Python tests、Gradle tests、lint、fast verify、full verify 全部通过
- [x] 不修改生产功能源码
- [x] 工作区干净
- [x] 不要求签名 APK
- [x] Device status：`UNVERIFIED`
- [x] Verification status：`BUILD_VERIFIED`

## 验证

```powershell
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"

.\gradlew.bat --version
.\gradlew.bat help --warning-mode all

.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug

.\gradlew.bat :app:testDebugUnitTest --configuration-cache
.\gradlew.bat :app:testDebugUnitTest --configuration-cache

python tools/source_hazard_scan.py --path app/src/main/java
python tools/verify.py fast --changed
python tools/verify.py full

git diff --check
git status --short
```

### 实际结果

- `compileall tools`：通过
- `unittest discover`：449 tests passed, 0 failed, skipped 2
- `gradlew --version`：Gradle 8.14.5，JDK 17.0.12
- `gradlew help --warning-mode all`：通过
- `compileDebugKotlin`：通过
- `compileDebugJavaWithJavac`：通过
- `testDebugUnitTest`：通过
- `lintDebug`：通过
- `testDebugUnitTest --configuration-cache`：通过，两次均 `Reusing configuration cache`
- `source_hazard_scan.py`：`Source hazard scan passed: 26 reviewed finding(s), 0 new`
- `verify.py fast --changed`：通过
- `verify.py full`：通过（含 compileDebugKotlin、compileDebugJavaWithJavac、testDebugUnitTest-all、lintDebug）
- `git diff --check`：通过（仅 CRLF 转 LF 正常提示）
- `git status --short`：干净

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: 90302c296ae1016627ee042e0b3230947aa16b22
- 实现提交 SHA: 53d32ed1702a4e19eafa9ed1d21ee4c6921bc26e
- Final HEAD: 53d32ed1702a4e19eafa9ed1d21ee4c6921bc26e
- Gradle 升级前版本: 8.9
- Gradle 升级后版本: 8.14.5
- AGP 升级前版本: 8.7.2
- AGP 升级后版本: 8.13.2
- Kotlin 升级前版本: 2.0.21
- Kotlin 升级后版本: 2.3.21
- repository 变化:
  - `pluginManagement` 改为 `google()`、`mavenCentral()`、`gradlePluginPortal()` 优先，华为云和阿里云镜像作为 fallback；
  - `dependencyResolutionManagement` 删除 `mavenLocal`、`maven("https://jitpack.io")`、mirror 优先；
  - 添加 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`；
  - `mavenLocal` 仅在 `-PuseLocalLibxposed=true` 时启用，且仅 `includeGroup("io.github.libxposed")`。
- verification metadata 组件数: 427
- configuration cache 第二次是否复用: 是，两次 `--configuration-cache` 均 `Reusing configuration cache` / `Configuration cache entry reused`
- dependency graph 非预期变化: 无
- 所有验证命令结果: 全部通过
- Device status: `UNVERIFIED`
- Verification status: `BUILD_VERIFIED`
