# 米客_forA13

这是一个可独立安装的 MIUI 14 / Android 13 维护分支，源码基于 MonwF 的
[CustoMIUIzer v23.11.26](https://github.com/MonwF/customiuizer/releases/tag/v23.11.26)，
并迁移到 LSPosed 2.0 / Vector 2.0 使用的 libxposed API 101。

## 主要适配目标

- 机型：Redmi Note 11T Pro / Pro+（`xaga`）
- 系统：基于 Android 13 的 MIUI 14
- ROM：`V14.0.10.0.TLOINXM`、`V14.0.7.0.TLOCNXM`
- API：libxposed API 101
- 参考框架：
  - LSPosed `v2.0.4 (7741)`
  - Vector `v2.0 (3046)`，对应 [Actions run 29805285935](https://github.com/JingMatrix/Vector/actions/runs/29805285935)

其他 Android 13 的 MIUI 14 版本可能可用，但不属于首要验证范围。为了防止
在错误系统版本上加载大量 MIUI Hook，本分支会在运行时拒绝 Android API 33
以外的系统。

## API 101 迁移状态

- API 依赖升级为 `io.github.libxposed:api:101.0.1`，服务依赖为 `101.0.0`。
- 模块元数据声明 `minApiVersion=101`、`targetApiVersion=101`。
- 生命周期改为 `onModuleLoaded`、`onPackageReady`、
  `onSystemServerStarting`。
- Hook 统一通过 API 101 的 `HookBuilder` 和 `Chain.intercept` 注册，并使用
  `ExceptionMode.PASSTHROUGH` 保留原方法异常语义。
- 资源替换和系统权限相关 Hook 已迁移为原生 API 101 拦截器。
- 其余体量较大的 MIUI 功能暂时通过集中兼容层运行；该兼容层保留参数修改、
  提前返回、异常传播和 after 回调修改结果的行为，后续可以按功能组逐步迁移，
  不需要再次更换 API。

应用 ID 为 `name.monwf.customiuizer.a13`，可与上游版和独立 A14 版共存。
由于是不同应用，偏好设置不会自动互相复制。

## 构建

需要 JDK 17 和 Android SDK 36：

```bash
./gradlew :app:assembleRelease
```

如果上级目录存在 `keystore.properties`，构建会使用其中配置的正式签名；否则
会生成便于本地测试的 debug 签名 release APK。产物位于
`app/build/outputs/apk/release/`。

## 真机回归建议

1. 停用具有相同功能的 API 100 旧版，只启用本模块的默认作用域并重启。
2. 先确认设置界面能够读写远程偏好。
3. 分组验证系统界面、桌面、`system_server`、手机管家、省电与性能、安装器、
   截图和通话界面功能。
4. 某项功能异常时先关闭该项并导出完整 LSPosed/Vector 日志；两个目标 ROM
   之间可能存在 MIUI 类名或方法签名差异。

## 致谢与许可

本项目基于 Mikanoshi 与 MonwF 的 CustoMIUIzer，并独立维护 A13/API 101
迁移。项目使用 [GPL-3.0](LICENSE) 许可证。
