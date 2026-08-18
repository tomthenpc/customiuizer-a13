#!/usr/bin/env python3
"""Phase F-R2 final-truth helpers: product taxonomy, owner proofs, hold purity."""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from difflib import SequenceMatcher, unified_diff
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
APP_NS = "{http://schemas.android.com/apk/res-auto}"

PRODUCT_NODE_TYPES = frozenset({"PRODUCT_ACTION", "PRODUCT_SUBOPTION"})
NON_PRODUCT_NODE_TYPES = frozenset({
    "NAVIGATION",
    "CATEGORY",
    "DEPENDENCY_HELPER",
    "INTERNAL_STATE",
    "HIDDEN_HELPER",
    "DYNAMIC_ISLAND_HELPER",
    "UNKNOWN",
})
PRESENT_STATES = frozenset({"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"})
DI_PRODUCT_KEY = "dynamic_island"
DI_HELPER_KEYS = frozenset({
    "system_strong_toast_island_offset",
    "system_strong_toast_position",
    "system_strong_toast_bottom_offset",
})

PREF_GET_RE = re.compile(
    r'(?:mPrefs|prefs|MainModule\.mPrefs|AppHelper)\.get(?:Boolean|Int|Long|Float|String|StringAsInt)\("([a-z0-9_]+)"'
)
QUOTED_KEY_RE = re.compile(r'"(?:pref_key_)?([a-z0-9_]{3,})"')
HOOK_STRING_RE = re.compile(
    r'(?:findAndHookMethod(?:Silently)?|hookAllMethods|findAndHookConstructor)\(\s*"([^"]+)"\s*,'
    r'(?:\s*[^,]+,)?\s*"([^"]+)"',
)
HOOK_METHOD_ONLY_RE = re.compile(
    r'(?:findAndHookMethod(?:Silently)?|hookAllMethods)\(\s*[A-Za-z_][\w.]*\s*,\s*"([^"]+)"'
)
HOOK_ALL_CTOR_RE = re.compile(r'hookAllConstructors\(\s*"([^"]+)"')
CALLBACK_RE = re.compile(r'override fun (before|after)\s*\(')
JAVA_CALLBACK_RE = re.compile(r'(beforeHookedMethod|afterHookedMethod|before|after)\s*\(')
INSTALL_CALLEE_RE = re.compile(r'([A-Za-z][A-Za-z0-9_]*(?:Hook|Hooks)?)\.([A-Za-z][A-Za-z0-9_]+)\s*\(')
KT_FUN_RE = re.compile(
    r'(?:^|\n)[ \t]*(?:(?:public|private|internal|protected|open|override|suspend|inline|actual|expect)\s+)*'
    r'fun\s+([A-Za-z_][A-Za-z0-9_]*)\s*[\(<]',
)
JAVA_FUN_RE = re.compile(
    r'(?:^|\n)[ \t]*(?:(?:public|private|protected|static|final|synchronized|native)\s+)+'
    r'[\w.<>,\[\]?]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\([^;{}]*\)\s*\{',
)
COMMENT_BLOCK_RE = re.compile(r'/\*.*?\*/', re.S)
COMMENT_LINE_RE = re.compile(r'//.*?$', re.M)
FATAL_WRAPPER_RE = re.compile(
    r'FatalErrors\.rethrowIfFatal\([^;]*\);?'
    r'|if\s*\([^)]*(?:OutOfMemoryError|ThreadDeath|VirtualMachineError)[^)]*\)\s*throw[^;]+;'
    r'|XposedHelpers\.log\("[^"]+"\s*,\s*\w+\s*\)',
)


def xml_attr(elem, local: str) -> str:
    for key, val in elem.attrib.items():
        if key == local or key.endswith("}" + local):
            return str(val)
    return ""


def is_xml_false(value: str) -> bool:
    return value.strip().lower() in {"false", "0"}


def is_xml_true(value: str) -> bool:
    return value.strip().lower() in {"true", "1"}


def is_product_node(node_type: str) -> bool:
    return node_type in PRODUCT_NODE_TYPES


def _has_user_title(title: str | None) -> bool:
    text = (title or "").strip()
    if not text:
        return False
    if text.startswith("@string/") and len(text) <= len("@string/"):
        return False
    return True


def is_app_selector_key(key: str) -> bool:
    """True only for a trailing `_apps` selector key, not a mid-key substring."""
    return key.lower().endswith("_apps")


def classify_ui_node(
    tag: str,
    key: str,
    *,
    visible: str | None = None,
    warning: str | None = None,
    title: str | None = None,
    selectable: str | None = None,
    persistent: str | None = None,
    count_as_summary: str | None = None,
) -> str:
    """Classify from XML widget evidence. Key suffixes are never sufficient alone."""
    low = key.lower()
    short_tag = tag.rsplit(".", 1)[-1]
    vis = visible or ""
    warn = warning or ""
    titled = _has_user_title(title)
    selectable_false = is_xml_false(selectable or "")
    persistent_false = is_xml_false(persistent or "")
    count_summary = is_xml_true(count_as_summary or "")

    if low == "warning" or (is_xml_false(vis) and is_xml_true(warn)):
        return "HIDDEN_HELPER"
    if low in DI_HELPER_KEYS or (low.endswith("_island_offset") or "island_offset" in low):
        return "DYNAMIC_ISLAND_HELPER"
    if is_xml_false(vis):
        return "HIDDEN_HELPER"
    if short_tag.endswith("PreferenceCategory") or short_tag == "PreferenceCategoryEx":
        return "CATEGORY"
    if low.endswith("_cat") or "_cat_" in low:
        return "CATEGORY"
    if low in {"system", "launcher", "controls", "various", "main", "prefs"}:
        return "NAVIGATION"
    if short_tag.endswith("PreferenceScreen"):
        return "NAVIGATION"
    # Informational / non-clickable notes (e.g. netspeed prerequisite).
    if selectable_false and (persistent_false or not titled):
        return "DEPENDENCY_HELPER"
    if any(token in low for token in ("_state", "_internal", "_applied", "_synced")) and not titled:
        return "INTERNAL_STATE"
    if short_tag.endswith("CheckBoxPreferenceEx") or short_tag.endswith("SwitchPreferenceCompat"):
        return "PRODUCT_ACTION"
    if any(short_tag.endswith(t) for t in (
        "ListPreferenceEx", "DropDownPreferenceEx", "SeekBarPreference", "ColorPreferenceEx",
    )):
        return "PRODUCT_SUBOPTION"
    if short_tag.endswith("PreferenceEx"):
        if is_app_selector_key(key) or count_summary:
            return "PRODUCT_SUBOPTION"
        if titled:
            return "PRODUCT_ACTION"
        return "DEPENDENCY_HELPER"
    if titled:
        return "PRODUCT_ACTION"
    return "UNKNOWN"


GENERIC_PROOF_PHRASES = (
    "owner hook result/argument rewrite as in matched bodies",
    "installer/hook members match or api33 variant in the same capability path",
    "identified owners",
    "api33 intercept/before-after translation",
    "miui 14 member names versus hyperos",
    "miui 14 members versus hyperos",
    "differences are api33 translation",
    "miui14 vs hyperos",
    "miui 14 vs hyperos",
    "differences are api33 intercept/before-after translation",
)

REVIEWED_VARIANT_FIELDS = (
    "diff_summary",
    "value_default_comparison",
    "hook_target_comparison",
    "callback_semantics_comparison",
    "arg_result_comparison",
    "a14_only_branches",
    "why_user_behavior_is_equivalent",
)


@dataclass(frozen=True)
class SourceOwner:
    path: str
    symbol: str
    kind: str
    hook_targets: tuple[str, ...]
    callback_phases: tuple[str, ...]
    keys: tuple[str, ...]
    normalized_body: str
    installer_callees: tuple[str, ...] = ()


@dataclass
class ProofManifest:
    proof_id: str
    a14_owner_path: str
    a14_symbol: str
    a14_installer: str
    a14_hook_targets: str
    a14_callback_phase: str
    a13_owner_path: str
    a13_symbol: str
    a13_installer: str
    a13_hook_targets: str
    a13_callback_phase: str
    preference_keys: tuple[str, ...]
    value_domain: str
    default_semantics: str
    result_argument_behavior: str
    api33_variant_reason: str
    proof_conclusion: str
    evidence_level: str = "STRUCTURAL_SEMANTIC_PROOF"
    body_relation: str = ""
    diff_summary: str = ""
    value_default_comparison: str = ""
    hook_target_comparison: str = ""
    callback_semantics_comparison: str = ""
    arg_result_comparison: str = ""
    a14_only_branches: str = ""
    why_user_behavior_is_equivalent: str = ""
    key_ownership_evidence: str = ""
    a14_key_owner_reference: str = ""
    a13_key_owner_reference: str = ""

    def covers(self, key: str) -> bool:
        return key in self.preference_keys


@dataclass
class DeadPathProof:
    key: str
    a14_ui_reference: str
    a14_search_references: str
    a14_nearest_candidate: str
    why_not_reachable: str


@dataclass
class PhaseFTransitionInput:
    key: str
    node_type: str
    a14_read: bool = False
    a13_read: bool = False
    host_package: str = ""
    hook_behavior_match: bool | None = None
    source_proof: ProofManifest | None = None
    dead_proof: DeadPathProof | None = None
    rom_hold: dict[str, str] | None = None
    unproven_bucket: str = ""


@dataclass
class PhaseFDecision:
    parity_state: str
    evidence_level: str
    proof_id: str
    reason: str
    product_feature: bool = True


def classify_phase_f_transition(inp: PhaseFTransitionInput) -> PhaseFDecision:
    """Final Phase-F state transition. Same-key reads are presence, not PRESENT."""
    if not is_product_node(inp.node_type):
        return PhaseFDecision(
            parity_state="NOT_PRODUCT_FEATURE",
            evidence_level="MECHANICAL_ONLY",
            proof_id="",
            reason=f"node_type={inp.node_type} is excluded from the product universe",
            product_feature=False,
        )
    if inp.dead_proof:
        return PhaseFDecision(
            parity_state="DEAD_UPSTREAM_PATH",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            proof_id=f"PROOF_DEAD_{inp.key.upper()}",
            reason=inp.dead_proof.why_not_reachable,
        )
    if inp.source_proof:
        return PhaseFDecision(
            parity_state=inp.source_proof.proof_conclusion,
            evidence_level=inp.source_proof.evidence_level,
            proof_id=inp.source_proof.proof_id,
            reason="verified owner/source proof",
        )
    if inp.hook_behavior_match is False:
        if inp.unproven_bucket == "SOURCE_REVIEW_REQUIRED":
            return PhaseFDecision(
                parity_state="SOURCE_REVIEW_REQUIRED",
                evidence_level="IMPLEMENTATION_PRESENCE" if (inp.a14_read and inp.a13_read) else "MECHANICAL_ONLY",
                proof_id="",
                reason="same key with different hook behavior is not PRESENT; source review required",
            )
        return PhaseFDecision(
            parity_state="UNPROVEN",
            evidence_level="IMPLEMENTATION_PRESENCE" if (inp.a14_read and inp.a13_read) else "MECHANICAL_ONLY",
            proof_id="",
            reason="same key with different hook behavior is not PRESENT",
        )
    if inp.unproven_bucket == "SOURCE_REVIEW_REQUIRED":
        return PhaseFDecision(
            parity_state="SOURCE_REVIEW_REQUIRED",
            evidence_level="IMPLEMENTATION_PRESENCE" if (inp.a14_read and inp.a13_read) else "MECHANICAL_ONLY",
            proof_id="",
            reason="module-owned or statically decidable; analyzer miss is not ROM uncertainty",
        )
    if inp.a14_read and inp.a13_read:
        return PhaseFDecision(
            parity_state="UNPROVEN",
            evidence_level="IMPLEMENTATION_PRESENCE",
            proof_id="",
            reason="same key + same reads + same host is IMPLEMENTATION_PRESENCE only",
        )
    if inp.rom_hold or inp.unproven_bucket == "ROM_DEVICE_HOLD":
        return PhaseFDecision(
            parity_state="HOLD_EVIDENCE",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            proof_id="PROOF_ROM_DEVICE_HOLD",
            reason=(inp.rom_hold or {}).get("unresolved_question", "ROM/device hold"),
        )
    if not inp.a14_read:
        return PhaseFDecision(
            parity_state="UNPROVEN",
            evidence_level="MECHANICAL_ONLY",
            proof_id="",
            reason="regex/pref-read miss is discovery only; not DEAD_UPSTREAM_PATH",
        )
    return PhaseFDecision(
        parity_state="UNPROVEN",
        evidence_level="MECHANICAL_ONLY",
        proof_id="",
        reason="no source semantic proof",
    )


def normalize_source(text: str) -> str:
    text = COMMENT_BLOCK_RE.sub(" ", text)
    text = COMMENT_LINE_RE.sub(" ", text)
    text = FATAL_WRAPPER_RE.sub(" ", text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def _match_braces(text: str, open_idx: int) -> int:
    depth = 0
    i = open_idx
    n = len(text)
    while i < n:
        ch = text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return n


def _functions_in_text(text: str, path: str) -> list[tuple[str, str]]:
    found: list[tuple[str, int]] = []
    if path.endswith(".java"):
        for m in JAVA_FUN_RE.finditer(text):
            found.append((m.group(1), m.end() - 1))
    else:
        for m in KT_FUN_RE.finditer(text):
            brace = text.find("{", m.end() - 1)
            if brace < 0:
                continue
            found.append((m.group(1), brace))
    out: list[tuple[str, str]] = []
    for symbol, brace_at in found:
        if brace_at >= len(text) or text[brace_at] != "{":
            continue
        end = _match_braces(text, brace_at)
        out.append((symbol, text[brace_at:end]))
    return out


def _keys_in_text(text: str) -> tuple[str, ...]:
    keys = []
    seen = set()
    for m in PREF_GET_RE.finditer(text):
        k = m.group(1)
        if k not in seen:
            seen.add(k)
            keys.append(k)
    for m in re.finditer(r'preferenceKeys?\s*=\s*(?:listOf|setOf)\((.*?)\)', text, re.S):
        for q in re.findall(r'"([a-z0-9_]+)"', m.group(1)):
            if q not in seen:
                seen.add(q)
                keys.append(q)
    for m in re.finditer(r'preferenceKey\s*=\s*"([a-z0-9_]+)"', text):
        if m.group(1) not in seen:
            seen.add(m.group(1))
            keys.append(m.group(1))
    for m in re.finditer(
        r'"(system_[a-z0-9_]+|launcher_[a-z0-9_]+|controls_[a-z0-9_]+|various_[a-z0-9_]+|miuizer_[a-z0-9_]+)"',
        text,
    ):
        if m.group(1) not in seen:
            seen.add(m.group(1))
            keys.append(m.group(1))
    for m in re.finditer(r'"pref_key_([a-z0-9_]+)"', text):
        if m.group(1) not in seen:
            seen.add(m.group(1))
            keys.append(m.group(1))
    return tuple(keys)


def _hook_targets(text: str) -> tuple[str, ...]:
    targets: list[str] = []
    seen = set()
    def add(item: str) -> None:
        if item and item not in seen:
            seen.add(item)
            targets.append(item)
    for m in HOOK_STRING_RE.finditer(text):
        add(f"{m.group(1)}#{m.group(2)}")
    for m in HOOK_METHOD_ONLY_RE.finditer(text):
        add(f"#{m.group(1)}")
    for m in HOOK_ALL_CTOR_RE.finditer(text):
        add(f"{m.group(1)}#<init>")
    return tuple(targets)


def _callback_phases(text: str) -> tuple[str, ...]:
    phases = []
    for m in CALLBACK_RE.finditer(text):
        if m.group(1) not in phases:
            phases.append(m.group(1))
    if "chain.proceed" in text or "intercept" in text:
        if "intercept" not in phases:
            phases.append("intercept")
    for m in JAVA_CALLBACK_RE.finditer(text):
        name = m.group(1)
        if name not in phases:
            phases.append(name)
    return tuple(phases) or ("unknown",)


def _installer_callees(text: str) -> tuple[str, ...]:
    callees = []
    seen = set()
    for m in INSTALL_CALLEE_RE.finditer(text):
        name = f"{m.group(1)}.{m.group(2)}"
        if name not in seen and ("Hook" in name or name.endswith(".hook")):
            seen.add(name)
            callees.append(name)
    return tuple(callees)


JUNK_SYMBOLS = frozenset({
    "evaluateEnabled", "isEnabledCondition", "install", "hasAnySystemUiStartupFeature",
    "before", "after", "invoke", "initialize", "refreshSnapshotLocked",
    "onActivityCreated", "onCreate", "onCreateView", "onResume", "onPause",
    "onPreferenceClick", "onPreferenceChange", "onViewCreated",
})
HOOK_CALL_RE = re.compile(r'(?:([A-Za-z][\w]*)\.)?([A-Za-z][\w]*Hook)\s*\(')
SKIP_CALLEES = frozenset({"MethodHook", "XC_MethodHook", "installHook", "HookerClassHelper"})
GET_KEY_RE = re.compile(r'get(?:Boolean|Int|Long|Float|String|StringAsInt)\("([a-z0-9_]+)"')


@dataclass
class RepoScan:
    owners: dict[str, list[SourceOwner]]
    symbols: dict[str, list[SourceOwner]]
    callees: dict[str, set[str]]


_SCAN_CACHE: dict[str, RepoScan] = {}


def _rel_java_path(repo: Path, src: Path) -> str:
    try:
        return str(src.relative_to(repo)).replace("\\", "/")
    except ValueError:
        return str(src).replace("\\", "/")


def _owner_rank(owner: SourceOwner) -> int:
    score = 0
    if owner.symbol in JUNK_SYMBOLS:
        score -= 20
    if owner.symbol.endswith("Hook"):
        score += 8
    if owner.hook_targets:
        score += 5
    if owner.kind == "hook":
        score += 3
    if owner.kind == "spec" and owner.symbol == "installHook":
        score += 4
    if owner.kind == "installer":
        score -= 6
    if owner.kind == "spec" and owner.symbol in {"evaluateEnabled", "isEnabledCondition"}:
        score -= 12
    return score


INSTALLER_BIND_CHARS = 400


def _key_callees_in_text(text: str) -> dict[str, set[str]]:
    """Bind a key only to the next *Hook( after that key, within INSTALLER_BIND_CHARS.

    A large lookback window is not installer evidence: it attaches sibling
    getBoolean keys to the wrong hook.
    """
    out: dict[str, set[str]] = {}
    hook_positions = [
        (m.start(), m.group(2))
        for m in HOOK_CALL_RE.finditer(text)
        if m.group(2) not in SKIP_CALLEES
    ]
    key_hits = [(m.end(), m.group(1)) for m in GET_KEY_RE.finditer(text)]
    key_hits += [(m.end(), m.group(1)) for m in re.finditer(r'preferenceKey\s*=\s*"([a-z0-9_]+)"', text)]
    for end, key in key_hits:
        if not key or key.startswith("pref_key_"):
            continue
        for pos, callee in hook_positions:
            if pos < end:
                continue
            if pos - end > INSTALLER_BIND_CHARS:
                break
            out.setdefault(key, set()).add(callee)
            break
    return out


def scan_repo(repo: Path) -> RepoScan:
    cache_key = str(repo.resolve()) if repo.exists() else str(repo)
    cached = _SCAN_CACHE.get(cache_key)
    if cached:
        return cached
    owners_by_key: dict[str, list[SourceOwner]] = {}
    symbols: dict[str, list[SourceOwner]] = {}
    callees: dict[str, set[str]] = {}
    root = repo / "app/src/main/java"
    if root.exists():
        files = list(root.rglob("*.kt")) + list(root.rglob("*.java"))
        for src in files:
            text = src.read_text(encoding="utf-8", errors="ignore")
            rel = _rel_java_path(repo, src)
            kind_file = "installer" if "Installer" in src.name or "Router" in src.name else "hook"
            if "feature" in rel.lower() and "Feature" in src.name:
                kind_file = "spec"
            for key, names in _key_callees_in_text(text).items():
                callees.setdefault(key, set()).update(names)
            for symbol, body in _functions_in_text(text, rel):
                keys = _keys_in_text(body)
                owner = SourceOwner(
                    path=rel,
                    symbol=symbol,
                    kind=kind_file,
                    hook_targets=_hook_targets(body),
                    callback_phases=_callback_phases(body),
                    keys=keys,
                    normalized_body=normalize_source(body),
                    installer_callees=_installer_callees(body),
                )
                symbols.setdefault(symbol, []).append(owner)
                for key in keys:
                    owners_by_key.setdefault(key, []).append(owner)
    scan = RepoScan(owners=owners_by_key, symbols=symbols, callees=callees)
    _SCAN_CACHE[cache_key] = scan
    return scan


def discover_source_owners(repo: Path) -> dict[str, list[SourceOwner]]:
    return scan_repo(repo).owners


def hook_targets_compatible(a14: SourceOwner, a13: SourceOwner) -> bool:
    if a14.hook_targets and a13.hook_targets:
        a14_methods = {t.split("#", 1)[-1] for t in a14.hook_targets}
        a13_methods = {t.split("#", 1)[-1] for t in a13.hook_targets}
        if a14_methods & a13_methods:
            return True
        a14_cls = {t.split("#", 1)[0].rsplit(".", 1)[-1] for t in a14.hook_targets if t.split("#", 1)[0]}
        a13_cls = {t.split("#", 1)[0].rsplit(".", 1)[-1] for t in a13.hook_targets if t.split("#", 1)[0]}
        if a14_cls & a13_cls:
            return True
        return False
    if not a14.hook_targets and not a13.hook_targets:
        return True
    return False


def _basename(path: str) -> str:
    return path.rsplit("/", 1)[-1]


def installer_ownership_compatible(
    a14: SourceOwner,
    a13: SourceOwner,
    left: list[SourceOwner] | None = None,
    right: list[SourceOwner] | None = None,
) -> bool:
    if _basename(a14.path) == _basename(a13.path):
        return True
    if a14.kind in {"spec", "installer"} and a13.kind in {"installer", "hook"}:
        return True
    if a13.kind == "spec" and a14.kind in {"installer", "hook"}:
        return True
    left = left or []
    right = right or []
    if any(o.kind == "installer" for o in left) and any(o.kind == "installer" for o in right):
        return True
    if "Installer" in a14.path or "Installer" in a13.path:
        return True
    if a14.kind == "hook" and a13.kind == "hook" and "mods/" in a14.path and "mods/" in a13.path:
        return True
    return False


def is_generic_proof_text(text: str) -> bool:
    low = (text or "").strip().lower()
    if not low:
        return True
    return any(p in low for p in GENERIC_PROOF_PHRASES)


def reviewed_variant_fields_complete(man: ProofManifest) -> bool:
    if man.body_relation != "REVIEWED_VARIANT":
        return False
    for name in REVIEWED_VARIANT_FIELDS:
        value = getattr(man, name, "")
        if not str(value).strip() or is_generic_proof_text(str(value)):
            return False
    return True


def proof_is_acceptable(man: ProofManifest) -> bool:
    if man.proof_conclusion == "PRESENT_EQUIVALENT":
        return man.body_relation == "IDENTICAL"
    if man.proof_conclusion == "PRESENT_A13_VARIANT":
        if not reviewed_variant_fields_complete(man):
            return False
        if man.proof_id.startswith("PROOF_OG_"):
            return bool(
                man.key_ownership_evidence
                and man.a14_key_owner_reference
                and man.a13_key_owner_reference
            )
        return True
    return True


def is_module_owned_settings(key: str, host_package: str = "") -> bool:
    if key.startswith("miuizer_") or key in {"miuizer_locale", "pref_key_miuizer_locale"}:
        return True
    return host_package == "SETTINGS"


def classify_unproven_bucket(
    key: str,
    *,
    host_package: str = "",
    has_a13: bool = False,
    a14_owner_found: bool = False,
    a13_owner_found: bool = False,
    in_rom_hold_map: bool = False,
) -> str:
    """Split analyzer misses from genuine ROM uncertainty. Not a final CSV state."""
    if is_module_owned_settings(key, host_package):
        return "SOURCE_REVIEW_REQUIRED"
    if has_a13 and (not a14_owner_found or not a13_owner_found):
        return "SOURCE_REVIEW_REQUIRED"
    if in_rom_hold_map and not has_a13:
        return "ROM_DEVICE_HOLD"
    if has_a13:
        return "SOURCE_REVIEW_REQUIRED"
    if in_rom_hold_map:
        return "ROM_DEVICE_HOLD"
    return "SOURCE_REVIEW_REQUIRED"


GET_DEFAULT_RE = re.compile(
    r'get(?:Boolean|Int|Long|Float|String|StringSet|StringAsInt)\("([a-z0-9_]+)"\s*,\s*([^)]+)\)'
)


def extract_pref_defaults(body: str) -> dict[str, str]:
    return {m.group(1): m.group(2).strip() for m in GET_DEFAULT_RE.finditer(body or "")}


def strip_hook_scaffolding(body: str) -> str:
    """Compare inner rewrite logic; not used for IDENTICAL auto-fingerprint."""
    text = body or ""
    text = re.sub(r'chain\.proceed\(\)', " ORIG_CALL ", text)
    text = re.sub(r'XposedHelpers\.(?:throwOrReturn|proceedOrThrow)\([^)]*\)', " ", text)
    text = re.sub(r'override fun intercept\([^)]*\)', " ", text)
    text = re.sub(r'override fun before\([^)]*\)', " ", text)
    text = re.sub(r'override fun after\([^)]*\)', " ", text)
    text = re.sub(r'param\.returnAndSkip\(', " SKIP(", text)
    text = re.sub(r'returnAndSkip\(', " SKIP(", text)
    text = re.sub(r'param\.setResult\(', " SET_RESULT(", text)
    text = re.sub(r'setResult\(', " SET_RESULT(", text)
    text = re.sub(r'var skipped = false', " ", text)
    text = re.sub(r'if \(skipped\) \{[^}]*\}', " ", text)
    return normalize_source(text)


def compact_diff_summary(a14_body: str, a13_body: str) -> str:
    sm = SequenceMatcher(a=a14_body, b=a13_body)
    parts = [f"normalized_ratio={sm.ratio():.3f}"]
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            continue
        a14_snip = a14_body[i1:i2][:90].replace("\n", " ")
        a13_snip = a13_body[j1:j2][:90].replace("\n", " ")
        parts.append(f"{tag}: A14`{a14_snip}` A13`{a13_snip}`")
        if len(parts) >= 7:
            break
    return "; ".join(parts)[:900]


def result_polarity_conflict(a14_body: str, a13_body: str) -> bool:
    def flags(body: str) -> tuple[bool, bool]:
        true_hit = bool(re.search(
            r'setResult\(\s*true\s*\)|result\s*=\s*true|returnAndSkip\(\s*true\s*\)|SKIP\(\s*true\s*\)|throwOrReturn\([^)]*\btrue\b',
            body,
        ))
        false_hit = bool(re.search(
            r'setResult\(\s*false\s*\)|result\s*=\s*false|returnAndSkip\(\s*false\s*\)|SKIP\(\s*false\s*\)|throwOrReturn\([^)]*\bfalse\b',
            body,
        ))
        return true_hit, false_hit

    a14_true, a14_false = flags(a14_body)
    a13_true, a13_false = flags(a13_body)
    if a14_true and a13_false and not a14_false and not a13_true:
        return True
    if a14_false and a13_true and not a14_true and not a13_false:
        return True
    return False


def extract_result_ops(body: str) -> str:
    bits: list[str] = []
    for label, pat in (
        ("returnAndSkip", r'returnAndSkip\(([^)]*)\)'),
        ("setResult", r'setResult\(([^)]*)\)'),
        ("result_assign", r'result\s*=\s*(true|false|null)'),
        ("chain.proceed", r'chain\.proceed\(\)'),
        ("SKIP", r'SKIP\(([^)]*)\)'),
    ):
        hits = re.findall(pat, body or "")
        if hits:
            sample = ",".join(str(h) for h in hits[:4])
            bits.append(f"{label}[{sample}]" if not isinstance(hits[0], tuple) else f"{label}x{len(hits)}")
            if isinstance(hits[0], str) and hits[0] not in {"true", "false", "null"}:
                bits[-1] = f"{label}[{sample}]"
    return "; ".join(bits) or "no result/argument rewrite literals"


def match_owner_pair(a14_owners: list[SourceOwner], a13_owners: list[SourceOwner]) -> tuple[SourceOwner, SourceOwner] | None:
    def usable(owners: list[SourceOwner]) -> list[SourceOwner]:
        ranked = sorted(owners, key=_owner_rank, reverse=True)
        filtered = [o for o in ranked if o.symbol not in JUNK_SYMBOLS]
        return filtered or ranked

    a14_hooks = usable([o for o in a14_owners if o.kind != "installer"] or a14_owners)
    a13_hooks = usable([o for o in a13_owners if o.kind != "installer"] or a13_owners)
    for a14 in a14_hooks:
        for a13 in a13_hooks:
            if a14.symbol == a13.symbol and _basename(a14.path) == _basename(a13.path):
                return a14, a13
    for a14 in a14_hooks:
        for a13 in a13_hooks:
            if a14.symbol == a13.symbol:
                return a14, a13
    a13_by_symbol = {o.symbol: o for o in a13_hooks}
    for a14 in a14_hooks:
        for callee in a14.installer_callees:
            simple = callee.split(".")[-1]
            if simple in a13_by_symbol:
                return a14, a13_by_symbol[simple]
    for a14 in a14_hooks:
        for a13 in a13_hooks:
            if _basename(a14.path) == _basename(a13.path) and (
                hook_targets_compatible(a14, a13) or bool(set(a14.keys) & set(a13.keys))
            ):
                return a14, a13
    for a14 in a14_hooks:
        for a13 in a13_hooks:
            if set(a14.keys) == set(a13.keys) and hook_targets_compatible(a14, a13) and a14.keys:
                return a14, a13
    if a14_hooks and a13_hooks and _owner_rank(a14_hooks[0]) >= 4 and _owner_rank(a13_hooks[0]) >= 4:
        if hook_targets_compatible(a14_hooks[0], a13_hooks[0]):
            return a14_hooks[0], a13_hooks[0]
    best: tuple[SourceOwner, SourceOwner] | None = None
    best_n = 0
    for a14 in a14_hooks[:6]:
        for a13 in a13_hooks[:6]:
            overlap = len(set(a14.keys) & set(a13.keys))
            if overlap > best_n:
                best_n = overlap
                best = (a14, a13)
    if best and best_n >= 2:
        return best
    return None


def _pick_symbol_owner(candidates: list[SourceOwner]) -> SourceOwner | None:
    if not candidates:
        return None
    return sorted(candidates, key=_owner_rank, reverse=True)[0]


def _candidate_owner_pairs(
    key: str,
    left: list[SourceOwner],
    right: list[SourceOwner],
    a14_scan: RepoScan | None,
    a13_scan: RepoScan | None,
) -> list[tuple[SourceOwner, SourceOwner, tuple[str, ...]]]:
    pairs: list[tuple[SourceOwner, SourceOwner, tuple[str, ...]]] = []
    seen: set[tuple[str, str, str, str]] = set()

    def add(a14: SourceOwner, a13: SourceOwner) -> None:
        ident = (a14.path, a14.symbol, a13.path, a13.symbol)
        if ident in seen:
            return
        seen.add(ident)
        covered = tuple(sorted((set(a14.keys) & set(a13.keys)) | {key}))
        pairs.append((a14, a13, covered))

    if a14_scan and a13_scan:
        shared = (a14_scan.callees.get(key) or set()) & (a13_scan.callees.get(key) or set())
        for callee in shared:
            a14 = _pick_symbol_owner(a14_scan.symbols.get(callee) or [])
            a13 = _pick_symbol_owner(a13_scan.symbols.get(callee) or [])
            if a14 and a13:
                add(a14, a13)
    pair = match_owner_pair(left, right) if left and right else None
    if pair:
        add(*pair)
    return pairs


def _identical_manifest(
    key: str,
    a14: SourceOwner,
    a13: SourceOwner,
    left: list[SourceOwner],
    right: list[SourceOwner],
    covered: tuple[str, ...],
) -> ProofManifest | None:
    if not a14.normalized_body or a14.normalized_body != a13.normalized_body:
        return None
    if key not in a14.keys or key not in a13.keys:
        return None
    if not installer_ownership_compatible(a14, a13, left, right):
        return None
    a14_installers = [o for o in left if o.kind == "installer"]
    a13_installers = [o for o in right if o.kind == "installer"]
    del covered
    keys = (key,)
    defaults = extract_pref_defaults(a14.normalized_body)
    return ProofManifest(
        proof_id=f"PROOF_FP_{_basename(a13.path).replace('.', '_')}_{a13.symbol}",
        a14_owner_path=a14.path,
        a14_symbol=a14.symbol,
        a14_installer=a14_installers[0].path if a14_installers else (a14.path if a14.kind in {"spec", "installer"} else a14.path),
        a14_hook_targets=",".join(a14.hook_targets) or "(no host hook members)",
        a14_callback_phase=",".join(a14.callback_phases),
        a13_owner_path=a13.path,
        a13_symbol=a13.symbol,
        a13_installer=a13_installers[0].path if a13_installers else (a13.path if a13.kind == "installer" else a13.path),
        a13_hook_targets=",".join(a13.hook_targets) or "(no host hook members)",
        a13_callback_phase=",".join(a13.callback_phases),
        preference_keys=keys,
        value_domain="owner-local preference domain",
        default_semantics=f"shared defaults {defaults.get(key, '(no explicit default literal)')}",
        result_argument_behavior=extract_result_ops(a14.normalized_body),
        api33_variant_reason="Normalized owner bodies are identical; same relevant keys; compatible installer ownership.",
        proof_conclusion="PRESENT_EQUIVALENT",
        evidence_level="STRUCTURAL_SEMANTIC_PROOF",
        body_relation="IDENTICAL",
        diff_summary="normalized bodies IDENTICAL",
        value_default_comparison=f"A14={defaults.get(key, 'n/a')}; A13={extract_pref_defaults(a13.normalized_body).get(key, 'n/a')}",
        hook_target_comparison=f"A14={','.join(a14.hook_targets) or 'none'}; A13={','.join(a13.hook_targets) or 'none'}",
        callback_semantics_comparison=f"A14={','.join(a14.callback_phases)}; A13={','.join(a13.callback_phases)}",
        arg_result_comparison=extract_result_ops(a14.normalized_body),
        a14_only_branches="none (identical body)",
        why_user_behavior_is_equivalent="Normalized owner text is identical, so preference reads and rewrite operations match.",
        key_ownership_evidence=f"{key}: LITERAL_READ in both owner bodies",
        a14_key_owner_reference=f"{a14.path}::{a14.symbol} LITERAL_READ {key}",
        a13_key_owner_reference=f"{a13.path}::{a13.symbol} LITERAL_READ {key}",
    )


def fingerprint_proof_for_key(
    key: str,
    a14_owners: dict[str, list[SourceOwner]],
    a13_owners: dict[str, list[SourceOwner]],
    *,
    a14_scan: RepoScan | None = None,
    a13_scan: RepoScan | None = None,
) -> ProofManifest | None:
    """Automatic PRESENT is allowed only for identical normalized bodies."""
    left = a14_owners.get(key) or []
    right = a13_owners.get(key) or []
    for a14, a13, covered in _candidate_owner_pairs(key, left, right, a14_scan, a13_scan):
        if a14.symbol in JUNK_SYMBOLS or a13.symbol in JUNK_SYMBOLS:
            continue
        man = _identical_manifest(key, a14, a13, left, right, covered)
        if man:
            return man
    return None


def source_review_variant_for_pair(
    key: str,
    a14: SourceOwner,
    a13: SourceOwner,
    left: list[SourceOwner],
    right: list[SourceOwner],
    covered: tuple[str, ...],
) -> ProofManifest | None:
    """Ratio/similarity must never authorize PRESENT. Owner-group review lives in parity_owner_groups."""
    del key, a14, a13, left, right, covered
    return None


def source_review_proof_for_key(
    key: str,
    a14_owners: dict[str, list[SourceOwner]],
    a13_owners: dict[str, list[SourceOwner]],
    *,
    a14_scan: RepoScan | None = None,
    a13_scan: RepoScan | None = None,
) -> ProofManifest | None:
    left = a14_owners.get(key) or []
    right = a13_owners.get(key) or []
    for a14, a13, covered in _candidate_owner_pairs(key, left, right, a14_scan, a13_scan):
        if a14.symbol in JUNK_SYMBOLS or a13.symbol in JUNK_SYMBOLS:
            continue
        man = source_review_variant_for_pair(key, a14, a13, left, right, covered)
        if man:
            return man
    return None


def _exclusive_keys_for_pair(
    a14: SourceOwner,
    a13: SourceOwner,
    a14_owners: dict[str, list[SourceOwner]],
    a13_owners: dict[str, list[SourceOwner]],
) -> tuple[str, ...]:
    shared = set(a14.keys) & set(a13.keys)
    return tuple(sorted(shared))



def _with_phase_e_reviewed_fields(man: ProofManifest) -> ProofManifest:
    if man.body_relation:
        return man
    man.body_relation = "REVIEWED_VARIANT"
    man.diff_summary = man.api33_variant_reason
    man.value_default_comparison = man.default_semantics
    man.hook_target_comparison = f"A14={man.a14_hook_targets}; A13={man.a13_hook_targets}"
    man.callback_semantics_comparison = f"A14={man.a14_callback_phase}; A13={man.a13_callback_phase}"
    man.arg_result_comparison = man.result_argument_behavior
    man.a14_only_branches = man.api33_variant_reason
    man.why_user_behavior_is_equivalent = f"{man.result_argument_behavior}. {man.api33_variant_reason}"
    return man


def phase_e_source_proofs() -> list[ProofManifest]:
    """Source-backed Phase E preserves plus F-R2 explicit reviewed variants."""
    raw = [
        ProofManifest(
            proof_id="PROOF_FSG_HORIZ",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt",
            a14_symbol="FSGesturesHook",
            a14_installer="ForceFsgNavBarCallerScope + Launcher installer / A14 launcher features",
            a14_hook_targets=(
                "com.miui.home.launcher.DeviceConfig#usingFsGesture,"
                "com.miui.home.recents.BaseRecentsImpl#createAndAddNavStubView,"
                "com.miui.home.recents.BaseRecentsImpl#updateFsgWindowState,"
                "com.miui.launcher.utils.MiuiSettingsUtils#getGlobalBoolean,"
                "com.miui.home.recents.GestureStubView#onTouchEvent,"
                "BaseRecentsImpl#lambda$showBackStubWindow,"
                "BaseRecentsImpl#lambda$updateFsgWindowVisibilityState"
            ),
            a14_callback_phase="intercept",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt",
            a13_symbol="FSGesturesHook",
            a13_installer="installers/LauncherInstaller.java",
            a13_hook_targets=(
                "com.miui.home.launcher.DeviceConfig#usingFsGesture,"
                "com.miui.home.recents.BaseRecentsImpl#createAndAddNavStubView,"
                "com.miui.home.recents.BaseRecentsImpl#updateFsgWindowState,"
                "com.miui.launcher.utils.MiuiSettingsUtils#getGlobalBoolean,"
                "com.miui.home.recents.GestureStubView#onTouchEvent"
            ),
            a13_callback_phase="before,after",
            preference_keys=("controls_fsg_horiz", "controls_fsg_horiz_apps"),
            value_domain="boolean force-FSG + StringSet skip-apps",
            default_semantics="controls_fsg_horiz default false; controls_fsg_horiz_apps default empty set",
            result_argument_behavior=(
                "usingFsGesture constant true; createAndAddNavStubView skipped when REAL_FORCE_FSG_NAV_BAR is false; "
                "updateFsgWindowState removes mNavStubView when not fsg; getGlobalBoolean(force_fsg_nav_bar) stashes "
                "the real result then reports true for BaseRecents callers; GestureStubView.onTouchEvent skipped for "
                "packages in controls_fsg_horiz_apps"
            ),
            api33_variant_reason=(
                "A13 identifies BaseRecentsImpl callers of getGlobalBoolean by walking Thread.currentThread().stackTrace "
                "for class com.miui.home.recents.BaseRecentsImpl. A14 uses ForceFsgNavBarCallerScope ThreadLocal around "
                "three verified HyperOS members. On API33/MIUI 14 the lambda names are not required; the stack-trace "
                "class filter preserves force-FSG plus per-app skip without those members."
            ),
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary=(
                "Shared: DeviceConfig.usingFsGesture=true, createAndAddNavStubView skip, updateFsgWindowState stub "
                "removal, GestureStubView skip-apps. Differ: A14 intercept/chain.proceed + ForceFsgNavBarCallerScope "
                "ThreadLocal on updateFsgWindowState and two BaseRecentsImpl lambdas; A13 before/after + stack-trace "
                "class scan in getGlobalBoolean."
            ),
            value_default_comparison=(
                "Both treat controls_fsg_horiz as the enable gate and controls_fsg_horiz_apps as the skip package set; "
                "neither inverts the boolean or replaces the StringSet with a whitelist."
            ),
            hook_target_comparison=(
                "Shared members: usingFsGesture, createAndAddNavStubView, updateFsgWindowState, getGlobalBoolean, "
                "GestureStubView.onTouchEvent. A14-only: lambda$showBackStubWindow$*$BaseRecentsImpl(boolean) and "
                "lambda$updateFsgWindowVisibilityState$*$BaseRecentsImpl(boolean, String)."
            ),
            callback_semantics_comparison=(
                "A14: intercept with one chain.proceed() on the unskipped path. A13: before returnAndSkip for "
                "createAndAddNavStubView/GestureStubView; after setResult(true) for getGlobalBoolean. proceed-once vs "
                "skip/setResult maps to the same skip-or-force-true user path."
            ),
            arg_result_comparison=(
                "Both stash REAL_FORCE_FSG_NAV_BAR from the real getGlobalBoolean result then report true to "
                "BaseRecents; both returnAndSkip(false) on GestureStubView ACTION_DOWN when the foreground package is "
                "in controls_fsg_horiz_apps; neither rewrites the MotionEvent."
            ),
            a14_only_branches=(
                "ForceFsgNavBarCallerScope fail-closed install of three verified BaseRecentsImpl callers; "
                "lambda$showBackStubWindow and lambda$updateFsgWindowVisibilityState. A13 does not hook those lambdas."
            ),
            why_user_behavior_is_equivalent=(
                "User-visible contract is force full-screen gestures plus disable horizontal FSG in selected apps. "
                "A13's stack-trace BaseRecentsImpl filter is the API33-compatible caller scope: it does not depend on "
                "HyperOS-only lambda names that MIUI 14 Home may lack. Extra A14 caller wrappers are robustness, not a "
                "second toggle."
            ),
        ),
        ProofManifest(
            proof_id="PROOF_MIUIZER_LOCALE",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/utils/AppLocaleController.kt",
            a14_symbol="AppLocaleController",
            a14_installer="Settings app / MainApplication apply()",
            a14_hook_targets="(settings app, no host hook)",
            a14_callback_phase="n/a",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/utils/AppLocaleController.kt",
            a13_symbol="AppLocaleController",
            a13_installer="AboutFragment.setupLocalePreference + MainApplication.apply()",
            a13_hook_targets="(settings app, no host hook)",
            a13_callback_phase="n/a",
            preference_keys=("miuizer_locale",),
            value_domain="locale tag: auto|en|zh-CN|zh-TW|ru-RU|ja-JP|vi-VN|cs-CZ|pt-BR|tr-TR|es-ES",
            default_semantics="default `auto`; unknown/legacy `1` normalize to auto",
            result_argument_behavior=(
                "Persists pref_key_miuizer_locale; apply() writes LocaleManager.applicationLocales or clears on auto; "
                "pref_key_miuizer_locale_applied is a derived fast-path marker, not a second user setting"
            ),
            api33_variant_reason=(
                "Both trees own AppLocaleController on API33 LocaleManager. A13 ListPreferenceEx lives on About; "
                "A14 row is on prefs_main.xml. Screen placement does not change the persisted tag or apply() contract."
            ),
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary=(
                "Shared: same LOCALE_PREF_KEY / APPLIED_LOCALE_PREF_KEY, same SUPPORTED_LOCALE_TAGS, auto fast-path, "
                "LocaleManager.applicationLocales. A14 adds setUserLocale commit rollback, Locale.setDefault, "
                "AppLocaleGateway test seam, FatalErrors. A13 keeps optional Context apply(), applicationLocaleApplier/"
                "Provider hooks, getLocaleContext no-op."
            ),
            value_default_comparison="Both default getString(pref_key_miuizer_locale, auto) and normalize unknown tags to auto.",
            hook_target_comparison="Neither side hooks SystemUI/Home; this is module Settings/app locale only.",
            callback_semantics_comparison="No Xposed callback. Change is persist + process restart / next apply().",
            arg_result_comparison="No host setResult. Framework write is LocaleManager.applicationLocales = tag list or empty for auto.",
            a14_only_branches="setUserLocale rollback on failed commit; Locale.setDefault; AppLocaleGateway.",
            why_user_behavior_is_equivalent=(
                "The user-visible control is the same language list persisted in pref_key_miuizer_locale and applied "
                "through Android 13 LocaleManager. No SystemUI/Home dump is required to decide this row."
            ),
        ),
        ProofManifest(
            proof_id="PROOF_BACKUP_V2",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/utils/BackupFormatV2.kt",
            a14_symbol="BackupFormatV2",
            a14_installer="app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt",
            a14_hook_targets="(settings app, no host hook)",
            a14_callback_phase="n/a",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/utils/BackupFormatV2.kt",
            a13_symbol="BackupFormatV2",
            a13_installer="app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt",
            a13_hook_targets="(settings app, no host hook)",
            a13_callback_phase="n/a",
            preference_keys=(),
            value_domain="typed backup entries / CUI2",
            default_semantics="encode V2; restore auto-detects V2 vs legacy",
            result_argument_behavior="CRC/size bounds; rollback on commit failure",
            api33_variant_reason="A13 V2 contract matches A14 M2 typed backup; no API34 host types.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_USB_DEFAULT_R1_LATCH",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt",
            a14_symbol="UsbDefaultFunctionFeature",
            a14_installer="SystemServerFeatures.kt::UsbDefaultFunctionFeatureId",
            a14_hook_targets="UsbDeviceManager / HAL setEnabledFunctions",
            a14_callback_phase="after/intercept",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt",
            a13_symbol="USBConfigHook",
            a13_installer="installers/SystemServerInstaller.java + SettingsInstaller.java",
            a13_hook_targets="UsbDeviceManager.setCurrentFunction; UsbConnectLatch rising-edge",
            a13_callback_phase="after",
            preference_keys=("system_usb_default_function", "system_defaultusb", "system_defaultusb_unsecure"),
            value_domain="follow-system/charge/MTP/PTP mapped onto A13 function strings",
            default_semantics="none = follow system; unsecure latch ignored when none",
            result_argument_behavior="A13 setCurrentFunction; disconnect clears UsbConnectLatch",
            api33_variant_reason="A14 HAL setEnabledFunctions(JZI) is not copied; A13 owns setCurrentFunction + R1 latch.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_HIDE_IME_DISMISS",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a14_symbol="HideImeDismissButtonHook",
            a14_installer="mods/utils/feature/SystemUiFeatures.kt",
            a14_hook_targets="com.android.systemui.navigationbar.NavigationBarView#updateNavButtonIcons",
            a14_callback_phase="after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a13_symbol="HideImeDismissButtonHook",
            a13_installer="installers/SystemUiInstaller.java",
            a13_hook_targets="com.android.systemui.navigationbar.NavigationBarView#updateNavButtonIcons",
            a13_callback_phase="after",
            preference_keys=("controls_hide_ime_dismiss_button",),
            value_domain="boolean",
            default_semantics="false keeps stock IME dismiss",
            result_argument_behavior="after updateNavButtonIcons, set IME back-alt visibility INVISIBLE when gestural",
            api33_variant_reason="Same NavigationBarView member; A13 uses installer boolean vs A14 FeatureSpec.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_LAUNCHER_DOCK_HEIGHT",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt",
            a14_symbol="DockHeightHook",
            a14_installer="mods/utils/feature/LauncherPackageReadyFeatures.kt",
            a14_hook_targets="com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight",
            a14_callback_phase="before",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt",
            a13_symbol="DockHeightHook",
            a13_installer="installers/LauncherInstaller.java",
            a13_hook_targets="com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight",
            a13_callback_phase="before",
            preference_keys=("launcher_dock_height",),
            value_domain="int dp, default 60",
            default_semantics="<=60 keeps ROM hotseat height",
            result_argument_behavior="before-hook returnAndSkip dp2px(dockHeight)",
            api33_variant_reason="Same DeviceConfig.calcHotSeatsHeight member on MIUI Home.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_FOLDER_BLUR_DISABLE",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt",
            a14_symbol="FolderBlurHook",
            a14_installer="mods/utils/feature/LauncherPostAttachFeatures.kt",
            a14_hook_targets="com.miui.home.launcher.common.BlurUtils#getLauncherBlur; FolderCling#open",
            a14_callback_phase="before/after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt",
            a13_symbol="FolderBlurHook",
            a13_installer="installers/LauncherInstaller.java",
            a13_hook_targets="com.miui.home.launcher.common.BlurUtils#getLauncherBlur; FolderCling#open",
            a13_callback_phase="before/after",
            preference_keys=("launcher_folderblur_disable", "launcher_folderblur_opacity"),
            value_domain="boolean disable + int opacity",
            default_semantics="disable=false uses opacity overlay; disable=true forces clear background",
            result_argument_behavior="resolveFolderBlurRatio(disable, opacity) skipped into getLauncherBlur",
            api33_variant_reason="A13 FolderBlurHook gained the A14 disable flag without replacing opacity storage.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_CHARGINGINFO_FONTSIZE",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt",
            a14_symbol="ChargingInfoHook",
            a14_installer="SystemUi feature catalog",
            a14_hook_targets="KeyguardIndicationTextView#<init>",
            a14_callback_phase="after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt",
            a13_symbol="ChargingInfoHook",
            a13_installer="mods/catalog/FeatureCatalog.kt + SystemUiInstaller.java",
            a13_hook_targets="com.android.systemui.statusbar.phone.KeyguardIndicationTextView#<init>",
            a13_callback_phase="after",
            preference_keys=("system_charginginfo_fontsize",),
            value_domain="int sp, default 16",
            default_semantics="default keeps system text size",
            result_argument_behavior="setTextSize(COMPLEX_UNIT_SP) when resolveChargingInfoFontSizeSp non-null",
            api33_variant_reason="Same KeyguardIndicationTextView constructor hook; fontsize is an A13-owned suboption.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_NETSPEED_CLOCK_STYLE",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a14_symbol="initNetSpeedStyle",
            a14_installer="installers/SystemUiInstaller.java / A14 SystemUiFeatures",
            a14_hook_targets="status bar NetworkSpeedView / meter style path",
            a14_callback_phase="after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a13_symbol="initNetSpeedStyle",
            a13_installer="installers/SystemUiInstaller.java",
            a13_hook_targets="status bar NetworkSpeedView / meter style path",
            a13_callback_phase="after",
            preference_keys=("system_netspeed_use_clock_style",),
            value_domain="boolean",
            default_semantics="false keeps netspeed typeface; true copies status-bar clock appearance",
            result_argument_behavior="applyStatusBarClockTextAppearance on netspeed text views",
            api33_variant_reason="A13 NetSpeedTypefaceHelper gained clock-style copy; same status-bar meter owner.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_DUALROWS_LEFT_RATIO",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a14_symbol="DualRowsHook",
            a14_installer="SystemUi installer / A14 features",
            a14_hook_targets="MiuiPhoneStatusBarView#updateCutoutLocation",
            a14_callback_phase="after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a13_symbol="DualRowStatusbarHook",
            a13_installer="installers/SystemUiInstaller.java",
            a13_hook_targets="com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation",
            a13_callback_phase="after",
            preference_keys=("system_statusbar_dualrows_left_ratio",),
            value_domain="int ratio, default 4",
            default_semantics="default split; custom ratio on no-cutout type 0",
            result_argument_behavior="left/right LinearLayout weights from resolveDualRowsCutoutWeights",
            api33_variant_reason="Same MiuiPhoneStatusBarView.updateCutoutLocation owner on A13 SystemUI.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_WIRELESS_HEADSET_SLOT",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a14_symbol="checkSlot",
            a14_installer="installers/SystemUiInstaller.java / A14 features",
            a14_hook_targets="status bar icon slot hide path",
            a14_callback_phase="before",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a13_symbol="checkSlot",
            a13_installer="installers/SystemUiInstaller.java",
            a13_hook_targets="status bar icon slot hide path",
            a13_callback_phase="before",
            preference_keys=("system_statusbaricons_wireless_headset",),
            value_domain="boolean",
            default_semantics="false keeps wireless_headset slot",
            result_argument_behavior="checkSlot('wireless_headset') hides the slot when enabled",
            api33_variant_reason="A13 hide-icons path already owned headset; wireless_headset is an extra slot name on the same function.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_PACKAGEINSTALLER_PURIFY",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt",
            a14_symbol="PurePackageInstallerHook",
            a14_installer="mods/utils/feature/PackageInstallerFeatures.kt",
            a14_hook_targets="MIUI package installer preference/settings members",
            a14_callback_phase="before/after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt",
            a13_symbol="PurePackageInstallerHook",
            a13_installer="installers/PackageInstallerRouter.java",
            a13_hook_targets="MIUI package installer preference/settings members",
            a13_callback_phase="before/after",
            preference_keys=("various_installer_purify",),
            value_domain="boolean",
            default_semantics="false keeps installer ads/recommend/verify UI",
            result_argument_behavior="purifiedInstallerBoolean/SystemInt/SecureInt rewrite installer prefs",
            api33_variant_reason="Same Various.PurePackageInstallerHook; A13 router vs A14 FeatureSpec.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_HIDE_REPORT",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt",
            a14_symbol="HideReportButtonHook",
            a14_installer="mods/utils/feature/SecurityCenterFeatures.kt",
            a14_hook_targets="com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu",
            a14_callback_phase="after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt",
            a13_symbol="HideReportButtonHook",
            a13_installer="installers/SecurityCenterInstaller.java",
            a13_hook_targets="com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu",
            a13_callback_phase="after",
            preference_keys=("various_hide_report_ondetails",),
            value_domain="boolean",
            default_semantics="false keeps report menu item",
            result_argument_behavior="after onCreateOptionsMenu, itemId 4 setVisible(false)",
            api33_variant_reason="Same ApplicationsDetailsActivity menu owner; A13 SecurityCenter installer.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_NETSPEED_BOLDFONT_RENAME",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a14_symbol="NetSpeedTypefaceHelper",
            a14_installer="A14 SystemUiFeatures / netspeed style",
            a14_hook_targets="network speed text typeface path",
            a14_callback_phase="after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
            a13_symbol="NetSpeedTypefaceHelper",
            a13_installer="installers/SystemUiInstaller.java",
            a13_hook_targets="network speed text typeface path",
            a13_callback_phase="after",
            preference_keys=("system_netspeed_boldfont", "system_netspeed_bold"),
            value_domain="boolean bold typeface",
            default_semantics="false = stock weight",
            result_argument_behavior="A13 key system_netspeed_bold drives the same typeface helper as A14 boldfont",
            api33_variant_reason="Capability-preserving key rename; not a second netspeed product.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
        ProofManifest(
            proof_id="PROOF_BT_ICON_ALWAYS_HIDE",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt",
            a14_symbol="HideIconsBluetoothHook",
            a14_installer="A14 SystemUi features",
            a14_hook_targets="bluetooth status-bar icon visibility",
            a14_callback_phase="before",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt",
            a13_symbol="HideIconsBluetoothHook",
            a13_installer="installers/SystemUiInstaller.java",
            a13_hook_targets="bluetooth status-bar icon visibility",
            a13_callback_phase="before",
            preference_keys=("system_statusbaricons_bluetoothicn", "system_statusbaricons_bluetooth"),
            value_domain="A14 boolean vs A13 list option 3 = always hide",
            default_semantics="stock bluetooth icon visible",
            result_argument_behavior="A13 option 3 forces bluetooth icon visibility false",
            api33_variant_reason="A13 already exposes always-hide as bluetooth=3; A14 split a dedicated key.",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        ),
    ]
    return [_with_phase_e_reviewed_fields(man) for man in raw]


def proof_index(manifests: list[ProofManifest]) -> dict[str, ProofManifest]:
    out: dict[str, ProofManifest] = {}
    for man in manifests:
        for key in man.preference_keys:
            out[key] = man
    return out


def search_a14_implementation(key: str, index: dict[str, str]) -> list[str]:
    hits: list[str] = []
    needles = (key, f"pref_key_{key}")
    for path, text in index.items():
        low = text
        if any(n in low for n in needles):
            hits.append(path)
            if len(hits) >= 20:
                break
    return hits


def is_production_impl_hit(path: str) -> bool:
    p = path.replace("\\", "/").lower()
    if "/test/" in p or "/androidtest/" in p:
        return False
    if p.endswith(".xml") and ("/res/xml/" in p or "/res/values" in p):
        return False
    if "/res/values" in p:
        return False
    return p.endswith(".kt") or p.endswith(".java")


def prove_dead_a14_key(
    key: str,
    ui_file: str,
    a14_index: dict[str, str],
    a14_specs: set[str],
    a14_owners: dict[str, list[SourceOwner]],
    nearest: str = "",
) -> DeadPathProof | None:
    if key in a14_specs:
        return None
    if a14_owners.get(key):
        return None
    hits = search_a14_implementation(key, a14_index)
    impl_hits = [h for h in hits if is_production_impl_hit(h)]
    # Contract/backup ignore lists are not feature owners.
    impl_hits = [
        h for h in impl_hits
        if "CurrentPreferenceContract" not in h
        and "BackupRestore" not in h
        and "LegacyMigration" not in h
    ]
    if impl_hits:
        return None
    search_ref = ", ".join(hits[:8]) if hits else "(xml/strings only)"
    return DeadPathProof(
        key=key,
        a14_ui_reference=ui_file,
        a14_search_references=search_ref,
        a14_nearest_candidate=nearest or "none",
        why_not_reachable=(
            f"UI/schema key `{key}` exists in {ui_file}; no FeatureSpec, no installer/action "
            f"callback, no Java/Kotlin owner, no alias/wrapper/custom Preference consumer at pinned A14."
        ),
    )


def build_source_index(repo: Path) -> dict[str, str]:
    index: dict[str, str] = {}
    roots = [
        repo / "app/src/main/java",
        repo / "app/src/main/res/xml",
        repo / "app/src/main/res/values",
        repo / "app/src/test/java",
    ]
    for root in roots:
        if not root.exists():
            continue
        for src in root.rglob("*"):
            if src.suffix.lower() not in {".kt", ".java", ".xml"}:
                continue
            try:
                rel = str(src.relative_to(repo)).replace("\\", "/")
            except ValueError:
                rel = str(src).replace("\\", "/")
            index[rel] = src.read_text(encoding="utf-8", errors="ignore")
    return index


def format_proof_markdown(manifests: list[ProofManifest]) -> str:
    lines = [
        "# A13 Phase F-R5 Semantic Proofs",
        "",
        "Automatic PRESENT requires normalized body IDENTICAL, the same relevant preference keys,",
        "and compatible installer ownership (BODY_RELATION=IDENTICAL).",
        "Non-identical owners require an explicit reviewed manifest (BODY_RELATION=REVIEWED_VARIANT)",
        "with filled difference fields and KEY_OWNERSHIP_EVIDENCE.",
        "Prefix, ranked-first, same-XML, and same-basename-alone never assign semantic ownership.",
        "SequenceMatcher ratio never authorizes PRESENT.",
        "Same-key reads or a visible row in both XML files alone are IMPLEMENTATION_PRESENCE.",
        "",
    ]
    for man in manifests:
        lines.append(f"## {man.proof_id}")
        lines.append("")
        lines.append(f"- PROOF_ID: `{man.proof_id}`")
        lines.append(f"- BODY_RELATION: `{man.body_relation or 'UNSPECIFIED'}`")
        lines.append(f"- A14_OWNER_PATH: `{man.a14_owner_path}`")
        lines.append(f"- A14_SYMBOL: `{man.a14_symbol}`")
        lines.append(f"- A14_INSTALLER: `{man.a14_installer}`")
        lines.append(f"- A14_HOOK_TARGETS: `{man.a14_hook_targets}`")
        lines.append(f"- A14_CALLBACK_PHASE: `{man.a14_callback_phase}`")
        lines.append(f"- A13_OWNER_PATH: `{man.a13_owner_path}`")
        lines.append(f"- A13_SYMBOL: `{man.a13_symbol}`")
        lines.append(f"- A13_INSTALLER: `{man.a13_installer}`")
        lines.append(f"- A13_HOOK_TARGETS: `{man.a13_hook_targets}`")
        lines.append(f"- A13_CALLBACK_PHASE: `{man.a13_callback_phase}`")
        lines.append(f"- PREFERENCE_KEYS: `{','.join(man.preference_keys)}`")
        lines.append(f"- VALUE_DOMAIN: {man.value_domain}")
        lines.append(f"- DEFAULT_SEMANTICS: {man.default_semantics}")
        lines.append(f"- RESULT/ARGUMENT_BEHAVIOR: {man.result_argument_behavior}")
        lines.append(f"- API33_VARIANT_REASON: {man.api33_variant_reason}")
        if man.body_relation == "REVIEWED_VARIANT":
            lines.append(f"- DIFF_SUMMARY: {man.diff_summary}")
            lines.append(f"- VALUE_DEFAULT_COMPARISON: {man.value_default_comparison}")
            lines.append(f"- HOOK_TARGET_COMPARISON: {man.hook_target_comparison}")
            lines.append(f"- CALLBACK_SEMANTICS_COMPARISON: {man.callback_semantics_comparison}")
            lines.append(f"- ARG_RESULT_COMPARISON: {man.arg_result_comparison}")
            lines.append(f"- A14_ONLY_BRANCHES: {man.a14_only_branches}")
            lines.append(f"- WHY_USER_BEHAVIOR_IS_EQUIVALENT: {man.why_user_behavior_is_equivalent}")
        if man.key_ownership_evidence:
            lines.append(f"- KEY_OWNERSHIP_EVIDENCE: {man.key_ownership_evidence}")
        if man.a14_key_owner_reference:
            lines.append(f"- A14_KEY_OWNER_REFERENCE: {man.a14_key_owner_reference}")
        if man.a13_key_owner_reference:
            lines.append(f"- A13_KEY_OWNER_REFERENCE: {man.a13_key_owner_reference}")
        lines.append(f"- PROOF_CONCLUSION: `{man.proof_conclusion}`")
        lines.append("")
    return "\n".join(lines)
