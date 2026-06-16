package com.example.aidemo.rag.chunk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Markdown 表格解析：行级 JSON + 摘要。 */
public final class MarkdownTableChunkSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MarkdownTableChunkSupport() {}

    public static TableChunkArtifacts parse(String markdown, String caption) {
        if (markdown == null || markdown.isBlank()) {
            return new TableChunkArtifacts();
        }
        List<String> lines = Arrays.stream(markdown.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(MarkdownTableChunkSupport::isTableLine)
                .toList();
        if (lines.size() < 2) {
            return new TableChunkArtifacts();
        }
        List<String> headers = splitCells(lines.get(0));
        if (headers.isEmpty()) {
            return new TableChunkArtifacts();
        }
        List<String> rowJsonLines = new ArrayList<>();
        int dataRowCount = 0;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isSeparatorLine(line)) {
                continue;
            }
            List<String> cells = splitCells(line);
            if (cells.isEmpty()) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int col = 0; col < headers.size(); col++) {
                row.put(headers.get(col), col < cells.size() ? cells.get(col) : "");
            }
            try {
                rowJsonLines.add(MAPPER.writeValueAsString(row));
            } catch (JsonProcessingException ex) {
                rowJsonLines.add(row.toString());
            }
            dataRowCount++;
        }
        String safeCaption = caption == null || caption.isBlank() ? "表格" : caption;
        String summary = String.format(
                "表格摘要：%s，列字段包括 %s，共 %d 行数据。", safeCaption, String.join("、", headers), dataRowCount);
        return new TableChunkArtifacts(headers, rowJsonLines, summary);
    }

    private static boolean isTableLine(String line) {
        return line.startsWith("|") && line.endsWith("|");
    }

    private static boolean isSeparatorLine(String line) {
        String normalized = line.replace("|", "").replace(":", "").replace("-", "").trim();
        return normalized.isEmpty();
    }

    private static List<String> splitCells(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return Arrays.stream(trimmed.split("\\|"))
                .map(String::trim)
                .filter(cell -> !cell.isBlank())
                .toList();
    }

    public record TableChunkArtifacts(List<String> headers, List<String> rowJsonLines, String summary) {

        public TableChunkArtifacts() {
            this(List.of(), List.of(), null);
        }

        public boolean hasRows() {
            return rowJsonLines != null && !rowJsonLines.isEmpty();
        }
    }
}
