package com.note.note.controller;

import com.note.common.PageResult;
import com.note.common.Result;
import com.note.note.dto.NoteResponse;
import com.note.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserNoteController {

    private final NoteService noteService;

    @GetMapping("/api/users/{userId}/notes")
    public Result<PageResult<NoteResponse>> listUserPublicNotes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(noteService.listUserPublicNotes(userId, page, size));
    }
}
