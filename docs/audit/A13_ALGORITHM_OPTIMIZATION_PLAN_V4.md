# A13 Algorithm Optimization Plan v4

```text
DocumentKind: PLAN
Product: A13
Repository: tomthenpc/customiuizer-a13
Branch: devin/a13-rom-intelligence-audit
EvidenceCommit: 182ee03a209da3405d723a8ce6bc7622b217df57
EvidenceState: STATIC
GeneratedBy: agent
SourceOfTruth: A13_HOT_MERGE_PROMPT_V4.md
DeviceEvidence: NOT_EXERCISED
```

## A. 恢复可复现状态

必须通过：

```text
automation checker
document checker
Audit
Fast
Full
remote HEAD == verified commit
```

## B. FeatureCatalog index

目标：

```kotlin
private val allSpecsInternal by lazy(LazyThreadSafetyMode.NONE) {
    registrySpecsInternal + adaptedSpecsInternal
}
private val specByCanonicalIdInternal by lazy(LazyThreadSafetyMode.NONE) {
    allSpecsInternal.associateBy(FeatureSpec::id)
}
@JvmStatic fun specs(): List<FeatureSpec> = allSpecsInternal
@JvmStatic fun specByCanonicalId(id: String): FeatureSpec? =
    specByCanonicalIdInternal[id]
```

要求：

- 不重复构造列表；
- ID 重复测试失败；
- 不引入通用 catalog 框架。

## C. Dispatcher 最小表面

只保留：

```text
createRuntime
installById
install
```

静态门禁：

```text
FeatureDispatcher 不直接读 preference key
FeatureDispatcher 不调用具体 Hook class
FeatureDispatcher 不拥有 compatibility installer
```

## D. Registry pipeline

测试：

- wrong scope/phase/process 不执行 condition；
- disabled 不占用 INSTALLING；
- fatal 清除 INSTALLING；
- transient 可 retry；
- batch register rollback。

## E. 文档生成

创建：

```text
tools/project_health_snapshot.py
tools/check_document_contracts.py
```

生成 current/delta 文档并接入 Fast。

## F. P3 Hook 收口

结果：

```text
UNKNOWN = 0
duplicate route = 0
unreachable dispatcher path = 0
stale legacy label = 0
```
