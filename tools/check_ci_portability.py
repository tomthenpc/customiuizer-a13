#!/usr/bin/env python3
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
WORKFLOWS = REPO / ".github" / "workflows"


def fail(msg: str, errors: list[str]) -> None:
    errors.append(msg)


def main() -> int:
    errors: list[str] = []
    workflows = sorted([*WORKFLOWS.glob("*.yml"), *WORKFLOWS.glob("*.yaml")])
    if not workflows:
        print("no workflows found", file=sys.stderr)
        return 1

    action_pin = re.compile(r"^\s*uses:\s*[^@\s]+@([0-9a-f]{40})\s*(?:#.*)?$", re.M)
    for wf in workflows:
        text = wf.read_text(encoding="utf-8", errors="replace")
        rel = wf.relative_to(REPO).as_posix()
        if "fetch-depth: 0" not in text:
            fail(f"{rel}: missing fetch-depth: 0", errors)
        if "persist-credentials: false" not in text:
            fail(f"{rel}: missing persist-credentials: false", errors)
        if re.search(r"\bgradlew\.bat\b", text, re.I):
            fail(f"{rel}: gradlew.bat is forbidden in Actions", errors)
        if re.search(r"\b(?:pwsh|powershell)\b", text, re.I):
            fail(f"{rel}: PowerShell shell is forbidden in Ubuntu Actions", errors)
        if re.search(r"[A-Za-z]:\\\\", text):
            fail(f"{rel}: hardcoded Windows path found", errors)
        if re.search(r"CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES|customiuizerA13KeystoreProperties", text):
            fail(f"{rel}: signing properties must not be referenced in CI", errors)
        if re.search(r"storePassword|keyPassword|keyAlias|storeFile", text):
            fail(f"{rel}: keystore fields must not appear in workflow", errors)
        if re.search(r"platforms;android-37|build-tools;37", text):
            fail(f"{rel}: A13 CI must not install API37 packages", errors)
        uses_lines = re.findall(r"^\s*uses:\s*([^\n]+)$", text, re.M)
        for line in uses_lines:
            if line.startswith("./"):
                continue
            if not action_pin.match(f"uses: {line}"):
                fail(f"{rel}: unpinned action: {line}", errors)

    ci_contract = subprocess.run(
        [
            sys.executable,
            str(REPO / "tools" / "ci_contract_scan.py"),
            "--expected-branch",
            "main",
            "--default-branch",
            "main",
        ],
        cwd=REPO,
        capture_output=True,
        text=True,
    )
    if ci_contract.returncode != 0:
        fail(ci_contract.stdout.strip() or ci_contract.stderr.strip() or "ci_contract_scan failed", errors)

    if errors:
        print("CI portability violations:")
        for err in errors:
            print(f"  - {err}")
        return 1
    print("CI portability passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
