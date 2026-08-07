#!/usr/bin/env python3
"""Static candidate scanner for Android owner retention / lifecycle topology.

This scanner only *discovers* candidates. It does not prove runtime memory leaks.
Output is deterministic and path-independent: all paths are relative to repo_root.
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
    classification: str = "UNKNOWN_REQUIRES_MANUAL_REVIEW"
    risk: str = "UNKNOWN"
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
    "Integer", "Boolean", "enum", "ClassLoader",
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

# ---------------------------------------------------------------------------
# Parsing helpers
# ---------------------------------------------------------------------------

@functools.lru_cache(maxsize=1024)
def _scope_state(text: str) -> dict[int, tuple[bool, bool, bool, bool]]:
    """Return line state: (in_fun, in_object, in_class, is_static_keyword)."""
    lines = text.splitlines()
    info: dict[int, tuple[bool, bool, bool, bool]] = {}
    stack: list[tuple[str, str]] = []
    pending: list[str] = []

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
            pending.append("type")
        elif fun_decl.match(line):
            pending.append("fun")

        for ch in line:
            if ch == "{":
                kind = pending.pop(0) if pending else "other"
                tk = _type_kind(line) if kind == "type" else "other"
                stack.append((kind, tk))
            elif ch == "}" and stack:
                stack.pop()

        in_fun = any(b == "fun" for b, _ in stack)
        in_object = any(t == "object" for _, t in stack)
        in_class = any(t in ("class", "interface", "enum", "enum class") for _, t in stack)
        is_static = "static" in line or "@JvmField" in line or "@JvmField" in prev
        info[i] = (in_fun, in_object, in_class, is_static)
    return info


def _is_in_static_scope(text: str, line: int) -> bool:
    info = _scope_state(text)
    in_fun, in_object, in_class, is_static = info.get(line, (False, False, False, False))
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
    t = type_str.lower()
    return any(o.lower() in t for o in _ASYNC_ROOT_TYPES)


def _contains_controller(type_str: str) -> bool:
    t = type_str.lower()
    return any(o.lower() in t for o in _CONTROLLER_TYPES)


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


def _find_balanced_release(text: str, register_name: str) -> tuple[str, int | None]:
    """Search for an unregister/remove matching a register in the same file.

    Returns (release_site_snippet, release_line) or ("", None).
    """
    mapping = {
        "registerReceiver": "unregisterReceiver",
        "registerContentObserver": "unregisterContentObserver",
        "addListener": "removeListener",
        "addCallback": "removeCallback",
        "addOn.*Listener": "removeOn.*Listener",
        "register[A-Za-z0-9_]*Listener": "unregister[A-Za-z0-9_]*Listener",
        "add[A-Za-z0-9_]*Listener": "remove[A-Za-z0-9_]*Listener",
    }
    for pat, unpat in mapping.items():
        if re.search(pat, register_name):
            regex = re.compile(r"\." + unpat.replace(".*", r"[A-Za-z0-9_]*") + r"\s*\(")
            for m in regex.finditer(text):
                line = text[:m.start()].count("\n") + 1
                return _snippet(text, m.start(), 80), line
    # Fallback: any remove/unregister call in the same file is weak evidence of release.
    for m in re.finditer(r"\.(?:remove|unregister)[A-Za-z0-9_]*\s*\(", text):
        line = text[:m.start()].count("\n") + 1
        return _snippet(text, m.start(), 80), line
    return "", None


def _find_delayed_release(text: str) -> tuple[str, int | None]:
    regex = re.compile(r"\.(?:removeCallbacks|removeMessages|removeCallbacksAndMessages)\s*\(")
    for m in regex.finditer(text):
        line = text[:m.start()].count("\n") + 1
        return _snippet(text, m.start(), 80), line
    return "", None


def _find_shutdown_release(text: str) -> tuple[str, int | None]:
    regex = re.compile(r"\.(?:shutdown|shutdownNow|interrupt|cancel)\s*\(")
    for m in regex.finditer(text):
        line = text[:m.start()].count("\n") + 1
        return _snippet(text, m.start(), 80), line
    return "", None


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
    r"(?:public|private|protected|internal|open|final|static|const|abstract|transient|volatile\s+)*"
    r"(?:"
    # Kotlin property: val/var/lateinit var name : Type = ...
    r"(?:val|var|lateinit\s+var)\s+[A-Za-z0-9_]+\s*(?::\s*([A-Za-z0-9_<>,\[\]?\s\.]+))?\s*(?:=.*)?"
    r"|"
    # Java field: Type name = ... or Type name;
    r"([A-Za-z0-9_<>,\[\]\.]+(?:<[^>]+>)?)\s+[A-Za-z0-9_]+\s*(?:=.*|;)"
    r")",
    re.M,
)

_REGEX_REGISTER = re.compile(
    r"\.(registerReceiver|registerContentObserver|addListener|registerListener|addCallback|registerCallback|"
    r"addOn[A-Za-z0-9_]*Listener|add[A-Za-z0-9_]*Listener|register[A-Za-z0-9_]*Listener)\s*\("
)

_REGEX_HANDLER = re.compile(r"\bHandler\s*\(")
_REGEX_POST = re.compile(r"\.(post|postDelayed|sendMessage|sendMessageDelayed|sendEmptyMessageDelayed)\s*\(")
_REGEX_THREAD_EXECUTOR = re.compile(r"\b(?:Thread|Executor|ExecutorService|ScheduledExecutorService|Timer)\s*(?:\(|\b[A-Za-z0-9_]+\s*=)")
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
    scope = _scope_state(text).get(line, (False, False, False, False))
    in_object = scope[1]
    is_static = scope[3]

    if root_kind in ("KOTLIN_OBJECT_FIELD", "COMPANION_OBJECT_FIELD", "STATIC_FIELD"):
        lifetime = "PROCESS_LIFETIME"
    else:
        lifetime = overrides.get("root_lifetime", "UNKNOWN")

    risk = overrides.pop("risk", "UNKNOWN")
    classification = overrides.pop("classification", "UNKNOWN_REQUIRES_MANUAL_REVIEW")

    # default classification / risk heuristics
    if root_kind in ("STATIC_FIELD", "KOTLIN_OBJECT_FIELD", "COMPANION_OBJECT_FIELD"):
        if _collection_type(retained_type):
            # If generic value type contains an Android owner, unbounded owner collection.
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
        classification = "LIFECYCLE_MANAGED" if release else "UNBALANCED_LISTENER_REGISTRATION"
        risk = "MEDIUM" if release else "HIGH"
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
            risk = "LOW" if release else "MEDIUM"
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
        classification=classification,
        risk=risk,
        confidence="low",
        **overrides,
    )


def _scan_file(path: Path) -> list[Candidate]:
    text = path.read_text(encoding="utf-8", errors="replace")
    candidates: list[Candidate] = []

    # 1. Field declarations (static, object, companion, instance with owner/collection)
    for m in _REGEX_FIELD.finditer(text):
        line = text[:m.start()].count("\n") + 1
        scope = _scope_state(text).get(line, (False, False, False, False))
        in_fun, in_object, in_class, is_static = scope
        if in_fun:
            continue  # local variable, not a field
        snippet = _snippet(text, m.start(), 160)
        retained = (m.group(1) or m.group(2) or "").strip() or _extract_retained_type(m.group(0))

        # Skip most const / literal primitive fields unless they are explicit reflection metadata
        if "const val" in snippet or "static final" in snippet or "static" in snippet and re.search(r"\b(?:String|int|long|boolean|float|double|byte|short|char)\b", snippet):
            if not _is_safe_metadata(retained) or not (in_object or is_static):
                continue

        has_collection = _collection_type(retained) is not None
        has_owner = _contains_owner(retained)
        has_weak = "WeakReference" in retained or "WeakReference" in snippet
        has_safe = _is_safe_metadata(retained)

        if not (has_owner or has_collection or has_weak or has_safe):
            continue

        # Instance fields with only safe metadata are usually lifecycle-managed but not retention roots.
        if not (in_object or is_static) and has_safe and not (has_owner or has_collection or has_weak):
            continue

        if in_object:
            root_kind = "KOTLIN_OBJECT_FIELD" if not is_static else "STATIC_FIELD"
        elif is_static:
            root_kind = "STATIC_FIELD"
        else:
            root_kind = "INSTANCE_FIELD"

        if "companion" in text[:m.start()][-200:].split("\n")[-1] or (in_object and "companion" in text[max(0, m.start()-500):m.start()].lower()):
            root_kind = "COMPANION_OBJECT_FIELD"

        c = _make_candidate(path, text, m.start(), root_kind, retained, root_description=snippet[:120])
        candidates.append(c)

    # 2. Registration / unregistration
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
        release, release_line = _find_balanced_release(text, kind)
        retained = _extract_call_first_arg(text[m.end():], 5000, path.stem)
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
        c = _make_candidate(
            path, text, m.start(), "HANDLER", "Handler",
            root_description="Handler(...)",
            release_site=_find_delayed_release(text)[0],
            lifecycle_boundary="PER_FEATURE" if _find_delayed_release(text)[0] else "UNKNOWN",
        )
        candidates.append(c)

    # 4. post / postDelayed / sendMessage
    for m in _REGEX_POST.finditer(text):
        call = text[m.end():]
        retained = _extract_call_first_arg(call, 5000, path.stem)
        if not _contains_owner(retained):
            continue
        release, _ = _find_delayed_release(text)
        c = _make_candidate(
            path, text, m.start(), "HANDLER", retained,
            root_description=f"{m.group(1)}(...)",
            registration_site=_snippet(text, m.start(), 80),
            release_site=release,
            lifecycle_boundary="PER_FEATURE" if release else "UNKNOWN",
        )
        candidates.append(c)

    # 5. Thread / Executor
    for m in _REGEX_THREAD_EXECUTOR.finditer(text):
        snippet = _snippet(text, m.start(), 160)
        retained = _extract_retained_type(snippet) or "Thread/Executor"
        release, _ = _find_shutdown_release(text)
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
        retained = _extract_call_arg_at(call, 2, 5000, path.stem)
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
    m = re.search(r"(?:val|var|lateinit\s+var)\s+[A-Za-z0-9_]+\s*:\s*([A-Za-z0-9_<>,\[\]?\s]+)", snippet)
    if m:
        t = re.sub(r"\s+", "", m.group(1).strip())
        if t and t not in ("val", "var", "lateinit", "const"):
            return t
    m = re.search(r"([A-Za-z0-9_<>,\[\]?\s]+)\s+[A-Za-z0-9_]+\s*[=;]", snippet)
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
    """Extract the first argument type/name from a call snippet.

    `call` is the text *inside* the argument list (after the opening '(').
    Handles object expressions and lambdas by tracking (), {}, [] brackets.
    """
    paren = 1  # we are already inside the call's argument list
    brace = 0
    bracket = 0
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
            if paren == 0:
                arg = call[:i]
                return _extract_arg_type(arg, path_stem)
        elif ch == "," and paren == 1 and brace == 0 and bracket == 0:
            arg = call[:i]
            return _extract_arg_type(arg, path_stem)
    return ""


def _path_stem_owner_type(path_stem: str) -> str:
    t = path_stem.lower()
    for owner in _SHORT_LIVED_OWNER_TYPES:
        if t.endswith(owner.lower()):
            return owner
    return ""


def _extract_call_arg_at(call: str, index: int, limit: int = 5000, path_stem: str = "") -> str:
    """Extract the Nth top-level argument from a call argument list.

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
                    return _extract_arg_type(call[start:i], path_stem)
                return ""
        elif ch == "," and paren == 1 and brace == 0 and bracket == 0:
            if arg_index == index:
                return _extract_arg_type(call[start:i], path_stem)
            arg_index += 1
            start = i + 1
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
    tokens = [t for t in re.findall(r"[A-Za-z0-9_<>,\[\].]+", arg) if t and t not in ("object", "val", "var", "it", "this")]
    if tokens:
        return re.sub(r"\s+", "", tokens[0])
    return re.sub(r"\s+", " ", arg).strip()[:80]


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


def _owner_severity(type_str: str) -> int:
    t = type_str.lower()
    # Higher score = more likely a short-lived owner leak.
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
    order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "INFO": 4, "UNKNOWN": 5}
    ranked = sorted(
        candidates,
        key=lambda c: (
            order.get(c.risk, 5),
            -_process_severity(c.process),
            -_owner_severity(c.retained_type),
            c.source_file,
            c.source_line,
        ),
    )
    return [asdict(c) for c in ranked[:10]]


def write_inventory(candidates: list[Candidate], out: Path) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "schema_version": "1.0",
        "generated_by": "tools/a13_memory_lifecycle_scan.py",
        "root_kind_counts": _root_kind_counts(candidates),
        "risk_counts": _summarize_risk_counts(candidates),
        "classification_counts": _classify_counts(candidates),
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
            # Make scanner use the temp copy
            candidates1 = scan(args.repo_root)
            # We must re-run from copy root, but the script has hardcoded REPO_ROOT. Use importlib.
            import importlib.util, sys
            spec = importlib.util.spec_from_file_location("mlp_scan", copy / "tools" / "a13_memory_lifecycle_scan.py")
            mod = importlib.util.module_from_spec(spec)
            sys.modules[spec.name] = mod
            spec.loader.exec_module(mod)
            # patch module global
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
