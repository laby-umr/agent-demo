package com.example.aidemo.document.ingest;

import com.example.aidemo.config.DocumentProperties;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DocumentStartupIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentStartupIngestor.class);

    private final DocumentIngestionService ingestionService;
    private final DocumentProperties documentProperties;

    public DocumentStartupIngestor(
            DocumentIngestionService ingestionService, DocumentProperties documentProperties) {
        this.ingestionService = ingestionService;
        this.documentProperties = documentProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!documentProperties.autoIngestOnStartup()) {
            return;
        }
        String configuredPath = documentProperties.defaultPdfPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            log.info("未配置 document.default-pdf-path，跳过自动入库");
            return;
        }
        Path pdfPath = Path.of(configuredPath);
        if (!pdfPath.toFile().exists()) {
            log.warn("默认 PDF 不存在，跳过自动入库: {}", pdfPath.toAbsolutePath());
            return;
        }
        try {
            ingestionService.ingest(pdfPath);
        } catch (Exception ex) {
            log.error("自动入库失败: {}", ex.getMessage(), ex);
        }
    }
}
