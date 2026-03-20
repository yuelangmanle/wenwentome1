# 开发进度书

## 当前阶段

- 阶段：`1.1` 执行中
- 当前基线：`1.0` 已正式发布
- 当前目标：执行 `1.1` 首轮体验增强，优先补齐书架、发现页和阅读闭环

## 已完成

- 统一书库、本地 TXT / EPUB 阅读
- 详情页与阅读状态持久化
- 发现页、书源导入、加入书库
- GitHub 私有仓库同步 MVP
- Discover 搜索竞态与重复入库硬化
- 旧快照书源字段兼容修复
- App 内 changelog 基础
- “我的”页关于与项目入口
- `main` 默认分支建立
- `v1.0` GitHub Release 与签名 APK 发布完成
- `1.1 / Task 7` 书架重构完成，书籍卡片、封面操作与状态展示已切到新版 UI
- 书架相关云端 CI 修复收口，`android-ci` 最新稳定 run 为 `23361400262`

## 当前风险

- 本机仍缺少 Java 运行时，Gradle 验证统一卡在 `Unable to locate a Java Runtime`
- Room 升级仍是 destructive migration
- 书源桥接主路径当前仍以 `LEGACY` 规则为主
- GitHub Actions 仍有 Node 20 deprecation annotation，后续需要升级 action 版本
- `1.1 / Task 8` 发现页“预览优先 + 阅读最新”改造仍在收尾，最终正确性依赖云端 CI 验证

## 下一步

1. 完成 `1.1 / Task 8`：接通发现页预览、刷新目录、阅读最新的导航闭环并补 smoke coverage
2. 推送 `release/android-reader-v1` 分支改动，观察 `android-ci` 对新增 discover/reader 流测试的反馈
3. 继续规划下一批阅读体验增强项，包括更完整的目录交互、翻页模式与网文入口体验
4. 升级 GitHub Actions 里依赖的 Node 20 action，并补充无本地 Java 环境下的云端验证说明
