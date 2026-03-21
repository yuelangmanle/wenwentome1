# WenwenToMe

本地书籍阅读器和网文阅读器二合一的 Android 应用。

## 当前能力

- 支持 TXT / EPUB 本地导入、入库与阅读
- 支持发现页搜索网文、导入书源并加入统一书库
- 支持统一书架、轻拟物书本卡片、继续阅读卡与新版书籍详情页
- 支持 GitHub 私有仓库同步、备份与恢复
- 支持在 App 内查看完整更新日志

## 下载

- 正式版本请前往 GitHub Releases 页面下载 `release APK`
- Release 页面：`https://github.com/yuelangmanle/wenwentome1/releases`
- 发布页：`https://yuelangmanle.github.io/wenwentome1/`
- 正式发版 tag 采用 `v<major>.<minor>`，例如 `v1.0`

## 版本信息

- 当前正式版本：`1.1`
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
- GitHub Pages 发布页源文件位于 `site/`
