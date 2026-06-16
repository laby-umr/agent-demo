package com.example.aidemo.knowledge;

import com.example.aidemo.document.model.Evidence;
import com.example.aidemo.rag.retrieval.ContextBackfillService;
import com.example.aidemo.rag.retrieval.UniversalRetrievalService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Universal RAG 召回，供 Document QA 使用。 */
@Service
public class KnowledgeRecallService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRecallService.class);

    private final UniversalRetrievalService universalRetrievalService;

    public KnowledgeRecallService(UniversalRetrievalService universalRetrievalService) {
        this.universalRetrievalService = universalRetrievalService;
    }

    public List<Evidence> recall(String query, boolean documentOnly) {
        UniversalRetrievalService.RetrievalResult result = universalRetrievalService.retrieve(query);
        log.debug(
                "[recall] intent={} dense={} sparse={} hits={} topScore={}",
                result.intent(),
                result.denseHitCount(),
                result.sparseHitCount(),
                result.hits().size(),
                result.topScore());
        return toEvidences(result.hits());
    }

    private List<Evidence> toEvidences(List<ContextBackfillService.RetrievalHit> hits) {
        if (hits.isEmpty()) {
            return List.of();
        }
        double maxScore = hits.stream().mapToDouble(ContextBackfillService.RetrievalHit::score).max().orElse(1);
        List<Evidence> evidences = new ArrayList<>();
        for (ContextBackfillService.RetrievalHit hit : hits) {
            double normalizedScore = maxScore > 0 ? hit.score() / maxScore : hit.score();
            evidences.add(new Evidence(
                    hit.segmentId(),
                    hit.page(),
                    hit.content(),
                    hit.contextText(),
                    normalizedScore,
                    hit.docId(),
                    hit.blockType()));
        }
        return evidences;
    }
}
