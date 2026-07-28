# CustoMIUIzer A13：A14 Claude 审计改动适配任务

> 目标仓库：`tomthenpc/customiuizer-a13`  
> 目标基线分支：`devin/r13.2-kotlin-api102`  
> 参考仓库：`tomthenpc/customiuizer-a14`  
> 参考分支：`devin/r14.13-kotlin-refactor`  
> 参考增量：从 `58b21260400a4bd0f0b505589461d5f735ac36f5` 到参考分支当前 HEAD  
> 建议目标版本：`r13.2.3-devin`  
> 适用系统：MIUI 14 / Android 13  
> libxposed 边界：`minApiVersion=101`、`targetApiVersion=102`

---

## 1. 任务目标

将 A14 分支经过 Claude 架构审计后确认有效的通用修复，按 A13 的真实代码结构移植到：

```text
tomthenpc/customiuizer-a13
└── devin/r13.2-kotlin-api102
```

本任务不是同步 A14 全量代码，也不是将 A13 强行改造成 A14 的工程结构。

必须遵守以下原则：

> 功能关闭时接近零成本；功能开启时只响应真实事件；高频路径无不必要分配、无重复反射、无阻塞；兼容代码限制在边界内。

同时必须坚持安全优先：

- 不改变现有功能逻辑；
- 不改变 Android 13 / MIUI 14 适配边界；
- 不机械复制 Android 14 的 Hook 类名、方法签名或资源结构；
- 每一项优化必须能独立验证、独立回退；
- Java 保留区不因“追求 Kotlin 化”被强制迁移；
- Kotlin 改写必须保留原 Java 控制流、异常传播、线程可见性及 JVM 互操作语义。

---

## 2. 目标分支当前基线

目标分支当前已具备：

- Kotlin DSL；
- Gradle version catalog；
- AGP 8.7.2；
- Gradle 8.9；
- Kotlin 2.0.21；
- JDK 17；
- libxposed API/service 102；
- `minApiVersion=101`；
- `targetApiVersion=102`；
- A13 独立包名及长期签名；
- 多批 Java → Kotlin 保守迁移；
- Preference 默认样式回归修复；
- 应用内语言切换污染 `Configuration` 的修复。

不得回退或破坏上述基线。

---

## 3. 必须适配的改动

## 3.1 修复 RemotePreferences 空快照被永久缓存

### 目标文件

```text
app/src/main/java/name/monwf/customiuizer/MainModule.java
```

### 当前问题

`initPrefs(Map<String, ?> allPrefs)` 在 `allPrefs` 为空时仍然设置：

```java
prefsLoaded = true;
```

如果模块进程启动时 RemotePreferences Provider 尚未完全可用，该进程会把空配置永久视为已加载，后续不再重试，直到进程重启。

### 修改要求

新增一次性日志标记，例如：

```java
private boolean emptyPrefsReported;
```

调整语义：

1. `allPrefs == null` 或 `allPrefs.isEmpty()` 时：
   - 每个进程只记录一次 `Empty preferences!`；
   - 直接返回；
   - 不设置 `prefsLoaded = true`；
   - 不清空已有 `mPrefs`；
2. 只有成功取得非空快照并写入 `mPrefs` 后，才设置：
   ```java
   prefsLoaded = true;
   ```
3. 保留现有 `getRemotePrefs()` 缓存；
4. 不增加轮询；
5. 不增加后台线程；
6. 不改变 RemotePreferences 名称。

### 验证点

- Provider 暂时返回空快照时，后续包加载仍可再次尝试；
- 空快照日志不重复刷屏；
- 成功加载后不重复复制整份偏好；
- 原有偏好变化监听仍能增量更新。

---

## 3.2 仅在监听器注册成功后设置状态标记

### 目标文件

```text
app/src/main/java/name/monwf/customiuizer/MainModule.java
```

### 当前问题

`watchPreferenceChange()` 在调用：

```java
remotePrefs.registerOnSharedPreferenceChangeListener(mListener);
```

之前就设置：

```java
prefsWatcherRegistered = true;
```

若注册过程抛出异常，后续调用会错误地认为监听器已经注册。

### 修改要求

顺序必须调整为：

```java
remotePrefs.registerOnSharedPreferenceChangeListener(mListener);
prefsWatcherRegistered = true;
```

要求：

- 不改变监听回调逻辑；
- 不改变 ignore key；
- 不重复注册；
- 不吞掉原有异常；
- 只有真实注册成功后才置位。

---

## 3.3 新增 PrefPair，消除分隔字符串解析中的正则分配

### 新增文件

```text
app/src/main/java/name/monwf/customiuizer/utils/PrefPair.kt
app/src/test/java/name/monwf/customiuizer/utils/PrefPairTest.kt
```

### 数据格式

模块多处使用：

```text
first|second
```

典型数据：

```text
package|activity
bssid|ssid
address|deviceName
```

### 实现要求

新增：

```kotlin
object PrefPair
```

至少提供：

```kotlin
const val DELIMITER: Char = '|'

@JvmStatic
fun first(pair: String): String

@JvmStatic
fun firstEquals(pair: String, needle: String): Boolean

@JvmStatic
fun containsFirst(pairs: Set<String>?, needle: String): Boolean
```

实现约束：

- `firstEquals` 不创建 Regex；
- `firstEquals` 不为首段比较创建临时子字符串；
- 大小写比较语义与原 `equalsIgnoreCase` 一致；
- 无分隔符时，整个字符串视为第一段；
- 以 `|` 开头时，第一段为空；
- 多个 `|` 时，只认第一个；
- Java 可通过静态方法调用。

建议实现方式：

- `indexOf('|')`
- `regionMatches(..., ignoreCase = true)`

### 必须检查并替换的调用

搜索：

```text
split("\\|".toRegex())
toRegex()
containsStringPair
substringBefore("|")
```

重点文件包括但不限于：

```text
app/src/main/java/name/monwf/customiuizer/utils/Helpers.kt
app/src/main/java/name/monwf/customiuizer/utils/AppHelper.kt
app/src/main/java/name/monwf/customiuizer/mods/
app/src/main/java/name/monwf/customiuizer/subs/
```

处理规则：

1. 只需取得第一段并比较：
   - 使用 `PrefPair.firstEquals()` 或 `PrefPair.containsFirst()`；
2. 需要两个字段：
   - 使用 `split(PrefPair.DELIMITER)`；
   - 或使用 `indexOf()` 手动解析；
3. 不得把单字符分隔解析改成 Regex；
4. 不改变尾部空字段处理语义；
5. 不改变已有数据存储格式。

### 测试要求

至少覆盖：

- 正常两段；
- 无分隔符；
- 首段为空；
- 多个分隔符；
- 忽略大小写；
- 前缀和后缀不得误判；
- null 集合；
- 空集合；
- 第二段为空。

---

## 3.4 修复 SystemUI 状态栏文本图标的静态强引用

### 目标文件

```text
app/src/main/java/name/monwf/customiuizer/mods/SystemUI.java
```

### 当前问题

A13 使用静态集合长期保存状态栏温度、电流等 View：

```java
mStatusbarTextIcons.add(iconView);
```

SystemUI 在主题、密度、横竖屏、显示配置变化或状态栏重新 inflate 时，会创建新 View。旧 View 及其 Context 仍被静态集合强引用，导致：

- 旧 View 无法回收；
- Context 被长期持有；
- 2 秒监控回调继续遍历已失效的 View；
- SystemUI 运行时间越长，无效对象越多。

### 修改要求

使用弱引用保存：

```java
private static final ArrayList<WeakReference<View>> statusbarTextIcons =
        new ArrayList<>(4);
```

新增统一方法，例如：

```java
private static void registerStatusbarTextIcon(View iconView)
private static void forEachStatusbarTextIcon(...)
private static void updateStatusbarTextIcons(...)
```

要求：

1. 每次注册前删除：
   - 已被 GC 的引用；
   - 与当前 `iconView` 相同的重复引用；
2. 每次遍历时删除失效引用；
3. 必须保留 A13 原有：
   - `TextIcon` tag；
   - `TextIcon.atRight`；
   - `TextIcon.iconType`；
   - `setBlocked`；
   - 新旧样式不同的 `setNetworkSpeed` 参数；
   - `showSystemIconArea` / `hideSystemIconArea` 行为；
4. 不直接复制 A14 的 `Int tag` 实现；
5. 不改变图标插入位置；
6. 不改变左右侧逻辑；
7. 不改变 91 / 92 图标类型；
8. 不改变监控刷新间隔；
9. 不改变 Hook 目标类和方法签名。

### 建议结构

```java
private interface StatusbarTextIconConsumer {
    void accept(View iconView, TextIcon textIcon) throws Throwable;
}
```

遍历时：

- 取出弱引用；
- 空引用立即删除；
- 从 `textIconTagId` 读取原 `TextIcon`；
- 再执行原有逻辑。

### 回归验证

必须验证：

- 左侧图标；
- 右侧图标；
- 双排状态栏；
- 新旧 NetworkSpeedView 样式；
- `showSystemIconArea`；
- `hideSystemIconArea`；
- 电池温度/电流刷新；
- 设备温度刷新；
- SystemUI 重启；
- 日夜主题切换；
- 横竖屏切换。

---

## 3.5 独立优化 A13 ResourceHooks.java 热路径

### 目标文件

```text
app/src/main/java/name/monwf/customiuizer/mods/utils/ResourceHooks.java
```

### 风险等级

```text
高
```

必须单独提交，便于独立回退。

### 当前问题

当前 `mReplaceHook` 在每次被 Hook 的资源读取中都会执行：

```java
ModuleHelper.findContext();
chain.getExecutable().getName();
```

普通未命中资源还会调用：

```java
getResourcePackageName()
getResourceTypeName()
getResourceEntryName()
```

随后拼接：

```text
package:type/name
```

再查询字符串 Map。

资源 Hook 一旦安装，会进入目标进程大量资源读取调用，未命中是绝大多数情况，因此必须优先降低 miss path 成本。

### 适配目标

在不改变 A13 行为的前提下：

- fake resource 使用整数 ID 直接查询；
- 已知真实资源 ID 的 replacement 使用 `SparseArray`；
- 未命中时不获取 Context；
- 未命中时不读取 `Executable.name`；
- 注册阶段尽量解析资源 ID；
- 读取表采用安全发布；
- 保留通配符替换；
- 保留三种 ReplacementType；
- 保留现有 Hook 方法范围。

### 推荐数据结构

保留或新增：

```java
private final Object replacementsLock = new Object();

private volatile SparseIntArray fakes = new SparseIntArray();

private volatile SparseArray<Pair<ReplacementType, Object>>
        resourceIdReplacements = new SparseArray<>();

private final ConcurrentHashMap<String, Pair<ReplacementType, Object>>
        unresolvedReplacements = new ConcurrentHashMap<>();
```

说明：

- `resourceIdReplacements`：能解析为真实资源 ID 的精确替换；
- `unresolvedReplacements`：
  - `*:` 通配符；
  - 注册时没有 Context；
  - 注册时找不到 ID；
  - 其他必须保留旧名称匹配语义的替换。

### 写入要求

`SparseArray` 和 `SparseIntArray` 非线程安全，不得原地写后让其他线程同时读。

采用 copy-on-write：

```java
synchronized (replacementsLock) {
    SparseArray<...> updated = resourceIdReplacements.clone();
    updated.put(resId, value);
    resourceIdReplacements = updated;
}
```

`fakes` 同理。

### 读取顺序

建议 miss path：

1. 从参数取得 `resId`；
2. 查询 `resourceIdReplacements`；
3. 查询 `fakes`；
4. 两者都未命中且 `unresolvedReplacements` 为空：
   - 立即 `chain.proceed()`；
5. 只有需要按资源名称匹配时才：
   - 获取 `Resources`；
   - 调用 `getResourcePackageName/typeName/entryName`；
   - 构造完整名称；
   - 查询精确 key 和 `*:` key；
6. 只有真实命中后才：
   - 获取 Context；
   - 获取 module resources；
   - 获取方法名；
   - 计算替换返回值。

### 必须保留的行为

- `ReplacementType.ID`
- `ReplacementType.DENSITY`
- `ReplacementType.OBJECT`
- `*:<type>/<name>` 通配符
- `getDimensionPixelOffset` / `getDimensionPixelSize` 的 Float → Int
- fake resource 的所有参数转发语义
- 原有异常日志和 fallback
- 原有 Hook 方法列表
- MIUI 14 资源名称兼容逻辑

### 禁止事项

- 不把整个类直接替换为 A14 的 `ResourceHooks.kt`；
- 不删除 A13 的通配符功能；
- 不缩减 Hook 方法；
- 不改变 ID / DENSITY / OBJECT 返回类型；
- 不在 miss path 新增 Kotlin 集合、序列、lambda 或 Regex；
- 不增加全局锁读取；
- 不使用普通 `HashMap<Int, ...>` 代替 SparseArray。

### 测试方式

静态测试：

- fake resource；
- 精确资源替换；
- 通配符替换；
- density 替换；
- object 替换；
- dimension pixel int 转换；
- 资源不存在；
- Context 暂不可用。

实机测试：

- 状态栏高度；
- 状态栏 margin；
- 控制中心样式；
- 通知紧凑布局；
- 音量计时；
- 圆角磁贴；
- 网络速度后缀；
- 模块关闭相关功能时不安装无关资源 Hook。

---

## 3.6 更新 Gradle 属性

### 目标文件

```text
gradle.properties
```

删除：

```properties
android.enableResourceOptimizations=true
org.gradle.unsafe.configuration-cache=true
```

新增：

```properties
org.gradle.configuration-cache=true
org.gradle.caching=true
```

保留：

```properties
android.nonFinalResIds=false
android.suppressUnsupportedCompileSdk=36
```

除非构建验证证明必须修改，否则不得顺带调整其他 Gradle 参数。

---

## 3.7 将语言入口从首页集中到 About 页面

### 目标文件

```text
app/src/main/res/xml/prefs_main.xml
app/src/main/res/xml/prefs_about.xml
app/src/main/java/name/monwf/customiuizer/MainFragment.java
app/src/main/java/name/monwf/customiuizer/AboutFragment.kt
app/src/main/java/name/monwf/customiuizer/utils/AppHelper.kt
```

### 目标

首页不再重复显示语言选项；语言切换统一放在 About 页面。

### 修改要求

#### `prefs_main.xml`

删除：

```xml
<name.monwf.customiuizer.prefs.ListPreferenceEx
    android:key="pref_key_miuizer_locale"
    ... />
```

保留 launcher icon 和其他设置。

#### `prefs_about.xml`

在 About 设置分类内加入：

```xml
<name.monwf.customiuizer.prefs.ListPreferenceEx
    android:key="pref_key_miuizer_locale"
    android:title="@string/miuizer_locale_title"
    android:summary="@string/miuizer_locale_summ"
    android:entries="@array/placeholder_array"
    android:entryValues="@array/placeholder_array_strval"
    miuizer:valueAsSummary="true"
    android:defaultValue="auto" />
```

确保根节点包含：

```xml
xmlns:miuizer="http://schemas.android.com/apk/res-auto"
```

#### `MainFragment.java`

删除首页中以下重复逻辑：

- locales 数组；
- localeNames 构造；
- `ListPreferenceEx locale` 初始化；
- locale change listener；
- 仅为该逻辑使用的 import。

不得影响：

- 搜索；
- launcher icon；
- 分类跳转；
- Handler / Runnable 生命周期处理。

#### `AboutFragment.kt`

在 View 创建完成后调用：

```kotlin
findPreference<ListPreferenceEx>("pref_key_miuizer_locale")?.let {
    AppHelper.setupLocalePreference(it)
}
```

保留 A13 当前 develop 版本日期显示逻辑，除非构建配置已经明确移除 `BUILD_TIME`。

### 验证

- 中文 → English；
- English → 跟随系统中文；
- 跟随系统；
- Activity recreate；
- 日间 / 夜间模式；
- About 页面 value summary；
- 首页不再显示重复入口。

---

## 4. 可选但建议同步的 UI 整理

以下内容可做，但应与核心 Hook 修复分开提交。

## 4.1 About 头部信息拆分

A13 当前将维护者和上游来源放在同一行：

```text
Maintained by tomthenpc · Based on open-source work by Mikanoshi and MonwF
```

可拆为：

```text
Maintained by tomthenpc
Based on open-source work by Mikanoshi and MonwF
Version r13.2.3-devin
```

涉及：

```text
app/src/main/res/layout/fragment_about_head.xml
app/src/main/res/values/strings.xml
app/src/main/res/values-zh-rCN/strings.xml
```

要求：

- 保持 A13 品牌；
- 不复制 A14 包名；
- 不删除已有翻译；
- 缺失翻译允许回退到 base strings；
- 不因 UI 整理改变 Hook 或配置逻辑。

---

## 5. 不应移植的 A14 改动

以下内容禁止机械移植：

### Android 14 专属内容

- Android 14 / API 34 版本闸门；
- HyperOS 1 专属类名；
- A14 SystemUI Hook 目标；
- Android 14 Receiver export 行为的重复修改；
- A14 Manifest 组件全包名；
- A14 applicationId；
- A14 namespace；
- A14 资源目录结构。

### 构建系统

不得升级：

- AGP；
- Gradle Wrapper；
- Kotlin；
- compileSdk；
- targetSdk；
- minSdk；
- AndroidX；
- libxposed；
- coroutines；
- DexKit；
- JDK。

### A14 独有实现

不得直接复制：

```text
tv.withaibuild.customiuizer.*
ResourceHooks.kt
SystemUIMonitorAndTileHooks.kt
A14 MainModule Android 14 包分支
A14 thermal zone 扫描实现
A14 statusbar tag Int 结构
A14 release/signing 信息
```

### 文档

A14 的以下文档只能作为审计方法参考，不能原样放进 A13：

```text
ARCHITECTURE_AUDIT_r14.13.md
DEVIN_A14_CHECKPOINT.md
REFACTOR_PROGRESS.md
```

A13 需要独立记录自身代码和验证结果。

---

## 6. Java → Kotlin 迁移核对规则

本任务可能修改 Kotlin 文件，但不要求继续扩大 Kotlin 化范围。

修改由 Java 迁移而来的 Kotlin 文件前，必须核对原 Java 控制流。

重点检查：

```text
break
continue
return
try/finally
synchronized
volatile
nullability
overload
static
```

规则：

1. Java `switch` 中的 `break` 在 Kotlin `when` 中消失属于正常；
2. 循环体中的 `break` / `continue` 消失，先按回归处理；
3. 不要把需要跳出循环的逻辑放进无法非局部跳出的 lambda；
4. Java 单字符 `split("\\|")` 不得机械改成 Kotlin Regex；
5. 热路径上的 `Map<Int, *>` 优先使用 `SparseArray`；
6. 热路径上的 `Map<Long, *>` 优先使用 `LongSparseArray`；
7. 原 Java `volatile` / `synchronized` 不得无依据删除；
8. Java 调用 Kotlin object 时必须保留 `@JvmStatic` / `@JvmField` / `@JvmOverloads` 兼容；
9. 不用 `runCatching`、`forEach`、Sequence 隐藏高频 Hook 控制流；
10. 不以“代码更短”为性能依据。

---

## 7. 版本更新

### 目标文件

```text
app/build.gradle.kts
CHANGELOG.md
```

建议：

```kotlin
val lastVersion = 121
val lastVersionName = "r13.2.3-devin"
```

前提：当前分支确实为：

```text
versionCode = 120
versionName = r13.2.2-devin
```

如果目标分支在执行任务前已有新提交，应在当前最大 `versionCode` 基础上加 1，不得覆盖新版本。

### Changelog 建议内容

```markdown
## r13.2.3-devin（Claude 审计修复适配）

### 修复

- 修复 RemotePreferences 早期返回空快照后被永久标记为已加载的问题。
- 仅在偏好监听器成功注册后设置注册状态。
- 修复 SystemUI 状态栏温度/电流文本 View 被静态集合长期强引用的问题。
- 将应用语言入口集中到 About 页面。

### 性能

- 新增 PrefPair，移除 pair 字符串解析中的重复 Regex 分配。
- 优化 ResourceHooks 资源读取未命中路径，减少 Context 查询、反射方法名读取和资源名称解析。
- 更新 Gradle configuration cache 属性并启用 build cache。

### 兼容边界

- 保持 MIUI 14 / Android 13。
- 保持 applicationId、namespace 和 Xposed 入口不变。
- 保持 minApiVersion=101、targetApiVersion=102。
- 不引入 API 102 专属类型到 API 101 公共加载路径。

### 验证

- `:app:test`
- `:app:lint`
- `:app:assembleDebug`
- `:app:assembleRelease`
- Release R8、v2 签名、zipalign 和 Xposed metadata 检查
```

实际验证结果必须按真实执行情况填写，不得提前写“通过”。

---

## 8. 建议提交拆分

必须按可回退单元拆分。

```text
1. fix(module): retry empty remote preferences and register watcher safely

2. perf(utils): add PrefPair and remove regex pair parsing

3. fix(systemui): avoid retaining stale status bar text views

4. perf(resources): reduce A13 resource hook miss-path overhead

5. build: modernize Gradle cache properties

6. refactor(locale): move locale preference to About page

7. chore: bump r13.2.3-devin and update changelog
```

要求：

- `ResourceHooks.java` 必须独立提交；
- SystemUI 弱引用修复不得与 ResourceHooks 混合；
- 版本号和 changelog 最后提交；
- 不使用一个超大提交覆盖全部修改；
- 每次提交后至少运行相关编译或测试。

---

## 9. 构建与验证

## 9.1 必须运行

Windows：

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:test
.\gradlew.bat :app:lint
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

Linux/macOS：

```bash
./gradlew clean
./gradlew :app:test
./gradlew :app:lint
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

如果 configuration cache 导致某个已有任务不兼容：

- 记录具体任务和错误；
- 不得静默关闭所有验证；
- 可对该次诊断使用：
  ```text
  --no-configuration-cache
  ```
- 但必须保留正式配置，除非确认该工程无法兼容。

## 9.2 必须检查

### APK

- applicationId：
  ```text
  tv.withaibuild.customiuizer.r13
  ```
- minSdk：
  ```text
  33
  ```
- targetSdk：
  ```text
  34
  ```
- ABI：
  ```text
  arm64-v8a
  ```
- v2 signing；
- zipalign；
- Release 使用 A13 正式签名；
- 不回退 Android Debug 签名。

### Xposed metadata

```text
app/src/main/resources/META-INF/xposed/module.prop
app/src/main/resources/META-INF/xposed/java_init.list
app/src/main/resources/META-INF/xposed/scope.list
```

必须保持：

```text
minApiVersion=101
targetApiVersion=102
staticScope=false
```

入口必须保持：

```text
name.monwf.customiuizer.MainModule
```

### API 边界

- API 101 公共加载路径不得引用仅 API 102 存在的类型；
- 不引入 Legacy Xposed API；
- 不改变 Android 13 版本闸门；
- 不改变默认 scope。

### R8

检查：

- Java/Kotlin 静态互操作；
- Hooker 类；
- `MainModule` 入口；
- PrefsProvider；
- Tasker；
- shortcuts；
- 自定义 Preference；
- 反射目标所需 keep rule。

---

## 10. 实机回归矩阵

### 设置应用

- 启动；
- 首页；
- About 页面；
- 语言切换；
- 跟随系统；
- 日间/夜间；
- launcher icon；
- 搜索；
- 子页面进入与返回；
- 设置重置；
- LSPosed 服务连接。

### Hook

- system_server 正常加载；
- SystemUI 正常加载；
- Launcher 正常加载；
- 无新增 `Failed to hook`；
- 无 `NoSuchMethodError`；
- 无 `ClassNotFoundError`；
- 无 RemotePreferences 异常；
- 无 ANR；
- 无模块相关 crash。

### SystemUI 文本图标

- 电池温度；
- 电流；
- 功率；
- 设备温度；
- 左侧；
- 右侧；
- 单排；
- 双排；
- 新旧样式；
- 主题切换；
- 横竖屏；
- SystemUI 重启。

### 资源替换

- 状态栏高度；
- 状态栏 padding；
- 控制中心样式；
- 系统字体；
- 紧凑通知；
- 音量计时；
- 圆角磁贴；
- fake layout；
- 通配符资源；
- 无关功能关闭时不产生异常。

---

## 11. 完成条件

任务只有同时满足以下条件才算完成：

- 所有代码按 A13 结构适配；
- 没有复制 A14 专属 Hook；
- `MainModule` 空偏好问题修复；
- watcher 标记顺序修复；
- `PrefPair` 及测试加入；
- pair Regex 热路径清理；
- SystemUI 旧 View 强引用修复；
- ResourceHooks miss path 优化；
- Gradle 属性更新；
- 语言入口集中到 About；
- 版本和 changelog 更新；
- 单元测试通过；
- Lint 通过；
- Debug 构建通过；
- Release 构建通过；
- R8 通过；
- v2 签名通过；
- zipalign 通过；
- Xposed metadata 不变；
- API 101/102 边界不变；
- 每项按独立提交拆分；
- 未创建 tag；
- 未创建 GitHub Release；
- 未自动合并目标分支。

---

## 12. 最终报告格式

完成后输出：

```markdown
# A13 r13.2.3-devin 适配结果

## 已完成

- ...

## 未实施

- ...
- 原因：...

## 提交

1. `<sha>` `<message>`
2. ...

## 构建

- `:app:test`：
- `:app:lint`：
- `:app:assembleDebug`：
- `:app:assembleRelease`：

## APK

- 文件名：
- 大小：
- SHA-256：
- applicationId：
- versionCode：
- versionName：
- minSdk：
- targetSdk：
- ABI：
- 签名方案：
- 证书 SHA-256：
- zipalign：

## Xposed

- minApiVersion：
- targetApiVersion：
- staticScope：
- java_init.list：
- Legacy API 扫描：

## 风险与待实机项

- ...

## 明确未执行

- 未合并目标分支
- 未创建 tag
- 未创建 Release
```
