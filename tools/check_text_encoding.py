#!/usr/bin/env python3
"""Check tracked text files for UTF-8 validity, BOM, replacement chars and mojibake.

Exit non-zero if any violation is found.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

TEXT_EXTENSIONS = frozenset({
    ".md", ".txt", ".xml", ".json", ".yml", ".yaml",
    ".py", ".sh", ".kt", ".java", ".kts", ".gradle",
    ".properties", ".csv", ".toml", ".cfg", ".ini",
    ".html", ".css", ".js", ".ts", ".bat",
})

KNOWN_TEXT_FILENAMES = frozenset({
    "SUMMARY", "Makefile", "Dockerfile", "Vagrantfile",
    "gradlew", ".gitignore", ".gitattributes", ".editorconfig",
})

MOJIBAKE_SEQUENCES = [
    "\u00e9\u00a1\u00b9",
    "\u6dc7\u00ae",
    "\u93c2\u00b0",
    "\u7e94\u00af",
    "\u7039\u0192",
    "\u9417\u0088",
    "\u00e4\u00bf\u00ae",
]

MOJIBAKE_PATTERNS = re.compile("|".join(re.escape(s) for s in MOJIBAKE_SEQUENCES))

UTF8_BOM = b"\xef\xbb\xbf"


def _is_text_file(name: str) -> bool:
    p = Path(name)
    if p.suffix.lower() in TEXT_EXTENSIONS:
        return True
    if p.name in KNOWN_TEXT_FILENAMES:
        return True
    return False


def tracked_files(root: Path) -> list[str]:
    result = subprocess.run(
        ["git", "ls-files", "-z"],
        cwd=root,
        stdout=subprocess.PIPE,
        check=True,
    )
    return [
        f for f in result.stdout.decode("utf-8", errors="surrogateescape").split("\0")
        if f
    ]


def check(root: Path | None = None) -> list[dict]:
    if root is None:
        root = REPO_ROOT
    violations: list[dict] = []

    for rel in tracked_files(root):
        if not _is_text_file(rel):
            continue
        path = root / rel
        if not path.is_file():
            continue
        raw = path.read_bytes()
        if not raw:
            continue

        has_bom = raw.startswith(UTF8_BOM)
        if has_bom:
            violations.append({"file": rel, "reason": "UTF-8 BOM present"})
            if raw[3:5] == b"#!":
                violations.append({
                    "file": rel,
                    "reason": "BOM before shebang (#!)",
                })

        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError as e:
            violations.append({
                "file": rel,
                "reason": f"invalid UTF-8: {e}",
            })
            continue

        if "\ufffd" in text:
            for i, line in enumerate(text.splitlines(), 1):
                if "\ufffd" in line:
                    violations.append({
                        "file": rel,
                        "reason": f"U+FFFD replacement character at line {i}",
                    })
                    break

        m = MOJIBAKE_PATTERNS.search(text)
        if m:
            for i, line in enumerate(text.splitlines(), 1):
                if MOJIBAKE_PATTERNS.search(line):
                    violations.append({
                        "file": rel,
                        "reason": f"suspected mojibake at line {i}: '{m.group()}'",
                    })
                    break

    return violations


def main() -> int:
    violations = check()
    if violations:
        for v in violations:
            print(f"ENCODING_ERROR: {v['file']}: {v['reason']}", file=sys.stderr)
        print(f"\n{len(violations)} encoding violation(s) found", file=sys.stderr)
        return 1
    tracked = tracked_files(REPO_ROOT)
    scanned = sum(1 for f in tracked if _is_text_file(f))
    print(f"Text encoding check passed: {scanned} files scanned")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
