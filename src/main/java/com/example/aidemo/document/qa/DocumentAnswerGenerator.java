package com.example.aidemo.document.qa;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.SystemMessage;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentAnswerGenerator {

    private static final String SYSTEM_PROMPT = """
            你是一个有帮助的中文问答助手。
            请优先依据用户消息中 <Reference></Reference> 标记的内容回答。
            若无 Reference，可基于常识简要回答。
            """;

    private final OpenAIChatModel chatModel;
    private final GenerateOptions generateOptions;

    public DocumentAnswerGenerator(OpenAIChatModel chatModel, GenerateOptions generateOptions) {
        this.chatModel = chatModel;
        this.generateOptions = generateOptions;
    }

    public String generate(String userPrompt, Duration timeout) {
        List<Msg> messages = List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(userPrompt));
        ChatResponse response = chatModel.stream(messages, List.of(), generateOptions).blockLast(timeout);
        if (response == null || response.getContent() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : response.getContent()) {
            if (block instanceof TextBlock textBlock) {
                builder.append(textBlock.getText());
            }
        }
        return builder.toString().trim();
    }
}
