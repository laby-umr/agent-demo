package com.example.aidemo.rag.model;

public enum BlockType {
    TEXT,
    TABLE_WHOLE,
    TABLE_ROW,
    TABLE_SUMMARY,
    IMAGE,
    FORMULA;

    public static BlockType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return TEXT;
        }
        try {
            return BlockType.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TEXT;
        }
    }
}
