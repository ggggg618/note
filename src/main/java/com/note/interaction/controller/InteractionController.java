package com.note.interaction.controller;

import com.note.common.BusinessException;
import com.note.common.PageResult;
import com.note.common.Result;
import com.note.common.UserContext;
import com.note.interaction.dto.AuthorResponse;
import com.note.interaction.dto.AuthorStatsResponse;
import com.note.interaction.dto.CommentRequest;
import com.note.interaction.dto.CommentResponse;
import com.note.interaction.dto.NoteResponse;
import com.note.interaction.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/api/likes/{noteId}")
    public Result<Void> like(@PathVariable Long noteId) {
        interactionService.likeNote(requireUserId(), noteId);
        return Result.ok();
    }

    @DeleteMapping("/api/likes/{noteId}")
    public Result<Void> unlike(@PathVariable Long noteId) {
        interactionService.unlikeNote(requireUserId(), noteId);
        return Result.ok();
    }

    @GetMapping("/api/likes/me")
    public Result<PageResult<NoteResponse>> listMyLikes(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.listMyLikes(requireUserId(), page, size));
    }

    @GetMapping("/api/feeds")
    public Result<PageResult<NoteResponse>> feed(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.feed(requireUserId(), page, size));
    }

    @PostMapping("/api/follows/{userId}")
    public Result<Void> follow(@PathVariable Long userId) {
        interactionService.follow(requireUserId(), userId);
        return Result.ok();
    }

    @DeleteMapping("/api/follows/{userId}")
    public Result<Void> unfollow(@PathVariable Long userId) {
        interactionService.unfollow(requireUserId(), userId);
        return Result.ok();
    }

    @GetMapping("/api/follows/{userId}/followers")
    public Result<PageResult<AuthorResponse>> listFans(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.listFans(userId, page, size));
    }

    @GetMapping("/api/follows/rank")
    public Result<PageResult<AuthorResponse>> hotAuthors(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.hotAuthors(page, size));
    }

    @GetMapping("/api/follows/me")
    public Result<List<Long>> listFollowingIds() {
        return Result.ok(interactionService.listFollowingIds(requireUserId()));
    }

    @GetMapping("/api/users/{userId}/stats")
    public Result<AuthorStatsResponse> authorStats(@PathVariable Long userId) {
        return Result.ok(interactionService.authorStats(userId));
    }

    @PostMapping("/api/comments")
    public Result<CommentResponse> addComment(@Valid @RequestBody CommentRequest request) {
        return Result.ok(interactionService.addComment(requireUserId(), request));
    }

    @GetMapping("/api/comments/{noteId}")
    public Result<List<CommentResponse>> listComments(@PathVariable Long noteId) {
        return Result.ok(interactionService.listComments(noteId));
    }

    @DeleteMapping("/api/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        interactionService.deleteComment(requireUserId(), id);
        return Result.ok();
    }

    private Long requireUserId() {
        Long userId = UserContext.id();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
