package com.example.aidemo.document.qa;

import com.example.aidemo.config.AgentScopeProperties;
import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.document.ingest.DocumentIngestionService;
import com.example.aidemo.document.model.AnswerConfidence;
import com.example.aidemo.document.model.Evidence;
import com.example.aidemo.knowledge.KnowledgeRecallService;
import com.example.aidemo.web.dto.DocumentQaResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentQaService {

    private static final Logger log = LoggerFactory.getLogger(DocumentQaService.class);

    private final KnowledgeRecallService recallService;
    private final DocumentAnswerGenerator answerGenerator;
    private final AnswerVerifier answerVerifier;
    private final NoAnswerGuard noAnswerGuard;
    private final DocumentIngestionService ingestionService;
    private final AgentScopeProperties properties;

    public DocumentQaService(
            KnowledgeRecallService recallService,
            DocumentAnswerGenerator answerGenerator,
            AnswerVerifier answerVerifier,
            NoAnswerGuard noAnswerGuard,
            DocumentIngestionService ingestionService,
            AgentScopeProperties properties) {
        this.recallService = recallService;
        this.answerGenerator = answerGenerator;
        this.answerVerifier = answerVerifier;
        this.noAnswerGuard = noAnswerGuard;
        this.ingestionService = ingestionService;
        this.properties = properties;
    }

    public DocumentQaResponse ask(String question, String sessionId) {
        String resolvedSessionId = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId;

        if (ingestionService.isInProgress()) {
            return new DocumentQaResponse(
                    "PDF 仍在入库中（embedding 向量化），请等待状态变为 COMPLETED 后再提问。",
                    AnswerConfidence.REFUSED.name(),
                    "入库未完成",
                    List.of(),
                    resolvedSessionId);
        }

        List<Evidence> evidences = recallService.recall(question, true);
        double topScore = evidences.isEmpty() ? 0 : evidences.get(0).score();
        log.info("[doc-qa] Universal RAG 检索完成, hits={}, topScore={}", evidences.size(), topScore);

        if (evidences.isEmpty() || noAnswerGuard.shouldBlock(evidences)) {
            return new DocumentQaResponse(
                    noAnswerGuard.blockedReply(),
                    AnswerConfidence.REFUSED.name(),
                    evidences.isEmpty() ? "知识库无匹配片段，请先上传 PDF 并等待入库完成" : "召回分数低于阈值",
                    evidences,
                    resolvedSessionId);
        }

        String groundedPrompt = GroundedPromptBuilder.build(question, evidences);
        String answer;
        try {
            log.info("[doc-qa] step2 调用 chat 模型生成 …");
            Duration timeout = Duration.ofSeconds(properties.chat().requestTimeoutSeconds());
            answer = answerGenerator.generate(groundedPrompt, timeout);
            log.info("[doc-qa] step2 完成, answerChars={}", answer == null ? 0 : answer.length());
        } catch (Exception ex) {
            return new DocumentQaResponse(
                    "模型调用失败: " + ex.getMessage(),
                    AnswerConfidence.REFUSED.name(),
                    ex.getMessage(),
                    evidences,
                    resolvedSessionId);
        }

        AnswerConfidence confidence = answerVerifier.verify(question, answer, evidences);
        if (confidence == AnswerConfidence.REFUSED) {
            String reply = noAnswerGuard.blockedReply();
            return new DocumentQaResponse(
                    reply != null ? reply : answerVerifier.refusalMessage(),
                    confidence.name(),
                    "答案与证据一致性不足",
                    evidences,
                    resolvedSessionId);
        }

        return new DocumentQaResponse(answer, confidence.name(), null, evidences, resolvedSessionId);
    }

    public List<Evidence> search(String query) {
        return recallService.recall(query, true);
    }
}
