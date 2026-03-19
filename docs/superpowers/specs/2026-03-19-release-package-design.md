# WenwenToMe 1.0 首发发布包设计文档

## 文档状态

- 状态：已完成交互确认，待用户审阅书面规格
- 日期：2026-03-19
- 适用范围：`1.0` 首发发布包及后续 `+0.1` 版本迭代规则
- 关联设计：
  - [Android 阅读器主设计](/Users/yueliangmanle/Desktop/codex/wenwentome/.worktrees/android-reader-mvp/docs/superpowers/specs/2026-03-18-android-reader-design.md)
  - [信息架构草图](/Users/yueliangmanle/Desktop/codex/wenwentome/.worktrees/android-reader-mvp/.mockups/info-architecture.html)

## 一、目标

本轮不是继续扩张阅读功能，而是把现有 MVP 补成一个可以正式发 `1.0` 的首发包。首发包必须同时满足四类结果：

1. GitHub 上可以通过正式 tag 创建可下载的正式版 Release
2. Android App 内能展示作者、项目链接、当前版本和完整更新日志
3. 仓库内具备给用户和给开发者都能用的完整文档
4. 以后每次发版都有稳定的版本号与更新日志规则，不靠临时约定

## 二、已确认决策

### 2.1 发布方式

- 正式版只在 `main` 分支打 tag 时发布
- Git tag 采用 `v<major>.<minor>` 格式
- `v1.0` 是第一个正式版本
- GitHub Actions 负责构建签名后的 `release APK`
- GitHub Release 页面作为正式发版页

### 2.2 App 内入口

- 不新增单独“关于页”
- 在“我的”页底部新增 `关于与项目` 卡片
- 卡片内展示：
  - 作者名：`月亮满了`
  - 当前版本号
  - GitHub 项目链接
  - 查看完整更新日志入口

### 2.3 更新日志展示方式

- 采用内置 `JSON` 版本历史作为 App 内主数据源
- App 必须离线可查看全部版本更新日志
- GitHub Release 文案与仓库 `CHANGELOG.md` 保持一致，但 App 以结构化 JSON 渲染

### 2.4 实施强度

- 采用“完整首发包”方案
- 本轮同时落地发布链路、App 内品牌信息、版本规则和文档体系
- 不做过度平台化，例如自动语义版本计算、在线拉取 release notes

## 三、范围拆分

本轮实现拆成四个子项目。

### 3.1 子项目 A：正式发版链路

目标：

- 在 GitHub Actions 中增加正式版构建与发布 workflow
- 当 `main` 上打出形如 `v1.0` 的 tag 时：
  - 校验版本号与 tag 一致
  - 构建签名 `release APK`
  - 创建 GitHub Release
  - 上传 `release APK` 作为 Release 附件

约束：

- 本地不进行正式打包
- 签名只走 GitHub Secrets
- 如果 Secrets 未配置，workflow 应明确失败并给出说明

### 3.2 子项目 B：App 内版本与项目信息

目标：

- 在“我的”页底部加入 `关于与项目` 卡片
- 可展示：
  - App 名称
  - 作者 `月亮满了`
  - 当前版本 `1.0`
  - GitHub 仓库入口
  - 更新日志入口

交互：

- 点击 GitHub 项目可跳转系统浏览器
- 点击更新日志可进入 App 内日志查看页或下拉展开区

本轮推荐：

- “我的”页仍保持单页结构
- `关于与项目` 卡片点击“查看完整更新日志”后进入一个轻量日志页面
- 不新增复杂品牌系统，也不加反馈、评分、检查更新等额外功能

### 3.3 子项目 C：仓库文档体系

目标：

- 补齐面对三类对象的文档：
  - 用户：怎么下载安装、怎么配置、怎么用
  - 发布者：怎么发 `1.0`、怎么准备 keystore 和 secrets
  - 开发者：怎么继续迭代、怎么记进度、怎么写版本日志

文档清单：

- `README.md`
  - 项目介绍
  - 当前能力
  - 快速开始
  - GitHub Release 下载说明
- `CHANGELOG.md`
  - 全量版本更新日志
- `docs/release-process.md`
  - 正式发版步骤
  - tag 规则
  - Secrets 说明
  - GitHub Release 生成规则
- `docs/development-handbook.md`
  - 开发书
  - 模块结构
  - 开发约束
  - 提交流程
- `docs/development-progress.md`
  - 开发进度书
  - 当前完成项
  - 当前风险
  - 下一阶段任务
- `docs/iteration-rules.md`
  - 迭代规则
  - 版本号递增规则
  - 更新日志维护规则
  - 发布前检查规则

### 3.4 子项目 D：版本与日志规则

目标：

- 让 `1.0` 之后的版本演进具有明确规则
- 避免以后版本号、tag、changelog、release note 各写各的

规则：

- 首个正式版为 `1.0`
- 每次正式版增加 `0.1`
- 例子：
  - `1.0 -> 1.1`
  - `1.1 -> 1.2`
  - `1.9 -> 2.0`
- Git tag 与 `versionName` 一一对应：
  - `versionName = 1.0`
  - `tag = v1.0`
- `versionCode` 采用可持续增长整数，不回退
- 每次正式发版都必须同步更新：
  - `app/build.gradle.kts` 中的 `versionName/versionCode`
  - `CHANGELOG.md`
  - `app` 内置 changelog JSON
  - GitHub Release 标题与说明

## 四、详细设计

### 4.1 发布工作流设计

新增一个正式发版 workflow，和现有 CI 并行存在但职责分离：

- 现有 `android-ci.yml`
  - 继续负责普通 push / PR 的测试与 debug 构建
- 新增 `android-release.yml`
  - 只负责 tag 触发的正式发版

触发条件：

- `push.tags: ['v*']`

主要步骤：

1. 检出代码
2. 安装 JDK 和 Android SDK
3. 校验当前分支/tag 来源是否符合 `main` 发布规则
4. 读取 `versionName`
5. 校验 `versionName` 与当前 tag 是否一致
6. 从 GitHub Secrets 还原签名 keystore
7. 写入临时 `keystore.jks`
8. 注入签名配置
9. 执行 `assembleRelease`
10. 生成 GitHub Release
11. 上传 `release APK`
12. 用 `CHANGELOG.md` 对应版本段落作为 release notes

Secrets 约定：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

失败策略：

- 缺任一 Secret 时直接失败
- tag 与 `versionName` 不一致时直接失败
- 未找到对应版本 changelog 时直接失败

### 4.2 App 内“关于与项目”设计

位置：

- “我的”页底部

卡片结构：

- 标题：`关于与项目`
- App 名称：`WenwenToMe`
- 作者：`月亮满了`
- 当前版本：如 `1.0`
- 项目地址按钮：`打开 GitHub 项目`
- 日志按钮：`查看完整更新日志`

实现方式：

- GitHub 链接使用系统 `Intent.ACTION_VIEW`
- 版本号从 `BuildConfig.VERSION_NAME` 读取
- 作者与仓库地址抽到统一常量，避免散落在 UI 层

### 4.3 App 内更新日志设计

本轮采用 `assets/changelog.json`。

结构建议：

```json
[
  {
    "version": "1.0",
    "releaseDate": "2026-03-19",
    "title": "首发版本",
    "highlights": [
      "支持 TXT / EPUB 本地导入与阅读",
      "支持发现页搜索网文并加入统一书库",
      "支持 GitHub 私有仓库同步与恢复"
    ],
    "details": [
      "新增统一书库、详情页和阅读态",
      "新增书源导入、启用/禁用和网文发现流程",
      "新增 GitHub 同步设置、备份与恢复入口"
    ]
  }
]
```

展示原则：

- 默认按版本倒序展示
- 每个版本展示：
  - 版本号
  - 发布日期
  - 摘要标题
  - Highlights
  - 详细变更
- 日志页面支持滚动浏览全部历史版本

后续扩展：

- 可以增加“首次进入新版本时高亮当前版本”
- 可以增加“只看本版本新增”模式
- 但不作为 1.0 的必需项

### 4.4 文档目录设计

建议新增：

- `README.md`
- `CHANGELOG.md`
- `docs/release-process.md`
- `docs/development-handbook.md`
- `docs/development-progress.md`
- `docs/iteration-rules.md`

内容边界：

- `README.md`
  - 对外入口
  - 少讲过程，多讲产品、安装与使用
- `CHANGELOG.md`
  - 对外与对内共用的版本历史
- `release-process.md`
  - 发版实际操作手册
- `development-handbook.md`
  - 长期维护规范
- `development-progress.md`
  - 当前项目状态快照
- `iteration-rules.md`
  - 以后每次发版/迭代的固定规则

### 4.5 版本规则设计

#### 正式版编号

- `1.0` 为首发
- 每次正式版 `minor + 1`
- `minor` 到 `9` 后，下一版进位 `major + 1`

示例：

- `1.0`
- `1.1`
- `1.2`
- ...
- `1.9`
- `2.0`

#### versionCode 规则

- `versionCode` 持续递增整数
- 不使用浮点映射
- 初始建议：
  - `1.0 -> 100`
  - `1.1 -> 110`
  - `1.2 -> 120`
  - `2.0 -> 200`

这样做的原因：

- 和 `versionName` 直觉一致
- 后续能保留中间插入空间
- 不会受字符串解析影响

#### 更新日志规则

每次正式发版前必须同步修改四处：

1. `app/build.gradle.kts`
2. `CHANGELOG.md`
3. `assets/changelog.json`
4. GitHub Release notes

发布后禁止只改其中一处。

## 五、数据流

### 5.1 发布数据流

`main` 打 tag
→ GitHub Actions 读取版本
→ 校验 tag / versionName
→ 构建签名 `release APK`
→ 读取 changelog
→ 创建 GitHub Release
→ 上传 APK

### 5.2 App 内版本信息流

Gradle `versionName`
→ `BuildConfig`
→ “关于与项目”卡片展示

`assets/changelog.json`
→ 日志仓储/解析器
→ 更新日志 UI

### 5.3 文档维护流

版本决策
→ 更新 `CHANGELOG.md`
→ 更新 `changelog.json`
→ 更新 `development-progress.md`
→ 打 tag
→ 自动生成 GitHub Release

## 六、错误处理

### 6.1 Release 构建失败

- 缺少签名 Secret：workflow 直接失败，并在日志中提示缺失项
- tag/version 不一致：直接失败
- changelog 缺对应版本：直接失败
- release APK 未生成：不创建 Release

### 6.2 App 内日志读取失败

- 如果 JSON 解析失败：
  - “更新日志”页面显示降级文案
  - 不影响 App 其他功能
- 如果当前版本未在 JSON 中找到：
  - 仍显示历史版本
  - 当前版本区域显示“日志准备中”

### 6.3 GitHub 跳转失败

- 如果系统无可用浏览器或跳转失败：
  - 给出 toast/snackbar 提示
  - 不造成页面崩溃

## 七、测试策略

### 7.1 App 侧

- 为“关于与项目”卡片增加 UI smoke test
- 为更新日志解析增加单元测试
- 为 GitHub 跳转入口保留可替换 handler，便于测试点击行为

### 7.2 文档与版本校验

- 为 `changelog.json` 增加结构校验测试
- 为版本号/tag 一致性增加 workflow 校验步骤

### 7.3 Release 侧

- 在 GitHub Actions 中分离：
  - CI：测试 + debug
  - Release：tag + release APK + 发布

## 八、非目标

本轮不做：

- 应用内在线检查更新
- Play 商店分发
- AAB 上架链路
- 自动从 Git commit 汇总 changelog
- 多语言文档
- 完整品牌官网或项目主页

## 九、实施顺序

建议按下面顺序进入实现计划：

1. 先补版本规则、README、CHANGELOG、开发文档骨架
2. 再补 App 内“关于与项目”卡片与更新日志页面
3. 再补 `assets/changelog.json` 与对应测试
4. 最后补 GitHub Release workflow 与签名说明

这样做的原因：

- 文档和版本规则先稳定，后面的 workflow 和 App 才有统一数据源
- App 内更新日志与仓库 changelog 可以在同一轮一起接上
- 正式发版链路最后接，能避免 workflow 反复返工

## 十、验收标准

当以下条件全部满足时，本轮算完成：

1. `app/build.gradle.kts` 版本改为 `1.0`
2. 仓库中存在完整 `CHANGELOG.md` 与开发文档
3. App 的“我的”页能看到作者、版本和 GitHub 项目入口
4. App 内能查看全部更新日志
5. `main` 分支打 `v1.0` tag 后，GitHub Actions 能构建正式 `release APK`
6. GitHub 自动创建 Release 页面并附带 APK
7. 后续 `1.1`、`1.2` 的版本/更新日志规则被文档化并可重复执行
