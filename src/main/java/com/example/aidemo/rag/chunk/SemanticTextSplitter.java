package com.example.aidemo.rag.chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 语义化文本切片：段落 → 句子 → 字符，默认 512 token / 80 重叠。 */
public class SemanticTextSplitter {

    private static final List<String> PARAGRAPH_SEPARATORS = List.of("\n\n\n", "\n\n", "\n");
    private static final Pattern SENTENCE_END_PATTERN =
            Pattern.compile("[。！？.!?]+[\\s\"'）)】\\]]*");

    private final int chunkSize;
    private final int chunkOverlap;

    public SemanticTextSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = Math.min(chunkOverlap, chunkSize / 2);
    }

    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return splitTextRecursive(text);
    }

    private List<String> splitTextRecursive(String text) {
        List<String> chunks = new ArrayList<>();
        if (estimateTokens(text) <= chunkSize) {
            chunks.add(text.trim());
            return chunks;
        }

        List<String> splits = null;
        String usedSeparator = null;
        for (String separator : PARAGRAPH_SEPARATORS) {
            if (text.contains(separator)) {
                splits = Arrays.asList(text.split(Pattern.quote(separator)));
                usedSeparator = separator;
                break;
            }
        }
        if (splits == null || splits.size() == 1) {
            splits = splitBySentences(text);
            usedSeparator = "";
        }
        return mergeSplits(splits, usedSeparator);
    }

    private List<String> splitBySentences(String text) {
        List<String> sentences = new ArrayList<>();
        int lastEnd = 0;
        Matcher matcher = SENTENCE_END_PATTERN.matcher(text);
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.end()).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                sentences.add(remaining);
            }
        }
        return sentences.isEmpty() ? List.of(text) : sentences;
    }

    private List<String> mergeSplits(List<String> splits, String separator) {
        List<String> chunks = new ArrayList<>();
        List<String> currentChunks = new ArrayList<>();
        int currentLength = 0;

        for (String split : splits) {
            if (split == null || split.isBlank()) {
                continue;
            }
            int splitTokens = estimateTokens(split);
            if (splitTokens > chunkSize) {
                if (!currentChunks.isEmpty()) {
                    chunks.add(String.join(separator, currentChunks).trim());
                    currentChunks.clear();
                    currentLength = 0;
                }
                if (!separator.isEmpty()) {
                    chunks.addAll(splitTextRecursive(split));
                } else {
                    chunks.addAll(forceSplitLongText(split));
                }
                continue;
            }
            int separatorTokens = separator.isEmpty() ? 0 : estimateTokens(separator);
            if (!currentChunks.isEmpty() && currentLength + splitTokens + separatorTokens > chunkSize) {
                chunks.add(String.join(separator, currentChunks).trim());
                currentChunks = getOverlappingChunks(currentChunks, separator);
                currentLength = estimateTokens(currentChunks, separator);
            }
            currentChunks.add(split);
            currentLength += splitTokens + separatorTokens;
        }
        if (!currentChunks.isEmpty()) {
            chunks.add(String.join(separator, currentChunks).trim());
        }
        return chunks;
    }

    private List<String> getOverlappingChunks(List<String> chunks, String separator) {
        if (chunkOverlap == 0 || chunks.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> overlapping = new ArrayList<>();
        int tokens = 0;
        for (int i = chunks.size() - 1; i >= 0; i--) {
            String chunk = chunks.get(i);
            int chunkTokens = estimateTokens(chunk);
            if (tokens + chunkTokens > chunkOverlap) {
                break;
            }
            overlapping.add(0, chunk);
            tokens += chunkTokens + (separator.isEmpty() ? 0 : estimateTokens(separator));
        }
        return overlapping;
    }

    private int estimateTokens(List<String> chunks, String separator) {
        int total = 0;
        for (int i = 0; i < chunks.size(); i++) {
            total += estimateTokens(chunks.get(i));
            if (i < chunks.size() - 1 && !separator.isEmpty()) {
                total += estimateTokens(separator);
            }
        }
        return total;
    }

    private List<String> forceSplitLongText(String text) {
        List<String> chunks = new ArrayList<>();
        int charsPerChunk = (int) (chunkSize * 0.8);
        for (int i = 0; i < text.length(); i += charsPerChunk) {
            chunks.add(text.substring(i, Math.min(i + charsPerChunk, text.length())).trim());
        }
        return chunks;
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int chineseChars = 0;
        int englishWords = 0;
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) {
                chineseChars++;
            }
        }
        for (String word : text.split("\\s+")) {
            if (word.matches(".*[a-zA-Z].*")) {
                englishWords++;
            }
        }
        return chineseChars + (int) (englishWords * 1.3);
    }
}
