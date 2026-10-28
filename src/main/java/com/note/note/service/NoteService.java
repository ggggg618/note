package com.note.note.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.BusinessException;
import com.note.common.PageResult;
import com.note.common.PageUtil;
import com.note.common.UserContext;
import com.note.ai.entity.NoteAi;
import com.note.ai.mapper.NoteAiMapper;
import com.note.interaction.entity.Comment;
import com.note.interaction.entity.NoteLike;
import com.note.interaction.mapper.CommentMapper;
import com.note.interaction.mapper.NoteLikeMapper;
import com.note.note.dto.NoteRequest;
import com.note.note.dto.NoteResponse;
import com.note.note.entity.Note;
import com.note.note.entity.NoteImage;
import com.note.note.mapper.NoteImageMapper;
import com.note.note.mapper.NoteMapper;
import com.note.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteMapper noteMapper;
    private final NoteImageMapper noteImageMapper;
    private final NoteAiMapper noteAiMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final CommentMapper commentMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public NoteResponse createNote(Long userId, NoteRequest request) {
        requireUser(userId);
        int visibility = request.getVisibility() == null ? 0 : request.getVisibility();
        validateVisibility(visibility);
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setBookName(request.getBookName());
        note.setVisibility(visibility);
        note.setAuditStatus(0);
        note.setLikeCount(0);
        noteMapper.insert(note);
        log.info("创建笔记: userId={}, noteId={}", userId, note.getId());
        return buildResponse(getExistingNote(note.getId()));
    }

    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteRequest request) {
        requireUser(userId);
        Note note = getExistingNote(noteId);
        if (!note.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new BusinessException("无权限操作");
        }
        if (request.getVisibility() != null) {
            validateVisibility(request.getVisibility());
        }
        LambdaUpdateWrapper<Note> updateWrapper = new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .set(Note::getTitle, request.getTitle())
                .set(Note::getContent, request.getContent())
                .set(Note::getBookName, request.getBookName());
        boolean becomesPublic = request.getVisibility() != null
                ? Integer.valueOf(1).equals(request.getVisibility())
                : Integer.valueOf(1).equals(note.getVisibility());
        boolean contentChanged = !Objects.equals(request.getTitle(), note.getTitle())
                || !Objects.equals(request.getContent(), note.getContent())
                || !Objects.equals(request.getBookName(), note.getBookName());
        boolean visibilityChanged = request.getVisibility() != null
                && !Objects.equals(request.getVisibility(), note.getVisibility());
        if (request.getVisibility() != null) {
            updateWrapper.set(Note::getVisibility, request.getVisibility());
        }
        if (becomesPublic && (contentChanged || visibilityChanged
                || Integer.valueOf(2).equals(note.getAuditStatus()))) {
            updateWrapper.set(Note::getAuditStatus, 0);
        }
        noteMapper.update(null, updateWrapper);
        log.info("更新笔记: userId={}, noteId={}", userId, noteId);
        return buildResponse(getExistingNote(noteId));
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        requireUser(userId);
        Note note = getExistingNote(noteId);
        if (!note.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new BusinessException("无权限操作");
        }
        noteMapper.deleteById(noteId);
        List<NoteImage> images = noteImageMapper.selectList(new LambdaQueryWrapper<NoteImage>()
                .eq(NoteImage::getNoteId, noteId));
        images.forEach(image -> fileStorageService.delete(image.getUrl()));
        noteImageMapper.delete(new LambdaQueryWrapper<NoteImage>()
                .eq(NoteImage::getNoteId, noteId));
        noteLikeMapper.delete(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getNoteId, noteId));
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, noteId));
        comments.forEach(comment -> parseMedia(comment.getMedia()).forEach(fileStorageService::delete));
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, noteId));
        log.info("删除笔记: userId={}, noteId={}", userId, noteId);
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

    public PageResult<NoteResponse> listMyNotes(Long userId, long page, long size, String keyword) {
        requireUser(userId);
        PageUtil.check(page, size);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String keywordLike = "%" + escapeLike(keyword.trim()) + "%";
            wrapper.and(w -> w.like(Note::getTitle, keywordLike)
                    .or()
                    .like(Note::getContent, keywordLike)
                    .or()
                    .like(Note::getBookName, keywordLike));
        }
        Page<Note> notePage = noteMapper.selectPage(
                new Page<>(page, size),
                wrapper
        );
        return toPageResult(notePage, page, size);
    }

    public PageResult<NoteResponse> listPublicNotes(long page, long size, String keyword) {
        PageUtil.check(page, size);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getVisibility, 1)
                .eq(Note::getAuditStatus, 1)
                .orderByDesc(Note::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String keywordLike = "%" + escapeLike(keyword.trim()) + "%";
            wrapper.and(w -> w.like(Note::getTitle, keywordLike)
                    .or()
                    .like(Note::getContent, keywordLike)
                    .or()
                    .like(Note::getBookName, keywordLike));
        }
        Page<Note> notePage = noteMapper.selectPage(new Page<>(page, size), wrapper);
        return toPageResult(notePage, page, size);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public PageResult<NoteResponse> listRankedNotes(long page, long size) {
        PageUtil.check(page, size);
        Page<Note> notePage = noteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getVisibility, 1)
                        .eq(Note::getAuditStatus, 1)
                        .orderByDesc(Note::getLikeCount)
                        .orderByDesc(Note::getCreatedAt)
        );
        return toPageResult(notePage, page, size);
    }

    public PageResult<NoteResponse> listUserPublicNotes(Long userId, long page, long size) {
        PageUtil.check(page, size);
        Page<Note> notePage = noteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getUserId, userId)
                        .eq(Note::getVisibility, 1)
                        .eq(Note::getAuditStatus, 1)
                        .orderByDesc(Note::getCreatedAt)
        );
        return toPageResult(notePage, page, size);
    }

    public NoteResponse getNoteDetail(Long currentUserId, Long noteId) {
        Note note = getExistingNote(noteId);
        boolean owner = currentUserId != null && note.getUserId().equals(currentUserId);
        boolean publicAndApproved = Integer.valueOf(1).equals(note.getVisibility())
                && Integer.valueOf(1).equals(note.getAuditStatus());
        if (!owner && !UserContext.isAdmin() && !publicAndApproved) {
            throw new BusinessException(403, "无权限查看");
        }
        return buildResponse(note);
    }

    @Transactional
    public List<String> uploadImages(Long userId, Long noteId, MultipartFile[] files) {
        requireUser(userId);
        Note note = getExistingNote(noteId);
        if (!note.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new BusinessException("无权限操作");
        }
        List<String> urls = new ArrayList<>();
        if (files == null || files.length == 0) {
            return urls;
        }
        for (int i = 0; i < files.length; i++) {
            String url = fileStorageService.storeImage(files[i]);
            NoteImage image = new NoteImage();
            image.setNoteId(noteId);
            image.setUrl(url);
            image.setSortOrder(i);
            noteImageMapper.insert(image);
            urls.add(url);
        }
        log.info("上传笔记图片: userId={}, noteId={}, count={}", userId, noteId, urls.size());
        return urls;
    }

    @Transactional
    public void deleteImage(Long userId, Long imageId) {
        requireUser(userId);
        NoteImage image = noteImageMapper.selectById(imageId);
        if (image == null) {
            throw new BusinessException("图片不存在");
        }
        Note note = getExistingNote(image.getNoteId());
        if (!note.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new BusinessException("无权限操作");
        }
        fileStorageService.delete(image.getUrl());
        noteImageMapper.deleteById(imageId);
    }

    @Transactional
    public void deleteImageByUrl(Long userId, Long noteId, String url) {
        requireUser(userId);
        Note note = getExistingNote(noteId);
        if (!note.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new BusinessException("无权限操作");
        }
        NoteImage image = noteImageMapper.selectOne(new LambdaQueryWrapper<NoteImage>()
                .eq(NoteImage::getNoteId, noteId)
                .eq(NoteImage::getUrl, url));
        if (image == null) {
            throw new BusinessException("图片不存在");
        }
        fileStorageService.delete(image.getUrl());
        noteImageMapper.deleteById(image.getId());
    }

    private NoteResponse buildResponse(Note note) {
        List<String> images = listImages(note.getId());
        List<NoteAi> aiResults = noteAiMapper.selectList(new LambdaQueryWrapper<NoteAi>()
                .eq(NoteAi::getNoteId, note.getId()));
        NoteAi ai = aiResults.stream().findFirst().orElse(null);
        String summary = null;
        String recommend = null;
        String score = null;
        String aiResult = null;
        if (ai != null) {
            if ("MIXED".equals(ai.getType())) {
                try {
                    JsonNode node = objectMapper.readTree(ai.getContent());
                    summary = node.path("summary").asText(null);
                    recommend = node.path("recommend").asText(null);
                    score = node.path("score").asText(null);
                } catch (Exception ignored) {
                }
                aiResult = ai.getContent();
            } else if ("SUMMARY".equals(ai.getType())) {
                summary = ai.getContent();
                aiResult = summary;
            } else if ("RECOMMEND".equals(ai.getType())) {
                recommend = ai.getContent();
            } else if ("SCORE".equals(ai.getType())) {
                score = ai.getContent();
            }
        }
        NoteResponse response = buildResponse(note, images, summary);
        response.setAiResult(aiResult);
        response.setAiSummary(summary);
        response.setAiRecommend(recommend);
        response.setAiScore(score);
        return response;
    }

    private NoteResponse buildResponse(Note note, List<String> images, String aiResult) {
        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setUserId(note.getUserId());
        response.setTitle(note.getTitle());
        response.setContent(note.getContent());
        response.setBookName(note.getBookName());
        response.setVisibility(note.getVisibility());
        response.setAuditStatus(note.getAuditStatus());
        response.setLikeCount(note.getLikeCount());
        response.setCreatedAt(note.getCreatedAt());
        response.setImages(images);
        response.setAiResult(aiResult);
        return response;
    }

    private List<String> listImages(Long noteId) {
        return noteImageMapper.selectList(
                        new LambdaQueryWrapper<NoteImage>()
                                .eq(NoteImage::getNoteId, noteId)
                                .orderByAsc(NoteImage::getSortOrder)
                                .orderByAsc(NoteImage::getId)
                )
                .stream()
                .map(NoteImage::getUrl)
                .toList();
    }

    private PageResult<NoteResponse> toPageResult(Page<Note> notePage, long page, long size) {
        List<Note> notes = notePage.getRecords();
        List<Long> noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<String>> imagesByNote = imagesByNote(noteIds);
        Map<Long, String> aiByNote = aiByNote(noteIds);
        List<NoteResponse> records = notes.stream()
                .map(note -> buildResponse(note,
                        imagesByNote.getOrDefault(note.getId(), Collections.emptyList()),
                        aiByNote.get(note.getId())))
                .toList();
        return new PageResult<>(notePage.getTotal(), page, size, records);
    }

    private Map<Long, List<String>> imagesByNote(List<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return noteImageMapper.selectList(new LambdaQueryWrapper<NoteImage>()
                        .in(NoteImage::getNoteId, noteIds)
                        .orderByAsc(NoteImage::getSortOrder)
                        .orderByAsc(NoteImage::getId))
                .stream()
                .collect(Collectors.groupingBy(NoteImage::getNoteId, LinkedHashMap::new,
                        Collectors.mapping(NoteImage::getUrl, Collectors.toList())));
    }

    private Map<Long, String> aiByNote(List<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return noteAiMapper.selectList(new LambdaQueryWrapper<NoteAi>()
                        .in(NoteAi::getNoteId, noteIds)
                        .orderByDesc(NoteAi::getUpdatedAt)
                        .orderByDesc(NoteAi::getId))
                .stream()
                .collect(Collectors.toMap(NoteAi::getNoteId, NoteAi::getContent, (first, second) -> first));
    }

    private Note getExistingNote(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        return note;
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("未登录");
        }
    }

    private void validateVisibility(int visibility) {
        if (visibility != 0 && visibility != 1) {
            throw new BusinessException("可见性只能为0或1");
        }
    }
}
