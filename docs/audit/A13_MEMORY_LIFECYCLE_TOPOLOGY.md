# A13-PERF-P2-0 Memory & Lifecycle Ownership / Retention Topology

## Metadata

| Field | Value |
|-------|-------|
| Task | `A13-PERF-P2-0` |
| Base SHA | `283e731b9f998c4fe188d919e3bddae1c0a5648c` |
| Branch | `devin/a13-memory-performance-optimization` |
| Scope | `app/src/main/java/**` |
| Production changes | `FORBIDDEN` in P2-0 |

## Summary counts

| Risk | Count |
|------|-------|
| HIGH | 4 |
| MEDIUM | 96 |
| LOW | 57 |
| INFO | 117 |
| UNKNOWN | 8 |

| Classification | Count |
|----------------|-------|
| SAFE_STABLE_METADATA | 117 |
| UNKNOWN_REQUIRES_MANUAL_REVIEW | 68 |
| LIFECYCLE_MANAGED | 51 |
| WEAK_EDGE_WITH_MANAGED_ROOT | 29 |
| UNBOUNDED_OWNER_COLLECTION | 7 |
| PROCESS_LIFETIME_INTENTIONAL | 5 |
| UNBALANCED_LISTENER_REGISTRATION | 2 |
| DELAYED_CALLBACK_OWNER_RETENTION | 1 |
| STRONG_SHORT_OWNER_FROM_PROCESS_ROOT | 1 |
| UNPROVEN_RELEASE_PATH | 1 |

| Root kind | Count |
|-----------|-------|
| ADDITIONAL_INSTANCE_FIELD | 77 |
| KOTLIN_OBJECT_FIELD | 66 |
| STATIC_FIELD | 43 |
| HANDLER | 33 |
| WEAK_REFERENCE | 29 |
| LISTENER_REGISTRATION | 9 |
| BROADCAST_RECEIVER_REGISTRATION | 9 |
| INSTANCE_FIELD | 7 |
| CONTENT_OBSERVER_REGISTRATION | 4 |
| THREAD_EXECUTOR | 4 |
| CALLBACK_REGISTRATION | 1 |

| Process | Candidate count |
|---------|-----------------|
| tv.withaibuild.customiuizer.r13 | 167 |
| system_server | 55 |
| com.android.systemui | 38 |
| com.miui.home | 22 |

## Process roots

### tv.withaibuild.customiuizer.r13

- Total candidates: 167
- KOTLIN_OBJECT_FIELD: 43
- STATIC_FIELD: 42
- HANDLER: 24
- WEAK_REFERENCE: 21
- ADDITIONAL_INSTANCE_FIELD: 10
- BROADCAST_RECEIVER_REGISTRATION: 9
- INSTANCE_FIELD: 7
- LISTENER_REGISTRATION: 5
- THREAD_EXECUTOR: 4
- CONTENT_OBSERVER_REGISTRATION: 1
- CALLBACK_REGISTRATION: 1

### system_server

- Total candidates: 55
- ADDITIONAL_INSTANCE_FIELD: 39
- HANDLER: 6
- WEAK_REFERENCE: 6
- KOTLIN_OBJECT_FIELD: 3
- CONTENT_OBSERVER_REGISTRATION: 1

### com.android.systemui

- Total candidates: 38
- ADDITIONAL_INSTANCE_FIELD: 16
- KOTLIN_OBJECT_FIELD: 13
- LISTENER_REGISTRATION: 2
- HANDLER: 2
- CONTENT_OBSERVER_REGISTRATION: 2
- WEAK_REFERENCE: 2
- STATIC_FIELD: 1

### com.miui.home

- Total candidates: 22
- ADDITIONAL_INSTANCE_FIELD: 12
- KOTLIN_OBJECT_FIELD: 7
- LISTENER_REGISTRATION: 2
- HANDLER: 1

## Registration / callback roots

### BROADCAST_RECEIVER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt:158` — mScreenOnReceiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:828` — mGlobalReceiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:937` — mSBReceiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:513` — showReceiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:769` — null — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:720` — receiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java:801` — receiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt:160` — devicesReceiver — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt:196` — wifiReceiver — LIFECYCLE_MANAGED (MEDIUM)

### CONTENT_OBSERVER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt:135` — Settings.System.getUriFor — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:103` — Settings.Global.getUriFor — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:104` — Settings.Global.getUriFor — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:862` — Settings.System.getUriFor — LIFECYCLE_MANAGED (MEDIUM)

### LISTENER_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:44` — XposedServiceHelper.OnServiceListener — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:79` — Activity — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt:84` — searchTextWatcher — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt:236` — shakeMgr — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt:169` — TextWatcher — UNBALANCED_LISTENER_REGISTRATION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:451` — AnimatorListenerAdapter — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:1389` — attachStateListener — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:271` — listener — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceBootstrap.java:233` — l — LIFECYCLE_MANAGED (MEDIUM)

### CALLBACK_REGISTRATION

- `app/src/main/java/tv/withaibuild/customiuizer/subs/WebPage.kt:36` — this — UNBALANCED_LISTENER_REGISTRATION (HIGH)

## Async roots (Handler / Runnable / Thread / Executor)

- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:88` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:100` — HANDLER — retained `Fragment` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:202` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt:209` — HANDLER — retained `Fragment` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt:234` — HANDLER — retained `Fragment` — DELAYED_CALLBACK_OWNER_RETENTION (HIGH)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:76` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:259` — HANDLER — retained `Runnable` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:288` — HANDLER — retained `Runnable` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt:392` — HANDLER — retained `Runnable` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt:76` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt:110` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt:455` — HANDLER — retained `runnable` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt:568` — HANDLER — retained `runnable` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt:626` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt:126` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:650` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:96` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:113` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:286` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:286` — HANDLER — retained `Runnable` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:518` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:853` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt:200` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt:214` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:66` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt:81` — THREAD_EXECUTOR — retained `ThreadPoolExecutor` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt:209` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt:210` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt:81` — HANDLER — retained `Thread` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt:94` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/System_AirplaneModeConfig.kt:19` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt:114` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt:91` — THREAD_EXECUTOR — retained `Object` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt:111` — THREAD_EXECUTOR — retained `Object` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt:403` — HANDLER — retained `Handler` — LIFECYCLE_MANAGED (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/BitmapCachedLoader.kt:181` — THREAD_EXECUTOR — retained `Object` — UNPROVEN_RELEASE_PATH (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/BitmapCachedLoader.kt:193` — HANDLER — retained `Handler` — PROCESS_LIFETIME_INTENTIONAL (MEDIUM)

## Collection roots

- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt:44` — `XposedServiceHelper.OnServiceListener` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt:34` — `ConcurrentHashMap<String, Pair<Float, Rect?>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt:407` — `MainModule.mPrefs.getStringSet` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt:410` — `MainModule.mPrefs.getStringSet` — UNKNOWN_REQUIRES_MANUAL_REVIEW (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt:135` — `Settings.System.getUriFor` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt:451` — `AnimatorListenerAdapter` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:103` — `Settings.Global.getUriFor` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt:104` — `Settings.Global.getUriFor` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:59` — `List<String>?` — WEAK_EDGE_WITH_MANAGED_ROOT (LOW)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt:1389` — `attachStateListener` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt:862` — `Settings.System.getUriFor` — LIFECYCLE_MANAGED (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookInstaller.kt:30` — `MutableMap<String, HookTargetRecord>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceSchema.kt:325` — `Map<String, PreferenceEntry>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt:42` — `java.util.LinkedHashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt:46` — `java.util.LinkedHashSet<String>` — UNKNOWN_REQUIRES_MANUAL_REVIEW (UNKNOWN)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AppHelper.kt:48` — `Set<String>` — SAFE_STABLE_METADATA (INFO)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:78` — `ArrayList<AppData>?` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:80` — `ArrayList<AppData>?` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:82` — `ArrayList<AppData>?` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:84` — `ArrayList<AppData>?` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:86` — `ArrayList<ModData>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)
- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt:117` — `HashSet<String>` — UNBOUNDED_OWNER_COLLECTION (MEDIUM)

## Safe metadata roots

- Total SAFE_STABLE_METADATA candidates: 117
- Examples: `Method`, `Field`, `Class`, `String` constants, `Int`/`Long` config, reflection metadata.
- These are stable process-lifetime metadata, not short-lived Android owner retention.

## Unknowns / manual-review queue

- Total UNKNOWN or MEDIUM candidates requiring manual review: 104
- These need ROM/runtime evidence to confirm release path, owner lifetime, or callback capture.
- WeakReference edges still require their registration root to be reviewed.

## Top 10 retention candidates

| Rank | ID | Risk | Classification | Process | Source | Line | Retained | Notes |
|------|----|------|----------------|---------|--------|------|----------|-------|
| 1 | mlp-00037 | HIGH | UNBALANCED_LISTENER_REGISTRATION | com.miui.home | LauncherIconHooks.kt | 169 | `TextWatcher` | ram.getThisObject(), "mMessage") as? TextView if (mMessage != null) mMessage.add... |
| 2 | mlp-00009 | HIGH | DELAYED_CALLBACK_OWNER_RETENTION | tv.withaibuild.customiuizer.r13 | SubFragment.kt | 234 | `Fragment` | inearSmoothScroller.SNAP_TO_START } } smoothScroller.targetPosition = position v... |
| 3 | mlp-00211 | HIGH | STRONG_SHORT_OWNER_FROM_PROCESS_ROOT | tv.withaibuild.customiuizer.r13 | StepCounterController.kt | 173 | `QueryTicket,
        val context` | StepViewRef( val ref: WeakReference<TextView>, val tag: String? ) private class ... |
| 4 | mlp-00227 | HIGH | UNBALANCED_LISTENER_REGISTRATION | tv.withaibuild.customiuizer.r13 | WebPage.kt | 36 | `this` | reActivity().onBackPressed() } } } requireActivity().onBackPressedDispatcher.add... |
| 5 | mlp-00048 | MEDIUM | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemAudioAndVisualAndMoreHooks.kt | 527 | `XposedHelpers.getLongField` | ck) { val lp = if (param.args.size == 1) param.args[0] else param.args[1] Xposed... |
| 6 | mlp-00050 | MEDIUM | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemChargingAndWallpaperHooks.kt | 111 | `wallpaperHandler` | ler == null) { wallpaperHandler = Handler(Looper.getMainLooper()) XposedHelpers.... |
| 7 | mlp-00051 | MEDIUM | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemChargingAndWallpaperHooks.kt | 183 | `wallpaperRunnable` | } } wallpaperHandler.postDelayed(wallpaperRunnable, 1800) XposedHelpers.setAddit... |
| 8 | mlp-00055 | MEDIUM | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 126 | `currentTouchTime` | } currentTouchTime = 0L } XposedHelpers.setAdditionalInstanceField(view, "curren... |
| 9 | mlp-00056 | MEDIUM | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 127 | `currentTouchX` | sedHelpers.setAdditionalInstanceField(view, "currentTouchTime", currentTouchTime... |
| 10 | mlp-00057 | MEDIUM | UNKNOWN_REQUIRES_MANUAL_REVIEW | system_server | SystemDisplayAndWindowHooks.kt | 128 | `currentTouchY` | XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", currentTouchX) X... |

## Top 3 strongest retention chains

### 1. LauncherIconHooks TextWatcher on `mMessage` (com.miui.home)

- Root: `TextView mMessage` receives `addTextChangedListener(object : TextWatcher { ... })`.
- Edge: STRONG listener registration with no matching `removeTextChangedListener`.
- Captured owner: `mMessage` (TextView) and `multx` (Float) are captured by the anonymous listener.
- Lifecycle: The listener is added inside an `after` hook each time the view is bound; repeated binding may accumulate listeners.
- Cardinality: unbounded with view rebinding; release path not proven.
- Risk: HIGH — unbalanced listener on a Launcher UI object.

### 2. SubFragment delayed smooth-scroller (tv.withaibuild.customiuizer.r13)

- Root: `view?.postDelayed({ ... }, 380)` inside `SubFragment.scrollToKey()/`.
- Edge: STRONG delayed Runnable held by the view's Handler.
- Captured owner: the lambda captures `smoothScroller` and `mList` (RecyclerView) which hold the Fragment's context.
- Lifecycle: The Fragment/View may be destroyed before 380ms elapse; no `removeCallbacks` is called in `onDestroyView`.
- Cardinality: one per scroll; can queue multiple if called repeatedly.
- Risk: HIGH — short-lived Fragment/View retained by a delayed callback with no explicit release.

### 3. WebPage OnBackPressedDispatcher callback (tv.withaibuild.customiuizer.r13)

- Root: `requireActivity().onBackPressedDispatcher.addCallback(this, callback)`.
- Edge: STRONG callback registration using the Fragment as `LifecycleOwner`.
- Captured owner: `callback` is an anonymous `OnBackPressedCallback` that captures `webView` and `mWebView`.
- Lifecycle: The callback is lifecycle-aware (removed automatically when the Fragment is destroyed), so the static risk is lower, but the scanner cannot confirm the release contract from source alone.
- Risk: scanner says HIGH, but after manual review the lifecycle-aware dispatcher lowers it to MEDIUM / false-positive unless `callback` also captures a non-lifecycle object.

## Manual supplemental coverage

Manual `rg`/`grep` cross-check performed over `app/src/main/java/**` for the keyword groups in section 25 of the task:

| Pattern | Manual grep hits | Scanner candidates | Notes |
|---------|------------------|--------------------|-------|
| `registerReceiver` | 30 | 9 | Utility registrations in `ModuleHelper` hide actual callers; scanner counts 9 top-level sites. |
| `registerContentObserver` | 6 | 4 | One `getIntExtra`/null-receiver site is not a retained observer. |
| `addListener`/`registerListener`/`addCallback`/etc. | 173 | 10 | Many `setOnXxxListener` assignments are one-shot view listeners, not add/remove registries. |
| `postDelayed` / `post(` | 49 | 33 | Includes `Handler` construction and `sendMessageDelayed`. |
| `WeakReference` / `WeakHashMap` / `SoftReference` | 44 | 29 | Field declarations only; call-site WeakReference not all captured. |
| `Thread` / `Executor` / `ExecutorService` / `Timer` | 35 | 4 | Most are imports or type references, not field roots. |
| `setAdditionalInstanceField` | 83 | 77 | 6 sites are `get`/`remove` helpers, not set roots. |
| `HashMap` / `ArrayList` / `ArrayDeque` / `SparseArray` / etc. | 252 | field-level collection roots in counts | Many are local or generic references. |

- **Manual supplemental count**: 0 new HIGH/CRITICAL candidates discovered beyond the scanner output.
- **False-positive / benign count**: 122 (117 `SAFE_STABLE_METADATA` + 5 `PROCESS_LIFETIME_INTENTIONAL`) classified as not requiring production change.

## Recommended P2-1 slice

```
RECOMMENDED_P2_1 = SubFragment.kt smooth-scroller delayed callback cleanup
```

### Why this is ranked first

- **Lifecycle mismatch**: a `Fragment` / `View` posts a delayed `Runnable` that captures `smoothScroller` and `mList`.
- **No proven release**: `SubFragment` has no `removeCallbacks` call for this specific delayed runnable in `onDestroyView` / `onPause`.
- **Multiplicity**: `scrollToKey()` can be invoked repeatedly, queuing multiple delayed runnables.
- **Statically verifiable**: fix is adding a `Runnable` field and `removeCallbacks` in the Fragment's view destruction path; can be tested with a unit test that checks the runnable is removed.
- **Scope small**: one file, one feature, no new architecture.
- **Regression risk low**: the delayed scroll is a UI convenience; removing it when the view is gone is safe.
- **Does not intersect P1B frozen slices**: `SubFragment.kt` is not in `SystemUILockScreenHooks`, `SystemUINotificationHooks`, `P1B-4A`, `SystemAudioAndVolumeHooks`, or P0 tooling.

### Alternative top candidate

- `LauncherIconHooks.kt:169` `addTextChangedListener` on `mMessage` is the highest-risk process (Launcher) and has the clearest unbalanced listener pattern, but it intersects the P1B-1 / Launcher slice; a dedicated P2 task should authorize reopening that slice before production change.

### P2-1 status

```
P2-1 = NOT_STARTED
P2-0 = AUDIT_COMPLETE
```

## Static scanner note

The scanner only discovers *candidates*. It does not prove runtime memory leaks. All HIGH/CRITICAL items were manually reviewed; MEDIUM/UNKNOWN items need ROM/runtime evidence before production change.
