# 开发进度书

## 当前阶段

- 阶段：`1.1` 执行中
- 当前基线：`1.0` 已正式发布
- 当前目标：收口 `1.1` 阅读体验增强，重点完成网文精确续读、阅读器闭环验证与云端产物整理

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
- 阅读器三模式、目录、章节跳转、继续阅读主链已接通
- 网文阅读保存进度后回流书架的 app 闭环测试已补齐
- `WEB` 阅读 locator 已升级为“章节 + 段落”精度，继续阅读可恢复到章内位置
- `android-ci` 最新稳定 run 为 `23376843732`

## 当前风险

- 本机仍缺少 Java 运行时，Gradle 验证统一卡在 `Unable to locate a Java Runtime`
- Room 升级仍是 destructive migration
- 书源桥接主路径当前仍以 `LEGACY` 规则为主
- GitHub Actions 仍有 Node 20 deprecation annotation，后续需要升级 action 版本
- GitHub artifact 下载到本地仍偶发 `EOF`，当前确认是下载链路问题，不是构建失败
- `development-progress.md` 之外的部分开发文档尚未统一同步到最新 reader/web 进展
- PDF 阅读仍未实现，这一项按当前阶段规划继续后置

## 下一步

1. 把 `1.1` 剩余文档收尾，包括 README、开发进度、发布说明与迭代记录的统一同步
2. 整理并验证本次成功 run 的 APK 产物获取链路，必要时补充备用下载说明
3. 继续推进下一批阅读体验增强项，包括更完整的书架视觉、详情页层次和网文目录交互
4. 升级 GitHub Actions 里依赖的 Node 20 action，并补充无本地 Java 环境下的云端验证说明
