package com.example.aidemo.rag.retrieval;

import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import com.example.aidemo.rag.model.QueryIntent;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** 检索分数二次调整：财务/表格查询优先含数字片段，弱化重复页眉标题。 */
public final class RetrievalContentScorer {

    private static final Pattern FINANCIAL_NUMBER =
            Pattern.compile("\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?|\\d+(?:\\.\\d+)?");
    private static final Pattern REPORT_TITLE_ONLY =
            Pattern.compile(".*(财务报表|财务报告|合并报表).*");

    private RetrievalContentScorer() {}

    public static Map<Long, Double> adjust(
            Map<Long, Double> scores, Map<Long, KnowledgeSegmentEntity> segmentMap, QueryIntent intent) {
        if (scores == null || scores.isEmpty()) {
            return scores == null ? Map.of() : scores;
        }
        Map<Long, Double> adjusted = new LinkedHashMap<>();
        Map<String, Long> bestByContentKey = new LinkedHashMap<>();

        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            KnowledgeSegmentEntity segment = segmentMap.get(entry.getKey());
            if (segment == null) {
                continue;
            }
            double score = entry.getValue() * contentMultiplier(segment, intent);
            String contentKey = normalizeContentKey(segment.getContent());
            Long existingId = bestByContentKey.get(contentKey);
            if (existingId != null) {
                Double existingScore = adjusted.get(existingId);
                if (existingScore != null && existingScore >= score) {
                    continue;
                }
                adjusted.remove(existingId);
            }
            bestByContentKey.put(contentKey, entry.getKey());
            adjusted.put(entry.getKey(), score);
        }
        return adjusted;
    }

    private static double contentMultiplier(KnowledgeSegmentEntity segment, QueryIntent intent) {
        String content = segment.getContent();
        if (content == null || content.isBlank()) {
            return 0.5;
        }
        double multiplier = 1.0;
        int digitCount = countDigits(content);
        int numberGroups = countFinancialNumbers(content);
        boolean financialIntent =
                intent == QueryIntent.FINANCIAL_REPORT
                        || intent == QueryIntent.TABLE_CELL
                        || intent == QueryIntent.TABLE_OVERVIEW;

        if (financialIntent && (digitCount >= 8 || numberGroups >= 2)) {
            multiplier *= 1.35;
        } else if (financialIntent && digitCount == 0) {
            multiplier *= 0.55;
        }

        if (content.length() < 120
                && REPORT_TITLE_ONLY.matcher(content).find()
                && numberGroups == 0) {
            multiplier *= 0.45;
        }

        Integer page = segment.getPageStart();
        if (financialIntent && page != null && page > 20 && digitCount >= 4) {
            multiplier *= 1.15;
        }
        return multiplier;
    }

    private static int countDigits(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private static int countFinancialNumbers(String text) {
        int count = 0;
        var matcher = FINANCIAL_NUMBER.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String normalizeContentKey(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
