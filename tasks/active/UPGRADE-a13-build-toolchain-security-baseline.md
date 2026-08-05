# UPGRADE-a13-build-toolchain-security-baseline

- Platform: A13
- Status: Active
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
- `build-dependencies-before.txt` / `build-dependencies-after.txt`（临时，不提交）
- 任务文件

## 必须保持

- 不修改 `app/src/main/java` 或 `app/src/test` 等生产/测试 Java/Kotlin 源码（除非新增测试为任务所必需）；
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

- [ ] Gradle 为 8.14.5
- [ ] AGP 为 8.13.2
- [ ] Kotlin 为 2.3.21
- [ ] JDK 仍为 17
- [ ] compileSdk 36 不再需要 `suppressUnsupportedCompileSdk`
- [ ] Wrapper 四个文件由官方 Wrapper 任务生成
- [ ] `distributionSha256Sum` 存在且匹配官方值
- [ ] 新增 `gradle/verification-metadata.xml`
- [ ] 默认 strict dependency verification 通过
- [ ] 官方 repository 优先
- [ ] JitPack 在无实际依赖时删除
- [ ] mavenLocal 默认关闭，只能显式 opt-in
- [ ] 无动态版本、SNAPSHOT 或意外传递依赖变化
- [ ] 无用 coroutines catalog 项删除
- [ ] `applicationVariants` 功能仍能配置和构建
- [ ] source hazard 保持 26 reviewed、0 new
- [ ] Python tests、Gradle tests、lint、fast verify、full verify 全部通过
- [ ] 不修改生产功能源码
- [ ] 工作区干净
- [ ] 不要求签名 APK
- [ ] Device status：`UNVERIFIED`
- [ ] Verification status：`BUILD_VERIFIED`

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

## 构建产物

未要求 APK。

## 完成记录

- Base SHA:
- 实现提交 SHA:
- Final HEAD:
- Gradle 升级前版本:
- Gradle 升级后版本:
- AGP 升级前版本:
- AGP 升级后版本:
- Kotlin 升级前版本:
- Kotlin 升级后版本:
- repository 变化:
- verification metadata 组件数:
- configuration cache 第二次是否复用:
- dependency graph 非预期变化:
- 所有验证命令结果:
