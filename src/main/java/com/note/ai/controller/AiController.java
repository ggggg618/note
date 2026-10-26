package com.note.ai.controller;

import com.note.ai.dto.AiRequest;
import com.note.ai.dto.AiResponse;
import com.note.ai.dto.ChatMessageRequest;
import com.note.ai.dto.ChatMessageResponse;
import com.note.ai.dto.ConversationResponse;
import com.note.ai.dto.SaveAiRequest;
import com.note.ai.entity.NoteAi;
import com.note.ai.service.AiService;
import com.note.common.BusinessException;
import com.note.common.Result;
import com.note.common.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private static final Set<String> AI_TYPES = Set.of("SUMMARY", "RECOMMEND", "SCORE");

    private final AiService aiService;

    @PostMapping("/summary")
    public Result<AiResponse> summary(@Valid @RequestBody AiRequest request) {
        return Result.ok(aiService.generateSummary(UserContext.id(), request.getNoteId()));
    }

    @PostMapping("/recommend")
    public Result<AiResponse> recommend(@Valid @RequestBody AiRequest request) {
        return Result.ok(aiService.generateRecommend(UserContext.id(), request.getNoteId()));
    }

    @PostMapping("/score")
    public Result<AiResponse> score(@Valid @RequestBody AiRequest request) {
        return Result.ok(aiService.generateScore(UserContext.id(), request.getNoteId()));
    }

    @PostMapping("/result/save")
    public Result<AiResponse> saveResult(@Valid @RequestBody SaveAiRequest request) {
        return Result.ok(aiService.saveResult(UserContext.id(), request));
    }

    @GetMapping("/result/{noteId}")
    public Result<AiResponse> getResult(@PathVariable Long noteId,
                                        @RequestParam(defaultValue = "SUMMARY") String type) {
        if (!AI_TYPES.contains(type)) {
            throw new BusinessException("无效的AI结果类型");
        }
        NoteAi result = aiService.getResult(UserContext.id(), noteId, type);
        return Result.ok(result == null ? null : new AiResponse(result.getType(), result.getContent()));
    }

    @PostMapping("/conversations")
    public Result<ConversationResponse> createConversation() {
        return Result.ok(aiService.createConversation(UserContext.id()));
    }

    @GetMapping("/conversations")
    public Result<List<ConversationResponse>> listConversations() {
        return Result.ok(aiService.listConversations(UserContext.id()));
    }

    @PostMapping("/conversations/{id}/messages")
    public Result<ChatMessageResponse> chat(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request) {
        return Result.ok(aiService.chat(UserContext.id(), id, request));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<ChatMessageResponse>> listMessages(@PathVariable Long id) {
        return Result.ok(aiService.listMessages(UserContext.id(), id));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        aiService.deleteConversation(UserContext.id(), id);
        return Result.ok();
    }
}
