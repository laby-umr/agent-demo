package com.example.aidemo.rag.retrieval;

import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.rag.entity.KnowledgeDocumentEntity;
import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import com.example.aidemo.rag.ingest.SegmentVectorIndexService;
import com.example.aidemo.rag.model.QueryIntent;
import com.example.aidemo.rag.repository.KnowledgeDocumentRepository;
import com.example.aidemo.rag.repository.KnowledgeSegmentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Universal RAG 检索编排：意图 → Multi-Query → Dense(Milvus) + Sparse(H2) → RRF → 块类型加权 → 上下文回填。
 */
@Service
public class UniversalRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(UniversalRetrievalService.class);

    private final KnowledgeRetrievalProperties properties;
    private final KnowledgeSegmentRepository segmentRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final SegmentVectorIndexService vectorIndexService;
    private final QueryExpansionService queryExpansionService;
    private final SegmentSparseSearchService sparseSearchService;
    private final ContextBackfillService contextBackfillService;

    public UniversalRetrievalService(
            KnowledgeRetrievalProperties properties,
            KnowledgeSegmentRepository segmentRepository,
            KnowledgeDocumentRepository documentRepository,
            SegmentVectorIndexService vectorIndexService,
            QueryExpansionService queryExpansionService,
            SegmentSparseSearchService sparseSearchService,
            ContextBackfillService contextBackfillService) {
        this.properties = properties;
        this.segmentRepository = segmentRepository;
        this.documentRepository = documentRepository;
        this.vectorIndexService = vectorIndexService;
        this.queryExpansionService = queryExpansionService;
        this.sparseSearchService = sparseSearchService;
        this.contextBackfillService = contextBackfillService;
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(String query) {
        return retrieve(query, properties.defaultTopK());
    }

    @Transactional
    public RetrievalResult retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            return RetrievalResult.empty();
        }
        if (segmentRepository.count() == 0) {
            return RetrievalResult.empty();
        }

        int searchTopK = Math.min(40, topK * properties.retrievalFactor());
        QueryIntent intent = QueryIntentClassifier.classify(query);
        List<String> variants = queryExpansionService.expand(query, intent);
        KnowledgeRetrievalProperties.HybridConfig hybrid = properties.hybrid();

        List<RrfFusion.WeightedRankedList> rankedLists = new ArrayList<>();
        int denseHitCount = 0;
        int sparseHitCount = 0;

        for (String variant : variants) {
            List<Long> denseIds = vectorIndexService.searchDense(variant, searchTopK).stream()
                    .map(SegmentVectorIndexService.DenseHit::segmentId)
                    .toList();
            denseHitCount += denseIds.size();
            if (!denseIds.isEmpty()) {
                rankedLists.add(new RrfFusion.WeightedRankedList(hybrid.denseWeight(), denseIds));
            }
            if (hybrid.enabled() && hybrid.sparseEnabled()) {
                List<Long> sparseIds = sparseSearchService.search(variant, searchTopK);
                sparseHitCount += sparseIds.size();
                if (!sparseIds.isEmpty()) {
                    rankedLists.add(new RrfFusion.WeightedRankedList(hybrid.sparseWeight(), sparseIds));
                }
            }
        }

        Map<Long, Double> fusedScores = RrfFusion.fuse(hybrid.rrfK(), rankedLists);
        if (fusedScores.isEmpty()) {
            log.info("[retrieve] no hits intent={} dense={} sparse={}", intent, denseHitCount, sparseHitCount);
            return new RetrievalResult(List.of(), intent, variants, denseHitCount, sparseHitCount, false);
        }

        List<Long> fusedSegmentIds = new ArrayList<>(fusedScores.keySet());
        List<KnowledgeSegmentEntity> segments = segmentRepository.findByIdIn(fusedSegmentIds);
        Map<Long, KnowledgeSegmentEntity> segmentMap = segments.stream()
                .collect(Collectors.toMap(KnowledgeSegmentEntity::getId, Function.identity(), (a, b) -> a));

        Map<Long, Double> boostedScores = new LinkedHashMap<>();
        KnowledgeRetrievalProperties.BlockRouteConfig blockRoute = properties.blockRoute();
        for (Map.Entry<Long, Double> entry : fusedScores.entrySet()) {
            KnowledgeSegmentEntity segment = segmentMap.get(entry.getKey());
            String blockType = segment != null ? segment.getBlockType() : null;
            boostedScores.put(
                    entry.getKey(), BlockTypeRouteBoost.apply(intent, blockType, entry.getValue(), blockRoute));
        }
        boostedScores = RetrievalContentScorer.adjust(boostedScores, segmentMap, intent);

        List<ContextBackfillService.RetrievalHit> candidates = boostedScores.entrySet().stream()
                .map(entry -> toHit(segmentMap.get(entry.getKey()), entry.getValue()))
                .filter(hit -> hit != null)
                .sorted(Comparator.comparingDouble(ContextBackfillService.RetrievalHit::score).reversed())
                .limit(searchTopK)
                .toList();

        List<ContextBackfillService.RetrievalHit> filtered = candidates.stream()
                .limit(topK)
                .toList();

        List<ContextBackfillService.RetrievalHit> enriched = contextBackfillService.enrich(filtered);
        if (!enriched.isEmpty()) {
            segmentRepository.incrementRetrievalCount(
                    enriched.stream().map(ContextBackfillService.RetrievalHit::segmentId).toList());
        }

        log.info(
                "[retrieve] intent={} variants={} dense={} sparse={} fused={} final={}",
                intent,
                variants.size(),
                denseHitCount,
                sparseHitCount,
                fusedScores.size(),
                enriched.size());

        return new RetrievalResult(enriched, intent, variants, denseHitCount, sparseHitCount, false);
    }

    private ContextBackfillService.RetrievalHit toHit(KnowledgeSegmentEntity segment, double score) {
        if (segment == null) {
            return null;
        }
        String docId = documentRepository.findById(segment.getDocumentId())
                .map(KnowledgeDocumentEntity::getDocId)
                .orElse("unknown");
        int page = segment.getPageStart() == null ? 1 : segment.getPageStart();
        return new ContextBackfillService.RetrievalHit(
                segment.getId(),
                segment.getDocumentId(),
                segment.getParentId(),
                segment.getContent(),
                segment.getEmbedText(),
                null,
                segment.getBlockType(),
                segment.getHeadingPath(),
                page,
                score,
                docId);
    }

    public record RetrievalResult(
            List<ContextBackfillService.RetrievalHit> hits,
            QueryIntent intent,
            List<String> queryVariants,
            int denseHitCount,
            int sparseHitCount,
            boolean rerankDegraded) {

        public static RetrievalResult empty() {
            return new RetrievalResult(List.of(), QueryIntent.GENERAL, List.of(), 0, 0, false);
        }

        public double topScore() {
            return hits.isEmpty() ? 0 : hits.get(0).score();
        }
    }
}
