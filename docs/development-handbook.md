# 开发书

## 项目目标

WenwenToMe 是一个统一支持本地书和网文的 Android 阅读器，第一阶段重点是统一书库、阅读体验和 GitHub 私有仓库同步，并通过 API Hub 统一管理 AI 能力、预算与调用记录。

## 模块边界

- `app`
  - App 壳、导航、应用级依赖装配
- `core:model`
  - 统一数据模型
- `core:database`
  - Room、DAO、DataStore
- `data:localbooks`
  - TXT / EPUB 导入与解析
- `data:apihub`
  - API Hub 数据与调用管理
- `bridge:source`
  - 书源桥接、规则解析、网文抓取
- `feature:library`
  - 统一书库
- `feature:discover`
  - 网文发现、书源管理、加入书库
- `feature:apihub`
  - API Hub 总览、Provider 与模型管理
- `feature:reader`
  - 详情页、阅读页、阅读状态
- `feature:settings`
  - 同步设置、关于与项目、更新日志
- `sync:github`
  - GitHub 私有仓库同步

## 开发约束

- 优先保持“统一书库”主模型，不把书源对象抬成顶层产品对象
- 编辑文件使用 `apply_patch`
- 搜索优先使用 `rg`
- 不误碰用户提供的 `书源.json` 与 `搜书大师_v23.3.apk`
- 本地缺少 JRE/JDK 时，不声称 Gradle 本地通过

## 提交习惯

- 以小步提交为主
- 每个提交只覆盖一个明确主题
- 先有测试，再写实现

## 当前发版约束

- 本地不做正式打包
- 正式版通过 GitHub Actions 产出
- 版本号、tag、CHANGELOG、App 内 changelog 必须保持一致
- GitHub Pages 发布页源文件统一放在 `site/`
- 发版前先跑 `python3 scripts/release_metadata.py validate-pack app/build.gradle.kts CHANGELOG.md app/src/main/assets/changelog.json README.md`
