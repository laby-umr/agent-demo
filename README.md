# 智能文档问答 Agent — 技术笔试作业

> **岗位**：Agent 开发工程师（大模型方向）  
> **输入文档**：作业附件 `GBT 1568-2008 键 技术条件.pdf`（扫描版，通过页面上传，不入库）  
> **预计用时**：4–6 小时（实际开发 + 文档约 8–10 小时，含 RAG 增强与联调）

最小可运行的「PDF 解析 → 结构化入库 → 混合检索 → Grounded 问答 → 自检/拒答」原型。

---

## 作业要求达成情况（自评）

| 作业目标 | 状态 | 说明 |
|----------|------|------|
| 1. PDF 类型判断与解析策略 | ✅ | `PdfTypeDetector`：TEXT / SCANNED / HYBRID |
| 2. 正文 / 条款 / 表格提取 | ⚠️ | 文本层 PDFBox + 条款正则；表格多空格解析 + 行级索引；**扫描件需 MinerU 或 Vision OCR** |
| 3. 构建可检索知识库 | ✅ | H2 分段主数据 + Milvus 向量索引 |
| 4. 问题检索相关证据 | ✅ | Dense + Sparse + RRF + 意图路由 + 上下文回填 |
| 5. 生成答案 + 来源页码/片段 | ✅ | `POST /api/documents/ask`，返回答案、`evidences[]`、`[p.X]` |
| 6. 答案自检 / 拒答 | ✅ | `NoAnswerGuard` + `AnswerVerifier` → `confidence` |
| 7. 测试方法与扩展方案 | ✅ | 单元测试 + eval 问题集 + [docs/TEST.md](docs/TEST.md) |

**结论**：架构与工程闭环**基本达到**作业要求；**扫描 PDF 端到端效果**取决于 OCR/MinerU 是否启用（见下方「已知限制」）。思路、模块划分、测试与扩展设计可支撑面试追问。

---

## 快速启动

### 环境

| 依赖 | 版本 / 地址 |
|------|-------------|
| JDK | 17 |
| Maven | 3.9+ |
| Milvus | `http://localhost:19530` |
| Node.js | 18+（前端） |
| DeepSeek API | Chat 生成 |
| 智谱 API | `embedding-3`（2048 维） |

### 配置密钥（勿提交仓库）

```powershell
$env:DEEPSEEK_API_KEY="sk-..."
$env:ZHIPU_API_KEY="..."
# 可选：复制 application-example.yml 为 application-local.yml
```

### 上传作业 PDF

在前端 **智能文档问答 Agent** 区域选择文件上传，或使用 curl（将路径换成你本地的 PDF）：

```bash
curl -F "file=@/path/to/GBT 1568-2008 键 技术条件.pdf" \
  http://localhost:8050/api/documents/ingest
```

### 启动

```bash
# 后端
mvn spring-boot:run

# 前端（另开终端）
cd frontend && npm install && npm run dev
```

- 后端：http://localhost:8050  
- 前端：http://localhost:5173  

打开页面 → **智能文档问答 Agent** → 上传 PDF → 等待 `COMPLETED` → 提问。

### 命令行演示

```bash
# 健康检查
curl http://localhost:8050/api/health

# 入库（路径换成你的 PDF）
curl -F "file=@/path/to/GBT 1568-2008 键 技术条件.pdf" \
  http://localhost:8050/api/documents/ingest

# 文档问答（作业主接口）
curl -X POST http://localhost:8050/api/documents/ask \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"本标准规定了什么内容？\"}"

# 检索调试
curl "http://localhost:8050/api/documents/search?q=键的技术要求"
```

---

## 系统架构（简图）

```
PDF 上传
  → DocumentParseRouter（MinerU 优先 / 降级 PDFBox+OCR）
  → HierarchicalKnowledgeChunker（Parent-Child + 表格三索引）
  → H2 knowledge_segment + Milvus 向量

用户问题
  → UniversalRetrievalService（意图 → Multi-Query → Dense+Sparse → RRF → 回填）
  → GroundedPromptBuilder → DeepSeek 生成
  → AnswerVerifier / NoAnswerGuard
  → 答案 + confidence + evidences（页码/片段/分数）
```

详细设计见 [docs/DESIGN.md](docs/DESIGN.md)，规格见 [docs/specs/document-agent-spec.md](docs/specs/document-agent-spec.md)。

---

## 项目结构

```
ai-demo/
├── src/main/java/com/example/aidemo/
│   ├── document/          # PDF 检测、解析、入库、问答
│   ├── rag/               # 分块、Milvus 索引、Universal 检索
│   ├── knowledge/         # 召回编排
│   ├── service/           # 通用 Chat（可选 RAG）
│   └── web/               # REST API
├── src/test/              # 单元测试 + eval 问题集
├── frontend/              # Vue3 演示 UI
├── docs/
│   ├── DESIGN.md          # 设计说明（提交用）
│   ├── TEST.md            # 测试说明（提交用）
│   ├── DEMO.md            # 演示截图（代替视频）
│   ├── img/               # 演示截图资源
│   └── specs/             # 实现规格
```

---

## API 一览

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/documents/ingest` | 上传 PDF 入库 |
| GET | `/api/documents/status` | 入库状态 |
| POST | `/api/documents/ask` | **作业文档问答**（含引用与置信度） |
| GET | `/api/documents/search?q=` | 检索调试 |
| POST | `/api/chat` | 通用聊天（可选 Milvus RAG） |

---

## 演示材料（截图）

未录制视频，演示见 **[docs/DEMO.md](docs/DEMO.md)**，截图位于 `docs/img/`：

| 截图 | 内容 |
|------|------|
| [问答2.png](docs/img/问答2.png) | 启动、后端 UP、通用 RAG 问答 |
| [有无引用.png](docs/img/有无引用.png) | PDF 入库 + 有依据问答 + 无答案拒答 + 知识引用 |
| [表格.png](docs/img/表格.png) | 财务报表表格题 + Markdown 表格 + 引用 |

---

## 已知限制与未完成项

| 项 | 原因 | 替代方案 |
|----|------|----------|
| 扫描 PDF 默认无 OCR | 当前 Chat 模型（DeepSeek）无 Vision；`document.ocr.enabled=false` | 启用本地 **MinerU**（`ai.document-parse.mineru.enabled=true`）或换多模态 OCR 模型 |
| Vision OCR 路径 | 已实现 `ScannedPdfExtractor`，需视觉 API | 配置支持 image 的 OpenAI 兼容端点 |
| Rerank | 未接入 cross-encoder | 可调 `top-k`、RRF 权重 |
| 演示视频 | 未录制 | 已用 [docs/DEMO.md](docs/DEMO.md) 截图代替 |

**未硬编码答案**；拒答阈值与检索分数均可通过 `application.yml` 调整。

---

## AI 工具使用说明

使用 **Cursor Agent** 完成：需求拆解 → Spec → 模块实现 → 单测校验 → 文档整理。

- **如何使用**：分模块迭代（解析 / 入库 / 检索 / QA），每步 `mvn test` 验证  
- **如何校验**：单元测试 + 前端 evidences 人工核对页码  
- **如何修正**：根据 `confidence=REFUSED`、evidences 为空或页码错误，调整 OCR 路径、分块策略或检索阈值  

详见 [docs/DESIGN.md#8-ai-工具使用](docs/DESIGN.md#8-ai-工具使用)。

---

## 测试

```bash
mvn test
cd frontend && npm test
```

说明见 [docs/TEST.md](docs/TEST.md)。

---

## 提交清单

- [x] GitHub/Gitee 代码库  
- [x] README（本文件）  
- [x] 设计说明 → [docs/DESIGN.md](docs/DESIGN.md)  
- [x] 测试说明 → [docs/TEST.md](docs/TEST.md)  
- [x] 演示截图 → [docs/DEMO.md](docs/DEMO.md)（代替视频）
- [x] 无 API Key 入库（使用环境变量）

提交邮箱：`yzzhang@kingdomai.com`
