# CustoMIUIzer A13 夜间持续架构审计与稳定优化指令

> Repository：`tomthenpc/customiuizer-a13`  
> 唯一工作分支：`devin/r13.2-kotlin-api102`  
> 运行目标：MIUI 14 / Android 13  
> 执行模式：夜间持续运行、稳定优先、无需对普通步骤逐项提问  
> 明确决定：本轮不升级 AGP、Gradle、Kotlin、JDK、compileSdk、targetSdk、minSdk 或依赖主版本

---

## 0. 本轮总目标

请接管并持续推进 CustoMIUIzer A13 的：

- 全仓架构审计；
- Java → Kotlin 迁移回归复核；
- 高频路径性能治理；
- 生命周期与内存治理；
- RemotePreferences 和配置链审计；
- SystemUI、Launcher、system_server 稳定性审计；
- ResourceHooks、ClassLoader、反射和 R8 审计；
- 设置应用、Locale、主题和 Fragment 生命周期审计；
- Gradle 配置治理；
- 测试、构建、APK 和 Xposed metadata 验证；
- 工程文档、AGENTS 和 checkpoint 同步。

以：

```text
https://github.com/tomthenpc/customiuizer-a13/tree/devin/r13.2-kotlin-api102
```

的最新真实状态作为唯一代码基线。

不得从 `main`、旧 Release、A14 分支、上游 tag 或旧 APK 覆盖当前开发分支。

---

## 1. 夜间持续运行模式

本任务按“无需对普通步骤逐项提问”的方式执行。

### 1.1 可以直接执行

以下操作无需询问：

- 读取源码、文档、Git 历史和构建配置；
- 搜索代码；
- 查看 diff；
- 创建和维护 `.devin/ACTIVE_TASK.md`；
- 编辑当前开发分支中的项目文件；
- 运行单元测试、Lint、Debug/Develop/Release 构建；
- 运行静态扫描、APK 分析和哈希计算；
- 使用当前已配置的正式签名；
- 按根因创建 commit；
- push 用户已授权的当前开发分支；
- 更新 checkpoint、审计文档和 changelog；
- 重试一次明确的瞬时失败命令；
- 在一个步骤被阻塞时继续执行其他不依赖步骤。

### 1.2 不因阻塞停止整夜任务

遇到以下情况，不询问、不原地等待、不反复重试：

#### 命令等待批准

如果终端显示：

```text
Command Awaiting Approval
```

则：

1. 将该命令、用途和当前状态记录到 `.devin/ACTIVE_TASK.md`；
2. 不把等待批准误报为代码卡死；
3. 不反复提交同一命令；
4. 继续所有不依赖该命令的读取、审计、编辑、测试设计和文档工作；
5. 最终报告中列为“环境审批阻塞”。

#### 正式签名缺失

如果 `../keystore.properties` 不存在、字段不完整或证书不可用：

1. 不询问用户；
2. 不伪造签名；
3. 不回退 Android Debug 证书冒充正式 Release；
4. 继续 Debug、单元测试、Lint、静态审计和不依赖正式签名的工作；
5. 将 Release、证书和安装验证列为阻塞项。

#### 设备不在线

如果 ADB、Root 或实机不可用：

1. 不询问用户；
2. 完成全部代码、构建、APK、R8 和静态验证；
3. 列出精确待实机矩阵；
4. 不把静态验证描述为实机通过。

#### 网络或依赖下载失败

1. 先检查是否已有本地缓存；
2. 对明确瞬时错误最多重试一次；
3. 仍失败则保存完整日志；
4. 继续其他不依赖网络的任务；
5. 不无限重试。

#### 单个构建任务失败

1. 保存完整日志和退出码；
2. 定位首个根因；
3. 做最小修复或记录阻塞；
4. 继续其他独立任务；
5. 不通过关闭 Lint、R8、resource shrink 或删除测试伪造成功。

### 1.3 只有以下操作禁止自动执行

即使处于夜间模式，也不得自动执行：

- `git reset --hard`
- `git restore .`
- `git checkout -- .`
- `git clean -fd`
- force push
- 修改、合并或强推 `main`
- 删除用户文件
- 清除应用数据或设备数据
- 创建或合并 PR
- 创建 tag
- 创建 GitHub Release
- 公开上传 APK
- 更换正式签名线
- 启用 Legacy Xposed API
- 改变用户可见产品行为但无法由证据判断预期
- 使用大范围 workaround 隐藏真实错误

遇到这些需求时，跳过并记录，不在夜间执行。

---

## 2. 任务连续性

持续维护：

```text
.devin/ACTIVE_TASK.md
```

该文件不提交 Git。

至少包含：

- 用户目标；
- Repository；
- 当前分支；
- 起始 HEAD；
- 当前 HEAD；
- tracking；
- 工作区状态；
- 已完成项；
- 正在执行项；
- 未完成项；
- 最后一条命令；
- 退出码；
- 日志路径；
- 已修改文件；
- 已生成产物；
- 已获得验证证据；
- 当前阻塞；
- 下一条精确动作；
- 禁止事项。

以下时机必须更新：

- 开始任务；
- 完成一个阶段；
- 修改实施路线；
- 命令失败；
- 命令被取消；
- 命令等待批准；
- 开始长构建；
- 结束长构建；
- 创建 commit；
- push；
- 任务收尾。

用户说“继续”“卡了吗”或会话恢复时：

1. 读取 `.devin/ACTIVE_TASK.md`；
2. 检查 branch、HEAD、tracking 和 `git status --short`；
3. 检查 `git diff --stat` 和相关完整 diff；
4. 检查残留进程、退出码和日志；
5. 从第一个未完成项继续；
6. 不重复询问已经给出的仓库、分支、目标和限制。

任务列表中的“完成”必须有以下至少一种证据：

- 工作区 diff；
- commit；
- 构建日志；
- 测试结果；
- APK；
- 哈希；
- 签名结果；
- 实机结果。

---

## 3. 当前固定工程边界

开始时必须从当前分支重新读取真实配置。

当前已知基线仅作为核对线索：

```text
JDK：17
Gradle Wrapper：8.9
AGP：8.7.2
Kotlin：2.0.21
compileSdk：36
minSdk：33
targetSdk：34
ABI：arm64-v8a
applicationId：tv.withaibuild.customiuizer.r13
namespace：name.monwf.customiuizer
Xposed minApiVersion：101
Xposed targetApiVersion：102
staticScope：false
```

### 3.1 本轮明确不升级

不得升级或修改版本：

- JDK；
- Java sourceCompatibility；
- Java targetCompatibility；
- Gradle Wrapper；
- Android Gradle Plugin；
- Kotlin；
- compileSdk；
- minSdk；
- targetSdk；
- Build Tools 主版本；
- libxposed API；
- libxposed service；
- AndroidX；
- commons-lang3；
- coroutines；
- JUnit；
- 其他依赖版本。

不得复制 A14 的：

```text
Gradle 9.x
AGP 9.x
Kotlin 2.3.x
compileSdk 37
libxposed 102.0.0
coroutines 1.11.x
```

A14 曾尝试将 AGP 9.2.1 升级到 9.3.1，但 Lint 阶段出现兼容性问题并回退。A13 当前仍在 AGP 8.7.2，跨越式升级风险更高，因此本轮完全取消工具链版本升级。

稳定性优先于版本数字。

### 3.2 允许的构建配置治理

不改变版本的前提下，可以审计并最小修改：

```properties
org.gradle.unsafe.configuration-cache=true
android.enableResourceOptimizations=true
```

处理原则：

1. 先确认当前 Gradle/AGP 是否识别；
2. 已废弃或无效时删除或替换；
3. 可以使用：
   ```properties
   org.gradle.configuration-cache=true
   org.gradle.caching=true
   ```
4. 不启用：
   ```properties
   org.gradle.parallel=true
   org.gradle.configuration-cache.parallel=true
   ```
5. 不提高 JVM 内存上限，除非有明确 OOM 证据；
6. 不写入本机绝对路径；
7. 不提交 SDK、JDK 或 keystore 路径；
8. configuration cache 不兼容时，优先定位项目自身问题；
9. 不为配置缓存大规模重写 Gradle 架构；
10. 无法低风险兼容时，可以关闭 configuration cache，但保留经过验证的 build cache。

这属于配置治理，不属于工具链版本升级。

---

## 4. 项目血统与参考边界

### 4.1 产品与直接上游

原始产品思想来自 Mikanoshi。

A13 主要功能上游：

```text
https://github.com/MonwF/customiuizer
```

Android 13 / MIUI 14 主要语义基线：

```text
v23.11.26
```

上游仅用于确认：

- 功能原意；
- Hook target；
- before/after 行为；
- preference key；
- 用户可见语义；
- MIUI 14 兼容分支；
- 原始错误处理。

不得用上游覆盖当前独立实现。

### 4.2 A14 参考边界

可参考：

```text
tomthenpc/customiuizer-a14
devin/r14.13-kotlin-refactor
```

只吸收经过 A13 代码验证的通用方法：

- Java → Kotlin 控制流审计；
- `break` / `continue` 回归；
- Regex 退化；
- primitive key boxing；
- Sparse 容器安全发布；
- WeakReference 生命周期治理；
- Resource Hook miss path；
- RemotePreferences 空快照；
- listener 注册状态；
- Gradle 废弃属性；
- 架构审计和任务连续性。

禁止复制：

- Android 14 版本闸门；
- HyperOS 类名；
- A14 Hook target；
- A14 package/applicationId/namespace；
- A14 Manifest；
- A14 SystemUI tag 结构；
- A14 资源名；
- A14 工具链版本；
- A14 签名和 Release 配置。

---

## 5. 审计原则

优先级：

1. 可构建、可安装、可运行；
2. 功能和 Hook 行为正确；
3. SystemUI、Launcher、system_server 稳定；
4. Android 13 / MIUI 14 兼容；
5. API 101 最低运行边界；
6. JVM、ClassLoader、反射、R8 和动态入口兼容；
7. 生命周期和资源安全；
8. 性能、内存和功耗；
9. 可维护性；
10. Kotlin 覆盖率和代码简短。

核心成本模型：

```text
调用频率 × 单次成本 × 进程数量 × 存活时间 × 故障影响
```

核心方向：

> 功能关闭时接近零成本；功能开启时只响应真实事件；高频路径无不必要分配、Regex、装箱、反射、I/O、Binder 和阻塞；兼容逻辑限制在明确边界。

不得把以下内容直接判定为缺陷：

- Java 文件仍存在；
- 代码不够函数式；
- 日志使用 error 级别；
- A14 实现不同；
- 理论上可能更慢；
- 文件行数较多；
- 新版本工具链存在。

先建立具体调用链和证据。

---

## 6. Java → Kotlin 迁移审计

审计所有由 Java 迁移而来的 Kotlin 文件。

先定位迁移 commit：

```powershell
git log --follow -- app/src/main/java/<path>.kt
```

检查迁移前 Java：

```powershell
git show "<迁移commit>^:app/src/main/java/<path>.java" |
    Select-String -Pattern '\bbreak\b|\bcontinue\b' -Context 6,1
```

必须核对：

- loop 中的 `break`；
- loop 中的 `continue`；
- 提前 return；
- `switch` → `when`；
- `try/finally`；
- `synchronized`；
- `volatile`；
- static 初始化；
- nullability；
- overload；
- primitive/boxed 类型；
- Java 调用点；
- 反射入口；
- R8 可达性。

规则：

1. Java `switch` 中的 `break` 在 Kotlin `when` 中消失属于正常；
2. 循环中的 `break` / `continue` 消失，先按回归处理；
3. 不用 `use {}`、`forEach {}` 隐藏循环退出；
4. Java `split("\\|")` 不得改成 `split("\\|".toRegex())`；
5. `first|second` 第一段比较优先使用 `indexOf`、`regionMatches` 或 `PrefPair`；
6. 热路径 `Map<Int, *>` / `Map<Long, *>` 评估 Sparse 容器；
7. Sparse 容器跨线程读取必须安全发布；
8. 不无证据删除同步或线程可见性；
9. 不用集合链、Sequence、`runCatching` 隐藏 Hook 热路径；
10. Kotlin object/companion 必须核对 Java 静态调用和初始化。

Kotlin 化目标不是统计比例。

稳定 Java 边界可以保留。

---

## 7. A13 优先审计链路

### 7.1 MainModule 与 RemotePreferences

检查：

- Provider 未就绪时空快照是否永久标记为已加载；
- `prefsLoaded` 是否只在有效快照后设置；
- listener 注册失败时是否提前设置状态；
- listener 是否重复注册；
- 无关进程是否加载配置；
- key 前缀归一化；
- StringSet 安全复制；
- 配置全量复制；
- 变更增量更新；
- Android/API gate 是否足够早。

### 7.2 ResourceHooks.java

检查：

- Hook 是否按需安装；
- miss path 是否读取 Context；
- miss path 是否读取 `Executable.name`；
- 是否每次解析 package/type/entry；
- resource ID 是否装箱；
- fake/replacement 容器线程安全；
- copy-on-write 和 volatile 发布；
- wildcard；
- ID/DENSITY/OBJECT；
- Float → Int；
- 缓存失效；
- 无关进程；
- `Chain.proceed()` 次数；
- A13 Java 行为是否完整保留。

ResourceHooks 修改必须独立 commit。

### 7.3 SystemUI

检查：

- 静态 View 强引用；
- 主题、旋转、密度、reinflate 后旧 View；
- WeakReference 空条目清理；
- Handler 周期任务；
- 息屏后周期唤醒；
- Receiver/Observer owner；
- 重复注册；
- 左右侧；
- 双排；
- TextIcon tag；
- SystemUI 重启；
- 异常日志刷屏。

SystemUI 生命周期修改必须独立 commit。

### 7.4 ClassLoader 与反射

检查：

- `XposedHelpers.java`；
- class/field/method/constructor 缓存；
- 负缓存；
- fallback ClassLoader 重复反射；
- 临时 ClassLoader 强引用；
- DexKit close；
- Android 13 ClassLoader；
- R8 keep；
- 字符串动态入口。

### 7.5 设置应用

检查：

- Locale；
- Configuration 污染；
- 重复语言入口；
- Activity recreate；
- Fragment Handler/Runnable；
- Receiver 成对注销；
- BT/WiFi/AppSelector；
- 日夜主题；
- 搜索；
- 返回栈；
- Preference summary；
- 多语言 fallback；
- resource shrink。

### 7.6 构建和 Xposed 边界

检查真实解析结果：

- AGP；
- Kotlin；
- Gradle；
- compileSdk；
- libxposed API artifact；
- libxposed service artifact；
- metadata target 102；
- API 101 公共路径；
- API 102 类型；
- Legacy API；
- R8；
- resource shrink；
- applicationId；
- namespace；
- ABI；
- v2 signing；
- zipalign；
- Xposed entry；
- scope。

如果 metadata 表达 API 102，但依赖实际仍为 API/service 101：

1. 记录事实；
2. 检查是否存在真实不一致；
3. 修正文档；
4. 本轮不升级 libxposed 依赖；
5. 不把 metadata target 102 自动解释为“使用 API 102 artifact 编译”。

---

## 8. 分阶段执行

### Phase 0：恢复事实

- branch；
- HEAD；
- tracking；
- remote；
- working tree；
- AGENTS；
- checkpoint；
- 构建配置；
- 签名可用性；
- ACTIVE_TASK。

### Phase 1：绿色基线

在修改代码前运行当前版本基线：

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon :app:test
.\gradlew.bat --no-daemon :app:lint
.\gradlew.bat --no-daemon :app:assembleDebug
```

签名可用时追加：

```powershell
.\gradlew.bat --no-daemon :app:assembleDevelop
.\gradlew.bat --no-daemon :app:assembleRelease
```

保存完整日志。

基线失败不等于停止审计。

### Phase 2：架构地图

建立：

```text
docs/ARCHITECTURE_AUDIT_A13.md
```

包括：

- 加载链；
- 进程分发；
- Hook 注册；
- 配置链；
- 生命周期；
- 高频路径；
- 设置应用；
- 构建链；
- API 101/102 实际边界。

### Phase 3：迁移回归自动审计

搜索：

```text
split("\\|".toRegex())
.toRegex()
forEach
use {
runCatching
Map<Int,
Map<Long,
Pair<
Triple<
Sequence
@Volatile
synchronized
WeakReference
ArrayList<View
Handler(
postDelayed
registerReceiver
registerContentObserver
```

对关键 Kotlin 文件与原 Java 比对。

### Phase 4：问题排序

每项标注：

- P0/P1/P2/P3；
- 证据；
- 影响进程；
- 触发频率；
- 生命周期；
- 修复风险；
- 验证方式；
- 已验证；
- 代码确认；
- 待实机；
- 候选；
- 不实施。

### Phase 5：低风险修复

优先：

- 明确控制流回归；
- 明确 Regex 退化；
- 明确状态提前置位；
- 明确监听重复；
- 明确静态 View 泄漏；
- 明确废弃 Gradle 配置；
- 有单元测试覆盖的工具函数。

### Phase 6：高风险基础设施

独立处理：

- ResourceHooks；
- XposedHelpers；
- ClassLoader；
- 配置模型；
- Hook runtime；
- package/FQCN。

一个根因一个 commit。

### Phase 7：构建配置治理

仅处理属性和兼容配置。

不升级任何版本。

验证 configuration cache 时连续运行：

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug
.\gradlew.bat --no-daemon :app:assembleDebug
```

记录是否出现：

```text
Reusing configuration cache
```

同时记录：

- `UP-TO-DATE`；
- `FROM-CACHE`；
- 实际执行 task；
- 配置缓存问题。

不强制追求配置缓存；稳定优先。

### Phase 8：完整验证

按项目实际任务运行：

```powershell
.\gradlew.bat --no-daemon clean
.\gradlew.bat --no-daemon :app:test
.\gradlew.bat --no-daemon :app:lint
.\gradlew.bat --no-daemon :app:lintRelease
.\gradlew.bat --no-daemon :app:lintVitalRelease
.\gradlew.bat --no-daemon :app:assembleDebug
.\gradlew.bat --no-daemon :app:assembleDevelop
.\gradlew.bat --no-daemon :app:assembleRelease
```

不存在的任务如实记录，不伪造。

如果可用，执行：

```powershell
.\gradlew.bat --no-daemon :app:analyzeReleaseR8Config
```

### Phase 9：APK 审计

检查：

- 文件名；
- 大小；
- SHA-256；
- applicationId；
- versionCode；
- versionName；
- minSdk；
- targetSdk；
- compileSdk；
- ABI；
- v1/v2；
- certificate SHA-256；
- zipalign；
- DEX；
- native libraries；
- Manifest；
- Xposed metadata；
- scope；
- entry；
- framework.jar 未打包；
- compileOnly API 未重复打包；
- dynamic resources 未误删。

### Phase 10：文档与 Git

更新：

- `AGENTS.md`；
- `docs/ARCHITECTURE_AUDIT_A13.md`；
- `docs/DEVIN_A13_CHECKPOINT.md`；
- `docs/A13_REFACTOR_PLAN.md`；
- `docs/A13_REFACTOR_PROGRESS.md`；
- `docs/VERIFICATION.md`；
- `CHANGELOG.md`。

只创建确实需要长期维护的文档。

---

## 9. 构建性能记录

本轮不做不同 AGP 版本的 A/B 对照。

只记录当前工具链：

### 冷构建

```powershell
.\gradlew.bat --stop
.\gradlew.bat clean
.\gradlew.bat --no-daemon :app:assembleDebug --profile
```

### 热构建

不修改源码，连续三次：

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug --profile
.\gradlew.bat --no-daemon :app:assembleDebug --profile
.\gradlew.bat --no-daemon :app:assembleDebug --profile
```

记录：

- 总耗时；
- configuration 时间；
- task execution；
- configuration cache；
- UP-TO-DATE；
- FROM-CACHE；
- profile 路径。

不得因为没有旧版本严格 A/B 对照而声称提升百分比。

---

## 10. 提交拆分

建议：

```text
docs: establish A13 architecture audit baseline
fix: correct Kotlin migration control-flow regressions
perf: remove regex pair parsing from hot paths
fix: correct remote preference loading and watcher state
fix: avoid retaining stale SystemUI views
perf: reduce A13 resource hook miss-path overhead
perf: cache safe ClassLoader fallback results
build: modernize compatible Gradle cache settings
refactor: correct locale and settings lifecycle handling
docs: record A13 verification and remaining device tests
chore: bump version and update changelog
```

规则：

- 只提交成功方案；
- 不提交失败的工具链升级试验；
- ResourceHooks 独立；
- SystemUI 生命周期独立；
- 构建配置独立；
- 格式化和行为修改分开；
- 版本号最后；
- 每个 commit 可回退；
- push 后核对远端 HEAD。

---

## 11. 最低实机矩阵

设备可用时验证：

- 安装精确 APK；
- APK SHA-256 与报告一致；
- 应用启动；
- 首页；
- About；
- Locale；
- 浅色/暗色；
- 搜索和返回；
- LSPosed 识别；
- SystemUI 加载；
- Launcher 加载；
- Settings 加载；
- system_server；
- RemotePreferences；
- 状态栏文本图标；
- 控制中心；
- 锁屏；
- 资源替换；
- SystemUI 重启；
- 横竖屏；
- 主题变化；
- 无 `NoClassDefFoundError`；
- 无 `VerifyError`；
- 无 `NoSuchMethodError`；
- 无持续异常刷屏；
- 无启动循环或 ANR。

设备不可用时不停止其他工作。

---

## 12. 最终报告

```markdown
# CustoMIUIzer A13 夜间审计结果

## 基线

- Repository：
- Branch：
- Start HEAD：
- End HEAD：
- Remote HEAD：
- Ahead/behind：
- Working tree：

## 夜间执行

- 开始时间：
- 结束时间：
- 等待批准命令：
- 环境阻塞：
- 已绕开阻塞继续完成：

## 当前工具链

- JDK：
- Java source/target：
- Gradle：
- AGP：
- Kotlin：
- compileSdk：
- minSdk：
- targetSdk：
- libxposed API resolved：
- libxposed service resolved：

## 明确未升级

- JDK：未升级
- Gradle：未升级
- AGP：未升级
- Kotlin：未升级
- compileSdk：未升级
- libxposed：未升级
- 依赖：未升级

## 架构地图

- 加载链：
- 配置链：
- 生命周期：
- 高频路径：
- 构建链：
- API 101/102 实际边界：

## 问题

| 优先级 | 问题 | 证据 | 影响 | 状态 |
| --- | --- | --- | --- | --- |

## 修改

- ...

## Kotlin/Java

- 保留 Java：
- 修复 Kotlin：
- JVM/R8 影响：

## 性能

- 已测量：
- 机制确认：
- 待测量：
- 未宣称：

## 构建

- test：
- lint：
- lintRelease：
- lintVitalRelease：
- assembleDebug：
- assembleDevelop：
- assembleRelease：
- R8：
- resource shrink：
- configuration cache：
- build cache：

## APK

- 文件：
- 大小：
- SHA-256：
- applicationId：
- versionCode：
- versionName：
- SDK：
- ABI：
- zipalign：
- signing：
- certificate SHA-256：

## Xposed

- minApiVersion：
- targetApiVersion：
- resolved API artifact：
- resolved service artifact：
- staticScope：
- entry：
- scope：
- Legacy API：
- API 101 公共路径：
- API 102 状态：

## 实机

- 已验证：
- 待实机：
- 无法确认：

## Git

- commits：
- push：
- PR：未创建
- merge：未执行
- tag：未创建
- Release：未创建

## 文档

- AGENTS：
- ACTIVE_TASK：
- checkpoint：
- architecture audit：
- progress：
- verification：
- changelog：

## 剩余事项

- ...
```

最终必须区分：

- 已验证；
- 代码层面确认；
- 待实机；
- 环境阻塞；
- 无法确认。

---

## 13. 最终目标

将：

```text
devin/r13.2-kotlin-api102
```

持续发展为 A13 当前最可靠、最稳定、最高性能、最清晰的代码线。

要求：

- 不升级工具链版本；
- 不机械复制 A14；
- 不牺牲 Android 13 / MIUI 14；
- 不污染 API 101 公共路径；
- 不为 Kotlin 覆盖率迁移稳定 Java；
- 不用抽象隐藏运行成本；
- 不用 warning 和理论风险制造无价值修改；
- 不因单个命令失败或等待批准停止整夜任务；
- 不询问普通执行步骤；
- 不伪造成功；
- 所有修改有证据、验证和可回退提交；
- 最终留下准确的代码、构建产物、日志和工程文档。
