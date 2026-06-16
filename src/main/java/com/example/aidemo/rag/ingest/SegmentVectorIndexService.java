package com.example.aidemo.rag.ingest;

import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Milvus 向量写入与 Dense 召回（segmentId 存在 payload）。 */
@Service
public class SegmentVectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(SegmentVectorIndexService.class);

    private final Knowledge knowledge;

    public SegmentVectorIndexService(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    public void embedSegments(List<KnowledgeSegmentEntity> segments, String docId) {
        if (segments == null || segments.isEmpty()) {
            return;
        }
        int batchSize = 16;
        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<Document> documents = new ArrayList<>();
            for (KnowledgeSegmentEntity segment : segments.subList(i, end)) {
                if (!segment.isEmbedEnabled()) {
                    continue;
                }
                String vectorId = "seg-" + segment.getId();
                segment.setVectorId(vectorId);
                String embedText = segment.getEmbedText() == null ? segment.getContent() : segment.getEmbedText();
                Map<String, Object> payload = new HashMap<>();
                payload.put("segmentId", segment.getId());
                payload.put("documentId", segment.getDocumentId());
                payload.put("page", segment.getPageStart() == null ? 1 : segment.getPageStart());
                payload.put("blockType", segment.getBlockType());
                payload.put("headingPath", segment.getHeadingPath());
                payload.put("chunkLevel", segment.getChunkLevel());

                DocumentMetadata metadata = DocumentMetadata.builder()
                        .content(TextBlock.builder().text(embedText).build())
                        .docId(docId)
                        .chunkId(vectorId)
                        .payload(payload)
                        .build();
                documents.add(new Document(metadata));
            }
            if (!documents.isEmpty()) {
                knowledge.addDocuments(documents).block();
                log.info("Milvus upsert progress: {}/{}", end, segments.size());
            }
        }
    }

    public List<DenseHit> searchDense(String query, int topK) {
        RetrieveConfig config = RetrieveConfig.builder()
                .limit(topK)
                .scoreThreshold(0.0)
                .build();
        List<Document> documents = knowledge.retrieve(query, config).block();
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<DenseHit> hits = new ArrayList<>();
        for (Document document : documents) {
            Object segmentIdObj = document.getMetadata().getPayloadValue("segmentId");
            if (!(segmentIdObj instanceof Number number)) {
                continue;
            }
            double score = document.getScore() == null ? 0 : document.getScore();
            hits.add(new DenseHit(number.longValue(), score, document.getMetadata().getDocId()));
        }
        return hits;
    }

    public record DenseHit(long segmentId, double score, String docId) {}
}
