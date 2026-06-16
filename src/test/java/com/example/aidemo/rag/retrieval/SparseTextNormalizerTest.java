package com.example.aidemo.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SparseTextNormalizerTest {

    @Test
    void stripsMarkdownNoise() {
        String normalized = SparseTextNormalizer.normalize("## 键 **技术** | 条件 [p.3]");
        assertFalse(normalized.contains("#"));
        assertFalse(normalized.contains("|"));
        assertTrue(normalized.contains("键"));
        assertTrue(normalized.contains("技术"));
    }
}
