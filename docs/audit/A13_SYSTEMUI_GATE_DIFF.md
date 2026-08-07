# A13 SystemUI Gate Differential Audit

Schema version: 1.0

## Summary

- Install conditions: 108
- Startup conditions: 119
- Conditional dispatchers: 8
- Unconditional dispatchers: 5
- Catalog SystemUI entries: 13

## Counts

- COMPARATOR_MISMATCH: 0
- COMPOSITE_CONDITION_MISMATCH: 0
- DEFAULT_MISMATCH: 0
- DOMAIN_CONTAMINATION: 0
- DYNAMIC_GLOBAL_ACTION_GATE: 1
- FEATURE_CATALOG_GATE_UNKNOWN: 0
- GATE_ONLY: 0
- GATE_ONLY_DYNAMIC_DOMAIN: 6
- GATE_ONLY_REDUNDANT: 0
- GATE_ONLY_UNEXPLAINED: 0
- INSTALLER_CATALOG_MATCH: 8
- INSTALLER_CATALOG_MISMATCH: 0
- INSTALLER_ONLY: 0
- MATCH: 197
- SEMANTIC_REVIEW_REQUIRED: 0
- UNMATCHED_INFRASTRUCTURE: 4

## Global Action Domain

- contaminated: False
- reason: positive domain requires _action and (controls_|system_)

## Records

### DYNAMIC_GLOBAL_ACTION_GATE — hasAnySystemUiStartupFeature_if_1
- severity: INFO
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 334-334
- explanation: Dynamic global action gate covers SystemUI _action keys.

### MATCH — install_if_2
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 51-51
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 335-335
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_3
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 52-52
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 336-336
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_7
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 80-82
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 337-337
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_7
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 80-82
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 337-337
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_8
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 86-89
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 338-338
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_8
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 86-89
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 338-338
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_9
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 95-95
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 339-339
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_9
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 95-95
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 339-339
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_10
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 96-96
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 340-340
- explanation: Startup gate covers this installer condition.

### MATCH — networkIndicatorWifi
- severity: OK
- feature_id: networkIndicatorWifi
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 97-97
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 341-341
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_12
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 99-99
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 342-342
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_13
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 100-100
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 343-343
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_14
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 101-101
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 344-344
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_15
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 102-102
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 345-345
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_16
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 103-103
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 346-346
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_16
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 103-103
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 346-346
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_17
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 104-104
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 347-347
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_18
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 105-107
- explanation: Covered by the dynamic global action gate.

### MATCH — install_if_18
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 105-107
- explanation: Covered by the dynamic global action gate.

### MATCH — install_if_19
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 108-111
- explanation: Covered by the dynamic global action gate.

### MATCH — install_if_19
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 108-111
- explanation: Covered by the dynamic global action gate.

### MATCH — install_if_19
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 108-111
- explanation: Covered by the dynamic global action gate.

### MATCH — install_if_19
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 108-111
- explanation: Covered by the dynamic global action gate.

### MATCH — install_if_20
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 112-112
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 350-350
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_21
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 113-113
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 351-351
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_22
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 115-115
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 352-352
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_23
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 116-119
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 353-353
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_24
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 120-120
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 354-354
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_25
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 121-121
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 355-355
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_26
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 122-122
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 356-356
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_27
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 123-123
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 357-357
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_28
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 124-124
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 358-358
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_29
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 126-126
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 359-359
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_30
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 127-127
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 360-360
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_31
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 128-128
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 361-361
- explanation: Startup gate covers this installer condition.

### MATCH — hideDismissView
- severity: OK
- feature_id: hideDismissView
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 129-129
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 362-362
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_33
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 130-130
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 363-363
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_34
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 131-131
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 364-364
- explanation: Startup gate covers this installer condition.

### MATCH — hideNavBarBeforeScreenshot
- severity: OK
- feature_id: hideNavBarBeforeScreenshot
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 132-132
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 365-365
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_36
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 133-133
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 366-366
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_37
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 134-134
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 367-367
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_38
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 135-149
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 368-368
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_39
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 153-153
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 369-369
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_40
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 154-154
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 370-370
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_41
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 155-158
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 371-371
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_41
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 155-158
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 371-371
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_42
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 171-176
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 372-372
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_43
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 179-179
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 373-373
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_44
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 182-182
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 374-374
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_45
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 185-187
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 375-375
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_45
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 185-187
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 375-375
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_46
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 188-188
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 376-376
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_47
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 189-189
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 377-377
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_48
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 190-190
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 378-378
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_49
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 191-191
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 379-379
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_50
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 192-192
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 380-380
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_51
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 193-193
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 381-381
- explanation: Startup gate covers this installer condition.

### MATCH — hideLockScreenHint
- severity: OK
- feature_id: hideLockScreenHint
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 194-194
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 382-382
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_53
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 195-195
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 383-383
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_54
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 196-196
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 384-384
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_55
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 197-197
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 385-385
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_56
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 198-198
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 386-386
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_57
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 199-199
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 387-387
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_58
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 200-200
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 388-388
- explanation: Startup gate covers this installer condition.

### MATCH — noNetworkSpeedSeparator
- severity: OK
- feature_id: noNetworkSpeedSeparator
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 201-201
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 389-389
- explanation: Startup gate covers this installer condition.

### MATCH — hideIconsClock
- severity: OK
- feature_id: hideIconsClock
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 202-202
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 390-390
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_61
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 203-209
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 391-391
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_61
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 203-209
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 391-391
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_62
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 212-222
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 392-392
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_63
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 225-225
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 393-393
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_64
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 226-226
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 394-394
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_65
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 227-227
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 395-395
- explanation: Startup gate covers this installer condition.

### MATCH — chargingInfo
- severity: OK
- feature_id: chargingInfo
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 228-228
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 396-396
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_67
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 229-229
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 397-397
- explanation: Startup gate covers this installer condition.

### MATCH — muteVisibleNotifications
- severity: OK
- feature_id: muteVisibleNotifications
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 230-230
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 398-398
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_69
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 231-231
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 399-399
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_70
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 232-235
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 400-400
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_70
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 232-235
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 400-400
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_70
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 232-235
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 400-400
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_71
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 236-236
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 401-401
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_72
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 237-243
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 402-402
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_72
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 237-243
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 402-402
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_72
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 237-243
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 402-402
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_72
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 237-243
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 402-402
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_72
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 237-243
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 402-402
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_72
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 237-243
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 402-402
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_73
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 244-244
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 403-403
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_74
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 245-245
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 404-404
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_75
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 246-248
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 405-405
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_75
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 246-248
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 405-405
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_75
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 246-248
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 405-405
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_76
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 249-249
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 406-406
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_77
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 250-250
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 407-407
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_78
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 251-251
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 408-408
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_79
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 252-254
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 409-409
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_79
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 252-254
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 409-409
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_80
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 255-255
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 410-410
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_81
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 256-256
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 411-411
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_82
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 257-257
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 412-412
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_83
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 258-258
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 413-413
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_84
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 259-259
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 414-414
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_85
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 260-263
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 415-415
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_85
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 260-263
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 415-415
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_85
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 260-263
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 415-415
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_86
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 266-266
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 416-416
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_87
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 267-267
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 417-417
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_88
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 287-287
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 418-418
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_89
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 289-294
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 419-419
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_89
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 289-294
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 419-419
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_89
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 289-294
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 419-419
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_89
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 289-294
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 419-419
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_90
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 295-295
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 420-420
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_91
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 296-296
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 421-421
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_92
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 297-297
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 422-422
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_93
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 298-298
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 423-423
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_94
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 299-299
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 424-424
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_95
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 300-300
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 425-425
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_96
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 301-301
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 426-426
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_97
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 302-302
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 427-427
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_98
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 303-303
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 428-428
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_99
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 306-306
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 429-429
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_100
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 309-309
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 430-430
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_101
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 312-312
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 431-431
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_101
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 312-312
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 431-431
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_102
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 313-313
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 432-432
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_103
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 314-314
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 433-433
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_104
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 315-315
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 434-434
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_105
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 317-320
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 435-435
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_105
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 317-320
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 435-435
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_105
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 317-320
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 435-435
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_106
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 323-323
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 436-436
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_107
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 326-326
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 437-437
- explanation: Startup gate covers this installer condition.

### MATCH — install_if_108
- severity: OK
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 329-329
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 438-438
- explanation: Startup gate covers this installer condition.

### INSTALLER_CATALOG_MATCH — tempHideOverlaySystemUI
- severity: OK
- feature_id: tempHideOverlaySystemUI
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 84-84
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 448-448
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 988-1014
- explanation: Catalog condition for tempHideOverlaySystemUI is covered by startup gate.

### INSTALLER_CATALOG_MATCH — hideStatusBarBeforeScreenshot
- severity: OK
- feature_id: hideStatusBarBeforeScreenshot
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 93-93
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 449-449
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 1015-1041
- explanation: Catalog condition for hideStatusBarBeforeScreenshot is covered by startup gate.

### INSTALLER_CATALOG_MATCH — statusBarClockTweak
- severity: OK
- feature_id: statusBarClockTweak
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 114-114
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 450-453
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 102-169
- explanation: Catalog condition for statusBarClockTweak is covered by startup gate.

### INSTALLER_CATALOG_MATCH — statusBarClockTweak
- severity: OK
- feature_id: statusBarClockTweak
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 114-114
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 450-453
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 102-169
- explanation: Catalog condition for statusBarClockTweak is covered by startup gate.

### INSTALLER_CATALOG_MATCH — statusBarClockTweak
- severity: OK
- feature_id: statusBarClockTweak
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 114-114
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 450-453
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 102-169
- explanation: Catalog condition for statusBarClockTweak is covered by startup gate.

### INSTALLER_CATALOG_MATCH — statusBarClockTweak
- severity: OK
- feature_id: statusBarClockTweak
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 114-114
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 450-453
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 102-169
- explanation: Catalog condition for statusBarClockTweak is covered by startup gate.

### INSTALLER_CATALOG_MATCH — noMoreIcon
- severity: OK
- feature_id: noMoreIcon
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 125-125
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 454-454
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 277-322
- explanation: Catalog condition for noMoreIcon is covered by startup gate.

### INSTALLER_CATALOG_MATCH — batteryIndicator
- severity: OK
- feature_id: batteryIndicator
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 152-152
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 455-455
- catalog: app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt lines 323-368
- explanation: Catalog condition for batteryIndicator is covered by startup gate.

### MATCH — setupStatusBar_if_1
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 439-439
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_3
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 440-440
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_4
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 441-441
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_5
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 442-442
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_7
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 443-443
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_8
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 444-444
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_9
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 445-445
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_10
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 446-446
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_11
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 447-447
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### MATCH — setupStatusBar_if_11
- severity: OK
- installer: C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\mods\SystemUIStatusBarHooks.kt lines 0-0
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 447-447
- explanation: Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.

### GATE_ONLY_DYNAMIC_DOMAIN — hasAnySystemUiStartupFeature_if_15
- severity: INFO
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 348-348
- explanation: Startup action gate for ['controls_fsg_assist_left_action'] has no individual installer condition; covered by dynamic global action gate.

### GATE_ONLY_DYNAMIC_DOMAIN — hasAnySystemUiStartupFeature_if_15
- severity: INFO
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 348-348
- explanation: Startup action gate for ['controls_fsg_assist_right_action'] has no individual installer condition; covered by dynamic global action gate.

### GATE_ONLY_DYNAMIC_DOMAIN — hasAnySystemUiStartupFeature_if_16
- severity: INFO
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 349-349
- explanation: Startup action gate for ['controls_navbarleft_action'] has no individual installer condition; covered by dynamic global action gate.

### GATE_ONLY_DYNAMIC_DOMAIN — hasAnySystemUiStartupFeature_if_16
- severity: INFO
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 349-349
- explanation: Startup action gate for ['controls_navbarleftlong_action'] has no individual installer condition; covered by dynamic global action gate.

### GATE_ONLY_DYNAMIC_DOMAIN — hasAnySystemUiStartupFeature_if_16
- severity: INFO
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 349-349
- explanation: Startup action gate for ['controls_navbarright_action'] has no individual installer condition; covered by dynamic global action gate.

### GATE_ONLY_DYNAMIC_DOMAIN — hasAnySystemUiStartupFeature_if_16
- severity: INFO
- startup: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 349-349
- explanation: Startup action gate for ['controls_navbarrightlong_action'] has no individual installer condition; covered by dynamic global action gate.

### UNMATCHED_INFRASTRUCTURE — install_if_1
- severity: INFO
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 49-49
- explanation: Excluded from startup coverage: phase=PACKAGE_GUARD, id=install_if_1

### UNMATCHED_INFRASTRUCTURE — install_if_4
- severity: INFO
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 57-57
- explanation: Excluded from startup coverage: phase=PRE_RESTART_GUARD_INFRASTRUCTURE, id=install_if_4

### UNMATCHED_INFRASTRUCTURE — install_if_5
- severity: INFO
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 64-64
- explanation: Excluded from startup coverage: phase=PRE_RESTART_GUARD_INFRASTRUCTURE, id=install_if_5

### UNMATCHED_INFRASTRUCTURE — install_if_6
- severity: INFO
- installer: app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java lines 74-74
- explanation: Excluded from startup coverage: phase=RESTART_GUARD, id=install_if_6
