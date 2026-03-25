# WenwenToMe

本地书籍阅读器和网文阅读器二合一的 Android 应用。

> 当前仓库已决定切换到 `GPL-3.0` 路线，并启动 `2.0` 底座迁移。`1.x` 产品线冻结在 `v1.6`，后续阅读与书源能力将基于 Legado 上游继续演进。

## 当前状态

- `1.x` 旧线：
  - 最新稳定版为 `v1.6`
  - 保留旧版 APK 下载与 Git 历史
  - 不再继续扩展自研 TXT / EPUB / 书源 / 阅读器内核
- `2.x` 新线：
  - 迁移到 Legado 作为阅读与书源底座
  - 保留 WenwenToMe 的 GitHub 仓库、包名、发版链路与品牌
  - 后续把 WenwenToMe 的 AI Hub 与 GitHub 同步能力接回新底座

## 迁移决策

- 继续使用当前 GitHub 仓库：`https://github.com/yuelangmanle/wenwentome1`
- 仓库 License 改为 `GPL-3.0`
- 上游底座采用 `Legado`
- 当前主迁移目标版本为 `2.0.0`
- 旧线冻结说明见：`docs/migration/legacy-line-freeze.md`

## 下载

- 正式版本请前往 GitHub Releases 页面下载 `release APK`
- Release 页面：`https://github.com/yuelangmanle/wenwentome1/releases`
- 发布页：`https://yuelangmanle.github.io/wenwentome1/`
- 旧线正式版：`v1.6`
- 新底座当前正式版：`v2.0.0`

## 版本信息

- 当前稳定旧版：`1.6`
- 当前新底座版本：`2.0.0`
- 作者：`月亮满了`
- 项目地址：`https://github.com/yuelangmanle/wenwentome1`
- 上游来源：`https://github.com/gedoor/legado`
- License：`GPL-3.0`

## 旧版使用说明

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

## License 与来源

- 本仓库后续发布内容遵循 `GPL-3.0`
- 新底座迁移基于 `Legado` 上游项目开展
- 第三方来源与说明见 `NOTICE-THIRD-PARTY.md`

## 开发与发版

- 发版流程见 [docs/release-process.md](docs/release-process.md)
- 开发规范见 [docs/development-handbook.md](docs/development-handbook.md)
- 迭代规则见 [docs/iteration-rules.md](docs/iteration-rules.md)
- 开发进度见 [docs/development-progress.md](docs/development-progress.md)
- API Hub 路线图见 [docs/api-hub-roadmap.md](docs/api-hub-roadmap.md)
- GitHub Pages 发布页源文件位于 `site/`
- 旧线冻结说明见 [docs/migration/legacy-line-freeze.md](docs/migration/legacy-line-freeze.md)
