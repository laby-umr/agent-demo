package com.example.aidemo.document.model;

public record ParsedBlock(
        int page,
        String text,
        String contentType,
        String clauseId) {}
