import os
import subprocess
import tempfile
import unittest
from pathlib import Path

from tools.check_text_encoding import check, _is_text_file, MOJIBAKE_PATTERNS, UTF8_BOM


def _git_repo(files: dict[str, bytes]) -> Path:
    """Create a temporary git repo with tracked files."""
    td = tempfile.mkdtemp(prefix="enc-test-")
    root = Path(td)
    subprocess.run(["git", "init", str(root)], capture_output=True, check=True)
    subprocess.run(
        ["git", "config", "user.email", "test@test"],
        cwd=root, capture_output=True, check=True,
    )
    subprocess.run(
        ["git", "config", "user.name", "test"],
        cwd=root, capture_output=True, check=True,
    )
    for name, content in files.items():
        p = root / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(content)
    subprocess.run(
        ["git", "add", "."],
        cwd=root, capture_output=True, check=True,
    )
    subprocess.run(
        ["git", "commit", "-m", "init", "--allow-empty"],
        cwd=root, capture_output=True, check=True,
    )
    return root


class TextEncodingTest(unittest.TestCase):

    def test_valid_utf8_chinese(self) -> None:
        root = _git_repo({"README.md": "# 你好世界\n简体中文测试".encode("utf-8")})
        self.assertEqual(check(root), [])

    def test_valid_utf8_english(self) -> None:
        root = _git_repo({"README.md": b"Hello World\n"})
        self.assertEqual(check(root), [])

    def test_utf8_bom_rejected(self) -> None:
        root = _git_repo({"README.md": UTF8_BOM + b"Hello\n"})
        violations = check(root)
        self.assertTrue(any("BOM" in v["reason"] for v in violations))

    def test_invalid_utf8_bytes(self) -> None:
        root = _git_repo({"bad.txt": b"Hello \x80\x81 World\n"})
        violations = check(root)
        self.assertTrue(any("invalid UTF-8" in v["reason"] for v in violations))

    def test_replacement_char_rejected(self) -> None:
        root = _git_repo({"bad.md": "Hello \ufffd World\n".encode("utf-8")})
        violations = check(root)
        self.assertTrue(any("U+FFFD" in v["reason"] for v in violations))

    def test_known_mojibake_detected(self) -> None:
        root = _git_repo({"bad.md": "This is é¡¹ broken text\n".encode("utf-8")})
        violations = check(root)
        self.assertTrue(any("mojibake" in v["reason"] for v in violations))

    def test_normal_chinese_not_falsely_rejected(self) -> None:
        content = (
            "# 更新日志\n\n"
            "## r13.12.0\n\n"
            "- 修复自定义动作保存后回到无动作的问题\n"
            "- 桌面手势页补齐重启桌面入口\n"
            "- 状态栏时钟、日期、温度、网速、电池\n"
            "- 完成架构与产品能力对照的静态收口\n"
        ).encode("utf-8")
        root = _git_repo({"CHANGELOG.md": content})
        self.assertEqual(check(root), [])

    def test_binary_files_ignored(self) -> None:
        root = _git_repo({
            "app.apk": b"\x50\x4b\x03\x04" + b"\x80\x81" * 100,
            "icon.png": b"\x89PNG" + b"\xff\xfe" * 50,
        })
        self.assertEqual(check(root), [])

    def test_untracked_files_ignored(self) -> None:
        root = _git_repo({"README.md": b"ok\n"})
        bad = root / "untracked.md"
        bad.write_bytes(b"\x80\x81 broken")
        self.assertEqual(check(root), [])

    def test_shebang_with_bom_fails(self) -> None:
        root = _git_repo({"run.py": UTF8_BOM + b"#!/usr/bin/env python3\nprint(1)\n"})
        violations = check(root)
        reasons = [v["reason"] for v in violations]
        self.assertTrue(any("shebang" in r for r in reasons))

    def test_is_text_file_coverage(self) -> None:
        self.assertTrue(_is_text_file("README.md"))
        self.assertTrue(_is_text_file("build.gradle.kts"))
        self.assertTrue(_is_text_file("app.properties"))
        self.assertTrue(_is_text_file("data.csv"))
        self.assertTrue(_is_text_file("gradlew"))
        self.assertTrue(_is_text_file(".gitignore"))
        self.assertFalse(_is_text_file("app.apk"))
        self.assertFalse(_is_text_file("icon.png"))
        self.assertFalse(_is_text_file("lib.jar"))

    def test_mojibake_regex_no_false_positives_on_common_chinese(self) -> None:
        safe = [
            "修复", "新增", "项目", "端口", "实现", "特性",
            "更新", "删除", "优化", "调整", "加固", "完成",
            "选择器", "生命周期", "状态栏", "偏好设置",
        ]
        for word in safe:
            self.assertIsNone(
                MOJIBAKE_PATTERNS.search(word),
                f"MOJIBAKE_PATTERNS must not match legitimate Chinese: {word}",
            )


if __name__ == "__main__":
    unittest.main()
