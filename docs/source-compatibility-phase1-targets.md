# Source Compatibility Phase 1 目标书源

## 背景
第一阶段的离线验收清单来自仓库根目录的 `书源.json`，筛选原则是：不依赖登录或验证码、主链可落在 HTML/CSS、XPath 或 JSONPath，并且排除 `<js>` 搜索模板。当前 fixture 清单由 `bridge/source/src/test/resources/fixtures/source-phase1/index.json` 固定维护，`SourceCompatibilityPhase1FixtureTest` 会按这个索引逐个回放搜索、详情、目录、正文四段样本。

## 首批 10 个目标源
| slug | sourceId | 站点名 | primaryEngine | 规则特征 | 样本书名 | 当前状态 |
|------|----------|--------|---------------|----------|----------|----------|
| `dandan-fiction` | <https://m.00shu.la/##@遇知> | 蛋蛋小说 | `HTML_CSS` | CSS 主链 + `##` 清洗 + `@js:` 收尾 | 将夜 | 已建样本 |
| `yunzhong-bookstore` | http://m.yunxs.com | 云中书库 | `HTML_CSS` | CSS 主链 + 基础字段抽取 | 将夜 | 已建样本 |
| `xxsy` | https://www.xxsy.net/ | 潇湘书院 | `HTML_CSS` | CSS 主链 + 列表/详情/目录复用 | 将夜 | 已建样本 |
| `xakshu8` | https://m.xakshu8.com | 爱看书吧 | `HTML_CSS` | CSS 主链 + 正文清洗 | 将夜 | 已建样本 |
| `knight-74txts` | https://www.74txts.com | 骑士小说 | `HTML_CSS` | CSS 主链 + cleaner/script 组合 | 将夜 | 已建样本 |
| `tongrenquan` | https://tongrenquan.org | 同人圈网 | `HTML_CSS` | CSS 主链 + 基础 round-trip 覆盖 | 将夜 | 已建样本 |
| `txtduo` | https://www.txtduo.net | ❤️多多书院 | `HTML_XPATH` | XPath 节点抽取 + 文本清洗 | 雪中悍刀行 | 已建样本 |
| `jiangnan-medabc` | https://www.medabc.com.cn | 江南小说 | `HTML_XPATH` | XPath 主链 + 目录/正文断言 | 雪中悍刀行 | 已建样本 |
| `fengdu-halloween` | <https://touchlife.cootekservice.com#🎃> | 🎃疯读小说🎃 | `JSON_PATH` | JSONPath 搜索/详情/目录 + cleaner/script | 疯读测试书 | 已建样本 |
| `fengdu-warmth` | <https://touchlife.cootekservice.com#温暖满怀> | 疯读小说 | `JSON_PATH` | JSONPath 主链 + cleaner/script | 疯读测试书 | 已建样本 |

## 规则覆盖分布
当前 manifest 设计保证 `SourceCompatibilityPhase1FixtureTest` 至少命中以下覆盖面：

- `HTML_CSS` 6 个：蛋蛋小说、云中书库、潇湘书院、爱看书吧、骑士小说、同人圈网。
- `HTML_XPATH` 2 个：❤️多多书院、江南小说。
- `JSON_PATH` 2 个：🎃疯读小说🎃、疯读小说。
- `hasJsonRule` 2 个：两组疯读 JSON fixture。
- `hasXPathRule` 2 个：❤️多多书院、江南小说。
- `hasCleanerOrScript` 至少 2 个：当前 10 个目标都覆盖了 cleaner 或轻量 `@js:` 后处理。

## 与 fixture 目录的对应关系
- 清单入口：`bridge/source/src/test/resources/fixtures/source-phase1/index.json`
- 每个目标源只保留一个 `manifest.json`
- HTML/JSON 响应体优先复用 `shared/css-clean`、`shared/xpath-basic`、`shared/json-basic`
- `bodyFile` 字段可以直接指向 `source-phase1/` 下的共享样本，不要求每个源重复复制四份响应体

## 说明
如果后续阶段新增目标源，必须同步三处：

- 在 `source-phase1/` 下新增目标源 `manifest.json`
- 把 manifest 相对路径追加到 `index.json`
- 更新本文件中的目标源列表与覆盖分布说明

当前环境没有可用的 Java Runtime，因此这些 fixture 尚未在本机会话里重新跑过 Gradle 校验；文档描述的是当前仓库内已经落地的离线样本结构。
