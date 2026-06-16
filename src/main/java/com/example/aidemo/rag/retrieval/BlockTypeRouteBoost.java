package com.example.aidemo.rag.retrieval;

import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.rag.model.BlockType;
import com.example.aidemo.rag.model.QueryIntent;

/** 按查询意图对块类型分数加权。 */
public final class BlockTypeRouteBoost {

    private BlockTypeRouteBoost() {}

    public static double apply(
            QueryIntent intent, String blockType, double score, KnowledgeRetrievalProperties.BlockRouteConfig config) {
        if (intent == null || score <= 0 || config == null || !config.enabled()) {
            return score;
        }
        BlockType block = BlockType.fromCode(blockType);
        return switch (intent) {
            case TABLE_CELL, FINANCIAL_REPORT -> switch (block) {
                case TABLE_ROW -> score * config.tableCellBoost();
                case TABLE_WHOLE -> score * config.tableWholeBoost();
                case TABLE_SUMMARY -> score * config.tableSummaryBoost();
                default -> score;
            };
            case TABLE_OVERVIEW -> block == BlockType.TABLE_SUMMARY ? score * config.tableSummaryBoost() : score;
            default -> score;
        };
    }
}
