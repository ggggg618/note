package com.note.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.admin.dto.AiStatsResponse;
import com.note.admin.dto.NoteAdminResponse;
import com.note.admin.dto.UserAdminResponse;
import com.note.admin.entity.AiLog;
import com.note.admin.mapper.AiLogMapper;
import com.note.common.BusinessException;
import com.note.common.PageResult;
import com.note.common.PageUtil;
import com.note.common.UserContext;
import com.note.interaction.entity.Comment;
import com.note.interaction.mapper.CommentMapper;
import com.note.note.entity.Note;
import com.note.note.mapper.NoteMapper;
import com.note.user.User;
import com.note.user.UserMapper;
import com.note.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    private final CommentMapper commentMapper;
    private final AiLogMapper aiLogMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public PageResult<UserAdminResponse> listUsers(long page, long size, String keyword) {
        checkAdmin();
        PageUtil.check(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + escapeLike(keyword.trim()) + "%";
            wrapper.and(query -> query.like(User::getUsername, like)
                    .or()
                    .like(User::getNickname, like));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<UserAdminResponse> records = userPage.getRecords().stream()
                .map(this::toUserAdminResponse)
                .toList();
        return new PageResult<>(userPage.getTotal(), userPage.getCurrent(), userPage.getSize(), records);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public void setUserStatus(Long userId, Integer status) {
        checkAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("用户状态只能为0或1");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
        log.info("管理员修改用户状态: userId={}, status={}", userId, status);
    }

    public PageResult<NoteAdminResponse> listNotes(long page, long size, Integer auditStatus) {
        checkAdmin();
        PageUtil.check(page, size);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getVisibility, 1);
        if (auditStatus != null) {
            wrapper.eq(Note::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(Note::getCreatedAt);

        Page<Note> notePage = noteMapper.selectPage(new Page<>(page, size), wrapper);
        List<NoteAdminResponse> records = notePage.getRecords().stream()
                .map(this::toNoteAdminResponse)
                .toList();
        return new PageResult<>(notePage.getTotal(), notePage.getCurrent(), notePage.getSize(), records);
    }

    public void auditNote(Long noteId, Integer auditStatus) {
        checkAdmin();
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new BusinessException("审核状态只能为1或2");
        }

        Note update = new Note();
        update.setId(noteId);
        update.setAuditStatus(auditStatus);
        if (noteMapper.updateById(update) == 0) {
            throw new BusinessException("笔记不存在");
        }
        log.info("管理员审核笔记: noteId={}, auditStatus={}", noteId, auditStatus);
    }

    public void offlineNote(Long noteId) {
        checkAdmin();
        Note update = new Note();
        update.setId(noteId);
        update.setVisibility(0);
        if (noteMapper.updateById(update) == 0) {
            throw new BusinessException("笔记不存在");
        }
        log.info("管理员下架笔记: noteId={}", noteId);
    }

    public void deleteComment(Long commentId) {
        checkAdmin();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getNoteId, comment.getNoteId()));
        Map<Long, List<Comment>> childrenByParentId = comments.stream()
                .filter(item -> item.getParentId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Comment::getParentId));

        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> deleteIds = new LinkedHashSet<>();
        queue.add(commentId);
        deleteIds.add(commentId);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            for (Comment child : childrenByParentId.getOrDefault(parentId, Collections.emptyList())) {
                if (deleteIds.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }
        for (Long deleteId : deleteIds) {
            Comment deleting = comments.stream()
                    .filter(c -> c.getId().equals(deleteId))
                    .findFirst()
                    .orElse(null);
            if (deleting != null) {
                parseMedia(deleting.getMedia()).forEach(fileStorageService::delete);
            }
            commentMapper.deleteById(deleteId);
        }
        log.info("管理员删除评论: commentId={}", commentId);
    }

    private List<String> parseMedia(String media) {
        if (media == null || media.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(media, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public AiStatsResponse aiStats() {
        checkAdmin();
        Long totalCalls = aiLogMapper.selectCount(null);
        Long successCalls = aiLogMapper.selectCount(
                new LambdaQueryWrapper<AiLog>()
                        .eq(AiLog::getSuccess, 1));
        Long failedCalls = aiLogMapper.selectCount(
                new LambdaQueryWrapper<AiLog>()
                        .eq(AiLog::getSuccess, 0));
        return new AiStatsResponse(totalCalls, successCalls, failedCalls);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private UserAdminResponse toUserAdminResponse(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }

    private NoteAdminResponse toNoteAdminResponse(Note note) {
        return new NoteAdminResponse(
                note.getId(),
                note.getUserId(),
                note.getTitle(),
                note.getBookName(),
                note.getContent(),
                note.getVisibility(),
                note.getAuditStatus(),
                note.getLikeCount(),
                note.getCreatedAt());
    }
}
