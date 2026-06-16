package com.example.aidemo.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aidemo.rag.model.QueryIntent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RrfFusionTest {

    @Test
    void fusesDenseAndSparseLists() {
        Map<Long, Double> fused = RrfFusion.fuse(
                60,
                List.of(
                        new RrfFusion.WeightedRankedList(1.0, List.of(10L, 20L)),
                        new RrfFusion.WeightedRankedList(1.2, List.of(20L, 30L))));
        assertTrue(fused.containsKey(20L));
        assertTrue(fused.get(20L) > fused.get(10L));
        assertTrue(fused.get(20L) > fused.get(30L));
    }
}
