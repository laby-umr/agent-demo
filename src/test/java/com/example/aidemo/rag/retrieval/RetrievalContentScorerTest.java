package com.example.aidemo.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import com.example.aidemo.rag.model.QueryIntent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetrievalContentScorerTest {

    @Test
    void prefersNumericTableOverRepeatedTitle() {
        KnowledgeSegmentEntity title = segment(1L, 3, "中信证券股份有限公司 2025年1月1日至6月30日止期间财务报表");
        KnowledgeSegmentEntity table = segment(
                2L,
                227,
                "短期借款  即期偿还  3个月内  合计\n短期借款  1,234.56  27,257,561,385.22");

        Map<Long, Double> scores = new LinkedHashMap<>();
        scores.put(1L, 1.0);
        scores.put(2L, 0.9);

        Map<Long, KnowledgeSegmentEntity> segmentMap = Map.of(1L, title, 2L, table);
        Map<Long, Double> adjusted =
                RetrievalContentScorer.adjust(scores, segmentMap, QueryIntent.FINANCIAL_REPORT);

        assertTrue(adjusted.get(2L) > adjusted.get(1L));
    }

    @Test
    void deduplicatesIdenticalTitleSegments() {
        KnowledgeSegmentEntity first = segment(1L, 2, "2025年1月1日至6月30日止期间财务报表");
        KnowledgeSegmentEntity second = segment(2L, 4, "2025年1月1日至6月30日止期间财务报表");

        Map<Long, Double> scores = new LinkedHashMap<>();
        scores.put(1L, 0.8);
        scores.put(2L, 1.0);

        Map<Long, Double> adjusted = RetrievalContentScorer.adjust(
                scores, Map.of(1L, first, 2L, second), QueryIntent.FINANCIAL_REPORT);

        assertEquals(1, adjusted.size());
        assertTrue(adjusted.containsKey(2L));
    }

    private static KnowledgeSegmentEntity segment(long id, int page, String content) {
        KnowledgeSegmentEntity entity = new KnowledgeSegmentEntity();
        entity.setId(id);
        entity.setPageStart(page);
        entity.setContent(content);
        return entity;
    }
}
