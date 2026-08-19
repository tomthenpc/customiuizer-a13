# A13 Phase E-R1 — USB Replug Lifecycle Corrective

BASE_SHA = baee5a9fd86266a1828b9d7790480cc36a8122a3
CORRECTIVE_SHA = ea4d797e61d07e7274a2dae78ebc256a0e7ba1d4
VERIFIED_TREE_SHA = 3905bd8d529e8a245b2d97db8b6fa0c502aea09f

TASK_SCOPE = USB default existing-hook lifecycle corrective only
PHASE_F_STARTED = NO

## Defect

`SystemSettingsMoreHooks.mUSBConnected` was set true after a handled USB
connect and was never reset when `USB_STATE` reported `connected=false`.
After one successful MTP/PTP application, a later cable replug in the same
`system_server` lifetime could skip default-function application.

## Fix

Kept `system_defaultusb`, `system_defaultusb_unsecure`,
`UsbDefaultFunctionMapper`, `USBConfigHook`, and `USBConfigSettingsHook`.
No second USB subsystem. A14 `UsbHandlerHal.setEnabledFunctions(JZI)` was
not copied.

`UsbConnectLatch` is a tiny pure helper used by the existing receiver:

- DISCONNECTED → CONNECTED = one apply attempt
- CONNECTED → CONNECTED = no duplicate apply
- CONNECTED → DISCONNECTED = reset latch
- DISCONNECTED → CONNECTED again = fresh apply attempt

Early-exits (non-USB plug type, already-enabled function, missing
`UsbManager`) still leave the latch unset so a later `USB_STATE` can retry.
Ordinary reflection failure still commits the latch after the inner catch.
Fatal `Throwable` still propagates. Receiver remains
`ModuleHelper.registerModuleReceiver(..., "system.usbStateReceiver", ...)`.
There is still one `mUSBConnected` owner.

## Validation

USB_REPLUG_SEQUENCE_TEST = PASS (`UsbConnectLatchTest`)
TARGETED_TESTS = PASS (`python tools/verify.py fast --changed --tests Usb`)
FAST_CHANGED = PASS (includes `:app:compileDebugKotlin` and `:app:compileDebugJavaWithJavac`)
FULL_GATE = PASS (`python tools/verify.py full` including lintDebug)
TOOLS_TESTS = PASS (`python -m unittest discover -s tools/tests -p "test_*.py"` → 1283 tests, 2 skipped)
COMPILEALL = PASS
DIFF_CHECK = PASS
DEPENDENCY_VERIFICATION = default / not disabled

STATIC_VERIFIED = YES
BUILD_VERIFIED = YES
DEVICE_VERIFIED = NO
LOG_VERIFIED = NO

READY_FOR_CHATGPT_PHASE_E_R1_FINAL_AUDIT
