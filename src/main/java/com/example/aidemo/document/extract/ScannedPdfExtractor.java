package com.example.aidemo.document.extract;

import com.example.aidemo.config.DocumentProperties;
import com.example.aidemo.document.model.ParsedBlock;
import com.example.aidemo.document.ocr.VisionLlmPageOcrEngine;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ScannedPdfExtractor {

    private static final Logger log = LoggerFactory.getLogger(ScannedPdfExtractor.class);

    private final VisionLlmPageOcrEngine ocrEngine;
    private final DocumentProperties documentProperties;

    public ScannedPdfExtractor(VisionLlmPageOcrEngine ocrEngine, DocumentProperties documentProperties) {
        this.ocrEngine = ocrEngine;
        this.documentProperties = documentProperties;
    }

    public List<ParsedBlock> extract(Path pdfPath) throws IOException, InterruptedException {
        List<ParsedBlock> blocks = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = document.getNumberOfPages();
            int maxPages = documentProperties.ocr().maxPages();
            int limit = maxPages > 0 ? Math.min(maxPages, pages) : pages;

            log.info("OCR extracting {} / {} pages from {}", limit, pages, pdfPath.getFileName());
            for (int pageIndex = 0; pageIndex < limit; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);
                String text = ocrEngine.transcribePage(image, pageIndex + 1);
                for (String paragraph : text.split("\\R\\R+")) {
                    String trimmed = paragraph.trim();
                    if (trimmed.isBlank()) {
                        continue;
                    }
                    blocks.add(new ParsedBlock(pageIndex + 1, trimmed, detectContentType(trimmed), null));
                }
            }
        }
        return blocks;
    }

    private String detectContentType(String text) {
        if (text.startsWith("|") || text.contains("\t") || text.matches(".*\\s{2,}\\S+.*")) {
            return "table";
        }
        return "paragraph";
    }
}
