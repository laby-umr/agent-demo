package com.example.aidemo.document.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aidemo.document.model.ParsedBlock;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockChunkMergerTest {

    @Test
    void skipsNoiseAndMergesSamePage() {
        List<ParsedBlock> input = List.of(
                new ParsedBlock(1, "CLSA Aviation II Investments", "table", null),
                new ParsedBlock(1, "-", "table", null),
                new ParsedBlock(1, "Fund I 883,193.45 70,622.22", "table", null),
                new ParsedBlock(2, "41 其他综合收益", "paragraph", null),
                new ParsedBlock(2, "2025年 1月 1日至 2025年 6月 30日止期间", "paragraph", null));

        List<ParsedBlock> merged = BlockChunkMerger.merge(input);
        assertEquals(2, merged.size());
        assertTrue(merged.get(0).text().contains("CLSA Aviation"));
        assertTrue(merged.get(0).text().contains("Fund I"));
        assertEquals(1, merged.get(0).page());
    }
}
