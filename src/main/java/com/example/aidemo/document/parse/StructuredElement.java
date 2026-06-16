package com.example.aidemo.document.parse;

public record StructuredElement(
        String type,
        String text,
        String markdown,
        String caption,
        String description,
        Integer page,
        Integer level) {

    public static StructuredElement of(String type, String text, Integer page, Integer level) {
        return new StructuredElement(type, text, text, null, null, page, level);
    }
}
