# WenwenToMe 浏览器找书开源集成方案

日期：2026-03-26

## 目标

给 `文文tome` 的“浏览器找书 + 优化阅读”确定一条可长期维护的开源集成路线，避免后续继续在浏览器内核、正文提取、阅读模式三件事上重复造轮子。

## 结论

推荐采用“两层方案”：

1. 正文提取层继续使用 `Mozilla Readability.js + DOMPurify`
2. 浏览器壳层维持当前 `Android WebView` 方案先跑通，再把 `GeckoView / Mozilla Android Components` 作为二阶段升级方向

这也是当前仓库里已经落地代码最贴近、成本最低、成功率最高的一条线。

## 推荐方案 A

### A1. 正文提取内核

直接保留并继续强化：

- `mozilla/readability`
- `DOMPurify`

原因：

- `Readability.js` 本身就是 Firefox Reader View 使用的正文提取核心。
- 官方文档明确建议在不可信 HTML 上配合 sanitizer 使用，当前我们已经按这个方向把 `Readability.js + DOMPurify` 放进 `app/src/main/assets/wenwen/browser/`。
- 这条链最适合做“进入网页后自动识别正文，失败时允许手动触发”的小说阅读增强。

适配结果：

- 当前仓库已经接入 `Readability.js`
- 已经加了 `DOMPurify`
- 已经补了目录识别、下一章识别、浏览器书落库、统一缓存页聚合

所以这部分不是重新选型，而是已经确认可继续沿用的正式底座。

### A2. 浏览器承载壳

一期继续用系统 `WebView`。

原因：

- 当前项目已经有成熟 `WebViewActivity`
- 搜索引擎切换、手动/自动优化阅读、目录跳章、缓存接力都已经围绕它接起来了
- 改动最小，能最快把“浏览器找书”从设计稿推进到可运行主链

适合承接的能力：

- 必应 / 百度 / 搜狗 / 搜狐 / 神马 / 谷歌 / 自定义搜索入口
- 网页导航、返回、长按、下载监听
- 注入 `Readability.js` 做正文识别
- 命中正文后跳到统一阅读器

## 推荐方案 B

### B1. 二阶段浏览器升级到底 GeckoView

如果后面你希望“浏览器找书”不只是一个内嵌网页页签，而是更像完整浏览器，再考虑引入：

- `GeckoView`
- `Mozilla Android Components`
- `reference-browser / Firefox Android` 作为架构参考

适合引入的时机：

- 需要更完整的浏览器会话管理
- 需要更强的扩展能力或 WebExtension 兼容
- 需要更细的内容拦截、权限、标签页、地址栏、浏览器组件能力

不建议现在直接整体搬进来的原因：

- 体量大，接入成本显著高于当前 `WebView`
- Firefox Android / Android Components 在 2024 年后已迁入 Mozilla Central，后续跟进成本会更高
- 现阶段你的核心目标是“找书 -> 识别正文 -> 转阅读 -> 统一缓存”，不是单独做一个完整 Firefox 分支

所以更合理的做法不是“整包集成 Fenix”，而是：

1. 先用现有 `WebView + Readability` 跑成品
2. 真遇到 `WebView` 的能力天花板，再局部替换成 `GeckoView`

## 不推荐路线

### 直接拿小型开源浏览器整包改

像一些单独的 WebView 浏览器项目，可以拿来参考菜单、手势、地址栏组织方式，但不建议作为正式底座直接并入。

原因：

- 很多项目只是轻量浏览器壳，强项在浏览器 UI，不在正文识别和阅读链闭环
- 维护活跃度、模块拆分质量、与你当前 Kotlin/Compose/Legado 主线的契合度都不如 Mozilla 这条链

## 对文文tome的落地建议

### 现在就做

1. 保持当前 `WebViewActivity + Readability.js + DOMPurify` 主线
2. 继续强化目录识别、下一章识别、缓存整本策略
3. 补浏览器搜索引擎管理页、浏览器模式设置页、悬浮优化阅读设置
4. 统一浏览器书和书源书的下载入口、缓存页、封面与来源展示

### 后面再做

1. 如果 `WebView` 命中率和交互能力不够，再评估 `GeckoView`
2. 如果要做真正的多标签浏览器，再参考 `reference-browser / Firefox Android`
3. 如果要支持更强网页增强，再研究 WebExtension 兼容层

## 当前项目最终建议

正式推荐：

- 正文识别：`Readability.js + DOMPurify`
- 浏览器壳：当前 `Android WebView`
- 中长期升级预案：`GeckoView + Mozilla Android Components`

不推荐：

- 现在直接把 Firefox Android 整包搬进项目
- 直接拿轻量开源浏览器整仓改造成正文阅读器

## 参考来源

- Mozilla Readability: https://github.com/mozilla/readability
- DOMPurify: https://github.com/cure53/DOMPurify
- GeckoView: https://github.com/mozilla/geckoview
- Firefox for Android / Android Components 总仓说明: https://github.com/mozilla-mobile/firefox-android
