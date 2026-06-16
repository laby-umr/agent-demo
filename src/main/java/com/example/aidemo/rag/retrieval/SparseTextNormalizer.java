package com.example.aidemo.rag.retrieval;

import java.util.regex.Pattern;

/** 全文检索用纯文本归一化（去 Markdown 噪声，保留中英数字）。 */
public final class SparseTextNormalizer {

    private static final Pattern MARKDOWN_NOISE = Pattern.compile("[#*_>`\\[\\](){}|~\\-]+");
    private static final Pattern NON_SEARCHABLE = Pattern.compile("[^\\p{IsHan}\\p{L}\\p{N}\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SparseTextNormalizer() {}

    public static String stripMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replace('|', ' ');
        normalized = MARKDOWN_NOISE.matcher(normalized).replaceAll(" ");
        normalized = NON_SEARCHABLE.matcher(normalized).replaceAll(" ");
        return WHITESPACE.matcher(normalized.trim()).replaceAll(" ");
    }

    public static String normalize(String text) {
        return stripMarkdown(text);
    }
}
