package com.note.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.admin.entity.AiLog;
import com.note.admin.mapper.AiLogMapper;
import com.note.ai.dto.AiResponse;
import com.note.ai.dto.ChatMessageRequest;
import com.note.ai.dto.ChatMessageResponse;
import com.note.ai.dto.ConversationResponse;
import com.note.ai.dto.SaveAiRequest;
import com.note.ai.entity.Conversation;
import com.note.ai.entity.ConversationMessage;
import com.note.ai.entity.NoteAi;
import com.note.ai.mapper.ConversationMapper;
import com.note.ai.mapper.ConversationMessageMapper;
import com.note.ai.mapper.NoteAiMapper;
import com.note.common.BusinessException;
import com.note.note.entity.Note;
import com.note.note.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final NoteMapper noteMapper;
    private final NoteAiMapper noteAiMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final AiLogMapper aiLogMapper;
    private final AiClient aiClient;

    public AiResponse generateSummary(Long userId, Long noteId) {
        Note note = requireOwnedNote(userId, noteId);
        String systemPrompt = "你是一名专业的读书笔记助手。请根据用户提供的笔记内容，用简洁清晰的中文生成一段内容摘要。";
        String content = callAi(userId, "SUMMARY", systemPrompt, note.getContent());
        return new AiResponse("SUMMARY", content);
    }

    public AiResponse generateRecommend(Long userId, Long noteId) {
        Note note = requireOwnedNote(userId, noteId);
        String systemPrompt = "你是一名专业的阅读推荐助手。请根据用户提供的笔记内容，结合主题和阅读深度推荐相关书籍、延伸主题或学习方法，并用中文说明推荐理由。";
        String content = callAi(userId, "RECOMMEND", systemPrompt, note.getContent());
        return new AiResponse("RECOMMEND", content);
    }

    public AiResponse generateScore(Long userId, Long noteId) {
        Note note = requireOwnedNote(userId, noteId);
        String systemPrompt = "你是一名笔记质量评审助手。请从完整性、相关性、思考深度三个维度评估用户笔记，给出 0-100 分，并用简短的中文理由说明评分依据。";
        String content = callAi(userId, "SCORE", systemPrompt, note.getContent());
        return new AiResponse("SCORE", content);
    }

    @Transactional
    public AiResponse saveResult(Long userId, SaveAiRequest request) {
        requireOwnedNote(userId, request.getNoteId());
        NoteAi existing = noteAiMapper.selectOne(new LambdaQueryWrapper<NoteAi>()
                .eq(NoteAi::getNoteId, request.getNoteId())
                .eq(NoteAi::getType, request.getType()));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            existing = new NoteAi();
            existing.setNoteId(request.getNoteId());
            existing.setType(request.getType());
            existing.setContent(request.getContent());
            existing.setCreatedAt(now);
            existing.setUpdatedAt(now);
            noteAiMapper.insert(existing);
        } else {
            existing.setContent(request.getContent());
            existing.setUpdatedAt(now);
            noteAiMapper.updateById(existing);
        }

        return new AiResponse(existing.getType(), existing.getContent());
    }

    public NoteAi getResult(Long userId, Long noteId) {
        requireOwnedNote(userId, noteId);
        return noteAiMapper.selectOne(new LambdaQueryWrapper<NoteAi>()
                .eq(NoteAi::getNoteId, noteId)
                .eq(NoteAi::getType, "SUMMARY"));
    }

    public ConversationResponse createConversation(Long userId) {
        requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle("新会话");
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setDeleted(0);
        conversationMapper.insert(conversation);
        return toConversationResponse(conversation);
    }

    public List<ConversationResponse> listConversations(Long userId) {
        requireUser(userId);
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getCreatedAt)
                        .orderByDesc(Conversation::getId));
        return conversations.stream().map(this::toConversationResponse).toList();
    }

    public ChatMessageResponse chat(Long userId, Long conversationId, ChatMessageRequest request) {
        requireOwnedConversation(userId, conversationId);

        List<ConversationMessage> recentMessages = conversationMessageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversationId)
                        .orderByDesc(ConversationMessage::getCreatedAt)
                        .orderByDesc(ConversationMessage::getId)
                        .last("LIMIT 10"));
        Collections.reverse(recentMessages);

        StringBuilder context = new StringBuilder("历史消息：\n");
        if (recentMessages.isEmpty()) {
            context.append("暂无历史消息\n");
        } else {
            for (ConversationMessage message : recentMessages) {
                context.append(message.getRole()).append("：").append(truncate(message.getContent(), 500)).append("\n");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        insertMessage(conversationId, "user", request.getContent(), now);
        String userPrompt = context + "用户：" + request.getContent();
        String systemPrompt = "你是笔记工坊平台的智能客服。只回答与本平台功能、笔记管理、账号、AI 辅助阅读等平台使用相关的问题；遇到与平台无关的问题时，请礼貌拒绝并引导用户咨询平台相关问题。"
                + "平台实际功能路径如下：发布公开笔记请点击顶部“我的笔记”，再点“新建笔记”，填写后选择“公开”并保存，随后等待管理员审核；"
                + "查看公开笔记在顶部“公开笔记”；关注动态在“关注动态”；AI 总结在笔记详情或卡片上；AI 客服在“AI 客服”。请按这些真实入口回答，不要编造“创作中心、发布笔记”等不存在的入口。";

        String reply = callAi(userId, "CHAT", systemPrompt, userPrompt);
        ConversationMessage assistantMessage = insertMessage(conversationId, "assistant", reply, LocalDateTime.now());
        return new ChatMessageResponse(assistantMessage.getId(), "assistant", reply, assistantMessage.getCreatedAt());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public List<ChatMessageResponse> listMessages(Long userId, Long conversationId) {
        requireOwnedConversation(userId, conversationId);
        List<ConversationMessage> messages = conversationMessageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversationId)
                        .orderByAsc(ConversationMessage::getCreatedAt)
                        .orderByAsc(ConversationMessage::getId));
        return messages.stream()
                .map(message -> new ChatMessageResponse(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        requireOwnedConversation(userId, conversationId);
        conversationMapper.deleteById(conversationId);
        conversationMessageMapper.delete(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId));
    }

    private Note requireOwnedNote(Long userId, Long noteId) {
        requireUser(userId);
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!userId.equals(note.getUserId())) {
            throw new BusinessException(403, "无权限操作");
        }
        return note;
    }

    private Conversation requireOwnedConversation(Long userId, Long conversationId) {
        requireUser(userId);
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(403, "无权限操作");
        }
        return conversation;
    }

    private ConversationMessage insertMessage(Long conversationId, String role, String content, LocalDateTime createdAt) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(createdAt);
        conversationMessageMapper.insert(message);
        return message;
    }

    private String callAi(Long userId, String type, String systemPrompt, String userPrompt) {
        String prompt = systemPrompt + "\n" + userPrompt;
        try {
            String response = aiClient.chat(systemPrompt, userPrompt);
            recordAiLog(userId, type, prompt, response, 1);
            return response;
        } catch (BusinessException ex) {
            recordAiLog(userId, type, prompt, ex.getMessage(), 0);
            log.error("AI 调用失败: userId={}, type={}, message={}", userId, type, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            recordAiLog(userId, type, prompt, ex.getMessage(), 0);
            log.error("AI 调用异常: userId={}, type={}", userId, type, ex);
            throw new BusinessException("AI 调用失败");
        }
    }

    private void recordAiLog(Long userId, String type, String prompt, String response, Integer success) {
        AiLog aiLog = new AiLog();
        aiLog.setUserId(userId);
        aiLog.setType(type);
        aiLog.setPrompt(prompt);
        aiLog.setResponse(response);
        aiLog.setSuccess(success);
        aiLog.setCreatedAt(LocalDateTime.now());
        aiLogMapper.insert(aiLog);
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(conversation.getId(), conversation.getTitle(), conversation.getCreatedAt());
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }
}
