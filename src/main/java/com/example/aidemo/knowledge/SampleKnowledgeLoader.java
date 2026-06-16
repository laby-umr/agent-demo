package com.example.aidemo.knowledge;

import com.example.aidemo.config.AgentScopeProperties;
import com.example.aidemo.service.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SampleKnowledgeLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleKnowledgeLoader.class);

    private final KnowledgeService knowledgeService;
    private final AgentScopeProperties properties;

    public SampleKnowledgeLoader(KnowledgeService knowledgeService, AgentScopeProperties properties) {
        this.knowledgeService = knowledgeService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.sampleKnowledgeEnabled()) {
            log.info("Skip sample knowledge preload (disabled)");
            return;
        }
        ClassPathResource resource = new ClassPathResource("knowledge/sample.txt");
        if (!resource.exists()) {
            log.warn("Sample knowledge file not found, skip preload");
            return;
        }

        String content = new String(resource.getInputStream().readAllBytes());
        try {
            knowledgeService.addDocument(content);
            log.info("Loaded sample knowledge document ({} chars)", content.length());
        } catch (Exception ex) {
            log.warn("Failed to preload sample knowledge (Ollama may be offline): {}", ex.getMessage());
        }
    }
}
