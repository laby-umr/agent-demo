package com.example.aidemo.rag.retrieval;

import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import com.example.aidemo.rag.repository.KnowledgeSegmentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 关键词稀疏召回（H2 LIKE，模拟 MySQL FULLTEXT）。 */
@Service
public class SegmentSparseSearchService {

    private final KnowledgeSegmentRepository segmentRepository;

    public SegmentSparseSearchService(KnowledgeSegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
    }

    public List<Long> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> terms = tokenize(SparseTextNormalizer.normalize(query));
        if (terms.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> hitCounts = new LinkedHashMap<>();
        for (String term : terms) {
            if (term.length() < 2 && !term.matches("\\d+")) {
                continue;
            }
            for (KnowledgeSegmentEntity segment : segmentRepository.searchByTerm(term)) {
                hitCounts.merge(segment.getId(), 1, Integer::sum);
            }
        }
        return hitCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static List<String> tokenize(String normalizedQuery) {
        Set<String> terms = new LinkedHashSet<>();
        for (String part : normalizedQuery.split("\\s+")) {
            if (!part.isBlank()) {
                terms.add(part.trim());
            }
        }
        if (terms.isEmpty() && !normalizedQuery.isBlank()) {
            terms.add(normalizedQuery.trim());
        }
        List<String> result = new ArrayList<>(terms);
        result.sort(Comparator.comparing(a -> a.toLowerCase(Locale.ROOT)));
        return result;
    }
}
