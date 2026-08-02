import re
import unittest
from pathlib import Path


class FeatureInventoryCompletenessTest(unittest.TestCase):
    """
    Mechanical gate for P1.2: the canonical process matrix must contain every
    catalog FeatureId and the expected column layout.

    This stops the source-of-truth docs from drifting behind the typed catalog.
    """

    @property
    def repo_root(self) -> Path:
        return Path(__file__).resolve().parents[2]

    def _parse_catalog_ids(self) -> set[str]:
        catalog = (
            self.repo_root
            / "app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt"
        )
        text = catalog.read_text(encoding="utf-8", errors="replace")

        # Each FeatureSpec block is indented 8 spaces and ends with an 8-space
        # closing parenthesis. We capture the body and extract the declared id.
        blocks = re.findall(
            r" {8}FeatureSpec\((.*?) {8}\)",
            text,
            re.DOTALL,
        )
        self.assertEqual(
            29,
            len(blocks),
            "Expected 29 FeatureSpec declarations in FeatureCatalog.kt",
        )

        ids = set()
        for block in blocks:
            m = re.search(r'id = "([^"]+)"', block)
            self.assertIsNotNone(m, f"Could not find id in FeatureSpec block: {block[:120]}")
            ids.add(m.group(1))
        return ids

    def _parse_matrix_ids(self) -> set[str]:
        matrix = self.repo_root / "docs/rom-intelligence/A13_PROCESS_MATRIX.md"
        text = matrix.read_text(encoding="utf-8", errors="replace")

        ids = set()
        expected_columns = 15
        header_seen = False
        for line in text.splitlines():
            stripped = line.strip()
            if not stripped.startswith("|"):
                continue
            cells = [c.strip() for c in stripped.split("|")]
            # Drop leading/trailing empty cells produced by the leading/trailing pipe.
            cells = [c for c in cells if c]

            if not cells:
                continue

            if cells[0] == "Feature ID":
                header_seen = True
                continue
            if cells[0].startswith("---"):
                continue

            if header_seen:
                self.assertEqual(
                    expected_columns,
                    len(cells),
                    f"Process matrix row has {len(cells)} columns, expected {expected_columns}: {stripped[:120]}",
                )
                canonical = cells[0].strip("`").strip()
                # Only catalog feature ids are lowerCamel identifiers without spaces/colons.
                if canonical and re.match(r"^[a-z][a-zA-Z0-9]+$", canonical):
                    ids.add(canonical)

        return ids

    def test_catalog_and_process_matrix_are_complete(self) -> None:
        catalog_ids = self._parse_catalog_ids()
        matrix_ids = self._parse_matrix_ids()

        missing_in_matrix = catalog_ids - matrix_ids
        extra_in_matrix = matrix_ids - catalog_ids

        self.assertEqual(
            set(),
            missing_in_matrix,
            f"FeatureCatalog ids missing from A13_PROCESS_MATRIX.md: {missing_in_matrix}",
        )
        self.assertEqual(
            set(),
            extra_in_matrix,
            f"A13_PROCESS_MATRIX.md has ids not in FeatureCatalog: {extra_in_matrix}",
        )
        self.assertEqual(
            29,
            len(matrix_ids),
            "A13_PROCESS_MATRIX.md must list exactly the 29 catalog feature ids",
        )


if __name__ == "__main__":
    unittest.main()
