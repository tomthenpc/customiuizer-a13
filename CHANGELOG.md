# 更新日志

## r13.9.1（2026-08-01）

### 兼容与诊断

- 将功能 Contract、目标 Resolver 与真实 installer 绑定到同一 ClassLoader 和同一 target variant，禁止混合 MIUI 14 / HyperOS 候选成员；
- 补齐 `STATUSBAR_CLOCK_TWEAK` 安装记录，使 Contract operation、真实 Hook operation、失败目标与异常类型可闭环核对；不降低 REQUIRED，也不添加猜测式 fallback；
- 修复 ResourceHooks getter 参数展开，消除旧日志中的参数形态错误，并把 method kind 绑定移出高频回调；
- 增加 HyperOS 1 / Android 13 ROM 能力识别与诊断。候选目标缺失时安全跳过，仍需实机 LSPosed 日志确认具体 ROM bundle。

### 稳定性、内存与性能

- 普通 Hook/反射/异步异常按功能隔离；直接或反射包装的 `OutOfMemoryError` 保持向外抛出；
- 加固 Receiver、Preference、计步器、设备监控、AudioVisualizer、锁屏专辑图、电池指示器与延迟输入事件的注册、取消和生命周期边界；
- 对状态栏时钟、网速、通知、控制动作、Launcher 和资源替换热路径减少重复反射、正则、临时集合、参数数组与回调对象；
- 按需初始化共享 Handler、资源 Hook 和偏好设施，关闭功能时不创建无关后台工作；
- 优化安全/隐私应用列表的数据更新和图标请求，降低重复查找与短生命周期对象数量。

### 界面与交互

- 缩短页面切换动画并优化 Preference 点击分发；
- 开关点击立即更新可见状态并保留无障碍语义，降低连续点击时“无反馈”的感知；
- 修复 View 销毁后的延迟输入、回调和引用清理，避免旧界面继续响应。

### 验证边界

- 完整静态验证、Kotlin/Java 编译、JVM 单元测试、Python 工具测试和 Lint 作为发布门禁；
- 正式 APK 已核验版本、SHA-256、A13 专用签名证书、zipalign、Xposed 元数据和 `debuggable=false`；
- MIUI 14 / Android 13 保留既有实机稳定基线；本版本新增改动及 HyperOS 1 / Android 13 仍需新的 LSPosed 详细日志验证。

### 正式产物

- APK：`CustoMIUIzer-A13-r13.9.1.apk`（`2,860,194` bytes）；
- APK SHA-256：`98F03BFB1FA29E776C3A638E771CCE6D1672F5C94F91B39B7D7D4362DB6EF96C`；
- 签名证书 SHA-256：`15CE32F03E4D8E62DF9390F77431862E59BF2CF95CD5A72F0C7330CDFCCA2934`；
- r13.8.6 使用了不同的历史证书，不能直接覆盖安装 r13.9.1；请先备份模块设置，再卸载旧版并安装本版。后续 A13 正式版固定使用本证书。

## 历代核心实现总结

本项目从 Android 13/libxposed API 101 独立移植开始，逐步完成独立包名与签名、API 101/102 元数据、System/SystemUI/Launcher 领域拆分、Java 到 Kotlin 的小批量等价迁移、RemotePreferences 与资源 Hook 加固、Receiver/Observer/Handler 生命周期治理、有界图标与媒体缓存、设置搜索和异步任务 latest-wins、防 OOM 边界、功能 Catalog/Contract/Resolver/诊断框架，以及状态栏、锁屏、通知、桌面和设置界面的持续性能优化。详细历史可通过保留的 Git tag 和提交记录追溯。
