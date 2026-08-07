# A13-PERF-P2-0 Memory & Lifecycle Ownership / Retention Topology

## Metadata

| Field | Value |
|-------|-------|
| Task | `A13-PERF-P2-0` |
| Base SHA | `5f00b0492c9cfc2cc62e171d424c269eeed3f492` |
| Branch | `devin/a13-memory-performance-optimization` |
| Scope | `app/src/main/java/**` |
| Production changes | `FORBIDDEN` in P2-0 |

## Summary counts

| Risk | Count |
|------|-------|
| HIGH | 17 |
| MEDIUM | 116 |
| LOW | 56 |
| INFO | 302 |
| UNKNOWN | 68 |

| Classification | Count |
|----------------|-------|
| SAFE_STABLE_METADATA | 302 |
| UNKNOWN_REQUIRES_MANUAL_REVIEW | 123 |
| PROCESS_LIFETIME_INTENTIONAL | 42 |
| WEAK_EDGE_WITH_MANAGED_ROOT | 34 |
| UNBOUNDED_OWNER_COLLECTION | 26 |
| LIFECYCLE_MANAGED | 15 |
| UNBALANCED_RECEIVER_REGISTRATION | 8 |
| UNBALANCED_LISTENER_REGISTRATION | 5 |
| DELAYED_CALLBACK_OWNER_RETENTION | 3 |
| UNBALANCED_OBSERVER_REGISTRATION | 1 |

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

- `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt:158` — mScreenOnReceiver — UNBALANCED_RECEIVER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:828` — mGlobalReceiver — UNBALANCED_RECEIVER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:937` — mSBReceiver — UNBALANCED_RECEIVER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:513` — showReceiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:769` — null — UNBALANCED_RECEIVER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:720` — receiver — UNBALANCED_RECEIVER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:801` — receiver — UNBALANCED_RECEIVER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt:160` — devicesReceiver — UNBALANCED_RECEIVER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt:196` — wifiReceiver — UNBALANCED_RECEIVER_REGISTRATION (HIGH)

### CONTENT_OBSERVER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt:135` — alarmObserver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:103` — contentObserver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:104` — contentObserver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:862` — alarmObserver — UNBALANCED_OBSERVER_REGISTRATION (HIGH)

### LISTENER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:44` — XposedServiceHelper.OnServiceListener — UNBALANCED_LISTENER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:79` — Activity — UNBALANCED_LISTENER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt:84` — searchTextWatcher — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt:236` — shakeMgr — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt:169` — TextWatcher — UNBALANCED_LISTENER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:451` — AnimatorListenerAdapter — UNBALANCED_LISTENER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:1389` — attachStateListener — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:271` — listener — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceBootstrap.java:233` — l — UNBALANCED_LISTENER_REGISTRATION (HIGH)

### CALLBACK_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/subs/WebPage.kt:36` — callback — LIFECYCLE_MANAGED (LOW)

## Async roots (Handler / Runnable / Thread / Executor)

- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:88` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:100` — HANDLER — retained `Fragment` — DELAYED_CALLBACK_OWNER_RETENTION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:202` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:209` — HANDLER — retained `Fragment` — DELAYED_CALLBACK_OWNER_RETENTION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt:234` — HANDLER — retained `Fragment` — DELAYED_CALLBACK_OWNER_RETENTION (HIGH)
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

- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:44` — `XposedServiceHelper.OnServiceListener` — UNBALANCED_LISTENER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/PackagePermissions.kt:15` — `MutableSet<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt:34` — `ConcurrentHashMap<String, Pair<Float, Rect?>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt:30` — `ArrayList<String>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt:407` — `MainModule.mPrefs.getStringSet` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt:410` — `MainModule.mPrefs.getStringSet` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:51` — `ArrayList<String>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:451` — `AnimatorListenerAdapter` — UNBALANCED_LISTENER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:58` — `List<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:59` — `ArrayList<WeakReference<View>` — WEAK_EDGE_WITH_MANAGED_ROOT (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:59` — `List<String>?` — WEAK_EDGE_WITH_MANAGED_ROOT (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:1389` — `attachStateListener` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:34` — `ConcurrentHashMap<String, FeatureSpec>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:35` — `ConcurrentHashMap<String, String>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt:36` — `ConcurrentHashMap<FeatureStateKey, FeatureState>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticRecorder.kt:32` — `LinkedHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticRecorder.kt:35` — `HashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/FeatureInstallState.kt:15` — `HashMap<Int, FeatureState>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookTargetResolver.kt:37` — `HashMap<String, Any?>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookTargetResolver.kt:38` — `HashMap<String, ResolutionLog>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:45` — `View.OnAttachStateChangeListener` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:47` — `View.OnAttachStateChangeListener?` — WEAK_EDGE_WITH_MANAGED_ROOT (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:63` — `ConcurrentHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:695` — `ConcurrentHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:697` — `ConcurrentHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:769` — `ConcurrentHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:771` — `ConcurrentHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:852` — `ConcurrentHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:928` — `ConcurrentHashMap<String,` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:958` — `ConcurrentHashMap<String,` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:1085` — `ConcurrentHashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt:51` — `Set<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ResourceHooks.java:97` — `ConcurrentHashMap<String,` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt:179` — `mutableListOf<StepViewRef>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:72` — `ConcurrentHashMap<MemberCacheKey.Method,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:73` — `ConcurrentHashMap<MemberCacheKey.Constructor,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java:187` — `HashMap<String,` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
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
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:78` — `ArrayList<AppData>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:80` — `ArrayList<AppData>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:82` — `ArrayList<AppData>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:84` — `ArrayList<AppData>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:86` — `ArrayList<ModData>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:117` — `HashSet<String>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
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

- Total SAFE_STABLE_METADATA candidates: 302
- Examples: `Method`, `Field`, `Class`, `String` constants, `Int`/`Long` config, reflection metadata.
- These are stable process-lifetime metadata, not short-lived Android owner retention.

## Unknowns / manual-review queue

- Total UNKNOWN or MEDIUM candidates requiring manual review: 184
- These need ROM/runtime evidence to confirm release path, owner lifetime, or callback capture.
- WeakReference edges still require their registration root to be reviewed.

## Top 10 retention candidates

| Rank | ID | Risk | Classification | Process | Source | Line | Retained | Notes |
|------|----|------|----------------|---------|--------|------|----------|-------|
| 1 | mlp-00170 | HIGH | UNBALANCED_LISTENER_REGISTRATION | com.android.systemui | SystemUILockScreenHooks.kt | 451 | `AnimatorListenerAdapter` | ull) mAnimatorSet.pause() mAnimatorSet.removeAllListeners() mAnimatorSet.addList... |
| 2 | mlp-00064 | HIGH | UNBALANCED_LISTENER_REGISTRATION | com.miui.home | LauncherIconHooks.kt | 169 | `TextWatcher` | ram.getThisObject(), "mMessage") as? TextView if (mMessage != null) mMessage.add... |
| 3 | mlp-00002 | HIGH | UNBALANCED_LISTENER_REGISTRATION | tv.withaibuild.customiuizer.r13 | MainActivity.kt | 79 | `Activity` | AppHelper.onLocalPreferenceChanged(AppHelper.remotePrefs, key, value) } AppHelpe... |
| 4 | mlp-00007 | HIGH | DELAYED_CALLBACK_OWNER_RETENTION | tv.withaibuild.customiuizer.r13 | MainFragment.kt | 100 | `Fragment` | UiThread { showXposedDialog(act) } } } mCheckActiveRunnable = runnable mMainHand... |
| 5 | mlp-00009 | HIGH | DELAYED_CALLBACK_OWNER_RETENTION | tv.withaibuild.customiuizer.r13 | MainFragment.kt | 209 | `Fragment` | Keyboard(act, view) } mHideKeyboardRunnable = hideRunnable mMainHandler?.postDel... |
| 6 | mlp-00011 | HIGH | DELAYED_CALLBACK_OWNER_RETENTION | tv.withaibuild.customiuizer.r13 | SubFragment.kt | 234 | `Fragment` | inearSmoothScroller.SNAP_TO_START } } smoothScroller.targetPosition = position v... |
| 7 | mlp-00001 | HIGH | UNBALANCED_LISTENER_REGISTRATION | tv.withaibuild.customiuizer.r13 | MainActivity.kt | 44 | `XposedServiceHelper.OnServiceListener` | AppHelper.setMirrorIgnoreKeys(ignoreKeys) if (AppHelper.remotePrefs == null) { X... |
| 8 | mlp-00025 | HIGH | UNBALANCED_RECEIVER_REGISTRATION | tv.withaibuild.customiuizer.r13 | Controls.kt | 158 | `mScreenOnReceiver` | if (t is OutOfMemoryError) throw t } } mContext.registerReceiver(mScreenOnReceiv... |
| 9 | mlp-00050 | HIGH | UNBALANCED_RECEIVER_REGISTRATION | tv.withaibuild.customiuizer.r13 | GlobalActions.kt | 828 | `mGlobalReceiver` | rasitic") //intentFilter.addAction(ACTION_PREFIX + "QueryXposedService") mGlobal... |
| 10 | mlp-00051 | HIGH | UNBALANCED_RECEIVER_REGISTRATION | tv.withaibuild.customiuizer.r13 | GlobalActions.kt | 937 | `mSBReceiver` | "FastReboot") intentFilter.addAction(ACTION_PREFIX + "ScrollToTop") mStatusBarCo... |

## Top 3 strongest retention chains

### 1. `SystemUILockScreenHooks.kt:451` — UNBALANCED_LISTENER_REGISTRATION (HIGH)

- **Root**: `LISTENER_REGISTRATION` retaining `AnimatorListenerAdapter` in process `com.android.systemui`.
- **Registration site**: `) mAnimatorSet.removeAllListeners() mAnimatorSet.addListener(object : AnimatorListenerAdapter() { override f`
- **Review rationale**: Identity-based review: no matching release/removal found in the same source file.

### 2. `LauncherIconHooks.kt:169` — UNBALANCED_LISTENER_REGISTRATION (HIGH)

- **Root**: `LISTENER_REGISTRATION` retaining `TextWatcher` in process `com.miui.home`.
- **Registration site**: `tView if (mMessage != null) mMessage.addTextChangedListener(object : TextWatcher { overr`
- **Review rationale**: addTextChangedListener with an inline TextWatcher on mMessage; no removeTextChangedListener found; listener captures mMessage and multx.

### 3. `MainActivity.kt:79` — UNBALANCED_LISTENER_REGISTRATION (HIGH)

- **Root**: `LISTENER_REGISTRATION` retaining `Activity` in process `tv.withaibuild.customiuizer.r13`.
- **Registration site**: `hanged(AppHelper.remotePrefs, key, value) } AppHelper.appPrefs?.registerOnSharedPreferenceChangeListener(prefsChanged) }`
- **Review rationale**: Identity-based review: no matching release/removal found in the same source file.

## Manual supplemental coverage

- **Candidates reviewed**: 408 of 559
- **HIGH/CRITICAL manually reviewed**: 17
- **MEDIUM/UNKNOWN needing ROM/runtime evidence**: 151
- **False-positive / benign count**: 344 (`SAFE_STABLE_METADATA` + `PROCESS_LIFETIME_INTENTIONAL`) classified as not requiring production change.

## Recommended P2-1 slice

```
RECOMMENDED_P2_1 = SystemUILockScreenHooks.kt:451 unbalanced listener registration
```

### Why this is ranked first

- **Top candidate**: `SystemUILockScreenHooks.kt:451` — `AnimatorListenerAdapter` — UNBALANCED_LISTENER_REGISTRATION (HIGH).
- **Review rationale**: Identity-based review: no matching release/removal found in the same source file.
- **Scope small**: one file or a single callback site, no new architecture.
- **Regression risk low**: the fix only adds a matching `removeCallbacks` / `removeListener` call in an existing lifecycle teardown path.

### P2-1 status

```
P2-1 = NOT_STARTED
P2-0 = AUDIT_COMPLETE
```

## Static scanner note

The scanner only discovers *candidates*. It does not prove runtime memory leaks. All HIGH/CRITICAL items were manually reviewed; MEDIUM/UNKNOWN items need ROM/runtime evidence before production change.
