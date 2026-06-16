package com.example.aidemo.rag.retrieval;

import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.rag.model.QueryIntent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Multi-Query 规则扩展（表格后缀 + 简单中英替换）。 */
@Component
public class QueryExpansionService {

    private static final Pattern HAS_ENGLISH = Pattern.compile("[A-Za-z]");
    private static final Pattern HAS_CHINESE = Pattern.compile("[\u4e00-\u9fff]");

    private final KnowledgeRetrievalProperties properties;

    public QueryExpansionService(KnowledgeRetrievalProperties properties) {
        this.properties = properties;
    }

    public List<String> expand(String query, QueryIntent intent) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        KnowledgeRetrievalProperties.MultiQueryConfig config = properties.multiQuery();
        if (!config.enabled()) {
            return List.of(query.trim());
        }
        Set<String> variants = new LinkedHashSet<>();
        String original = query.trim();
        variants.add(original);

        if (intent == QueryIntent.TABLE_CELL || intent == QueryIntent.FINANCIAL_REPORT) {
            variants.add(original + " 表格");
            variants.add(original + " table");
        }

        if (intent == QueryIntent.FINANCIAL_REPORT) {
            if (original.contains("2025年1月1日") && !original.contains("2025年6月30日")) {
                variants.add(original + " 2025年6月30日");
            }
            if (original.contains("财务报表") && !original.contains("合并")) {
                variants.add(original.replace("财务报表", "合并财务报表"));
            }
        }

        if (original.contains("保密") && !original.contains("confidential")) {
            variants.add(original.replace("保密", "confidential"));
        }
        if (original.toLowerCase().contains("confidential") && !original.contains("保密")) {
            variants.add(original + " 保密");
        }
        if (original.contains("期限") && !original.contains("period")) {
            variants.add(original + " period");
        }

        if (HAS_ENGLISH.matcher(original).find() && HAS_CHINESE.matcher(original).find()) {
            variants.add(original.replaceAll("\\s+", " "));
        }

        return limitVariants(variants, config.maxVariants());
    }

    private static List<String> limitVariants(Set<String> variants, int maxVariants) {
        List<String> result = new ArrayList<>();
        for (String variant : variants) {
            if (variant == null || variant.isBlank()) {
                continue;
            }
            result.add(variant.trim());
            if (result.size() >= Math.max(1, maxVariants)) {
                break;
            }
        }
        return result;
    }
}
