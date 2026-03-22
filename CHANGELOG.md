# Changelog

All notable changes to this project will be documented in this file.

## [1.2] - 2026-03-23

### Added
- API Hub 总览页、Provider 管理、模型绑定、预算/失败回退、价格目录与调用记录页
- API Hub 能力：预算保护、价格覆盖、fallback 日志、usage log 与数据库迁移
- App 内 API Hub 子路由接通
- TXT / EPUB / WEB 三条阅读链路回归，阅读模式切换保留当前位置与阅读进度
- EPUB 自动跳过封面页、导航页、目录页并优先正文
- 书架新增筛选与排序
- 阅读器目录 / 进度 / 左右翻页 / 上下滚动体验收口
- 阅读器内置 AI 面板，支持章节总结、段落解释、AI 翻译与 TTS 文本朗读入口
- AI 面板支持 API Hub 绑定驱动、结果缓存复用、错误态与未配置提示
- 详情页新增 AI 补全入口，支持简介/作者/标签优化与推荐封面建议
- 封面管理支持自动刷新、手动导入照片与恢复自动封面
- 元数据增强结果缓存
- 网文增强：书源健康分、发现页健康 badge、搜索排序增强与自动切源提示
- 刷新目录失败自动切换候选书源并同步更新状态

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
