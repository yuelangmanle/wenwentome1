# 发版流程

## 目标

正式版本只在 `main` 分支打 tag 时发布，GitHub Actions 自动构建签名 `release APK` 并创建 GitHub Release。

## Secrets

在 GitHub 仓库 Secrets 中配置以下项：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

当前 `1.0` 发版使用 PKCS12 签名文件（`.p12`）。建议将本地留档放在 `.local-secrets/android-release/`，不要提交到仓库。

## 发版前检查

1. 更新 `app/build.gradle.kts` 中的 `versionCode` 与 `versionName`
2. 更新 `CHANGELOG.md`
3. 更新 `app/src/main/assets/changelog.json`
4. 确认 README 和文档无需同步修订
5. 确认代码已合并到 `main`

## 正式发版步骤

1. 切到 `main`
2. 确认工作区干净
3. 打 tag，例如 `v1.0`
4. 推送 tag
5. 等待 GitHub Actions 的 `android-release` workflow 完成
6. 在 GitHub Releases 页面检查 Release 标题、说明和 APK 附件

## 手动触发当前版本发版

当需要从 GitHub Actions 页面手动触发当前 `main` 上的正式版时：

1. 从 Actions 打开 `android-release`
2. 选择 `Run workflow`
3. `Use workflow from` 选择 `main`
4. 填入 `tag_name`，例如 `v1.0`
5. 运行后由 workflow 校验 `tag_name`、`versionName` 和 `CHANGELOG.md`
6. 成功后自动创建或更新对应 GitHub Release

注意：

- 该入口只适用于 `main` 当前版本，不能用来回补已经过去的旧版本 tag
- 如果某个 tag 已经触发过 `android-release`，优先在该 workflow run 上直接 `Re-run jobs`

## 规则

- tag 必须与 `versionName` 一致
- 例如 `versionName = 1.0` 时，tag 必须是 `v1.0`
- tag 对应提交必须来自 `main`
- 手动触发当前版本 release 时也必须显式填写 `tag_name`

## 失败排查

- 缺 Secret：workflow 会直接失败并提示缺失项
- tag 与版本不一致：`scripts/release_metadata.py check-tag` 失败
- changelog 缺对应版本：`scripts/release_metadata.py notes` 失败
- 签名失败：检查 PKCS12 签名文件、alias 和密码
