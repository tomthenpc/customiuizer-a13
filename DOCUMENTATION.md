# A13 文档规则

## 根目录长期文档

- `AGENTS.md`：Devin 执行规则；
- `PROJECT.md`：产品和工程边界；
- `ARCHITECTURE.md`：稳定架构；
- `WORKFLOW.md`：任务生命周期；
- `ROADMAP.md`：Now / Next / Later；
- `COMPATIBILITY.md`：A13 平台和移植状态；
- `DOCUMENTATION.md`：本规则。

## docs/

只允许：

- 当前仍有效的构建与验证说明；
- 少量长期兼容说明；
- 架构决策记录。

禁止重新引入：

- Review 报告堆；
- Implement 报告堆；
- 每阶段状态文档；
- 固定 HEAD 清单；
- 重复 parity 表；
- 可由 Git 历史回答的流水账。

## 更新条件

只有当长期事实改变时更新根目录文档。单个任务的过程只写在任务合同；完成后压缩为
结果和证据。
