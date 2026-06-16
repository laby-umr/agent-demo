package com.example.aidemo.document.parse;

import com.example.aidemo.document.detect.PdfTypeDetector;
import com.example.aidemo.document.extract.ScannedPdfExtractor;
import com.example.aidemo.document.extract.TextLayerPdfExtractor;
import com.example.aidemo.document.model.ParsedBlock;
import com.example.aidemo.document.model.PdfType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 本地 PDF 解析：PDFBox 文本层 + Vision OCR，对齐 ai-demo 原实现。 */
@Component
public class LocalPdfDocumentParseClient {

    private static final Logger log = LoggerFactory.getLogger(LocalPdfDocumentParseClient.class);

    private final PdfTypeDetector pdfTypeDetector;
    private final TextLayerPdfExtractor textLayerPdfExtractor;
    private final ScannedPdfExtractor scannedPdfExtractor;

    public LocalPdfDocumentParseClient(
            PdfTypeDetector pdfTypeDetector,
            TextLayerPdfExtractor textLayerPdfExtractor,
            ScannedPdfExtractor scannedPdfExtractor) {
        this.pdfTypeDetector = pdfTypeDetector;
        this.textLayerPdfExtractor = textLayerPdfExtractor;
        this.scannedPdfExtractor = scannedPdfExtractor;
    }

    public StructuredParseResult parse(Path pdfPath) throws Exception {
        PdfType pdfType = pdfTypeDetector.detect(pdfPath);
        log.info("Local PDF parse type={} file={}", pdfType, pdfPath.getFileName());

        List<ParsedBlock> blocks =
                switch (pdfType) {
                    case TEXT -> textLayerPdfExtractor.extract(pdfPath);
                    case SCANNED -> scannedPdfExtractor.extract(pdfPath);
                    case HYBRID -> {
                        List<ParsedBlock> merged = new ArrayList<>(textLayerPdfExtractor.extract(pdfPath));
                        if (merged.stream().mapToInt(block -> block.text().length()).sum() < 200) {
                            merged.addAll(scannedPdfExtractor.extract(pdfPath));
                        }
                        yield merged;
                    }
                };

        List<StructuredElement> elements = blocks.stream()
                .map(block -> StructuredElement.of(
                        block.contentType(),
                        block.text(),
                        block.page(),
                        block.clauseId() == null ? null : 1))
                .toList();

        String markdown = String.join("\n\n", blocks.stream().map(ParsedBlock::text).toList());
        return new StructuredParseResult(markdown, "LOCAL_" + pdfType.name(), "MEDIUM", false, elements);
    }

    public StructuredParseResult parseBytes(byte[] bytes, String fileName) throws Exception {
        Path temp = Files.createTempFile("ai-demo-parse-", "-" + fileName);
        try {
            Files.write(temp, bytes);
            return parse(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
