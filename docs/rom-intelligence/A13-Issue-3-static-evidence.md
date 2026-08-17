# A13 Issue #3 — Floating Notification Menu / Open-in-FW 静态证据

> 本批包含 Stage A/B 情报与 Stage D production 修复证据。
> 所有 ROM 分区/APK/JAR 均来自用户本地样本，未上传。
> Stage D 结论：**PASS**（HOLD 解除）—— HyperOS `veux` 的 `NotificationGuts$GutsContent` 构造器已通过 `SystemNotificationMoreHooks.kt` 中的 bounded structural resolver 支持，构建与单元测试通过；实机验证未进行。

## 1. Issue 描述

- GitHub Issue #3: “Floating Notification Menu missing”
- 设备：Poco X4 Pro，代号 `veux`
- ROM：HyperOS 1.0.10.0，Android 13
- 现象：新模块版本（r13.11.1）菜单缺失；回退 r13.10.1 后恢复

## 2. 涉及 preference 与调用链

### 2.1 `Extended notification menu` (`system_notifrowmenu`)

- **Preference key**: `pref_key_system_notifrowmenu`
- **业务名**: Extended notification menu / 扩展通知菜单
- **Installer**: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java:127`
  ```java
  if (MainModule.mPrefs.getBoolean("system_notifrowmenu"))
      SystemNotificationMoreHooks.NotificationRowMenuHook(lpparam);
  ```
- **Hook**: `SystemNotificationMoreHooks.NotificationRowMenuHook`
- **Target class**: `com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow`
- **Target method**: `createMenuViews(boolean, boolean)`
- **JVM descriptor**: `(ZZ)V`
- **Target process**: `com.android.systemui`
- **目标 APK 位置（HyperOS veux）**: `system_ext_a/priv-app/MiuiSystemUI/MiuiSystemUI.apk`（package `com.android.systemui`）

该路径是 legacy installer 路径，`FeatureCatalog` 中无对应 Feature ID。

### 2.2 `Open notification in floating window` (`system_notify_openinfw`)

- **Preference key**: `pref_key_system_notify_openinfw`
- **Installer**: `SystemUiInstaller.java:314`
  ```java
  if (MainModule.mPrefs.getBoolean("system_notify_openinfw"))
      SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam);
  ```
- **Hook**: `SystemUINotificationHooks.OpenNotifyInFloatingWindowHook`
- **Target class**: `com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter`
- **Target method**: `startNotificationIntent(...)`
- **Secondary class**: `com.android.systemui.statusbar.notification.policy.AppMiniWindowManager`
- **Secondary method**: `launchMiniWindowActivity(String, PendingIntent)`
- **Required framework reflection**:
  - `android.service.notification.StatusBarNotification#isSubstituteNotification()`
  - `android.service.notification.StatusBarNotification#mPkgName` (field)

## 3. r13.10.1 基线考古

r13.10.1 的版本代码 `135` 在多个 commit 中出现，导致基线不唯一：

| 候选 SHA | 说明 | 置信度 |
|---|---|---|
| `b5f92a2` | `chore(release): prepare r13.10.1 release candidate`，首次把 version 改为 135/r13.10.1 | 低（只是 RC 准备，发生在 P1B-4A 重写之前） |
| `3dc0c1b` | `docs: finalize r13.10.1 release documentation`，尚未发生 P1B-4A notification menu 重写 | 中 |
| `1ea342d76e472da1349f8234b22fc31fd5395307` | P1B-3 QA R3 关闭 commit，tasks/completed/A13-PERF-P1B-3-QS-TILE-HOT-PATH.md 记录的工程最终 SHA 与历史签名 APK 版本点 | 中（存在有效 APK 候选） |
| `570cf21` | 版本 135/r13.10.1 的最后一个 commit，下一版本即 r13.11.0 | **高（最后一个 version 135 提交）** |

关键 diff（`b5f92a2` → `570cf21`，`SystemNotificationMoreHooks.kt`）:

- 旧实现（`b5f92a2` 及更早）使用 `MiuiNotificationMenuItem.constructors[0]`，无参数类型检查，向构造器第 4 个参数传 `null`。
- 新实现（`570cf21` / r13.11.1）使用 `XposedHelpers.findConstructorBestMatch(menuItemClass, menuRowClass, Context.class, int.class, Drawable.class, int.class)`，强制要求第 4 个参数类型为 `android.graphics.drawable.Drawable`。

这意味着：
1. 如果 HyperOS `veux` 的 `MiuiNotificationMenuItem` 构造器第 4 个参数不是 `Drawable` 而是其他类型（如 `NotificationGuts$GutsContent`），`findConstructorBestMatch` 会失败，导致 `menuItemConstructor == null`，hook 直接 `return`。
2. 若用户实际回退的是 `b5f92a2`（RC 构建）或更早无 `findConstructorBestMatch` 版本，则旧 `constructors[0]` 逻辑可在同样 ROM 上工作。

## 4. HyperOS 1 veux ROM 证据

### 4.1 分区提取

| 项目 | 值 |
|---|---|
| `super.img` SHA-256 | `3d3d1c427fdab3c64503bd8f2c947f46748887ba1c52df7f1b4d6cc6bcade175` |
| `system_ext_a` 文件系统 | EXT4（`0xef53` 位于 1080+1024） |
| `system_a` 文件系统 | EROFS（magic `0xE0F5E1E2` 位于 1024） |
| `product_a` 文件系统 | EXT4 |
| `MiuiSystemUI.apk` 路径 | `system_ext_a/priv-app/MiuiSystemUI/MiuiSystemUI.apk` |
| `MiuiSystemUI.apk` SHA-256 | `dd2271dfcd6975c0d8997d4a00a7ee975b0b45f7da6737487f4bf7dfed867b94` |
| `MiuiSystemUI.apk` package | `com.android.systemui` |
| `MiuiSystemUI.apk` dex count | 3 |

### 4.2 DEX 目标验证

通过 `tools/rom_dex_inspector.py` 解析 `veux_MiuiSystemUI.apk`：

- `Lcom/android/systemui/statusbar/notification/row/MiuiNotificationMenuRow;` 存在。
- `createMenuViews` 签名：`(Z Z)V`（即 `createMenuViews(boolean, boolean)`）。
- 相关字段：
  - `mContext` : `Landroid/content/Context;`
  - `mMenuItems` : `Ljava/util/ArrayList;`
  - `mMenuContainer` : `Landroid/view/ViewGroup;`
  - `mSbn` : `Lcom/android/systemui/statusbar/notification/ExpandedNotification;`
  - `mParent` : `Lcom/android/systemui/statusbar/notification/row/ExpandableNotificationRow;`
  - `mMenuMargin` : `I`
- 嵌套类 `Lcom/android/systemui/statusbar/notification/row/MiuiNotificationMenuRow$MiuiNotificationMenuItem;` 存在。
- 其构造器签名：
  ```
  (Lcom/android/systemui/statusbar/notification/row/MiuiNotificationMenuRow;
   Landroid/content/Context;
   I
   Lcom/android/systemui/statusbar/notification/row/NotificationGuts$GutsContent;
   I)V
  ```
- 当前 `SystemNotificationMoreHooks.kt` 使用 `findConstructorBestMatch(..., Drawable.class, ...)`，将不会匹配到上述构造器，因为第 4 个实际参数类型是 `NotificationGuts$GutsContent`，不是 `Drawable`。

### 4.3 与当前源码的匹配结论

| 检查项 | 状态 | 说明 |
|---|---|---|
| `MiuiNotificationMenuRow` 类存在 | ✅ STATIC_VERIFIED | DEX 中可定位 |
| `createMenuViews(ZZ)V` 方法存在 | ✅ STATIC_VERIFIED | 签名一致 |
| `mContext/mMenuItems/mMenuContainer/mSbn/mParent/mMenuMargin` 字段存在 | ✅ STATIC_VERIFIED | 字段名与源码查找一致 |
| `MiuiNotificationMenuItem` 嵌套类存在 | ✅ STATIC_VERIFIED | 可定位 |
| `MiuiNotificationMenuItem` 构造器与源码假设匹配 | ❌ MISMATCH | 源码期望第 4 参数 `Drawable`，实际为 `NotificationGuts$GutsContent` |

## 5. MIUI 14 xaga 证据

### 5.1 样本清单

| 样本 | 代号 | Android | 版本 | super.img SHA-256 |
|---|---|---|---|---|
| MIUI 14 China xaga | xaga | 13 | V14.0.7.0.TLOCNXM | `a71f7265622a68831fe1d69d354cd92048102dfda345948eb7f207a3a867fffa` |
| MIUI 14 India xaga | xaga | 13 | V14.0.10.0.TLOINXM | `8ce7caa680e11c156529b5b9b7fe6d68ef03425041c8ce3f1f2bf1ce53d2c4e0` |

完整 super 分区表见 `A13_rom_super_xaga_cn.json/csv`、`A13_rom_super_xaga_in.json/csv`。

### 5.2 文件系统解析结果

- `product_a`、`system_a` 分区均为 **EROFS**（magic `0xE0F5E1E2` 位于 1024）。
- EROFS 数据为压缩/结构化存储，`MiuiNotificationMenuRow`、`createMenuViews`、`MiuiStatusBarNotificationActivityStarter`、`AppMiniWindowManager` 等目标字符串未出现在原始分区字节表层。
- 当前环境缺少可用的 EROFS 解压/提取工具（`erofs-utils`、`erofsfuse`、WSL 等），无法从 MIUI 14 样本独立提取 `SystemUI.apk` 并做 DEX 级证据。
- 因此 MIUI 14 样本目标 class/member 状态保持 **UNRESOLVED**。

### 5.3 已拒绝/不可用的方法

| 方法 | 状态 | 原因 |
|---|---|---|
| full-ROM JADX | 未使用 | 违反本批范围，且 FS 未解析 |
| erofs Python 库 | 不可用 | pip 无成熟 erofs 解析包；srlabs/extractor 示例脚本无法正确解析 xaga CN 目录结构 |
| WSL / Linux erofs-fuse | 不可用 | 当前环境未安装 WSL，无 root 权限 |
| simg2img / lpunpack 二进制 | 不可用 | 未在 PATH 中找到 |
| payload_dumper.exe | 不适用 | 该工具针对 OTA `payload.bin`，非 fastboot `super.img` |

## 6. Issue #3 目标矩阵

详见 `A13-Issue-3-matrix.csv` 与 `A13-Issue-3-matrix.json`。

| ROM | class | member | member descriptor | constructor 4th param (expected) | constructor 4th param (actual) | status |
|---|---|---|---|---|---|---|
| MIUI 14 CN xaga | `com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow` | `createMenuViews` | `(ZZ)V` | — | — | UNRESOLVED |
| MIUI 14 IN xaga | 同上 | 同上 | `(ZZ)V` | — | — | UNRESOLVED |
| HyperOS 1 ID veux | 同上 | 同上 | `(ZZ)V` | `android.graphics.drawable.Drawable` | `com.android.systemui.statusbar.notification.row.NotificationGuts$GutsContent` | STATIC_VERIFIED （构造器不匹配） |

## 7. 回归假设

### H1：模块回归（`NotificationRowMenuHook` 构造器参数硬编码）

- 从 `b5f92a2`（旧逻辑，使用 `constructors[0]` 无类型检查）到 `570cf21` / HEAD（使用 `findConstructorBestMatch(..., Drawable.class, ...)`），`NotificationRowMenuHook` 引入了构造器第 4 参数的类型约束。
- 在 MIUI 14 上，若 `MiuiNotificationMenuItem` 构造器第 4 参数为 `Drawable`，则 r13.11.1 仍可工作。
- 在 HyperOS 1 `veux` 上，构造器第 4 参数为 `NotificationGuts$GutsContent`，`findConstructorBestMatch` 返回 `null` 或被 `try/catch` 吞掉，hook 安装后 `menuItemConstructor` 为空，菜单按钮未被注入。
- 这与“同一个 ROM 在 r13.10.1 正常、r13.11.1 失效”兼容，前提是用户回退的 r13.10.1 是 `b5f92a2`/`3dc0c1b` 等早于 P1B-4A 重写的构建。

### H2：ROM 目标变体（`MiuiNotificationMenuRow$MiuiNotificationMenuItem` 构造器签名不同）

- HyperOS 1 `veux` 的 `MiuiNotificationMenuItem` 构造器第 4 参数为 `NotificationGuts$GutsContent`（或等价变体），而 MIUI 14 可能是 `Drawable` 或 `GutsContent` 的不同实现。
- 这是 ROM/framework 目标变化，不是模块固有 bug。

### H3：两者皆有

- 模块在 P1B-4A 中把构造器查找从“无类型检查”改成“必须匹配 `Drawable`”，恰好与 HyperOS 1 的构造器变体不兼容，导致问题仅在 HyperOS 1 上暴露。

## 8. 下一步最小修正建议（不实现）

针对 H1/H3：

1. 在 `SystemNotificationMoreHooks.NotificationRowMenuHook` 中，将 `MiuiNotificationMenuItem` 构造器查找从固定 `Drawable.class` 改为运行时探测：
   - 先尝试 `(Context, int, Drawable, int)`；
   - 失败时回退到 `(Context, int, NotificationGuts$GutsContent, int)` 或直接使用 `getDeclaredConstructors()` 选择第一个 5 参数构造器。
2. 保留旧 `b5f92a2` 时代的 `constructors[0]` 行为作为 HyperOS 兼容回退。
3. 对 `mSbn` 类型（`StatusBarNotification` vs `ExpandedNotification`）保持现有字段查找，因为 `XposedHelpers.findFieldIfExists` 按名字而非类型匹配。
4. 不修改 `SystemUINotificationHooks.kt`（`OpenNotifyInFloatingWindowHook`）作为 Issue #3 的修复，因为当前证据指向 `NotificationRowMenuHook` 构造器匹配失败。

预期变更文件（仅在后续 production 批处理，本批不做）：
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`

## 9. 证据等级

- HyperOS veux `MiuiNotificationMenuRow` class/member：`STATIC_VERIFIED`
- HyperOS veux `MiuiNotificationMenuItem` 构造器：`STATIC_VERIFIED`（且与源码假设 **不匹配**）
- MIUI 14 xaga class/member：`UNRESOLVED`（EROFS 无法解析）
- 代码回归 diff（`b5f92a2` → `570cf21`）：`STATIC_VERIFIED`
- 运行时行为（HyperOS 1 实机）：`UNVERIFIED`
- 修正后效果：`UNVERIFIED`

## 10. 关键结论

- **LP extent 偏移已修正**：`rom_super_inspector.py` 不再把 `first_logical_sector` 加到 `target_data`，三份 ROM 元数据已用正确公式重新生成。
- **HyperOS veux 已拿到直接 DEX 证据**：`MiuiSystemUI.apk` 从 `system_ext_a` 提取并解析，`NotificationRowMenuHook` 目标类/方法存在。
- **MIUI 14 xaga 仍受 EROFS 工具限制**：无法获得同等 DEX 证据。
- **Issue #3 最可能根因**：`NotificationRowMenuHook` 在 P1B-4A 重构后使用 `findConstructorBestMatch(..., Drawable.class, ...)`，与 HyperOS veux 实际 `NotificationGuts$GutsContent` 构造器参数不匹配，导致 menu item 无法构造。
- **最终状态**：`HOLD` 已在 Stage D 解除（见第 11 节）。

## 11. Stage D — Production Corrective

Stage D 仅修复 Issue #3，保持 Issue #4 不变。

### 11.1 预检结论

| 检查项 | 结论 |
|---|---|
| `mMenuContainer` declared type | `Landroid/view/ViewGroup;` |
| `mMenuContainer` concrete object | `new-instance Landroid/widget/LinearLayout;` |
| `LinearLayout` cast | `SAFE`（实际对象为 `LinearLayout` 子类） |
| `MiuiNotificationMenuItem#getMenuView()` | 在父类 `NotificationMenuRow$NotificationMenuItem` 中声明并继承 |
| `getMenuView` resolution | `PASS` |

### 11.2 修复策略

- **精确 ABI 优先**：安装时先尝试 `NotificationGuts$GutsContent` 构造器 `(outer, Context, int, GutsContent, int)`。
- **结构回退**：精确失败时，枚举 `declaredConstructors`，只接受 `(outer, Context, int, reference, int)` 且唯一无歧义。
- **安全边界**：无匹配或歧义时返回 `null`，`createMenuViews` after-callback 原样 `return`，保持既有 no-op 行为。
- **无顺序依赖**：不取 `constructors[0]`。
- **无 ROM/version 分支**：不判断包名、版本、ROM。
- **无 DexKit**：纯反射，按 classLoader 隔离缓存。
- **热路径无扫描**：构造器解析在 `NotificationRowMenuHook` 安装阶段执行一次，after-callback 仅复用已缓存的 `Constructor`。

### 11.3 验证结果

- `python tools/verify.py fast --tests NotificationRowMenuHookTest`：**PASS**
- `python tools/verify.py fast --tests NotificationMenuItemConstructorResolverTest`：**PASS**
- `python tools/verify.py full`：**PASS**（包含编译与单元测试）
- `python -m compileall tools`：**PASS**
- `python -m unittest discover -s tools/tests -p "test_*.py"`：**PASS**（1267 tests，skip 2）
- `git diff --check`：**PASS**（无空白错误）

### 11.4 证据等级（Stage D）

- 静态目标：`STATIC_VERIFIED`
- 构建验证：`BUILD_VERIFIED`
- 实机/运行时验证：`UNVERIFIED`（未连接目标设备）
- 修正后效果：`UNVERIFIED`
