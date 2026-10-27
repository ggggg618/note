package com.note.interaction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.BusinessException;
import com.note.common.CommentTreeUtil;
import com.note.common.PageResult;
import com.note.common.PageUtil;
import com.note.common.UserContext;
import com.note.interaction.dto.AuthorResponse;
import com.note.interaction.dto.AuthorStatsResponse;
import com.note.interaction.dto.CommentRequest;
import com.note.interaction.dto.CommentResponse;
import com.note.interaction.entity.Comment;
import com.note.interaction.entity.Follow;
import com.note.interaction.entity.NoteLike;
import com.note.interaction.mapper.CommentMapper;
import com.note.interaction.mapper.FollowMapper;
import com.note.interaction.mapper.NoteLikeMapper;
import com.note.note.entity.Note;
import com.note.note.mapper.NoteMapper;
import com.note.note.dto.NoteResponse;
import com.note.user.User;
import com.note.user.UserMapper;
import com.note.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final NoteLikeMapper noteLikeMapper;
    private final FollowMapper followMapper;
    private final CommentMapper commentMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public void likeNote(Long userId, Long noteId) {
        requireUser(userId);
        requirePublicNote(noteId);
        Long count = noteLikeMapper.selectCount(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getNoteId, noteId));
        if (count != null && count > 0) {
            throw new BusinessException("请勿重复点赞");
        }

        NoteLike like = new NoteLike();
        like.setUserId(userId);
        like.setNoteId(noteId);
        like.setCreatedAt(LocalDateTime.now());
        try {
            noteLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("请勿重复点赞");
        }

        noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .setSql("like_count = like_count + 1"));
        log.info("点赞成功: userId={}, noteId={}", userId, noteId);
    }

    @Transactional
    public void unlikeNote(Long userId, Long noteId) {
        requireUser(userId);
        int deleted = noteLikeMapper.delete(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getNoteId, noteId));
        if (deleted == 0) {
            throw new BusinessException("尚未点赞");
        }

        noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .setSql("like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END"));
        log.info("取消点赞: userId={}, noteId={}", userId, noteId);
    }

    public PageResult<NoteResponse> listMyLikes(Long userId, long page, long size) {
        requireUser(userId);
        PageUtil.check(page, size);
        long offset = (page - 1) * size;
        List<NoteResponse> records = noteLikeMapper.selectLikedNotePage(userId, offset, size);
        long total = noteLikeMapper.countLikedNotes(userId);
        return new PageResult<>(total, page, size, records);
    }

    public PageResult<NoteResponse> feed(Long userId, long page, long size) {
        requireUser(userId);
        PageUtil.check(page, size);

        List<Long> followeeIds = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId))
                .stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());
        if (followeeIds.isEmpty()) {
            return new PageResult<>(0, page, size, List.of());
        }

        Page<Note> notePage = noteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getVisibility, 1)
                        .eq(Note::getAuditStatus, 1)
                        .in(Note::getUserId, followeeIds)
                        .orderByDesc(Note::getCreatedAt)
                        .orderByDesc(Note::getId));

        List<NoteResponse> records = notePage.getRecords().stream()
                .map(note -> {
                    NoteResponse response = new NoteResponse();
                    response.setId(note.getId());
                    response.setUserId(note.getUserId());
                    response.setTitle(note.getTitle());
                    response.setBookName(note.getBookName());
                    response.setContent(note.getContent());
                    response.setVisibility(note.getVisibility());
                    response.setLikeCount(note.getLikeCount());
                    response.setCreatedAt(note.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());

        return new PageResult<>(notePage.getTotal(), notePage.getCurrent(), notePage.getSize(), records);
    }

    public void follow(Long userId, Long targetId) {
        requireUser(userId);
        if (userId.equals(targetId)) {
            throw new BusinessException("不能关注自己");
        }
        if (userMapper.selectById(targetId) == null) {
            throw new BusinessException("用户不存在");
        }
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .eq(Follow::getFolloweeId, targetId));
        if (count != null && count > 0) {
            throw new BusinessException("已关注该用户");
        }

        Follow follow = new Follow();
        follow.setFollowerId(userId);
        follow.setFolloweeId(targetId);
        follow.setCreatedAt(LocalDateTime.now());
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("已关注该用户");
        }
        log.info("关注成功: follower={}, followee={}", userId, targetId);
    }

    public void unfollow(Long userId, Long targetId) {
        requireUser(userId);
        int deleted = followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .eq(Follow::getFolloweeId, targetId));
        if (deleted == 0) {
            throw new BusinessException("尚未关注该用户");
        }
        log.info("取消关注: follower={}, followee={}", userId, targetId);
    }

    public List<Long> listFollowingIds(Long userId) {
        requireUser(userId);
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId))
                .stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());
    }

    public PageResult<AuthorResponse> listFans(Long userId, long page, long size) {
        requireUser(userId);
        PageUtil.check(page, size);
        Page<Follow> fanPage = followMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFolloweeId, userId)
                        .orderByDesc(Follow::getCreatedAt));

        List<Long> fanIds = fanPage.getRecords().stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
        Map<Long, User> users = authorsById(fanIds);
        Map<Long, Long> noteCounts = noteCountsByUser(fanIds);
        Map<Long, Long> followerCounts = followerCountsByUser(fanIds);
        List<AuthorResponse> records = fanPage.getRecords().stream()
                .map(follow -> {
                    User user = users.get(follow.getFollowerId());
                    if (user == null) {
                        return null;
                    }
                    return new AuthorResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                            safeCount(noteCounts.get(user.getId())), safeCount(followerCounts.get(user.getId())));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageResult<>(fanPage.getTotal(), fanPage.getCurrent(), fanPage.getSize(), records);
    }

    public PageResult<AuthorResponse> hotAuthors(long page, long size) {
        PageUtil.check(page, size);
        long offset = (page - 1) * size;
        List<Map<String, Object>> rows = followMapper.selectHotAuthorsPage(offset, size);
        Map<Long, Long> followerCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("followee_id");
            Object cntObj = row.get("cnt");
            if (idObj != null) {
                followerCounts.put(((Number) idObj).longValue(), cntObj == null ? 0L : ((Number) cntObj).longValue());
            }
        }

        long total = followMapper.countHotAuthors();
        List<Long> authorIds = List.copyOf(followerCounts.keySet());

        Map<Long, User> users = authorsById(authorIds);
        Map<Long, Long> noteCounts = noteCountsByUser(authorIds);
        List<AuthorResponse> records = authorIds.stream()
                .map(id -> {
                    User user = users.get(id);
                    if (user == null) {
                        return null;
                    }
                    return new AuthorResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                            safeCount(noteCounts.get(id)), safeCount(followerCounts.get(id)));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageResult<>(total, page, size, records);
    }

    public AuthorStatsResponse authorStats(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
        long noteCount = safeCount(noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(Note::getVisibility, 1)
                .eq(Note::getAuditStatus, 1)));
        long followerCount = safeCount(followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId)));
        long followingCount = safeCount(followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)));
        return new AuthorStatsResponse(noteCount, followerCount, followingCount);
    }

    public CommentResponse addComment(Long userId, CommentRequest request) {
        requireUser(userId);
        requirePublicNote(request.getNoteId());
        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent == null || !request.getNoteId().equals(parent.getNoteId())) {
                throw new BusinessException("父评论不存在");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setNoteId(request.getNoteId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setMedia(serializeMedia(request.getMedia()));
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setDeleted(0);
        commentMapper.insert(comment);
        log.info("发表评论: userId={}, noteId={}, commentId={}", userId, request.getNoteId(), comment.getId());
        return response(comment);
    }

    public PageResult<CommentResponse> listComments(Long noteId, long page, long size) {
        PageUtil.check(page, size);
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        boolean publicApproved = Integer.valueOf(1).equals(note.getVisibility())
                && Integer.valueOf(1).equals(note.getAuditStatus());
        Long currentUserId = UserContext.id();
        boolean owner = currentUserId != null && currentUserId.equals(note.getUserId());
        if (!publicApproved && !owner && !UserContext.isAdmin()) {
            throw new BusinessException(403, "无权限查看");
        }

        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, noteId)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId));

        Map<Long, List<Comment>> childrenByParentId = comments.stream()
                .filter(comment -> comment.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId, LinkedHashMap::new, Collectors.toList()));

        List<CommentResponse> topLevel = comments.stream()
                .filter(comment -> comment.getParentId() == null)
                .map(comment -> response(comment, childrenByParentId))
                .collect(Collectors.toList());
        long total = topLevel.size();
        int from = (int) ((page - 1) * size);
        if (from >= total) {
            return new PageResult<>(total, page, size, Collections.emptyList());
        }
        int to = (int) Math.min(from + size, total);
        return new PageResult<>(total, page, size, topLevel.subList(from, to));
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        requireUser(userId);
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!userId.equals(comment.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException("无权删除该评论");
        }

        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, comment.getNoteId()));
        Set<Long> deleteIds = CommentTreeUtil.collectSubtreeIds(commentId, comments);
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
        log.info("删除评论: userId={}, commentId={}", userId, commentId);
    }

    private Map<Long, User> authorsById(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private Map<Long, Long> noteCountsByUser(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return noteMapper.selectList(new LambdaQueryWrapper<Note>()
                        .in(Note::getUserId, userIds)
                        .eq(Note::getVisibility, 1)
                        .eq(Note::getAuditStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(Note::getUserId, Collectors.counting()));
    }

    private Map<Long, Long> followerCountsByUser(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .in(Follow::getFolloweeId, userIds))
                .stream()
                .collect(Collectors.groupingBy(Follow::getFolloweeId, Collectors.counting()));
    }

    private CommentResponse response(Comment comment) {
        return response(comment, Collections.emptyMap());
    }

    private CommentResponse response(Comment comment, Map<Long, List<Comment>> childrenByParentId) {
        List<CommentResponse> children = childrenByParentId
                .getOrDefault(comment.getId(), Collections.emptyList())
                .stream()
                .map(child -> response(child, childrenByParentId))
                .collect(Collectors.toList());
        return new CommentResponse(comment.getId(), comment.getNoteId(), comment.getUserId(),
                comment.getParentId(), comment.getContent(), parseMedia(comment.getMedia()),
                comment.getCreatedAt(), children);
    }

    private String serializeMedia(List<String> media) {
        if (media == null || media.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(media);
        } catch (Exception e) {
            throw new BusinessException("评论媒体格式错误");
        }
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

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private Note requirePublicNote(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!Integer.valueOf(1).equals(note.getVisibility())
                || !Integer.valueOf(1).equals(note.getAuditStatus())) {
            throw new BusinessException("该笔记未公开或未通过审核");
        }
        return note;
    }

    private long safeCount(Long count) {
        return count == null ? 0L : count;
    }

}
