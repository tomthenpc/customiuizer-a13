# A13 执行手册

> 本手册是 Claude、Devin 及其他模型在 `tomthenpc/customiuizer-a13` 仓库执行任务的统一流程。
> 优先级：`AGENTS.md` > 本手册 > 当前任务文档 > 源码与验证证据 > 其他文档 > Git 历史。

---

## 一、开始任务

每次工作前必须执行：

```powershell
git rev-parse --show-toplevel
git status --short
git branch --show-current
git rev-parse HEAD
git log -5 --oneline
git diff --stat
git diff --check
git remote -v
```

记录：
- 仓库、分支、HEAD、工作区改动、未跟踪文件。
- 当前是否有等待审批或被取消的命令。
- `java` / `gradle` / `python` 进程是否异常（见“卡死判断”）。

若发现 `.devin/ACTIVE_TASK.md`，读取后从第一个未完成动作继续。

---

## 二、修改前证据

对任何代码改动，先完成：

1. **找到真实调用链**
   - 在 `MainModule.java` / `MainModule` 相关文件中搜索被调用符号。
   - 搜索反射、DexKit、字符串类名、XML 引用、R8 keep 规则。

2. **确认目标进程**
   - `onSystemServerStarting`：作用域为 `system_server`。
   - `onPackageReady`：按包名分支，入口前进行进程排除。
   - 设置应用进程：`MainActivity`、`MainFragment`、`subs/*`。

3. **确认功能开关**
   - 找到对应的 `preference key` 与默认值。
   - 确认功能关闭时是否接近零成本（不注册 Hook、不初始化无关资源）。

4. **确认生命周期**
   - Hook、Receiver、Observer、Listener、Callback、Handler、Coroutine 必须有明确 owner。
   - 必须有可重复调用的释放/反注册路径。

5. **对照迁移前 Java**
   - 对 Java→Kotlin 迁移文件：
     ```powershell
     git log --follow -- app/src/main/java/<path>.kt
     git show "<迁移commit>^:app/src/main/java/<path>.java" |
         Select-String -Pattern '\bbreak\b|\bcontinue\b' -Context 6,1
     ```
   - 核对 `break` / `continue` / `return` / 循环 / `switch` → `when`。

6. **对照 A13 上游语义**
   - 上游：`MonwF/customiuizer v23.11.26`。
   - 只确认原意，不机械复制。

7. **只把 A14 用作方法参考**
   - 不复制 A14 Android 14 类名、Hook target、包名、资源、Manifest。
   - 不引用 A14 `tv.withaibuild.customiuizer.r14` applicationId。

8. **记录改动前行为和风险**
   - 在 `.devin/ACTIVE_TASK.md` 或相关文档中写下“修改前、修改后、风险、验证”。

---

## 三、修改流程

```text
证据
→ 最小修改
→ 定向编译
→ 定向测试
→ 静态门禁
→ 全量测试
→ lint
→ debug/release
→ R8 核对
→ 实机验证
→ 文档同步
→ 单独提交
```

每完成一个阶段更新 `.devin/ACTIVE_TASK.md`。

---

## 四、最小修改原则

- 不合并多个迁移阶段。
- 不引入无关依赖或构建升级。
- 不为了行数短而牺牲性能、控制流可读性或异常边界。
- 不修改 Hook target、注册顺序、before/after 语义、`Chain.proceed()` 次数，除非任务明确要求且有验证覆盖。
- 不改变 `MainModule` 有序调用序列。

---

## 五、编译与验证命令

A13 当前可用 Gradle 任务（以实际 `gradlew.bat tasks` 输出为准）：

```powershell
.\gradlew.bat :app:compileDebugKotlin --stacktrace
.\gradlew.bat :app:testDebugUnitTest --stacktrace
.\gradlew.bat :app:lintDebug --stacktrace
.\gradlew.bat :app:assembleDebug --stacktrace
.\gradlew.bat :app:assembleRelease --stacktrace
.\gradlew.bat :app:lintVitalRelease --stacktrace
```

若某任务不存在，记录真实可用替代项，禁止伪造已执行结果。

---

## 六、命令失败处理

- 只处理第一根因。
- 不连续叠加猜测性修改。
- 命令被取消不等于代码失败；取消后先检查工作区状态再继续。
- `Model provider unreachable` 不等于工作区损坏。
- 不重新执行已经有可靠证据通过的阶段。
- 从 `.devin/ACTIVE_TASK.md` 的第一个未完成动作继续。

---

## 七、卡死判断

遇到响应缓慢或怀疑卡死时，先查：

```powershell
Get-Process java, gradle, python -ErrorAction SilentlyContinue
Get-CimInstance Win32_Process |
    Where-Object { $_.Name -match 'java|gradle|python' } |
    Select-Object ProcessId, Name, CommandLine
```

同时检查：
- 命令是否等待批准；
- 终端是否已被取消；
- Gradle daemon 是否仍在运行；
- 输出文件是否仍在变化；
- CPU 是否有活动；
- 是否只是模型服务不可用。

---

## 八、长任务续接

每完成一个阶段更新 `.devin/ACTIVE_TASK.md`：

- 当前 HEAD
- 工作区改动摘要
- 最近一次成功/失败命令
- 下一未完成动作

不要等到上下文快满才记录。

---

## 九、文档同步

涉及以下内容的改动必须同步更新 `AGENTS.md`、本手册或相关 `docs/`：

- 新 Hook 注册模式
- 新生命周期/所有权模式
- 新工具或静态门禁规则
- 新验证证据
- R8 / Manifest / 资源入口变化

---

## 十、提交前检查

```powershell
git diff --check
git status --short
git diff --stat
python tools/check-invariants.py      # 工具就位后
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

- 不提交 `keystore.properties`、APK、签名备份、日志、`.devin/ACTIVE_TASK.md` 等本地文件。
- 文档改动不重新生成已验证的 APK。
- 每个迁移阶段单独提交，提交信息写清阶段、入口数量与验证证据。
