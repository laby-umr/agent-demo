package com.example.aidemo.document.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Generates a tiny text-layer PDF for local smoke tests when assignment scan PDF is unavailable. */
public final class SmokePdfFactory {

    private SmokePdfFactory() {}

    public static Path createSample(Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(40, 700);
                stream.showText("GBT 1568-2008 sample: key technical requirements and inspection methods.");
                stream.endText();
            }
            document.save(target.toFile());
        }
        return target;
    }
}
