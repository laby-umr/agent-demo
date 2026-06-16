package com.example.aidemo.config;

/** 启动时校验 API Key，给出可操作的配置提示。 */
public final class ApiKeyStartupValidator {

    private ApiKeyStartupValidator() {}

    public static void requireKeys(AgentScopeProperties properties) {
        String chatKey = blankToNull(properties.chat().apiKey());
        String embeddingKey = blankToNull(properties.embedding().apiKey());
        if (chatKey != null && embeddingKey != null) {
            return;
        }
        throw new IllegalStateException("""
                未配置 API Key，应用无法启动。

                任选一种方式（密钥勿提交 Git）：

                1) 编辑 src/main/resources/application-local.yml，填入：
                   agentscope.chat.api-key
                   agentscope.embedding.api-key

                2) 设置环境变量后启动：
                   DEEPSEEK_API_KEY=sk-...
                   ZHIPU_API_KEY=...

                3) IDEA：Run Configuration → Environment variables 添加上述变量
                """);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
