# API 中心与阅读体验增强设计

## 1. 背景

WenwenToMe 当前已经具备以下基础：

- 统一书库，支持 `TXT / EPUB / WEB`
- 书架、详情页、阅读页三条主链
- 书源导入、发现页搜索、加入书库
- GitHub 私有仓库同步、App 内更新日志、GitHub Pages 发布页
- GitHub Actions 云端验证链路

但当前产品仍存在两类明显缺口：

1. 阅读体验仍有未收口项
   - 阅读模式切换时不能稳定保留当前阅读位置
   - EPUB 真实样本回归覆盖仍不够宽
   - 书架筛选 / 排序能力已有底层，没有完整 UI
2. API 能力还没有形成统一底座
   - 后续计划接入阅读助手、翻译、TTS、封面增强、元数据增强、网文增强
   - 如果直接按功能逐个接入，会重复实现密钥管理、预算限制、回退逻辑、同步策略和错误处理

因此本轮设计不只是在现有阅读器上再堆几个联网功能，而是要建立一个可以长期扩展的 `API 中心（API Hub）` 底座，并把前面尚未收口的阅读体验一并纳入第一阶段。

## 2. 本轮目标

### 2.1 产品目标

- 建立统一的 `API 中心`，支持用户自填 API
- 同时兼容 `OpenAI 兼容协议` 与 `国内主流平台`
- 支持每个能力单独绑定 `provider + model`
- 支持本地保存与加密同步两种密钥策略
- 支持预算保护、超额保护、失败回退
- 把阅读体验未收口项与第一阶段 API 底座一起推进

### 2.2 明确已确认的用户决策

- API Key 由用户自己填写
- API Key 存储同时支持：
  - `仅本地保存`
  - `加密后同步`
- 提供商生态同时支持：
  - `OpenAI 兼容协议`
  - `国内主流平台`
- 每个能力可以单独配置：
  - `provider`
  - `model`
- 需要预算保护、超额保护、自动降级与回退策略
- 后期预留功能必须在设计文档中明确列出，避免后续上下文压缩导致丢失

## 3. 范围拆分

本项目拆为两个层次推进，但共用同一套配置和数据模型。

### 3.1 基础层：API 中心

负责以下职责：

- Provider 管理
- API Key 存储与校验
- 模型清单与能力绑定
- 预算策略
- 调用日志
- 失败回退
- 本地缓存
- 同步策略

### 3.2 能力层：API 功能插件

第一阶段和后续阶段的 AI / API 功能都作为能力插件挂在 API 中心上，包括：

- 阅读助手
- 章节总结
- AI 翻译
- TTS 朗读
- 封面增强
- 元数据增强
- 网文搜索增强
- 书源健康评分
- 自动切换源
- AI 分类与推荐

## 4. 信息架构

### 4.1 导航位置

`API 中心` 放在“我的”页里作为一级入口，但具体能力入口仍放在各自业务场景中。

导航结构建议如下：

- `我的 -> API 中心`
- `API 中心 -> 总览`
- `API 中心 -> Provider 管理`
- `API 中心 -> 模型与能力绑定`
- `API 中心 -> 密钥与同步`
- `API 中心 -> 预算与回退策略`
- `API 中心 -> 调用记录`
- `API 中心 -> 实验功能`

### 4.2 页面职责

#### 总览

展示当前全局状态：

- 已启用 provider 数量
- 已绑定能力数量
- 今日调用次数
- 预算使用率
- 最近错误与回退状态

#### Provider 管理

支持：

- 新增 provider
- 编辑 provider
- 禁用 / 启用 provider
- 选择模板：
  - OpenAI 兼容
  - 国内平台预设
  - 完全自定义

#### 模型与能力绑定

为每个能力单独绑定：

- 主 provider
- 主 model
- 备用 provider
- 备用 model
- 是否启用

#### 密钥与同步

每个 provider 单独管理：

- API Key
- 保存方式
  - 仅本地
  - 加密同步
- 最近校验结果
- 最近校验时间

#### 预算与回退策略

支持设置：

- 日预算
- 月预算
- 高成本调用前确认
- 超额后动作
  - 降级
  - 暂停
  - 询问用户

#### 调用记录

用于排查：

- 哪个能力调用了哪个模型
- 是否成功
- 耗时
- 预估成本
- 错误信息

#### 实验功能

用于承载后续高阶能力，不污染主配置流。

### 4.3 能力入口位置

#### 阅读页

- 阅读助手
- 段落解释
- 阅读问答
- 章节总结
- 翻译
- TTS

#### 书籍详情页

- 封面增强
- 元数据补全
- 作者介绍
- 推荐书籍

#### 发现 / 网文页

- 智能搜索增强
- 书源健康评分
- 自动切换源
- 更新提醒增强

#### 书架页

- 筛选 / 排序 UI
- AI 分类建议
- 标签建议
- 书单整理建议

## 5. 能力矩阵

### 5.1 第一批落地能力

#### API 能力

- 阅读助手
- 章节总结
- AI 翻译
- TTS 朗读
- 封面增强
- 元数据增强

#### 阅读体验增强

- 阅读模式切换保留当前位置
- EPUB 真实样本回归与正文入口验证
- 书架筛选 / 排序 UI
- `TXT / EPUB / WEB` 三条 app 级 smoke test 扩宽

### 5.2 第二批增强

- 网文智能搜索增强
- 书源健康评分
- 自动切换源
- 更新提醒增强
- AI 分类建议
- 智能推荐

### 5.3 后期保留功能

以下功能明确记录进本设计，后续即使上下文压缩也不能丢：

- 整本书知识卡片
- 多轮阅读对话
- 长篇上下文记忆
- AI 书单策展
- 跨书语义检索
- AI 笔记系统
- 朗读声音管理
- OCR 封面与信息识别
- 云端能力模板导入
- API 成本分析面板
- 离线缓存复用
- 敏感能力开关

## 6. 数据模型

### 6.1 ApiProvider

字段建议：

- `id`
- `name`
- `kind`
  - `OPENAI_COMPATIBLE`
  - `DOMESTIC_PRESET`
  - `CUSTOM`
- `baseUrl`
- `enabled`
- `supportsChat`
- `supportsTts`
- `supportsEmbeddings`
- `supportsImage`

### 6.2 ApiCredential

字段建议：

- `providerId`
- `apiKeyEncrypted`
- `storageMode`
  - `LOCAL_ONLY`
  - `SYNC_ENCRYPTED`
- `lastVerifiedAt`
- `isValid`

### 6.3 ApiModel

字段建议：

- `id`
- `providerId`
- `modelId`
- `label`
- `capabilities`
- `costLevel`
  - `LOW`
  - `MEDIUM`
  - `HIGH`
- `contextWindow`

### 6.4 ApiCapabilityBinding

字段建议：

- `capabilityId`
- `providerId`
- `modelId`
- `fallbackProviderId`
- `fallbackModelId`
- `enabled`

### 6.5 ApiBudgetPolicy

字段建议：

- `dailyLimit`
- `monthlyLimit`
- `warnThreshold`
- `requireConfirmAboveCost`
- `overBudgetAction`
  - `DOWNGRADE`
  - `PAUSE`
  - `ASK`
- `fallbackToLowerCostModel`

### 6.6 ApiInvocationLog

字段建议：

- `id`
- `capabilityId`
- `providerId`
- `modelId`
- `bookId`
- `chapterRef`
- `status`
- `durationMs`
- `estimatedCost`
- `createdAt`
- `errorMessage`

## 7. 安全与同步

### 7.1 密钥存储规则

支持两种模式：

- `仅本地保存`
- `加密后同步`

### 7.2 硬性约束

- 如果用户没有设置同步加密密码，则不允许把 API Key 走 GitHub 同步
- 明文 API Key 不进入 GitHub 同步链路
- 调用日志默认不保存敏感正文，只保存能力、模型、时间、状态、成本和错误信息

### 7.3 同步边界

允许同步的对象：

- Provider 定义
- 模型绑定
- 预算策略
- 允许同步的加密凭据

默认不同步：

- 本地独占密钥
- 临时测试状态
- 最近调用的正文内容

## 8. 预算、回退与缓存

### 8.1 预算保护

默认三层保护：

- 软提示
- 硬限制
- 自动降级

### 8.2 回退链

每次能力调用统一走以下链路：

1. 主 provider + 主 model
2. 备用 provider + 备用 model
3. 返回错误给 UI
4. 写入调用日志

### 8.3 缓存策略

- 章节总结按 `bookId + chapterRef + promptVersion` 缓存
- 翻译按 `paragraphHash + targetLanguage + modelBinding` 缓存
- 元数据增强按 `bookId + sourceSnapshot` 缓存
- 封面候选按 `book title + author` 缓存

## 9. 模块落位

### 9.1 现有模块扩展

- `core:model`
  - 新增 API 中心模型
- `core:database`
  - 新增 Room 表、DAO、缓存表、密钥存储适配
- `feature:settings`
  - API 中心入口与管理页
- `feature:reader`
  - 阅读助手、翻译、TTS、章节总结、阅读体验增强
- `feature:library`
  - 筛选 / 排序 UI、AI 分类建议、封面增强入口
- `feature:discover`
  - 网文搜索增强、书源健康评分、自动切源、更新提醒增强
- `sync:github`
  - 加密同步 API 中心配置

### 9.2 架构原则

- 不另起第二套阅读链路
- 不绕开现有 `BookRecord / ReadingState / RemoteBinding`
- 所有 API 能力通过统一绑定与统一预算中心接入
- 业务页面只声明“我要什么能力”，不直接管理 provider 与密钥细节

## 10. 分阶段实施计划

### 阶段 1：API 底座 + 阅读体验收口

包含：

- API 中心数据模型
- 数据库存储
- Provider 管理
- 模型绑定
- 密钥与同步策略
- 预算与回退
- 阅读模式切换保位
- EPUB 真实样本回归补强
- 书架筛选 / 排序 UI
- `TXT / EPUB / WEB` smoke test 扩宽

### 阶段 2：阅读器 AI 能力

包含：

- 阅读助手
- 段落解释
- 阅读问答
- 章节总结
- 翻译
- TTS
- 结果缓存

### 阶段 3：书籍信息增强

包含：

- 封面增强
- 元数据补全
- 作者简介
- 标签建议
- 相关推荐
- 书架 AI 分类建议

### 阶段 4：网文增强

包含：

- 智能搜索增强
- 书源健康评分
- 自动切换源
- 更新提醒增强
- 实验功能入口

## 11. 测试策略

### 11.1 单元测试

覆盖：

- Provider 管理
- 密钥保存与同步策略
- 预算限制
- 回退策略
- 缓存命中与失效

### 11.2 UI 测试

覆盖：

- API 中心管理页
- 模型绑定页
- 阅读页 AI 入口
- 书架筛选 / 排序

### 11.3 app 级流程测试

覆盖：

- `TXT`
- `EPUB`
- `WEB`

三条主链都必须有 smoke flow，而不是只压 `WEB`。

### 11.4 云端验证

- 继续以 GitHub Actions 为准
- 本地无 Java 时不声称 Gradle 已通过

## 12. 风险与约束

### 12.1 主要风险

- API 提供商差异大，模型能力与接口风格不统一
- 用户自填 API 带来配置错误和可用性问题
- 预算与成本控制如果没有统一底座，后期极易失控
- 阅读器体验项如果继续后置，会直接影响第一批 API 能力的用户感知

### 12.2 当前不纳入本轮

- PDF 阅读
- 真正的在线支付或充值体系
- 服务器托管式统一中转

## 13. 结论

本轮不应把 “API 功能” 当成若干零散外挂，而应先建立统一 `API 中心`，再把阅读助手、TTS、翻译、封面增强、元数据增强和网文增强按能力插件方式挂接。

同时，阅读体验未收口项必须进入第一阶段，而不是继续后置。否则即便 API 功能接入成功，用户对核心阅读体验的感知仍会被模式切换丢位、EPUB 正文入口不稳和书架组织能力不足所削弱。
