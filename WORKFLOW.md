# A13 工作流

## 任务启动

从 `tasks/TASK_TEMPLATE.md` 创建一个 active 任务。任务合同必须包含目标、范围、
必须保持、非目标、验收和验证命令。

不创建 Review/Implement 双任务。

## Explore

只读取目标进程、installer、feature、Hook/Controller、preference 和相关测试。
找到可信修改路径后立即进入实现。

## Implement

直接修改实现、测试和必要文档。普通技术决策由 Devin 自行完成。范围扩大时必须写入
当前任务，不创建隐形子项目。

## Verify

1. 运行最小针对性测试；
2. 运行 `python tools/verify.py fast --changed`；
3. 修复失败；
4. 收口时运行完整门禁；
5. 需要产物时再构建 APK。

## Review

ChatGPT/人工只审最终 diff：

- 行为；
- Hook 时序；
- 兼容边界；
- 热路径；
- 生命周期；
- 测试真实性；
- 未授权范围。

Review 问题在原任务内修复。

## Done

移动到 `tasks/completed/`，只保留结果和证据。更新 ROADMAP 的状态，不复制长报告。

## Blocked

仅允许外部阻塞：

- 必需设备日志缺失；
- 私有依赖不可访问；
- 正式签名缺失且任务要求 Release；
- 必须由用户决定产品行为。

编译错误、测试失败和实现困难不是 Blocked。
