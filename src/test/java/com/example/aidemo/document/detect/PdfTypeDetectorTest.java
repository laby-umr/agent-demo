package com.example.aidemo.document.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.aidemo.document.model.PdfType;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfTypeDetectorTest {

    private final PdfTypeDetector detector = new PdfTypeDetector();

    @Test
    void detectsTextPdf(@TempDir Path tempDir) throws IOException {
        Path pdf = tempDir.resolve("text.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("GBT 1568-2008 sample text layer for testing detection logic.");
                stream.endText();
            }
            document.save(pdf.toFile());
        }

        assertEquals(PdfType.TEXT, detector.detect(pdf));
    }

    @Test
    void detectsScannedPdf(@TempDir Path tempDir) throws IOException {
        Path pdf = tempDir.resolve("blank.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }
        assertEquals(PdfType.SCANNED, detector.detect(pdf));
    }
}
