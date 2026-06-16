package com.example.aidemo.document.extract;

import com.example.aidemo.document.model.ParsedBlock;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class TextLayerPdfExtractor {

    private static final Pattern CLAUSE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\s+");

    public List<ParsedBlock> extract(Path pdfPath) throws IOException {
        List<ParsedBlock> blocks = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pages = document.getNumberOfPages();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                for (String paragraph : pageText.split("\\R\\R+")) {
                    String trimmed = paragraph.trim();
                    if (trimmed.isBlank()) {
                        continue;
                    }
                    blocks.add(new ParsedBlock(
                            page,
                            trimmed,
                            detectContentType(trimmed),
                            detectClauseId(trimmed)));
                }
            }
        }
        return blocks;
    }

    private String detectContentType(String text) {
        if (text.contains("\t") || text.matches(".*\\s{2,}\\S+.*\\s{2,}\\S+.*")) {
            return "table";
        }
        return "paragraph";
    }

    private String detectClauseId(String text) {
        Matcher matcher = CLAUSE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
