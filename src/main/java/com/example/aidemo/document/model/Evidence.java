package com.example.aidemo.document.model;

public record Evidence(
        long segmentId,
        int page,
        String snippet,
        String contextText,
        double score,
        String docId,
        String blockType) {

    public String promptText() {
        return contextText != null && !contextText.isBlank() ? contextText : snippet;
    }
}
