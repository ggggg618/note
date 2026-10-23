package com.note.note.service;

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

    @Transactional
    public NoteResponse createNote(Long userId, NoteRequest request) {
        requireUser(userId);
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setBookName(request.getBookName());
        note.setVisibility(request.getVisibility() == null ? 0 : request.getVisibility());
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
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作");
        }
        LambdaUpdateWrapper<Note> updateWrapper = new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .set(Note::getTitle, request.getTitle())
                .set(Note::getContent, request.getContent())
                .set(Note::getBookName, request.getBookName());
        boolean becomesPublic = request.getVisibility() != null
                ? request.getVisibility() == 1
                : Integer.valueOf(1).equals(note.getVisibility());
        boolean contentChanged = !Objects.equals(request.getTitle(), note.getTitle())
                || !Objects.equals(request.getContent(), note.getContent())
                || !Objects.equals(request.getBookName(), note.getBookName());
        boolean visibilityChanged = request.getVisibility() != null
                && !Objects.equals(request.getVisibility(), note.getVisibility());
        if (request.getVisibility() != null) {
            updateWrapper.set(Note::getVisibility, request.getVisibility());
        }
        if (becomesPublic && (contentChanged || visibilityChanged)) {
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
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, noteId));
        log.info("删除笔记: userId={}, noteId={}", userId, noteId);
    }

    public PageResult<NoteResponse> listMyNotes(Long userId, long page, long size) {
        requireUser(userId);
        PageUtil.check(page, size);
        Page<Note> notePage = noteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getUserId, userId)
                        .orderByDesc(Note::getCreatedAt)
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
            String keywordLike = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(Note::getTitle, keywordLike)
                    .or()
                    .like(Note::getContent, keywordLike));
        }
        Page<Note> notePage = noteMapper.selectPage(new Page<>(page, size), wrapper);
        return toPageResult(notePage, page, size);
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
            String url = fileStorageService.store(files[i]);
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

    private NoteResponse buildResponse(Note note) {
        List<String> images = listImages(note.getId());
        String aiResult = null;
        NoteAi noteAi = noteAiMapper.selectOne(new LambdaQueryWrapper<NoteAi>()
                .eq(NoteAi::getNoteId, note.getId()));
        if (noteAi != null) {
            aiResult = noteAi.getContent();
        }
        return buildResponse(note, images, aiResult);
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
                        .in(NoteAi::getNoteId, noteIds))
                .stream()
                .collect(Collectors.toMap(NoteAi::getNoteId, NoteAi::getContent));
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
}
