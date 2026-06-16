package com.example.aidemo.service;

import com.example.aidemo.config.AgentScopeProperties;
import com.example.aidemo.web.dto.SearchResponse;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeService {

    private final Knowledge knowledge;
    private final AgentScopeProperties properties;
    private final TextReader textReader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);

    public KnowledgeService(Knowledge knowledge, AgentScopeProperties properties) {
        this.knowledge = knowledge;
        this.properties = properties;
    }

    public void addDocument(String content) {
        List<Document> documents = textReader.read(ReaderInput.fromString(content)).block();
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("文档内容为空或无法分块");
        }
        knowledge.addDocuments(documents).block();
    }

    public SearchResponse search(String query) {
        AgentScopeProperties.Rag rag = properties.rag();
        List<Document> documents = knowledge.retrieve(
                        query,
                        RetrieveConfig.builder()
                                .limit(rag.topK())
                                .scoreThreshold(rag.scoreThreshold())
                                .build())
                .block();

        List<SearchResponse.SearchHit> hits = documents == null
                ? List.of()
                : documents.stream()
                        .map(doc -> new SearchResponse.SearchHit(
                                doc.getMetadata().getContentText(),
                                doc.getScore() == null ? 0.0 : doc.getScore()))
                        .toList();

        return new SearchResponse(query, hits);
    }
}
