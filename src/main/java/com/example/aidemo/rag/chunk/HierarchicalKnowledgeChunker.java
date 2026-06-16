package com.example.aidemo.rag.chunk;

import com.example.aidemo.config.AiDocumentParseProperties;
import com.example.aidemo.document.parse.StructuredElement;
import com.example.aidemo.document.parse.StructuredParseResult;
import com.example.aidemo.rag.model.BlockType;
import com.example.aidemo.rag.model.ChunkLevel;
import com.example.aidemo.rag.model.ElementType;
import com.example.aidemo.rag.model.KnowledgeChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** 结构化层级分片：Parent-Child + 表格三索引 + 图片/公式独立块。 */
@Component
public class HierarchicalKnowledgeChunker {

    private final AiDocumentParseProperties.StructuredChunkConfig config;
    private final SemanticTextSplitter textSplitter;

    public HierarchicalKnowledgeChunker(AiDocumentParseProperties properties) {
        this.config = properties.structuredChunk();
        this.textSplitter = new SemanticTextSplitter(config.childMaxTokens(), config.childOverlapTokens());
    }

    public List<KnowledgeChunk> chunk(StructuredParseResult parseResult) {
        if (parseResult == null || !parseResult.supportsStructuredChunking()) {
            return chunkPlainText(parseResult != null ? parseResult.markdown() : "");
        }
        List<KnowledgeChunk> chunks = new ArrayList<>();
        AtomicInteger sectionCounter = new AtomicInteger();
        StringBuilder parentBuffer = new StringBuilder();
        String currentParentKey = null;
        List<String> headingStack = new ArrayList<>();

        for (StructuredElement element : parseResult.elements()) {
            ElementType type = ElementType.fromCode(element.type());
            switch (type) {
                case TITLE -> {
                    flushParent(chunks, parentBuffer, currentParentKey, headingStack);
                    updateHeadingStack(headingStack, element);
                    currentParentKey = "section-" + sectionCounter.incrementAndGet();
                    parentBuffer.setLength(0);
                    appendParentLine(parentBuffer, element.text());
                }
                case TABLE -> chunks.addAll(buildTableChunks(element, headingStack));
                case IMAGE -> chunks.add(buildImageChunk(element, headingStack));
                case FORMULA -> chunks.add(buildFormulaChunk(element, headingStack));
                case TEXT -> {
                    appendParentLine(parentBuffer, element.text());
                    chunks.addAll(buildTextChildChunks(element, headingStack, currentParentKey));
                }
            }
        }
        flushParent(chunks, parentBuffer, currentParentKey, headingStack);
        return chunks.stream().filter(chunk -> chunk.content() != null && !chunk.content().isBlank()).toList();
    }

    private List<KnowledgeChunk> chunkPlainText(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (String segment : textSplitter.split(markdown)) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            chunks.add(new KnowledgeChunk()
                    .content(segment)
                    .embedText(segment)
                    .blockType(BlockType.TEXT)
                    .chunkLevel(ChunkLevel.CHILD)
                    .embedEnabled(true));
        }
        return chunks;
    }

    private void flushParent(
            List<KnowledgeChunk> chunks,
            StringBuilder parentBuffer,
            String parentTempKey,
            List<String> headingStack) {
        if (parentTempKey == null || parentBuffer.isEmpty()) {
            return;
        }
        String content = parentBuffer.toString().trim();
        if (content.isBlank()) {
            return;
        }
        chunks.add(new KnowledgeChunk()
                .content(content)
                .embedText(buildEmbedText(headingStack, content))
                .blockType(BlockType.TEXT)
                .chunkLevel(ChunkLevel.PARENT)
                .tempKey(parentTempKey)
                .headingPath(joinHeadingPath(headingStack))
                .embedEnabled(config.embedParent()));
    }

    private List<KnowledgeChunk> buildTextChildChunks(
            StructuredElement element, List<String> headingStack, String parentTempKey) {
        if (element.text() == null || element.text().isBlank()) {
            return List.of();
        }
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (String segment : textSplitter.split(element.text())) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            chunks.add(new KnowledgeChunk()
                    .content(segment)
                    .embedText(buildEmbedText(headingStack, segment))
                    .blockType(BlockType.TEXT)
                    .chunkLevel(ChunkLevel.CHILD)
                    .parentTempKey(parentTempKey)
                    .headingPath(joinHeadingPath(headingStack))
                    .pageStart(element.page())
                    .pageEnd(element.page())
                    .embedEnabled(true));
        }
        return chunks;
    }

    private List<KnowledgeChunk> buildTableChunks(StructuredElement element, List<String> headingStack) {
        String tableMarkdown = blankToDefault(element.markdown(), element.text());
        String caption = blankToDefault(element.caption(), "表格");
        String wholeContent = caption.isBlank() ? tableMarkdown : caption + "\n" + tableMarkdown;
        String headingPath = joinHeadingPath(headingStack);

        List<KnowledgeChunk> chunks = new ArrayList<>();
        chunks.add(new KnowledgeChunk()
                .content(wholeContent.trim())
                .embedText(buildEmbedText(headingStack, wholeContent))
                .blockType(BlockType.TABLE_WHOLE)
                .chunkLevel(ChunkLevel.CHILD)
                .headingPath(headingPath)
                .pageStart(element.page())
                .pageEnd(element.page())
                .embedEnabled(true));

        MarkdownTableChunkSupport.TableChunkArtifacts artifacts =
                MarkdownTableChunkSupport.parse(tableMarkdown, caption);
        if (!artifacts.hasRows()) {
            artifacts = PlainTextTableChunkSupport.parse(tableMarkdown, caption);
        }
        if (config.tableRowIndexEnabled() && artifacts.hasRows()) {
            for (String rowJson : artifacts.rowJsonLines()) {
                String rowContent = caption + " " + rowJson;
                chunks.add(new KnowledgeChunk()
                        .content(rowContent.trim())
                        .embedText(buildEmbedText(headingStack, rowContent))
                        .blockType(BlockType.TABLE_ROW)
                        .chunkLevel(ChunkLevel.CHILD)
                        .headingPath(headingPath)
                        .pageStart(element.page())
                        .pageEnd(element.page())
                        .embedEnabled(true));
            }
        }
        if (config.tableSummaryEnabled() && artifacts.summary() != null && !artifacts.summary().isBlank()) {
            chunks.add(new KnowledgeChunk()
                    .content(artifacts.summary())
                    .embedText(buildEmbedText(headingStack, artifacts.summary()))
                    .blockType(BlockType.TABLE_SUMMARY)
                    .chunkLevel(ChunkLevel.CHILD)
                    .headingPath(headingPath)
                    .pageStart(element.page())
                    .pageEnd(element.page())
                    .embedEnabled(true));
        }
        return chunks;
    }

    private KnowledgeChunk buildImageChunk(StructuredElement element, List<String> headingStack) {
        StringBuilder content = new StringBuilder();
        if (element.caption() != null && !element.caption().isBlank()) {
            content.append(element.caption());
        }
        if (element.description() != null && !element.description().isBlank()) {
            if (!content.isEmpty()) {
                content.append("\n");
            }
            content.append(element.description());
        }
        if (element.text() != null && !element.text().isBlank()) {
            if (!content.isEmpty()) {
                content.append("\n");
            }
            content.append(element.text());
        }
        String body = content.toString().trim();
        return new KnowledgeChunk()
                .content(body)
                .embedText(buildEmbedText(headingStack, body))
                .blockType(BlockType.IMAGE)
                .chunkLevel(ChunkLevel.CHILD)
                .headingPath(joinHeadingPath(headingStack))
                .pageStart(element.page())
                .pageEnd(element.page())
                .embedEnabled(true);
    }

    private KnowledgeChunk buildFormulaChunk(StructuredElement element, List<String> headingStack) {
        String body = "[公式] " + blankToDefault(element.text(), "");
        return new KnowledgeChunk()
                .content(body.trim())
                .embedText(buildEmbedText(headingStack, body))
                .blockType(BlockType.FORMULA)
                .chunkLevel(ChunkLevel.CHILD)
                .headingPath(joinHeadingPath(headingStack))
                .pageStart(element.page())
                .pageEnd(element.page())
                .embedEnabled(true);
    }

    private static void updateHeadingStack(List<String> headingStack, StructuredElement element) {
        int level = element.level() != null && element.level() > 0 ? element.level() : 2;
        while (headingStack.size() >= level) {
            headingStack.remove(headingStack.size() - 1);
        }
        headingStack.add(blankToDefault(element.text(), ""));
    }

    private static void appendParentLine(StringBuilder parentBuffer, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!parentBuffer.isEmpty()) {
            parentBuffer.append("\n");
        }
        parentBuffer.append(text.trim());
    }

    private static String joinHeadingPath(List<String> headingStack) {
        return headingStack.stream()
                .filter(item -> item != null && !item.isBlank())
                .reduce((a, b) -> a + " > " + b)
                .orElse("");
    }

    private static String buildEmbedText(List<String> headingStack, String content) {
        String headingPath = joinHeadingPath(headingStack);
        if (headingPath.isBlank()) {
            return content;
        }
        return headingPath + "\n" + content;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
