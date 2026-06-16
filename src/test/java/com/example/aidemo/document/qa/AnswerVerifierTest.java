package com.example.aidemo.document.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.document.model.AnswerConfidence;
import com.example.aidemo.document.model.Evidence;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnswerVerifierTest {

    private AnswerVerifier verifier;

    @BeforeEach
    void setUp() {
        KnowledgeRetrievalProperties properties = new KnowledgeRetrievalProperties();
        properties.setMinAnswerScore(0.45);
        properties.setMinEvidenceOverlap(0.08);
        verifier = new AnswerVerifier(properties);
    }

    @Test
    void refusesWhenNoEvidence() {
        assertEquals(AnswerConfidence.REFUSED, verifier.verify("问题", "任意答案", List.of()));
    }

    @Test
    void refusesWhenScoreTooLow() {
        List<Evidence> evidences = List.of(new Evidence(1L, 1, "键 技术条件 材料要求", null, 0.2, "doc", "TEXT"));
        assertEquals(AnswerConfidence.REFUSED, verifier.verify("材料要求是什么", "答案", evidences));
    }

    @Test
    void highConfidenceWhenOverlapGood() {
        List<Evidence> evidences =
                List.of(new Evidence(3L, 3, "本标准规定了键的技术要求、检验方法", null, 0.72, "gbt1568", "TEXT"));
        AnswerConfidence confidence = verifier.verify(
                "标准规定了什么",
                "本标准规定了键的技术要求和检验方法 [p.3]",
                evidences);
        assertEquals(AnswerConfidence.HIGH, confidence);
    }
}
