# FIX-modulehelper-receiver-lifecycle-fatal-propagation

## Goal
Modify four receiver lifecycle methods in `ModuleHelper.java` to use the shared `throwIfFatal` helper for fatal-error propagation, while preserving their existing ordinary fallback behavior.

## Scope
- `ModuleHelper.registerModuleReceiver`
- `ModuleHelper.registerOwnedReceiver`
- `ModuleHelper.tryRelease`
- `ModuleHelper.releaseReceiver`

## Contract
1. In each method, the first statement in the `catch (Throwable t)` block must be `throwIfFatal(t);`.
2. Fatal errors (`OutOfMemoryError`, `ThreadDeath`, `VirtualMachineError`) must propagate before any state change, logging, stale-queue addition, or fallback return.
3. Ordinary failure behavior must remain unchanged:
   - Registration methods: set `newRegistration.state = RegistrationState.REGISTER_FAILED`, call `log(key, t)`, return `false`.
   - `tryRelease`: set `registration.state = RegistrationState.STALE`, return `false`.
   - `releaseReceiver`: set `registration.state = RegistrationState.STALE`; for owned receivers add to `staleOwnedReceivers`, otherwise add to `staleModuleReceivers`.
4. No use of `ReflectionFatality` or direct `instanceof OutOfMemoryError` guards in the four target catch blocks.
5. Keep the compact `releaseReceiver` catch layout, but ensure `throwIfFatal(t);` is first.

## Verification
- New static contract test: `tools/tests/test_module_helper_receiver_lifecycle_fatality.py` (35 tests).
- Existing `tools/tests/test_module_helper_reflection_fallback_fatality.py` and `tools/tests/test_module_helper_hook_install_fatality.py` must still pass.
- `python tools/verify.py full`
- `python -m compileall tools`
- `python -m unittest discover -s tools/tests -p "test_*.py"`
- `git diff --check`

## Status
In progress.
