package com.example.aidemo.document.parse;

import com.example.aidemo.config.AiDocumentParseProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** MinerU / parse-adapter HTTP 客户端，协议对齐 laby-admin。 */
@Component
public class MineruDocumentParseClient {

    private static final Logger log = LoggerFactory.getLogger(MineruDocumentParseClient.class);

    private final AiDocumentParseProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public MineruDocumentParseClient(AiDocumentParseProperties properties) {
        this.properties = properties;
    }

    public boolean isAvailable() {
        AiDocumentParseProperties.EngineConfig mineru = properties.mineru();
        return properties.enabled() && mineru.enabled() && mineru.baseUrl() != null && !mineru.baseUrl().isBlank();
    }

    public StructuredParseResult parse(byte[] bytes, String fileName) throws IOException, InterruptedException {
        AiDocumentParseProperties.EngineConfig mineru = properties.mineru();
        String url = mineru.baseUrl().replaceAll("/$", "") + mineru.parsePath();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("fileName", fileName);
        body.put("contentBase64", Base64.getEncoder().encodeToString(bytes));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(mineru.timeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("MinerU HTTP " + response.statusCode() + ": " + response.body());
        }
        return parseResponse(response.body());
    }

    StructuredParseResult parseResponse(String jsonBody) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        String markdown = root.path("markdown").asText("");
        String quality = root.path("quality").asText("HIGH");
        List<StructuredElement> elements = new ArrayList<>();
        JsonNode elementsNode = root.path("elements");
        if (elementsNode.isArray()) {
            for (JsonNode item : elementsNode) {
                elements.add(new StructuredElement(
                        item.path("type").asText("paragraph"),
                        item.path("text").asText(""),
                        item.path("markdown").asText(""),
                        item.path("caption").asText(null),
                        item.path("description").asText(null),
                        item.path("page").isNumber() ? item.path("page").asInt() : null,
                        item.path("level").isNumber() ? item.path("level").asInt() : null));
            }
        }
        log.info("MinerU parsed markdownChars={}, elements={}", markdown.length(), elements.size());
        return new StructuredParseResult(markdown, "MINERU", quality, false, elements);
    }
}
