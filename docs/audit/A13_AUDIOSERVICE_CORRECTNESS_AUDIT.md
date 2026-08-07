# A13 AudioService Hot-Path Correctness Audit

本文件记录 P1B-2（AudioService hot-path 优化）在 QA 阶段的行为等价性审计结果。

## 1. 任务元数据

| 字段 | 值 |
|------|-----|
| 任务编号 | `A13-PERF-QA-1` / `P1B-2` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 当前 HEAD | `0ca93c2`（由 `git rev-parse --short HEAD` 确认） |
| 基线基座 SHA | `3d38cdd53a6190c68187a803badaf201dfda25cd` |
| 原始工程实现 SHA | `74b54e5c525aa3059bf3e88667f63f558ac7260f` |
| 原始关闭点 SHA | `5f780b8a15727114bd29f01188191a2520ff2509` |
| 说明 | 治理记录中曾误用 `fec4ee6` 作为参考点；该 SHA 无效，本审计不使用。 |

## 2. 审计范围

- `VolumeStepsHook`（目标 `AudioService#createStreamStates`）
- `NotificationVolumeServiceHook` 中的 `AudioService$VolumeStreamState#readSettings` 替换逻辑

不处理 `updateStreamVolumeAlias`、`shouldZenMuteStream`、音量 UI、Tile、通知面板、Launcher 等其他 AudioService 路径。

## 3. VolumeStepsHook 行为矩阵

来源：

- 基线：`app/src/test/java/tv/withaibuild/customiuizer/mods/LegacyAudioServiceBehaviorOracle.kt` 第 18-27 行
- 当前：`app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVolumeHooks.kt` 第 126-141 行

| 审计点 | 基线 (legacy oracle) | 原始工程版 | 当前 HEAD (`0ca93c2`) |
|--------|---------------------|------------|-----------------------|
| 偏好读取时机 | callback-time `MainModule.mPrefs.getInt("system_volumesteps", 0)` | install-time/外部闭包捕获 | install-time 读取 `mult`（第 129 行），callback 只执行数组乘法 |
| Hook 安装时机 | 无 Hook，测试直接调用 oracle | `system_volumesteps > 0` 时安装 | `mult > 0` 时安装（第 130 行），否则不安装 |
| 反射解析时机 | callback-time `findClass`/`getStaticObjectField` | install-time 缓存 Class/Field | install-time `findClassIfExists`/`findFieldIfExists`（第 127-128 行） |
| 回调副作用 | 读取 `MAX_STREAM_VOLUME`、数组缩放、set 回写 | 原地修改并回写 | 原地修改并通过 `maxStreamVolumeField.set(null, ...)` 回写（第 138 行） |
| 是否 skip original | 不涉及；before callback 不中断原方法 | 不 skip | 不 skip |
| 异常行为 | 反射异常直接向上传播 | 安装失败直接 return；callback 无显式 catch | 安装缺失目标 `?: return`；callback 无显式 catch |
| 锁 | 无 | 无 | 无 |
| 重复回调行为 | 按当前 `mult` 重复缩放，数组累计缩放 | 与基线一致，但 `mult` 已固定 | 重复 `createStreamStates` 仍按 install-time `mult` 累计缩放，与基线一致 |
| 降级行为 | `mult <= 0` 不处理 | `mult <= 0` 不安装 | `mult <= 0` 不安装（第 130 行） |
| fatal 行为 | 无专门 fatal 处理 | 无专门 fatal 处理 | 本 hook 未调用 `rethrowAudioFatal`；安装阶段异常自然传播 |

**契约说明**：当前实现将 `mult` 在 install 阶段捕获，是因为 `volumeSteps` 为 `REBOOT` 生效特性；安装后修改偏好值不应影响当前启动周期。该行为在 `volumeSteps_preferenceMutationAfterInstall_contract` 中显式记录，不是与基线的漂移。

## 4. NotificationVolume readSettings 行为矩阵

来源：

- 基线：`app/src/test/java/tv/withaibuild/customiuizer/mods/LegacyAudioServiceBehaviorOracle.kt` 第 29-56 行
- 当前：`app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVolumeHooks.kt` 第 143-293 行

| 审计点 | 基线 (legacyReadSettings) | 原始工程版 | 当前 HEAD (`0ca93c2`) |
|--------|--------------------------|------------|-----------------------|
| 偏好读取时机 | 无 `system_separatevolume` 门控；测试使用 `MainModule.mPrefs` | install 时确认 `system_separatevolume` | install 时确认 `system_separatevolume` 后安装 |
| Hook 安装时机 | 无 Hook | `system_separatevolume` 启用时安装 | 由 `FeatureDispatcher` 根据偏好安装 |
| 反射解析时机 | callback-time `findClass`/`getObjectField`/`callMethod`/`callStaticMethod` | install-time 解析 Class/Field/Method | install-time 解析所有 Class/Field/Method（第 144-205 行） |
| 回调副作用 | 为 `streamType == 1` 的流按 `DEVICE_OUT_ALL_SET` 重建 `mIndexMap` | 重建 `mIndexMap`，完成后 skip original | 重建 `mIndexMap`，成功后 `mIndexMapField.set` + `param.returnAndSkip(null)`（第 290-291 行） |
| 是否 skip original | `streamType != 1` 返回 false；成功返回 true；异常抛出时原方法继续 | `streamType != 1` 不 skip；成功 skip；单设备异常 `continue` 后仍 skip（与基线不一致） | `streamType != 1` 不 skip；成功 skip；单设备异常 `throw t` 不 skip，原方法继续（第 265-267、273-275、280-283 行） |
| 异常行为 | `XposedHelpers` 调用异常直接抛出；返回 null 时 `continue` | 单设备异常 `continue` 并可能最终 skip；`mIndexMapField.set` 可能被吞 | 单设备异常 `rethrowAudioFatal(t); throw t`；null 结果 `continue`；`mIndexMapField.set` 不吞异常 |
| 锁 | `synchronized(volumeState.javaClass)`（第 32 行） | `synchronized(volumeStreamStateClass)`（缓存类对象） | `synchronized(param.member.declaringClass)`（第 259 行），与基线一致 |
| 重复回调行为 | 每次 callback 重建 map | 每次 callback 重建 map | 每次 callback 重新填充同一 `mIndexMap` 实例，实例间独立 |
| 降级行为 | `streamType != 1` 直接返回；其他缺失返回 false | `streamType != 1` 直接 return；反射成员缺失不安装；单设备失败 `continue`（偏离基线） | `streamType != 1` 直接 return；反射成员缺失不安装；单设备失败 `throw t` 使原方法执行，部分 map 与基线一致 |
| fatal 行为 | 无专门 fatal 处理 | 无 `rethrowAudioFatal` | 通过 `rethrowAudioFatal` 向上抛出 `OutOfMemoryError`/`ThreadDeath`/`VirtualMachineError`（包括被包装在 cause 链中前 8 层），非 fatal 继续抛出 `t`（第 113-123 行） |

## 5. 对当前 HEAD 应用的修正（相对原始工程版）

1. **新增 `rethrowAudioFatal(t)` 辅助方法**：
   - 捕获链遍历最多 8 层；遇到 `VirtualMachineError`（含 `OutOfMemoryError`）、`ThreadDeath` 直接重新抛出。
   - 被包装的致命异常也能逃逸，避免热路径中吞掉 OOM。

2. **`synchronized` 锁对象恢复为 `param.member.declaringClass`**：
   - 原始工程版为减少反射调用改为 `volumeStreamStateClass`（缓存的 `Class` 对象）。
   - 当前恢复为 `param.member.declaringClass`，其语义等价于 `AudioService$VolumeStreamState` 类对象，与基线 legacy 一致。

3. **单设备反射失败由 `continue` 改为 `throw t`**：
   - `getSettingNameForDevice`、`Settings.System.getIntForUser`、`getValidIndex` 的 `Method.invoke` 抛出非 fatal 异常时，先 `rethrowAudioFatal(t)`，再 `throw t`。
   - 这样 hook 不会 `returnAndSkip`，原方法继续执行；框架只记录一次异常。
   - 由于异常抛出前已经成功写入的部分设备数据仍保留在 `mIndexMap` 中，部分 map 与 legacy 行为一致。

4. **`mIndexMapField.set` 不再被吞异常的 try/catch 包裹**：
   - 最终 map 回写要么成功，要么按真实异常传播，不再出现"写回失败但仍 skip original"的状态不一致。

## 6. 测试证据

### 6.1 `AudioServiceHotPathCallbackTest.kt`

- `volumeStepsHook_modifiesMaxStreamVolume`：`system_volumesteps=150` 时 `createStreamStates` `before` 回调按 150% 缩放 `MAX_STREAM_VOLUME`。
- `volumeStepsHook_doesNotInstallWhenPreferenceIsZero`：`system_volumesteps=0` 时不安装 Hook。
- `readSettingsHook_returnsEarly_forNonStreamType1`：`mStreamType != 1` 时直接返回，不请求设备类型，不 skip original。
- `readSettingsHook_buildsIndexMap_forStreamType1`：`mStreamType == 1` 时按 `AudioSystem.DEVICE_OUT_ALL_SET` 遍历设备，调用 `getValidIndex(50, true)` 并 skip original。
- `readSettingsHook_keepsIndexMapEmpty_forMissingSettingsValue`：非默认设备缺少设置值时仅默认设备写入 map。

### 6.2 `AudioServiceHotPathCorrectnessTest.kt`

- `volumeSteps_preferenceMutationAfterInstall_contract`：安装后修改 `system_volumesteps`，当前仍使用 install-time `mult`；legacy 使用 callback-time `mult`；记录该可接受契约。
- `volumeSteps_repeatedCreateStreamStates_preservesLegacyBehavior`：重复触发 `createStreamStates` 时 `MAX_STREAM_VOLUME` 累计缩放与 legacy 一致。
- `volumeSteps_multiples`：验证 1/50/100/150/120 多种倍率下的缩放结果。
- `volumeSteps_multGreaterThan100NoClamp`：200% 倍率不截断。
- `volumeSteps_negativeOrZeroDoesNotInstall`：负值/零值不安装 Hook。
- `readSettings_fullSuccess_matchesLegacyIndexMap`：`streamType==1` 完整成功时当前 `mIndexMap` 与 legacy oracle 一致。
- `readSettings_missingSettingValue_matchesLegacyDefaultDeviceOnly`：缺省设置值时仅默认设备写入，map 与 legacy 一致。
- `readSettings_streamTypeNot1_doesNotSkipOriginal`：`streamType != 1` 不 skip original，不操作 map。
- `readSettings_multipleVolumeStreamState_instancesAreIndependent`：不同 `VolumeStreamState` 实例状态互不影响。
- `readSettings_getSettingNameForDeviceFailure_doesNotSkipOriginalAndMatchesLegacyPartialMap`：`getSettingNameForDevice` 异常时当前不 skip original，部分 map 与 legacy 一致。
- `readSettings_getIntForUserFailure_doesNotSkipOriginalAndMatchesLegacyPartialMap`：`Settings.System.getIntForUser` 异常时行为一致。
- `readSettings_getValidIndexFailure_doesNotSkipOriginalAndMatchesLegacyPartialMap`：`getValidIndex` 异常时行为一致。
- `rethrowAudioFatal_directOutOfMemoryError_rethrows`：直接 `OutOfMemoryError` 被抛出。
- `rethrowAudioFatal_wrappedOutOfMemoryError_rethrowsCause`：包装后的 `OutOfMemoryError` 被抛出。
- `rethrowAudioFatal_wrappedThreadDeath_rethrowsCause`：包装后的 `ThreadDeath` 被抛出。
- `rethrowAudioFatal_wrappedOrdinaryRuntimeException_doesNotRethrow`：普通 `RuntimeException` 不抛出。
- `readSettings_lockOwner_matchesMethodDeclaringClass`：`synchronized` 锁对象为 `param.member.declaringClass`。
- `readSettings_skipOriginal_matrix`：`streamType != 1`、完整成功、单设备失败三种情况下的 skip 契约。
- `rethrowAudioFatal_withoutCauseTraversal_mustFail_wrappedFatal`：确认 cause 链遍历逻辑存在，否则包装 fatal 会被错误吞掉。

## 7. 验证结果

| 验证项 | 命令 | 状态 |
|--------|------|------|
| Android 单元测试 | `gradlew :app:testDebugUnitTest` | PASS |
| 快速验证（AudioService 相关测试） | `python tools/verify.py fast --tests AudioServiceHotPath` | PASS |
| 快速验证（变更） | `python tools/verify.py fast --changed` | PASS |
| 完整验证 | `python tools/verify.py full` | PASS |
| Python 工具编译 | `python -m compileall tools` | PASS |
| Python 单元测试 | `python -m unittest discover -s tools/tests -p "test_*.py"` | PASS |
| 构建 legacy exception registry | `tools/build_legacy_exception_registry.py --build` | PASS |
| Hook 成本扫描稳定性 | `tools/a13_hook_cost_scan.py --verify-stability` | PASS |

## 8. P1B-4A 冻结状态

- `A13-PERF-P1B-4A` 仍处于 `QA_CONDITIONAL` 冻结状态。
- 阻塞项：`ROM_LIFECYCLE_EVIDENCE_REQUIRED`。
- 本次提交仅修正其任务元数据中的 commit SHA，不修改功能结论与验收状态。
