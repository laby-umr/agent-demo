package com.example.aidemo.knowledge;

import com.example.aidemo.config.AgentScopeProperties;
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.store.MilvusStore;
import io.agentscope.core.rag.store.VDBStoreBase;
import io.milvus.v2.common.IndexParam;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeConfig {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeConfig.class);

    @Bean
    OpenAITextEmbedding textEmbedding(AgentScopeProperties properties) {
        AgentScopeProperties.Embedding embedding = properties.embedding();
        ExecutionConfig executionConfig = ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(embedding.requestTimeoutSeconds()))
                .maxAttempts(2)
                .build();

        return OpenAITextEmbedding.builder()
                .baseUrl(embedding.baseUrl())
                .apiKey(embedding.apiKey())
                .modelName(embedding.model())
                .dimensions(embedding.dimensions())
                .executionConfig(executionConfig)
                .build();
    }

    @Bean(destroyMethod = "close")
    MilvusStore milvusStore(AgentScopeProperties properties) throws VectorStoreException {
        AgentScopeProperties.Milvus milvus = properties.milvus();
        IndexParam.MetricType metricType = IndexParam.MetricType.valueOf(milvus.metricType());

        MilvusStore store = MilvusStore.builder()
                .uri(milvus.uri())
                .collectionName(milvus.collectionName())
                .databaseName(milvus.databaseName())
                .dimensions(milvus.dimensions())
                .metricType(metricType)
                .build();

        log.info(
                "Milvus connected: uri={}, collection={}, dimensions={}",
                milvus.uri(),
                milvus.collectionName(),
                milvus.dimensions());

        return store;
    }

    @Bean
    Knowledge knowledge(OpenAITextEmbedding embedding, VDBStoreBase vectorStore) {
        return SimpleKnowledge.builder()
                .embeddingModel(embedding)
                .embeddingStore(vectorStore)
                .build();
    }
}
