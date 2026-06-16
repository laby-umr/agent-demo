package com.example.aidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "document")
public record DocumentProperties(String defaultPdfPath, boolean autoIngestOnStartup, Ocr ocr) {

    public record Ocr(boolean enabled, int maxPages) {}
}
