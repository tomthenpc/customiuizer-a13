# A13 拆分与迁移方法

> 本文件记录 K3、K4、K5、K6 的大文件拆分与 Java → Kotlin 迁移方法。
> 核心原则：**文件拆分本身不应改变运行时行为。** 拆分是否安全取决于成员文本、入口映射和有序调用序列，而不是“看起来差不多”。

---

## 一、拆分前测量

对任何超过 1000 行的 `mods/*.java` / `mods/*.kt` 对象，先量化：

1. **public 入口数量**：被 `MainModule` 或其他外部 Java/Kotlin 调用的方法数量。
2. **private helper 数量**：仅被同域函数调用的私有方法数量。
3. **跨域调用**：某个成员是否调用另一个明显属于不同功能的成员。
4. **共享字段**：`static` / companion object 字段被哪些功能域使用。
5. **静态初始化**：`object { init { } }` / `static { }` 的初始化顺序是否依赖成员布局。
6. **Java 调用点**：`MainModule.java` 通过 `System.XXX` 调用时，方法签名和 JVM 可见性。
7. **反射入口**：DexKit、字符串类名、`getIdentifier` 等动态访问点。
8. **R8 keep**：`proguard-rules.pro` 中 `-keep` 是否依赖对象名或包名。
9. **Hook 注册顺序**：`MainModule` 中调用顺序即注册顺序。
10. **`Chain.proceed()` 次数**：`before` / `intercept` / `after` 中的 `proceed` / `setResult` / `returnAndSkip` 次数。

使用命令：

```powershell
Select-String -Path app\src\main\java\name\monwf\customiuizer\MainModule.java -Pattern "System\.([A-Za-z0-9_]+)" -AllMatches | ForEach-Object { $_.Matches | ForEach-Object { $_.Groups[1].Value } } | Sort-Object -Unique
```

---

## 二、功能域判断

只有满足以下条件，成员才能独立拆分到新文件：

1. 域内 helper 不被其他域调用。
2. 共享状态可以明确归属到一个 owner 或注册表。
3. 入口调用可以机械映射：`System.XXX` → `System<Domain>Hooks.XXX`。
4. 不改变静态初始化顺序（`System` object 的初始化在新 object 中被同顺序调用时不改变初始化时机）。
5. 不改变 JVM 签名（方法名、参数类型、返回类型、可见性、`@JvmStatic` 等）。
6. 不改变注册顺序（`MainModule` 调用顺序保持不变）。
7. 不改变 `before` / `after` / `intercept` / `Chain.proceed()` 次数。

若任一条件不满足，不得拆分。

---

## 三、迁移证明

每个 Java → Kotlin 迁移成员必须：

1. **与 Java 原实现逐段比对**
   - 使用：
     ```powershell
     git show "<迁移commit>^:app/src/main/java/<path>.java"
     ```
   - 对未提交的迁移，用：
     ```powershell
     git diff HEAD app/src/main/java/name/monwf/customiuizer/mods/<file>.java
     ```

2. **核对 `break` / `continue`**
   ```powershell
   git show "<迁移commit>^:app/src/main/java/<path>.java" |
       Select-String -Pattern '\bbreak\b|\bcontinue\b' -Context 6,1
   ```
   - Java `switch` 中的 `break` 在 Kotlin `when` 中消失正常。
   - 循环体内的 `break` / `continue` 消失一律先按回归处理。

3. **核对 primitive / boxed 类型**
   - `Int::class.javaPrimitiveType` 与 `Integer::class.java` 不混用。
   - 反射调用 `callMethod` 参数类型匹配目标方法签名。

4. **核对 `@JvmStatic`、`@JvmField`**
   - 被 Java `MainModule` 调用的 Kotlin `object` 方法必须加 `@JvmStatic`。
   - 需要与 Java 共享的静态字段加 `@JvmField`。

5. **核对异常边界**
   - `after` / `before` / `intercept` 中不吞异常、不空返回伪造成功。
   - 注册到 framework 的回调必须 `ModuleHelper.guarded`（见 `A13_RUNTIME_INVARIANTS.md`）。

6. **核对同步和 `volatile`**
   - 迁移后字段的线程可见性不得下降。
   - `SparseArray` / `SparseIntArray` 跨线程读取必须有安全发布。

7. **核对 Hook target**
   - 类名、方法名、参数类型、Hook 类型（`findAndHookMethod` / `hookAllMethods` / `hookAllConstructors`）不变。

8. **核对参数读写**
   - 只读参数不用 `getArgsArray` + `proceed(args)`，优先 `chain.getArg(i)` + `chain.proceed()`。
   - 需要修改参数时才使用数组。

9. **核对 `result` 和提前返回**
   - `setResult` / `returnAndSkip` 在 Kotlin 中语义不变。
   - `before` 中 `returnAndSkip(null)` 与 Java 中 `param.returnAndSkip(null)` 一致。

---

## 四、facade 规则

`System.kt` facade 只能：

1. 保存兼容入口。
2. 转发到唯一实现。
3. 保持 JVM 调用方式。
4. 保持入口顺序。

`System.kt` facade 不得：

1. 复制实现。
2. 重新读取偏好。
3. 新建注册表。
4. 动态扫描。
5. 通过反射路由。
6. 引入额外对象。
7. 隐藏异常。
8. 重复注册 Hook。

### facade 生成流程

1. 从 `MainModule.java` 提取所有 `System.XXX` 调用，建立入口清单。
2. 扫描 `System*.kt` 文件，建立 `XXX → System<Domain>Hooks` 映射。
3. 检测重复入口、漏入口、参数类型不匹配。
4. 输出映射表供人工审查。
5. 生成 `System.kt`（仅 `@JvmStatic fun XXX(...) { System<Domain>Hooks.XXX(...) }`）。
6. 比对 `MainModule.java` 前后有序调用序列：
   - 方法名序列不变；
   - 仅接收者从 `System` 变为 `System<Domain>Hooks`。
7. 编译 `compileDebugKotlin`。
8. 确认无重复 JVM 签名、无重复 Hook 注册。
9. 最后才删除 `System.java`。

---

## 五、工具使用

A14 工具需按 A13 路径和包名改写后才能使用：

| 工具 | A13 适配状态 | 说明 |
| --- | --- | --- |
| `tools/split-hook-domain.py` | 需修改 | 包名改为 `name.monwf.customiuizer.mods`；`MainModule` 改为 `MainModule.java`；路径分隔符改为 `/` 或按 Windows 处理 |
| `tools/repoint-hook-calls.py` | 需修改 | 包名与接收者类型映射改为 A13 |
| `tools/check-invariants.py` | 需新增或移植 | 优先把 A14 规则适配为 A13 规则 |
| `tools/analyze_lsposed_log.py` | 需新增 `a13` profile | applicationId 改为 `tv.withaibuild.customiuizer.r13` |
| `tools/capture-memory-baseline.ps1` | 需移植 | ADB 目标包名和进程名改为 A13 |
| `tools/compare-memory-baseline.py` | 可直接使用 | 输入输出路径按 A13 改 |

所有工具必须先具备：
- `--dry-run`
- 修改前输出计划
- 修改后做机械校验
- 失败时不写入任何文件
- 不自动删除 Java 文件
- 脚本本身可审查和可复现

---

## 六、删除 `System.java` 的条件

必须同时满足：

1. `MainModule.java` 已完整移除 `import name.monwf.customiuizer.mods.System` 中未使用的部分（保留 facade `System` 导入）。
2. `MainModule.java` 中所有 `System.XXX` 调用已重定向到 `System<Domain>Hooks.XXX`，或者仍调用 `System.XXX` 但 facade 正确转发。
3. `System.kt` facade 编译通过。
4. `compileDebugKotlin` / `testDebugUnitTest` / `lintDebug` 全绿。
5. 调用序列校验：重定向前后 `MainModule` 方法名序列一致。
6. 其他 Java/Kotlin 文件无 `System.*` 引用（除 `MainModule` 的 `System.XXX`）。
7. R8 保留方法数与迁移前一致（允许 `access$` 桥接差异）。
8. 生成 `git diff --stat`，确认 `System.java` 删除为纯删除，`MainModule` 仅接收者变化。

---

## 七、回滚纪律

- 禁止 `git reset --hard`、`git restore .`、`git checkout -- .`、`git clean -fd`。
- 删除 `System.java` 后若发现调用遗漏，优先补全 `System.kt` facade 或对应 `System*Hooks`，不恢复 `System.java`。
- 重大回滚需用户明确授权。
