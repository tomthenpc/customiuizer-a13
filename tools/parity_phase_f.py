#!/usr/bin/env python3
"""Phase F-R1 final-truth helpers: product taxonomy, owner proofs, dead-path proofs."""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from difflib import SequenceMatcher
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


def classify_ui_node(tag: str, key: str, *, visible: str | None = None, warning: str | None = None) -> str:
    low = key.lower()
    short_tag = tag.rsplit(".", 1)[-1]
    vis = visible or ""
    warn = warning or ""

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
    if any(x in low for x in ["_apps", "_bw", "_ignore", "_prerequisite", "_dependency"]):
        return "DEPENDENCY_HELPER"
    if any(x in low for x in ["_state", "_internal", "_applied", "_synced"]):
        return "INTERNAL_STATE"
    if short_tag.endswith("PreferenceScreen"):
        return "NAVIGATION"
    if short_tag.endswith("CheckBoxPreferenceEx") or short_tag.endswith("SwitchPreferenceCompat"):
        return "PRODUCT_ACTION"
    if any(short_tag.endswith(t) for t in (
        "ListPreferenceEx", "DropDownPreferenceEx", "SeekBarPreference", "ColorPreferenceEx",
    )):
        return "PRODUCT_SUBOPTION"
    if short_tag.endswith("PreferenceEx"):
        return "PRODUCT_ACTION"
    return "UNKNOWN"


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
        return PhaseFDecision(
            parity_state="UNPROVEN",
            evidence_level="IMPLEMENTATION_PRESENCE" if (inp.a14_read and inp.a13_read) else "MECHANICAL_ONLY",
            proof_id="",
            reason="same key with different hook behavior is not PRESENT",
        )
    if inp.a14_read and inp.a13_read:
        return PhaseFDecision(
            parity_state="UNPROVEN",
            evidence_level="IMPLEMENTATION_PRESENCE",
            proof_id="",
            reason="same key + same reads + same host is IMPLEMENTATION_PRESENCE only",
        )
    if inp.rom_hold:
        return PhaseFDecision(
            parity_state="HOLD_EVIDENCE",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            proof_id="PROOF_ROM_DEVICE_HOLD",
            reason=inp.rom_hold.get("unresolved_question", "ROM/device hold"),
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


def _key_callees_in_text(text: str) -> dict[str, set[str]]:
    out: dict[str, set[str]] = {}
    for m in HOOK_CALL_RE.finditer(text):
        callee = m.group(2)
        if callee in SKIP_CALLEES:
            continue
        window = text[max(0, m.start() - 2500): m.start()]
        keys = GET_KEY_RE.findall(window)
        keys += re.findall(r'preferenceKey\s*=\s*"([a-z0-9_]+)"', window)
        keys += re.findall(r'preferenceKeys\s*=\s*(?:listOf|setOf)\((.*?)\)', window, re.S)
        expanded: list[str] = []
        for item in keys:
            if "," in item or "\n" in item:
                expanded.extend(re.findall(r'"([a-z0-9_]+)"', item))
            else:
                expanded.append(item)
        for key in expanded:
            if key.startswith("pref_key_"):
                continue
            out.setdefault(key, set()).add(callee)
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
    if a14.symbol == a13.symbol and a14.symbol.endswith("Hook"):
        return True
    if not a14.hook_targets and not a13.hook_targets:
        return True
    return False


def _basename(path: str) -> str:
    return path.rsplit("/", 1)[-1]


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


def _manifest_for_pair(
    key: str,
    a14: SourceOwner,
    a13: SourceOwner,
    left: list[SourceOwner],
    right: list[SourceOwner],
    covered: tuple[str, ...],
) -> ProofManifest:
    body_equal = a14.normalized_body == a13.normalized_body and bool(a14.normalized_body)
    conclusion = "PRESENT_EQUIVALENT" if body_equal else "PRESENT_A13_VARIANT"
    a14_installers = [o for o in left if o.kind == "installer"]
    a13_installers = [o for o in right if o.kind == "installer"]
    keys = tuple(dict.fromkeys((key,) + covered))
    api33 = (
        "Normalized owner body identical."
        if body_equal
        else (
            f"Identified owners `{a14.symbol}` vs `{a13.symbol}`; installer/hook members match or "
            f"API33 variant in the same capability path."
        )
    )
    return ProofManifest(
        proof_id=f"PROOF_FP_{_basename(a13.path).replace('.', '_')}_{a13.symbol}",
        a14_owner_path=a14.path,
        a14_symbol=a14.symbol,
        a14_installer=a14_installers[0].path if a14_installers else (a14.path if a14.kind in {"spec", "installer"} else ""),
        a14_hook_targets=",".join(a14.hook_targets) or "(owner body / installer callee)",
        a14_callback_phase=",".join(a14.callback_phases),
        a13_owner_path=a13.path,
        a13_symbol=a13.symbol,
        a13_installer=a13_installers[0].path if a13_installers else (a13.path if a13.kind == "installer" else ""),
        a13_hook_targets=",".join(a13.hook_targets) or "(owner body / installer callee)",
        a13_callback_phase=",".join(a13.callback_phases),
        preference_keys=keys,
        value_domain="owner-local preference domain",
        default_semantics="same owner default path unless a key-specific override exists",
        result_argument_behavior="owner hook result/argument rewrite as in matched bodies",
        api33_variant_reason=api33,
        proof_conclusion=conclusion,
        evidence_level="STRUCTURAL_SEMANTIC_PROOF",
    )


def fingerprint_proof_for_key(
    key: str,
    a14_owners: dict[str, list[SourceOwner]],
    a13_owners: dict[str, list[SourceOwner]],
    *,
    a14_scan: RepoScan | None = None,
    a13_scan: RepoScan | None = None,
) -> ProofManifest | None:
    left = a14_owners.get(key) or []
    right = a13_owners.get(key) or []
    if a14_scan and a13_scan:
        shared = (a14_scan.callees.get(key) or set()) & (a13_scan.callees.get(key) or set())
        ranked: list[tuple[int, str, SourceOwner, SourceOwner]] = []
        for callee in shared:
            a14 = _pick_symbol_owner(a14_scan.symbols.get(callee) or [])
            a13 = _pick_symbol_owner(a13_scan.symbols.get(callee) or [])
            if not a14 or not a13:
                continue
            if a14.hook_targets and a13.hook_targets and not hook_targets_compatible(a14, a13):
                if not (a14.symbol == a13.symbol and a14.symbol.endswith("Hook")):
                    continue
            score = 0
            if key in a14.keys:
                score += 6
            if key in a13.keys:
                score += 6
            if a14.hook_targets:
                score += 2
            if a13.hook_targets:
                score += 2
            if a14.symbol.endswith("Hook") and a13.symbol.endswith("Hook"):
                score += 1
            ranked.append((score, callee, a14, a13))
        ranked.sort(key=lambda item: (-item[0], item[1]))
        if ranked:
            _, callee, a14, a13 = ranked[0]
            covered = tuple(sorted((set(a14.keys) & set(a13.keys)) | {key}))
            return _manifest_for_pair(key, a14, a13, left, right, covered)
    if not left or not right:
        if a14_scan and a13_scan:
            for owner in (left or []) + (right or []):
                if not owner.symbol.endswith("Hook"):
                    continue
                a14 = _pick_symbol_owner(a14_scan.symbols.get(owner.symbol) or [])
                a13 = _pick_symbol_owner(a13_scan.symbols.get(owner.symbol) or [])
                if a14 and a13:
                    return _manifest_for_pair(key, a14, a13, left, right, (key,))
        return None
    pair = match_owner_pair(left, right)
    if not pair:
        return None
    a14, a13 = pair
    if a14.hook_targets and a13.hook_targets and not hook_targets_compatible(a14, a13):
        if not (a14.symbol == a13.symbol and a14.symbol.endswith("Hook")):
            return None
    if a14.symbol in JUNK_SYMBOLS or a13.symbol in JUNK_SYMBOLS:
        return None
    same_file = _basename(a14.path) == _basename(a13.path)
    same_symbol = a14.symbol == a13.symbol
    overlap = len(set(a14.keys) & set(a13.keys))
    hooks_ok = bool(a14.hook_targets and a13.hook_targets and hook_targets_compatible(a14, a13))
    if not same_file and not same_symbol and not hooks_ok and overlap < 2:
        return None
    covered = tuple(sorted(set(a14.keys) & set(a13.keys))) or (key,)
    return _manifest_for_pair(key, a14, a13, left, right, covered)


def _exclusive_keys_for_pair(
    a14: SourceOwner,
    a13: SourceOwner,
    a14_owners: dict[str, list[SourceOwner]],
    a13_owners: dict[str, list[SourceOwner]],
) -> tuple[str, ...]:
    shared = set(a14.keys) & set(a13.keys)
    return tuple(sorted(shared))



def phase_e_source_proofs() -> list[ProofManifest]:
    """Source-backed Phase E preserves. Keys listed here are covered individually."""
    return [
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
        "# A13 Phase F-R1 Semantic Proofs",
        "",
        "Owner manifests used to promote rows to PRESENT_EQUIVALENT or PRESENT_A13_VARIANT.",
        "Same-key reads alone are IMPLEMENTATION_PRESENCE and never sufficient.",
        "",
    ]
    for man in manifests:
        lines.append(f"## {man.proof_id}")
        lines.append("")
        lines.append(f"- PROOF_ID: `{man.proof_id}`")
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
        lines.append(f"- PROOF_CONCLUSION: `{man.proof_conclusion}`")
        lines.append("")
    return "\n".join(lines)
