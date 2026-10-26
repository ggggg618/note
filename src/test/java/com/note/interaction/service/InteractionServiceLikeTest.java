package com.note.interaction.service;

import com.note.common.PageResult;
import com.note.interaction.dto.NoteResponse;
import com.note.interaction.entity.NoteLike;
import com.note.interaction.mapper.NoteLikeMapper;
import com.note.note.entity.Note;
import com.note.note.mapper.NoteMapper;
import com.note.user.User;
import com.note.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class InteractionServiceLikeTest {

    @Autowired
    private InteractionService interactionService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private NoteLikeMapper noteLikeMapper;

    @Test
    void filtersPrivateNotesFromLikes() {
        User user = new User();
        user.setUsername("like_test_user");
        user.setPassword("x");
        user.setNickname("x");
        user.setRole("USER");
        user.setStatus(1);
        user.setTokenVersion(0);
        userMapper.insert(user);

        Note note = new Note();
        note.setUserId(user.getId());
        note.setTitle("公开笔记");
        note.setContent("内容");
        note.setBookName("书");
        note.setVisibility(1);
        note.setAuditStatus(1);
        note.setLikeCount(1);
        noteMapper.insert(note);

        NoteLike like = new NoteLike();
        like.setUserId(user.getId());
        like.setNoteId(note.getId());
        like.setCreatedAt(LocalDateTime.now());
        noteLikeMapper.insert(like);

        PageResult<NoteResponse> before = interactionService.listMyLikes(user.getId(), 1, 10);
        assertEquals(1, before.getTotal());

        note.setVisibility(0);
        noteMapper.updateById(note);

        PageResult<NoteResponse> after = interactionService.listMyLikes(user.getId(), 1, 10);
        assertEquals(0, after.getTotal());
    }
}
