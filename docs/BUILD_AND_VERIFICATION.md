# A13 构建与验证

## 快速门禁

```powershell
python tools/verify.py fast --changed
```

## 针对性测试

```powershell
python tools/verify.py fast --tests <TestClassName>
```

## 完整门禁

```powershell
python tools/verify.py full
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
git diff --check
```

## Debug APK

仅任务要求时：

```powershell
.\gradlew.bat :app:assembleDebug
```

记录：

- APK 路径；
- 文件大小；
- 签名类型；
- SHA-256；
- 对应 Final SHA。

## Release

只有用户明确要求并且仓库外 A13 专用签名配置有效时运行。禁止 Debug 冒充 Release，
禁止提交或公开上传密钥、密码、APK 和本地签名配置。

## 验证边界

编译、测试和 APK 构建不能替代 MIUI/HyperOS 实机验证。最终报告必须标明证据等级。
