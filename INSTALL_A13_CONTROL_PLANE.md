# 安装并启动 A13 最终自治控制层

## 1. 源目录

推荐解压到：

```text
C:\Users\tv\Downloads\A13_Autonomous_Control_Plane_FINAL_v2
```

必须包含：

```text
GOAL.md
AGENTS.md
TASK_STATE.md
DEVIN_START_PROMPT.md
INSTALL_A13_CONTROL_PLANE.md
scripts\verify.ps1
scripts\bootstrap-and-start.ps1
```

---

## 2. 唯一目标

仓库：

```text
tomthenpc/customiuizer-a13
```

分支：

```text
devin/a13-rom-intelligence-audit
```

分支必须精确匹配。不得使用 wildcard，不得新建分支，不得合并 `main`。

---

## 3. 零日常干预启动方式

只需向当前 A13 Agent 发送一次：

```text
在当前 A13 仓库执行以下脚本并在成功后立即继续长期自治任务，不等待我的常规确认：

powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Users\tv\Downloads\A13_Autonomous_Control_Plane_FINAL_v2\scripts\bootstrap-and-start.ps1" -SourceRoot "C:\Users\tv\Downloads\A13_Autonomous_Control_Plane_FINAL_v2"

脚本完成后：

1. 完整读取仓库内 DEVIN_START_PROMPT.md。
2. 将其中 fenced 指令作为持续执行合同。
3. 立即执行 P0.1。
4. 自己修改、自己运行、自己测试、自己发现问题、自己修复、自己提交、自己 push、自己检查 CI，并持续到 MACHINE_COMPLETE。
5. 除真实设备、ROM 样本、签名材料、权限或产品决策外，不再要求我手动确认。
6. 只允许分支 devin/a13-rom-intelligence-audit。
7. 不创建新分支，不合并 main，不 tag，不 release。
```

---

## 4. 脚本行为

`bootstrap-and-start.ps1` 会：

- 定位当前 Git 仓库；
- 验证 `origin`；
- 精确验证当前分支；
- 验证没有 unfinished Git operation；
- 验证七个源文件存在；
- 只复制控制层文件；
- 比较 SHA-256；
- 运行控制层 Audit；
- 只暂存控制层文件；
- 创建独立治理提交；
- 只 push 到授权分支；
- 再次运行 Audit；
- 输出下一步要求。

它不会：

- reset；
- clean；
- 删除本地代码；
- 覆盖业务文件；
- 合并 main；
- 创建新分支；
- force-push；
- tag/release。

---

## 5. 未来更换分支

不要将分支改成模糊匹配。

未来确需更换分支时，由仓库所有者创建独立治理变更，同时修改：

```text
GOAL.md
AGENTS.md
DEVIN_START_PROMPT.md
INSTALL_A13_CONTROL_PLANE.md
scripts/verify.ps1
scripts/bootstrap-and-start.ps1
```

在此之前，其他分支均无写权限。
