package com.example.aidemo.document.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aidemo.config.DocumentProperties;
import com.example.aidemo.document.detect.PdfTypeDetector;
import com.example.aidemo.document.extract.TextLayerPdfExtractor;
import com.example.aidemo.document.model.ParsedBlock;
import com.example.aidemo.document.model.PdfType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentIngestionPipelineTest {

    private final PdfTypeDetector detector = new PdfTypeDetector();
    private final TextLayerPdfExtractor extractor = new TextLayerPdfExtractor();

    @Test
    void smokeExtractTextLayerPdf(@TempDir Path tempDir) throws Exception {
        Path pdf = SmokePdfFactory.createSample(tempDir.resolve("smoke.pdf"));

        assertEquals(PdfType.TEXT, detector.detect(pdf));

        List<ParsedBlock> blocks = extractor.extract(pdf);
        assertTrue(blocks.size() >= 1);
        assertTrue(blocks.get(0).text().contains("GBT 1568-2008"));
    }
}
