import shutil
import tempfile
import unittest
from pathlib import Path

import tools.a13_memory_lifecycle_scan as mls


class MemoryLifecycleScanTest(unittest.TestCase):
    """Static candidate scanner unit tests.

    These tests verify that the scanner discovers candidates, classifies them
    conservatively, and produces deterministic, path-independent output.
    """

    @classmethod
    def setUpClass(cls):
        cls._original_repo_root = mls.REPO_ROOT

    @classmethod
    def tearDownClass(cls):
        mls.REPO_ROOT = cls._original_repo_root

    def _make_repo(self, files: dict[str, str]) -> Path:
        td = Path(tempfile.mkdtemp())
        src = td / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer"
        src.mkdir(parents=True, exist_ok=True)
        for rel, content in files.items():
            (src / rel).write_text(content, encoding="utf-8")
        return td

    def test_static_activity_field_is_candidate(self):
        repo = self._make_repo({
            "StaticOwner.kt": """
object StaticOwner {
    var leakedActivity: Activity? = null
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        acts = [c for c in candidates if c.retained_type.lower().startswith("activity") and c.root_kind == "KOTLIN_OBJECT_FIELD"]
        self.assertEqual(len(acts), 1, f"expected 1 static Activity field, got {acts}")
        self.assertEqual(acts[0].classification, "STRONG_SHORT_OWNER_FROM_PROCESS_ROOT")
        self.assertEqual(acts[0].risk, "HIGH")

    def test_static_view_field_is_candidate(self):
        repo = self._make_repo({
            "StaticOwner.kt": """
object StaticOwner {
    var leakedView: View? = null
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        views = [c for c in candidates if c.retained_type.lower().startswith("view") and c.root_kind == "KOTLIN_OBJECT_FIELD"]
        self.assertEqual(len(views), 1)
        self.assertEqual(views[0].classification, "STRONG_SHORT_OWNER_FROM_PROCESS_ROOT")

    def test_static_method_is_safe_metadata(self):
        repo = self._make_repo({
            "Reflect.kt": """
object Reflect {
    val methodRef: Method? = null
    val fieldRef: Field? = null
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        safe = [c for c in candidates if c.classification == "SAFE_STABLE_METADATA"]
        self.assertGreaterEqual(len(safe), 2, f"expected at least Method/Field metadata, got {safe}")

    def test_application_context_not_auto_leak(self):
        repo = self._make_repo({
            "App.kt": """
object App {
    val app: Application? = null
    val ctx: Context? = null
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        for c in candidates:
            if c.retained_type.lower().startswith("application"):
                self.assertIn(c.classification, ("PROCESS_LIFETIME_INTENTIONAL", "STRONG_SHORT_OWNER_FROM_PROCESS_ROOT"))

    def test_register_receiver_balanced(self):
        repo = self._make_repo({
            "Receiver.kt": """
class Receiver {
    fun on() {
        context.registerReceiver(receiver, filter)
    }
    fun off() {
        context.unregisterReceiver(receiver)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "BROADCAST_RECEIVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "LIFECYCLE_MANAGED")
        self.assertEqual(regs[0].risk, "MEDIUM")

    def test_register_receiver_unbalanced(self):
        repo = self._make_repo({
            "ReceiverOnly.kt": """
class ReceiverOnly {
    fun on() {
        context.registerReceiver(receiver, filter)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "BROADCAST_RECEIVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "UNBALANCED_RECEIVER_REGISTRATION")
        self.assertEqual(regs[0].risk, "HIGH")

    def test_register_content_observer_unbalanced(self):
        repo = self._make_repo({
            "ObserverOnly.kt": """
class ObserverOnly {
    fun on() {
        resolver.registerContentObserver(uri, false, observer)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "CONTENT_OBSERVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "UNBALANCED_OBSERVER_REGISTRATION")

    def test_add_listener_balanced(self):
        repo = self._make_repo({
            "Listener.kt": """
class Listener {
    fun on() {
        target.addListener(listener)
    }
    fun off() {
        target.removeListener(listener)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind in ("LISTENER_REGISTRATION", "CALLBACK_REGISTRATION")]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "LIFECYCLE_MANAGED")

    def test_post_delayed_capturing_owner(self):
        repo = self._make_repo({
            "MainActivity.kt": """
class MainActivity {
    val handler = Handler(Looper.getMainLooper())
    fun schedule() {
        handler.postDelayed({ doSomething() }, 1000)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        posts = [c for c in candidates if c.root_kind == "HANDLER" and c.retained_type.lower().startswith("activity")]
        self.assertEqual(len(posts), 1)
        self.assertEqual(posts[0].classification, "DELAYED_CALLBACK_OWNER_RETENTION")
        self.assertEqual(posts[0].risk, "HIGH")

    def test_remove_callbacks_recognized(self):
        repo = self._make_repo({
            "Clean.kt": """
class Clean {
    val handler = Handler(Looper.getMainLooper())
    fun schedule() {
        handler.postDelayed({ doSomething() }, 1000)
    }
    fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        posts = [c for c in candidates if c.root_kind == "HANDLER"]
        self.assertEqual(len(posts), 1)
        self.assertEqual(posts[0].classification, "LIFECYCLE_MANAGED")
        self.assertIn("removeCallbacks", posts[0].release_site)

    def test_weak_reference_not_auto_safe(self):
        repo = self._make_repo({
            "Weak.kt": """
object Weak {
    val weakAct = WeakReference<Activity>(activity)
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        weaks = [c for c in candidates if c.root_kind == "WEAK_REFERENCE"]
        self.assertEqual(len(weaks), 1)
        self.assertEqual(weaks[0].edge_strength, "WEAK")

    def test_bounded_deque_recognized(self):
        repo = self._make_repo({
            "Bounded.kt": """
object Bounded {
    val stale = ArrayDeque<View>(10)
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        collections = [c for c in candidates if c.root_kind == "KOTLIN_OBJECT_FIELD" and "deque" in c.retained_type.lower()]
        self.assertEqual(len(collections), 1)

    def test_unbounded_map_owner_values_recognized(self):
        repo = self._make_repo({
            "Unbounded.kt": """
object Unbounded {
    val owners: HashMap<String, Activity> = HashMap()
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        maps = [c for c in candidates if "HashMap" in c.retained_type and c.retained_type.lower().startswith("hashmap")]
        self.assertEqual(len(maps), 1)
        self.assertEqual(maps[0].classification, "UNBOUNDED_OWNER_COLLECTION")
        self.assertEqual(maps[0].risk, "HIGH")

    def test_additional_instance_field_not_process_global_root(self):
        repo = self._make_repo({
            "Xposed.kt": """
class Xposed {
    fun hook(param: MethodHookParam) {
        XposedHelpers.setAdditionalInstanceField(param.thisObject, "tag", view)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        adds = [c for c in candidates if c.root_kind == "ADDITIONAL_INSTANCE_FIELD"]
        self.assertEqual(len(adds), 1)
        self.assertEqual(adds[0].root_kind, "ADDITIONAL_INSTANCE_FIELD")
        self.assertNotEqual(adds[0].root_kind, "KOTLIN_OBJECT_FIELD")

    def test_source_path_independence(self):
        files = {
            "PathA.kt": """
object PathA {
    var activity: Activity? = null
}
"""
        }
        repo_a = self._make_repo(files)
        repo_b = Path(tempfile.mkdtemp())
        shutil.copytree(repo_a, repo_b, dirs_exist_ok=True)
        mls.REPO_ROOT = repo_a
        c_a = [mls.asdict(c) for c in mls.scan(repo_a)]
        mls.REPO_ROOT = repo_b
        c_b = [mls.asdict(c) for c in mls.scan(repo_b)]
        self.assertEqual(c_a, c_b)

    def test_deterministic_json_ordering(self):
        files = {
            "Order.kt": """
object Order {
    var a: Activity? = null
    var b: View? = null
}
"""
        }
        repo = self._make_repo(files)
        mls.REPO_ROOT = repo
        c1 = [mls.asdict(c) for c in mls.scan(repo)]
        c2 = [mls.asdict(c) for c in mls.scan(repo)]
        self.assertEqual(c1, c2)

    # Negative identity-proof tests (R1)

    def test_register_receiver_mismatched_not_balanced(self):
        repo = self._make_repo({
            "MismatchedReceiver.kt": """
class MismatchedReceiver {
    fun on() {
        context.registerReceiver(receiverA, filter)
    }
    fun off() {
        context.unregisterReceiver(receiverB)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "BROADCAST_RECEIVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "UNBALANCED_RECEIVER_REGISTRATION")
        self.assertEqual(regs[0].risk, "HIGH")

    def test_register_observer_mismatched_not_balanced(self):
        repo = self._make_repo({
            "MismatchedObserver.kt": """
class MismatchedObserver {
    fun on() {
        resolver.registerContentObserver(uri, false, observerA)
    }
    fun off() {
        resolver.unregisterContentObserver(observerB)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "CONTENT_OBSERVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "UNBALANCED_OBSERVER_REGISTRATION")
        self.assertEqual(regs[0].risk, "HIGH")

    def test_listener_mismatched_not_balanced(self):
        repo = self._make_repo({
            "MismatchedListener.kt": """
class MismatchedListener {
    fun on() {
        target.addListener(listenerA)
    }
    fun off() {
        target.removeListener(listenerB)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind in ("LISTENER_REGISTRATION", "CALLBACK_REGISTRATION")]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "UNBALANCED_LISTENER_REGISTRATION")
        self.assertEqual(regs[0].risk, "HIGH")

    def test_handler_mismatched_not_balanced(self):
        repo = self._make_repo({
            "MainActivity.kt": """
class MainActivity {
    fun schedule() {
        handlerA.postDelayed({ updateView() }, 1000)
    }
    fun cleanup() {
        handlerB.removeCallbacksAndMessages(null)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        posts = [c for c in candidates if c.root_kind == "HANDLER" and c.retained_type.lower().startswith("activity")]
        self.assertEqual(len(posts), 1)
        self.assertIn("DELAYED", posts[0].classification)
        self.assertEqual(posts[0].risk, "HIGH")

    def test_executor_mismatched_not_balanced(self):
        repo = self._make_repo({
            "MismatchedExecutor.kt": """
class MismatchedExecutor {
    val executorA = Executors.newSingleThreadExecutor()
    val executorB = Executors.newSingleThreadExecutor()
    fun cleanup() {
        executorB.shutdown()
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        exes = [c for c in candidates if c.root_kind == "THREAD_EXECUTOR"]
        self.assertEqual(len(exes), 2)
        a = [c for c in exes if c.retained_type == "executorA"]
        self.assertEqual(len(a), 1)
        self.assertIn("UNPROVEN", a[0].classification)
        self.assertEqual(a[0].risk, "HIGH")

    def test_exact_identity_release_balanced(self):
        repo = self._make_repo({
            "ExactMatch.kt": """
class ExactMatch {
    fun on() {
        target.addListener(listenerA)
        context.registerReceiver(receiverA, filter)
    }
    fun off() {
        target.removeListener(listenerA)
        context.unregisterReceiver(receiverA)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        listener = [c for c in candidates if c.root_kind in ("LISTENER_REGISTRATION", "CALLBACK_REGISTRATION")]
        receiver = [c for c in candidates if c.root_kind == "BROADCAST_RECEIVER_REGISTRATION"]
        self.assertEqual(len(listener), 1)
        self.assertEqual(len(receiver), 1)
        self.assertEqual(listener[0].classification, "LIFECYCLE_MANAGED")
        self.assertEqual(receiver[0].classification, "LIFECYCLE_MANAGED")

    # R2 negative identity-proof tests

    def test_receiver_let_alias_unbalanced(self):
        """identity?.let { receiver.unregister(it) } must match identity."""
        repo = self._make_repo({
            "Alias.kt": """
class Alias {
    fun on() {
        context.registerReceiver(receiverA, filter)
    }
    fun off() {
        context?.let { it.unregisterReceiver(receiverB) }
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "BROADCAST_RECEIVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertIn("UNBALANCED", regs[0].classification)

    def test_receiver_target_let_alias_balanced(self):
        """receiver?.let { it.unregister(identity) } must match identity."""
        repo = self._make_repo({
            "Alias.kt": """
class Alias {
    fun on() {
        context?.registerReceiver(receiverA, filter)
    }
    fun off() {
        context?.let {
            try { it.unregisterReceiver(receiverA) } catch (t: Throwable) {}
        }
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "BROADCAST_RECEIVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].classification, "LIFECYCLE_MANAGED")

    def test_handler_runnable_let_alias_balanced(self):
        """runnableField?.let { handler.removeCallbacks(it) } must match."""
        repo = self._make_repo({
            "MainFragment.kt": """
class MainFragment {
    private var mRunnable: Runnable? = null
    private var mHandler: Handler? = null
    fun schedule() {
        mRunnable?.let { mHandler?.removeCallbacks(it) }
        val runnable = Runnable { doWork() }
        mRunnable = runnable
        mHandler?.postDelayed(runnable, 800)
    }
    fun destroy() {
        mRunnable?.let { mHandler?.removeCallbacks(it) }
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        posts = [c for c in candidates if c.root_kind == "HANDLER" and "postDelayed" in (c.root_description or "")]
        self.assertEqual(len(posts), 1)
        self.assertEqual(posts[0].classification, "LIFECYCLE_MANAGED")

    def test_remove_all_listeners_bounded(self):
        """removeAllListeners before addListener is bounded replacement, not HIGH."""
        repo = self._make_repo({
            "AnimatorHooks.kt": """
class AnimatorHooks {
    fun bind(anim: AnimatorSet) {
        anim.pause()
        anim.removeAllListeners()
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator?) {}
        })
        anim.resume()
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        listeners = [c for c in candidates if c.root_kind in ("LISTENER_REGISTRATION", "CALLBACK_REGISTRATION")]
        self.assertEqual(len(listeners), 1)
        self.assertEqual(listeners[0].classification, "BOUNDED_REPLACEMENT_RETENTION")
        self.assertNotEqual(listeners[0].risk, "HIGH")

    def test_raw_high_without_source_proof_not_reviewed(self):
        """A raw HIGH with no release and no per-source rule must be NEEDS_MANUAL_REVIEW."""
        repo = self._make_repo({
            "Unproven.kt": """
class Unproven {
    fun on() {
        context.registerReceiver(receiverA, filter)
    }
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        regs = [c for c in candidates if c.root_kind == "BROADCAST_RECEIVER_REGISTRATION"]
        self.assertEqual(len(regs), 1)
        self.assertEqual(regs[0].review_status, "NEEDS_MANUAL_REVIEW")

    # R3 collection semantics regression tests

    def test_arraylist_string_not_owner_collection(self):
        repo = self._make_repo({
            "HookedTiles.kt": """
object HookedTiles {
    val hookedTiles = ArrayList<String>()
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        cols = [c for c in candidates if c.root_kind in ("KOTLIN_OBJECT_FIELD", "STATIC_FIELD")]
        self.assertEqual(len(cols), 1)
        self.assertNotEqual(cols[0].classification, "UNBOUNDED_OWNER_COLLECTION")
        self.assertEqual(cols[0].classification, "PROCESS_LIFETIME_METADATA_COLLECTION")

    def test_hashmap_view_is_owner_collection(self):
        repo = self._make_repo({
            "ViewCache.kt": """
object ViewCache {
    val views = HashMap<String, android.view.View>()
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        cols = [c for c in candidates if c.root_kind in ("KOTLIN_OBJECT_FIELD", "STATIC_FIELD")]
        self.assertEqual(len(cols), 1)
        self.assertEqual(cols[0].classification, "UNBOUNDED_OWNER_COLLECTION")

    def test_hashmap_activity_is_owner_collection(self):
        repo = self._make_repo({
            "ActivityCache.kt": """
object ActivityCache {
    val activities = HashMap<String, android.app.Activity>()
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        cols = [c for c in candidates if c.root_kind in ("KOTLIN_OBJECT_FIELD", "STATIC_FIELD")]
        self.assertEqual(len(cols), 1)
        self.assertEqual(cols[0].classification, "UNBOUNDED_OWNER_COLLECTION")

    def test_concurrenthashmap_pair_rect_is_config_collection(self):
        repo = self._make_repo({
            "FwApps.kt": """
object FwApps {
    val fwApps = ConcurrentHashMap<String, Pair<Float, android.graphics.Rect?>>()
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        cols = [c for c in candidates if c.root_kind in ("KOTLIN_OBJECT_FIELD", "STATIC_FIELD")]
        self.assertEqual(len(cols), 1)
        self.assertEqual(cols[0].classification, "PROCESS_LIFETIME_CONFIG_COLLECTION")

    def test_reflection_cache_is_metadata_collection(self):
        repo = self._make_repo({
            "ReflectionCache.kt": """
object ReflectionCache {
    val methodCache = ConcurrentHashMap<String, java.lang.reflect.Method>()
    val constructorCache = ConcurrentHashMap<String, java.lang.reflect.Constructor<*>>()
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        cols = [c for c in candidates if c.root_kind in ("KOTLIN_OBJECT_FIELD", "STATIC_FIELD")]
        self.assertEqual(len(cols), 2)
        for c in cols:
            self.assertEqual(c.classification, "PROCESS_LIFETIME_METADATA_COLLECTION")

    def test_unknown_custom_collection_is_unknown_cardinality(self):
        repo = self._make_repo({
            "UnknownCache.kt": """
class UnknownType
object UnknownCache {
    val cache = HashMap<String, UnknownType>()
}
"""
        })
        mls.REPO_ROOT = repo
        candidates = mls.scan(repo)
        cols = [c for c in candidates if c.root_kind in ("KOTLIN_OBJECT_FIELD", "STATIC_FIELD")]
        self.assertEqual(len(cols), 1)
        self.assertEqual(cols[0].classification, "UNKNOWN_COLLECTION_CARDINALITY")
        self.assertEqual(cols[0].review_status, "NEEDS_ROM_EVIDENCE")


if __name__ == "__main__":
    unittest.main()
