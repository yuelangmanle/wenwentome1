# 迭代规则

## 版本号规则

- 当前新底座首个正式版：`2.0.0`
- 后续正式版按语义化版本递增，例如 `2.0.1`、`2.1.0`

示例：

- `2.0.0 -> 2.0.1`
- `2.0.1 -> 2.1.0`
- `2.1.0 -> 2.1.1`

## versionCode 规则

- `versionCode` 必须持续递增
- 当前基线使用 `20000 + git commit 总数` 方案，保证 `2.x` 新产品线始终高于旧 `1.x`

## 更新日志规则

每次正式发版必须同步更新以下 6 处：

1. `app/build.gradle`
2. `CHANGELOG.md`
3. `app/src/main/assets/updateLog.md`
4. GitHub Release notes
5. `README.md` 中的当前发布目标
6. `site/index.html` 中的当前发布目标

## 发布规则

- 正式版只通过 `main` 分支 tag 发布
- tag 格式固定为 `v<versionName>`
- tag 与 `versionName` 必须一致
- 发版前必须运行：
  - `python3 scripts/release_metadata.py validate-pack app/build.gradle CHANGELOG.md app/src/main/assets/updateLog.md README.md --site-path site/index.html`

## 文档规则

每次正式发版前至少检查以下文档：

- `README.md`
- `CHANGELOG.md`
- `docs/release-process.md`
- `docs/development-progress.md`
- 如果仓库启用了 GitHub Pages，还要检查 `site/`

## 提交规则

- 先测试，后实现
- 每个任务结束后做一次最小验证
- 不在未说明风险的情况下声称“已通过”
