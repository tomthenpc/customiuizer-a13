#!/usr/bin/env python3
"""Regenerate docs/audit/A13_HOOK_OWNERSHIP_INVENTORY.md from current source.

This tool maps every ModuleHelper hook call site to an owning public function and
checks whether that function is referenced from a typed FeatureSpec in
FeatureCatalog.kt.  The resulting inventory uses:

- REGISTRY_FEATURE: every hook call in the file is inside a function called from
  the typed catalog.
- LEGACY_EXCEPTION: at least one hook call is inside a function not called from
  the typed catalog.
- INSTALLER_INFRASTRUCTURE: explicit installer/bootstrap files listed below.
- UNKNOWN: no owner could be determined (target 0).
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = REPO_ROOT / "app/src/main/java/tv/withaibuild/customiuizer"
CATALOG = SRC_ROOT / "mods/catalog/FeatureCatalog.kt"
INVENTORY = REPO_ROOT / "docs/audit/A13_HOOK_OWNERSHIP_INVENTORY.md"

INSTALLER_FILES = {
    "installers/GenericAppInstaller.java",
    "installers/LauncherInstaller.java",
    "installers/SystemUiInstaller.java",
    "mods/utils/DeviceInfoMonitor.kt",
    "mods/utils/HookInstaller.kt",
    "mods/utils/ResourceHooks.java",
}

HOOK_PATTERN = re.compile(
    r"ModuleHelper\.(findAndHookMethod|hookAllConstructors|hookAllMethods)",
    re.IGNORECASE,
)


def find_public_functions(text: str) -> list[tuple[int, str]]:
    """Return (line, name) for top-level/public-like functions in a Kotlin object file."""
    # Match Java/Kotlin static/public function declarations at the top/object level.
    # This is heuristic: "fun Xxx(" or "public static void Xxx(" or "@JvmStatic\nfun Xxx"
    pattern = re.compile(
        r"(?:^|\n)(?:\s*@JvmStatic\s*)?(?:public\s+|private\s+|protected\s+|internal\s+)?"
        r"(?:static\s+)?(?:fun|void|boolean|int|float|String)\s+(\w+)\s*\(",
        re.MULTILINE,
    )
    funcs = []
    for m in pattern.finditer(text):
        line = text[: m.start()].count("\n") + 1
        funcs.append((line, m.group(1)))
    return funcs


def enclosing_function(line_no: int, funcs: list[tuple[int, str]]) -> str | None:
    """Return the function declaration that contains the given line."""
    current = None
    for line, name in funcs:
        if line > line_no:
            break
        current = name
    return current


def catalog_references(catalog_text: str) -> set[str]:
    """Return the set of 'FooHooks.BarHook' references called from the typed catalog."""
    refs: set[str] = set()
    for m in re.finditer(r"(\w+Hooks)\.(\w+)", catalog_text):
        refs.add(f"{m.group(1)}.{m.group(2)}")
    return refs


def scan_files() -> dict[str, dict]:
    catalog_text = CATALOG.read_text(encoding="utf-8", errors="replace")
    typed_refs = catalog_references(catalog_text)

    file_data: dict[str, dict] = {}
    for path in SRC_ROOT.rglob("*"):
        if path.suffix not in (".kt", ".java"):
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        rel = str(path.relative_to(REPO_ROOT)).replace("\\", "/")

        funcs = find_public_functions(text)
        calls_by_func: dict[str, int] = defaultdict(int)
        for m in HOOK_PATTERN.finditer(text):
            line_no = text[: m.start()].count("\n") + 1
            func = enclosing_function(line_no, funcs)
            calls_by_func[func or "<anonymous>"] += 1

        if not calls_by_func:
            continue

        total = sum(calls_by_func.values())
        file_data[rel] = {
            "total": total,
            "calls": dict(calls_by_func),
            "typed_funcs": set(),
            "legacy_funcs": set(),
        }

        for func in calls_by_func:
            # Build the likely reference string from the filename and function name
            file_stem = path.stem
            ref = f"{file_stem}.{func}"
            if ref in typed_refs:
                file_data[rel]["typed_funcs"].add(func)
            else:
                file_data[rel]["legacy_funcs"].add(func)

    return file_data


def categorize(rel: str, data: dict) -> tuple[str, str]:
    if rel in INSTALLER_FILES:
        return "INSTALLER_INFRASTRUCTURE", "bootstrap / utility installer"

    legacy = data["legacy_funcs"]
    typed = data["typed_funcs"]

    if not typed and not legacy:
        return "UNKNOWN", "no hook calls found"
    if not legacy:
        return "REGISTRY_FEATURE", "all hook calls owned by typed catalog"
    if not typed:
        return "LEGACY_EXCEPTION", "no typed catalog owner"
    return "LEGACY_EXCEPTION", f"mixed: typed={len(typed)}, legacy={len(legacy)}"


def build_primary_process(rel: str, data: dict) -> str:
    # Best-guess process from filename and legacy/typed mix
    if "System" in rel or "systemui" in rel.lower():
        return "com.android.systemui"
    if "Launcher" in rel:
        return "com.miui.home / com.mi.android.globallauncher"
    if "installers" in rel:
        if "Launcher" in rel:
            return "com.miui.home / com.mi.android.globallauncher"
        if "SystemUi" in rel:
            return "com.android.systemui"
        return "per-app / mixed"
    return "mixed"


def generate_inventory() -> str:
    data = scan_files()

    summary: dict[str, int] = defaultdict(int)
    file_count: dict[str, int] = defaultdict(int)
    rows: list[str] = []

    for rel in sorted(data):
        d = data[rel]
        category, notes = categorize(rel, d)
        process = build_primary_process(rel, d)
        summary[category] += d["total"]
        file_count[category] += 1

        display_rel = rel.replace("app/src/main/java/tv/withaibuild/customiuizer/", "")
        rows.append(
            f"| `{display_rel}` | {d['total']} | {process} | `{category}` | {notes} |"
        )

    total_files = len(data)
    total_calls = sum(d["total"] for d in data.values())

    lines = [
        "# A13 Hook Ownership Inventory",
        "",
        "> Branch: `devin/a13-rom-intelligence-audit`",
        "> Baseline commit: `HEAD`",
        "> Generated: auto",
        "> Repository: `tomthenpc/customiuizer-a13`",
        "> Device evidence: `NOT_EXERCISED`",
        "",
        "---",
        "",
        "## 1. Classification",
        "",
        "| Category | Meaning |",
        "|---|---|",
        "| `REGISTRY_FEATURE` | Hook implementation owned by a typed catalog `FeatureId` and installed through `FeatureInstallRegistry`. |",
        "| `INSTALLER_INFRASTRUCTURE` | Bootstrap / utility hook used by an `Installer` or shared runtime helper, not a business Feature. |",
        "| `LEGACY_EXCEPTION` | Business hook with at least one call site not yet owned by a typed catalog Feature. |",
        "| `UNKNOWN` | Hook with no reachable owner (target: 0). |",
        "",
        "---",
        "",
        "## 2. Summary",
        "",
        "| Category | Files | Direct `ModuleHelper.*` call sites | Share |",
        "|---|---|---|---|",
    ]

    for cat in ["REGISTRY_FEATURE", "INSTALLER_INFRASTRUCTURE", "LEGACY_EXCEPTION", "UNKNOWN"]:
        calls = summary[cat]
        files = file_count[cat]
        share = f"{(calls / total_calls * 100):.1f} %" if total_calls else "0 %"
        lines.append(f"| `{cat}` | {files} | {calls} | {share} |")

    lines.extend([
        f"| **Total** | **{total_files}** | **{total_calls}** | **100 %** |",
        "",
        "---",
        "",
        "## 3. Per-file inventory",
        "",
        "| File | Direct calls | Primary process | Category | Notes |",
        "|---|---|---|---|---|",
    ])
    lines.extend(rows)
    lines.append("")

    return "\n".join(lines)


def main() -> int:
    INVENTORY.write_text(generate_inventory(), encoding="utf-8")
    print(f"Updated {INVENTORY}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
