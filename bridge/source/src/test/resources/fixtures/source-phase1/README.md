# Source Phase 1 离线 fixtures

## 目录结构

```
bridge/source/src/test/resources/fixtures/source-phase1/
  index.json                          # manifest 相对路径列表
  <slug>/
    manifest.json                     # 单个目标源的完整 round-trip 描述
  shared/
    css-clean/
      search.html
      detail.html
      content.html
    xpath-basic/
      search.html
      detail.html
      content.html
    json-basic/
      search.json
      detail.json
      content.json
```

`SourceCompatibilityPhase1FixtureTest` 先读取 `index.json`，再逐个加载 `<slug>/manifest.json`。manifest 自己不承载大段 HTML/JSON，而是通过 `requests.*.bodyFile` 指向 `shared/` 下的复用样本，避免 10 个目标源重复复制四套响应体。

## manifest 结构

每个 `manifest.json` 采用统一结构：

```json
{
  "slug": "example-source",
  "sourceId": "https://example.com",
  "sourceName": "示例书源",
  "sampleBookTitle": "示例书名",
  "query": "示例书名",
  "primaryEngine": "HTML_CSS",
  "hasJsonRule": false,
  "hasXPathRule": false,
  "hasCleanerOrScript": true,
  "source": {
    "searchUrl": "https://fixture.wenwentome.test/css/search?title={{key}}",
    "ruleSearch": {},
    "ruleBookInfo": {},
    "ruleToc": {},
    "ruleContent": {}
  },
  "requests": {
    "search": {
      "url": "https://fixture.wenwentome.test/css/search?title=%E7%A4%BA%E4%BE%8B",
      "bodyFile": "shared/css-clean/search.html"
    },
    "detail": {
      "url": "https://fixture.wenwentome.test/css/book/example",
      "bodyFile": "shared/css-clean/detail.html"
    },
    "toc": {
      "url": "https://fixture.wenwentome.test/css/book/example",
      "bodyFile": "shared/css-clean/detail.html"
    },
    "content": {
      "url": "https://fixture.wenwentome.test/css/chapter/1",
      "bodyFile": "shared/css-clean/content.html"
    }
  },
  "expected": {
    "searchTitle": "示例书名",
    "detailTitle": "示例书名",
    "firstChapterTitle": "第一章",
    "firstChapterRef": "https://fixture.wenwentome.test/css/chapter/1",
    "contentSnippet": "真正正文"
  }
}
```

关键字段说明：

- `primaryEngine` 只能使用 `HTML_CSS`、`HTML_XPATH`、`JSON_PATH`
- `hasJsonRule`、`hasXPathRule`、`hasCleanerOrScript` 供覆盖率断言使用
- `source` 存放构造给 `SourceRuleParser` 的简化版 legacy source JSON
- `requests` 定义离线 HTTP 回放 URL 与 fixture 文件映射
- `expected` 定义 round-trip 断言结果

## shared archetype fixtures

当前只有三套共享 archetype：

- `shared/css-clean`：覆盖 CSS selector、属性抽取、`##` 清洗和轻量 `@js:`
- `shared/xpath-basic`：覆盖 XPath 节点文本抽取与正文清洗
- `shared/json-basic`：覆盖 JSONPath 搜索/详情/目录/正文主链

如果后续需要新 archetype，优先新增到 `shared/`，只有在结构明显无法复用时才新增源专属 HTML/JSON 文件。

## 添加新目标源

1. 在 `source-phase1/<slug>/` 下新建 `manifest.json`。
2. 复用现有 `shared/` 响应体，或者先补新的 archetype 样本，再在 `requests.*.bodyFile` 里引用。
3. 把 `<slug>/manifest.json` 追加到 `index.json`。
4. 保持总体覆盖至少满足 4 个 `HTML_CSS`、2 个 `JSON_PATH`、2 个 `HTML_XPATH`，并覆盖 cleaner/script 分支。
5. 运行 `./gradlew :bridge:source:test --tests \"*SourceCompatibilityPhase1FixtureTest\"` 验证 round-trip，再同步更新外层文档。

当前会话缺少 Java Runtime，所以上面的 Gradle 命令还不能在本机完成验证；新增或修改 fixture 后，后续需要在具备 JDK 17 的环境里补跑。
