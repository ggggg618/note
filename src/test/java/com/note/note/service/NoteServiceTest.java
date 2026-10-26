package com.note.note.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.ai.mapper.NoteAiMapper;
import com.note.common.BusinessException;
import com.note.interaction.mapper.CommentMapper;
import com.note.interaction.mapper.NoteLikeMapper;
import com.note.note.dto.NoteRequest;
import com.note.note.entity.Note;
import com.note.note.mapper.NoteImageMapper;
import com.note.note.mapper.NoteMapper;
import com.note.util.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteMapper noteMapper;
    @Mock
    private NoteImageMapper noteImageMapper;
    @Mock
    private NoteAiMapper noteAiMapper;
    @Mock
    private NoteLikeMapper noteLikeMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NoteService noteService;

    @Test
    void rejectsInvalidVisibilityOnCreate() {
        NoteRequest request = request(2);

        assertThrows(BusinessException.class, () -> noteService.createNote(1L, request));
        verify(noteMapper, never()).insert(any(Note.class));
    }

    @Test
    void rejectsInvalidVisibilityOnUpdate() {
        Note note = new Note();
        note.setId(1L);
        note.setUserId(1L);
        note.setVisibility(0);
        when(noteMapper.selectById(1L)).thenReturn(note);

        assertThrows(BusinessException.class, () -> noteService.updateNote(1L, 1L, request(2)));
        verify(noteMapper, never()).update(any(), any());
    }

    private NoteRequest request(int visibility) {
        NoteRequest request = new NoteRequest();
        request.setTitle("标题");
        request.setContent("内容");
        request.setBookName("书名");
        request.setVisibility(visibility);
        return request;
    }
}
