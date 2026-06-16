package com.example.aidemo.service;

import com.example.aidemo.config.AgentScopeProperties;
import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.document.model.Evidence;
import com.example.aidemo.document.qa.DocumentAnswerGenerator;
import com.example.aidemo.document.qa.GroundedPromptBuilder;
import com.example.aidemo.web.dto.ChatResponse;
import com.example.aidemo.web.dto.SearchResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 通用聊天。
 * <ul>
 *   <li>agentscope.rag.enabled-for-chat=false：直连 DeepSeek</li>
 *   <li>enabled-for-chat=true：Milvus 向量检索（对应右侧「知识入库」）+ DeepSeek 生成</li>
 * </ul>
 * PDF 结构化问答请用 {@link com.example.aidemo.document.qa.DocumentQaService}。
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final KnowledgeService knowledgeService;
    private final DocumentAnswerGenerator answerGenerator;
    private final KnowledgeRetrievalProperties retrievalProperties;
    private final AgentScopeProperties properties;
    private final Duration blockTimeout;

    public ChatService(
            KnowledgeService knowledgeService,
            DocumentAnswerGenerator answerGenerator,
            KnowledgeRetrievalProperties retrievalProperties,
            AgentScopeProperties properties) {
        this.knowledgeService = knowledgeService;
        this.answerGenerator = answerGenerator;
        this.retrievalProperties = retrievalProperties;
        this.properties = properties;
        this.blockTimeout = Duration.ofSeconds(properties.chat().requestTimeoutSeconds());
    }

    public ChatResponse chat(String question, String sessionId) {
        String resolvedSessionId = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId;

        String prompt;
        if (properties.rag().enabledForChat()) {
            log.info("[chat] RAG 模式：step1 智谱 embedding + Milvus 检索 …");
            SearchResponse search = knowledgeService.search(question);
            List<Evidence> evidences = toEvidences(search);
            List<Evidence> relevant = filterRelevant(evidences);
            log.info("[chat] 检索完成 hits={} relevant={}", evidences.size(), relevant.size());
            prompt = relevant.isEmpty()
                    ? question
                    : GroundedPromptBuilder.build(question, relevant);
        } else {
            log.info("[chat] 直连 LLM（跳过 embedding 检索）");
            prompt = question;
        }

        try {
            log.info("[chat] step2 调用 DeepSeek 生成 …");
            String answer = answerGenerator.generate(prompt, blockTimeout);
            if (answer == null || answer.isBlank()) {
                return new ChatResponse("模型未返回有效回答，请检查 DeepSeek API 配置。", resolvedSessionId);
            }
            return new ChatResponse(answer, resolvedSessionId);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            if (message.contains("interrupted") || message.contains("Timeout")) {
                message = "模型响应超时，请稍后重试。";
            }
            log.warn("[chat] LLM 调用失败: {}", message);
            return new ChatResponse(message, resolvedSessionId);
        }
    }

    private List<Evidence> toEvidences(SearchResponse search) {
        if (search.hits() == null || search.hits().isEmpty()) {
            return List.of();
        }
        double maxScore = search.hits().stream()
                .mapToDouble(SearchResponse.SearchHit::score)
                .max()
                .orElse(1.0);
        List<Evidence> evidences = new ArrayList<>();
        for (SearchResponse.SearchHit hit : search.hits()) {
            double normalized = maxScore > 0 ? hit.score() / maxScore : hit.score();
            evidences.add(new Evidence(0L, 1, hit.content(), null, normalized, "knowledge", "TEXT"));
        }
        return evidences;
    }

    private List<Evidence> filterRelevant(List<Evidence> evidences) {
        double minScore = retrievalProperties.minAnswerScore();
        return evidences.stream().filter(item -> item.score() >= minScore).toList();
    }
}
