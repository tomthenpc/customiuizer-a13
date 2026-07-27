# DEVIN R13 CHECKPOINT

> 本文件记录当前真实状态，不记录长期规则。长期规则统一放在根目录 `AGENTS.md`。
> 每完成一个有意义的代码、构建、Git 或实机闭环，立即更新本文件；替换过期事实，不要只在末尾追加。

## 当前目标

- 整理根目录 `AGENTS.md` 与 `docs/DEVIN_R13_CHECKPOINT.md`，同步 Devin 执行规则与 A13 项目边界；确保仓库级规则唯一、无冲突。

## 当前基线

- **Repository:** `tomthenpc/customiuizer-a13`
- **Branch:** `devin/r13.2-kotlin-api102`
- **HEAD:** `362cf83`
- **versionName / versionCode:** `r13.2.2-devin` / `120`
- **applicationId:** `tv.withaibuild.customiuizer.r13`
- **libxposed API:** `minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
- **Hot Reload:** `false`
- **Legacy Xposed API:** `false`
- **SDK:** `minSdk=33`，`targetSdk=34`，`compileSdk=36`
- **ABI:** `arm64-v8a`
- **最新已确认实机版本:** 未确认
- **最后正常行为基线:** `MonwF/customiuizer v23.11.26`

## 本轮已完成

### 代码
- `MainFragment.java` 搜索导航修复：点击搜索结果后先 `collapseSearch()` 再 `openModCat(...)`，避免返回时搜索页闪现后跳回首页（commit `362cf83`，已 push）。

### 测试与构建
- `./gradlew :app:assembleDebug`：BUILD SUCCESSFUL（`search-fix-debug.log`）
- `./gradlew :app:assembleRelease`：BUILD SUCCESSFUL，包含 `lintVitalRelease`（`search-fix-release.log`）
- `./gradlew :app:compileDebugJavaWithJavac :app:compileDebugKotlin`：BUILD SUCCESSFUL（`compile-check.log`）

### APK 与签名
- 产物：
  - `CustoMIUIzer-A13-r13.2.2-search-fix-debug.apk`
  - `CustoMIUIzer-A13-r13.2.2-search-fix-release.apk`
- Debug APK SHA-256：`ebd25bc0e9c6b56be9350b3e079b9ec2219d7d816721fa07154261df689dbcc2`
- Release 签名配置指向 `../keystore.properties`；当前仓库未提供该文件，release 包使用占位签名配置，未正式签名。

### Git
- 搜索修复已 push 到 `devin/r13.2-kotlin-api102`（`362cf83`）。
- 当前工作区：
  - 已修改：`docs/DEVIN_R13_CHECKPOINT.md`（本文件）
  - 未跟踪：`AGENTS.md`（待提交）、`.vscode/settings.json`、多个本地 APK 与构建日志。

### 文档
- 新建根目录 `AGENTS.md`，合并 A13 项目边界、长期规则与轻度 Claude 风格执行习惯。
- 更新 `docs/DEVIN_R13_CHECKPOINT.md` 为当前真实状态。

## 最新绿色验证

- **任务/命令：** `./gradlew --no-daemon :app:compileDebugJavaWithJavac :app:compileDebugKotlin`
- **结果：** BUILD SUCCESSFUL
- **产物：** `compile-check.log`
- **验证日期：** 2026-07-27

## 当前问题与阻塞

- 无。

## 待实机验证

- **项目：** 搜索定位返回行为
  - **条件：** 安装 `CustoMIUIzer-A13-r13.2.2-search-fix-debug.apk`，搜索关键词，点击结果进入目标页，按系统返回或 Toolbar 返回。
  - **预期：** 一次返回直接到达主设置列表（首页），无搜索页闪现，无二次跳转。
  - **状态：** pending
- **项目：** 搜索框旋转/页面重建
  - **条件：** 搜索状态下旋转设备或触发配置变更。
  - **预期：** 搜索关键词与结果列表状态保持或按设计重置，不崩溃。
  - **状态：** pending

## 下一步

- 提交并 push `AGENTS.md` 和 `docs/DEVIN_R13_CHECKPOINT.md` 到 `devin/r13.2-kotlin-api102`。

## 发布状态

- main 已合并：否
- PR 已创建：否
- tag 已创建：否
- GitHub Release 已创建：否
- APK 已公开上传：否
