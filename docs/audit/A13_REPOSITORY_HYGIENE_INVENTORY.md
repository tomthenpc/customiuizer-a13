# A13 Repository Hygiene Inventory

## 审计元数据

| 字段 | 值 |
|------|-----|
| Base SHA | `00ac7ebdcc6943fb0341e839e34b6eade78c5060` |
| 审计日期 | 2026-08-06 |
| 审计任务 | `AUDIT-A13-REPOSITORY-HYGIENE-INVENTORY` |
| 最终状态 | `MANUAL_REVIEW_REQUIRED` |

## 当前仓库状态

| 验证项 | 结果 |
|--------|------|
| 分支 | `devin/a13-rom-intelligence-audit` |
| HEAD | `00ac7ebdcc6943fb0341e839e34b6eade78c5060` |
| upstream | `origin/devin/a13-rom-intelligence-audit` |
| ahead/behind | `0/0` |
| 工作区 | clean |
| tracked 文件总数 | 748 |

## 第一阶段：跟踪文件清单

### 跟踪文件分类命中

| 分类 | 命中项 | 建议 |
|------|--------|------|
| `.idea/` 配置 | `.idea/codeStyles/Project.xml`<br>`.idea/codeStyles/codeStyleConfig.xml`<br>`.idea/compiler.xml`<br>`.idea/encodings.xml`<br>`.idea/gradle.xml`<br>`.idea/inspectionProfiles/Project_Default.xml`<br>`.idea/jarRepositories.xml`<br>`.idea/vcs.xml` | 均为共享项目配置，无用户绝对路径；建议保留。 |
| `.vscode/` 配置 | 无 tracked | — |
| `*.iml` | 无 tracked | — |
| `local.properties` | 无 tracked | — |
| `keystore.properties` | 无 tracked | — |
| `secrets.properties` | 无 tracked | — |
| `.jks/.keystore/.p12/.pfx` | 无 tracked | — |
| `build/out/captures/backup/.gradle/.kotlin/.cxx/` | 无 tracked | — |
| `apk/aab/apks/dex/odex/vdex/so/class/jar` | `app/lib/framework.jar`<br>`app/lib/miui.jar`<br>`app/lib/miuisystem.jar`<br>`gradle/wrapper/gradle-wrapper.jar` | `gradle-wrapper.jar` 为标准 wrapper，保留；`app/lib/*.jar` 为框架 stub/库，体积较大，建议人工确认是否应继续以二进制形式跟踪或替换为 Maven/下载脚本。 |
| `mapping.txt` | 无 tracked | — |
| `.log/.tmp/.temp/.bak/.orig/.rej/.swp/.swo` | 无 tracked | — |
| `__pycache__/.pytest_cache/.mypy_cache/` | 无 tracked | — |
| `Thumbs.db/desktop.ini/.DS_Store` | 无 tracked | — |

### 显著跟踪文件

| 路径 | 大小 | 类型 | 建议 |
|------|------|------|------|
| `app/lib/framework.jar` | 2.54 MiB | 二进制 JAR | 人工确认 |
| `app/lib/miui.jar` | 1.64 MiB | 二进制 JAR | 人工确认 |
| `app/lib/miuisystem.jar` | 856 KiB | 二进制 JAR | 人工确认 |
| `gradle/wrapper/gradle-wrapper.jar` | 43 KiB | 二进制 JAR | 保留 |
| `docs/audit/HOOK_SURFACE_BASELINE.json` | 207 KiB | JSON 审计基线 | 保留 |
| `gradle/verification-metadata.xml` | 191 KiB | Gradle 验证元数据 | 保留 |

## 第二阶段：本地未跟踪与 ignored 清单

使用 dry-run 命令（未执行实际删除）：

```powershell
git status --short --untracked-files=all      # 0 条非 ignored 未跟踪项
git status --short --ignored --untracked-files=all
git clean -nd
git clean -ndX
```

### 分类统计

| 分类 | 数量 | 说明 |
|------|------|------|
| `UNTRACKED_NOT_IGNORED` | 0 | — |
| `IGNORED_GENERATED` | 72 | 根目录 `*.log`、`build*.log`、`compile*.log` 等 |
| `IGNORED_LOCAL_CONFIGURATION` | 55 | `.devin/` 目录、`.vscode/` 目录、`local.properties` |
| `IGNORED_SECRET_OR_SIGNING` | 0 | — |
| `IGNORED_BUILD_ARTIFACT` | 约 12,402 | `.gradle/`、`.kotlin/`、`app/build/`、`tools/__pycache__/` 等 |
| `UNKNOWN_REQUIRES_REVIEW` | 1 | `k7_window_dump.xml`（已在 `.git/info/exclude` 中忽略） |

### 根目录存在的未跟踪/ignored 文件示例

- `CustoMIUIzer-A13-r13.2.2-*.apk`（5 个 APK，约 1.2–2.7 MiB）
- `baseline-debug.log`、`build*.log`、`compile*.log` 等日志
- `local.properties`
- `rom-target-diff.md`

以上文件均被忽略（`.gitignore` 或 `.git/info/exclude`），未跟踪。

## 第三阶段：IDE 配置分类

### 已跟踪 `.idea/` 文件

| 文件 | 分类 | 理由 |
|------|------|------|
| `.idea/codeStyles/Project.xml` | 共享 | 定义 XML 排列规则，无绝对路径 |
| `.idea/codeStyles/codeStyleConfig.xml` | 共享 | 引用默认 code style |
| `.idea/compiler.xml` | 共享 | 指定 bytecode target 17 |
| `.idea/encodings.xml` | 共享 | 指定无 BOM |
| `.idea/gradle.xml` | 共享 | 使用 `$PROJECT_DIR$` 与 `Embedded JDK` |
| `.idea/inspectionProfiles/Project_Default.xml` | 共享 | 项目级 inspection 配置，无绝对路径 |
| `.idea/jarRepositories.xml` | 共享 | Maven 仓库列表，包含 `$USER_HOME$` 变量 |
| `.idea/vcs.xml` | 共享 | 使用 `$PROJECT_DIR$` 映射 Git |

结论：所有跟踪的 `.idea/` 文件均为共享项目配置，无用户级敏感信息。建议保留。

### `.git/info/exclude` 配置

本地 `exclude` 包含：

```text
.vscode/
*.apk
.devin/mcp_config.local.json
k7_window_dump.xml
```

这些文件当前未被跟踪，但规则只存在于本地 `exclude`，未进入共享 `.gitignore`。

## 第四阶段：大文件与二进制检查

### Top 20 跟踪文件（按大小降序）

| 大小 | 路径 | 类型 | 建议 |
|------|------|------|------|
| 2.54 MiB | `app/lib/framework.jar` | 二进制 | 人工确认 |
| 1.64 MiB | `app/lib/miui.jar` | 二进制 | 人工确认 |
| 856 KiB | `app/lib/miuisystem.jar` | 二进制 | 人工确认 |
| 207 KiB | `docs/audit/HOOK_SURFACE_BASELINE.json` | 文本/JSON | 保留 |
| 191 KiB | `gradle/verification-metadata.xml` | 文本/XML | 保留 |
| 129 KiB | `app/src/main/res/values-ru-rRU/strings.xml` | 文本/XML | 保留（合法资源） |
| 108 KiB | `app/src/main/java/.../SystemUIStatusBarHooks.kt` | 文本/Kotlin | 保留（源码） |
| 107 KiB | `app/src/main/res/values-ja-rJP/strings.xml` | 文本/XML | 保留 |
| 104 KiB | `app/src/main/java/.../FeatureCatalog.kt` | 文本/Kotlin | 保留 |
| 99 KiB | `app/src/main/res/values-vi-rVN/strings.xml` | 文本/XML | 保留 |
| 95 KiB | `app/src/main/res/values/arrays.xml` | 文本/XML | 保留 |
| 95 KiB | `app/src/main/res/raw/extended_power_menu` | 文本 | 保留 |
| 44 KiB | `gradle/wrapper/gradle-wrapper.jar` | 二进制 | 保留 |

超过 20 KiB 的文件以源码、翻译资源和审计基线为主，均属合理。

### 跟踪的二进制/非文本文件

| 路径 | 扩展名 | 说明 |
|------|--------|------|
| `app/lib/framework.jar` | `.jar` | 框架 stub |
| `app/lib/miui.jar` | `.jar` | 框架 stub |
| `app/lib/miuisystem.jar` | `.jar` | 框架 stub |
| `gradle/wrapper/gradle-wrapper.jar` | `.jar` | Gradle wrapper |
| `app/src/main/res/drawable-*/ic_*.png` 等 | `.png` | 图标资源 |
| `app/src/main/res/raw/test1.mp3` | `.mp3` | 测试/示例资源 |

未发现跟踪的 APK、AAB、DEX、ODEX、VDEX、SO、class 文件。

## 第五阶段：目录结构审计

### 根目录项

| 名称 | 类型 | 说明 |
|------|------|------|
| `.agents` | dir | agent skill 配置 |
| `.devin` | dir | 本地 Devin 工作文件（ignored） |
| `.git` | dir | Git 元数据 |
| `.github` | dir | GitHub 模板 |
| `.gitignore` | file | 共享 gitignore |
| `.gradle` | dir | Gradle 缓存（ignored） |
| `.idea` | dir | IDE 共享配置 |
| `.kotlin` | dir | Kotlin 守护进程（ignored） |
| `.vscode` | dir | VS Code 本地配置（ignored，仅在 local exclude） |
| `AGENTS.md` 等 | file | 项目文档 |
| `app` | dir | 应用源码/资源 |
| `build` | dir | 构建输出（ignored） |
| `docs` | dir | 文档与审计报告 |
| `gradle` | dir | Gradle wrapper |
| `java` | dir | 含 `java/lang/annotations.xml`（tracked），位置异常 |
| `local-rom-samples` | dir | 本地 ROM 输入目录（ignored，当前为空） |
| `scripts` | dir | 项目脚本 |
| `tasks` | dir | 任务文档 |
| `tools` | dir | 工具脚本与测试 |

### 异常/待确认项

| 路径 | 问题 |
|------|------|
| `java/lang/annotations.xml` | 位于仓库根 `java/lang/` 下，非标准源码树或 `.idea/externalAnnotations`，用途需确认。 |
| `app/lib/*.jar` | 大体积二进制依赖库，需确认是否应长期跟踪。 |

### `tasks` 目录

| 目录 | 文件数 | 状态 |
|------|--------|------|
| `tasks/active/` | 0 | 干净，无遗留 active 文件 |
| `tasks/completed/` | 29 | 历史任务完成文档 |

未发现同名 active/completed 重复。

### `docs` 目录

| 子目录 | 文件数 | 说明 |
|--------|--------|------|
| `docs/audit/` | 7 | 审计报告与基线 |
| `docs/rom-intelligence/` | 1 | ROM 过程矩阵 |

## 第六阶段：`.gitignore` 规则验证

使用 `git check-ignore -v --no-index` 测试结果：

| 候选路径 | 是否被忽略 | 匹配规则/来源 | 评估 |
|----------|-------------|---------------|------|
| `.idea/workspace.xml` | 是 | `.gitignore:12:/.idea/workspace.xml` | 正确 |
| `.idea/codeStyles/Project.xml` | 否 | — | 共享文件，未忽略，跟踪合理 |
| `.vscode/settings.json` | 是 | `.git/info/exclude:9:.vscode/` | 仅本地 exclude，应加入共享 `.gitignore` |
| `project.iml` | 是 | `.gitignore:1:*.iml` | 正确 |
| `local.properties` | 是 | `.gitignore:6:/local.properties` | 正确 |
| `keystore.properties` | 是 | `.gitignore:22:keystore.properties` | 正确 |
| `secrets.properties` | 是 | `.gitignore:27:secrets.properties` | 正确 |
| `app/build/.../apk/*.apk` | 是 | `app/.gitignore:1:/build` | 正确 |
| `build/rom-intelligence/sample.json` | 是 | `.gitignore:16:/build` | 正确 |
| `local-rom-samples/sample.apk` | 是 | `.gitignore:32:/local-rom-samples/` | 正确 |
| `tools/__pycache__/sample.pyc` | 是 | `.gitignore:30:__pycache__/` | 正确 |
| `app/release/*.apk` | 是 | `.gitignore:4:app/release/*` | 正确，但建议统一为 `app/release/` |
| `app/standalone/*.apk` | 是 | `.gitignore:5:app/standalone/*` | 正确，但建议统一为 `app/standalone/` |
| `app/develop/*.apk` | 是 | `.gitignore:20:app/develop/` | 正确 |
| `backup/foo.bak` | 是 | `.gitignore:28:backup/` | 正确 |
| `foo.log` | 是 | `.gitignore:8:*.log` | 正确 |
| `Thumbs.db` | 否 | — | 缺失规则 |
| `.DS_Store` | 是 | `.gitignore:15:.DS_Store` | 正确 |
| `.gradle/...` | 是 | `.gitignore:2:.gradle` | 正确 |
| `.kotlin/...` | 是 | `.gitignore:3:.kotlin/` | 正确 |
| `rom-target-diff.md` | 是 | `.gitignore:36:rom-target-diff.md` | 正确 |
| `java/lang/annotations.xml` | 否 | — | 共享 tracking，非 ignore 问题 |

### 建议新增/调整的 `.gitignore` 规则

| 建议 | 理由 |
|------|------|
| 新增 `.vscode/` | 当前仅在 `.git/info/exclude`，未共享 |
| 新增 `Thumbs.db` | Windows 缩略图缓存，当前未被忽略 |
| 新增 `desktop.ini` | Windows 目录配置文件 |
| 新增 `*.apk` 或 `/**/*.apk` | 当前根 APK 仅由 local exclude 忽略 |
| 考虑 `app/release/` 与 `app/standalone/` | 与 `app/develop/` 风格统一 |
| 考虑 `**/.shelf/`、`.idea/tasks.xml`、`.idea/usage.statistics.xml` | 用户级 IDE 文件 |

## 第七阶段：安全检查

### 敏感文件名命中

使用 `git ls-files` 命中以下路径（按文件名匹配 `secret|password|passwd|token|credential|private.?key|keystore|signing`）：

| 路径 | 实际内容判断 |
|------|--------------|
| `app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt` | Android 系统凭据验证 Activity，非硬编码 secret |
| `app/src/main/java/tv/withaibuild/customiuizer/CredentialsLauncher.kt` | 同上，启动入口 |
| `app/src/main/java/tv/withaibuild/customiuizer/CredentialsShortcut.kt` | 同上，快捷方式 |
| `app/src/main/res/drawable-xxhdpi-v4/ic_credentials.png` | 图标资源 |
| `scripts/check-signing-config.ps1` | 签名配置检查脚本 |

未读取任何文件内容，未发现 tracked `keystore.properties`、`*.jks`、`*.p12`、`secrets.properties`。

结论：不存在 `P0_TRACKED_SECRET_REVIEW_REQUIRED`。

## 建议删除项

本任务不执行删除。仅记录潜在删除候选：

| 候选 | 理由 | 风险 |
|------|------|------|
| 无明确删除项 | 未发现重复、过期或明确误跟踪文件 | — |

## 需要人工确认项

| 项 | 理由 |
|----|------|
| `java/lang/annotations.xml` | 位于 `java/lang/` 根目录，不是标准源码或 `.idea/externalAnnotations`，需确认用途。 |
| `app/lib/framework.jar` | 2.54 MiB 二进制库，需确认是否继续跟踪或替换为 Maven/版本化下载脚本。 |
| `app/lib/miui.jar` | 1.64 MiB 二进制库，同上。 |
| `app/lib/miuisystem.jar` | 856 KiB 二进制库，同上。 |
| `.gitignore` 调整建议 | 是否纳入 `.vscode/`、`Thumbs.db`、`desktop.ini`、`*.apk` 等规则。 |

## 最终决策

```text
MANUAL_REVIEW_REQUIRED
```

理由：

- 无 tracked secret 风险；
- 无明确误跟踪的构建产物；
- 共享 IDE 配置合理；
- 但存在 `java/lang/annotations.xml` 和 `app/lib/*.jar` 这两个需要人工判断用途与去留的项；
- `.gitignore` 调整也需人工决定是否纳入下一清理提交。

不批准自动执行 `CHORE-A13-REPOSITORY-HYGIENE-CLEANUP`，直到上述人工确认完成。

## 输出清单

- `docs/audit/A13_REPOSITORY_HYGIENE_INVENTORY.md`（本文件）
- 未新增任何 APK、AAB、DEX、smali 或反编译输出
- 未修改 `.gitignore`、生产源码、测试或构建配置
- 未删除任何文件
