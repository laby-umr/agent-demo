package com.example.aidemo.document.qa;

import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.document.model.AnswerConfidence;
import com.example.aidemo.document.model.Evidence;
import io.agentscope.core.rag.model.Document;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AnswerVerifier {

    private final KnowledgeRetrievalProperties retrievalProperties;

    public AnswerVerifier(KnowledgeRetrievalProperties retrievalProperties) {
        this.retrievalProperties = retrievalProperties;
    }

    public AnswerConfidence verify(String question, String answer, List<Evidence> evidences) {
        if (evidences.isEmpty()) {
            return AnswerConfidence.REFUSED;
        }

        double topScore = evidences.get(0).score();
        if (topScore < retrievalProperties.minAnswerScore()) {
            return AnswerConfidence.REFUSED;
        }

        if (isRefusalAnswer(answer)) {
            return AnswerConfidence.REFUSED;
        }

        double overlap = overlapRatio(answer, evidences);
        if (overlap >= retrievalProperties.minEvidenceOverlap()) {
            return topScore >= 0.55 ? AnswerConfidence.HIGH : AnswerConfidence.MEDIUM;
        }
        return AnswerConfidence.LOW;
    }

    public boolean shouldRefuseBeforeGeneration(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return true;
        }
        Double score = documents.get(0).getScore();
        return score == null || score < retrievalProperties.minAnswerScore();
    }

    public String refusalMessage() {
        return NoAnswerGuard.STRICT_REPLY;
    }

    private boolean isRefusalAnswer(String answer) {
        if (answer == null) {
            return true;
        }
        String normalized = answer.toLowerCase(Locale.ROOT);
        return normalized.contains("无法回答")
                || normalized.contains("没有足够")
                || normalized.contains("未找到")
                || normalized.contains("不能确定");
    }

    private double overlapRatio(String answer, List<Evidence> evidences) {
        Set<String> answerTokens = tokenize(answer);
        if (answerTokens.isEmpty()) {
            return 0;
        }
        Set<String> evidenceTokens = new HashSet<>();
        for (Evidence evidence : evidences) {
            evidenceTokens.addAll(tokenize(evidence.promptText()));
        }
        if (evidenceTokens.isEmpty()) {
            return 0;
        }
        long hit = answerTokens.stream().filter(evidenceTokens::contains).count();
        return (double) hit / answerTokens.size();
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) {
            return tokens;
        }
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\[p\\.\\d+\\]", " ");
        for (String token : normalized.split("[\\s，。；、：:（）()\\[\\]《》<>\\-]+")) {
            if (token.length() < 2) {
                continue;
            }
            tokens.add(token);
            if (containsCjk(token)) {
                for (int i = 0; i < token.length() - 1; i++) {
                    tokens.add(token.substring(i, i + 2));
                }
            }
        }
        return tokens;
    }

    private boolean containsCjk(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (Character.UnicodeScript.of(token.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}
