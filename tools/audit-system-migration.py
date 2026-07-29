#!/usr/bin/env python3
"""Reproducible audit for the K5 System.java -> Kotlin migration.

Run from the repository root:
    python tools/audit-system-migration.py

Exit code:
    0 if the migration passes the audit
    non-zero if facade mapping is incomplete or implementation logic is found
"""
import re
import os
import sys
import hashlib
import subprocess
from pathlib import Path
from datetime import datetime, timezone
from collections import defaultdict


def get_repo_root() -> Path:
    script = Path(__file__).resolve()
    return script.parent.parent


REPO = get_repo_root()

MAIN_MODULE = REPO / "app/src/main/java/name/monwf/customiuizer/MainModule.java"
SYSTEM_FACADE = REPO / "app/src/main/java/name/monwf/customiuizer/mods/System.kt"
MODS_DIR = REPO / "app/src/main/java/name/monwf/customiuizer/mods"
BUILD_DIR = REPO / "app/build"
MAPPING_DIR = BUILD_DIR / "outputs/mapping"
APK_DIR = BUILD_DIR / "outputs/apk"


def latest_file(parent: Path, pattern: str) -> Path | None:
    if not parent.exists():
        return None
    files = sorted(parent.rglob(pattern), key=lambda p: p.stat().st_mtime, reverse=True)
    return files[0] if files else None


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


# ---------------------------------------------------------------------------
# MainModule System.* call extraction
# ---------------------------------------------------------------------------
MAIN_MODULE_CALL_RE = re.compile(r"(?<![A-Za-z0-9_.])System\.([A-Za-z0-9_]+)\s*\(")


def extract_mainmodule_calls(text: str) -> list[str]:
    calls = []
    for line in text.splitlines():
        # strip comments roughly
        if "//" in line:
            line = line.split("//", 1)[0]
        calls.extend(MAIN_MODULE_CALL_RE.findall(line))
    return calls


# ---------------------------------------------------------------------------
# System.kt facade parsing
# ---------------------------------------------------------------------------
FACADE_METHOD_RE = re.compile(
    r"""
    @JvmStatic\s+
    fun\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*(?::\s*([^\n{=]+))?\s*\{
    """,
    re.VERBOSE | re.DOTALL,
)


def extract_facade_methods(text: str) -> list[dict]:
    methods = []
    for m in FACADE_METHOD_RE.finditer(text):
        name, args_raw, ret = m.groups()
        # find the matching closing brace for this method
        start = m.end()
        brace = 1
        i = start
        while i < len(text) and brace > 0:
            if text[i] == "{":
                brace += 1
            elif text[i] == "}":
                brace -= 1
            i += 1
        body = text[start : i - 1]
        methods.append({
            "name": name,
            "args_raw": (args_raw or "").strip(),
            "return": (ret or "Unit").strip(),
            "body": body.strip(),
            "start": m.start(),
            "end": i,
        })
    return methods


def parse_simple_type(t: str) -> str:
    t = t.strip().split("=")[0].strip()
    if t.endswith("?"):
        t = t[:-1]
    return t.split(".")[-1]


def parse_facade_args(args_raw: str) -> list[tuple[str, str]]:
    if not args_raw:
        return []
    parts = [p.strip() for p in args_raw.split(",") if p.strip()]
    result = []
    for p in parts:
        if ":" in p:
            name, type_ = p.split(":", 1)
            result.append((parse_simple_type(type_), name.strip()))
        else:
            tokens = p.split()
            if len(tokens) >= 2:
                result.append((parse_simple_type(" ".join(tokens[1:])), tokens[0]))
    return result


def is_pure_delegation(method: dict) -> tuple[bool, str, str]:
    """Return (is_delegation, target_object, target_method)."""
    body = re.sub(r"//.*", "", method["body"])
    body = body.replace("\n", " ")
    body = re.sub(r"\s+", " ", body).strip()

    # Unit return: Object.method(args)
    unit_match = re.match(r"^([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\((.*)\)$", body)
    if unit_match:
        return True, unit_match.group(1), unit_match.group(2)

    # Non-unit: return Object.method(args)
    ret_match = re.match(r"^return\s+([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\((.*)\)$", body)
    if ret_match:
        return True, ret_match.group(1), ret_match.group(2)

    return False, "", ""


# ---------------------------------------------------------------------------
# System*Hooks.kt parsing
# ---------------------------------------------------------------------------
OBJECT_RE = re.compile(r"^object\s+([A-Za-z0-9_]+)\s*\{", re.MULTILINE)
HOOK_METHOD_RE = re.compile(
    r"^\s*(?:@[A-Za-z0-9_.]+(?:\([^)]*\))?\s+)*fun\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)",
    re.MULTILINE,
)


def extract_hook_methods(text: str, filename: str) -> list[dict]:
    obj_match = OBJECT_RE.search(text)
    object_name = obj_match.group(1) if obj_match else filename.replace(".kt", "")
    methods = []
    for m in HOOK_METHOD_RE.finditer(text):
        name, args_raw = m.groups()
        methods.append({
            "object": object_name,
            "name": name,
            "args": parse_facade_args(args_raw or ""),
            "args_raw": (args_raw or "").strip(),
            "filename": filename,
        })
    return methods


# ---------------------------------------------------------------------------
# R8 mapping.txt / usage.txt parsing
# ---------------------------------------------------------------------------
def parse_mapping(path: Path) -> dict:
    """Return mapping info as:
    {
        class: {orig_class: (obf_class, methods:[...], fields:[...])},
        method: {(orig_class, method_name, args): obf_name, ...},
    }
    """
    data = {"class": {}, "method": {}}
    current = None
    with path.open("r", encoding="utf-8") as f:
        for raw in f:
            line = raw.rstrip("\n")
            if not line:
                current = None
                continue
            # class line: a.b.C -> d.e:
            class_m = re.match(r"^([^\s:]+(?:\.[^\s:]+)*)\s*->\s*([^\s:]+):\s*$", line)
            if class_m:
                orig, obf = class_m.group(1), class_m.group(2)
                current = orig
                data["class"][orig] = {"obf": obf, "methods": [], "fields": []}
                continue
            if current is None:
                continue
            # method line:    [line:endline:]ret class.method(args):orig_line -> obf
            method_m = re.match(
                r"^\s*(?:\d+:\d+:)?([^\s]+)\s+([^\s]+)\.([A-Za-z0-9_]+)\(([^)]*)\)(?::\d+)?\s*->\s*([^\s]+)$",
                line,
            )
            if method_m:
                ret, cls, name, args, obf = method_m.groups()
                data["class"][current]["methods"].append({
                    "ret": ret,
                    "orig": f"{cls}.{name}({args})",
                    "obf": obf,
                })
                data["method"][(cls, name, args)] = obf
                continue
            # field line:    type class.field -> obf
            field_m = re.match(r"^\s*([^\s]+)\s+([^\s]+)\.([A-Za-z0-9_]+)\s*->\s*([^\s]+)$", line)
            if field_m:
                data["class"][current]["fields"].append({
                    "type": field_m.group(1),
                    "orig": f"{field_m.group(2)}.{field_m.group(3)}",
                    "obf": field_m.group(4),
                })
    return data


def parse_usage(path: Path) -> dict:
    """Return usage report as:
    {
        class: {
            orig_class: {
                "status": "full" | "members",
                "members": [str, ...]
            }
        }
    }
    """
    data = defaultdict(lambda: {"status": None, "members": []})
    current = None
    with path.open("r", encoding="utf-8") as f:
        for raw in f:
            line = raw.rstrip("\n")
            if not line:
                current = None
                continue
            # class header is a line ending with ':' or just a class name with no indent
            top = re.match(r"^([^\s]+):\s*$", line)
            plain = re.match(r"^([^\s]+)$", line)
            if top:
                current = top.group(1)
                data[current]["status"] = "members"
                continue
            if plain:
                current = plain.group(1)
                data[current]["status"] = "full"
                continue
            if current:
                # indented member
                data[current]["members"].append(line.strip())
    return dict(data)


# ---------------------------------------------------------------------------
# APK inspection helpers
# ---------------------------------------------------------------------------
def apkanalyzer_path() -> Path | None:
    envs = ["ANDROID_HOME", "ANDROID_SDK_ROOT"]
    locations = [
        "cmdline-tools/latest/bin/apkanalyzer.bat",
        "tools/bin/apkanalyzer.bat",
        "cmdline-tools/latest/bin/apkanalyzer",
        "tools/bin/apkanalyzer",
    ]
    for env in envs:
        value = os.environ.get(env)
        if not value:
            continue
        root = Path(value)
        if not root.exists():
            continue
        for loc in locations:
            p = root / loc
            if p.exists():
                return p
    # Fallback to a typical Windows SDK location
    root = Path("C:/Android/Sdk")
    if root.exists():
        for loc in locations:
            p = root / loc
            if p.exists():
                return p
    return None


def find_apks() -> tuple[Path | None, Path | None]:
    debug = latest_file(APK_DIR / "debug", "*.apk")
    release = latest_file(APK_DIR / "release", "*.apk")
    return debug, release


def run_apkanalyzer_dex_packages(apk: Path, analyzer: Path) -> str:
    try:
        result = subprocess.run(
            [str(analyzer), "dex", "packages", str(apk), "--defined-only"],
            capture_output=True,
            text=True,
            timeout=120,
        )
        return result.stdout + result.stderr
    except Exception as e:
        return f"apkanalyzer error: {e}"


def run_apkanalyzer_files(apk: Path, analyzer: Path) -> str:
    try:
        result = subprocess.run(
            [str(analyzer), "files", "list", str(apk)],
            capture_output=True,
            text=True,
            timeout=60,
        )
        return result.stdout + result.stderr
    except Exception as e:
        return f"apkanalyzer error: {e}"


# ---------------------------------------------------------------------------
# Report generation
# ---------------------------------------------------------------------------
class Reporter:
    def __init__(self):
        self.lines = []
        self.failures = []

    def add(self, line: str):
        self.lines.append(line)

    def fail(self, line: str):
        self.lines.append(f"FAIL: {line}")
        self.failures.append(line)

    def warn(self, line: str):
        self.lines.append(f"WARN: {line}")

    def text(self) -> str:
        return "\n".join(self.lines)

    def ok(self) -> bool:
        return not self.failures


def format_timestamp(path: Path | None) -> str:
    if not path or not path.exists():
        return "n/a"
    mtime = path.stat().st_mtime
    dt = datetime.fromtimestamp(mtime, tz=timezone.utc)
    return dt.strftime("%Y-%m-%d %H:%M:%S UTC")


def main():
    r = Reporter()

    r.add("# K5 System.java -> Kotlin migration audit")
    r.add("")
    r.add(f"Repo root: {REPO}")
    r.add(f"Audit time: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}")
    r.add("")

    # --- 1. MainModule calls ---
    if not MAIN_MODULE.exists():
        r.fail(f"MainModule not found: {MAIN_MODULE}")
        return r

    main_text = MAIN_MODULE.read_text(encoding="utf-8")
    main_calls = extract_mainmodule_calls(main_text)
    unique_calls = sorted(set(main_calls))
    r.add("## MainModule System.* calls")
    r.add(f"- Total call sites: {len(main_calls)}")
    r.add(f"- Unique methods called: {len(unique_calls)}")
    for name in unique_calls:
        count = main_calls.count(name)
        r.add(f"  - {name}: {count}")
    r.add("")

    # --- 2. Facade methods ---
    if not SYSTEM_FACADE.exists():
        r.fail(f"System.kt facade not found: {SYSTEM_FACADE}")
        return r

    facade_text = SYSTEM_FACADE.read_text(encoding="utf-8")
    facade_methods = extract_facade_methods(facade_text)
    r.add("## System.kt facade methods")
    r.add(f"- Public @JvmStatic methods: {len(facade_methods)}")

    facade_by_name = {m["name"]: m for m in facade_methods}
    non_delegation = []
    for m in facade_methods:
        is_deleg, target_obj, target_method = is_pure_delegation(m)
        if not is_deleg:
            non_delegation.append(m["name"])
        else:
            m["target_object"] = target_obj
            m["target_method"] = target_method

    if non_delegation:
        r.fail(f"Facade contains implementation logic in: {', '.join(non_delegation)}")
    else:
        r.add("- All facade methods are pure delegations")

    # Duplicate facade method names (overloads)
    facade_name_counts = defaultdict(int)
    for m in facade_methods:
        facade_name_counts[m["name"]] += 1
    dups = [name for name, c in facade_name_counts.items() if c > 1]
    if dups:
        r.warn(f"Facade has overloaded method names (Java interop risk): {', '.join(dups)}")
    r.add("")

    # --- 3. MainModule calls vs facade ---
    r.add("## MainModule call -> facade coverage")
    missing_in_facade = [c for c in unique_calls if c not in facade_by_name]
    if missing_in_facade:
        r.fail(f"MainModule calls missing from facade: {', '.join(missing_in_facade)}")
    else:
        r.add(f"- All {len(unique_calls)} unique MainModule calls are covered by the facade")
    r.add("")

    # --- 4. Hooks files ---
    hooks_files = sorted(MODS_DIR.glob("System*Hooks.kt"))
    r.add("## System*Hooks files")
    r.add(f"- Files found: {len(hooks_files)}")
    all_hook_methods: list[dict] = []
    for hf in hooks_files:
        text = hf.read_text(encoding="utf-8")
        methods = extract_hook_methods(text, hf.name)
        all_hook_methods.extend(methods)
        r.add(f"  - {hf.name}: {len(methods)} methods")
    r.add("")

    hook_target_map: dict[tuple[str, str, tuple], dict] = {}
    for hm in all_hook_methods:
        key = (hm["object"], hm["name"], tuple(t for t, _ in hm["args"]))
        hook_target_map[key] = hm

    # --- 5. Facade target -> Hooks coverage ---
    r.add("## Facade delegation -> Hooks target coverage")
    missing_targets = []
    for m in facade_methods:
        if "target_object" not in m:
            continue
        target_obj = m["target_object"]
        target_method = m["target_method"]
        target_fqcn = f"name.monwf.customiuizer.mods.{target_obj}"
        target_key = (target_obj, target_method, tuple(t for t, _ in parse_facade_args(m["args_raw"])))
        if target_key not in hook_target_map:
            missing_targets.append(f"{target_fqcn}.{target_method} (called from System.{m['name']})")

    if missing_targets:
        r.fail(f"Facade targets missing in System*Hooks: {', '.join(missing_targets[:20])}")
        if len(missing_targets) > 20:
            r.add(f"  ... and {len(missing_targets) - 20} more")
    else:
        r.add(f"- All {len(facade_methods)} facade delegations resolve to a System*Hooks method")
    r.add("")

    # --- 6. Duplicate methods across Hooks files ---
    hook_key_counts = defaultdict(list)
    for hm in all_hook_methods:
        key = (hm["name"], tuple(t for t, _ in hm["args"]))
        hook_key_counts[key].append(hm)
    duplicates = {k: v for k, v in hook_key_counts.items() if len(v) > 1}
    r.add("## Duplicate hook method signatures")
    if duplicates:
        for (name, args), methods in sorted(duplicates.items()):
            files = [m["filename"] for m in methods]
            r.warn(f"Duplicate: {name} in {', '.join(files)}")
    else:
        r.add("- No duplicate method signatures across System*Hooks files")
    r.add("")

    # --- 7. R8 mapping / usage ---
    r.add("## R8 mapping and usage audit")
    usage_path = latest_file(MAPPING_DIR, "usage.txt")
    mapping_path = latest_file(MAPPING_DIR, "mapping.txt")
    if not usage_path or not mapping_path:
        r.warn(f"R8 mapping files not found under {MAPPING_DIR}")
    else:
        r.add(f"- usage.txt: {usage_path}")
        r.add(f"- mapping.txt: {mapping_path}")

        usage_data = parse_usage(usage_path)
        mapping_data = parse_mapping(mapping_path)

        # System class in usage/mapping
        system_in_usage = usage_data.get("name.monwf.customiuizer.mods.System")
        system_in_mapping = mapping_data["class"].get("name.monwf.customiuizer.mods.System")

        r.add("")
        r.add("### `name.monwf.customiuizer.mods.System` (facade)")
        if system_in_usage:
            r.add("- Present in usage.txt (removed by R8 shrinking)")
            if system_in_usage["status"] == "full":
                r.add("  - Class itself removed")
            else:
                r.add(f"  - Members listed as removed: {len(system_in_usage['members'])}")
                for mem in system_in_usage["members"]:
                    r.add(f"    - {mem}")
        else:
            r.add("- Not present in usage.txt (not reported as removed)")

        if system_in_mapping:
            r.add(f"- Present in mapping.txt: {system_in_mapping['obf']}")
            r.add(f"  - Mapped methods: {len(system_in_mapping['methods'])}")
            for mm in system_in_mapping["methods"][:10]:
                r.add(f"    - {mm['orig']} -> {mm['obf']}")
            if len(system_in_mapping["methods"]) > 10:
                r.add(f"    ... and {len(system_in_mapping['methods']) - 10} more")
        else:
            r.add("- Not found in mapping.txt class headers (may have been removed or inlined)")

        # System*Hooks classes
        r.add("")
        r.add("### System*Hooks classes (18 domain objects)")
        removed_hooks = []
        kept_hooks = []
        for hf in hooks_files:
            obj_name = hf.name.replace(".kt", "")
            fqcn = f"name.monwf.customiuizer.mods.{obj_name}"
            in_usage = fqcn in usage_data
            in_mapping = fqcn in mapping_data["class"]
            if in_usage and not in_mapping:
                removed_hooks.append(obj_name)
            elif in_mapping:
                kept_hooks.append((obj_name, mapping_data["class"][fqcn]["obf"]))
            else:
                # neither - common for inlined/merged small classes
                kept_hooks.append((obj_name, "not in mapping headers"))

        if removed_hooks:
            r.add(f"- Classes reported as removed in usage.txt: {len(removed_hooks)}")
            for h in removed_hooks:
                r.add(f"  - {h}")
            r.add("  (Removal of hook-only classes is acceptable if their methods were inlined into the System facade by R8.)")

        r.add(f"- Classes with mapping headers: {len([h for h in kept_hooks if h[1] != 'not in mapping headers'])}")
        for h, obf in kept_hooks:
            if obf != "not in mapping headers":
                r.add(f"  - {h} -> {obf}")

        # Cross-check: are any facade targets removed in usage/missing in mapping?
        r.add("")
        r.add("### Facade target method reachability")
        for m in facade_methods:
            if "target_object" not in m:
                continue
            target_obj = m["target_object"]
            target_method = m["target_method"]
            target_fqcn = f"name.monwf.customiuizer.mods.{target_obj}"
            target_key = (target_fqcn, target_method)
            if target_fqcn in usage_data and target_fqcn not in mapping_data["class"]:
                # target class removed; method likely inlined into facade
                r.warn(
                    f"System.{m['name']} -> {target_obj}.{target_method}: target class removed; "
                    "confirm via APK/Dex that the call chain still resolves."
                )

    # --- 8. APK info ---
    r.add("")
    r.add("## APK artifacts")
    debug_apk, release_apk = find_apks()
    for label, apk in [("Debug", debug_apk), ("Release", release_apk)]:
        r.add(f"### {label} APK")
        if not apk:
            r.fail(f"{label} APK not found")
            continue
        r.add(f"- Path: {apk}")
        r.add(f"- Size: {apk.stat().st_size} bytes")
        r.add(f"- SHA-256: {sha256(apk)}")
        r.add(f"- Last write: {format_timestamp(apk)}")

        analyzer = apkanalyzer_path()
        if analyzer:
            r.add(f"- apkanalyzer: {analyzer}")
            # list dex classes for the mods package
            dex_out = run_apkanalyzer_dex_packages(apk, analyzer)
            lines = dex_out.splitlines()
            relevant = [ln for ln in lines if ("name.monwf.customiuizer.mods" in ln or "name.monwf.customiuizer.MainModule" in ln)]
            if relevant:
                r.add(f"- APK dex packages containing `name/monwf/customiuizer/mods`: {len(relevant)} entries")
                for ln in relevant[:30]:
                    r.add(f"    {ln}")
                if len(relevant) > 30:
                    r.add(f"    ... and {len(relevant) - 30} more")
            else:
                r.add("- apkanalyzer output (first 30 lines):")
                for ln in lines[:30]:
                    r.add(f"    {ln}")
        else:
            r.add("- apkanalyzer not found; install Android SDK cmdline-tools for APK Dex inspection.")

    # --- 9. Summary ---
    r.add("")
    r.add("## Summary")
    if r.ok():
        r.add("PASS: Migration audit completed with no blocking issues.")
    else:
        r.add(f"FAIL: {len(r.failures)} issue(s) found:")
        for f in r.failures:
            r.add(f"  - {f}")

    return r


if __name__ == "__main__":
    reporter = main()
    print(reporter.text())
    sys.exit(0 if reporter.ok() else 1)
