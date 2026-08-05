# PERF-A13-DEVICEINFO-LOW-ALLOCATION-SYSFS-SAMPLER — Completed

## 元数据

- Base SHA: `311f9ab2995744d853df832f05089743a326302f`
- Implementation SHA: `67b945fa163940b28ae343a6dfef4d3f1dea6c6d`
- Branch: `devin/a13-rom-intelligence-audit`

## 实现要点

- 新增 `DeviceInfoSysfsParser`：纯 Kotlin、无 Android 依赖，逐字节扫描 `ByteArray`，直接 ASCII 转 Int，支持 `\n`/`\r\n`、`+`/`-`、数值两侧空白、重复键 last-wins、Int 上下界溢出回 0。
- 重写 `DeviceInfoMonitor`：
  - 移除 `Properties`、`RandomAccessFile`、`ReadResult`、`IconUpdate`、per-tick `isInteractive` 调用。
  - 复用固定 `ByteArray`（8192 / 64 bytes）和 `BatteryValues`。
  - `readSysfsFile` 使用 `FileInputStream.use`，有界读取，detect buffer overflow，首句 `RuntimeFatality.throwIfFatal(t)`。
  - `runTick` 先 `isCurrentTick` gate 再 I/O。
  - UI 更新改为 `obtainMessage(UPDATE_MESSAGE, type, show, text)`，type 91/92 保持不变。
- 更新 JVM 测试：
  - `DeviceInfoSysfsParserTest` 24 项覆盖。
  - `DeviceInfoMonitorLifecycleTest` 新增屏幕状态 / stop gate 测试。
  - `SystemUIStatusBarHooksDeviceMonitorTest` 改为 primitive raw values。
- 新增 `tools/tests/test_device_info_low_allocation_sampler.py` 60 项静态合同测试。
- 同步 `docs/audit/A13_LEGACY_EXCEPTION_REGISTRY.json` inputDigest（异常处理模式变更）。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `python -m compileall tools` | 通过 |
| `python -m unittest discover -s tools/tests -p "test_*.py"` | 870 通过，2 skipped |
| `tools/tests/test_device_info_low_allocation_sampler.py` | 60/60 通过 |
| `python tools/verify.py full` | 通过（compile / test / lint 全通过） |
| `python tools/source_hazard_scan.py --path app/src/main/java` | `0 reviewed, 0 new` |
| `gradlew :app:testDebugUnitTest --dependency-verification=strict` | 通过 |
| 生产源码 `.printStackTrace(` 搜索 | 无输出 |
| `git diff --check` | 无报错 |

## Mutation 验证

所有 mutation 均临时引入、触发对应测试失败、并恢复，未进入提交：

1. parser 中 `String(buffer)` → `test_parser_has_no_string_constructor` 失败，已恢复。
2. `Properties().load(input)` → `test_monitor_does_not_call_properties_load` 失败，已恢复。
3. `runTick` 中 `ByteArray(1)` → `test_buffer_not_created_inside_functions` 失败，已恢复。
4. 清空 `BatteryValues.reset()` → `parseBatteryUeventResetsOutputOnEachParse` 失败，已恢复。
5. 重复键 first-wins → `parseBatteryUeventLastDuplicateWins` 失败，已恢复。
6. 删除 Int 溢出检查 → `parseBatteryUeventReturnsZeroForPositiveOverflow` 失败，已恢复。
7. `RandomAccessFile.readLine()` → `test_monitor_does_not_call_read_line` 失败，已恢复。
8. `runTick` 中 `powerManager?.isInteractive` → `test_run_tick_does_not_call_is_interactive` 失败，已恢复。
9. 恢复 `IconUpdate` data class → `test_no_icon_update_data_class` 失败，已恢复。
10. 删除 catch 中 `RuntimeFatality` → `test_read_sysfs_catch_first_statement_is_runtime_fatality` 失败，已恢复。
11. `BASE_DELAY_MS = 1_000L` → `DeviceInfoMonitorLifecycleTest.failuresBackOffToBoundAndSuccessRestoresBaseDelay` 失败，已恢复。
12. 删除 `SCREEN_OFF` `removeMessages` → `test_screen_off_removes_messages` 失败，已恢复。

## 状态

`STATIC_VERIFIED`
