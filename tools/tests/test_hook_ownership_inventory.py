import re
import unittest
from pathlib import Path


class HookOwnershipInventoryCompletenessTest(unittest.TestCase):
    """
    Mechanical gate for P1.3: every production `ModuleHelper` hook-helper call
    site must be accounted for in `docs/audit/A13_HOOK_OWNERSHIP_INVENTORY.md`,
    and the inventory must contain no UNKNOWN entries.
    """

    @property
    def repo_root(self) -> Path:
        return Path(__file__).resolve().parents[2]

    def _scan_source_calls(self) -> dict[str, int]:
        src = self.repo_root / "app/src/main/java/tv/withaibuild/customiuizer"
        pattern = re.compile(
            r"ModuleHelper\.(findAndHookMethod|hookAllConstructors|hookAllMethods)",
            re.IGNORECASE,
        )
        calls: dict[str, int] = {}
        for f in src.rglob("*"):
            if f.suffix not in (".kt", ".java"):
                continue
            text = f.read_text(encoding="utf-8", errors="replace")
            count = len(pattern.findall(text))
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
                r"^\| `([^`]+)` \| (\d+) \| ([^|]+) \| `([^`]+)` \|",
                line,
            )
            if not m:
                continue
            file_path, count_str, _process, category = m.groups()
            # Normalize to forward slashes and the form used by the source tree.
            file_path = file_path.replace("\\", "/")
            counts[file_path] = int(count_str)
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

        # The inventory records paths relative to tv.withaibuild.customiuizer
        # (e.g. 'mods/SystemUIStatusBarHooks.kt'); the source scan uses repo-root
        # relative paths. Strip the common prefix for comparison.
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


if __name__ == "__main__":
    unittest.main()
