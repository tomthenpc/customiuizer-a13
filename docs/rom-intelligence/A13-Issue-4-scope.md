# Issue #4 Repository Scope Decision

## Issue

- Title: 米客 A14 r14.18.6
- Body: 状态栏温度显示不生效
- Device: Xiaomi 14 Ultra
- ROM: HyperOS 1.0.15

## Classification

- Android base: Android 14
- A14 r14.18.6 tag: exists in A14 repository (`r14.18.6`)
- A13 project scope: Android 13 only
- HyperOS 1.0.15 on Xiaomi 14 Ultra: Android 14 platform

## Decision

- `A13_SCOPE = NO`
- `A14_PRODUCTION_ACTION = NONE`
- `FURTHER_STATIC_ANALYSIS = NO`
- `ISSUE_4_CLASSIFICATION = OUT_OF_SCOPE_ROM_GENERATION`

## Route

- `ROUTE_TO_A14_AUDIT = YES`
- Do not modify A13 production or A14 production in this batch.
- Do not close or edit the GitHub issue itself.
