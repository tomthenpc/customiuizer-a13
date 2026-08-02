# Verified Tree Transaction v6

```text
DocumentKind: CURRENT
Product: A13
Source: A13_A14_PROGRESS_CONTROL_FINAL_V6
```

## 1. 当前缺陷

无法可靠执行：

```text
运行 Full
→ 修改 SMART_OPERATION_STATE 中 LastFullVerificationCommit
→ 再提交
```

因为最后一次状态提交产生了新的 HEAD，新的 HEAD 并不是刚才 Full 验证的 commit。

## 2. 以 tree 为证据

新增字段：

```text
LastVerifiedTree:
LastVerifiedMode:
LastVerifiedAt:
LastVerifiedCommandsDigest:
```

验证流程：

```text
完成全部代码/state/docs
→ stage 精确文件
→ git write-tree
→ 得到 STAGED_TREE_SHA
→ 在 staged snapshot/等价导出树上运行 checker 和验证
→ 将 tree SHA 写入 commit trailer 或外部验证记录
→ commit
→ 确认 commit tree == STAGED_TREE_SHA
→ push
```

不要在 commit 后再创建“记录 Full commit”的 state-only commit。

推荐 trailer：

```text
Verified-Tree: <40-char tree sha>
Verification: targeted,fast
Full-Verification: pass | not-required
```

## 3. SMART state 的 SHA 规则

以下字段不再人工维护自指向 commit：

```text
LastQualifyingCheckpoint
LastFullVerificationCommit
LastStandardSweepCommit
LastDeepSweepCommit
```

选择一种实现：

### 推荐

由 `tools/progress_snapshot.py` 从 Git trailers 和 commit history 动态派生，不作为人工事实写入 state。

### 兼容方案

使用：

```text
LastQualifyingCheckpoint: HEAD
LastFullVerificationTree: <tree sha>
```

checker 在 commit 后验证 `HEAD^{tree}` 匹配。

禁止 state-only 修正 SHA 循环。

## 4. 最终 HEAD

每次 push 后只做只读检查：

```text
local HEAD == upstream HEAD
HEAD tree == Verified-Tree
working tree clean 或剩余修改已分类
CI state 正确
```

只读检查结果不需要再修改仓库状态。
