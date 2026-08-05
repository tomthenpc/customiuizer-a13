# REL-A13-r13.10.0-private-signed

## Base SHA
9ceadcb6ae5d6a6386685ad468175474b2a3663f

## Engineering/Build SHA
19b2804742c8eb596b2936781b9b7eff6caf9e72

## 目标版本
- versionCode: 134
- versionName: r13.10.0

## 签名配置
- 外部签名目录：`C:\Users\tv\Documents\buildkey\r13`
- 使用 properties 文件名：`keystore.properties`
- 未提交任何签名材料、keystore、密码或本地路径配置

## Python 测试数
- 新静态合同测试：45
- 全量 `python -m unittest discover -s tools/tests -p "test_*.py"`：786 通过

## Release JVM 结果
- `:app:testReleaseUnitTest` BUILD SUCCESSFUL

## Release/Vital Lint 结果
- `:app:lintRelease` 通过
- `:app:lintVitalRelease` 已完成（无 fatal）

## APK 本地路径
`C:\Users\tv\Downloads\CustoMIUIzer-A13-r13.10.0\CustoMIUIzer-A13-r13.10.0.apk`

## APK 大小
2925230 bytes

## APK SHA-256
`74BB8B9C67973A63F100D3EF99EFF3C847DD3E5381195846D00916E72C107781`

## 证书 SHA-256
`15CE32F03E4D8E62DF9390F77431862E59BF2CF95CD5A72F0C7330CDFCCA2934`

## V2 signing
PASS（APK Signature Scheme v2：true）

## zipalign
PASS（-c -v 4 验证成功）

## 包与版本元数据
- package: `tv.withaibuild.customiuizer.r13`
- versionCode: 134
- versionName: `r13.10.0`
- minSdkVersion: 33
- targetSdkVersion: 34
- compileSdkVersion: 36
- ABI: `arm64-v8a`

## Xposed 元数据
- `minApiVersion=101`
- `targetApiVersion=102`
- `staticScope=false`
- `META-INF/xposed/module.prop`、`java_init.list`、`scope.list` 均存在

## debuggable
Manifest 中不存在 `android:debuggable` 属性，实际为 false

## tracked worktree 状态
构建时工作区已提交并推送干净

## 状态
- `BUILD_VERIFIED`
- `deviceStatus=UNVERIFIED`
