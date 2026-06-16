package com.example.aidemo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties({
    AgentScopeProperties.class,
    DocumentProperties.class,
    AiDocumentParseProperties.class,
    KnowledgeRetrievalProperties.class
})
public class AgentScopeConfig {
}
