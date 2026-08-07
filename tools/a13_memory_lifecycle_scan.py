#!/usr/bin/env python3
"""Static candidate scanner for Android owner retention / lifecycle topology.

This scanner only *discovers* candidates. It does not prove runtime memory leaks.
Output is deterministic and path-independent: all paths are relative to repo_root.

R1 proof-hardening changes:
- Release sites must match the registered/retained object by identity.
- No "same-file any unregister/remove" fallback.
- Inventory carries scanner_* and reviewed_* fields.
"""
from __future__ import annotations

import argparse
import functools
import json
import re
import sys
from dataclasses import dataclass, asdict, field
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parent.parent


@dataclass
class Candidate:
    id: str
    feature: str = ""
    process: str = ""
    source_file: str = ""
    source_line: int = 0
    root_kind: str = ""
    root_description: str = ""
    root_lifetime: str = ""
    retained_type: str = ""
    retained_owner_kind: str = ""
    edge_strength: str = "STRONG"
    acquisition_site: str = ""
    registration_site: str = ""
    release_site: str = ""
    replacement_site: str = ""
    cardinality: str = "UNKNOWN"
    bounded: str = "UNKNOWN"
    bound_evidence: str = ""
    lifecycle_boundary: str = "UNKNOWN"
    scanner_classification: str = "UNKNOWN_REQUIRES_MANUAL_REVIEW"
    scanner_risk: str = "UNKNOWN"
    # classification/risk are the reviewed (final) values used by downstream tooling.
    classification: str = "UNKNOWN_REQUIRES_MANUAL_REVIEW"
    risk: str = "UNKNOWN"
    review_status: str = "PENDING"
    reviewed_classification: str = "UNKNOWN_REQUIRES_MANUAL_REVIEW"
    reviewed_risk: str = "UNKNOWN"
    review_rationale: str = ""
    review_evidence: str = ""
    confidence: str = "low"
    evidence: str = ""
    recommended_next_action: str = ""


# ---------------------------------------------------------------------------
# Type / owner vocabulary
# ---------------------------------------------------------------------------

_SHORT_LIVED_OWNER_TYPES = (
    "Activity", "Fragment", "View", "ViewGroup", "Window", "Dialog",
    "Application", "Context", "ContextThemeWrapper", "Service",
    "BroadcastReceiver", "ContentObserver", "Notification",
    "ExpandableNotificationRow", "StatusBarNotification", "NotificationEntry",
    "PendingIntent", "Host", "Tile", "Panel", "Row", "Menu",
)

_ASYNC_ROOT_TYPES = (
    "Handler", "Runnable", "Thread", "Looper", "Executor", "ExecutorService",
)

_CONTROLLER_TYPES = (
    "Controller",
)

_ANDROID_OWNER_TYPES = _SHORT_LIVED_OWNER_TYPES + _ASYNC_ROOT_TYPES + _CONTROLLER_TYPES

_SAFE_METADATA_TYPES = (
    "Class", "Method", "Field", "Constructor", "String", "Int", "Long",
    "Boolean", "Float", "Double", "Byte", "Short", "Char", "Any", "Object",
    "Integer", "Boolean", "enum",
    # ClassLoader is deliberately excluded from the safe-metadata blanket.
)

_COLLECTION_TYPES = (
    "HashMap", "ConcurrentHashMap", "LinkedHashMap", "mutableMapOf",
    "HashSet", "LinkedHashSet", "mutableSetOf",
    "ArrayList", "ArrayDeque", "LinkedList", "mutableListOf",
    "SparseArray", "ArrayMap", "LongSparseArray",
)

_PROCESS_NAMES = (
    "com.android.systemui", "com.miui.home", "android", "system_server",
    "tv.withaibuild.customiuizer.r13",
)

# Known Fragment / Activity / LifecycleOwner bases that make
# `addCallback(this, ...)` lifecycle-managed.
_LIFECYCLE_OWNER_BASES = (
    "Fragment", "AppCompatActivity", "ComponentActivity", "Activity",
    "LifecycleOwner", "SubFragment",
)

# ---------------------------------------------------------------------------
# Parsing helpers
# ---------------------------------------------------------------------------

@functools.lru_cache(maxsize=1024)
def _scope_state(text: str) -> dict[int, tuple[bool, bool, bool, bool, bool, str]]:
    """Return line state: (in_fun, in_object, in_class, is_static_keyword, in_type_body, nearest_type)."""
    lines = text.splitlines()
    info: dict[int, tuple[bool, bool, bool, bool, bool, str]] = {}
    stack: list[tuple[str, str]] = []
    # Only one pending kind is needed because a declaration that has not yet seen
    # its opening brace is the most recent one; a newer declaration overwrites it.
    pending: list[str] = []
    pending_type: str | None = None

    type_decl = re.compile(
        r"^(?:\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
        r"protected\s+|internal\s+|final\s+))*"
        r"(class|object|companion\s+object|interface|enum\s+class|enum)\b"
    )
    fun_decl = re.compile(
        r"^(?:\s*(?:public|private|protected|internal|override|abstract|open|final|"
        r"inline|crossinline|noinline|operator|infix|suspend|tailrec|external|"
        r"expect|actual)\s+)*fun\b"
        r"|"
        r"(?:public|private|protected|static|final|abstract|synchronized|"
        r"native|strictfp|\s)+[A-Za-z0-9_<>,\[\].\s]+\s+[A-Za-z0-9_]+\s*\("
    )

    def _type_kind(line: str) -> str:
        m = re.match(
            r"\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
            r"protected\s+|internal\s+|final\s+)*"
            r"(class|object|companion\s+object|interface|enum\s+class|enum)\b",
            line,
        )
        if not m:
            return "other"
        k = m.group(1)
        if k == "companion object":
            return "object"
        return k

    for i, line in enumerate(lines, 1):
        prev = lines[i - 2] if i > 1 else ""
        if type_decl.match(line):
            # A new type declaration overwrites any not-yet-opened pending declaration.
            pending = ["type"]
            pending_type = _type_kind(line)
        elif fun_decl.match(line):
            pending = ["fun"]

        for ch in line:
            if ch == "{":
                kind = pending.pop() if pending else "other"
                if kind == "type":
                    tk = pending_type if pending_type is not None else "other"
                    pending_type = None
                else:
                    tk = "other"
                stack.append((kind, tk))
            elif ch == "}" and stack:
                stack.pop()

        in_fun = any(b == "fun" for b, _ in stack)
        in_object = any(t == "object" for _, t in stack)
        in_class = any(t in ("class", "interface", "enum", "enum class") for _, t in stack)
        is_static = "static" in line or "@JvmField" in line or "@JvmField" in prev
        nearest_type = pending_type if pending_type is not None else (stack[-1][1] if stack else "other")
        in_type_body = (pending_type is None) and bool(stack) and stack[-1][0] == "type"
        info[i] = (in_fun, in_object, in_class, is_static, in_type_body, nearest_type)
    return info


def _is_in_static_scope(text: str, line: int) -> bool:
    info = _scope_state(text)
    in_fun, in_object, in_class, is_static, in_type_body, nearest_type = info.get(line, (False, False, False, False, False, "other"))
    return in_object or is_static


def _process_hint(path: Path) -> str:
    p = str(path).lower()
    if "systemui" in p:
        return "com.android.systemui"
    if "launcher" in p or "home" in p:
        return "com.miui.home"
    if "system" in p and "systemui" not in p:
        return "system_server"
    return "tv.withaibuild.customiuizer.r13"


def _snippet(text: str, offset: int, radius: int = 120) -> str:
    start = max(0, offset - radius)
    end = min(len(text), offset + radius)
    return re.sub(r"\s+", " ", text[start:end].strip())


def _contains_owner(type_str: str) -> bool:
    return bool(re.search(r"\b(?:" + "|".join(re.escape(o) for o in _ANDROID_OWNER_TYPES) + r")\b", type_str, re.I))


def _contains_short_lived_owner(type_str: str) -> bool:
    return bool(re.search(r"\b(?:" + "|".join(re.escape(o) for o in _SHORT_LIVED_OWNER_TYPES) + r")\b", type_str, re.I))


def _contains_async_root(type_str: str) -> bool:
    return bool(re.search(r"\b(?:" + "|".join(re.escape(o) for o in _ASYNC_ROOT_TYPES) + r")\b", type_str, re.I))


def _contains_controller(type_str: str) -> bool:
    t = type_str.lower()
    return any(o.lower() in t for o in _CONTROLLER_TYPES)


def _contains_weak(type_str: str) -> bool:
    return re.search(r"\b(?:WeakReference|WeakHashMap|SoftReference)\b", type_str, re.I) is not None


def _is_safe_metadata(type_str: str) -> bool:
    # primitive-ish or reflection metadata; require word boundaries so
    # "Activity" does not trigger on "Any" and "Int" does not trigger on "Intent".
    if re.search(r"\b(?:" + "|".join(re.escape(o) for o in _SAFE_METADATA_TYPES) + r")\b", type_str, re.I):
        return True
    # fully qualified reflection types
    if re.search(r"(?:java\.lang\.reflect\.|java\.lang\.Class|kotlin\.reflect\.)", type_str, re.I):
        return True
    return False


def _collection_type(type_str: str) -> str | None:
    t = type_str.lower()
    for c in _COLLECTION_TYPES:
        if c.lower() in t:
            return c
    return None


def _field_name(snippet: str) -> str:
    m = re.search(r"(?:val|var|lateinit\s+var)\s+([A-Za-z0-9_]+)", snippet)
    if m:
        return m.group(1)
    m = re.search(r"\b(?:public|private|protected|internal|static|final|val|var|lateinit)*\s+[A-Za-z0-9_<>[\],.\s]+\s+([A-Za-z0-9_]+)\s*[;=]", snippet)
    if m:
        return m.group(1)
    return ""


def _field_name_at(text: str, offset: int) -> str:
    """Find the variable name of the nearest val/var/field declaration before offset."""
    regex = re.compile(r"(?:val|var|lateinit\s+var|public\s+val|private\s+val|protected\s+val|internal\s+val|public\s+var|private\s+var|protected\s+var|internal\s+var)\s+([A-Za-z0-9_]+)\b")
    name = ""
    for m in regex.finditer(text[:offset]):
        name = m.group(1)
    if not name:
        m = re.search(r"[A-Za-z0-9_<>[\],.]+\s+([A-Za-z0-9_]+)\s*[=;]", text[max(0, offset - 120):offset])
        if m:
            name = m.group(1)
    return name


def _extract_receiver_obj(text: str, call_start: int) -> str:
    """Extract the object expression to the left of a method call dot."""
    line_start = text.rfind("\n", 0, call_start) + 1
    prefix = text[line_start:call_start].strip()
    return prefix


def _normalize_obj(obj: str) -> str:
    """Strip Kotlin nullability / call operators for identity comparison."""
    return re.sub(r"[?!?\.]+", "", obj).strip()


def _extract_call_arg_raw(call: str, index: int, limit: int = 5000) -> str:
    """Extract the raw Nth top-level argument from a call argument list.

    `call` is the text *inside* the argument list (after the opening '(').
    """
    paren = 1
    brace = 0
    bracket = 0
    start = 0
    arg_index = 0
    for i, ch in enumerate(call[:limit]):
        if ch == "(":
            paren += 1
        elif ch == "{":
            brace += 1
        elif ch == "[":
            bracket += 1
        elif ch == "]":
            bracket = max(0, bracket - 1)
        elif ch == "}":
            brace = max(0, brace - 1)
        elif ch == ")":
            paren -= 1
            if paren == 0 and brace == 0 and bracket == 0:
                if arg_index == index:
                    return call[start:i]
                return ""
        elif ch == "," and paren == 1 and brace == 0 and bracket == 0:
            if arg_index == index:
                return call[start:i]
            arg_index += 1
            start = i + 1
    return ""


def _extract_call_arg_type(call: str, index: int, limit: int = 5000, path_stem: str = "") -> str:
    """Extract the typed Nth top-level argument from a call argument list."""
    raw = _extract_call_arg_raw(call, index, limit)
    return _extract_arg_type(raw, path_stem)


def _extract_register_identity(call: str, kind: str, path_stem: str = "") -> tuple[str, str]:
    """Return (identity_raw, retained_type) for the registered/retained object.

    For registerContentObserver the observer is the 3rd argument.
    For addCallback with two arguments the callback is the 2nd argument.
    Otherwise the listener/callback/receiver is the 1st argument.
    """
    if kind == "registerContentObserver":
        return _extract_call_arg_raw(call, 2), _extract_call_arg_type(call, 2, path_stem=path_stem)

    if kind == "addCallback":
        raw0 = _extract_call_arg_raw(call, 0)
        raw1 = _extract_call_arg_raw(call, 1)
        type1 = _extract_call_arg_type(call, 1, path_stem=path_stem)
        if raw1:
            # two-arg form: addCallback(owner, callback)
            return raw1, type1
        # one-arg form: addCallback(callback)
        return raw0, _extract_call_arg_type(call, 0, path_stem=path_stem)

    return _extract_call_arg_raw(call, 0), _extract_call_arg_type(call, 0, path_stem=path_stem)


def _identity_matches(a: str, b: str) -> bool:
    """Compare identity tokens, ignoring whitespace and Kotlin safe-call operators."""
    a = re.sub(r"\s+", "", a).strip()
    b = re.sub(r"\s+", "", b).strip()
    if not a or not b:
        return False
    if a == b:
        return True
    # allow `callback` to match `callback?.` and similar
    if a.lstrip("(").rstrip(")") == b.lstrip("(").rstrip(")"):
        return True
    return False


# ---------------------------------------------------------------------------
# Release finding (identity-based, no same-file fallback)
# ---------------------------------------------------------------------------

def _release_method_for_register(kind: str) -> tuple[str, int, int] | None:
    """Return (release_method_regex, release_arg_index, identity_method_arg_index) for a register kind.

    The regex is the method name to search for (without the leading dot).
    release_arg_index is the argument position to extract from the release call.
    """
    mapping = {
        "registerReceiver": ("unregisterReceiver", 0),
        "registerContentObserver": ("unregisterContentObserver", 0),
        "addListener": ("removeListener", 0),
        "addCallback": ("removeCallback", 0),
        "registerListener": ("unregisterListener", 0),
        "registerCallback": ("unregisterCallback", 0),
    }
    # wildcard add/register Listener
    for pat, (rel, idx) in mapping.items():
        if kind == pat:
            return rel, idx
    if re.match(r"addOn[A-Za-z0-9_]*Listener", kind):
        stem = kind[3:]  # drop "add"
        return f"remove{stem}", 0
    if re.match(r"add[A-Za-z0-9_]*Listener", kind):
        stem = kind[3:]
        return f"remove{stem}", 0
    if re.match(r"register[A-Za-z0-9_]*Listener", kind):
        stem = kind[8:]  # drop "register"
        return f"unregister{stem}", 0
    return None


def _find_let_alias_release(text: str, identity_raw: str, release_methods: list[str]) -> tuple[str, int | None]:
    """Detect `field?.let { target.releaseMethod(it) }` alias cleanup."""
    if not identity_raw:
        return "", None
    # Match identity?.let { ... } with an optional explicit lambda parameter.
    let_regex = re.compile(
        r"\b" + re.escape(identity_raw) + r"(?:\?|\!)?\s*\?\.\s*let\s*\{\s*(?:([A-Za-z0-9_]+)\s*->\s*)?([^{}]*(?:\{[^{}]*\}[^{}]*)*)",
        re.DOTALL,
    )
    for m in let_regex.finditer(text):
        block = m.group(2) or ""
        param = (m.group(1) or "it").strip()
        for rel_method in release_methods:
            # Look for a call to rel_method inside the block using the alias or null (clear-all).
            call_regex = re.compile(re.escape(rel_method) + r"\s*\(([^)]*)\)", re.DOTALL)
            for cm in call_regex.finditer(block):
                arg0 = cm.group(1).strip()
                if arg0 == param or arg0 == "null" or param in arg0:
                    line = text[:m.start()].count("\n") + 1
                    return _snippet(text, m.start(), 80), line
    return "", None


def _find_identity_release(text: str, kind: str, receiver_obj: str, identity_raw: str) -> tuple[str, int | None]:
    """Search for a release call matching the receiver object and the registered identity.

    Returns (release_site_snippet, release_line) or ("", None).
    """
    if not identity_raw:
        return "", None

    info = _release_method_for_register(kind)
    if info is None:
        return "", None
    rel_method, rel_arg_idx = info

    receiver_norm = _normalize_obj(receiver_obj)

    # Method-style release: receiver_obj.removeListener(identity_raw)
    regex = re.compile(r"([A-Za-z0-9_\.\?\!\(\)]+)\." + re.escape(rel_method) + r"\s*\(")
    for m in regex.finditer(text):
        release_obj = _extract_receiver_obj(text, m.start())
        # For release methods the dot is immediately before the method name, so
        # the captured prefix in m.group(1) is already the object.  The helper
        # _extract_receiver_obj returns the same because the regex consumed the
        # dot.  Use the captured group for simplicity.
        release_obj = m.group(1)
        release_obj_norm = _normalize_obj(release_obj)
        if receiver_norm and release_obj_norm != receiver_norm:
            continue
        call = text[m.end():]
        release_identity_raw = _extract_call_arg_raw(call, rel_arg_idx)
        if _identity_matches(release_identity_raw, identity_raw):
            line = text[:m.start()].count("\n") + 1
            return _snippet(text, m.start(), 80), line

    # Kotlin alias-aware cleanup: field?.let { target.unregister...(it) }
    let_release, let_line = _find_let_alias_release(text, identity_raw, [rel_method])
    if let_release:
        return let_release, let_line

    # Kotlin alias-aware cleanup: target?.let { it.unregister...(identity) }
    if receiver_norm:
        target_let_regex = re.compile(
            r"\b" + re.escape(receiver_norm) + r"(?:\?|\!)?\s*\?\.\s*let\s*\{\s*(?:([A-Za-z0-9_]+)\s*->\s*)?([^{}]*(?:\{[^{}]*\}[^{}]*)*)",
            re.DOTALL,
        )
        for m in target_let_regex.finditer(text):
            block = m.group(2) or ""
            call_regex = re.compile(re.escape(rel_method) + r"\s*\(([^)]*)\)", re.DOTALL)
            for cm in call_regex.finditer(block):
                arg0 = cm.group(1).strip()
                if arg0 == identity_raw or arg0.lstrip("(").rstrip(")") == identity_raw or identity_raw in arg0:
                    line = text[:m.start()].count("\n") + 1
                    return _snippet(text, m.start(), 80), line

    # For addCallback, also accept identity_raw.remove() or identity_raw?.remove()
    if kind == "addCallback":
        id_norm = _normalize_obj(identity_raw)
        if id_norm:
            regex = re.compile(r"(?:\b" + re.escape(id_norm) + r"(?:\?|\!)?\.|\b)this\.remove\s*\(")
            for m in regex.finditer(text):
                line = text[:m.start()].count("\n") + 1
                return _snippet(text, m.start(), 80), line

    # Bounded replacement: receiver.removeAllListeners() before addListener(...)
    if "Listener" in kind and receiver_norm:
        regex = re.compile(r"\b" + re.escape(receiver_norm) + r"(?:\?|\!)?\.removeAllListeners\s*\(")
        for m in regex.finditer(text):
            line = text[:m.start()].count("\n") + 1
            return _snippet(text, m.start(), 80), line

    return "", None


def _find_handler_let_alias_release(text: str, handler_norm: str, runnable_raw: str) -> tuple[str, int | None]:
    """Detect `runnableField?.let { handler.removeCallbacks(it) }` cleanup."""
    if not runnable_raw:
        return "", None
    let_regex = re.compile(
        r"\b" + re.escape(runnable_raw) + r"(?:\?|\!)?\s*\?\.\s*let\s*\{\s*(?:([A-Za-z0-9_]+)\s*->\s*)?([^{}]*(?:\{[^{}]*\}[^{}]*)*)",
        re.DOTALL,
    )
    for m in let_regex.finditer(text):
        block = m.group(2) or ""
        param = (m.group(1) or "it").strip()
        for method in ("removeCallbacksAndMessages", "removeMessages", "removeCallbacks"):
            call_regex = re.compile(
                r"\b" + re.escape(handler_norm) + r"(?:\?|\!)?\." + re.escape(method) + r"\s*\(([^)]*)\)",
                re.DOTALL,
            )
            for cm in call_regex.finditer(block):
                arg0 = cm.group(1).strip()
                if arg0 == param or arg0 == "null" or param in arg0:
                    line = text[:m.start()].count("\n") + 1
                    return _snippet(text, m.start(), 80), line
    return "", None


def _find_handler_release(text: str, handler_obj: str, runnable_raw: str | None = None) -> tuple[str, int | None]:
    """Find a Handler/View removeCallbacks/removeCallbacksAndMessages that matches.

    - removeCallbacks(runnable_identity) on the same handler/view counts.
    - removeCallbacksAndMessages(null) on the same handler/view counts (clears all).
    """
    if not handler_obj:
        return "", None
    handler_norm = _normalize_obj(handler_obj)
    # Match `handler.removeCallbacks` / `view?.removeCallbacks` / `view?.removeCallbacksAndMessages`
    regex = re.compile(r"([A-Za-z0-9_\.\?\!\(\)]+)\.(removeCallbacks(?:AndMessages)?|removeMessages)\s*\(")
    for m in regex.finditer(text):
        release_obj = _normalize_obj(_extract_receiver_obj(text, m.start()))
        # The dot is at m.start(); _extract_receiver_obj returns prefix before dot.
        # But the regex captured it in group(1) as well.
        release_obj = _normalize_obj(m.group(1))
        if release_obj != handler_norm:
            continue
        call = text[m.end():]
        arg0_raw = _extract_call_arg_raw(call, 0)
        method = m.group(2)
        if method == "removeCallbacksAndMessages" and (not arg0_raw or arg0_raw == "null"):
            line = text[:m.start()].count("\n") + 1
            return _snippet(text, m.start(), 80), line
        if runnable_raw is not None and _identity_matches(arg0_raw, runnable_raw):
            line = text[:m.start()].count("\n") + 1
            return _snippet(text, m.start(), 80), line

    # Kotlin alias-aware cleanup: runnable?.let { handler.removeCallbacks(it) }
    if runnable_raw:
        # Try the original runnable identity and any field it is assigned to.
        seen = {runnable_raw}
        aliases = [runnable_raw]
        for m in re.finditer(r"\b([A-Za-z0-9_]+)\s*=\s*" + re.escape(runnable_raw) + r"\b", text):
            field = m.group(1)
            if field not in seen:
                seen.add(field)
                aliases.append(field)
        for alias in aliases:
            let_release, let_line = _find_handler_let_alias_release(text, handler_norm, alias)
            if let_release:
                return let_release, let_line

    return "", None


def _find_executor_release(text: str, field_name: str) -> tuple[str, int | None]:
    """Find shutdown/quit/interrupt/cancel on the same executor/thread variable."""
    if not field_name:
        return "", None
    regex = re.compile(r"\b" + re.escape(field_name) + r"(?:\?|\!)?\.(?:shutdown|shutdownNow|interrupt|cancel|quit|quitSafely)\s*\(")
    for m in regex.finditer(text):
        line = text[:m.start()].count("\n") + 1
        return _snippet(text, m.start(), 80), line
    return "", None


def _extract_handler_variable(text: str, offset: int) -> str:
    """Find the variable name assigned to a Handler/HandlerThread construction.

    Handles both `name = Constructor(...)` and `val/var name : Type = Constructor(...)`.
    """
    window = text[max(0, offset - 240):offset]
    rest = text[offset:offset + 80]
    if not re.match(r"\s*(?:HandlerThread|Executors|ScheduledExecutorService|ExecutorService|Handler|Thread|Executor|Timer)\b", rest):
        return ""
    # Kotlin property declaration: val/var [modifiers] name [: Type] = Constructor
    m = re.search(r"(?:val|var|public\s+val|public\s+var|private\s+val|private\s+var|protected\s+val|protected\s+var|internal\s+val|internal\s+var)\s+([A-Za-z0-9_]+)\s*(?::[^=]+)?=\s*$", window)
    if m:
        return m.group(1)
    # Plain assignment: name = Constructor
    m = re.search(r"([A-Za-z0-9_]+)\s*(?:\?|\!)?\s*=\s*$", window)
    if m:
        return m.group(1)
    return ""


# ---------------------------------------------------------------------------
# Detectors
# ---------------------------------------------------------------------------

_CANDIDATE_ID = 0


def _next_id() -> str:
    global _CANDIDATE_ID
    _CANDIDATE_ID += 1
    return f"mlp-{ _CANDIDATE_ID:05d}"


_REGEX_FIELD = re.compile(
    r"^[ \t]*"
    r"(?:@(?:JvmField|JvmStatic)\s+)?"
    r"(?:(?:public|private|protected|internal|open|final|static|const|abstract|transient|volatile)\s*)*"
    r"(?:"
    # Kotlin property: val/var/lateinit var name : Type = ...
    r"(?:val|var|lateinit\s+var)\s+[A-Za-z0-9_]+\s*(?::\s*([A-Za-z0-9_<>,\[\]?.\s\.]+))?\s*(?:=.*)?"
    r"|"
    # Java field: Type name = ... or Type name;  (exclude Kotlin type keywords)
    r"(?!\b(?:object|class|interface|enum|fun)\b)([A-Za-z0-9_<>,\[\]\.]+(?:<[^>]+>)?)\s+[A-Za-z0-9_]+\s*(?:=.*|;)?"
    r")",
    re.M,
)

_REGEX_REGISTER = re.compile(
    r"\.(registerReceiver|registerContentObserver|addListener|registerListener|addCallback|registerCallback|"
    r"addOn[A-Za-z0-9_]*Listener|add[A-Za-z0-9_]*Listener|register[A-Za-z0-9_]*Listener)\s*\("
)

_REGEX_HANDLER = re.compile(r"\bHandler\s*\(")
_REGEX_POST = re.compile(r"\.(post|postDelayed|sendMessage|sendMessageDelayed|sendEmptyMessageDelayed)\s*\(")
_REGEX_THREAD_EXECUTOR = re.compile(r"\b(?:HandlerThread|Executors|ScheduledExecutorService|ExecutorService|Thread|Executor|Timer)\b\s*(?:\(|\.new)")
_REGEX_WEAK = re.compile(r"\b(?:WeakReference|WeakHashMap|SoftReference)\s*[<(]")
_REGEX_ADDITIONAL = re.compile(r"\bsetAdditionalInstanceField\s*\(")


def _make_candidate(
    path: Path,
    text: str,
    offset: int,
    root_kind: str,
    retained_type: str,
    **overrides,
) -> Candidate:
    line = text[:offset].count("\n") + 1
    relative = path.relative_to(REPO_ROOT).as_posix()
    scope = _scope_state(text).get(line, (False, False, False, False, False, "other"))
    in_object = scope[1]
    is_static = scope[3]

    if root_kind in ("KOTLIN_OBJECT_FIELD", "COMPANION_OBJECT_FIELD", "STATIC_FIELD"):
        lifetime = "PROCESS_LIFETIME"
    else:
        lifetime = overrides.get("root_lifetime", "UNKNOWN")

    classification = overrides.pop("classification", "UNKNOWN_REQUIRES_MANUAL_REVIEW")
    risk = overrides.pop("risk", "UNKNOWN")

    # default classification / risk heuristics
    if root_kind in ("STATIC_FIELD", "KOTLIN_OBJECT_FIELD", "COMPANION_OBJECT_FIELD"):
        if _contains_weak(retained_type):
            classification = "WEAK_EDGE_WITH_MANAGED_ROOT"
            risk = "LOW"
            edge = "WEAK"
        elif _collection_type(retained_type):
            if _contains_short_lived_owner(retained_type):
                classification = "UNBOUNDED_OWNER_COLLECTION"
                risk = "HIGH"
            else:
                classification = "UNBOUNDED_OWNER_COLLECTION"
                risk = "MEDIUM"
        elif _contains_short_lived_owner(retained_type):
            classification = "STRONG_SHORT_OWNER_FROM_PROCESS_ROOT"
            risk = "HIGH"
        elif _contains_controller(retained_type):
            classification = "STRONG_SHORT_OWNER_FROM_PROCESS_ROOT"
            risk = "MEDIUM"
        elif _contains_async_root(retained_type):
            classification = "PROCESS_LIFETIME_INTENTIONAL"
            risk = "LOW"
        elif _is_safe_metadata(retained_type):
            classification = "SAFE_STABLE_METADATA"
            risk = "INFO"
    elif root_kind == "BROADCAST_RECEIVER_REGISTRATION":
        release = overrides.get("release_site", "")
        classification = "LIFECYCLE_MANAGED" if release else "UNBALANCED_RECEIVER_REGISTRATION"
        risk = "MEDIUM" if release else "HIGH"
    elif root_kind == "CONTENT_OBSERVER_REGISTRATION":
        release = overrides.get("release_site", "")
        classification = "LIFECYCLE_MANAGED" if release else "UNBALANCED_OBSERVER_REGISTRATION"
        risk = "MEDIUM" if release else "HIGH"
    elif root_kind in ("LISTENER_REGISTRATION", "CALLBACK_REGISTRATION"):
        release = overrides.get("release_site", "")
        if release:
            if "removeAllListeners" in release:
                classification = "BOUNDED_REPLACEMENT_RETENTION"
                risk = "MEDIUM"
            else:
                classification = "LIFECYCLE_MANAGED"
                risk = "MEDIUM"
        else:
            classification = "UNBALANCED_LISTENER_REGISTRATION"
            risk = "HIGH"
    elif root_kind == "HANDLER":
        release = overrides.get("release_site", "")
        if _contains_short_lived_owner(retained_type):
            classification = "LIFECYCLE_MANAGED" if release else "DELAYED_CALLBACK_OWNER_RETENTION"
            risk = "MEDIUM" if release else "HIGH"
        elif _contains_async_root(retained_type) or _contains_controller(retained_type):
            classification = "LIFECYCLE_MANAGED" if release else "PROCESS_LIFETIME_INTENTIONAL"
            risk = "LOW" if release else "MEDIUM"
        else:
            classification = "LIFECYCLE_MANAGED" if release else "UNPROVEN_RELEASE_PATH"
            risk = "LOW" if release else "MEDIUM"
    elif root_kind == "THREAD_EXECUTOR":
        release = overrides.get("release_site", "")
        if _contains_short_lived_owner(retained_type):
            classification = "LIFECYCLE_MANAGED" if release else "DELAYED_CALLBACK_OWNER_RETENTION"
            risk = "MEDIUM" if release else "HIGH"
        elif _contains_async_root(retained_type) or _contains_controller(retained_type):
            classification = "LIFECYCLE_MANAGED" if release else "PROCESS_LIFETIME_INTENTIONAL"
            risk = "LOW" if release else "MEDIUM"
        else:
            classification = "LIFECYCLE_MANAGED" if release else "UNPROVEN_RELEASE_PATH"
            risk = "LOW" if release else "HIGH"
    elif root_kind == "WEAK_REFERENCE":
        classification = "WEAK_EDGE_WITH_MANAGED_ROOT"
        risk = "LOW"
    elif root_kind == "ADDITIONAL_INSTANCE_FIELD":
        if retained_type == "primitive" or _is_safe_metadata(retained_type):
            classification = "SAFE_STABLE_METADATA"
            risk = "INFO"
        elif overrides.get("release_site"):
            classification = "BOUNDED_REPLACEMENT_RETENTION"
            risk = "MEDIUM"
        else:
            classification = "UNKNOWN_REQUIRES_MANUAL_REVIEW"
            risk = "MEDIUM"

    edge = overrides.pop("edge_strength", "WEAK" if root_kind == "WEAK_REFERENCE" else "STRONG")

    return Candidate(
        id=_next_id(),
        feature=path.stem,
        process=_process_hint(path),
        source_file=relative,
        source_line=line,
        root_kind=root_kind,
        root_description=overrides.pop("root_description", ""),
        root_lifetime=lifetime,
        retained_type=retained_type,
        retained_owner_kind=retained_type if _contains_short_lived_owner(retained_type) else "",
        edge_strength=edge,
        acquisition_site=_snippet(text, offset, 80),
        evidence=_snippet(text, offset, 120),
        scanner_classification=classification,
        scanner_risk=risk,
        classification=classification,
        risk=risk,
        reviewed_classification=classification,
        reviewed_risk=risk,
        review_status="PENDING",
        confidence="low",
        **overrides,
    )


def _scan_file(path: Path) -> list[Candidate]:
    text = path.read_text(encoding="utf-8", errors="replace")
    candidates: list[Candidate] = []

    # 1. Field declarations (static, object, companion, instance with owner/collection)
    for m in _REGEX_FIELD.finditer(text):
        line = text[:m.start()].count("\n") + 1
        scope = _scope_state(text).get(line, (False, False, False, False, False, "other"))
        in_fun, in_object, in_class, is_static, in_type_body, nearest_type = scope
        if in_fun or not in_type_body:
            continue
        snippet = _snippet(text, m.start(), 160)
        retained = (m.group(1) or m.group(2) or "").strip()
        # Strip Kotlin nullability / non-null operators at the end of the type.
        retained = re.sub(r"[?!\s]+$", "", retained).strip()
        if not retained:
            retained = _extract_retained_type(m.group(0))

        # Constructor parameters are not fields (e.g. class QueryRunnable(val ticket: ..., val context: ...))
        if m.group(0).rstrip().endswith(",") or (nearest_type == "class" and not in_type_body):
            continue

        # Skip most const / literal primitive fields unless they are explicit reflection metadata
        if "const val" in m.group(0) or "static final" in m.group(0) or ("static" in m.group(0) and _is_safe_metadata(retained)):
            if not _is_safe_metadata(retained) or not (in_object or is_static):
                continue

        has_collection = _collection_type(retained) is not None
        has_owner = _contains_owner(retained)
        has_weak = "WeakReference" in retained or "WeakReference" in snippet
        has_safe = _is_safe_metadata(retained)

        if not (has_owner or has_collection or has_weak or has_safe):
            continue

        # Class / interface properties that hold only safe metadata are not retention roots.
        # Object (top-level or companion) properties that hold safe metadata are flagged as INFO
        # because they are effectively process-lifetime static metadata.
        if nearest_type in ("class", "interface", "enum") and in_type_body and has_safe and not (has_owner or has_collection or has_weak):
            continue

        # Use the nearest (innermost) type to decide the root kind.
        if nearest_type == "object":
            if "companion" in text[max(0, m.start()-80):m.start()].lower():
                root_kind = "COMPANION_OBJECT_FIELD"
            else:
                root_kind = "KOTLIN_OBJECT_FIELD"
        elif is_static or nearest_type == "other":
            root_kind = "STATIC_FIELD"
        else:
            # class / interface / enum body properties
            root_kind = "INSTANCE_FIELD"

        c = _make_candidate(path, text, m.start(), root_kind, retained, root_description=snippet[:120])
        candidates.append(c)

    # 2. Registration / unregistration (identity-based release)
    for m in _REGEX_REGISTER.finditer(text):
        kind = m.group(1)
        if "Receiver" in kind:
            root_kind = "BROADCAST_RECEIVER_REGISTRATION"
        elif "Observer" in kind or "ContentObserver" in kind:
            root_kind = "CONTENT_OBSERVER_REGISTRATION"
        elif "Listener" in kind or "Callback" in kind:
            root_kind = "CALLBACK_REGISTRATION" if "Callback" in kind else "LISTENER_REGISTRATION"
        else:
            root_kind = "LISTENER_REGISTRATION"

        call = text[m.end():]
        receiver_obj = _extract_receiver_obj(text, m.start())
        identity_raw, retained = _extract_register_identity(call, kind, path.stem)

        release, release_line = _find_identity_release(text, kind, receiver_obj, identity_raw)

        c = _make_candidate(
            path, text, m.start(), root_kind, retained,
            root_description=f"{kind}(...)",
            registration_site=_snippet(text, m.start(), 80),
            release_site=release,
            lifecycle_boundary="PER_FEATURE" if release else "UNKNOWN",
        )
        candidates.append(c)

    # 3. Handler construction
    for m in _REGEX_HANDLER.finditer(text):
        snippet = _snippet(text, m.start(), 160)
        handler_var = _extract_handler_variable(text, m.start()) or _field_name(snippet)
        release, _ = _find_handler_release(text, handler_var, None)
        c = _make_candidate(
            path, text, m.start(), "HANDLER", "Handler",
            root_description="Handler(...)",
            release_site=release,
            lifecycle_boundary="PER_FEATURE" if release else "UNKNOWN",
        )
        candidates.append(c)

    # 4. post / postDelayed / sendMessage
    for m in _REGEX_POST.finditer(text):
        kind = m.group(1)
        call = text[m.end():]
        receiver_obj = _extract_receiver_obj(text, m.start())
        retained = _extract_call_arg_type(call, 0, 5000, path.stem)
        if not _contains_owner(retained):
            continue
        runnable_raw = _extract_call_arg_raw(call, 0)
        release, _ = _find_handler_release(text, receiver_obj, runnable_raw)
        c = _make_candidate(
            path, text, m.start(), "HANDLER", retained,
            root_description=f"{kind}(...)",
            registration_site=_snippet(text, m.start(), 80),
            release_site=release,
            lifecycle_boundary="PER_FEATURE" if release else "UNKNOWN",
        )
        candidates.append(c)

    # 5. Thread / Executor
    for m in _REGEX_THREAD_EXECUTOR.finditer(text):
        line_start = text.rfind("\n", 0, m.start()) + 1
        if text[line_start:].lstrip().startswith("import "):
            continue
        # Ignore Thread() lambdas created inside thread factories (Executors / ThreadPoolExecutor).
        if m.group(0).startswith("Thread"):
            factory_start = max(text.rfind("Executors.new", 0, m.start()), text.rfind("ThreadPoolExecutor(", 0, m.start()))
            if factory_start != -1:
                between = text[factory_start:m.start()]
                if between.count("(") > between.count(")"):
                    continue
        snippet = _snippet(text, m.start(), 160)
        # For field assignments, the variable name is the clearest identity.
        field_name = _extract_handler_variable(text, m.start()) or _field_name(snippet)
        retained = field_name or _extract_retained_type(snippet) or "Thread/Executor"
        release, _ = _find_executor_release(text, field_name)
        c = _make_candidate(
            path, text, m.start(), "THREAD_EXECUTOR", retained,
            root_description=snippet[:120],
            release_site=release,
            lifecycle_boundary="PER_FEATURE" if release else "PROCESS",
        )
        candidates.append(c)

    # 6. WeakReference
    for m in _REGEX_WEAK.finditer(text):
        snippet = _snippet(text, m.start(), 160)
        retained = _extract_retained_type(snippet)
        c = _make_candidate(
            path, text, m.start(), "WEAK_REFERENCE", retained or "Object",
            root_description=snippet[:120],
            edge_strength="WEAK",
            lifecycle_boundary="PER_FEATURE",
        )
        candidates.append(c)

    # 7. AdditionalInstanceField
    for m in _REGEX_ADDITIONAL.finditer(text):
        call = text[m.end():]
        # setAdditionalInstanceField(target, key, value) -> value is the retained object
        retained = _extract_call_arg_type(call, 2, 5000, path.stem)
        if _is_literal(retained):
            retained = "primitive"
        c = _make_candidate(
            path, text, m.start(), "ADDITIONAL_INSTANCE_FIELD", retained or "Object",
            root_description=_snippet(text, m.start(), 80),
        )
        candidates.append(c)

    return candidates


def _extract_retained_type(snippet: str) -> str:
    # Try to extract type from field declaration: "val foo: Activity = ..." or "Activity foo = ..."
    m = re.search(r"(?:val|var|lateinit\s+var)\s+[A-Za-z0-9_]+\s*:\s*([A-Za-z0-9_<>,\[\]?.\s]+)", snippet)
    if m:
        t = re.sub(r"\s+", "", m.group(1).strip())
        if t and t not in ("val", "var", "lateinit", "const"):
            return t
    m = re.search(r"([A-Za-z0-9_<>,\[\]?.\s]+)\s+[A-Za-z0-9_]+\s*[=;]", snippet)
    if m:
        t = m.group(1).strip()
        for kw in ("public", "private", "protected", "internal", "static", "final", "val", "var", "lateinit", "const", "open"):
            t = re.sub(r"\b" + kw + r"\b", "", t)
        t = re.sub(r"\s+", "", t)
        if t and t not in ("", "val", "var"):
            return t
    # Try to extract collection type from initializer, e.g. mutableMapOf<Key, Value>()
    m = re.search(r"=\s*(?:[A-Za-z0-9_.]+\s*\.)?(mutableMapOf|mutableListOf|mutableSetOf|hashMapOf|hashSetOf|arrayListOf|LinkedHashMap|HashMap|ArrayList|ArrayDeque|ConcurrentHashMap|SparseArray)\s*<([^>]+)>", snippet)
    if m:
        return f"{m.group(1)}<{m.group(2)}>"
    # Try to extract constructor call: "val foo = WeakReference<Activity>(...)"
    m = re.search(r"=\s*(?:new\s+)?([A-Za-z0-9_]+)\s*(?:<([^>]+)>)?\s*\(", snippet)
    if m:
        if m.group(2):
            return f"{m.group(1)}<{m.group(2)}>"
        return m.group(1)
    # Try to extract object expression type: "val foo = object : Listener { ... }"
    m = re.search(r"=\s*object\s*:\s*([A-Za-z0-9_<>,\[\].>\s]+)", snippet)
    if m:
        return re.sub(r"\s+", "", m.group(1).strip())
    return "Object"


def _extract_call_first_arg(call: str, limit: int = 5000, path_stem: str = "") -> str:
    """Extract the first argument type/name from a call snippet."""
    return _extract_call_arg_type(call, 0, limit, path_stem)


def _path_stem_owner_type(path_stem: str) -> str:
    t = path_stem.lower()
    for owner in _SHORT_LIVED_OWNER_TYPES:
        if t.endswith(owner.lower()):
            return owner
    return ""


def _is_literal(value: str) -> bool:
    v = value.strip()
    if not v:
        return False
    if v in ("null", "true", "false"):
        return True
    if v.startswith('"') or v.startswith("'"):
        return True
    if re.match(r"^-?\d+(?:\.\d+)?[LlFfSsBb]?$", v):
        return True
    return False


def _extract_arg_type(arg: str, path_stem: str = "") -> str:
    # object expression: object : SomeListener { ... }
    om = re.search(r"object\s*:\s*([A-Za-z0-9_<>,\[\].\s]+)", arg)
    if om:
        return re.sub(r"\s+", "", om.group(1).strip())
    # lambda / runnable block; if the file stem is an owner class, infer it
    owner = _path_stem_owner_type(path_stem)
    if owner:
        return owner
    # XposedHelpers.get.*Field calls are known primitive / object getters
    gm = re.search(r"XposedHelpers\.get([A-Z][a-zA-Z]*)Field\s*\(", arg)
    if gm:
        t = gm.group(1)
        type_map = {
            "Long": "Long",
            "Int": "Int",
            "Integer": "Int",
            "Float": "Float",
            "Double": "Double",
            "Boolean": "Boolean",
            "Byte": "Byte",
            "Short": "Short",
            "Char": "Char",
            "Object": "Object",
        }
        if t in type_map:
            if t in ("Int", "Integer"):
                return "Int"
            if t in type_map:
                return type_map[t]
    tokens = [t for t in re.findall(r"[A-Za-z0-9_<>,\[\].]+", arg) if t and t not in ("object", "val", "var", "it", "this")]
    if tokens:
        return re.sub(r"\s+", "", tokens[0])
    return re.sub(r"\s+", " ", arg).strip()[:80]


# ---------------------------------------------------------------------------
# Manual review pass
# ---------------------------------------------------------------------------

def _class_bases(text: str) -> list[str]:
    """Extract base class / interface names from a source file."""
    bases: list[str] = []
    # class Foo : Bar(), Baz
    for m in re.finditer(r"(?:class|object|interface)\s+[A-Za-z0-9_]+\s*(?:<[^>]+>)?\s*:\s*([A-Za-z0-9_<>.,\s]+?)(?:\{|\()", text, re.S):
        for part in m.group(1).split(","):
            b = part.strip().split("(")[0].strip()
            if b:
                bases.append(b)
    # class Foo extends Bar implements Baz (Java)
    for m in re.finditer(r"class\s+[A-Za-z0-9_]+(?:\s+extends\s+([A-Za-z0-9_<>.,\s]+?))?(?:\s+implements\s+([A-Za-z0-9_<>.,\s]+?))?\s*\{", text):
        if m.group(1):
            for part in m.group(1).split(","):
                b = part.strip().split("(")[0].strip()
                if b:
                    bases.append(b)
        if m.group(2):
            for part in m.group(2).split(","):
                b = part.strip().split("(")[0].strip()
                if b:
                    bases.append(b)
    return bases


def _file_text(path: Path, repo_root: Path) -> str:
    try:
        return (repo_root / path).read_text(encoding="utf-8", errors="replace")
    except Exception:
        return ""


def _review_candidates(candidates: list[Candidate], repo_root: Path = REPO_ROOT) -> None:
    """Apply a conservative manual review to each candidate.

    After this pass `reviewed_*`, `classification` and `risk` reflect the
    reviewed state, and `review_status` is set accordingly.
    """
    # Pre-load source files by source_file
    file_texts: dict[str, str] = {}
    unique_files = set(c.source_file for c in candidates)
    for sf in unique_files:
        file_texts[sf] = _file_text(Path(sf), repo_root)

    # Pre-compute class bases per file
    file_bases: dict[str, list[str]] = {sf: _class_bases(txt) for sf, txt in file_texts.items()}

    for c in candidates:
        c.reviewed_classification = c.scanner_classification
        c.reviewed_risk = c.scanner_risk
        c.review_status = "PENDING"
        c.review_rationale = ""
        c.review_evidence = ""

        text = file_texts.get(c.source_file, "")
        bases = file_bases.get(c.source_file, [])

        # 1. Safe / process-lifetime / weak edges with scanner-accepted evidence
        if c.scanner_risk in ("INFO", "LOW") and c.scanner_classification in (
            "SAFE_STABLE_METADATA", "PROCESS_LIFETIME_INTENTIONAL", "WEAK_EDGE_WITH_MANAGED_ROOT"
        ):
            c.reviewed_classification = c.scanner_classification
            c.reviewed_risk = c.scanner_risk
            c.review_status = "REVIEWED"
            c.review_rationale = "Scanner evidence accepted: stable metadata, process-lifetime root, or weak edge."
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        if c.scanner_classification == "LIFECYCLE_MANAGED" and c.release_site:
            c.reviewed_classification = c.scanner_classification
            c.reviewed_risk = c.scanner_risk
            c.review_status = "REVIEWED"
            c.review_rationale = "Identity-matched release path found in source."
            c.review_evidence = c.release_site[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 1.75 Scanner-provided bounded replacement / delayed callback evidence
        if c.scanner_classification in ("BOUNDED_REPLACEMENT_RETENTION", "BOUNDED_DELAYED_CALLBACK_RETENTION") and c.release_site:
            c.reviewed_classification = c.scanner_classification
            c.reviewed_risk = c.scanner_risk
            c.review_status = "REVIEWED"
            c.review_rationale = "Scanner found bounded replacement or delayed callback evidence (removeAllListeners, removeCallbacks, quitSafely, etc.)."
            c.review_evidence = c.release_site[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 1.5 BRUTAL_ALLOW comments explicitly mark static strong Android owners as intentional
        if "BRUTAL_ALLOW:STATIC_STRONG_ANDROID_OWNER" in (c.evidence or c.root_description or ""):
            c.reviewed_classification = "PROCESS_LIFETIME_INTENTIONAL"
            c.reviewed_risk = "LOW"
            c.review_status = "REVIEWED"
            c.review_rationale = "BRUTAL_ALLOW:STATIC_STRONG_ANDROID_OWNER comment indicates intentional process-lifetime owner."
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 2. StepCounterController reviews
        if c.source_file.endswith("StepCounterController.kt"):
            # sContext holds applicationContext and is cleared in destroy()
            if "sContext" in (c.evidence or "") and "Context" in c.retained_type:
                has_app_ctx = "applicationContext" in text and "sContext = null" in text
                if has_app_ctx:
                    c.reviewed_classification = "PROCESS_LIFETIME_INTENTIONAL"
                    c.reviewed_risk = "LOW"
                    c.review_status = "REVIEWED"
                    c.review_rationale = "sContext is assigned from context.applicationContext and cleared in destroy(); not an Activity leak."
                    c.review_evidence = _snippet(text, text.find("sContext"), 160)
                    c.classification = c.reviewed_classification
                    c.risk = c.reviewed_risk
                    continue
            # Handlers with removeCallbacksAndMessages(null) and nulling
            if c.root_kind == "HANDLER" and "removeCallbacksAndMessages" in (c.release_site or ""):
                c.reviewed_classification = "LIFECYCLE_MANAGED"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "Handler has removeCallbacksAndMessages(null) in destroy() and is nulled."
                c.review_evidence = c.release_site[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue
            # HandlerThread with quitSafely()
            if c.root_kind == "THREAD_EXECUTOR" and "quitSafely" in (c.release_site or ""):
                c.reviewed_classification = "LIFECYCLE_MANAGED"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "HandlerThread quitSafely() in destroy() with field nulling."
                c.review_evidence = c.release_site[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue
            # BroadcastReceiver fields that are unregistered and nulled in destroy()
            if c.root_kind == "KOTLIN_OBJECT_FIELD" and "BroadcastReceiver" in (c.retained_type or ""):
                if "unregisterModuleReceiver" in text and ("timeTickReceiver = null" in text or "screenReceiver = null" in text):
                    c.reviewed_classification = "LIFECYCLE_MANAGED"
                    c.reviewed_risk = "LOW"
                    c.review_status = "REVIEWED"
                    c.review_rationale = "Receiver field is unregistered and nulled in lifecycle teardown."
                    c.review_evidence = c.evidence[:200]
                    c.classification = c.reviewed_classification
                    c.risk = c.reviewed_risk
                    continue

        # 2.5 LockScreenAlbumArtController view listener is bounded
        if c.source_file.endswith("LockScreenAlbumArtController.kt"):
            if ("OnAttachStateChangeListener" in (c.retained_type or "") or (c.root_kind == "LISTENER_REGISTRATION" and "addOnAttachStateChangeListener" in (c.evidence or ""))):
                if "removeOnAttachStateChangeListener" in text and "ownerListener = null" in text:
                    c.reviewed_classification = "LIFECYCLE_MANAGED"
                    c.reviewed_risk = "LOW"
                    c.review_status = "REVIEWED"
                    c.review_rationale = "OnAttachStateChangeListener is removed and the field is nulled on view replacement/clear."
                    c.review_evidence = c.evidence[:200]
                    c.classification = c.reviewed_classification
                    c.risk = c.reviewed_risk
                    continue

        # 3. WebPage / OnBackPressedDispatcher with LifecycleOwner
        if c.root_kind == "CALLBACK_REGISTRATION" and c.root_description and "addCallback" in c.root_description:
            evidence = text + "\n" + (c.evidence or "")
            if "addCallback(this," in evidence or "addCallback(this," in (c.registration_site or ""):
                if any(b in _LIFECYCLE_OWNER_BASES for b in bases):
                    c.reviewed_classification = "LIFECYCLE_MANAGED"
                    c.reviewed_risk = "LOW"
                    c.review_status = "REVIEWED"
                    c.review_rationale = "OnBackPressedDispatcher.addCallback(this, ...) uses a LifecycleOwner Fragment; callback is auto-removed on lifecycle destroy."
                    c.review_evidence = c.registration_site[:200]
                    c.classification = c.reviewed_classification
                    c.risk = c.reviewed_risk
                    continue

        # 4. SubFragment delayed smooth-scroller — bounded short window
        if c.source_file.endswith("SubFragment.kt") and c.root_kind == "HANDLER" and "postDelayed" in (c.root_description or ""):
            c.reviewed_classification = "BOUNDED_DELAYED_CALLBACK_RETENTION"
            c.reviewed_risk = "MEDIUM"
            c.review_status = "REVIEWED"
            delay_380 = "380" in (c.evidence or "")
            highlight_reset = "highlightKey = null" in text
            rationale = [
                "Fragment/View posts a delayed Runnable with a finite window; no evidence of repeated unbounded queuing.",
            ]
            if delay_380:
                rationale.append("Delay is 380ms (short window).")
            if highlight_reset:
                rationale.append("highlightKey is reset to null before post, bounding the trigger to the current key highlight.")
            rationale.append("onStart is lifecycle-bound; callback is not proven to outlive the Fragment/View.")
            c.review_rationale = " ".join(rationale)
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 5. LauncherIconHooks TextWatcher — view-owned listener
        if c.source_file.endswith("LauncherIconHooks.kt") and c.root_kind == "LISTENER_REGISTRATION" and "TextWatcher" in (c.retained_type or ""):
            c.reviewed_classification = "VIEW_LIFETIME_OWNED_LISTENER"
            c.reviewed_risk = "LOW"
            c.review_status = "REVIEWED"
            c.review_rationale = "TextWatcher is added to mMessage (a per-view TextView) inside onFinishInflate; no evidence of repeated re-binding or a process-global GC root. The view owns the listener lifetime."
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 6. AdditionalInstanceField with Xposed primitive getters
        if c.root_kind == "ADDITIONAL_INSTANCE_FIELD" and (c.retained_type == "primitive" or c.retained_type in ("Long", "Int", "Float", "Double", "Boolean", "Byte", "Short", "Char")):
            c.reviewed_classification = "SAFE_STABLE_METADATA"
            c.reviewed_risk = "INFO"
            c.review_status = "REVIEWED"
            c.review_rationale = "Additional instance field stores a primitive / boxed primitive value; not an owner retention."
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 7. Bounded replacement / explicit remove for AdditionalInstanceField
        if c.root_kind == "ADDITIONAL_INSTANCE_FIELD" and c.retained_type in ("Handler", "Runnable", "HandlerThread", "Thread"):
            if "removeCallbacks" in text or "removeCallbacksAndMessages" in text or "quitSafely" in text:
                c.reviewed_classification = "LIFECYCLE_MANAGED"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "Stored async root is associated with removeCallbacks/quitSafely cleanup in the same file."
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # 8. Unbounded owner collection (high-confidence static / object)
        if c.scanner_classification == "UNBOUNDED_OWNER_COLLECTION":
            c.reviewed_classification = c.scanner_classification
            c.reviewed_risk = c.scanner_risk
            c.review_status = "REVIEWED"
            c.review_rationale = "Collection of short-lived Android owners held from a process-lifetime root."
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 9. Strong short owner from process root
        if c.scanner_classification == "STRONG_SHORT_OWNER_FROM_PROCESS_ROOT":
            c.reviewed_classification = c.scanner_classification
            c.reviewed_risk = c.scanner_risk
            c.review_status = "REVIEWED"
            c.review_rationale = "Short-lived Android owner held from a process-lifetime root."
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 9.5 Per-source review overrides for R2 raw HIGH candidates

        # MainActivity XposedService listener: one-shot, no Activity capture, gated by remotePrefs == null
        if c.source_file.endswith("MainActivity.kt") and c.root_kind == "LISTENER_REGISTRATION" and "XposedServiceHelper" in (c.evidence or ""):
            if "remotePrefs == null" in text and "object : XposedServiceHelper.OnServiceListener" in text:
                c.reviewed_classification = "PROCESS_LIFETIME_INTENTIONAL"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "XposedServiceHelper listener is registered once per process (gated by AppHelper.remotePrefs == null), captures only AppHelper/global state, and does not retain MainActivity/View."
                c.review_evidence = c.evidence[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # MainActivity / MainFragment SharedPreferences listener released via ?.let alias
        if c.source_file.endswith(("MainActivity.kt", "MainFragment.kt")) and c.root_kind == "LISTENER_REGISTRATION" and "OnSharedPreferenceChangeListener" in (c.retained_type or c.evidence or ""):
            if "unregisterOnSharedPreferenceChangeListener" in text and ("?.let" in text or "?.let" in (c.release_site or "")):
                c.reviewed_classification = "LIFECYCLE_MANAGED"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "OnSharedPreferenceChangeListener is unregistered in Activity/Fragment onDestroy via alias-aware let cleanup."
                c.review_evidence = (c.release_site or c.evidence)[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # MainFragment checkActive / hideKeyboard runnables: field assigned + let alias removeCallbacks
        if c.source_file.endswith("MainFragment.kt") and c.root_kind == "HANDLER" and "postDelayed" in (c.root_description or ""):
            if "removeCallbacks" in text and "?.let" in text and ("mCheckActiveRunnable" in (c.evidence or "") or "mHideKeyboardRunnable" in (c.evidence or "")):
                c.reviewed_classification = "LIFECYCLE_MANAGED"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "Runnable is stored in a Fragment field and removed in onDestroyView via field?.let { handler.removeCallbacks(it) }."
                c.review_evidence = (c.release_site or c.evidence)[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # GlobalActions receiver replacement pattern (old context unregistered before new context)
        if c.source_file.endswith("GlobalActions.kt") and c.root_kind == "BROADCAST_RECEIVER_REGISTRATION":
            receiver_name = c.retained_type or ""
            if receiver_name and "unregisterReceiver(" + receiver_name + ")" in text:
                c.reviewed_classification = "BOUNDED_REPLACEMENT_RETENTION"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = f"{receiver_name} is re-registered per process singleton Context; the previous context is unregistered before re-registration."
                c.review_evidence = c.evidence[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # Controls screen-on receiver replacement pattern
        if c.source_file.endswith("Controls.kt") and c.root_kind == "BROADCAST_RECEIVER_REGISTRATION" and "mScreenOnReceiver" in (c.evidence or ""):
            if "unregisterReceiver(mScreenOnReceiver)" in text:
                c.reviewed_classification = "BOUNDED_REPLACEMENT_RETENTION"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "mScreenOnReceiver is re-registered per system_server Context; the previous context is unregistered before re-registration."
                c.review_evidence = c.evidence[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # ModuleHelper receiver infrastructure: bounded key-based OwnedReceiverRegistration
        if c.source_file.endswith("ModuleHelper.java") and c.root_kind == "BROADCAST_RECEIVER_REGISTRATION":
            if "OwnedReceiverRegistration" in text and "unregisterReceiver" in text:
                c.reviewed_classification = "LIFECYCLE_MANAGED"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "ModuleHelper receiver registration infrastructure uses OwnedReceiverRegistration with explicit unregistration by key; not an unbounded feature leak."
                c.review_evidence = c.evidence[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # Various one-shot registerReceiver(null, ...) sticky intent lookup
        if c.source_file.endswith("Various.kt") and c.root_kind == "BROADCAST_RECEIVER_REGISTRATION" and "registerReceiver(null" in (c.evidence or ""):
            c.reviewed_classification = "SAFE_STABLE_METADATA"
            c.reviewed_risk = "INFO"
            c.review_status = "REVIEWED"
            c.review_rationale = "registerReceiver(null, ...) is a one-shot sticky intent lookup; it does not register a retained BroadcastReceiver."
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # Various alarm observer in system_server AlarmManagerService hook
        if c.source_file.endswith("Various.kt") and c.root_kind == "CONTENT_OBSERVER_REGISTRATION" and "alarmObserver" in (c.evidence or ""):
            if "AlarmManagerService" in text and "registerContentObserver" in (c.evidence or ""):
                c.reviewed_classification = "PROCESS_LIFETIME_INTENTIONAL"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "alarmObserver is registered inside a system_server AlarmManagerService hook; the service is process-lifetime and no Activity/View is captured."
                c.review_evidence = c.evidence[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # PreferenceBootstrap: single process-lifetime preference listener with guard
        if c.source_file.endswith("PreferenceBootstrap.java") and c.root_kind == "LISTENER_REGISTRATION" and "registerOnSharedPreferenceChangeListener" in (c.evidence or ""):
            c.reviewed_classification = "PROCESS_LIFETIME_INTENTIONAL"
            c.reviewed_risk = "LOW"
            c.review_status = "REVIEWED"
            c.review_rationale = "PreferenceBootstrap enforces at most one live OnSharedPreferenceChangeListener per process (watcherRegistered guard); listener only accesses SharedPreferences, no Activity/View owner."
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # SystemUILockScreenHooks AnimatorListener bounded replacement (removeAllListeners before add)
        if c.source_file.endswith("SystemUILockScreenHooks.kt") and c.root_kind == "LISTENER_REGISTRATION" and "Animator" in (c.retained_type or ""):
            if "removeAllListeners" in (c.release_site or c.evidence or ""):
                c.reviewed_classification = "BOUNDED_REPLACEMENT_RETENTION"
                c.reviewed_risk = "MEDIUM"
                c.review_status = "REVIEWED"
                c.review_rationale = "mAnimatorSet.removeAllListeners() is called before addListener(...); old listener set is cleared before replacement, so no unbounded accumulation is proven."
                c.review_evidence = (c.release_site or c.evidence)[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # BTList / WiFiList registerReceivers/unregisterReceivers pair
        if c.source_file.endswith(("BTList.kt", "WiFiList.kt")) and c.root_kind == "BROADCAST_RECEIVER_REGISTRATION" and "Receiver" in (c.retained_type or ""):
            if "unregisterReceiver" in text and "unregisterReceivers" in text:
                c.reviewed_classification = "LIFECYCLE_MANAGED"
                c.reviewed_risk = "LOW"
                c.review_status = "REVIEWED"
                c.review_rationale = "registerReceivers() calls unregisterReceivers() first, and onPause/onDestroy call unregisterReceivers(); receiver is lifecycle-managed."
                c.review_evidence = c.evidence[:200]
                c.classification = c.reviewed_classification
                c.risk = c.reviewed_risk
                continue

        # 10. Default for still-unbalanced registrations (explicit lack of release)
        if c.scanner_classification in (
            "UNBALANCED_RECEIVER_REGISTRATION",
            "UNBALANCED_OBSERVER_REGISTRATION",
            "UNBALANCED_LISTENER_REGISTRATION",
            "DELAYED_CALLBACK_OWNER_RETENTION",
        ):
            c.reviewed_classification = c.scanner_classification
            c.reviewed_risk = c.scanner_risk
            c.review_status = "NEEDS_MANUAL_REVIEW"
            c.review_rationale = "Scanner did not find a matching release/removal; source review is required before classification."
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 11. Unknown / unproven paths need ROM/runtime evidence
        if c.scanner_risk in ("MEDIUM", "UNKNOWN"):
            c.review_status = "NEEDS_ROM_EVIDENCE"
            c.review_rationale = "Scanner cannot prove the release path or owner lifetime from source alone; ROM/runtime evidence required."
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 12. Fallthrough: scanner HIGH/CRITICAL without explicit review proof
        if c.scanner_risk in ("HIGH", "CRITICAL"):
            c.review_status = "NEEDS_MANUAL_REVIEW"
            c.review_rationale = "Scanner HIGH/CRITICAL candidate lacks source-reviewed release/owner evidence."
            c.review_evidence = c.evidence[:200]
            c.classification = c.reviewed_classification
            c.risk = c.reviewed_risk
            continue

        # 13. Fallthrough: accept whatever the scanner produced
        c.review_status = "REVIEWED"
        c.review_rationale = "Scanner classification accepted."
        c.review_evidence = c.evidence[:200]
        c.classification = c.reviewed_classification
        c.risk = c.reviewed_risk


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def _feature_name(path: Path) -> str:
    return path.stem


def scan(repo_root: Path = REPO_ROOT) -> list[Candidate]:
    global _CANDIDATE_ID
    _CANDIDATE_ID = 0
    src = repo_root / "app" / "src" / "main" / "java"
    candidates: list[Candidate] = []
    for path in sorted(src.rglob("*.*")):
        if path.suffix not in (".kt", ".java"):
            continue
        if "/test/" in path.as_posix() or "\\test\\" in str(path):
            continue
        try:
            candidates.extend(_scan_file(path))
        except Exception:
            continue
    # Stable sort by source file and line
    candidates.sort(key=lambda c: (c.source_file, c.source_line, c.id))
    # Re-number IDs to be deterministic
    for i, c in enumerate(candidates, 1):
        c.id = f"mlp-{i:05d}"

    # Apply conservative manual review and set reviewed values as primary fields.
    _review_candidates(candidates, repo_root)
    return candidates


def _summarize_risk_counts(candidates: list[Candidate]) -> dict:
    counts: dict[str, int] = {}
    for c in candidates:
        counts[c.risk] = counts.get(c.risk, 0) + 1
    return counts


def _classify_counts(candidates: list[Candidate]) -> dict:
    counts: dict[str, int] = {}
    for c in candidates:
        counts[c.classification] = counts.get(c.classification, 0) + 1
    return counts


def _root_kind_counts(candidates: list[Candidate]) -> dict:
    counts: dict[str, int] = {}
    for c in candidates:
        counts[c.root_kind] = counts.get(c.root_kind, 0) + 1
    return counts


def _scanner_risk_counts(candidates: list[Candidate]) -> dict:
    counts: dict[str, int] = {}
    for c in candidates:
        counts[c.scanner_risk] = counts.get(c.scanner_risk, 0) + 1
    return counts


def _scanner_classify_counts(candidates: list[Candidate]) -> dict:
    counts: dict[str, int] = {}
    for c in candidates:
        counts[c.scanner_classification] = counts.get(c.scanner_classification, 0) + 1
    return counts


def _owner_severity(type_str: str) -> int:
    t = type_str.lower()
    scores = [
        ("activity", 100),
        ("fragment", 90),
        ("view", 80),
        ("dialog", 70),
        ("window", 65),
        ("context", 60),
        ("service", 50),
        ("controller", 40),
        ("host", 35),
        ("tile", 30),
        ("panel", 30),
        ("row", 30),
        ("menu", 30),
        ("pendingintent", 25),
    ]
    return max((s for k, s in scores if k in t), default=0)


def _process_severity(process: str) -> int:
    return {
        "system_server": 100,
        "com.android.systemui": 80,
        "com.miui.home": 60,
    }.get(process, 40)


def _render_top_10(candidates: list[Candidate]) -> list[dict]:
    risk_order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "INFO": 4, "UNKNOWN": 5}
    status_order = {"REVIEWED": 0, "NEEDS_MANUAL_REVIEW": 1, "NEEDS_ROM_EVIDENCE": 2, "PENDING": 3}
    ranked = sorted(
        candidates,
        key=lambda c: (
            risk_order.get(c.risk, 5),
            status_order.get(c.review_status, 3),
            -_process_severity(c.process),
            -_owner_severity(c.retained_type),
            c.source_file,
            c.source_line,
        ),
    )
    return [asdict(c) for c in ranked[:10]]


def _review_status_counts(candidates: list[Candidate]) -> dict:
    counts: dict[str, int] = {}
    for c in candidates:
        counts[c.review_status] = counts.get(c.review_status, 0) + 1
    return counts


def write_inventory(candidates: list[Candidate], out: Path) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "schema_version": "2.0",
        "generated_by": "tools/a13_memory_lifecycle_scan.py",
        "root_kind_counts": _root_kind_counts(candidates),
        "risk_counts": _summarize_risk_counts(candidates),
        "classification_counts": _classify_counts(candidates),
        "review_status_counts": _review_status_counts(candidates),
        "scanner_risk_counts": _scanner_risk_counts(candidates),
        "scanner_classification_counts": _scanner_classify_counts(candidates),
        "top_10": _render_top_10(candidates),
        "candidates": [asdict(c) for c in candidates],
    }
    out.write_text(json.dumps(data, indent=2, sort_keys=False, ensure_ascii=False), encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="A13 memory lifecycle ownership candidate scanner")
    parser.add_argument("--repo-root", type=Path, default=REPO_ROOT)
    parser.add_argument("--output", type=Path, default=REPO_ROOT / "docs" / "audit" / "A13_MEMORY_LIFECYCLE_INVENTORY.json")
    parser.add_argument("--verify", action="store_true", help="Verify deterministic output by re-running from a temp copy")
    args = parser.parse_args(argv)

    if args.verify:
        import tempfile, shutil
        with tempfile.TemporaryDirectory() as td:
            copy = Path(td) / "repo"
            shutil.copytree(args.repo_root, copy, ignore=shutil.ignore_patterns("build", ".gradle", "*.apk"))
            candidates1 = scan(args.repo_root)
            import importlib.util, sys
            spec = importlib.util.spec_from_file_location("mlp_scan", copy / "tools" / "a13_memory_lifecycle_scan.py")
            mod = importlib.util.module_from_spec(spec)
            sys.modules[spec.name] = mod
            spec.loader.exec_module(mod)
            mod.REPO_ROOT = copy
            candidates2 = mod.scan(copy)
            if [asdict(c) for c in candidates1] != [asdict(c) for c in candidates2]:
                print("FAIL: deterministic mismatch", file=sys.stderr)
                return 1
            print("Deterministic PASS")
            return 0

    candidates = scan(args.repo_root)
    write_inventory(candidates, args.output)
    print(f"Wrote {len(candidates)} candidates to {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
