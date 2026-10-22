package com.note.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.admin.dto.AiStatsResponse;
import com.note.admin.dto.NoteAdminResponse;
import com.note.admin.dto.UserAdminResponse;
import com.note.admin.entity.AiLog;
import com.note.admin.mapper.AiLogMapper;
import com.note.common.BusinessException;
import com.note.common.PageResult;
import com.note.common.UserContext;
import com.note.interaction.mapper.CommentMapper;
import com.note.note.entity.Note;
import com.note.note.mapper.NoteMapper;
import com.note.user.User;
import com.note.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    private final CommentMapper commentMapper;
    private final AiLogMapper aiLogMapper;

    public PageResult<UserAdminResponse> listUsers(long page, long size, String keyword) {
        checkAdmin();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<UserAdminResponse> records = userPage.getRecords().stream()
                .map(this::toUserAdminResponse)
                .toList();
        return new PageResult<>(userPage.getTotal(), userPage.getCurrent(), userPage.getSize(), records);
    }

    public void setUserStatus(Long userId, Integer status) {
        checkAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("用户状态只能为0或1");
        }

        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    public PageResult<NoteAdminResponse> listNotes(long page, long size, Integer auditStatus) {
        checkAdmin();
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
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
    }

    public void offlineNote(Long noteId) {
        checkAdmin();
        Note update = new Note();
        update.setId(noteId);
        update.setVisibility(0);
        if (noteMapper.updateById(update) == 0) {
            throw new BusinessException("笔记不存在");
        }
    }

    public void deleteComment(Long commentId) {
        checkAdmin();
        if (commentMapper.deleteById(commentId) == 0) {
            throw new BusinessException("评论不存在");
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
                note.getVisibility(),
                note.getAuditStatus(),
                note.getLikeCount(),
                note.getCreatedAt());
    }
}
