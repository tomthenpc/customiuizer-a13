# LSPosed 完整日志审查协议

> 动态路径：每次审查前由用户传入 `full.log` 或 LSPosed 日志文件路径。  
> 本文件不得写死任何 Windows、macOS 或 Linux 绝对路径。

## 使用方式

每次审查时，在调用处提供日志文件路径：

```powershell
$logPath = '<用户每次传入的绝对路径>'
```

禁止在本文件中存储或假设路径，例如：

```text
C:\Users\tv\...\full.log
/data/adb/lspd/log/full.log
```

## 审查清单

对传入的日志执行以下检查，按优先级处理：

1. **模块加载**
   - 搜索 `Loading modules for ...` 确认目标进程加载了 A13 模块。
   - 确认无 `Failed to load module` 或 `NoSuchMethodError`。

2. **入口与 scope**
   - 确认 `name.monwf.customiuizer.MainModule` 出现在加载日志中。
   - 确认 scope 包含 `com.android.systemui`、`com.miui.home`、`android`、`com.android.settings` 等目标。

3. **Hook 失败与异常**
   - 搜索 `Hook failed`、`NoSuchFieldError`、`NoSuchMethodError`、`ClassNotFoundError`。
   - 记录首次出现的目标类名和方法签名。

4. **RemotePreferences 与 provider**
   - 搜索 `Empty preferences!` 不应刷屏，应只出现一次。
   - 搜索 `RemotePreferences`、`IllegalArgumentException`、`SecurityException`。

5. **SystemUI / Launcher 生命周期**
   - 搜索 `CustoMIUIzer` 标签在 SystemUI 重启、主题切换、横竖屏切换后的行为。
   - 检查无 ANR、`Slow operation` 或与 `BatteryIndicator` 相关的频繁异常。

6. **资源 Hook**
   - 搜索 `ResourceHooks` 相关异常。
   - 确认功能关闭时未注册无关资源 Hook（无对应包名的 `setResReplacement` 调用）。

7. **Locale / 设置应用**
   - 语言切换后确认 `AppLocaleController` 无 NPE 或重复设置日志。

8. **搜索**
   - 进入搜索、返回首页、旋转等操作后确认 `MainFragment`/`SearchNavigation` 无异常循环。

## 输出格式

每次审查后记录：

```text
- 日志文件：$logPath
- 审查时间：<ISO 8601>
- 模块加载：是/否
- 关键异常：<首次出现项>
- 实机功能状态：<用户描述 + 日志证据>
- 是否阻塞发布：是/否
- 下一步：<需要修复 or 继续实机验证>
```

## 安全边界

- 不修改用户原始日志文件。
- 不将日志路径写死到仓库文件。
- 不将包含个人信息的日志片段提交到 Git。
