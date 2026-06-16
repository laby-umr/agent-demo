package com.example.aidemo.document.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aidemo.config.KnowledgeRetrievalProperties;
import com.example.aidemo.document.model.Evidence;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoAnswerGuardTest {

    @Test
    void blocksWhenEmptyOrLowScore() {
        KnowledgeRetrievalProperties properties = new KnowledgeRetrievalProperties();
        properties.setMinAnswerScore(0.45);
        NoAnswerGuard guard = new NoAnswerGuard(properties);

        assertTrue(guard.shouldBlock(List.of()));
        assertTrue(guard.shouldBlock(List.of(new Evidence(1L, 1, "x", null, 0.2, "d", "TEXT"))));
        assertFalse(guard.shouldBlock(List.of(new Evidence(1L, 1, "x", null, 0.8, "d", "TEXT"))));
        assertEquals(NoAnswerGuard.STRICT_REPLY, guard.blockedReply());
    }
}
