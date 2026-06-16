package com.example.aidemo.document.detect;

import com.example.aidemo.document.model.PdfType;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTypeDetector {

    private static final int MIN_CHARS_PER_PAGE = 40;

    public PdfType detect(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int pages = document.getNumberOfPages();
            if (pages == 0) {
                return PdfType.SCANNED;
            }

            PDFTextStripper stripper = new PDFTextStripper();
            int textPages = 0;
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).replaceAll("\\s+", "");
                if (text.length() >= MIN_CHARS_PER_PAGE) {
                    textPages++;
                }
            }

            if (textPages == 0) {
                return PdfType.SCANNED;
            }
            if (textPages == pages) {
                return PdfType.TEXT;
            }
            return PdfType.HYBRID;
        }
    }
}
