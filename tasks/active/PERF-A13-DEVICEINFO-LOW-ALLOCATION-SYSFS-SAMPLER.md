# PERF-A13-DEVICEINFO-LOW-ALLOCATION-SYSFS-SAMPLER

## 目标

降低启用状态栏电池详情或设备温度时的周期性 CPU、I/O、对象分配和 GC 压力。

## 范围

- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoSysfsParser.kt`
- 相关 JVM 测试
- 静态合同测试 `tools/tests/test_device_info_low_allocation_sampler.py`

## 关键合同

- 2 秒采样间隔、60 秒失败退避、屏幕状态控制保持不变。
- 移除 `Properties`、`RandomAccessFile`、`ReadResult`、`IconUpdate` 与 per-tick `PowerManager.isInteractive`。
- 新增纯 ASCII sysfs parser，复用固定 `ByteArray`，使用 `Message.arg1/arg2/obj` 传递 UI 更新。
- 保持原 UI type 91/92、文本格式、fallback 数值与 sysfs 路径。

## 验收

- [ ] `DeviceInfoSysfsParser` 与修改后的 `DeviceInfoMonitor` 编译通过
- [ ] JVM 测试全部通过
- [ ] Python 静态/行为测试全部通过
- [ ] mutation 验证失败并恢复
- [ ] lint / source hazard / verify 通过
- [ ] 三笔提交：active docs、perf、complete docs

## Base SHA

`311f9ab2995744d853df832f05089743a326302f`
