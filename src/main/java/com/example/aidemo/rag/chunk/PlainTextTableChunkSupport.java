package com.example.aidemo.rag.chunk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** PDF 文本层表格（多空格分列）解析：行级 JSON + 摘要。 */
public final class PlainTextTableChunkSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");

    private PlainTextTableChunkSupport() {}

    public static MarkdownTableChunkSupport.TableChunkArtifacts parse(String rawText, String caption) {
        if (rawText == null || rawText.isBlank()) {
            return new MarkdownTableChunkSupport.TableChunkArtifacts();
        }
        List<String> lines = Arrays.stream(rawText.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.size() < 2) {
            return new MarkdownTableChunkSupport.TableChunkArtifacts();
        }

        List<List<String>> rows = new ArrayList<>();
        for (String line : lines) {
            List<String> cells = splitCells(line);
            if (cells.size() >= 2) {
                rows.add(cells);
            }
        }
        if (rows.size() < 2) {
            return new MarkdownTableChunkSupport.TableChunkArtifacts();
        }

        List<String> headers = rows.get(0);
        List<String> rowJsonLines = new ArrayList<>();
        int dataRowCount = 0;
        for (int i = 1; i < rows.size(); i++) {
            List<String> cells = rows.get(i);
            if (cells.stream().allMatch(cell -> cell.matches("[-—_]+"))) {
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
        if (rowJsonLines.isEmpty()) {
            return new MarkdownTableChunkSupport.TableChunkArtifacts();
        }
        String safeCaption = caption == null || caption.isBlank() ? "表格" : caption;
        String summary = String.format(
                "表格摘要：%s，列字段包括 %s，共 %d 行数据。", safeCaption, String.join("、", headers), dataRowCount);
        return new MarkdownTableChunkSupport.TableChunkArtifacts(headers, rowJsonLines, summary);
    }

    private static List<String> splitCells(String line) {
        return Arrays.stream(MULTI_SPACE.split(line.trim()))
                .map(String::trim)
                .filter(cell -> !cell.isBlank())
                .toList();
    }
}
