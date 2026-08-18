# A13_PHASE_F_FINAL_PARITY_REPORT

BASE_SHA = d25bb9d37d3ee60d13657a24361336d8c705cb71
VERIFIED_TREE_SHA = e77fe1bfd934dfb6df24d0aa84e3416f47ef98eb
REPORT_HEAD_SHA = (this commit)
REMOTE_HEAD = (after push)
LOCAL_HEAD = (after push)
WORKTREE = CLEAN

A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa

A14_PRODUCT_FEATURE_COUNT = 566
A13_PRODUCT_FEATURE_COUNT = 562
A13_ONLY_KEEP_COUNT = 64

PRESENT_EQUIVALENT = 0
PRESENT_A13_VARIANT = 474
PARTIAL_PARITY = 0
MISSING_IN_A13 = 0
INTENTIONAL_EXCLUDED = 1
DEAD_UPSTREAM_PATH = 24
HOLD_EVIDENCE = 67
INSUFFICIENT_EVIDENCE = 0

DYNAMIC_ISLAND_EXCLUDED = YES

Accounting: 474 + 0 + 0 + 1 + 24 + 67 + 0 = 566.

FEATURES_NEWLY_PORTED = none in Phase F production. Remaining E3/E4/E5 gaps were held after source preflight, not implemented.

EXISTING_A13_FEATURES_UPGRADED = none in Phase F. Phase E ports (IME dismiss, dock height, folder blur disable, charging fontsize, netspeed clock style, dual-row ratio, wireless headset, installer purify, hide report, backup V2, USB mapper + R1 latch) were reclassified from stale MISSING/PARTIAL/INSUFFICIENT to PRESENT_A13_VARIANT.

FALSE_GAPS_REJECTED =
- `controls_hide_ime_dismiss_button` was still forced MISSING by a stale Phase D override despite the Phase E hook
- `system_usb_default_function` is an A13 USBConfigHook variant, not a remaining HAL gap
- `infra.backup_restore` is the A13 V2 typed backup path
- same-key rows with matching host family and production reads are A13 variants, not insufficient evidence

A14_DEAD_UI_PATHS = 24 including `system_hidestatusbar_whenscreenrecord` (XML/strings only at pinned A14) and other A14 UI keys with no production pref read on either tree.

PRODUCTION_COMMITS = none (no Phase F production hook/UI change)
TEST_COMMITS = e77fe1bfd934dfb6df24d0aa84e3416f47ef98eb

FULL_GATE = PASS
TOOLS_TESTS = PASS (1283 tests, 2 skipped)
COMPILEALL = PASS
DIFF_CHECK = PASS
DEPENDENCY_VERIFICATION = default / not disabled

DEVICE_VERIFIED_FEATURES = none
STATIC_ONLY_HOLDS = 67 (see docs/parity/A13_PHASE_F_HOLD_EVIDENCE.md)

PHASE_A_REOPENED = NO
PHASE_B_REOPENED = NO
PHASE_C_REOPENED = NO
PHASE_E_REOPENED = NO

Phase F did not reopen frozen A/B/C/E production. F1 remaining Control Center plugin colors, digital signal, drawer date, volume shortcut colors, system_server ForceDark/Blur/auto-brightness-reset, and SecurityCenter component-trim lists require ROM/device evidence and stay HOLD.

READY_FOR_CHATGPT_PHASE_F_FINAL_AUDIT
