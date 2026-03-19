# 开发进度书

## 当前阶段

- 阶段：`1.0` 首发发布包收口中
- 当前目标：把现有 MVP 补成可正式发版的完整版本

## 已完成

- 统一书库、本地 TXT / EPUB 阅读
- 详情页与阅读状态持久化
- 发现页、书源导入、加入书库
- GitHub 私有仓库同步 MVP
- Discover 搜索竞态与重复入库硬化
- 旧快照书源字段兼容修复
- App 内 changelog 基础
- “我的”页关于与项目入口
- tag 驱动 release workflow 基础

## 当前风险

- 本机仍缺少 Java 运行时，Gradle 验证统一卡在 `Unable to locate a Java Runtime`
- Room 升级仍是 destructive migration
- 书源桥接主路径当前仍以 `LEGACY` 规则为主

## 下一步

1. 完成剩余 UI 静态复核
2. 在 GitHub 上验证 release workflow
3. 配置 release 签名 Secrets
4. 在 `main` 打 `v1.0` 完成首发
