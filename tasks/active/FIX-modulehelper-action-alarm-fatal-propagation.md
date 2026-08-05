# FIX-modulehelper-action-alarm-fatal-propagation

## Goal
Modify four user-action/alarm helper methods in `ModuleHelper.java` to use the shared `throwIfFatal` helper for fatal-error propagation, while preserving their existing ordinary fallback behavior.

## Scope
- `ModuleHelper.getNextMIUIAlarmTime`
- `ModuleHelper.openAppInfo` (outer and inner catch)
- `ModuleHelper.getActionImage`
- `ModuleHelper.getActionName`

## Contract
1. In each catch block, the first executable statement must be `throwIfFatal(t);` (or `throwIfFatal(t2);` for `openAppInfo` inner catch).
2. Fatal errors (`OutOfMemoryError`, `ThreadDeath`, `VirtualMachineError`) must propagate before any logging, fallback, or return.
3. Ordinary failure behavior must remain unchanged:
   - Alarm: log the error and return the current `nextTime`.
   - `openAppInfo` outer: fall back to the system application details settings.
   - `openAppInfo` inner: log `t2` and complete.
   - `getActionImage` / `getActionName`: return `null`.
4. No direct `instanceof OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError` guards in the five target catch blocks.
5. No use of `ReflectionFatality`.
6. Alarm algorithm, Intent parameters, user behavior, action preference keys, toggle resource mappings, and activity name fallback must remain unchanged.

## Verification
- New static contract test: `tools/tests/test_module_helper_action_alarm_fatality.py`.
- Updated `tools/tests/test_module_helper_receiver_lifecycle_fatality.py` scope test.
- Existing `tools/tests/test_module_helper_reflection_fallback_fatality.py` and `tools/tests/test_module_helper_hook_install_fatality.py` must still pass.
- `python tools/verify.py full`
- `python -m compileall tools`
- `python -m unittest discover -s tools/tests -p "test_*.py"`
- `python tools/source_hazard_scan.py --path app/src/main/java`
- `python tools/verify.py fast --changed`
- `git diff --check`

## Status
In progress.
