# WenwenToMe

本地书籍阅读器和网文阅读器二合一的 Android 应用。

## 当前能力

- 支持 TXT / EPUB 本地导入、入库与阅读
- 支持发现页搜索网文、导入书源并加入统一书库
- 支持统一书架、轻拟物书本卡片、筛选排序与新版书籍详情页
- 支持 TXT / EPUB / WEB 阅读链路回归，阅读模式切换保留进度
- 支持阅读器目录 / 进度 / 左右翻页 / 上下滚动体验收口
- 支持阅读器 AI 面板（章节总结 / 段落解释 / 翻译 / TTS 入口）并由 API Hub 能力绑定驱动
- 支持详情页 AI 元数据增强、推荐封面与封面管理
- 支持网文书源健康分、搜索排序增强与自动切源提示
- 支持 API Hub 总览、Provider 管理、模型绑定、预算回退、价格目录与调用记录
- 支持 GitHub 私有仓库同步、备份与恢复
- 支持在 App 内查看完整更新日志

## 下载

- 正式版本请前往 GitHub Releases 页面下载 `release APK`
- Release 页面：`https://github.com/yuelangmanle/wenwentome1/releases`
- 发布页：`https://yuelangmanle.github.io/wenwentome1/`
- 正式发版 tag 采用 `v<major>.<minor>`，例如 `v1.4`

## 版本信息

- 当前正式版本：`1.4`
- 作者：`月亮满了`
- 项目地址：`https://github.com/yuelangmanle/wenwentome1`

## 使用说明

### 本地书

1. 在“书库”页点击导入
2. 选择 TXT 或 EPUB 文件
3. 导入后会进入统一书架，并可直接阅读

### 网文

1. 进入“发现”页
2. 搜索书名或先导入书源
3. 点击“加入书库”

### 同步

1. 进入“我的”页
2. 配置 GitHub 仓库信息和 Token
3. 使用“立即备份”或“恢复到本机”

## 开发与发版

- 发版流程见 [docs/release-process.md](docs/release-process.md)
- 开发规范见 [docs/development-handbook.md](docs/development-handbook.md)
- 迭代规则见 [docs/iteration-rules.md](docs/iteration-rules.md)
- 开发进度见 [docs/development-progress.md](docs/development-progress.md)
- API Hub 路线图见 [docs/api-hub-roadmap.md](docs/api-hub-roadmap.md)
- GitHub Pages 发布页源文件位于 `site/`
