package com.example.aidemo.document.parse;

import com.example.aidemo.config.AiDocumentParseProperties;
import com.example.aidemo.document.model.ParsedBlock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文档解析路由，策略对齐 laby-admin {@code DocumentParseRouter}：
 * PDF 优先 MinerU，失败/不可用时降级本地 PDFBox + Vision OCR。
 */
@Component
public class DocumentParseRouter {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseRouter.class);

    private final AiDocumentParseProperties properties;
    private final MineruDocumentParseClient mineruClient;
    private final LocalPdfDocumentParseClient localPdfClient;

    public DocumentParseRouter(
            AiDocumentParseProperties properties,
            MineruDocumentParseClient mineruClient,
            LocalPdfDocumentParseClient localPdfClient) {
        this.properties = properties;
        this.mineruClient = mineruClient;
        this.localPdfClient = localPdfClient;
    }

    public StructuredParseResult parse(Path pdfPath) throws Exception {
        byte[] bytes = Files.readAllBytes(pdfPath);
        return parse(bytes, pdfPath.getFileName().toString(), pdfPath);
    }

    public StructuredParseResult parse(byte[] bytes, String fileName, Path pdfPathForLocal) throws Exception {
        if (!properties.enabled()) {
            return localPdfClient.parse(pdfPathForLocal != null ? pdfPathForLocal : writeTemp(bytes, fileName));
        }

        String ext = extension(fileName);
        boolean mineruAttempted = false;
        if ("pdf".equals(ext) && shouldUseMineru()) {
            mineruAttempted = true;
            try {
                StructuredParseResult mineru = mineruClient.parse(bytes, fileName);
                if (mineru.markdown() != null && !mineru.markdown().isBlank()) {
                    return mineru;
                }
                log.warn("MinerU returned empty markdown, degrade to local PDF parser");
            } catch (Exception ex) {
                log.warn("MinerU parse failed, degrade to local PDF parser: {}", ex.getMessage());
            }
        }

        Path localPath = pdfPathForLocal != null ? pdfPathForLocal : writeTemp(bytes, fileName);
        StructuredParseResult local = localPdfClient.parse(localPath);
        if (pdfPathForLocal == null) {
            Files.deleteIfExists(localPath);
        }
        if (mineruAttempted) {
            return new StructuredParseResult(
                    local.markdown(), local.engine(), local.quality(), true, local.elements());
        }
        return local;
    }

    public List<ParsedBlock> toBlocks(StructuredParseResult result) {
        if (result.elements() != null && !result.elements().isEmpty()) {
            List<ParsedBlock> blocks = new ArrayList<>();
            for (StructuredElement element : result.elements()) {
                String text = pickText(element);
                if (text.isBlank()) {
                    continue;
                }
                int page = element.page() == null ? 1 : element.page();
                String clauseId = element.level() != null ? String.valueOf(element.level()) : null;
                blocks.add(new ParsedBlock(page, text, element.type(), clauseId));
            }
            if (!blocks.isEmpty()) {
                return blocks;
            }
        }
        return splitMarkdown(result.markdown());
    }

    private boolean shouldUseMineru() {
        if (!"auto".equalsIgnoreCase(properties.defaultEngine())
                && !"mineru".equalsIgnoreCase(properties.defaultEngine())) {
            return "mineru".equalsIgnoreCase(properties.defaultEngine()) && mineruClient.isAvailable();
        }
        return mineruClient.isAvailable();
    }

    private static List<ParsedBlock> splitMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        List<ParsedBlock> blocks = new ArrayList<>();
        int page = 1;
        for (String paragraph : markdown.split("\\R\\R+")) {
            String trimmed = paragraph.trim();
            if (!trimmed.isBlank()) {
                blocks.add(new ParsedBlock(page, trimmed, "paragraph", null));
            }
        }
        return blocks;
    }

    private static String pickText(StructuredElement element) {
        if (element.markdown() != null && !element.markdown().isBlank()) {
            return element.markdown().trim();
        }
        return element.text() == null ? "" : element.text().trim();
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static Path writeTemp(byte[] bytes, String fileName) throws Exception {
        Path temp = Files.createTempFile("ai-demo-doc-", "-" + fileName);
        Files.write(temp, bytes);
        return temp;
    }
}
