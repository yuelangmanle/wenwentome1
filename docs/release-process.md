# 发版流程

## 目标

正式版本只在 `main` 分支打 tag 时发布，GitHub Actions 自动构建签名 `release APK`、创建 GitHub Release，并可同步更新 GitHub Pages 发布页。

## Secrets

在 GitHub 仓库 Secrets 中配置以下项：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

建议使用统一 keystore 文件（`.jks` 或 `.p12` 均可）。建议将本地留档放在 `.local-secrets/android-release/`，不要提交到仓库。

## 发版前检查

1. 更新 `app/build.gradle` 中的版本声明
2. 更新 `CHANGELOG.md`
3. 更新 `app/src/main/assets/updateLog.md`
4. 更新 `README.md` 与 `site/index.html` 中的当前正式版本
5. 运行 `python3 scripts/release_metadata.py validate-pack app/build.gradle CHANGELOG.md app/src/main/assets/updateLog.md README.md --site-path site/index.html`
6. 确认 README 和文档无需同步修订
7. 确认代码已合并到 `main`

## 正式发版步骤

1. 切到 `main`
2. 确认工作区干净
3. 打 tag，例如 `v1.3`
4. 推送 tag
5. 等待 GitHub Actions 的 `android-release` workflow 完成
6. 在 GitHub Releases 页面检查 Release 标题、说明和 APK 附件
7. 如果本次修改了 `site/`，确认 `github-pages` workflow 已把发布页同步到线上

## 手动触发当前版本发版

当需要从 GitHub Actions 页面手动触发当前 `main` 上的正式版时：

1. 从 Actions 打开 `android-release`
2. 选择 `Run workflow`
3. `Use workflow from` 选择 `main`
4. 填入已经存在、且指向当前 `main` 提交的 `tag_name`，例如 `v1.3`
5. 运行后由 workflow 校验 `tag_name`、`versionName`、发布页和版本资料
6. 成功后自动补发或更新对应 GitHub Release，不会代替你创建新正式 tag

注意：

- 该入口只适用于 `main` 当前版本，不能用来回补已经过去的旧版本 tag
- 该入口也不能代替“打正式 tag”这一步；正式发版仍以 `main` 上已有 tag 为准
- 如果某个 tag 已经触发过 `android-release`，优先在该 workflow run 上直接 `Re-run jobs`

## 规则

- tag 必须与 `versionName` 一致
- 例如 `versionName = 2.0.0` 时，tag 必须是 `v2.0.0`
- tag 对应提交必须来自 `main`
- 手动触发当前版本 release 时也必须显式填写 `tag_name`
- `CHANGELOG.md`、`app/src/main/assets/updateLog.md`、`README.md` 和 `site/index.html` 中的版本信息必须与当前发布目标一致

## 无本地 Java 时的云端验证

如果当前机器没有 Java 运行时，不在本地强行打包，改走 GitHub 云端验证：

1. 提交并推送当前分支
2. 运行 `gh run list --workflow android-ci --limit 1` 找到最新 run 的 `databaseId`
3. 再运行 `gh run watch <databaseId> --exit-status`
4. 成功后在 Actions 页面或 `gh run download <databaseId> -n debug-apk` 下载 `debug-apk` artifact
5. 如果下载时出现 `EOF`，优先回到对应 run 重试，或直接在浏览器中下载 artifact

说明：

- `android-ci` 不依赖 release secrets，只负责测试和 `debug APK`
- `android-release` 依赖 `ANDROID_KEYSTORE_*` secrets，只在正式发版时使用
- GitHub Pages 发布页与 Android 打包相互独立，可单独部署

## 失败排查

- 缺 Secret：workflow 会直接失败并提示缺失项
- tag 与版本不一致：`scripts/release_metadata.py check-tag` 失败
- 文档、App 内更新日志或发布页版本不一致：`scripts/release_metadata.py validate-pack` 失败
- changelog 缺对应版本：`scripts/release_metadata.py notes` 失败
- 签名失败：检查 keystore 文件、alias 和密码
