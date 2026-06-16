package com.example.aidemo.rag.retrieval;

import com.example.aidemo.config.AiDocumentParseProperties;
import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import com.example.aidemo.rag.model.BlockType;
import com.example.aidemo.rag.repository.KnowledgeSegmentRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 结构化 RAG 上下文回填：Parent 整节 + table_row 附整表。 */
@Service
public class ContextBackfillService {

    private static final String EXPANDED_SEPARATOR = "\n---\n";

    private final KnowledgeSegmentRepository segmentRepository;
    private final AiDocumentParseProperties documentParseProperties;

    public ContextBackfillService(
            KnowledgeSegmentRepository segmentRepository, AiDocumentParseProperties documentParseProperties) {
        this.segmentRepository = segmentRepository;
        this.documentParseProperties = documentParseProperties;
    }

    public List<RetrievalHit> enrich(List<RetrievalHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return hits;
        }
        boolean parentBackfill = documentParseProperties.structuredChunk().parentBackfillEnabled();
        Set<Long> loadIds = new HashSet<>();
        if (parentBackfill) {
            hits.stream()
                    .map(RetrievalHit::parentId)
                    .filter(Objects::nonNull)
                    .forEach(loadIds::add);
        }

        Map<Long, KnowledgeSegmentEntity> relatedMap = new HashMap<>();
        for (RetrievalHit hit : hits) {
            if (BlockType.TABLE_ROW.name().equals(hit.blockType())) {
                segmentRepository
                        .findFirstByDocumentIdAndBlockTypeAndHeadingPath(
                                hit.documentId(), BlockType.TABLE_WHOLE.name(), hit.headingPath())
                        .ifPresent(tableWhole -> relatedMap.put(tableWhole.getId(), tableWhole));
            }
        }
        if (!loadIds.isEmpty()) {
            segmentRepository.findByIdIn(loadIds).forEach(item -> relatedMap.put(item.getId(), item));
        }

        List<RetrievalHit> enriched = new ArrayList<>(hits.size());
        for (RetrievalHit hit : hits) {
            enriched.add(hit.withExpandedContent(buildExpandedContent(hit, relatedMap, parentBackfill)));
        }
        return enriched;
    }

    private String buildExpandedContent(
            RetrievalHit hit, Map<Long, KnowledgeSegmentEntity> relatedMap, boolean parentBackfillEnabled) {
        List<String> parts = new ArrayList<>();
        if (parentBackfillEnabled && hit.parentId() != null) {
            KnowledgeSegmentEntity parent = relatedMap.get(hit.parentId());
            if (parent != null && parent.getContent() != null && !parent.getContent().isBlank()) {
                parts.add(parent.getContent());
            }
        }
        if (BlockType.TABLE_ROW.name().equals(hit.blockType())) {
            relatedMap.values().stream()
                    .filter(item -> Objects.equals(item.getDocumentId(), hit.documentId()))
                    .filter(item -> BlockType.TABLE_WHOLE.name().equals(item.getBlockType()))
                    .filter(item -> Objects.equals(item.getHeadingPath(), hit.headingPath()))
                    .map(KnowledgeSegmentEntity::getContent)
                    .findFirst()
                    .ifPresent(parts::add);
        }
        parts.add(hit.content() == null ? "" : hit.content());
        return parts.stream().filter(part -> part != null && !part.isBlank()).collect(Collectors.joining(EXPANDED_SEPARATOR));
    }

    public record RetrievalHit(
            long segmentId,
            long documentId,
            Long parentId,
            String content,
            String embedText,
            String expandedContent,
            String blockType,
            String headingPath,
            int page,
            double score,
            String docId) {

        public RetrievalHit withExpandedContent(String expanded) {
            return new RetrievalHit(
                    segmentId,
                    documentId,
                    parentId,
                    content,
                    embedText,
                    expanded,
                    blockType,
                    headingPath,
                    page,
                    score,
                    docId);
        }

        public String contextText() {
            return expandedContent != null && !expandedContent.isBlank() ? expandedContent : content;
        }
    }
}
