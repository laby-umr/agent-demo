package com.example.aidemo.rag.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reciprocal Rank Fusion（默认 k=60）。 */
public final class RrfFusion {

    private RrfFusion() {}

    public record WeightedRankedList(double weight, List<Long> segmentIds) {}

    public static Map<Long, Double> fuse(int k, List<WeightedRankedList> lists) {
        if (lists == null || lists.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (WeightedRankedList list : lists) {
            if (list == null || list.segmentIds() == null || list.segmentIds().isEmpty()) {
                continue;
            }
            double weight = list.weight() <= 0 ? 1.0 : list.weight();
            for (int rank = 0; rank < list.segmentIds().size(); rank++) {
                Long segmentId = list.segmentIds().get(rank);
                if (segmentId == null) {
                    continue;
                }
                double contribution = weight / (k + rank + 1);
                scores.merge(segmentId, contribution, Math::max);
            }
        }
        return sortByScoreDesc(scores);
    }

    private static Map<Long, Double> sortByScoreDesc(Map<Long, Double> scores) {
        List<Map.Entry<Long, Double>> entries = new ArrayList<>(scores.entrySet());
        entries.sort(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey));
        Map<Long, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }
}
