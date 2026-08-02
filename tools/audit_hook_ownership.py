#!/usr/bin/env python3
"""A13 Hook ownership auditor.

Scans production Java/Kotlin for hook call sites and classifies each call site
into a P3 ownership category.  A call site is `REGISTRY_FEATURE` when it lives
inside a function that is referenced as the installer of a typed `FeatureSpec`.
"""
from __future__ import annotations

import re
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SOURCE_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"
OUT_FILE = REPO_ROOT / "docs" / "audit" / "A13_HOOK_OWNERSHIP_INVENTORY.md"

HOOK_RE = re.compile(
    r"\b(ModuleHelper|XposedHelpers|XposedBridge|HookerClassHelper)\s*\.\s*"
    r"(findAndHookMethod|findAndHookConstructor|hookAllMethods|hookAllConstructors|hookMethod|hookAll)"
    r"|\bfindAndHookMethod\s*\("
    r"|\bhookAllMethods\s*\("
    r"|\bhookAllConstructors\s*\(",
    re.S,
)

TYPED_INSTALL_RE = re.compile(
    r"(FeatureInstallRegistry\.installById|\.installById\(|FeatureInstallRegistry\.installAll|\.installAll\()",
    re.S,
)


REGISTRY_FILES = {
    "tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt",
    "tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt",
    "tv/withaibuild/customiuizer/mods/catalog/CanaryContracts.kt",
    "tv/withaibuild/customiuizer/mods/catalog/CatalogContracts.kt",
    "tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt",
    "tv/withaibuild/customiuizer/mods/catalog/FeatureId.kt",
    "tv/withaibuild/customiuizer/mods/catalog/FeatureRuntime.kt",
}


def class_name_to_file(class_name: str) -> Path | None:
    """Map a simple class/object name (e.g. PackagePermissions) to a source file."""
    for ext in (".kt", ".java"):
        candidate = SOURCE_ROOT / "tv" / "withaibuild" / "customiuizer" / "mods" / f"{class_name}{ext}"
        if candidate.exists():
            return candidate
        # Maybe in utils/
        candidate = SOURCE_ROOT / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / f"{class_name}{ext}"
        if candidate.exists():
            return candidate
    return None


def extract_typed_installers() -> dict[Path, set[str]]:
    """Parse FeatureCatalog.kt and return {source_file: {function_names}}."""
    catalog = SOURCE_ROOT / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog" / "FeatureCatalog.kt"
    text = catalog.read_text(encoding="utf-8")

    result: dict[Path, set[str]] = defaultdict(set)

    # Split on top-level FeatureSpec( occurrences outside of comments.
    for m in re.finditer(r"FeatureSpec\(", text):
        start = m.start()
        # Balanced paren to extract the call block
        depth = 0
        end = start
        for i in range(start, len(text)):
            if text[i] == "(":
                depth += 1
            elif text[i] == ")":
                depth -= 1
                if depth == 0:
                    end = i
                    break

        block = text[start : end + 1]

        # installer = { ... }
        installer_match = re.search(r"installer\s*=\s*\{(.*)\}", block, re.S)
        if not installer_match:
            continue

        installer_body = installer_match.group(1)

        # Find the actual installer call: ObjectName.functionName(...)
        for call in re.finditer(r"(\w+)\.(\w+)\s*\(", installer_body):
            class_name = call.group(1)
            func_name = call.group(2)
            if class_name in ("HookInstaller", "runtime", "prefs", "it", "this", "PackageReadyParam"):
                continue
            file = class_name_to_file(class_name)
            if file:
                result[file].add(func_name)

    return result


def file_typed_functions() -> dict[Path, set[str]]:
    """Return the set of typed-installer function names for each source file."""
    return extract_typed_installers()


def classify_file(rel: str) -> str | None:
    if rel.startswith("tv/withaibuild/customiuizer/mods/utils/") and "ModuleHelper" in rel:
        return "API_BRIDGE"
    if "org/apache/commons/lang3/reflect" in rel:
        return "API_BRIDGE"
    if rel in REGISTRY_FILES:
        return "INSTALLER_INFRASTRUCTURE"
    if rel.startswith("tv/withaibuild/customiuizer/installers/") or rel == "tv/withaibuild/customiuizer/MainModule.java":
        return "INSTALLER_INFRASTRUCTURE"
    if re.search(r"tv/withaibuild/customiuizer/mods/utils/(HookInstaller|ProcessRouter|SystemUiBootstrapCoordinator|ResourceHooks)", rel):
        return "INSTALLER_INFRASTRUCTURE"
    if re.search(r"ResourceHooks", rel):
        return "INSTALLER_INFRASTRUCTURE"
    if rel.startswith("tv/withaibuild/customiuizer/mods/"):
        return None  # per-call classification
    return None


def nearest_function(lines: list[str], line_idx: int) -> str:
    best: tuple[int, str] | None = None
    for i in range(line_idx, -1, -1):
        line = lines[i]
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("//") or stripped.startswith("/*") or stripped.startswith("*"):
            continue
        indent = len(line) - len(line.lstrip())
        m = re.match(r"(?:fun|override fun|open fun)\s+(\w+)", stripped)
        if not m:
            m = re.match(r"(?:public|private|protected|static|final|synchronized|\s)+(?:[\w<>,\[\]]+\s+)*(\w+)\s*\(", stripped)
        if m and m.group(1) not in ("if", "while", "for", "switch", "catch", "return"):
            if best is None or indent < best[0]:
                best = (indent, m.group(1))
                if indent <= 4:
                    break
    return best[1] if best else "?"


def main() -> int:
    typed_funcs = file_typed_functions()

    rows: list[tuple[str, int, int, int, str, str, str]] = []
    file_stats: dict[str, dict[str, int]] = defaultdict(lambda: {"hook": 0, "typed": 0, "registry": 0, "legacy": 0})
    category_totals: dict[str, int] = defaultdict(int)
    category_files: dict[str, set[str]] = defaultdict(set)
    total = 0

    for path in sorted(SOURCE_ROOT.rglob("*.kt")) + sorted(SOURCE_ROOT.rglob("*.java")):
        rel = path.relative_to(SOURCE_ROOT).as_posix()
        text = path.read_text(encoding="utf-8")
        lines = text.splitlines()

        file_default = classify_file(rel)
        file_typed = typed_funcs.get(path, set())

        for i, line in enumerate(lines, start=1):
            if not HOOK_RE.search(line):
                continue

            total += 1
            func = nearest_function(lines, i - 1)

            if file_default == "API_BRIDGE":
                category = "API_BRIDGE"
            elif file_default == "INSTALLER_INFRASTRUCTURE":
                category = "INSTALLER_INFRASTRUCTURE"
            elif file_default is None and rel.startswith("tv/withaibuild/customiuizer/mods/"):
                category = "REGISTRY_FEATURE" if func in file_typed else "LEGACY_EXCEPTION"
            else:
                category = "UNKNOWN"

            file_stats[rel]["hook"] += 1
            if category == "REGISTRY_FEATURE":
                file_stats[rel]["registry"] += 1
            else:
                file_stats[rel]["legacy"] += 1
            category_totals[category] += 1
            category_files[category].add(rel)
            rows.append((rel, i, func, category, line.strip()))

    # Build per-file summary
    file_summaries: list[tuple[str, int, int, int, str, str]] = []
    for rel, stats in file_stats.items():
        registry = stats["registry"]
        legacy = stats["legacy"]
        hook = stats["hook"]
        rel_path = SOURCE_ROOT / rel
        default = classify_file(rel)
        if default in ("API_BRIDGE", "INSTALLER_INFRASTRUCTURE"):
            category = default
            note = "infrastructure"
        elif registry == 0:
            category = "LEGACY_EXCEPTION"
            note = "no typed catalog owner"
        elif legacy == 0:
            category = "REGISTRY_FEATURE"
            note = "typed catalog"
        else:
            category = "LEGACY_EXCEPTION"
            note = f"mixed: typed={registry}, legacy={legacy}"
        file_summaries.append((rel, hook, registry, legacy, category, note))

    md = ["# A13 Hook Ownership Inventory\n\n"]
    md.append(f"Total direct hook call sites: {total}\n\n")
    md.append("| Category | Files | Direct calls | Share |\n|---|---|---:|---:|\n")
    for cat in ("REGISTRY_FEATURE", "INSTALLER_INFRASTRUCTURE", "API_BRIDGE", "LEGACY_EXCEPTION", "UNKNOWN"):
        files = category_files.get(cat, set())
        count = category_totals.get(cat, 0)
        share = (count / total * 100) if total > 0 else 0.0
        md.append(f"| `{cat}` | {len(files)} | {count} | {share:.1f} % |\n")
    md.append("\n")

    md.append("## Per-file summary\n\n")
    md.append("| File | Direct calls | Registry calls | Legacy calls | Category | Notes |\n")
    md.append("|---|---|---:|---:|---|---|---|\n")
    for rel, hook, registry, legacy, category, note in sorted(file_summaries, key=lambda r: (r[4], r[0])):
        md.append(f"| `{rel}` | {hook} | {registry} | {legacy} | `{category}` | {note} |\n")

    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    OUT_FILE.write_text("".join(md), encoding="utf-8", newline="\n")
    print(f"Wrote {OUT_FILE}")
    print(f"Total hook sites: {total}")
    for cat in ("REGISTRY_FEATURE", "INSTALLER_INFRASTRUCTURE", "API_BRIDGE", "LEGACY_EXCEPTION", "UNKNOWN"):
        print(f"  {cat}: {category_totals.get(cat, 0)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
