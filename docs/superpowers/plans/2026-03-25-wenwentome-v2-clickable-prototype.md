# WenwenToMe V2 Clickable Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个严格遵循已确认信息架构的高保真可点击网页原型，用于展示 `文文tome` V2 的书架、发现、我的、阅读、详情、下载缓存管理和浏览器找书交互。

**Architecture:** 采用单文件静态原型方案，在一个 HTML 中内聚视觉样式、页面结构和轻量状态机脚本，确保可以通过本地浏览器直接预览。原型以“手机应用容器 + 桌面说明壳层”的方式展示，重点演示页面层级、入口归属、弹层逻辑和关键转场，而不是实现真实数据或联网功能。

**Tech Stack:** HTML5, CSS3, Vanilla JavaScript

---

### Task 1: 梳理原型边界与文件落点

**Files:**
- Create: `docs/superpowers/plans/2026-03-25-wenwentome-v2-clickable-prototype.md`
- Create: `site/wenwentome-v2-prototype.html`
- Reference: `docs/superpowers/specs/2026-03-25-wenwentome-v2-ui-design.md`

- [ ] **Step 1: 确认原型覆盖页面与状态**

需要覆盖的页面与弹层：

```text
书架
发现-书源搜索
发现-浏览器找书
我的
书籍详情
阅读页
下载缓存管理
书架菜单
发现菜单
搜索引擎快速切换面板
下载弹层
阅读页右上角菜单
```

- [ ] **Step 2: 确认原型文件使用单文件结构**

Run: `test -f site/wenwentome-v2-prototype.html || true`
Expected: 文件不存在或为空，准备创建新文件，不影响现有 `site/index.html`

- [ ] **Step 3: 提交边界明确的落点决定**

```bash
git add docs/superpowers/plans/2026-03-25-wenwentome-v2-clickable-prototype.md
git commit -m "docs: add clickable prototype implementation plan"
```

说明：如果当前轮不单独提交计划文档，可在最终实现提交时一并纳入。

### Task 2: 搭建高保真视觉骨架

**Files:**
- Create: `site/wenwentome-v2-prototype.html`

- [ ] **Step 1: 写出基础结构与视口容器**

```html
<div class="prototype-shell">
  <aside class="desktop-panel"></aside>
  <main class="device-stage">
    <section class="phone"></section>
  </main>
</div>
```

- [ ] **Step 2: 写出视觉变量与现代化主题**

```css
:root {
  --bg-0: #f4efe8;
  --bg-1: #efe4d5;
  --ink-0: #1d1d1f;
  --ink-1: #5d5f68;
  --brand: #245d52;
  --brand-2: #d57a4f;
  --card: rgba(255, 250, 244, 0.88);
}
```

- [ ] **Step 3: 加入页面级动态背景与卡片层级**

Run: `open site/wenwentome-v2-prototype.html`
Expected: 页面有明确氛围层次，不是默认安卓或纯白线框稿

### Task 3: 实现页面流转与关键交互状态机

**Files:**
- Modify: `site/wenwentome-v2-prototype.html`

- [ ] **Step 1: 写出最小状态模型**

```js
const state = {
  tab: "shelf",
  discoverMode: "source",
  activeOverlay: null,
  activeReaderBook: null,
  browserEngine: "必应"
};
```

- [ ] **Step 2: 实现底部导航与发现双入口切换**

Run: `open site/wenwentome-v2-prototype.html`
Expected: 点击 `书架 / 发现 / 我的` 可切换，发现页内 `书源搜索 / 浏览器找书` 可切换

- [ ] **Step 3: 实现右上角菜单与面板联动**

```js
function toggleOverlay(name) {
  state.activeOverlay = state.activeOverlay === name ? null : name;
  render();
}
```

- [ ] **Step 4: 实现书架卡片进入阅读页的转场**

Run: `open site/wenwentome-v2-prototype.html`
Expected: 点击书架书封，出现从封面区域展开到阅读页的演示级动效

### Task 4: 实现确认过的页面内容与入口归属

**Files:**
- Modify: `site/wenwentome-v2-prototype.html`

- [ ] **Step 1: 书架页只保留书架语义**

```text
继续阅读
书架分组/列表
右上角菜单：导入书籍、下载缓存管理、批量管理、排序筛选
```

- [ ] **Step 2: 发现页实现双模式**

```text
书源搜索：搜索框、结果列表、进入书籍详情
浏览器找书：搜索引擎标签、浏览器地址栏、网页识别提示、悬浮优化阅读按钮
```

- [ ] **Step 3: 我的页收纳 AI 与同步**

Run: `open site/wenwentome-v2-prototype.html`
Expected: `AI / 同步` 只出现在 `我的` 页，不混到书架首页

- [ ] **Step 4: 细化详情页、下载管理页、阅读菜单**

```text
详情页：封面、作者、简介、来源、加入书架、开始阅读
下载管理：统一缓存列表、封面、来源、进度、范围策略
阅读菜单：书籍详情、下载、换源
```

### Task 5: 加入浏览器找书专属交互

**Files:**
- Modify: `site/wenwentome-v2-prototype.html`

- [ ] **Step 1: 做搜索引擎快速切换面板**

```text
必应 / 百度 / 搜狗 / 搜狐 / 神马 / 谷歌 / 自定义
```

- [ ] **Step 2: 做发现页右上角管理菜单**

```text
书源搜索模式：书源管理、导入书源、启用/禁用、删除
浏览器找书模式：搜索引擎管理、自定义搜索引擎、切换浏览器模式、浏览器找书设置
```

- [ ] **Step 3: 做优化阅读悬浮窗状态演示**

Run: `open site/wenwentome-v2-prototype.html`
Expected: 可以看到自动优化与手动长按优化的说明性状态

### Task 6: 验证原型与交互路径

**Files:**
- Verify: `site/wenwentome-v2-prototype.html`

- [ ] **Step 1: 启动本地静态预览**

Run: `python3 -m http.server 64094`
Expected: 本地可通过 `http://localhost:64094/site/wenwentome-v2-prototype.html` 访问

- [ ] **Step 2: 手工验证关键链路**

```text
书架 -> 三点菜单 -> 下载缓存管理
书架 -> 点击书封 -> 阅读页 -> 菜单 -> 下载
发现 -> 书源搜索 -> 结果 -> 详情 -> 开始阅读
发现 -> 浏览器找书 -> 搜索引擎切换 -> 网页识别 -> 优化阅读
我的 -> AI / 同步
```

- [ ] **Step 3: 整理交付说明**

需要向用户明确：

```text
这是高保真可点击原型，不是最终 Android 实现
当前重点是信息架构、视觉和动效表达
确认后再回写 spec 并进入真实 UI 开发
```
