#!/usr/bin/env python3
"""Deterministic, static Hook cost scanner for CustoMIUIzer A13.

Uses only the Python standard library.  Does not run on a device and does not
modify source.  Produces a machine-readable JSON cost map plus an optional
Markdown summary.

The scanner is intentionally regex- and heuristic-based.  Fields that cannot be
determined statically are recorded as "unknown".  It does not claim to perform
full Kotlin/Java semantic analysis and does not introduce an AST parser.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SOURCE_ROOT = REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer"


# Hook and long-lifetime registration APIs we can recognize.
HOOK_CALL_RE = re.compile(
    r"\b(?:findAndHookMethod|findAndHookConstructor|hookAllMethods|hookAllConstructors|hookMethod|hookBefore|hookAfter)\s*\("
)

# Other long-lifetime registrations / scheduled callbacks.
REGISTRATION_RE = re.compile(
    r"\b(?:registerReceiver|registerContentObserver|registerSettingsObserver|"
    r"registerDisplayListener|addOnGlobalLayoutListener|addOnPreDrawListener|"
    r"addOnDrawListener|addOnTouchListener|addOnLayoutChangeListener|"
    r"postDelayed|postAtTime|scheduleAtFixedRate|schedule|executeOnExecutor|"
    r"GlobalScope\s*\.\s*launch|lifecycleScope\s*\.\s*launch|viewModelScope\s*\.\s*launch)\s*[\(\.]"
)


@dataclass
class SourceSpan:
    """A span of source text between two offsets, used for callback extraction."""
    start: int
    end: int


def _strip_line_comment(line: str) -> str:
    # Very rough: removes // comments but not inside strings.
    idx = line.find("//")
    if idx >= 0:
        return line[:idx]
    return line


def _is_in_string(text: str, offset: int) -> bool:
    """Return True if offset is inside a double-quoted string."""
    in_string = False
    escape = False
    for i, ch in enumerate(text):
        if i >= offset:
            break
        if ch == "\\" and not escape:
            escape = True
            continue
        escape = False
        if ch == '"':
            in_string = not in_string
    return in_string


def _is_in_comment(text: str, offset: int) -> bool:
    """Return True if offset is inside a Java/Kotlin line or block comment."""
    # Block comments.
    for m in re.finditer(r"/\*[\s\S]*?\*/", text):
        if m.start() < offset < m.end():
            return True
    # Line comments.
    for m in re.finditer(r"//[^\n]*", text):
        if m.start() < offset < m.end():
            return True
    return False


class ScopeTracker:
    """Track brace scopes with awareness of strings and comments."""

    def __init__(self, text: str) -> None:
        self.text = text
        self.length = len(text)

    def find_matching_close(self, open_offset: int) -> int | None:
        """Find the `}` that closes the `{` at open_offset."""
        depth = 0
        in_string = False
        escape = False
        in_line_comment = False
        i = open_offset
        while i < self.length:
            ch = self.text[i]
            if in_line_comment:
                if ch == "\n":
                    in_line_comment = False
                i += 1
                continue
            if not in_string and ch == "/" and i + 1 < self.length and self.text[i + 1] == "/":
                in_line_comment = True
                i += 2
                continue
            if not in_string and ch == "/" and i + 1 < self.length and self.text[i + 1] == "*":
                # Block comment. Find the end.
                end = self.text.find("*/", i + 2)
                if end == -1:
                    return None
                i = end + 2
                continue
            if ch == "\\" and not escape:
                escape = True
                i += 1
                continue
            escape = False
            if ch == '"':
                in_string = not in_string
            if not in_string:
                if ch == "{":
                    depth += 1
                elif ch == "}":
                    depth -= 1
                    if depth == 0:
                        return i
            i += 1
        return None

    def enclosing_block(self, offset: int) -> SourceSpan | None:
        """Return the span of the brace block that contains offset."""
        # Walk backwards to find the opening brace that started the block.
        depth = 0
        in_string = False
        escape = False
        for i in range(offset, -1, -1):
            ch = self.text[i]
            if ch == '"':
                in_string = not in_string
                continue
            if in_string:
                continue
            if ch == "}":
                depth += 1
            elif ch == "{":
                if depth == 0:
                    close = self.find_matching_close(i)
                    if close is None or close < offset:
                        return None
                    return SourceSpan(i, close)
                depth -= 1
        return None


def _scope_state(text: str) -> dict[int, tuple[bool, bool, bool, bool]]:
    """Re-implementation of the brace scope logic used by source_hazard_scan.py.

    Returns a map from 1-indexed line numbers to (in_fun, in_object, in_class, is_static_field).
    """
    lines = text.splitlines()
    line_count = len(lines)
    info: dict[int, tuple[bool, bool, bool, bool]] = {}

    type_decl_re = re.compile(
        r"^(?:\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
        r"protected\s+|internal\s+|final\s+))*"
        r"(class|object|companion\s+object|interface|enum\s+class|enum)\b"
    )
    fun_decl_re = re.compile(
        r"^(?:\s*(?:public|private|protected|internal|override|abstract|open|final|"
        r"inline|crossinline|noinline|operator|infix|suspend|tailrec|external|"
        r"expect|actual)\s+)*fun\b"
        r"|"
        r"(?:public|private|protected|static|final|abstract|synchronized|"
        r"native|strictfp|\s)+[A-Za-z0-9_<>,\[\].\s]+\s+[A-Za-z0-9_]+\s*\("
    )

    def declaration_kind(line: str) -> str | None:
        if type_decl_re.match(line):
            return "type"
        if fun_decl_re.match(line):
            return "fun"
        return None

    def detect_type_kind(line: str) -> str:
        m = re.match(
            r"\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
            r"protected\s+|internal\s+|final\s+)*"
            r"(class|object|companion\s+object|interface|enum\s+class|enum)\b",
            line,
        )
        if not m:
            return "other"
        kind = m.group(1)
        if kind == "companion object":
            return "object"
        if kind == "object":
            return "object"
        return "class"

    def has_static_keyword(line: str, prev_line: str) -> bool:
        if re.search(r"\bstatic\b", line):
            return True
        if "@JvmField" in line or "@JvmField" in prev_line:
            return True
        return False

    stack: list[tuple[str, str]] = []
    pending: list[str] = []
    for i, line in enumerate(lines, 1):
        prev_line = lines[i - 2] if i > 1 else ""
        kind = declaration_kind(line)
        if kind:
            pending.append(kind)

        for ch in line:
            if ch == "{":
                block_kind = pending.pop(0) if pending else "other"
                type_kind = detect_type_kind(line) if block_kind == "type" else "other"
                if type_kind == "companion":
                    type_kind = "object"
                stack.append((block_kind, type_kind))
            elif ch == "}":
                if stack:
                    stack.pop()

        in_fun = any(b == "fun" for b, _ in stack)
        in_object = any(t == "object" for _, t in stack)
        in_class = any(t == "class" for _, t in stack)
        is_static = has_static_keyword(line, prev_line)
        info[i] = (in_fun, in_object, in_class, is_static)

    return info


def _line_for_offset(text: str, offset: int) -> int:
    return text[:offset].count("\n") + 1


def _extract_first_top_level_type(text: str) -> str:
    """Return the first top-level class/object name in the file."""
    for line in text.splitlines():
        line = _strip_line_comment(line)
        m = re.match(
            r"\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
            r"protected\s+|internal\s+|final\s+)*(?:class|object|companion\s+object|interface|enum\s+class|enum)\s+"
            r"([A-Za-z0-9_]+)",
            line,
        )
        if m:
            return m.group(1)
    return "unknown"


def _extract_top_level_type(text: str, line_no: int) -> str:
    """Return the name of the top-level type that contains line_no."""
    lines = text.splitlines()
    scope = _scope_state(text)
    best_name = "unknown"
    for i in range(line_no, 0, -1):
        if i > len(lines):
            continue
        in_fun, in_object, in_class, _ = scope.get(i, (False, False, False, False))
        if in_object or in_class:
            m = re.match(
                r"\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
                r"protected\s+|internal\s+|final\s+)*(?:class|object|companion\s+object|interface|enum\s+class|enum)\s+"
                r"([A-Za-z0-9_]+)",
                lines[i - 1],
            )
            if m:
                return m.group(1)
    return best_name


def _extract_enclosing_function(text: str, line_no: int) -> str:
    """Return the name of the function/method that contains line_no."""
    lines = text.splitlines()
    scope = _scope_state(text)
    for i in range(line_no, 0, -1):
        in_fun, _, _, _ = scope.get(i, (False, False, False, False))
        if in_fun:
            line = _strip_line_comment(lines[i - 1])
            # Kotlin: fun name(...)
            m = re.search(r"\bfun\s+([A-Za-z0-9_]+(?:\s*\.\s*[A-Za-z0-9_]+)?)\s*\(", line)
            if m:
                return m.group(1)
            # Java: returnType name(...)
            m = re.search(r"\b(?:public|private|protected|static|final|synchronized|abstract|\s)+"
                          r"[A-Za-z0-9_<>,\[\].\s]+\s+([A-Za-z0-9_]+)\s*\(", line)
            if m:
                return m.group(1)
    return "unknown"


def _extract_callback_body(text: str, match_end: int) -> str | None:
    """Extract the body of the anonymous callback object/class following a hook call."""
    tracker = ScopeTracker(text)
    # Look for `new Type(` or `object : Type(` followed by `{`.
    # We search a small window after the match to find the first `{` that is not
    # inside a type argument or string.  This is heuristic.
    search = text[match_end:match_end + 600]
    # Find the first `{` that is not inside `()` (to skip the end of the call).
    # We track parentheses and ignore strings.
    in_string = False
    escape = False
    paren_depth = 0
    for i, ch in enumerate(search):
        if ch == "\\" and not escape:
            escape = True
            continue
        escape = False
        if ch == '"':
            in_string = not in_string
            continue
        if in_string:
            continue
        if ch == "(":
            paren_depth += 1
        elif ch == ")":
            paren_depth -= 1
        elif ch == "{" and paren_depth == 0:
            # Also ensure this `{` is part of an anonymous class/object, not a lambda block.
            # Heuristic: the text before the `{` should contain "new", ":", or a hook type name.
            prefix = search[max(0, i - 120):i]
            if re.search(r"\bnew\b|:\s*[A-Za-z0-9_]|MethodHook|AfterHookCallback|BeforeHookCallback", prefix):
                close = tracker.find_matching_close(match_end + i)
                if close is not None:
                    return text[match_end + i + 1:close]
            # If not, continue searching.
    return None


def _extract_call_arguments(text: str, match_start: int) -> str:
    """Return the full argument list text (including parentheses) for a call."""
    open_idx = text.find("(", match_start)
    if open_idx == -1:
        return ""
    depth = 0
    in_string = False
    escape = False
    for i in range(open_idx, len(text)):
        ch = text[i]
        if ch == "\\" and not escape:
            escape = True
            continue
        escape = False
        if ch == '"':
            in_string = not in_string
            continue
        if in_string:
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return text[open_idx:i + 1]
    return ""


def _extract_target_class_and_method(args: str) -> tuple[str, str]:
    """Best-effort extraction of target class and method from a hook call argument list."""
    # Remove parentheses.
    inner = args.strip()[1:-1].strip() if args.startswith("(") and args.endswith(")") else args
    if not inner:
        return "unknown", "unknown"
    parts = _split_top_level_commas(inner)
    if not parts:
        return "unknown", "unknown"
    # The first argument is the class: either a string literal or a class expression.
    first = parts[0].strip()
    if first.startswith('"') or first.startswith("'"):
        cls = _strip_quotes(first)
    else:
        cls = first
    # The method name must be a string literal; if it is a variable we cannot determine it.
    method = "unknown"
    for p in parts[1:]:
        p = p.strip()
        if p.startswith('"') or p.startswith("'"):
            stripped = _strip_quotes(p)
            if stripped and not stripped.startswith("new "):
                if re.match(r"^[A-Za-z0-9_<>$]+$", stripped):
                    method = stripped
                    break
    return cls, method


def _split_top_level_commas(text: str) -> list[str]:
    parts: list[str] = []
    current = ""
    depth = 0
    in_string = False
    escape = False
    for ch in text:
        if ch == "\\" and not escape:
            escape = True
            current += ch
            continue
        escape = False
        if ch == '"':
            in_string = not in_string
            current += ch
            continue
        if in_string:
            current += ch
            continue
        if ch in "(<":
            depth += 1
        elif ch in ")>":
            depth -= 1
        elif ch == "," and depth == 0:
            parts.append(current)
            current = ""
            continue
        current += ch
    if current or parts:
        parts.append(current)
    return parts


def _strip_quotes(s: str) -> str:
    s = s.strip()
    if (s.startswith('"') and s.endswith('"')) or (s.startswith("'") and s.endswith("'")):
        return s[1:-1]
    return s


def _detect_hook_type(snippet: str) -> str:
    lower = snippet.lower()
    if "findandhookconstructor" in lower:
        return "CONSTRUCTOR_HOOK"
    if "hookallconstructors" in lower:
        return "ALL_CONSTRUCTORS"
    if "hookallmethods" in lower:
        return "ALL_METHODS"
    if "findandhookmethod" in lower:
        return "METHOD_HOOK"
    if "hookmethod" in lower:
        return "METHOD_HOOK"
    if "hookbefore" in lower or "hookafter" in lower:
        return "METHOD_CALLBACK"
    if "registerreceiver" in lower:
        return "BROADCAST_RECEIVER"
    if "registercontentobserver" in lower:
        return "CONTENT_OBSERVER"
    if "registerdisplaylistener" in lower:
        return "DISPLAY_LISTENER"
    if "addongloballayoutlistener" in lower:
        return "VIEW_TREE_OBSERVER"
    if "postdelayed" in lower or "postattime" in lower:
        return "HANDLER_DELAYED"
    if "scheduleatfixedrate" in lower or re.search(r"\.schedule\s*\(", snippet):
        return "SCHEDULED_EXECUTOR"
    if "globalscope" in lower:
        return "COROUTINE_SCOPE"
    return "UNKNOWN"


def _callback_frequency_class(target_class: str, target_method: str, body: str | None) -> str:
    """Classify the callback frequency/phase into the fixed P1B-0 enum.

    Startup paths (Application/Activity/Service onCreate, attach, setup) must
    not be reported as hot paths.
    """
    tm = target_method.lower()
    tc = target_class.lower()

    # Touch, draw, layout and frame-driven hooks.
    frame_hot = ("ontouch", "dispatchtouchevent", "onintercepttouchevent", "ondraw", "ondispatchdraw",
                 "onmeasure", "onlayout", "onscrolled", "onscroll", "onfling", "oncompute",
                 "updatetime", "runtick", "ontick", "doinbackground", "dispatchdraw", "draw",
                 "onpredraw", "ongloballayout")
    for h in frame_hot:
        if h in tm:
            return "FRAME_OR_LAYOUT_HOT"

    # User interaction (not frame continuous, but directly user-driven).
    if any(h in tm for h in ("onclick", "onlongclick", "onitemclick", "onkey", "onback")):
        return "USER_INTERACTION"

    # Process / component startup.  Do not mark as hot.
    if any(h in tm for h in ("oncreate", "onattach", "onapplication", "attach", "onstart", "onresume",
                              "onpause", "onstop", "onrestart", "onconfigurationchanged",
                              "onviewattached", "onviewdetached", "afterhookedmethod", "beforehookedmethod",
                              "install", "setup", "init", "finish", "ondestroy", "once")):
        # Distinguish Application/Service/Provider onCreate (process startup) from Activity/Fragment.
        if "application" in tc or "systemui" in tc or "launcher" in tc or "service" in tc or "provider" in tc:
            return "PROCESS_STARTUP"
        return "COMPONENT_STARTUP"

    # Event-driven callbacks at medium/low rate.
    if any(h in tm for h in ("onreceive", "onchange", "onresult", "handlemessage", "onmessage",
                              "onbind", "onunbind", "onconnected", "ondisconnected")):
        return "EVENT_DRIVEN_MEDIUM"

    # Runnable.run and generic delayed callbacks depend on usage.
    if "run" in tm:
        if body and re.search(r"\b(for|while)\s*\(", body):
            return "EVENT_DRIVEN_HIGH"
        return "EVENT_DRIVEN_MEDIUM"

    return "UNKNOWN"


def _analyze_callback_body(body: str | None) -> dict[str, str]:
    result = {
        "preference_read_in_callback": "unknown",
        "reflection_lookup_in_callback": "unknown",
        "collection_allocation_in_callback": "unknown",
        "string_allocation_in_callback": "unknown",
        "regex_use_in_callback": "unknown",
        "logging_in_callback": "unknown",
        "binder_or_system_call_in_callback": "unknown",
        "listener_has_unregister_path": "unknown",
        "delayed_callback_has_cancel_path": "unknown",
    }
    if not body:
        return result

    # Preference reads inside a callback.  MainModule.mPrefs is a PrefMap in-memory
    # snapshot (IN_MEMORY_SNAPSHOT_READ or SHARED_PREFERENCES_API_READ); remote
    # SharedPreferences or Settings.* are DISK_OR_IPC_READ.
    if re.search(r"\bMainModule\.mPrefs\.|\bmPrefs\.|\.getBoolean\(|\.getString\(|\.getInt\(|\.getStringAsInt\(|\.getStringSet\(", body):
        result["preference_read_in_callback"] = "IN_MEMORY_SNAPSHOT_READ"
    elif re.search(r"getRemotePreferences|\.getSharedPreferences\(|remote\.(?:getBoolean|getString|getInt|getStringAsInt|getStringSet)\b", body):
        result["preference_read_in_callback"] = "DISK_OR_IPC_READ"
    elif re.search(r"Settings\.(?:System|Secure|Global)\.", body):
        result["preference_read_in_callback"] = "DISK_OR_IPC_READ"
    elif re.search(r"getBoolean\(|getString\(|getInt\(|getStringAsInt\(|getStringSet\(", body):
        # Unknown preference object; treat as SharedPreferences API.
        result["preference_read_in_callback"] = "SHARED_PREFERENCES_API_READ"
    else:
        result["preference_read_in_callback"] = "false"

    # Reflection in a callback is only CALLBACK_TIME_REFLECTION; `.class` is
    # cached class metadata; everything else is UNKNOWN.
    if re.search(r"findClass|getDeclaredMethod|getMethod|getDeclaredField|getField|getClassLoader|forName|\.javaClass\b|\.getClass\b", body):
        result["reflection_lookup_in_callback"] = "CALLBACK_TIME_REFLECTION"
    elif re.search(r"\b\w+\.class\b|Class\.forName", body):
        result["reflection_lookup_in_callback"] = "CACHED_METADATA_USE"
    else:
        result["reflection_lookup_in_callback"] = "false"

    result["collection_allocation_in_callback"] = "true" if re.search(
        r"\b(listOf|mapOf|setOf|arrayListOf|hashMapOf|hashSetOf|mutableListOf|mutableMapOf|mutableSetOf|"
        r"ArrayList|HashMap|HashSet|LinkedHashMap|ConcurrentHashMap)\s*\(", body
    ) else "false"
    result["string_allocation_in_callback"] = "true" if re.search(
        r"StringBuilder|StringBuffer|String\.format|\.toString\(\)|toString\(\)", body
    ) else "false"
    result["regex_use_in_callback"] = "true" if re.search(
        r"Regex\(|Pattern\.compile|\.toRegex\b|\.matches\s*\(", body
    ) else "false"
    result["logging_in_callback"] = "true" if re.search(
        r"XposedHelpers\.log|Log\.|\.printStackTrace|System\.out|System\.err", body
    ) else "false"
    result["binder_or_system_call_in_callback"] = "true" if re.search(
        r"getSystemService|getContentResolver|Settings\.(?:System|Secure|Global)|sendBroadcast|"
        r"startActivity|startService|bindService|query|acquire|release\(\)", body
    ) else "false"
    # For the callback itself, look for removeCallbacks/unregister in the same file, not just body.
    return result


def _find_unregister_or_cancel(text: str) -> bool:
    return bool(re.search(
        r"unregisterReceiver|unregisterContentObserver|removeCallbacks|removeMessages|"
        r"removeUpdates|unregisterListener|removeOnGlobalLayoutListener", text
    ))


def _build_feature_catalog_map(source_root: Path) -> dict[str, dict[str, Any]]:
    """Parse FeatureCatalog.kt for FeatureSpec definitions."""
    feature_catalog = source_root / "mods" / "catalog" / "FeatureCatalog.kt"
    mapping: dict[str, dict[str, Any]] = {}
    if not feature_catalog.exists():
        return mapping
    text = feature_catalog.read_text(encoding="utf-8", errors="replace")
    # Collect imported mod class names so we can identify the real installer call.
    mod_class_names: set[str] = set()
    for line in text.splitlines():
        m = re.match(r"\s*import\s+tv\.withaibuild\.customiuizer\.mods\.([A-Za-z0-9_]+)", line)
        if m:
            mod_class_names.add(m.group(1))

    for m in re.finditer(r"FeatureSpec\s*\(", text):
        start = m.start()
        args = _extract_call_arguments(text, start)
        if not args:
            continue
        inner = args[1:-1]
        feature_id = _extract_kv_string(inner, "id")
        if not feature_id:
            continue
        process_scope = _extract_kv(inner, "processScope")
        preference_keys = _extract_string_set(inner, "preferenceKeys")
        condition = _extract_condition_summary(inner)
        # Find the installer call, e.g. `PackagePermissions.hook(runtime.lpparam)`.
        installer_call = _extract_installer_call(inner, mod_class_names)
        class_name = "unknown"
        if installer_call:
            m2 = re.match(r"([A-Za-z0-9_]+)\.", installer_call)
            if m2:
                class_name = m2.group(1)
        mapping[class_name] = {
            "feature_id": feature_id,
            "process_scope": process_scope,
            "preference_keys": preference_keys,
            "condition": condition,
            "installer_call": installer_call,
        }
    return mapping


def _extract_kv(text: str, key: str) -> str:
    m = re.search(rf"\b{re.escape(key)}\s*=\s*([^,\n]+)", text)
    return m.group(1).strip() if m else "unknown"


def _extract_kv_string(text: str, key: str) -> str:
    val = _extract_kv(text, key)
    return _strip_quotes(val)


def _extract_string_set(text: str, key: str) -> list[str]:
    m = re.search(rf"\b{re.escape(key)}\s*=\s*setOf\s*\((.*?)\)", text, re.S)
    if not m:
        return []
    inner = m.group(1)
    return [_strip_quotes(p.strip()) for p in _split_top_level_commas(inner) if p.strip()]


def _extract_condition_summary(text: str) -> str:
    m = re.search(r"condition\s*=\s*\{\s*([^}]+)\}", text)
    if not m:
        return "unknown"
    body = m.group(1).strip()
    if body == "true":
        return "ALWAYS_TRUE"
    if "getBoolean" in body or "getInt" in body or "getString" in body:
        return "PREFERENCE_GATED"
    return "COMPLEX"


def _extract_installer_call(text: str, mod_class_names: set[str] | None = None) -> str:
    # Find the first method call inside the installer block whose receiver is a mod class.
    for m in re.finditer(r"\b([A-Za-z0-9_]+)\.[A-Za-z0-9_]+\s*\([^\)]*\)", text):
        call = m.group(0).strip()
        cls = m.group(1)
        if mod_class_names and cls in mod_class_names:
            return call
        # Fallback: receiver starts with uppercase and is not an internal helper.
        if mod_class_names is None and cls[0].isupper() and cls not in ("HookInstaller", "FeatureDispatcher", "InstallSummary", "CanaryContracts", "DiagnosticIds", "FeatureRuntime", "ClassLoader"):
            return call
    return ""


def _build_installer_class_map(source_root: Path) -> dict[str, list[str]]:
    """Map each mod class name to the installer(s) that reference it."""
    installers_dir = source_root / "installers"
    class_to_installers: dict[str, list[str]] = {}
    if not installers_dir.exists():
        return class_to_installers
    for path in sorted(installers_dir.rglob("*")):
        if path.suffix not in (".kt", ".java"):
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        installer_name = path.stem
        # Collect imported mod classes.
        imports: set[str] = set()
        for line in text.splitlines():
            m = re.match(r"\s*import\s+tv\.withaibuild\.customiuizer\.mods\.([A-Za-z0-9_]+)", line)
            if m:
                imports.add(m.group(1))
        # Find `ClassName.method(` invocations.
        for cls in imports:
            if re.search(rf"\b{re.escape(cls)}\.[A-Za-z0-9_]+\s*\(", text):
                class_to_installers.setdefault(cls, []).append(installer_name)
    return class_to_installers


def _build_installer_method_map(source_root: Path) -> dict[tuple[str, str], list[str]]:
    """Map (mod class, mod method) to the installer(s) that call it.

    This is more accurate than class-level mapping because a single mod class
    can be reused by multiple installers, and P1B-0 process attribution must be
    per method call, not per class.
    """
    installers_dir = source_root / "installers"
    method_map: dict[tuple[str, str], list[str]] = {}
    if not installers_dir.exists():
        return method_map
    scope_map = _build_process_scope_map()
    for path in sorted(installers_dir.rglob("*")):
        if path.suffix not in (".kt", ".java"):
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        installer_name = path.stem
        installer_process = scope_map.get(installer_name, "unknown")
        # Collect imported mod classes (simple name -> full simple name).
        imports: dict[str, str] = {}
        for line in text.splitlines():
            m = re.match(r"\s*import\s+tv\.withaibuild\.customiuizer\.mods\.([A-Za-z0-9_]+)", line)
            if m:
                imports[m.group(1)] = m.group(1)
        for simple, _ in imports.items():
            for m in re.finditer(rf"\b{re.escape(simple)}\.([A-Za-z0-9_]+)\s*\(", text):
                method = m.group(1)
                method_map.setdefault((simple, method), []).append(installer_process)
    return method_map


def _build_process_scope_map() -> dict[str, str]:
    """Map installer name to the primary ProcessScope it serves."""
    return {
        "SystemServerInstaller": "SYSTEM_SERVER",
        "SystemUiInstaller": "SYSTEM_UI",
        "LauncherInstaller": "LAUNCHER",
        "SettingsInstaller": "SETTINGS_MAIN",
        "SecurityCenterInstaller": "SECURITY_CENTER_MAIN",
        "PowerKeeperInstaller": "POWER_KEEPER",
        "PhoneInstaller": "PHONE",
        "MediaInstaller": "MEDIA",
        "PackageInstallerRouter": "PACKAGE_INSTALLER",
        "AndroidPackageInstaller": "ANDROID_PACKAGE",
        "GenericAppInstaller": "GENERIC_APP",
        "InputMethodInstaller": "INPUT_METHOD",
        "WallpaperInstaller": "WALLPAPER",
    }


def _build_process_scope_for_file(
    rel_path: str,
    class_name: str,
    first_top_level_type: str,
    registration_function: str,
    installer_map: dict[str, list[str]],
    installer_method_map: dict[tuple[str, str], list[str]],
    scope_map: dict[str, str],
    feature_catalog: dict[str, dict[str, Any]],
) -> str:
    # Method-level mapping is the most accurate. A single mod class may host
    # methods called from different installers (e.g. SystemNotificationMoreHooks
    # has methods called from both SystemUiInstaller and SettingsInstaller).
    for cn in (class_name, first_top_level_type):
        if not cn:
            continue
        methods = installer_method_map.get((cn, registration_function))
        if methods:
            scopes = sorted({m for m in methods})
            if len(scopes) == 1:
                return _normalize_scope(scopes[0])
            return "MULTI_PROCESS:" + ",".join(scopes)
    # Direct feature catalog hit (try both the extracted class and first top-level).
    for cn in (class_name, first_top_level_type):
        if cn in feature_catalog:
            return _normalize_scope(feature_catalog[cn]["process_scope"])
    # Manager app UI files.
    base_name = Path(rel_path).stem
    if base_name in ("MainActivity", "MainApplication", "MainFragment", "SubFragment", "ActivitySelector", "AppSelector", "BTList", "WiFiList", "AudioVisualizer", "BatteryIndicator") or class_name in ("MainActivity", "MainApplication", "MainFragment", "SubFragment", "ActivitySelector", "AppSelector", "BTList", "WiFiList", "AudioVisualizer", "BatteryIndicator"):
        return "MANAGER_APP"
    # Installer map (try both class names).
    for cn in (class_name, first_top_level_type):
        installers = installer_map.get(cn, [])
        if installers:
            scopes = sorted({scope_map.get(i, "unknown") for i in installers})
            if len(scopes) == 1:
                return _normalize_scope(scopes[0])
            return "MULTI_PROCESS:" + ",".join(scopes)
    # If file is in installers, derive from installer name.
    if "installers/" in rel_path:
        base = Path(rel_path).stem
        return _normalize_scope(scope_map.get(base, "unknown"))
    # mods/utils and catalog are shared/utilities, not primary hook sites.
    if "mods/utils/" in rel_path or "mods/catalog/" in rel_path or "mods/diagnostics/" in rel_path:
        return "SHARED"
    return "unknown"


def _normalize_scope(raw: str) -> str:
    raw = raw.strip()
    if raw.startswith("ProcessScope."):
        return raw.split(".", 1)[1]
    return raw


def _default_enabled_state(feature_id: str, condition: str) -> str:
    # We cannot determine default enabled state statically in most cases.
    if condition == "ALWAYS_TRUE":
        return "always_enabled"
    if condition == "PREFERENCE_GATED":
        return "preference_dependent"
    return "unknown"


def _registered_when_feature_disabled(condition: str) -> str:
    if condition == "ALWAYS_TRUE":
        return "true"
    if condition == "PREFERENCE_GATED":
        return "false"
    return "unknown"


def _gate_locations(process_scope: str) -> tuple[str, str, str]:
    base = process_scope.split(":", 1)[0]
    if base == "SYSTEM_SERVER":
        return "MainModule.onSystemServerStarting", "MainModule.onSystemServerStarting", "ProcessScopes.resolve"
    if base == "MANAGER_APP":
        return "Application.onCreate", "Application.onCreate", "package_name"
    if base == "SYSTEM_UI":
        return "FeatureSpec.condition / SystemUiInstaller.install", "MainModule.onPackageReady", "SystemUiInstaller.pkg.equals"
    if base in ("LAUNCHER", "SETTINGS_MAIN", "SECURITY_CENTER_MAIN", "POWER_KEEPER", "PHONE", "MEDIA", "PACKAGE_INSTALLER", "INPUT_METHOD", "WALLPAPER", "ANDROID_PACKAGE", "GENERIC_APP"):
        return "FeatureSpec.condition / installer", "MainModule.onPackageReady", "ProcessScopes.resolve"
    if base == "MULTI_PROCESS":
        return "FeatureSpec.condition / multiple installers", "MainModule.onPackageReady", "ProcessScopes.resolve"
    return "unknown", "unknown", "unknown"


def _package_scope_for_process(process_scope: str) -> str:
    mapping = {
        "SYSTEM_SERVER": "android",
        "SYSTEM_UI": "com.android.systemui",
        "LAUNCHER": "com.miui.home, com.mi.android.globallauncher",
        "SETTINGS_MAIN": "com.android.settings",
        "SECURITY_CENTER_MAIN": "com.miui.securitycenter",
        "POWER_KEEPER": "com.miui.powerkeeper",
        "PHONE": "com.android.incallui",
        "MEDIA": "com.miui.screenshot, com.miui.gallery",
        "PACKAGE_INSTALLER": "com.miui.packageinstaller",
        "INPUT_METHOD": "input method packages",
        "WALLPAPER": "com.miui.miwallpaper",
        "ANDROID_PACKAGE": "android",
        "MANAGER_APP": "tv.withaibuild.customiuizer.r13",
    }
    base = process_scope.split(":", 1)[0]
    return mapping.get(base, "unknown")


def _registration_phase(process_scope: str) -> str:
    base = process_scope.split(":", 1)[0]
    if base == "SYSTEM_SERVER":
        return "SYSTEM_SERVER_STARTING"
    return "PACKAGE_READY"


@dataclass
class HookCostRecord:
    hook_id: str
    feature_id: str
    feature_name: str
    source_file: str
    source_line: int
    registration_function: str
    target_class: str
    target_method: str
    hook_type: str
    target_process: str
    target_package_scope: str
    registration_phase: str
    preference_key: str
    default_enabled_state: str
    feature_gate_location: str
    process_gate_location: str
    package_gate_location: str
    registered_when_feature_disabled: str
    feature_class_loaded_before_gate: str
    callback_frequency_class: str
    preference_read_in_callback: str
    reflection_lookup_in_callback: str
    collection_allocation_in_callback: str
    string_allocation_in_callback: str
    regex_use_in_callback: str
    logging_in_callback: str
    binder_or_system_call_in_callback: str
    retained_android_owner: str
    listener_has_unregister_path: str
    delayed_callback_has_cancel_path: str
    duplicate_target_group: str
    system_mechanism_alternative: str
    static_priority_score: int
    confidence: str
    notes: str

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


def _compute_score(fields: dict[str, Any]) -> int:
    score = 0
    if fields["target_process"] in ("SYSTEM_UI", "SYSTEM_SERVER", "LAUNCHER", "PHONE"):
        score += 2
    if fields["callback_frequency_class"] in ("FRAME_OR_LAYOUT_HOT", "EVENT_DRIVEN_HIGH"):
        score += 4
    if fields["callback_frequency_class"] in ("EVENT_DRIVEN_MEDIUM", "USER_INTERACTION"):
        score += 2
    if fields["callback_frequency_class"] in ("COMPONENT_STARTUP", "PROCESS_STARTUP"):
        score += 1
    if fields["preference_read_in_callback"] == "DISK_OR_IPC_READ":
        score += 4
    if fields["preference_read_in_callback"] == "SHARED_PREFERENCES_API_READ":
        score += 3
    if fields["preference_read_in_callback"] == "IN_MEMORY_SNAPSHOT_READ":
        score += 1
    if fields["reflection_lookup_in_callback"] == "CALLBACK_TIME_REFLECTION":
        score += 3
    if fields["reflection_lookup_in_callback"] == "CACHED_METADATA_USE":
        score += 1
    if fields["collection_allocation_in_callback"] == "true":
        score += 2
    if fields["binder_or_system_call_in_callback"] == "true":
        score += 2
    if fields["registered_when_feature_disabled"] == "true":
        score += 4
    if fields["listener_has_unregister_path"] == "false":
        score += 2
    if fields["delayed_callback_has_cancel_path"] == "false":
        score += 2
    if fields["duplicate_target_group"] != "":
        score += 1
    if fields["feature_class_loaded_before_gate"] == "true":
        score += 3
    return score


def _confidence_for_record(fields: dict[str, Any]) -> str:
    unknowns = sum(1 for v in fields.values() if v == "unknown")
    total = len(fields)
    ratio = unknowns / total if total else 0
    if ratio < 0.3:
        return "high"
    if ratio < 0.6:
        return "medium"
    return "low"


class HookCostScanner:
    def __init__(self, source_root: Path) -> None:
        self.source_root = source_root
        self.feature_catalog = _build_feature_catalog_map(source_root)
        self.installer_map = _build_installer_class_map(source_root)
        self.installer_method_map = _build_installer_method_map(source_root)
        self.scope_map = _build_process_scope_map()

    def scan(self) -> list[HookCostRecord]:
        records: list[HookCostRecord] = []
        paths = sorted(self.source_root.rglob("*"))
        for path in paths:
            if path.suffix not in (".kt", ".java"):
                continue
            rel = path.relative_to(self.source_root).as_posix()
            text = path.read_text(encoding="utf-8", errors="replace")
            records.extend(self._scan_file(path, rel, text))

        # Deduplicate target class/method groups.
        target_groups: dict[tuple[str, str], list[HookCostRecord]] = {}
        for r in records:
            key = (r.target_class, r.target_method)
            target_groups.setdefault(key, []).append(r)
        for r in records:
            key = (r.target_class, r.target_method)
            if len(target_groups[key]) > 1:
                r.duplicate_target_group = f"{len(target_groups[key])} sites: " + ", ".join(
                    f"{rec.source_file}:{rec.source_line}" for rec in target_groups[key]
                )

        # After all records, check file-level unregister/cancel for listener-like registrations.
        for r in records:
            if r.hook_type in ("BROADCAST_RECEIVER", "CONTENT_OBSERVER", "DISPLAY_LISTENER", "VIEW_TREE_OBSERVER", "HANDLER_DELAYED"):
                file_path = self.source_root / r.source_file
                if file_path.exists():
                    whole = file_path.read_text(encoding="utf-8", errors="replace")
                    r.listener_has_unregister_path = "true" if _find_unregister_or_cancel(whole) else "false"
                    r.delayed_callback_has_cancel_path = "true" if "removeCallbacks" in whole else "false"

        return records

    def _scan_file(self, path: Path, rel: str, text: str) -> list[HookCostRecord]:
        records: list[HookCostRecord] = []
        for pattern, is_hook in [(HOOK_CALL_RE, True), (REGISTRATION_RE, False)]:
            for m in pattern.finditer(text):
                if _is_in_string(text, m.start()) or _is_in_comment(text, m.start()):
                    continue
                line_no = _line_for_offset(text, m.start())
                feature_name = _extract_top_level_type(text, line_no)
                first_top_level_type = _extract_first_top_level_type(text)
                source_file = f"app/src/main/java/tv/withaibuild/customiuizer/{rel}"
                snippet = _strip_line_comment(text.splitlines()[line_no - 1]).strip()
                args = _extract_call_arguments(text, m.start())
                target_class, target_method = _extract_target_class_and_method(args)
                if not is_hook:
                    target_method = "register/callback"
                hook_type = _detect_hook_type(snippet)
                registration_function = _extract_enclosing_function(text, line_no)
                process_scope = _build_process_scope_for_file(rel, feature_name, first_top_level_type, registration_function, self.installer_map, self.installer_method_map, self.scope_map, self.feature_catalog)
                package_scope = _package_scope_for_process(process_scope)

                fcat = self.feature_catalog.get(feature_name) or self.feature_catalog.get(first_top_level_type) or {}
                feature_id = fcat.get("feature_id", first_top_level_type if first_top_level_type != "unknown" else feature_name)
                condition = fcat.get("condition", "unknown")
                preference_keys = fcat.get("preference_keys", [])
                default_state = _default_enabled_state(feature_id, condition)
                registered_when_disabled = _registered_when_feature_disabled(condition)
                fgate, pgate, pkg_gate = _gate_locations(process_scope)

                # Feature class loaded before gate: best-effort heuristic.
                # If the feature catalog condition is ALWAYS_TRUE or the file is referenced
                # unconditionally in an installer, the class is loaded before feature gate.
                class_loaded_before = "unknown"
                if condition == "ALWAYS_TRUE":
                    class_loaded_before = "true"
                elif condition == "PREFERENCE_GATED":
                    class_loaded_before = "false"

                body = _extract_callback_body(text, m.end())
                cb = _analyze_callback_body(body)
                freq = _callback_frequency_class(target_class, target_method, body)

                retained = "unknown"
                if re.search(r"(?:Activity|View|Fragment|Window|Context)\s+[A-Za-z0-9_]+\s*=?\s*(?!null)", body or ""):
                    retained = "true"
                else:
                    retained = "false"

                fields: dict[str, Any] = {
                    "target_process": process_scope,
                    "callback_frequency_class": freq,
                    "preference_read_in_callback": cb["preference_read_in_callback"],
                    "reflection_lookup_in_callback": cb["reflection_lookup_in_callback"],
                    "collection_allocation_in_callback": cb["collection_allocation_in_callback"],
                    "binder_or_system_call_in_callback": cb["binder_or_system_call_in_callback"],
                    "registered_when_feature_disabled": registered_when_disabled,
                    "listener_has_unregister_path": cb["listener_has_unregister_path"],
                    "delayed_callback_has_cancel_path": cb["delayed_callback_has_cancel_path"],
                    "duplicate_target_group": "",
                    "feature_class_loaded_before_gate": class_loaded_before,
                }
                score = _compute_score(fields)

                record = HookCostRecord(
                    hook_id=hashlib.sha256(f"{source_file}:{line_no}:{snippet}".encode()).hexdigest()[:24],
                    feature_id=feature_id,
                    feature_name=feature_name,
                    source_file=source_file,
                    source_line=line_no,
                    registration_function=registration_function,
                    target_class=target_class,
                    target_method=target_method,
                    hook_type=hook_type,
                    target_process=process_scope,
                    target_package_scope=package_scope,
                    registration_phase=_registration_phase(process_scope),
                    preference_key=preference_keys[0] if preference_keys else "unknown",
                    default_enabled_state=default_state,
                    feature_gate_location=fgate,
                    process_gate_location=pgate,
                    package_gate_location=pkg_gate,
                    registered_when_feature_disabled=registered_when_disabled,
                    feature_class_loaded_before_gate=class_loaded_before,
                    callback_frequency_class=freq,
                    preference_read_in_callback=cb["preference_read_in_callback"],
                    reflection_lookup_in_callback=cb["reflection_lookup_in_callback"],
                    collection_allocation_in_callback=cb["collection_allocation_in_callback"],
                    string_allocation_in_callback=cb["string_allocation_in_callback"],
                    regex_use_in_callback=cb["regex_use_in_callback"],
                    logging_in_callback=cb["logging_in_callback"],
                    binder_or_system_call_in_callback=cb["binder_or_system_call_in_callback"],
                    retained_android_owner=retained,
                    listener_has_unregister_path=cb["listener_has_unregister_path"],
                    delayed_callback_has_cancel_path=cb["delayed_callback_has_cancel_path"],
                    duplicate_target_group="",
                    system_mechanism_alternative="unknown",
                    static_priority_score=score,
                    confidence="unknown",
                    notes=f"args={args[:120]}",
                )
                record.confidence = _confidence_for_record(record.as_dict())
                records.append(record)
        return records


def _to_jsonable(records: list[HookCostRecord]) -> list[dict[str, Any]]:
    return [r.as_dict() for r in records]


def _regression_checks(source_root: Path) -> list[dict[str, Any]]:
    """Static regression checks for P1B-0 zero-feature cost reductions."""
    findings: list[dict[str, Any]] = []
    installer = source_root / "installers" / "AndroidPackageInstaller.java"
    if installer.exists():
        text = installer.read_text(encoding="utf-8", errors="replace")
        if "isAnyFeatureEnabled(MainModule.mPrefs)" in text:
            findings.append({
                "id": "EARLY_FEATURE_GATE_ANDROID",
                "source": "installers/AndroidPackageInstaller.java",
                "status": "pass",
                "message": "isAnyFeatureEnabled gate present before FeatureRuntime/FeatureDispatcher",
            })
        else:
            findings.append({
                "id": "EARLY_FEATURE_GATE_ANDROID",
                "source": "installers/AndroidPackageInstaller.java",
                "status": "fail",
                "message": "missing isAnyFeatureEnabled early gate",
            })
        if re.search(r"if\s*\(\s*listenerNeeded\s*\)\s*watchPreferences\.run\(\)", text):
            findings.append({
                "id": "GUARDED_WATCH_PREFERENCES_ANDROID",
                "source": "installers/AndroidPackageInstaller.java",
                "status": "pass",
                "message": "watchPreferences.run() is gated by listenerNeeded",
            })
        elif "watchPreferences.run()" in text:
            findings.append({
                "id": "GUARDED_WATCH_PREFERENCES_ANDROID",
                "source": "installers/AndroidPackageInstaller.java",
                "status": "fail",
                "message": "watchPreferences is still called unconditionally",
            })
        if "FeatureRuntime androidRuntime = null" in text or re.search(r"if\s*\([^)]*system_cleanshare[^)]*system_cleanopenwith[^)]*\).*\{[^}]*FeatureRuntime", text, re.S):
            findings.append({
                "id": "LAZY_FEATURE_RUNTIME_ANDROID",
                "source": "installers/AndroidPackageInstaller.java",
                "status": "pass",
                "message": "FeatureRuntime is created only when a catalog feature may be enabled",
            })
    return findings


def _write_cost_map(records: list[HookCostRecord], output: Path, source_root: Path | None = None) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "schema_version": 1,
        "scanner": "a13_hook_cost_scan.py",
        "generated_by": "static-analysis",
        "total_records": len(records),
        "records": _to_jsonable(records),
    }
    if source_root is not None:
        data["regression_findings"] = _regression_checks(source_root)
    output.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="A13 hook cost scanner")
    parser.add_argument("--source-root", type=Path, default=DEFAULT_SOURCE_ROOT, help="source root to scan")
    parser.add_argument("--output", type=Path, default=REPO_ROOT / "docs" / "audit" / "A13_HOOK_COST_MAP.json", help="output JSON path")
    parser.add_argument("--verify-stability", action="store_true", help="run twice and assert identical output")
    args = parser.parse_args(argv)

    scanner = HookCostScanner(args.source_root)
    records = scanner.scan()
    _write_cost_map(records, args.output, source_root=args.source_root)
    print(f"Wrote {len(records)} hook cost records to {args.output}")

    if args.verify_stability:
        scanner2 = HookCostScanner(args.source_root)
        records2 = scanner2.scan()
        if _to_jsonable(records) != _to_jsonable(records2):
            print("Stability check failed: two runs produced different output", file=sys.stderr)
            return 1
        print("Stability check passed")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
