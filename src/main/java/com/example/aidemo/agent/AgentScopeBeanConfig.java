package com.example.aidemo.agent;

import com.example.aidemo.config.AgentScopeProperties;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentScopeBeanConfig {

    @Bean
    ExecutionConfig chatExecutionConfig(AgentScopeProperties properties) {
        return ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(properties.chat().requestTimeoutSeconds()))
                .maxAttempts(2)
                .build();
    }

    @Bean
    OpenAIChatModel chatModel(AgentScopeProperties properties, ExecutionConfig chatExecutionConfig) {
        AgentScopeProperties.Chat chat = properties.chat();
        return OpenAIChatModel.builder()
                .baseUrl(chat.baseUrl())
                .apiKey(chat.apiKey())
                .modelName(chat.model())
                .stream(false)
                .generateOptions(GenerateOptions.builder()
                        .stream(false)
                        .executionConfig(chatExecutionConfig)
                        .build())
                .build();
    }

    @Bean
    GenerateOptions chatGenerateOptions(ExecutionConfig chatExecutionConfig) {
        return GenerateOptions.builder()
                .stream(false)
                .executionConfig(chatExecutionConfig)
                .build();
    }

    @Bean
    ReActAgent ragReactAgent(
            OpenAIChatModel chatModel,
            Knowledge knowledge,
            AgentScopeProperties properties,
            ExecutionConfig chatExecutionConfig) {
        AgentScopeProperties.Rag rag = properties.rag();
        return ReActAgent.builder()
                .name("rag-assistant")
                .sysPrompt("""
                        你是一个有帮助的中文问答助手。
                        请优先依据系统提供的知识库内容回答问题。
                        如果知识库中没有相关信息，请明确说明并基于常识简要回答。
                        回答请简洁，控制在 200 字以内。
                        """)
                .model(chatModel)
                .toolkit(new Toolkit())
                .knowledge(knowledge)
                .ragMode(RAGMode.GENERIC)
                .maxIters(properties.agent().maxIters())
                .modelExecutionConfig(chatExecutionConfig)
                .retrieveConfig(RetrieveConfig.builder()
                        .limit(rag.topK())
                        .scoreThreshold(rag.scoreThreshold())
                        .build())
                .checkRunning(false)
                .build();
    }

    @Bean
    HarnessAgent ragAgent(ReActAgent ragReactAgent) {
        return HarnessAgent.builder()
                .fromAgent(ragReactAgent)
                .checkRunning(false)
                .disableSessionPersistence()
                .build();
    }
}
