import re
import unittest
from pathlib import Path


class HookOwnershipInventoryCompletenessTest(unittest.TestCase):
    """
    Mechanical gate for P1.3: every production hook call site must be accounted
    for in `docs/audit/A13_HOOK_OWNERSHIP_INVENTORY.md`, and the inventory must
    contain no UNKNOWN entries.
    """

    HOOK_RE = re.compile(
        r"\bModuleHelper\s*\.\s*"
        r"(findAndHookMethod|hookAllConstructors|hookAllMethods)",
        re.S,
    )

    @property
    def repo_root(self) -> Path:
        return Path(__file__).resolve().parents[2]

    def _scan_source_calls(self) -> dict[str, int]:
        src = self.repo_root / "app/src/main/java/tv/withaibuild/customiuizer"
        calls: dict[str, int] = {}
        for f in src.rglob("*"):
            if f.suffix not in (".kt", ".java"):
                continue
            text = f.read_text(encoding="utf-8", errors="replace")
            count = len(self.HOOK_RE.findall(text))
            if count:
                rel = str(f.relative_to(self.repo_root)).replace("\\", "/")
                calls[rel] = count
        return calls

    def _parse_inventory(self) -> tuple[dict[str, int], set[str]]:
        inventory = self.repo_root / "docs/audit/A13_HOOK_OWNERSHIP_INVENTORY.md"
        text = inventory.read_text(encoding="utf-8", errors="replace")

        counts: dict[str, int] = {}
        categories: set[str] = set()
        for line in text.splitlines():
            m = re.match(
                r"^\| `([^`]+)` \| (\d+) \| [^|]+ \| `([^`]+)` \|",
                line,
            )
            if not m:
                continue
            file_path, direct, category = m.groups()
            file_path = file_path.replace("\\", "/")
            prefix = "tv/withaibuild/customiuizer/"
            if file_path.startswith(prefix):
                file_path = file_path[len(prefix):]
            counts[file_path] = int(direct)
            categories.add(category.strip())

        return counts, categories

    def test_inventory_covers_all_hook_calls(self) -> None:
        source_calls = self._scan_source_calls()
        inventory_counts, categories = self._parse_inventory()

        source_total = sum(source_calls.values())
        inventory_total = sum(inventory_counts.values())

        self.assertEqual(
            source_total,
            inventory_total,
            f"Hook call total mismatch: source={source_total}, inventory={inventory_total}",
        )

        prefix = "app/src/main/java/tv/withaibuild/customiuizer/"
        source_keys = {k[len(prefix):] if k.startswith(prefix) else k for k in source_calls}

        missing_from_inventory = source_keys - set(inventory_counts.keys())
        extra_in_inventory = set(inventory_counts.keys()) - source_keys

        self.assertEqual(
            set(),
            missing_from_inventory,
            f"Files with hook calls missing from inventory: {missing_from_inventory}",
        )
        self.assertEqual(
            set(),
            extra_in_inventory,
            f"Inventory files with no hook calls: {extra_in_inventory}",
        )

    def test_inventory_has_no_unknown_entries(self) -> None:
        _counts, categories = self._parse_inventory()
        self.assertNotIn(
            "UNKNOWN",
            categories,
            "A13_HOOK_OWNERSHIP_INVENTORY.md must not contain UNKNOWN entries",
        )


class HookRegexDecouplingTest(unittest.TestCase):
    """Contract: canonical HOOK_RE and OWNERSHIP_REPORT_HOOK_RE are independent."""

    def test_canonical_hook_re_covers_all_helper_forms(self) -> None:
        import sys
        sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
        from audit_hook_ownership import HOOK_RE

        must_match = [
            "ModuleHelper.findAndHookMethod(",
            "XposedHelpers.findAndHookMethod(",
            "XposedBridge.hookAllMethods(",
            "HookerClassHelper.findAndHookMethod(",
            "findAndHookMethod(",
            "hookAllMethods(",
            "hookAllConstructors(",
        ]
        for sample in must_match:
            self.assertIsNotNone(
                HOOK_RE.search(sample),
                f"canonical HOOK_RE must match: {sample}",
            )

    def test_ownership_report_regex_is_narrower(self) -> None:
        import sys
        sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
        from audit_hook_ownership import HOOK_RE, OWNERSHIP_REPORT_HOOK_RE

        must_not_match = [
            "XposedHelpers.findAndHookMethod(",
            "XposedBridge.hookAllMethods(",
            "HookerClassHelper.findAndHookMethod(",
            "findAndHookMethod(",
        ]
        for sample in must_not_match:
            self.assertIsNone(
                OWNERSHIP_REPORT_HOOK_RE.search(sample),
                f"OWNERSHIP_REPORT_HOOK_RE must NOT match: {sample}",
            )

        must_match = [
            "ModuleHelper.findAndHookMethod(",
            "ModuleHelper.hookAllConstructors(",
            "ModuleHelper.hookAllMethods(",
        ]
        for sample in must_match:
            self.assertIsNotNone(
                OWNERSHIP_REPORT_HOOK_RE.search(sample),
                f"OWNERSHIP_REPORT_HOOK_RE must match: {sample}",
            )

    def test_ownership_change_does_not_alter_legacy_census(self) -> None:
        import sys
        sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
        from build_legacy_exception_registry import scan_legacy_call_sites, build_registry

        sites = scan_legacy_call_sites()
        registry = build_registry(sites)
        self.assertEqual(
            registry["totalLegacyCallSites"],
            522,
            "Legacy census must remain 522 call sites",
        )
        self.assertEqual(
            registry["totalLegacyGroups"],
            212,
            "Legacy census must remain 212 groups",
        )


if __name__ == "__main__":
    unittest.main()
