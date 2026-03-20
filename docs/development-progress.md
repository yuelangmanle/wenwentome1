# 开发进度书

## 当前阶段

- 阶段：`1.0` 已正式发布
- 当前目标：在首发版稳定基础上准备 `1.1` 迭代

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

## 当前风险

- 本机仍缺少 Java 运行时，Gradle 验证统一卡在 `Unable to locate a Java Runtime`
- Room 升级仍是 destructive migration
- 书源桥接主路径当前仍以 `LEGACY` 规则为主
- GitHub Actions 仍有 Node 20 deprecation annotation，后续需要升级 action 版本

## 下一步

1. 做一轮 `1.0` 真机安装与阅读流体验复核
2. 规划 `1.1` 的阅读体验和书源桥接增强项
3. 升级 GitHub Actions 里依赖的 Node 20 action
4. 继续补本地 Java 环境外的云端验证说明
