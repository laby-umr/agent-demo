package com.example.aidemo.document.qa;

import com.example.aidemo.document.model.Evidence;
import java.util.stream.Collectors;

/** Grounded 提示词构建，Reference 使用上下文回填后的 expanded 内容。 */
public final class GroundedPromptBuilder {

    private static final String KNOWLEDGE_USER_MESSAGE_TEMPLATE =
            """
            使用 <Reference></Reference> 标记中的内容作为本次对话的参考:

            %s

            回答要求：
            - 避免提及你是从 <Reference></Reference> 获取的知识。
            - 只能依据 Reference 内容回答；若依据不足请明确说明。
            - 必须在答案末尾标注引用页码，格式 [p.X]；X 必须来自 Reference 的 page 属性，不得臆造页码范围。
            - 若 Reference 中含有财务数字、表格行或合计，应摘录与用户问题相关的具体数值与列名。
            - 回答简洁；涉及财务报表/表格数据时可适当延长，优先给出数字。

            用户问题：%s
            """;

    private GroundedPromptBuilder() {}

    public static String build(String question, java.util.List<Evidence> evidences) {
        String reference = evidences.stream()
                .map(evidence -> "<Reference page=\"" + evidence.page() + "\" score=\""
                        + String.format("%.3f", evidence.score()) + "\" block=\""
                        + (evidence.blockType() == null ? "TEXT" : evidence.blockType()) + "\">"
                        + evidence.promptText()
                        + "</Reference>")
                .collect(Collectors.joining("\n\n"));
        return String.format(KNOWLEDGE_USER_MESSAGE_TEMPLATE, reference, question);
    }
}
