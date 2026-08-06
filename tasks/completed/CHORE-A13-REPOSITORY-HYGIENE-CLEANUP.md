# CHORE-A13-REPOSITORY-HYGIENE-CLEANUP — Completed

## 元数据

- Base SHA: `c2390c391ca5c69851c68f191797d4695b67e387`
- Implementation SHA: `74de73f41eb6be41a06b70dadd15edec1e55227d`
- Branch: `devin/a13-rom-intelligence-audit`

## 改动摘要

- 删除 `app/lib/miui.jar`（构建/脚本中无引用）。
- 删除 `java/lang/annotations.xml`（构建/脚本/IDE 配置中无引用）。
- 规范化 `.gitignore`：
  - 新增 `.vscode/`、`Thumbs.db`、`desktop.ini`。
  - 新增 `*.apk`、`* .aab`、`* .apks`。
  - 新增 `/out/`、`.cxx/`、`/k7_window_dump.xml`。
  - 统一 `app/release/`、`app/develop/`、`app/standalone/` 规则。
  - 移除冗余 `/build/rom-intelligence/`（已被 `/build/` 覆盖）。
- 保留 `app/lib/framework.jar`、`app/lib/miuisystem.jar`、`gradle/wrapper/gradle-wrapper.jar`。
- 保留 8 个共享 `.idea/` 配置文件。
- 未修改生产源码、测试、版本号或发布配置。

## 验证

| 验证项 | 命令 | 结果 |
|--------|------|------|
| `miui.jar` 引用 | `git grep -F "miui.jar"` | 无命中（除审计文档外） |
| `framework.jar` / `miuisystem.jar` 引用 | `git grep -E 'lib/(framework\|miuisystem)\.jar' -- *.gradle.kts` | `app/build.gradle.kts:185-186` 命中 |
| `annotations.xml` 引用 | `git grep -E "java/lang/annotations\.xml\|externalAnnotations"` | 无命中（除审计文档外） |
| 编译 | `gradlew :app:compileDebugKotlin --rerun-tasks` | BUILD SUCCESSFUL |
| 编译 | `gradlew :app:compileDebugJavaWithJavac` | BUILD SUCCESSFUL |
| 测试 | `gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL |
| 测试 | `gradlew :app:lintDebug` | BUILD SUCCESSFUL |
| 依赖校验 | `gradlew :app:testDebugUnitTest --dependency-verification=strict` | BUILD SUCCESSFUL |
| Python | `python -m compileall tools` | 通过 |
| Python | `python -m unittest discover -s tools/tests -p "test_*.py"` | 902 通过（2 skipped） |
| diff 检查 | `git diff --check` | 无报错 |
| APK/AAB/APKS | `Get-ChildItem . -Recurse -File -Include *.apk,*.aab,*.apks` | 未新增/更新时间 |

## 状态

`STATIC_VERIFIED`
