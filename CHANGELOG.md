# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Changed
- 仓库已确认切换到 `GPL-3.0` 路线，并启动基于 Legado 的 `2.0.0` 底座迁移
- `1.x` 产品线冻结在 `v1.6`，后续不再继续增强旧自研 TXT / EPUB / 书源 / 阅读器内核

### Planned
- 用 Legado 替换当前运行时底座，恢复成熟的 TXT / EPUB / 网文搜索 / 书源管理能力
- 在新底座上接回 WenwenToMe 的 AI Hub 与 GitHub 同步能力
- 恢复云端正式签名发布，并以 `v2.0.0` 作为新产品线首个版本

## [1.6] - 2026-03-25

### Fixed
- TXT 首次打开改为复用解析缓存，并补齐 UTF-16 BOM / 空字节启发式识别，导入中文 TXT 后不再长时间卡住或掉到空正文
- 无章节纯正文 TXT 现在会稳定回退到“正文”章节，避免进入阅读页后显示“暂无正文内容”
- EPUB 导入放宽到“结构脏但可读”也允许入库，同时对真坏文件统一给出“EPUB 文件无效或已损坏”的明确提示

### Changed
- 阅读页改成正文优先布局，目录 / 设置 / AI / 模式入口全部改成按需展开，不再长期挤占正文区域
- 发现页搜索改为“输入”和“提交”分离，新增搜索按钮并支持键盘 Search；书源管理补上删除入口
- “我的”页默认折叠云同步大表单，`API 中心` 首页改成普通用户可读的状态总览
- 当前版本已通过 GitHub Actions 云端 `testDebugUnitTest + assembleDebug` 验证，并准备从 `main` 走正式签名发布

## [1.5] - 2026-03-24

### Fixed
- TXT 导入新增 UTF-8 / GB18030 编码嗅探与章节切分修复，中文 TXT 不再因为编码或标题识别失败导致正文错乱
- TXT 阅读恢复兼容旧版全书段落 locator，同时支持按章节结构化跳转，章节目录与继续阅读不再互相打架
- EPUB 阅读补齐目录损坏/导航异常时的可读章节回退逻辑，详情页元数据识别也同步增强
- 非法 EPUB 导入现在会在入库阶段直接拒绝，避免坏文件进入书架后在阅读链路里崩掉

### Changed
- 本次正式发版重新切回 `origin/main` 云端签名发布，release workflow 使用独立 release keystore secrets

## [1.4] - 2026-03-23

### Added
- 书源兼容底座第一阶段：结构化规则模型、统一规则执行器，以及 HTML/CSS、XPath、JSONPath 三条主链执行能力
- `##` 文本清洗、轻量 `@js:` 字段后处理与 unsupported 规则显式诊断
- 首批 10 个目标源离线 fixture、shared archetype 响应体与 round-trip 验收测试

### Changed
- 详情、目录、正文链路统一改走 bridge 内部规则执行层，不再依赖业务流程里的字符串截断
- Discover 侧对 unsupported 规则和普通请求失败做了区分提示，刷新目录时同步接入 source health 记录
- `publish/main` 与 `main` 均已通过 GitHub Actions 云端 `testDebugUnitTest + assembleDebug` 验证

## [1.3] - 2026-03-23

### Fixed
- 发现页搜索与选书结果改为在 `IO` 线程执行桥接请求，避免网文搜索和加入流程阻塞主线程
- `TXT / EPUB` 阅读进度改为基于全文段落数与窗口起点计算，修复中后段阅读时的百分比失真
- 阅读器顶部进度条、进度文案与设置面板统一跟随当前视口位置更新，不再停留在旧进度
- “读最新章” 写入阅读状态时重置进度百分比，避免沿用旧章节进度

### Changed
- Release 元数据校验补充 `site/index.html` 版本一致性检查
- `android-release` workflow 的手动触发入口收紧为“必须填写已存在且指向当前 `main` 提交的 tag”
- GitHub Actions JavaScript actions 运行时显式切到 Node 24，降低旧运行时退役风险

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
