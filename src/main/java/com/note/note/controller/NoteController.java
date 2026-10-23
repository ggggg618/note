package com.note.note.controller;

import com.note.common.PageResult;
import com.note.common.Result;
import com.note.common.UserContext;
import com.note.note.dto.NoteRequest;
import com.note.note.dto.NoteResponse;
import com.note.note.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public Result<NoteResponse> createNote(@Valid @RequestBody NoteRequest request) {
        return Result.ok(noteService.createNote(UserContext.id(), request));
    }

    @GetMapping("/my")
    public Result<PageResult<NoteResponse>> listMyNotes(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(noteService.listMyNotes(UserContext.id(), page, size));
    }

    @GetMapping
    public Result<PageResult<NoteResponse>> listPublicNotes(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(noteService.listPublicNotes(page, size, keyword));
    }

    @GetMapping("/rank")
    public Result<PageResult<NoteResponse>> listRankedNotes(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(noteService.listRankedNotes(page, size));
    }

    @GetMapping("/{id}")
    public Result<NoteResponse> getNoteDetail(@PathVariable Long id) {
        return Result.ok(noteService.getNoteDetail(UserContext.id(), id));
    }

    @PutMapping("/{id}")
    public Result<NoteResponse> updateNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return Result.ok(noteService.updateNote(UserContext.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(UserContext.id(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/images")
    public Result<List<String>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) {
        return Result.ok(noteService.uploadImages(UserContext.id(), id, files));
    }
}
