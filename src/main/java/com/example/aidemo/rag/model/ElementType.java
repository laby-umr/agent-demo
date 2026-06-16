package com.example.aidemo.rag.model;

public enum ElementType {
    TITLE,
    TEXT,
    TABLE,
    IMAGE,
    FORMULA;

    public static ElementType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return TEXT;
        }
        return switch (code.toLowerCase()) {
            case "title", "heading" -> TITLE;
            case "table" -> TABLE;
            case "image", "figure" -> IMAGE;
            case "formula", "equation" -> FORMULA;
            default -> TEXT;
        };
    }
}
