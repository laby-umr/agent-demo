package com.example.aidemo.document.qa;

import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.document.model.Evidence;
import java.util.List;
import org.springframework.stereotype.Component;

/** 无引用守卫，对齐 laby-admin {@code NoAnswerGuard}。 */
@Component
public class NoAnswerGuard {

    public static final String STRICT_REPLY = "抱歉，知识库中未找到足够依据回答该问题。";
    public static final String HINT_REPLY = "未在文档中找到明确依据，请尝试换个问法或确认 PDF 已入库。";

    private final KnowledgeRetrievalProperties properties;

    public NoAnswerGuard(KnowledgeRetrievalProperties properties) {
        this.properties = properties;
    }

    public boolean shouldBlock(List<Evidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return true;
        }
        double topScore = evidences.get(0).score();
        return topScore < properties.minAnswerScore();
    }

    public String blockedReply() {
        if ("hint".equalsIgnoreCase(properties.noAnswerPolicy())) {
            return HINT_REPLY;
        }
        if ("relaxed".equalsIgnoreCase(properties.noAnswerPolicy())) {
            return null;
        }
        return STRICT_REPLY;
    }
}
