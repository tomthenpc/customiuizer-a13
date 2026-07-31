# LSPosed 日志离线分析

A13 只进行离线分析。脚本不调用 `adb`、`su`、`logcat`、网络或任何设备命令。

## 工具

```text
tools/analyze_lsposed_log.py
```

## 用法

```bash
python tools/analyze_lsposed_log.py log.txt
python tools/analyze_lsposed_log.py logs/
python tools/analyze_lsposed_log.py exported.zip
python tools/analyze_lsposed_log.py exported.zip --format markdown --output build/log-analysis/k1
python tools/analyze_lsposed_log.py exported.zip --format json --output build/log-analysis/k1
```

## 输入

- `.txt` / `.log` 文件
- 目录（递归搜索 `.txt` / `.log`）
- `.zip` 压缩包（直接解压 `.txt` / `.log`）
- 多文件组合

## 输出

### `text`（默认）

直接输出摘要到 stdout。

### `markdown`

写入 `{output}/summary.md`。

### `json`

写入 `{output}/summary.json`。

## 分析项

- A13 marker
- 模块版本
- processName / 目标进程
- system_server / SystemUI / Launcher / Settings / SecurityCenter
- HookDiagnostics
- `Empty preferences!` / `EMPTY_PENDING`
- Receiver 注册/注销失败
- stale Receiver
- `ClassNotFound`
- `NoSuchMethod`
- `NoSuchField`
- `InvocationTargetException`
- DexKit
- SystemUI / Launcher / system_server 崩溃
- 异常指纹和重复次数
- 首次/最后时间
- 可能源码类

## 安全边界

- 不修改用户原始日志。
- 不将日志路径写死到仓库。
- 不将包含个人信息的日志片段提交到 Git。
- 大文件按行流式读取，指纹字典有硬上限。
