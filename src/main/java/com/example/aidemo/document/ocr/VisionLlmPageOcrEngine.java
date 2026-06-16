package com.example.aidemo.document.ocr;

import com.example.aidemo.config.AgentScopeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VisionLlmPageOcrEngine {

    private static final Logger log = LoggerFactory.getLogger(VisionLlmPageOcrEngine.class);

    private final AgentScopeProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public VisionLlmPageOcrEngine(AgentScopeProperties properties) {
        this.properties = properties;
    }

    public String transcribePage(BufferedImage image, int pageNumber) throws IOException, InterruptedException {
        String base64 = encodeImage(image);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.chat().model());
        root.put("stream", false);

        ArrayNode messages = root.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");

        ArrayNode content = user.putArray("content");
        content.addObject()
                .put("type", "text")
                .put("text", """
                        你是 OCR 助手。请逐字转录图片中的中文技术文档内容。
                        要求：
                        1. 保留条款编号（如 4.1、5.2.1）
                        2. 表格用 markdown 表格格式输出
                        3. 不要添加解释，只输出转录正文
                        """);
        content.addObject()
                .put("type", "image_url")
                .set("image_url", objectMapper.createObjectNode().put("url", "data:image/png;base64," + base64));

        String body = objectMapper.writeValueAsString(root);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.chat().baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(properties.chat().requestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.chat().apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Vision OCR failed: HTTP " + response.statusCode() + " " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String text = json.path("choices").path(0).path("message").path("content").asText("");
        log.info("OCR page {} done, chars={}", pageNumber, text.length());
        return text;
    }

    private String encodeImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
