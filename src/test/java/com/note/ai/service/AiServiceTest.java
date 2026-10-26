package com.note.ai.service;

import com.note.admin.mapper.AiLogMapper;
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

import static org.mockito.ArgumentMatchers.any;
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
    void savesEachAiTypeIndependently() {
        Note note = new Note();
        note.setId(1L);
        note.setUserId(1L);
        when(noteMapper.selectById(1L)).thenReturn(note);
        when(noteAiMapper.selectOne(any())).thenReturn(null);

        aiService.saveResult(1L, saveRequest("SUMMARY", "总结"));
        aiService.saveResult(1L, saveRequest("RECOMMEND", "推荐"));

        verify(noteAiMapper, times(2)).insert(any(NoteAi.class));
    }

    private SaveAiRequest saveRequest(String type, String content) {
        SaveAiRequest request = new SaveAiRequest();
        request.setNoteId(1L);
        request.setType(type);
        request.setContent(content);
        return request;
    }
}
