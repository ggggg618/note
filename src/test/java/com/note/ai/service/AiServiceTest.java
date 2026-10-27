package com.note.ai.service;

import com.note.admin.mapper.AiLogMapper;
import com.note.admin.entity.AiLog;
import com.note.ai.dto.SaveAiRequest;
import com.note.ai.entity.NoteAi;
import com.note.ai.mapper.ConversationMapper;
import com.note.ai.mapper.ConversationMessageMapper;
import com.note.ai.mapper.NoteAiMapper;
import com.note.note.entity.Note;
import com.note.note.mapper.NoteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private NoteMapper noteMapper;
    @Mock
    private NoteAiMapper noteAiMapper;
    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private ConversationMessageMapper conversationMessageMapper;
    @Mock
    private AiLogMapper aiLogMapper;
    @Mock
    private AiClient aiClient;

    @InjectMocks
    private AiService aiService;

    @Test
    void replacesExistingAiResultForSameNote() {
        Note note = new Note();
        note.setId(1L);
        note.setUserId(1L);
        when(noteMapper.selectById(1L)).thenReturn(note);
        when(noteAiMapper.selectList(any())).thenReturn(List.of());

        aiService.saveResult(1L, saveRequest("SUMMARY", "总结"));
        verify(noteAiMapper, times(1)).insert(any(NoteAi.class));

        NoteAi existing = new NoteAi();
        existing.setId(10L);
        existing.setNoteId(1L);
        existing.setType("SUMMARY");
        existing.setContent("总结");
        when(noteAiMapper.selectList(any())).thenReturn(List.of(existing));

        aiService.saveResult(1L, saveRequest("RECOMMEND", "推荐"));

        verify(noteAiMapper, times(1)).updateById(existing);
        verify(noteAiMapper, times(1)).insert(any(NoteAi.class));
    }

    @Test
    void redactsPrivateNoteResponseInAiLog() {
        Note note = new Note();
        note.setId(1L);
        note.setUserId(1L);
        note.setVisibility(0);
        when(noteMapper.selectById(1L)).thenReturn(note);
        when(aiClient.chat(any(), any())).thenReturn("包含私有内容的总结");

        aiService.generateSummary(1L, 1L);

        ArgumentCaptor<AiLog> captor = ArgumentCaptor.forClass(AiLog.class);
        verify(aiLogMapper, atLeastOnce()).insert(captor.capture());
        assertEquals("[私有笔记内容已省略]", captor.getValue().getResponse());
    }

    @Test
    void allowsAiGenerationForAnotherUsersPublicNote() {
        Note note = new Note();
        note.setId(1L);
        note.setUserId(2L);
        note.setVisibility(1);
        note.setAuditStatus(1);
        when(noteMapper.selectById(1L)).thenReturn(note);
        when(aiClient.chat(any(), any())).thenReturn("公开笔记摘要");

        var response = aiService.generateSummary(1L, 1L);

        assertEquals("SUMMARY", response.getType());
        assertEquals("公开笔记摘要", response.getContent());
    }

    private SaveAiRequest saveRequest(String type, String content) {
        SaveAiRequest request = new SaveAiRequest();
        request.setNoteId(1L);
        request.setType(type);
        request.setContent(content);
        return request;
    }
}
