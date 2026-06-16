package com.example.aidemo.rag.chunk;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aidemo.document.parse.StructuredElement;
import com.example.aidemo.document.parse.StructuredParseResult;
import com.example.aidemo.rag.model.BlockType;
import com.example.aidemo.rag.model.KnowledgeChunk;
import com.example.aidemo.config.AiDocumentParseProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class HierarchicalKnowledgeChunkerTest {

    @Test
    void chunksTableIntoTripleIndex() {
        AiDocumentParseProperties properties = new AiDocumentParseProperties();
        properties.structuredChunk().setTableRowIndexEnabled(true);
        properties.structuredChunk().setTableSummaryEnabled(true);

        StructuredParseResult parseResult = new StructuredParseResult(
                "",
                "TEST",
                "HIGH",
                false,
                List.of(
                        StructuredElement.of("title", "薪酬表", 1, 1),
                        new StructuredElement(
                                "table",
                                "",
                                "| Name | Age |\n| --- | --- |\n| 张三 | 28 |",
                                "员工表",
                                null,
                                2,
                                null)));

        HierarchicalKnowledgeChunker chunker = new HierarchicalKnowledgeChunker(properties);
        List<KnowledgeChunk> chunks = chunker.chunk(parseResult);

        assertTrue(chunks.stream().anyMatch(c -> c.blockType() == BlockType.TABLE_WHOLE));
        assertTrue(chunks.stream().anyMatch(c -> c.blockType() == BlockType.TABLE_ROW));
        assertTrue(chunks.stream().anyMatch(c -> c.blockType() == BlockType.TABLE_SUMMARY));
    }
}
