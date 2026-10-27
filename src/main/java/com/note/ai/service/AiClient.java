package com.note.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiClient {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${ai.vision-model:Qwen/Qwen2.5-VL-7B-Instruct}")
    private String visionModel;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public AiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"sk-placeholder".equals(apiKey);
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return "AI 服务未配置，这是模拟结果：已根据你的内容生成参考。";
        }

        try {
            return chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            throw new BusinessException("AI 调用失败");
        }
    }

    public Map<String, String> recognizeBook(byte[] image, String mimeType) {
        if (!isConfigured()) {
            throw new BusinessException("AI 未配置，无法识别图片");
        }
        String dataUrl = "data:" + (mimeType == null ? "image/jpeg" : mimeType) + ";base64,"
                + Base64.getEncoder().encodeToString(image);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", visionModel);
        body.put("temperature", 0.2);
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)),
                        Map.of("type", "text", "text",
                                "请识别这张书籍图片中的信息，只返回一个 JSON 对象，不要多余文字：{\"title\":\"书名\",\"author\":\"作者\",\"description\":\"简介\"}。没有的信息用空字符串。")
                )
        )));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            String response = restTemplate.postForObject(buildChatUrl(), entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode json = objectMapper.readTree(content);
            Map<String, String> result = new LinkedHashMap<>();
            result.put("title", json.path("title").asText(""));
            result.put("author", json.path("author").asText(""));
            result.put("description", json.path("description").asText(""));
            return result;
        } catch (Exception e) {
            throw new BusinessException("图片识别失败");
        }
    }

    private String buildChatUrl() {
        String base = baseUrl == null ? "https://api.openai.com" : baseUrl;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/v1/chat/completions";
    }
}
