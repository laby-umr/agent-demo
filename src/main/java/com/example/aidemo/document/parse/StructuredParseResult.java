package com.example.aidemo.document.parse;

import java.util.List;

public record StructuredParseResult(
        String markdown,
        String engine,
        String quality,
        boolean degraded,
        List<StructuredElement> elements) {

    public static StructuredParseResult empty(String engine) {
        return new StructuredParseResult("", engine, "LOW", true, List.of());
    }

    /** 高质量 elements 可用时走层级分片，否则语义切分。 */
    public boolean supportsStructuredChunking() {
        if (elements == null || elements.isEmpty()) {
            return false;
        }
        if ("LOW".equalsIgnoreCase(quality) || degraded) {
            return false;
        }
        return elements.stream().anyMatch(element -> element.type() != null && !element.type().isBlank());
    }
}
