# FIX-settings-entrypoint-controlled-diagnostics

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

移除六个 settings-app 入口/UI 文件中的 6 处 `.printStackTrace()`，统一接入 `tv.withaibuild.customiuizer.utils.SettingsDiagnostics`。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/AboutFragment.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/PrefsProvider.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- `tools/tests/test_settings_entrypoint_diagnostics.py`
- `docs/audit/SOURCE_HAZARD_BASELINE.json`
- `tasks/active/FIX-settings-entrypoint-controlled-diagnostics.md`
- `tasks/completed/FIX-settings-entrypoint-controlled-diagnostics.md`

## 完成摘要

六个目标文件中的 `.printStackTrace()` 已全部替换为 `SettingsDiagnostics.failure(...)`，并保留完整 fatal guard。`Credentials.kt` 内层 KeyStore fallback 未改动。所有验证通过，baseline 从 14 降至 8。

## 验证结果

- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：494 tests passed, 0 failed, skipped 2
- `python tools/verify.py fast --changed`：通过
- `python tools/verify.py full`：通过
- `python tools/source_hazard_scan.py --path app/src/main/java`：`8 reviewed, 0 new`
- `git diff --check`：通过
- `git status --short`：干净

## 完成记录

- Base SHA: 5618e29fb88255e7132b28f9305f8e3093cd3b75
- Implementation SHA: 783f4a0f6384c3e1ef2299702fbf7c6c4049bb5a
- Remaining baseline count: 8
