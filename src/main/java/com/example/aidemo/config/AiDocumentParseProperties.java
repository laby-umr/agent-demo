package com.example.aidemo.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.document-parse")
public class AiDocumentParseProperties {

    private boolean enabled = true;
    private String defaultEngine = "auto";
    private Map<String, String> routeOverrides = new HashMap<>();
    private EngineConfig mineru = new EngineConfig();
    private StructuredChunkConfig structuredChunk = new StructuredChunkConfig();

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String defaultEngine() {
        return defaultEngine;
    }

    public void setDefaultEngine(String defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    public Map<String, String> routeOverrides() {
        return routeOverrides;
    }

    public void setRouteOverrides(Map<String, String> routeOverrides) {
        this.routeOverrides = routeOverrides;
    }

    public EngineConfig mineru() {
        return mineru;
    }

    public void setMineru(EngineConfig mineru) {
        this.mineru = mineru;
    }

    public StructuredChunkConfig structuredChunk() {
        return structuredChunk;
    }

    public void setStructuredChunk(StructuredChunkConfig structuredChunk) {
        this.structuredChunk = structuredChunk;
    }

    public static class EngineConfig {
        private boolean enabled;
        private String baseUrl = "http://127.0.0.1:8000";
        private String parsePath = "/api/v1/parse";
        private int timeoutMs = 300_000;

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String baseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String parsePath() {
            return parsePath;
        }

        public void setParsePath(String parsePath) {
            this.parsePath = parsePath;
        }

        public int timeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class StructuredChunkConfig {
        private int childMaxTokens = 512;
        private int childOverlapTokens = 80;
        private boolean tableRowIndexEnabled = true;
        private boolean tableSummaryEnabled = true;
        private boolean embedParent = false;
        private boolean parentBackfillEnabled = true;

        public int childMaxTokens() {
            return childMaxTokens;
        }

        public void setChildMaxTokens(int childMaxTokens) {
            this.childMaxTokens = childMaxTokens;
        }

        public int childOverlapTokens() {
            return childOverlapTokens;
        }

        public void setChildOverlapTokens(int childOverlapTokens) {
            this.childOverlapTokens = childOverlapTokens;
        }

        public boolean tableRowIndexEnabled() {
            return tableRowIndexEnabled;
        }

        public void setTableRowIndexEnabled(boolean tableRowIndexEnabled) {
            this.tableRowIndexEnabled = tableRowIndexEnabled;
        }

        public boolean tableSummaryEnabled() {
            return tableSummaryEnabled;
        }

        public void setTableSummaryEnabled(boolean tableSummaryEnabled) {
            this.tableSummaryEnabled = tableSummaryEnabled;
        }

        public boolean embedParent() {
            return embedParent;
        }

        public void setEmbedParent(boolean embedParent) {
            this.embedParent = embedParent;
        }

        public boolean parentBackfillEnabled() {
            return parentBackfillEnabled;
        }

        public void setParentBackfillEnabled(boolean parentBackfillEnabled) {
            this.parentBackfillEnabled = parentBackfillEnabled;
        }
    }
}
