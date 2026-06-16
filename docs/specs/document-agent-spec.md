# 智能文档问答 Agent — 实现规格

> 版本：2026-06-12 · 对齐当前代码实现

## 1. 范围

在 Spring Boot + AgentScope 2 + Milvus 上实现作业要求的 7 步闭环，输入为 `GBT 1568-2008 键 技术条件.pdf`。

## 2. 非功能需求

- 可本地复现启动（JDK 17 + Milvus + API Key）  
- 不在仓库提交密钥  
- 问答返回答案、置信度、证据列表（页码/片段/分数）  
- HTTP 超时 ≥ 120s（扫描/OCR 场景 600s）  

## 3. 解析规格

### 3.1 PDF 类型

```
每页 extract 字符数 >= 40 → 计为文本页
文本页 = 0        → SCANNED
文本页 = 总页数   → TEXT
否则              → HYBRID
```

### 3.2 解析路由

```
if MinerU available && engine in (auto, mineru):
    try MinerU
    on fail/empty → local PDF
else:
    local PDF (PDFBox [+ OCR if SCANNED/HYBRID])
```

### 3.3 块类型

- `paragraph` / `table`（启发式：多空格、制表符）  
- 条款编号：行首 `\d+(\.\d+)*`  

## 4. 分块规格

| chunkLevel | blockType | embed | 说明 |
|------------|-----------|-------|------|
| PARENT | TEXT | 可选 | 章节聚合 |
| CHILD | TEXT | 是 | 检索单元，带 pageStart |
| CHILD | TABLE_WHOLE | 是 | 整表 |
| CHILD | TABLE_ROW | 是 | 行 JSON |
| CHILD | TABLE_SUMMARY | 是 | 列名 + 行数摘要 |

表格行解析：

1. Markdown `\| col \| col \|`  
2. 降级：多空格分列（`PlainTextTableChunkSupport`）  

## 5. 入库规格

1. 清空上次 ingest 的 H2 文档/分段  
2. 写入 `knowledge_document` + `knowledge_segment`  
3. Milvus upsert，payload 含 `segmentId`  
4. 状态机：`RUNNING → SPLITTING → EMBEDDING → COMPLETED | FAILED`  

## 6. 检索规格

```
topK_default = 8
searchTopK = min(40, topK * retrievalFactor)

for each query variant:
    denseIds = Milvus.search(variant, searchTopK)
    sparseIds = H2 LIKE search(variant, searchTopK)
fuse RRF(dense, sparse)
boost by QueryIntent × BlockType
adjust by RetrievalContentScorer
backfill context
return topK hits
```

### 6.1 意图

| Intent | 触发示例 |
|--------|----------|
| SECTION | 第X章、条款 |
| TABLE_OVERVIEW | 总结、有哪些 |
| FINANCIAL_REPORT | 财务报表、YYYY年MM月DD日期间 |
| TABLE_CELL | 多少、金额、公差、账面 |
| ENTITY | 标准号、长数字 |
| GENERAL | 其他 |

## 7. 问答规格

### 7.1 请求

`POST /api/documents/ask`

```json
{ "question": "...", "sessionId": "optional" }
```

### 7.2 响应

```json
{
  "answer": "string，末尾 [p.X]",
  "confidence": "HIGH|MEDIUM|LOW|REFUSED",
  "refusalReason": "string|null",
  "evidences": [{
    "segmentId": 1,
    "page": 3,
    "snippet": "...",
    "contextText": "...",
    "score": 0.85,
    "docId": "gbt1568-2008",
    "blockType": "TEXT"
  }],
  "sessionId": "..."
}
```

### 7.3 拒答条件

- ingest 未完成  
- evidences 为空  
- top score < `min-answer-score`（默认 0.30）  
- 生成答案被 Verifier 判为拒答话术或重叠不足  

## 8. 配置项（摘要）

见 `application.yml`：

- `agentscope.chat.*` — DeepSeek  
- `agentscope.embedding.*` — 智谱 embedding-3  
- `agentscope.milvus.*` — 向量库  
- `document.ocr.*` — Vision OCR  
- `ai.document-parse.mineru.*` — MinerU  
- `ai.knowledge-retrieval.*` — 检索/拒答阈值  

## 9. 测试映射

| 规格项 | 测试 |
|--------|------|
| PDF 类型 | PdfTypeDetectorTest |
| 文本抽取 | DocumentIngestionPipelineTest |
| 表格分块 | HierarchicalKnowledgeChunkerTest |
| RRF | RrfFusionTest |
| 财务检索加权 | RetrievalContentScorerTest |
| 拒答 | NoAnswerGuardTest, AnswerVerifierTest |
| 端到端 eval | gbt1568-questions.json + 前端/curl 人工验证 |

## 10. 已知偏差

- 作业要求扫描 PDF：生产路径需 MinerU/OCR，默认配置下仅保证**架构与文本层 PDF** 闭环  
- `/api/chat` 使用 AgentScope SimpleKnowledge，与 `/api/documents/ask` 的 Universal RAG **相互独立**  
