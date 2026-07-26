# 米客 A13

`CustoMIUIzer-A13` 是 [CustoMIUIzer](https://github.com/MonwF/customiuizer) 上游版本 `v23.11.26` 的一个长期独立维护分支，面向 MIUI 14 / Android 13 设备继续演进。

> **注意**：A14 分支（`customiuizer-a14`）仅作为**工程方法**（Kotlin DSL、version catalog、CI、测试方式）的参考，不能直接作为 A13 的 ROM 事实来源。A13 的 MIUI 类名、方法签名、Hook target、资源 ID、SystemUI / Launcher 结构、ROM 判断、preference key、Manifest 组件等均未从 A14 复制。

## 主要适配目标

- **机型**：Redmi Note 11T Pro / Pro+（`xaga`）
- **系统**：MIUI 14 / Android 13（API 33）
- **目标 ROM**：`V14.0.10.0.TLOINXM`、`V14.0.7.0.TLOCNXM`
- **应用 ID**：`tv.withaibuild.customiuizer.r13`
- **ABI**：`arm64-v8a`
- **libxposed**：`minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
- **参考框架**：LSPosed 2.0 / Vector 2.0

其他 Android 13 的 MIUI 14 版本可能可用，但不属于首要验证范围。

## 与上游和 A14 的关系

- **上游**：功能语义以 `MonwF/customiuizer v23.11.26` 为固定基线。
- **A13 独立**：应用 ID、签名、版本号、构建系统与上游和 A14 完全隔离，互不覆盖安装。
- **A14 仅作参考**：A14 在 Kotlin DSL、version catalog、AGP/Kotlin 工具链、CI 结构、API 101/102 兼容方法、测试方式上可作为工程参考，但不能提供 A13 的运行事实。

## 构建

环境要求：JDK 17、Android SDK（包含 API 36 编译平台和 build-tools 37.0.1）。

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release / Develop 构建要求项目根目录上级存在 `../keystore.properties`，并指向 A13 正式签名（`C:/Users/tv/Documents/buildkey/r13/customiuizer-a13-release.p12`）。缺少正式签名时构建会明确失败，不会回退到 Android Debug 签名。Debug 构建和普通测试不受影响。

本地默认构建产物目录：

- Debug：`app/build/outputs/apk/debug/`
- Release：`app/build/outputs/apk/release/`

> 当前版本 `r13.2.0-devin` 由于 AGP 8.7 VariantOutput API 限制，APK 输出名使用 AGP 默认 `app-<variant>.apk`，后续将恢复为 `CustoMIUIzer-A13-r13.x.x.apk`。

## 验证

推荐的本地验证组合：

```bash
./gradlew clean :app:test :app:lintRelease :app:assembleDebug :app:assembleRelease
```

验证内容：

- 单元测试（`ModuleMetadataTest` 校验 `module.prop` / `java_init.list`）
- `lintRelease`
- Debug 与 Release 编译
- Release R8 + resource shrink
- zipalign 对齐
- `META-INF/xposed/module.prop`：`minApiVersion=101`、`targetApiVersion=102`、`staticScope=false`
- `META-INF/xposed/java_init.list`：`name.monwf.customiuizer.MainModule`
- `applicationId = tv.withaibuild.customiuizer.r13`，`minSdk=33`，`targetSdk=34`
- Release APK v2 签名证书 CN=`CustoMIUIzer A13`

## 安装与启用

1. 卸载或停用旧签名的 A13 版本（原私钥已遗失，旧版本无法覆盖安装）。
2. 安装新的 APK，在 LSPosed / Vector 中启用默认作用域并重启。
3. 先确认设置界面可以读写远程偏好。
4. 按功能组分别验证 System UI、桌面、system_server、手机管家、省电与性能、安装器、截图、通话界面。
5. 某项功能异常时，关闭该项并导出完整框架日志；不同 ROM 之间可能存在 MIUI 类名或方法签名差异。

## 主要功能分类

与上游 `v23.11.26` 对应的功能分组保持一致：

- 系统：锁屏、状态栏、控制中心、闹钟、充电动画、截图、壁纸等
- 桌面：图标、手势、Dock、抽屉等
- 通话与联系人：通话界面亮度、隐藏功能等
- 各种：全局手势、滚动到顶、浮动窗口、Tasker 等
- 设置界面与偏好存储

具体功能以上游 `v23.11.26` 为准；本分支优先保留其 A13 行为。

## 路线图与已知限制

- ✅ 新 A13 长期签名已建立并验证
- ✅ 构建系统迁移到 Kotlin DSL + version catalog
- ✅ API 101/102 兼容元数据
- ✅ 单元测试、Lint、CI 基线
- 🔄 生命周期与高频 Hook 的系统性治理（按功能组推进）
- 🔄 低风险 Kotlin-first 迁移（已完成部分 Activity 入口，继续按功能组推进）
- ⏳ 输出 APK 命名恢复为 `CustoMIUIzer-A13-r13.x.x.apk`
- ⏳ 实机回归验证（需要目标 ROM 设备）

## 许可证与致谢

基于 [Mikanoshi](https://github.com/Mikanoshi) 与 [MonwF](https://github.com/MonwF) 的 CustoMIUIzer。A13 独立维护分支使用 [GPL-3.0](LICENSE) 许可证。