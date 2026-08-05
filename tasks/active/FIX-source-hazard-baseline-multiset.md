# FIX-source-hazard-baseline-multiset

- Platform: A13
- Status: Active
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

修复 `tools/source_hazard_scan.py` baseline 对重复指纹的静默漏检，使同一文件新增的第二处相同 hazard 能被正确报告，同时保持现有 fingerprint 算法不变、不加入行号。

## 当前问题

`Finding.fingerprint` 由 rule、path 和 normalized snippet 组成。同一文件中的多个相同命中会产生相同 fingerprint。`load_baseline()` 返回 `set[str]`，因此 baseline 无法表达同一 fingerprint 的出现次数。

例如，baseline 已审核同一文件中的一处 `.printStackTrace(` 后，该文件新增第二处相同命中，扫描器仍会返回 0 new，造成漏检。

## 允许修改

- `tools/source_hazard_scan.py`
- `tools/tests/test_source_hazard_scan.py`（新增）
- 相关 Python 门禁测试

## 必须保持

- 现有 fingerprint 算法不变，不加入行号；
- `--write-baseline` 格式兼容，继续保存完整 fingerprints 列表（含重复项）；
- `--strict-all` 仍把全部 finding 视为新 finding；
- 输出中的 baseline 数量表示 baseline finding 总数，而不是唯一 fingerprint 数量；
- 原有 allow marker 和 Throwable 测试继续通过；
- 当前 A13 仓库仍为 47 个 reviewed findings、0 new。

## 实现要求

1. 保持现有 fingerprint 算法不变，不加入行号。
2. 将 baseline 匹配从集合语义改为多重集合语义，优先使用 `collections.Counter`。
3. 当前 findings 与 baseline 比较时：
   - baseline 中每个 fingerprint 的一次记录只能抵消一次当前 finding；
   - 当前出现次数超过 baseline 计数的部分必须报告为 new finding；
   - 当前出现次数少于 baseline 时不失败。
4. `--strict-all` 仍应把全部 finding 视为新 finding。
5. `--write-baseline` 格式保持兼容，继续保存完整 fingerprints 列表，包括重复项。
6. 输出中的 baseline 数量应表示 baseline finding 总数，而不是唯一 fingerprint 数量。
7. 不修改 Android 生产源码。
8. 不顺带实施 `PreferenceLoadPredicate`，不扩大到其他架构重构。

建议抽取可测试函数 `find_new_findings(findings: list[Finding], baseline: Counter[str]) -> list[Finding]`。

## 非目标

- 不实施 `PreferenceLoadPredicate`；
- 不修改 Android 生产源码；
- 不更改 hazard 规则或 fingerprint 算法。

## 验收标准

- [ ] `source_hazard_scan.py` baseline 加载后总计数为 47，而不是唯一指纹数 20；
- [ ] 人工构造的第二处重复 hazard 能被报告；
- [ ] baseline 与当前重复次数完全相同时，结果为 0 new；
- [ ] fingerprint 不包含行号的稳定性保持不变；
- [ ] baseline 总数保留重复项计数；
- [ ] 原有 allow marker 和 Throwable 测试继续通过；
- [ ] 完整本地门禁通过；
- [ ] 最终 diff 已审查；
- [ ] 工作区没有未解释改动。

## 验证

```powershell
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
python tools/source_hazard_scan.py --path app/src/main/java
python tools/verify.py fast --changed
python tools/verify.py full
```

## 构建产物

未要求 APK。

## 完成记录

- Base SHA:
- Final SHA:
- Commits:
- Behavior changed:
- Verification:
- Device evidence:
- Known limits:
