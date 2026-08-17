# A13 Issue #3 — Floating Notification Menu / Open-in-FW 静态证据

> 本批仅建立 ROM 情报与回归证据，不做 production 修复。  
> 所有 ROM 分区/APK/JAR 均来自用户本地样本，未上传。

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
| `b5f92a2` | `chore(release): prepare r13.10.1 release candidate`，首次把 version 改为 135/r13.10.1 | 低（只是 RC 准备） |
| `3dc0c1b` | `docs: finalize r13.10.1 release documentation`，尚未发生 P1B-4A notification menu 重写 | 中 |
| `1ea342d76e472da1349f8234b22fc31fd5395307` | P1B-3 QA R3 关闭 commit，tasks/completed/A13-PERF-P1B-3-QS-TILE-HOT-PATH.md 记录的工程最终 SHA 与历史签名 APK 版本点 | **高（首选候选）** |
| `570cf21` | 版本 135/r13.10.1 的最后一个 commit，下一版本即 r13.11.0 | 中 |

由于存在多个合法 r13.10.1 时间点的 commit，且用户侧真实 APK 的具体构建 SHA 未知，本报告将 `1ea342d` 作为主要工作基线，但标记 `UNRESOLVED` 并保留候选范围。

## 4. 与当前 HEAD 的回归 diff（相关范围）

### 4.1 从 `1ea342d` → HEAD

```text
app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt
.../installers/SystemUiInstaller.java
```

主要变化集中在 `OpenNotifyInFloatingWindowHook`：
- 新增 `rethrowNotificationFatal` / `invokeNotificationCompat` 包装。
- `isSubstituteNotificationMethod` 从“找不到时按 `false` 处理”变为“找不到直接 `return`”。
- 新增强制要求 `mPkgNameField` 字段，找不到也直接 `return`。
- 原来失败时使用 `StatusBarNotification#getPackageName()`，现在改为从 `mPkgName` 反射取值。

`NotificationRowMenuHook` 在该范围内**无 diff**。

### 4.2 从 `3dc0c1b` → `1ea342d`（P1B-4A 区间）

```text
app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt
```

`NotificationRowMenuHook` 发生 P1B-4A 热路径重写：
- 从简单 `findClass + findAndHookMethod` 改为 install-time 反射缓存。
- 引入大量 `findFieldIfExists`/`findMethodBestMatch` 预解析。
- 对 `mContext`、`mMenuItems`、`mMenuContainer`、`mSbn`、`mParent` 等字段的缺失统一 `return`。
- 点击时重新绑定 `mSbn`/`mParent`，增加事务化 menu 注入。

## 5. ROM 语料

### 5.1 样本清单

| 样本 | 代号 | Android | 版本 | super.img SHA-256 | system_a 大小 | system_a SHA-256 |
|---|---|---|---|---|---|---|
| MIUI 14 China xaga | xaga | 13 | V14.0.7.0.TLOCNXM | `a71f7265622a68831fe1d69d354cd92048102dfda345948eb7f207a3a867fffa` | 869,396,480 | `ada302aa27304f2cfdeb68a84db0d572909cb8c3f0d91b7aad180bbcff2d9799` |
| MIUI 14 India xaga | xaga | 13 | V14.0.10.0.TLOINXM | `8ce7caa680e11c156529b5b9b7fe6d68ef03425041c8ce3f1f2bf1ce53d2c4e0` | 926,306,304 | `76a782cde46f1ca22409e242b1792ffb75f0aa6e3c85ec210763f9b040d775e9` |
| HyperOS 1 Indonesia veux | veux | 13 | OS1.0.10.0.TKCIDXM | `3d3d1c427fdab3c64503bd8f2c947f46748887ba1c52df7f1b4d6cc6bcade175` | 1,438,543,872 | `f724b83527dc71e35fd29635ad5fafacfd8cd5566366cccedcd915bd471ae8f8` |

完整 super 分区表见 `A13_rom_super_xaga_cn.json/csv`、`A13_rom_super_xaga_in.json/csv`、`A13_rom_super_veux_id.json/csv`。

### 5.2 文件系统解析结果

- `system_a` 与 `product_a` 分区均无法被 `ext4`、`f2fs`、`erofs`、`squashfs` 常见魔数识别。
- 分区内部存在高度可压缩/已压缩数据（erofs 或类似压缩只读 FS 的强候选）。
- 在 `system_a` 原始字节搜索 `MiuiNotificationMenuRow`、`createMenuViews`、`MiuiStatusBarNotificationActivityStarter`、`AppMiniWindowManager`、`launchMiniWindowActivity`、`isSubstituteNotification` 均未命中，符合“目标字符串位于压缩/不可直接搜索的 FS 中”的判断。
- 因此无法直接从这三份 ROM 提取 `SystemUI.apk`/JAR/DEX 并做反编译级 class/member 证据。

### 5.3 已拒绝/不可用的方法

| 方法 | 状态 | 原因 |
|---|---|---|
| full-ROM JADX | 未使用 | 违反本批范围，且 FS 未解析 |
| erofs Python 库 | 不可用 | pip 无成熟 erofs 解析包 |
| WSL / Linux erofs-fuse | 不可用 | 当前环境未安装 WSL，无 root 权限 |
| simg2img / lpunpack 二进制 | 不可用 | 未在 PATH 中找到 |
| payload_dumper.exe | 不适用 | 该工具针对 OTA `payload.bin`，非 fastboot `super.img` |

## 6. Issue #3 目标矩阵

| ROM | class | member | descriptor | resource | status |
|---|---|---|---|---|---|
| MIUI 14 CN xaga | `com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow` | `createMenuViews` | `(ZZ)V` | `R.string.system_notifrowmenu_openinfw` 等 | UNVERIFIED |
| MIUI 14 IN xaga | 同上 | 同上 | 同上 | 同上 | UNVERIFIED |
| HyperOS 1 ID veux | 同上（MIUI 14 baseline） | 同上 | 同上 | 同上 | UNVERIFIED |

`UNVERIFIED` 指：
- `super.img` 分区元数据已静态验证（partition table、`system_a`/`product_a` 存在）。
- 但目标 class/member/descriptor 在 ROM 字节中尚未被直接定位，缺少可独立审计的 APK/DEX/Smali 证据。

## 7. 回归假设

### H1：模块回归（`OpenNotifyInFloatingWindowHook`）

- 从 `1ea342d` 到 HEAD，该 hook 新增强制依赖 `StatusBarNotification#isSubstituteNotification()` 和 `StatusBarNotification#mPkgName`。
- 如果 HyperOS 1 的 `StatusBarNotification` 无此方法/字段，或字段为 `private`/`package-private` 导致 `findFieldIfExists` 不可见，hook 会提前 `return`，功能静默失效。
- 这与“同一个 ROM 在 r13.10.1 正常、r13.11.1 失效”一致。

### H2：ROM 目标变体（`MiuiNotificationMenuRow`）

- 如果问题指的是“扩展通知菜单”，则 HyperOS 1 中 `MiuiNotificationMenuRow` 的 `createMenuViews` 签名或所在包可能与 MIUI 14 不同。
- 当前 `NotificationRowMenuHook` 要求精确匹配 `com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow` 与 `MiuiNotificationMenuRow$MiuiNotificationMenuItem`。
- 该 hook 在 `1ea342d` 与 HEAD 之间无 diff；若 HyperOS 目标变体，r13.10.1 也应失效，除非用户使用的是早于 P1B-4A 重写的 r13.10.1 构建（`3dc0c1b` 之前）。

### H3：两者皆有

- 若用户同时启用了 `system_notifrowmenu` 和 `system_notify_openinfw`，则 H1 与 H2 可能同时影响体验。

## 8. 下一步最小修正建议（不实现）

针对 H1（最可能且 diff 最小）：
- 在 `SystemUINotificationHooks.OpenNotifyInFloatingWindowHook` 中恢复 `isSubstituteNotificationMethod` 与 `mPkgNameField` 的可选性。
- 当 `isSubstituteNotification()` 不存在时，回退到旧行为：视 `isSubstitute = false`。
- 当 `mPkgName` 字段不存在时，回退到 `StatusBarNotification#getPackageName()`（AOSP 标准 API）。
- 保持 `rethrowNotificationFatal` 对 `OutOfMemoryError` / `VirtualMachineError` 的透传，仅将普通异常降级为失败而不是强制 `return`。

预期变更文件：
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`

该改动最小、可回退，不会触碰 `SystemNotificationMoreHooks.kt` 的 P1B-4A 路径，也不引入新的 ROM target contract。

## 9. 证据等级

- 代码回归 diff：`STATIC_VERIFIED`
- ROM 分区结构：`STATIC_VERIFIED`
- ROM 内目标 class/member：`UNVERIFIED`
- 运行时行为（HyperOS 1 实机）：`UNVERIFIED`
- 修正后效果：`UNVERIFIED`
