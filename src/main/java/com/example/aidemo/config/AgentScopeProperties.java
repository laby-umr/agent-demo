package com.example.aidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentscope")
public record AgentScopeProperties(
        Chat chat,
        Embedding embedding,
        Milvus milvus,
        Rag rag,
        Agent agent,
        boolean sampleKnowledgeEnabled) {

    /** DeepSeek 等 OpenAI 兼容 Chat API */
    public record Chat(String baseUrl, String apiKey, String model, int requestTimeoutSeconds) {}

    /** 智谱 embedding-3 等 OpenAI 兼容 Embedding API */
    public record Embedding(String baseUrl, String apiKey, String model, int dimensions, int requestTimeoutSeconds) {}

    public record Milvus(
            String uri,
            String collectionName,
            String databaseName,
            int dimensions,
            String metricType) {
    }

    public record Rag(int topK, double scoreThreshold, boolean enabledForChat) {
    }

    public record Agent(int maxIters) {
    }
}
