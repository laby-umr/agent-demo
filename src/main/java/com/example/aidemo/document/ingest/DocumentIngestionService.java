package com.example.aidemo.document.ingest;

import com.example.aidemo.document.parse.DocumentParseRouter;
import com.example.aidemo.document.parse.StructuredParseResult;
import com.example.aidemo.rag.chunk.HierarchicalKnowledgeChunker;
import com.example.aidemo.rag.chunk.SemanticTextSplitter;
import com.example.aidemo.rag.entity.KnowledgeDocumentEntity;
import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import com.example.aidemo.rag.model.ChunkLevel;
import com.example.aidemo.rag.model.KnowledgeChunk;
import com.example.aidemo.rag.repository.KnowledgeDocumentRepository;
import com.example.aidemo.rag.repository.KnowledgeSegmentRepository;
import com.example.aidemo.rag.retrieval.SparseTextNormalizer;
import com.example.aidemo.rag.ingest.SegmentVectorIndexService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结构化入库：解析 → 层级分片 → H2 分段表 → Milvus 向量化。
 * MySQL/H2 为分段主数据，Milvus 为检索索引。
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentParseRouter documentParseRouter;
    private final HierarchicalKnowledgeChunker chunker;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeSegmentRepository segmentRepository;
    private final SegmentVectorIndexService vectorIndexService;

    private final AtomicReference<IngestStatus> status = new AtomicReference<>(IngestStatus.idle());

    public DocumentIngestionService(
            DocumentParseRouter documentParseRouter,
            HierarchicalKnowledgeChunker chunker,
            KnowledgeDocumentRepository documentRepository,
            KnowledgeSegmentRepository segmentRepository,
            SegmentVectorIndexService vectorIndexService) {
        this.documentParseRouter = documentParseRouter;
        this.chunker = chunker;
        this.documentRepository = documentRepository;
        this.segmentRepository = segmentRepository;
        this.vectorIndexService = vectorIndexService;
    }

    public IngestStatus getStatus() {
        return status.get();
    }

    public IngestStatus ingest(Path pdfPath) throws Exception {
        return doIngest(pdfPath);
    }

    @Async
    public void ingestAsync(Path pdfPath) {
        try {
            doIngest(pdfPath);
        } catch (Exception ex) {
            log.error("Async ingest failed: {}", ex.getMessage(), ex);
            status.set(new IngestStatus(
                    "FAILED",
                    pdfPath.getFileName().toString(),
                    null,
                    null,
                    null,
                    ex.getMessage()));
        }
    }

    @Transactional
    protected IngestStatus doIngest(Path pdfPath) throws Exception {
        if (!Files.exists(pdfPath)) {
            throw new IllegalArgumentException("PDF 不存在: " + pdfPath);
        }

        status.set(IngestStatus.running(pdfPath.getFileName().toString()));
        String docId = sanitizeDocId(pdfPath.getFileName().toString());

        clearPreviousIngest();

        status.set(new IngestStatus("SPLITTING", pdfPath.getFileName().toString(), null, null, null, null));
        StructuredParseResult parsed = documentParseRouter.parse(pdfPath);
        List<KnowledgeChunk> chunks = chunker.chunk(parsed);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("未能从 PDF 提取任何可入库分段，请检查 MinerU/OCR 配置");
        }

        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setFileName(pdfPath.getFileName().toString());
        document.setDocId(docId);
        document.setFullText(parsed.markdown());
        document.setTokenEstimate(SemanticTextSplitter.estimateTokens(parsed.markdown()));
        document.setParseEngine(parsed.engine() + (parsed.degraded() ? "_DEGRADED" : ""));
        document.setParseQuality(parsed.quality());
        document.setDocumentType("PDF");
        document.setIngestStatus("EMBEDDING");
        document.setDegraded(parsed.degraded());
        documentRepository.save(document);

        log.info(
                "Chunked {} segments (structured={}) engine={}",
                chunks.size(),
                parsed.supportsStructuredChunking(),
                parsed.engine());

        List<KnowledgeSegmentEntity> persisted = persistChunks(document.getId(), chunks);
        status.set(new IngestStatus(
                "EMBEDDING",
                pdfPath.getFileName().toString(),
                document.getParseEngine(),
                chunks.size(),
                persisted.size(),
                null));

        List<KnowledgeSegmentEntity> embedCandidates = persisted.stream()
                .filter(KnowledgeSegmentEntity::isEmbedEnabled)
                .toList();
        vectorIndexService.embedSegments(embedCandidates, docId);
        segmentRepository.saveAll(embedCandidates);

        document.setIngestStatus("SUCCESS");
        documentRepository.save(document);

        IngestStatus done = new IngestStatus(
                "COMPLETED",
                pdfPath.getFileName().toString(),
                document.getParseEngine(),
                chunks.size(),
                embedCandidates.size(),
                null);
        status.set(done);
        log.info(
                "Ingested {} embed segments from {} (engine={}, degraded={})",
                embedCandidates.size(),
                pdfPath,
                parsed.engine(),
                parsed.degraded());
        return done;
    }

    private void clearPreviousIngest() {
        segmentRepository.deleteAll();
        documentRepository.deleteAll();
    }

    private List<KnowledgeSegmentEntity> persistChunks(Long documentId, List<KnowledgeChunk> chunks) {
        Map<String, Long> parentTempKeyToId = new HashMap<>();
        List<KnowledgeSegmentEntity> parents = new ArrayList<>();
        List<KnowledgeChunk> parentChunks = new ArrayList<>();
        List<KnowledgeChunk> childChunks = new ArrayList<>();

        for (KnowledgeChunk chunk : chunks) {
            if (chunk.chunkLevel() == ChunkLevel.PARENT) {
                parentChunks.add(chunk);
            } else {
                childChunks.add(chunk);
            }
        }

        for (KnowledgeChunk chunk : parentChunks) {
            KnowledgeSegmentEntity parent = toSegmentEntity(documentId, chunk);
            segmentRepository.save(parent);
            parents.add(parent);
            if (chunk.tempKey() != null && !chunk.tempKey().isBlank()) {
                parentTempKeyToId.put(chunk.tempKey(), parent.getId());
            }
        }

        List<KnowledgeSegmentEntity> children = new ArrayList<>();
        for (KnowledgeChunk chunk : childChunks) {
            KnowledgeSegmentEntity child = toSegmentEntity(documentId, chunk);
            if (chunk.parentTempKey() != null && !chunk.parentTempKey().isBlank()) {
                child.setParentId(parentTempKeyToId.get(chunk.parentTempKey()));
            }
            children.add(child);
        }
        segmentRepository.saveAll(children);

        List<KnowledgeSegmentEntity> all = new ArrayList<>(parents);
        all.addAll(children);
        return all;
    }

    private KnowledgeSegmentEntity toSegmentEntity(Long documentId, KnowledgeChunk chunk) {
        KnowledgeSegmentEntity entity = new KnowledgeSegmentEntity();
        entity.setDocumentId(documentId);
        entity.setContent(chunk.content());
        entity.setEmbedText(chunk.embedText() == null ? chunk.content() : chunk.embedText());
        entity.setSparseText(SparseTextNormalizer.normalize(entity.getEmbedText()));
        entity.setBlockType(chunk.blockType().name());
        entity.setChunkLevel(chunk.chunkLevel().name());
        entity.setHeadingPath(chunk.headingPath());
        entity.setPageStart(chunk.pageStart());
        entity.setPageEnd(chunk.pageEnd());
        entity.setEmbedEnabled(chunk.embedEnabled());
        return entity;
    }

    public boolean isInProgress() {
        String state = status.get().state();
        return "RUNNING".equals(state) || "SPLITTING".equals(state) || "EMBEDDING".equals(state);
    }

    private String sanitizeDocId(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record IngestStatus(
            String state,
            String fileName,
            String pdfType,
            Integer blockCount,
            Integer chunkCount,
            String error) {

        static IngestStatus idle() {
            return new IngestStatus("IDLE", null, null, null, null, null);
        }

        static IngestStatus running(String fileName) {
            return new IngestStatus("RUNNING", fileName, null, null, null, null);
        }
    }
}
