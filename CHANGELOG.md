# Changelog

All notable changes to this project will be documented in this file.

## [1.1] - 2026-03-21

### Added
- 书架页升级为轻拟物藏书墙布局，继续阅读卡与书本封面卡片统一成更接近实体书的视觉语言
- 书籍详情页升级为四段结构，补齐封面优先展示、目录展开、当前章/最新章识别与封面管理分区
- 新增 GitHub Pages 发布页，用于集中展示版本信息、下载入口、项目链接与发版说明

### Changed
- 继续阅读入口统一锚定 `detail-read-button`，详情页与 app 级回归测试改为稳定 selector 驱动
- Release 元数据脚本新增版本包一致性校验，自动检查 `versionName`、`versionCode`、`CHANGELOG.md`、App 内 changelog 与 README 是否同步
- 补全文档中的“无本地 Java 时走 GitHub 云端验证”的说明

## [1.0] - 2026-03-19

### Added
- 支持 TXT / EPUB 本地导入与阅读
- 支持发现页搜索网文并加入统一书库
- 支持 GitHub 私有仓库同步与恢复

### Changed
- 新增 App 内完整更新日志能力的基础数据源
