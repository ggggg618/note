package com.note.ai.service;

import com.note.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiClient {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private final ChatClient.Builder chatClientBuilder;

    public AiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank() || "sk-placeholder".equals(apiKey)) {
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
}
