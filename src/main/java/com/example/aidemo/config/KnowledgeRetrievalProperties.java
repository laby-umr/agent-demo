package com.example.aidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.knowledge-retrieval")
public class KnowledgeRetrievalProperties {

    private boolean enabled = true;
    private int defaultTopK = 8;
    private double defaultSimilarityThreshold = 0.60;
    private int retrievalFactor = 4;
    private double minAnswerScore = 0.45;
    private String noAnswerPolicy = "strict";
    private double minEvidenceOverlap = 0.08;
    private HybridConfig hybrid = new HybridConfig();
    private MultiQueryConfig multiQuery = new MultiQueryConfig();
    private BlockRouteConfig blockRoute = new BlockRouteConfig();
    private RerankConfig rerank = new RerankConfig();

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int defaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public double defaultSimilarityThreshold() {
        return defaultSimilarityThreshold;
    }

    public void setDefaultSimilarityThreshold(double defaultSimilarityThreshold) {
        this.defaultSimilarityThreshold = defaultSimilarityThreshold;
    }

    public int retrievalFactor() {
        return retrievalFactor;
    }

    public void setRetrievalFactor(int retrievalFactor) {
        this.retrievalFactor = retrievalFactor;
    }

    public double minAnswerScore() {
        return minAnswerScore;
    }

    public void setMinAnswerScore(double minAnswerScore) {
        this.minAnswerScore = minAnswerScore;
    }

    public String noAnswerPolicy() {
        return noAnswerPolicy;
    }

    public void setNoAnswerPolicy(String noAnswerPolicy) {
        this.noAnswerPolicy = noAnswerPolicy;
    }

    public double minEvidenceOverlap() {
        return minEvidenceOverlap;
    }

    public void setMinEvidenceOverlap(double minEvidenceOverlap) {
        this.minEvidenceOverlap = minEvidenceOverlap;
    }

    public HybridConfig hybrid() {
        return hybrid;
    }

    public void setHybrid(HybridConfig hybrid) {
        this.hybrid = hybrid;
    }

    public MultiQueryConfig multiQuery() {
        return multiQuery;
    }

    public void setMultiQuery(MultiQueryConfig multiQuery) {
        this.multiQuery = multiQuery;
    }

    public BlockRouteConfig blockRoute() {
        return blockRoute;
    }

    public void setBlockRoute(BlockRouteConfig blockRoute) {
        this.blockRoute = blockRoute;
    }

    public RerankConfig rerank() {
        return rerank;
    }

    public void setRerank(RerankConfig rerank) {
        this.rerank = rerank;
    }

    public boolean isStrictPolicy() {
        return !"relaxed".equalsIgnoreCase(noAnswerPolicy);
    }

    public static class HybridConfig {
        private boolean enabled = true;
        private boolean sparseEnabled = true;
        private double denseWeight = 1.0;
        private double sparseWeight = 1.2;
        private int rrfK = 60;

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean sparseEnabled() {
            return sparseEnabled;
        }

        public void setSparseEnabled(boolean sparseEnabled) {
            this.sparseEnabled = sparseEnabled;
        }

        public double denseWeight() {
            return denseWeight;
        }

        public void setDenseWeight(double denseWeight) {
            this.denseWeight = denseWeight;
        }

        public double sparseWeight() {
            return sparseWeight;
        }

        public void setSparseWeight(double sparseWeight) {
            this.sparseWeight = sparseWeight;
        }

        public int rrfK() {
            return rrfK;
        }

        public void setRrfK(int rrfK) {
            this.rrfK = rrfK;
        }
    }

    public static class MultiQueryConfig {
        private boolean enabled = true;
        private int maxVariants = 3;

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int maxVariants() {
            return maxVariants;
        }

        public void setMaxVariants(int maxVariants) {
            this.maxVariants = maxVariants;
        }
    }

    public static class BlockRouteConfig {
        private boolean enabled = true;
        private double tableCellBoost = 1.3;
        private double tableWholeBoost = 1.1;
        private double tableSummaryBoost = 1.4;

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double tableCellBoost() {
            return tableCellBoost;
        }

        public void setTableCellBoost(double tableCellBoost) {
            this.tableCellBoost = tableCellBoost;
        }

        public double tableWholeBoost() {
            return tableWholeBoost;
        }

        public void setTableWholeBoost(double tableWholeBoost) {
            this.tableWholeBoost = tableWholeBoost;
        }

        public double tableSummaryBoost() {
            return tableSummaryBoost;
        }

        public void setTableSummaryBoost(double tableSummaryBoost) {
            this.tableSummaryBoost = tableSummaryBoost;
        }
    }

    public static class RerankConfig {
        private boolean enabled = false;

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
