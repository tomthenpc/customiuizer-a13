# A13-PERF-P2-0 Memory & Lifecycle Ownership / Retention Topology

## Metadata

| Field | Value |
|-------|-------|
| Task | `A13-PERF-P2-0` |
| Base SHA | `4dbe02599bfe09ea7efb5b0d94c2f35cb614d72a` |
| Branch | `devin/a13-memory-performance-optimization` |
| Scope | `app/src/main/java/**` |
| Production changes | `FORBIDDEN` in P2-0 |
| P2-0 state | `QA_ACCEPTED / FROZEN` |
| P2-1 state | `NOT_STARTED` |
| JDK25 migration | `READY_TO_BRANCH` |
| P1B | `SEALED` |
| QA-1 | `SEALED` |
| P1B-4A | `ROM_LIFECYCLE_EVIDENCE_PENDING` |

## Summary counts

| Risk | Count |
|------|-------|
| MEDIUM | 96 |
| LOW | 92 |
| INFO | 303 |
| UNKNOWN | 68 |

| Classification | Count |
|----------------|-------|
| SAFE_STABLE_METADATA | 303 |
| UNKNOWN_REQUIRES_MANUAL_REVIEW | 123 |
| PROCESS_LIFETIME_INTENTIONAL | 45 |
| WEAK_EDGE_WITH_MANAGED_ROOT | 34 |
| PROCESS_LIFETIME_METADATA_COLLECTION | 25 |
| LIFECYCLE_MANAGED | 22 |
| BOUNDED_REPLACEMENT_RETENTION | 4 |
| BOUNDED_DELAYED_CALLBACK_RETENTION | 1 |
| VIEW_LIFETIME_OWNED_LISTENER | 1 |
| PROCESS_LIFETIME_CONFIG_COLLECTION | 1 |

| Root kind | Count |
|-----------|-------|
| KOTLIN_OBJECT_FIELD | 314 |
| ADDITIONAL_INSTANCE_FIELD | 77 |
| INSTANCE_FIELD | 68 |
| HANDLER | 33 |
| WEAK_REFERENCE | 29 |
| STATIC_FIELD | 11 |
| LISTENER_REGISTRATION | 9 |
| BROADCAST_RECEIVER_REGISTRATION | 9 |
| CONTENT_OBSERVER_REGISTRATION | 4 |
| COMPANION_OBJECT_FIELD | 3 |
| THREAD_EXECUTOR | 1 |
| CALLBACK_REGISTRATION | 1 |

| Process | Candidate count |
|---------|-----------------|
| tv.withaibuild.customiuizer.r13 | 410 |
| system_server | 73 |
| com.android.systemui | 58 |
| com.miui.home | 18 |

## Process roots

### tv.withaibuild.customiuizer.r13

- Total candidates: 410
- KOTLIN_OBJECT_FIELD: 257
- INSTANCE_FIELD: 67
- HANDLER: 24
- WEAK_REFERENCE: 21
- STATIC_FIELD: 11
- ADDITIONAL_INSTANCE_FIELD: 10
- BROADCAST_RECEIVER_REGISTRATION: 9
- LISTENER_REGISTRATION: 5
- COMPANION_OBJECT_FIELD: 3
- CONTENT_OBSERVER_REGISTRATION: 1
- THREAD_EXECUTOR: 1
- CALLBACK_REGISTRATION: 1

### system_server

- Total candidates: 73
- ADDITIONAL_INSTANCE_FIELD: 39
- KOTLIN_OBJECT_FIELD: 21
- HANDLER: 6
- WEAK_REFERENCE: 6
- CONTENT_OBSERVER_REGISTRATION: 1

### com.android.systemui

- Total candidates: 58
- KOTLIN_OBJECT_FIELD: 34
- ADDITIONAL_INSTANCE_FIELD: 16
- LISTENER_REGISTRATION: 2
- HANDLER: 2
- CONTENT_OBSERVER_REGISTRATION: 2
- WEAK_REFERENCE: 2

### com.miui.home

- Total candidates: 18
- ADDITIONAL_INSTANCE_FIELD: 12
- KOTLIN_OBJECT_FIELD: 2
- LISTENER_REGISTRATION: 2
- INSTANCE_FIELD: 1
- HANDLER: 1

## Registration / callback roots

### BROADCAST_RECEIVER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt:158` — mScreenOnReceiver — BOUNDED_REPLACEMENT_RETENTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:828` — mGlobalReceiver — BOUNDED_REPLACEMENT_RETENTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:937` — mSBReceiver — BOUNDED_REPLACEMENT_RETENTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:513` — showReceiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:769` — null — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:720` — receiver — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:801` — receiver — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt:160` — devicesReceiver — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt:196` — wifiReceiver — LIFECYCLE_MANAGED (MEDIUM)

### CONTENT_OBSERVER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt:135` — alarmObserver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:103` — contentObserver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:104` — contentObserver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:862` — alarmObserver — PROCESS_LIFETIME_INTENTIONAL (LOW)

### LISTENER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:44` — XposedServiceHelper.OnServiceListener — PROCESS_LIFETIME_INTENTIONAL (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:79` — Activity — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt:84` — searchTextWatcher — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt:236` — shakeMgr — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt:169` — TextWatcher — VIEW_LIFETIME_OWNED_LISTENER (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:451` — AnimatorListenerAdapter — BOUNDED_REPLACEMENT_RETENTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:1389` — attachStateListener — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:271` — listener — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceBootstrap.java:233` — l — PROCESS_LIFETIME_INTENTIONAL (LOW)

### CALLBACK_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/subs/WebPage.kt:36` — callback — LIFECYCLE_MANAGED (LOW)

## Async roots (Handler / Runnable / Thread / Executor)

- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:88` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:100` — HANDLER — retained `Fragment` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:202` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:209` — HANDLER — retained `Fragment` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt:234` — HANDLER — retained `Fragment` — BOUNDED_DELAYED_CALLBACK_RETENTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:76` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:259` — HANDLER — retained `Runnable` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:288` — HANDLER — retained `Runnable` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:392` — HANDLER — retained `Runnable` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt:76` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt:110` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt:455` — HANDLER — retained `runnable` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt:568` — HANDLER — retained `runnable` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt:626` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt:126` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:650` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:96` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:113` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:286` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:286` — HANDLER — retained `Runnable` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:518` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:853` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt:200` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt:214` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:66` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt:208` — THREAD_EXECUTOR — retained `queryThread` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt:209` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt:210` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt:81` — HANDLER — retained `Thread` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt:94` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/System_AirplaneModeConfig.kt:19` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt:114` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt:403` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/BitmapCachedLoader.kt:193` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)

## Collection roots

- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:44` — `XposedServiceHelper.OnServiceListener` — PROCESS_LIFETIME_INTENTIONAL (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/PackagePermissions.kt:15` — `MutableSet<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt:34` — `ConcurrentHashMap<String, Pair<Float, Rect?>` — PROCESS_LIFETIME_CONFIG_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt:30` — `ArrayList<String>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt:407` — `MainModule.mPrefs.getStringSet` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt:410` — `MainModule.mPrefs.getStringSet` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:51` — `ArrayList<String>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:451` — `AnimatorListenerAdapter` — BOUNDED_REPLACEMENT_RETENTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:58` — `List<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:59` — `ArrayList<WeakReference<View>` — WEAK_EDGE_WITH_MANAGED_ROOT (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:59` — `List<String>?` — WEAK_EDGE_WITH_MANAGED_ROOT (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:1389` — `attachStateListener` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:34` — `ConcurrentHashMap<String, FeatureSpec>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:35` — `ConcurrentHashMap<String, String>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:36` — `ConcurrentHashMap<FeatureStateKey, FeatureState>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticRecorder.kt:32` — `LinkedHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticRecorder.kt:35` — `HashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/FeatureInstallState.kt:15` — `HashMap<Int, FeatureState>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookTargetResolver.kt:37` — `HashMap<String, Any?>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookTargetResolver.kt:38` — `HashMap<String, ResolutionLog>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:45` — `View.OnAttachStateChangeListener` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:47` — `View.OnAttachStateChangeListener?` — WEAK_EDGE_WITH_MANAGED_ROOT (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:63` — `ConcurrentHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:695` — `ConcurrentHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:697` — `ConcurrentHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:769` — `ConcurrentHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:771` — `ConcurrentHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:852` — `ConcurrentHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:928` — `ConcurrentHashMap<String,` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:958` — `ConcurrentHashMap<String,` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:1085` — `ConcurrentHashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt:51` — `Set<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ResourceHooks.java:97` — `ConcurrentHashMap<String,` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt:179` — `mutableListOf<StepViewRef>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:72` — `ConcurrentHashMap<MemberCacheKey.Method,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:73` — `ConcurrentHashMap<MemberCacheKey.Constructor,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:187` — `HashMap<String,` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceEx.kt:37` — `View.OnLongClickListener` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceSchema.kt:325` — `Map<String, PreferenceEntry>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/prefs/SpinnerEx.kt:26` — `ArrayList<Int>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/prefs/SpinnerExFake.kt:12` — `ArrayList<Pair<String, String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt:25` — `ArrayList<AppData>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt:42` — `java.util.LinkedHashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/ShortcutSelector.kt:29` — `ArrayList<ResolveInfo>

    override fun onCreate` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt:46` — `java.util.LinkedHashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AppDataAdapter.kt:27` — `ArrayList<AppData>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AppDataAdapter.kt:33` — `LinkedHashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AppDataAdapter.kt:34` — `LinkedHashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AppHelper.kt:48` — `Set<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/BitmapCachedLoader.kt:32` — `HashMap<String, MutableList<T>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:78` — `ArrayList<AppData>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:80` — `ArrayList<AppData>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:82` — `ArrayList<AppData>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:84` — `ArrayList<AppData>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:86` — `ArrayList<ModData>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:117` — `HashSet<String>` — PROCESS_LIFETIME_METADATA_COLLECTION (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt:29` — `ArrayList<AppData>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt:31` — `HashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceAdapter.kt:22` — `ArrayList<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt:29` — `ArrayList<AppData>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt:31` — `HashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/ResolveInfoAdapter.kt:26` — `CopyOnWriteArrayList<ResolveInfo>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/ResolveInfoAdapter.kt:27` — `CopyOnWriteArrayList<ResolveInfo>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/SortableListView.kt:43` — `View.OnTouchListener
    private var mScrollBound` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)

## Safe metadata roots

- Total SAFE_STABLE_METADATA candidates: 303
- Examples: `Method`, `Field`, `Class`, `String` constants, `Int`/`Long` config, reflection metadata.
- These are stable process-lifetime metadata, not short-lived Android owner retention.

## Unknowns / manual-review queue

- Total UNKNOWN or MEDIUM candidates requiring manual review: 164
- These need ROM/runtime evidence to confirm release path, owner lifetime, or callback capture.
- WeakReference edges still require their registration root to be reviewed.

## Top 10 retention candidates

| Rank | ID | Risk | Status | Classification | Process | Source | Line | Retained | Notes |
|------|----|------|--------|----------------|---------|--------|------|----------|-------|
| 1 | mlp-00078 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemChargingAndWallpaperHooks.kt | 111 | `wallpaperHandler` | ler == null) { wallpaperHandler = Handler(Looper.getMainLooper()) XposedHelpers.... |
| 2 | mlp-00079 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemChargingAndWallpaperHooks.kt | 183 | `wallpaperRunnable` | } } wallpaperHandler.postDelayed(wallpaperRunnable, 1800) XposedHelpers.setAddit... |
| 3 | mlp-00083 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 126 | `currentTouchTime` | } currentTouchTime = 0L } XposedHelpers.setAdditionalInstanceField(view, "curren... |
| 4 | mlp-00084 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 127 | `currentTouchX` | sedHelpers.setAdditionalInstanceField(view, "currentTouchTime", currentTouchTime... |
| 5 | mlp-00085 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 128 | `currentTouchY` | XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", currentTouchX) X... |
| 6 | mlp-00086 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 151 | `initBlurRatio` | ) val initBlurRatio = MainModule.mPrefs.getInt("system_drawer_blur", 100) Xposed... |
| 7 | mlp-00087 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 152 | `initBlurRatio` | onalInstanceField(notificationShadeDepthController, "mCustomBlurModifier", initB... |
| 8 | mlp-00088 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 158 | `opt` | XposedHelpers.getObjectField(owner, "notificationShadeDepthController") XposedHe... |
| 9 | mlp-00089 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 159 | `opt` | rs.setAdditionalInstanceField(currentDepthController, "mCustomBlurModifier", opt... |
| 10 | mlp-00090 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 170 | `blurRatio` | val mBlurUtils = XposedHelpers.getObjectField(parentCtrl, "blurUtilsExt") Xposed... |

## Top evidence-pending candidates

| Rank | ID | Risk | Status | Classification | Process | Source | Line | Retained | Notes |
|------|----|------|--------|----------------|---------|--------|------|----------|-------|
| 1 | mlp-00077 | MEDIUM | NEEDS_ROM_EVIDENCE | PROCESS_LIFETIME_INTENTIONAL | system_server | SystemChargingAndWallpaperHooks.kt | 110 | `Handler` | "mWallpaperHandler") as? Handler if (wallpaperHandler == null) { wallpaperHandle... |
| 2 | mlp-00078 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemChargingAndWallpaperHooks.kt | 111 | `wallpaperHandler` | ler == null) { wallpaperHandler = Handler(Looper.getMainLooper()) XposedHelpers.... |
| 3 | mlp-00079 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemChargingAndWallpaperHooks.kt | 183 | `wallpaperRunnable` | } } wallpaperHandler.postDelayed(wallpaperRunnable, 1800) XposedHelpers.setAddit... |
| 4 | mlp-00083 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 126 | `currentTouchTime` | } currentTouchTime = 0L } XposedHelpers.setAdditionalInstanceField(view, "curren... |
| 5 | mlp-00084 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 127 | `currentTouchX` | sedHelpers.setAdditionalInstanceField(view, "currentTouchTime", currentTouchTime... |
| 6 | mlp-00085 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 128 | `currentTouchY` | XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", currentTouchX) X... |
| 7 | mlp-00086 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 151 | `initBlurRatio` | ) val initBlurRatio = MainModule.mPrefs.getInt("system_drawer_blur", 100) Xposed... |
| 8 | mlp-00087 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 152 | `initBlurRatio` | onalInstanceField(notificationShadeDepthController, "mCustomBlurModifier", initB... |
| 9 | mlp-00088 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 158 | `opt` | XposedHelpers.getObjectField(owner, "notificationShadeDepthController") XposedHe... |
| 10 | mlp-00089 | MEDIUM | NEEDS_ROM_EVIDENCE | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 159 | `opt` | rs.setAdditionalInstanceField(currentDepthController, "mCustomBlurModifier", opt... |

## Top 3 strongest retention chains

### 1. `SystemChargingAndWallpaperHooks.kt:111` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)

- **Root**: `ADDITIONAL_INSTANCE_FIELD` retaining `wallpaperHandler` in process `system_server`.
- **Review rationale**: Scanner cannot prove the release path or owner lifetime from source alone; ROM/runtime evidence required.

### 2. `SystemChargingAndWallpaperHooks.kt:183` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)

- **Root**: `ADDITIONAL_INSTANCE_FIELD` retaining `wallpaperRunnable` in process `system_server`.
- **Review rationale**: Scanner cannot prove the release path or owner lifetime from source alone; ROM/runtime evidence required.

### 3. `SystemDisplayAndWindowHooks.kt:126` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)

- **Root**: `ADDITIONAL_INSTANCE_FIELD` retaining `currentTouchTime` in process `system_server`.
- **Review rationale**: Scanner cannot prove the release path or owner lifetime from source alone; ROM/runtime evidence required.

## Manual supplemental coverage

- **Candidates reviewed**: 408 of 559
- **RAW HIGH/CRITICAL SOURCE-REVIEWED**: 26 (CRITICAL=0)
- **FINAL HIGH/CRITICAL**: 0
- **MEDIUM/UNKNOWN needing ROM/runtime evidence**: 151
- **Collection breakdown**: owner=0, metadata/state=25, config=1, unknown=0
- **False-positive / benign count**: 348 (`SAFE_STABLE_METADATA` + `PROCESS_LIFETIME_INTENTIONAL`) classified as not requiring production change.

## Recommended P2-1 slice

```
RECOMMENDED_P2_1 = SubFragment.kt smooth-scroller delayed callback cleanup
```

### Why this is ranked first

- **Top candidate**: `SubFragment.kt:234` — `Fragment` — BOUNDED_DELAYED_CALLBACK_RETENTION (MEDIUM).
- **Review rationale**: Fragment/View posts a delayed Runnable with a finite window; no evidence of repeated unbounded queuing. Delay is 380ms (short window). highlightKey is reset to null before post, bounding the trigger to the current key highlight. onStart is lifecycle-bound; callback is not proven to outlive the Fragment/View.
- **Scope small**: one file or a single callback site, no new architecture.
- **Regression risk low**: the fix only adds a matching `removeCallbacks` / `removeListener` call in an existing lifecycle teardown path.

### P2-1 status

```
P2-1 = NOT_STARTED
P2-0 = QA_ACCEPTED / FROZEN
P1B = SEALED
QA-1 = SEALED
P1B-4A = ROM_LIFECYCLE_EVIDENCE_PENDING
JDK25_MIGRATION = READY_TO_BRANCH
A13_PERFORMANCE_STABLE_BASE = <R3 FINAL SHA>
```

## Static scanner note

The scanner only discovers *candidates*. It does not prove runtime memory leaks. All raw HIGH/CRITICAL items were source-reviewed and downgraded where evidence did not support a HIGH/CRITICAL classification. MEDIUM/UNKNOWN items need ROM/runtime evidence before production change.
