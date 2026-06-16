package com.example.aidemo.document.ingest;

import com.example.aidemo.document.model.ParsedBlock;
import java.util.ArrayList;
import java.util.List;

/** 合并 OCR 碎行，减少 embedding 次数、提升检索质量。 */
public final class BlockChunkMerger {

    private static final int TARGET_CHARS = 800;
    private static final int MIN_CHARS = 20;

    private BlockChunkMerger() {}

    public static List<ParsedBlock> merge(List<ParsedBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        List<ParsedBlock> merged = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int page = blocks.get(0).page();
        String contentType = blocks.get(0).contentType();

        for (ParsedBlock block : blocks) {
            if (!isMeaningful(block.text())) {
                continue;
            }
            if (buffer.length() > 0
                    && (block.page() != page
                            || buffer.length() + block.text().length() > TARGET_CHARS)) {
                flush(merged, page, contentType, buffer);
                buffer = new StringBuilder();
            }
            page = block.page();
            contentType = block.contentType();
            if (buffer.length() > 0) {
                buffer.append('\n');
            }
            buffer.append(block.text().trim());
        }
        if (buffer.length() > 0) {
            flush(merged, page, contentType, buffer);
        }
        return merged;
    }

    private static void flush(List<ParsedBlock> merged, int page, String contentType, StringBuilder buffer) {
        String text = buffer.toString().trim();
        if (text.length() >= MIN_CHARS || text.matches(".*\\d+\\.\\d+.*")) {
            merged.add(new ParsedBlock(page, text, contentType, null));
        }
    }

    static boolean isMeaningful(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (trimmed.matches("^[-–—\\d\\s\\.]+$")) {
            return false;
        }
        if (trimmed.length() < 8 && !trimmed.matches(".*\\d+\\.\\d+.*")) {
            return false;
        }
        return true;
    }
}
