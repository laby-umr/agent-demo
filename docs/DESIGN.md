# 设计说明

> 智能文档问答 Agent — Agent 开发工程师（大模型方向）技术笔试

## 1. 问题拆解

真实交付中，客户 PDF 往往同时具备以下难点：

1. **扫描件无文本层**，不能假设 `copy-paste` 可用  
2. **条款编号 + 表格混排**，扁平分块会丢失结构和页码  
3. **RAG 易检索到页眉标题而非表格数字**（金融/标准文档常见）  
4. **无依据时仍生成**，造成幻觉与合规风险  

本原型按「可解释、可测试、可扩展」原则拆为 **解析 → 结构化分块 → 双路检索 → Grounded 生成 → 自检** 五段，而不是单层 `vectorstore + LLM` 套壳。

---

## 2. 架构与模块

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ PDF 上传     │────▶│ DocumentParseRouter│────▶│ StructuredParse │
└─────────────┘     │ MinerU / Local PDF │     │ Result (elements)│
                    └──────────────────┘     └────────┬────────┘
                                                        │
                    ┌──────────────────┐                ▼
                    │ Hierarchical      │     ┌─────────────────┐
                    │ KnowledgeChunker  │◀────│ 页码/块类型/条款  │
                    └────────┬─────────┘     └─────────────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
     ┌────────────────┐            ┌────────────────┐
     │ H2 segment 表   │            │ Milvus 向量索引 │
     └────────────────┘            └────────────────┘

用户问题 ──▶ QueryIntentClassifier
         ──▶ QueryExpansionService (Multi-Query)
         ──▶ Dense(Milvus) + Sparse(H2 LIKE)
         ──▶ RrfFusion + BlockTypeRouteBoost + RetrievalContentScorer
         ──▶ ContextBackfillService (Parent / 整表回填)
         ──▶ GroundedPromptBuilder
         ──▶ DeepSeek Chat
         ──▶ NoAnswerGuard + AnswerVerifier
         ──▶ DocumentQaResponse
```

### 2.1 解析层 (`document.*`)

| 模块 | 职责 |
|------|------|
| `PdfTypeDetector` | 按页统计可抽取字符数 → TEXT / SCANNED / HYBRID |
| `DocumentParseRouter` | PDF 优先 MinerU；失败降级 `LocalPdfDocumentParseClient` |
| `TextLayerPdfExtractor` | PDFBox 按页抽取 + 条款编号正则 + 表格启发式 |
| `ScannedPdfExtractor` | 页图渲染 + Vision LLM OCR（需多模态模型） |
| `DocumentIngestionService` | 解析 → 分块 → H2 持久化 → Milvus upsert |

### 2.2 分块层 (`rag.chunk`)

- **Parent-Child**：Parent 保留章节上下文（可选 embedding）；Child 为检索单元  
- **表格三索引**：`TABLE_WHOLE` / `TABLE_ROW`（行级 JSON）/ `TABLE_SUMMARY`  
- **PlainTextTableChunkSupport**：PDF 多空格分列表格的行级解析（非 Markdown 表格）

### 2.3 检索层 (`rag.retrieval`)

- **Hybrid**：Milvus 稠密 + H2 关键词稀疏，RRF 融合  
- **意图路由**：`TABLE_CELL` / `FINANCIAL_REPORT` / `SECTION` 等对块类型加权  
- **RetrievalContentScorer**：含数字片段加权、重复页眉降权、内容去重  
- **ContextBackfillService**：table_row 附整表；text child 附 parent 段落  

### 2.4 问答与可靠性 (`document.qa`)

| 组件 | 作用 |
|------|------|
| `GroundedPromptBuilder` | 仅允许依据 `<Reference page="…">` 作答，要求标注 `[p.X]` |
| `NoAnswerGuard` | 检索 top 分低于阈值 → 生成前拒答 |
| `AnswerVerifier` | 答案-证据 token 重叠 + 拒答话术检测 → `HIGH/MEDIUM/LOW/REFUSED` |

---

## 3. 关键取舍

| 决策 | 选择 | 理由 |
|------|------|------|
| 向量库 | Milvus + H2 分段表 | 向量检索与结构化 metadata 分离，便于调试 segmentId |
| Embedding | 智谱 embedding-3 (2048) | 中文语义效果较好；维度变更需重建 collection |
| Chat | DeepSeek API | 稳定、低成本；**无 Vision** → 扫描 OCR 需另路 |
| 扫描 PDF | MinerU 优先，Vision OCR 备选 | 作业禁止手工录入；MinerU 对中文扫描+表格更稳 |
| Agent 框架 | AgentScope 2（Milvus 集成）+ 自研 Universal RAG | 作业 RAG 走显式管线，便于解释引用与拒答；通用 chat 仍可用 AgentScope Knowledge |
| 拒答策略 | strict | 金融/合规场景默认「无依据不答」 |

---

## 4. 边界情况处理

### 4.1 扫描 PDF

- 检测为 `SCANNED` 时走 OCR 分支  
- **当前默认**：`document.ocr.enabled=false`（DeepSeek 不支持 image）  
- **推荐**：启动 MinerU，`ai.document-parse.mineru.enabled=true`  

### 4.2 OCR 错误

- 不过度清洗 OCR 文本，保留原始 chunk 便于人工核对  
- 检索多路 + 多 chunk 部分容错；低重叠降置信或拒答  

### 4.3 表格

- 启发式 `contentType=table`  
- Markdown `\|...\|` 与 PDF 多空格两种解析路径  
- 查询意图为表格/财务时提高 `TABLE_ROW` / `TABLE_WHOLE` 权重  

### 4.4 无答案 / 离题

- 检索分数 `< min-answer-score` → `NoAnswerGuard` 拒答  
- 生成后 `AnswerVerifier` 检测「无法回答」类话术与证据重叠不足  

### 4.5 重复页眉误导

- 标题块在多页重复导致检索偏前页 → `RetrievalContentScorer` 降权无数字短标题、提升含数字片段  

---

## 5. 业务场景扩展

| 场景 | 扩展方式 |
|------|----------|
| **金融报表** | `FINANCIAL_REPORT` 意图 + 数字加权；PDF 用 MinerU；调大 `top-k` |
| **合规合同** | 强化条款编号解析；拒答策略保持 strict；审计 log 保留 segmentId |
| **多文档** | `KnowledgeDocumentEntity.docId` 过滤；API 增加 docId 参数 |
| **客户交付** | MinerU/OCR 可配置；embedding/chat 换私有部署；eval 集按客户文档维护 |
| **质量回归** | `gbt1568-questions.json` + CI 跑 `mvn test`；可选 live eval profile |

---

## 6. 配置说明

主配置：`src/main/resources/application.yml`（**无密钥**，用环境变量）

| 变量 | 含义 |
|------|------|
| `DEEPSEEK_API_KEY` | Chat API |
| `ZHIPU_API_KEY` | Embedding API |
| `MILVUS_URI` | Milvus 地址 |

扫描 PDF 关键项：

```yaml
document.ocr.enabled: false          # Vision OCR 开关
ai.document-parse.mineru.enabled   # MinerU 开关
```

---

## 7. 完成情况诚实说明

| 功能 | 完成度 |
|------|--------|
| PDF 类型检测 | 完成 |
| 文本层解析 + 条款/表格启发式 | 完成 |
| MinerU 集成（降级链） | 完成（需本地部署 MinerU） |
| Vision OCR | 代码完成，默认关闭 |
| Universal Hybrid RAG | 完成 |
| Grounded 问答 + 页码引用 | 完成 |
| 拒答 / 置信度 | 完成 |
| Vue 演示 UI + evidences 面板 | 完成 |
| 单元测试 + eval 问题集 | 完成 |
| 扫描 PDF 端到端（无 MinerU/OCR） | **未完成** — 需用户启用解析引擎 |
| Cross-encoder Rerank | 未做 |
| 演示材料 | 已用截图，见 [DEMO.md](DEMO.md) |

---

## 8. AI 工具使用

- **工具**：Cursor Agent（Claude / Composer）  
- **流程**：作业要求拆解 → 写 Spec → 分模块实现 → 单测 → 联调 → 文档  
- **校验**：不直接信任生成代码；每个模块有对应 `*Test.java`；问答结果看 `evidences` 页码是否与原 PDF 一致  
- **修正示例**：金融 PDF 检索命中页眉 → 加 `RetrievalContentScorer`；表格无行索引 → 加 `PlainTextTableChunkSupport`  

---

## 9. 依赖问题记录

| 问题 | 排查 | 替代 |
|------|------|------|
| DeepSeek 无 Vision | OCR API 400 | 改用 MinerU 或 LM Studio 多模态 |
| Milvus 维度变更 | embedding 2048 维与旧 collection 不一致 | 重建 `ai_demo_embedding3` 并重新 ingest |
| 扫描 PDF chunk 为空 | `pdfType=SCANNED` 且 OCR 关闭 | 开启 MinerU 或 OCR |
| 本地推理慢 | — | 换云端 API；调大 timeout（已设 600s） |

---

## 10. 演示截图

界面效果见 [DEMO.md](DEMO.md)：

![表格问答示例](img/表格.png)

| 文件 | 说明 |
|------|------|
| `img/有无引用.png` | PDF 入库、有依据问答、无答案拒答、知识引用面板 |
| `img/表格.png` | 财务报表表格题，Markdown 表格 + `[p.X]` |
| `img/问答2.png` | 后端 UP、通用 RAG 问答与检索调试 |
