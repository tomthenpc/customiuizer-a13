# CustoMIUIzer A13 项目章程

## 目标

维护 Android 13 专用 CustoMIUIzer 分支，为 MIUI 14 提供稳定、低开销的系统定制，
并在真实证据支持下扩展 HyperOS 1 / Android 13 兼容性。

## 固定边界

- applicationId：`tv.withaibuild.customiuizer.r13`；
- Android 13 / API 33；
- 独立版本、签名、发布和兼容策略；
- 不支持 Android 14+；
- 不与 A14 合并 APK、生产源码树或运行时依赖。

## 优先级

```text
不崩溃与可启动
> 已有用户行为不回归
> 用户明确缺陷
> 长期搁置功能
> ROM 兼容
> 性能、耗电和内存
> 结构与语言现代化
```

## 开发模型

- 用户确定目标和体验；
- ChatGPT 检查代码、编写任务合同、审查最终 diff；
- Devin 按合同实现、验证和构建；
- `tools/verify.py`、单元测试和 Gradle 作为客观门禁；
- Git 当前分支是唯一真实工作状态。

## 非目标

- 用无限 Runtime Hardening 阶段替代功能推进；
- 追求 Java→Kotlin 百分比；
- 为与 A14 逐行一致而破坏 A13；
- 通过增加文档数量证明安全；
- 把未实机验证的 HyperOS 兼容写成稳定支持。
