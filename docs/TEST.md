# 测试说明

## 1. 测试分层

| 层级 | 内容 | 命令 |
|------|------|------|
| 单元测试 | 分块、检索融合、意图分类、拒答/验证逻辑 | `mvn test` |
| 前端测试 | API 封装、组件 smoke | `cd frontend && npm test` |
| 集成 / 人工 | PDF 入库 + 问答 + evidences 核对 | 前端或 curl |
| 评估问题集 | 固定问题参考 | `src/test/resources/eval/gbt1568-questions.json` |

---

## 2. 单元测试清单

```
src/test/java/
├── document/detect/PdfTypeDetectorTest.java      # PDF 类型
├── document/ingest/DocumentIngestionPipelineTest # 文本层 smoke PDF
├── document/ingest/BlockChunkMergerTest.java
├── document/qa/AnswerVerifierTest.java           # 证据重叠 / 拒答
├── document/qa/NoAnswerGuardTest.java            # 低分拒答
├── rag/chunk/HierarchicalKnowledgeChunkerTest.java # 表格三索引
├── rag/retrieval/QueryIntentClassifierTest.java
├── rag/retrieval/RrfFusionTest.java
├── rag/retrieval/RetrievalContentScorerTest.java # 页眉 vs 数字表格
└── rag/retrieval/SparseTextNormalizerTest.java
```

运行：

```bash
mvn test
```

期望：全部 PASS（不依赖外部 API / Milvus）。

---

## 3. 评估问题集

文件：`src/test/resources/eval/gbt1568-questions.json`

| ID | 类型 | 问题 | 期望 |
|----|------|------|------|
| q1-body | 正文 | 本标准规定了什么内容？ | 非 REFUSED，有 evidences |
| q2-table | 表格 | 键的尺寸偏差或公差表格中有哪些要求？ | 非 REFUSED |
| q3-clause | 条款 | GBT 1568-2008 的适用范围是什么？ | 非 REFUSED |
| q4-no-answer | 无答案 | 今天上海天气怎么样？ | **REFUSED** |
| q5-no-answer | 无答案 | 请介绍 iPhone 17 的参数 | **REFUSED** |
| q6-vague | 模糊 | 这个文件好不好？ | 不强制（可能 LOW） |

### 人工 eval（演示用）

问题列表见 `src/test/resources/eval/gbt1568-questions.json`。后端已启动且 PDF 已入库后，在前端逐条提问，或：

```bash
curl -X POST http://localhost:8050/api/documents/ask \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"今天上海天气怎么样？\"}"
```

无答案题期望 `confidence=REFUSED`；正文/表格题期望非 REFUSED 且 `evidences` 非空。

---

## 4. 人工测试用例（演示必做 ≥5 题）

### 4.1 启动与入库

1. `GET /api/health` → `status: UP`  
2. 上传 PDF → `GET /api/documents/status` → `state: COMPLETED`  
3. 记录 `pdfType`、`blockCount`、`chunkCount`  

### 4.2 正文问题

```
本标准规定了什么内容？
GBT 1568-2008 适用于哪些键？
```

**通过标准**：`confidence` 非 REFUSED；`evidences` 含 snippet；答案含 `[p.X]`；页码与 PDF 大致一致。

### 4.3 表格问题（必做 1 题）

```
键的尺寸偏差或公差表格中有哪些要求？
标准中表格规定的极限偏差是多少？
```

**通过标准**：答案引用表格相关页；evidences 的 `blockType` 可能为 `TABLE_WHOLE` / `TABLE_ROW`。

### 4.4 无答案问题（必做 1 题）

```
今天上海天气怎么样？
请介绍 iPhone 17 的参数
```

**通过标准**：`confidence=REFUSED` 或明确说明文档无依据；不应编造天气/手机参数。

### 4.5 来源与自检

检查 `POST /api/documents/ask` 响应字段：

```json
{
  "answer": "... [p.3]",
  "confidence": "HIGH",
  "evidences": [
    {
      "page": 3,
      "snippet": "...",
      "score": 0.92,
      "blockType": "TEXT"
    }
  ]
}
```

前端 **evidences 面板** 应展示 snippet 与 score。

---

## 5. 检索调试

不调用 LLM，仅看召回：

```bash
curl "http://localhost:8050/api/documents/search?q=公差"
```

核对：top hits 是否来自含「公差」的页；表格题是否命中 `TABLE_*` 块。

---

## 6. 回归与 OCR 错误

| 风险 | 测试方法 |
|------|----------|
| 改 embedding 维度 | 重建 Milvus collection + 全量 re-ingest |
| 改分块参数 | 跑 `HierarchicalKnowledgeChunkerTest` + 人工 2 题 |
| OCR 错字 | 故意用含错字的 chunk 入库，看检索是否仍能部分命中 |
| 阈值过严导致误拒 | 调低 `min-answer-score`；观察 q1 是否恢复 |

---

## 7. 扫描 PDF 专项

作业 PDF 为**扫描件**，若 `pdfType=SCANNED` 且 `chunkCount` 很小：

1. 开启 MinerU 或 Vision OCR（见 [DESIGN.md](DESIGN.md)）  
2. 重新 ingest  
3. 重复 q1–q5  

**未启用 OCR/MinerU 时**，单元测试仍可通过（使用程序生成的文本层 smoke PDF），但** live 问答效果不能代表扫描件真实能力**——须在提交材料中如实说明。

---

## 8. 演示截图（代替视频）

完整说明见 [DEMO.md](DEMO.md)。

### 8.1 启动与 RAG

![启动与通用 RAG 问答](img/问答2.png)

### 8.2 PDF 入库、有/无依据问答、引用证据

![有依据与无依据问答对比](img/有无引用.png)

- 有依据：账面价值 + `HIGH` + `[p.1]` + 知识引用 8 条  
- 无依据：文档未提及的日期，不编造答案  

### 8.3 表格问题

![表格问答](img/表格.png)

- 财务报表表格以 Markdown 表格输出  
- `confidence=HIGH`，含页码引用  

### 8.4 单元测试

```bash
mvn test
```

截图可在本地终端运行后自行补充；单元测试不依赖 API Key。
