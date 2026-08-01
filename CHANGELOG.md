# 更新日志

## r13.9.2（2026-08-01）

### 内存与稳定性

- 锁屏专辑图所属 View 脱离窗口时，立即取消未完成任务，并释放模块设置的背景、单帧输出缓存和静态处理结果；保留源图引用供重新附着后按需生成，功能行为不变；
- 保持一条有界处理队列、latest-generation 发布门禁和中间 Bitmap 回收，避免已取消结果重新占用 SystemUI 内存。

### 界面与交互

- 设置页切换动画按 A14 当前实现调整为 `350ms`，恢复更易感知的页面节奏；
- 开关直接继承所在行的按压状态，按下即反馈；移除每次点击创建的透明度动画，减少临时渲染状态和连续点击干扰。

### 兼容与发布

- 未修改 MIUI 14 / Android 13 或 HyperOS 1 / Android 13 的 ROM Hook 目标、Contract 或 fallback；
- LSPosed 仓库增加独立 `SUMMARY`，模块列表只显示简洁摘要，不再展开整段 README；
- Kotlin/Java 编译、静态不变量、单元测试、Lint、版本、签名、zipalign、Xposed 元数据和 `debuggable=false` 作为发布门禁；新增改动仍需实机 LSPosed 日志确认。

## 历代核心实现总结

A13 版本线已完成独立包名与 A13 专用签名、libxposed API 101/102、System/SystemUI/Launcher 领域拆分、小批量 Kotlin 化、资源与偏好 Hook 加固、Receiver/Observer/Handler 生命周期治理、有界缓存、可取消异步任务、OOM 边界和 Contract/Resolver 兼容诊断。详细历史保留在 Git tag 与提交记录中。
