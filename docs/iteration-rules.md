# 迭代规则

## 版本号规则

- 首个正式版：`1.0`
- 后续每次正式版：`+0.1`
- `1.9` 后下一版进位为 `2.0`

示例：

- `1.0 -> 1.1`
- `1.1 -> 1.2`
- `1.8 -> 1.9`
- `1.9 -> 2.0`

## versionCode 规则

- `versionCode` 必须持续递增
- 推荐按十位步进：
  - `1.0 -> 100`
  - `1.1 -> 110`
  - `1.2 -> 120`
  - `2.0 -> 200`

## 更新日志规则

每次正式发版必须同步更新以下 4 处：

1. `app/build.gradle.kts`
2. `CHANGELOG.md`
3. `app/src/main/assets/changelog.json`
4. GitHub Release notes

## 发布规则

- 正式版只通过 `main` 分支 tag 发布
- tag 格式固定为 `v<major>.<minor>`
- tag 与 `versionName` 必须一致

## 文档规则

每次正式发版前至少检查以下文档：

- `README.md`
- `CHANGELOG.md`
- `docs/release-process.md`
- `docs/development-progress.md`

## 提交规则

- 先测试，后实现
- 每个任务结束后做一次最小验证
- 不在未说明风险的情况下声称“已通过”
