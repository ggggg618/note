package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.common.BusinessException;
import com.note.common.Result;
import com.note.common.UserContext;
import com.note.interaction.entity.Comment;
import com.note.interaction.mapper.CommentMapper;
import com.note.note.entity.Note;
import com.note.note.entity.NoteImage;
import com.note.note.mapper.NoteImageMapper;
import com.note.note.mapper.NoteMapper;
import com.note.user.User;
import com.note.user.UserMapper;
import com.note.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final NoteImageMapper noteImageMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;

    @PostMapping("/api/files")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.store(file));
    }

    @GetMapping("/api/images/{filename}")
    public ResponseEntity<byte[]> image(@PathVariable String filename) {
        String url = "/uploads/" + filename;
        NoteImage image = noteImageMapper.selectOne(new LambdaQueryWrapper<NoteImage>()
                .eq(NoteImage::getUrl, url));
        if (image != null) {
            Note note = noteMapper.selectById(image.getNoteId());
            if (note == null) {
                throw new BusinessException(404, "文件不存在");
            }
            boolean publicApproved = Integer.valueOf(1).equals(note.getVisibility())
                    && Integer.valueOf(1).equals(note.getAuditStatus());
            Long uid = UserContext.id();
            boolean owner = uid != null && uid.equals(note.getUserId());
            if (!publicApproved && !owner && !UserContext.isAdmin()) {
                throw new BusinessException(403, "无权限访问");
            }
        } else if (!isPublicAvatar(url) && !isPublicCommentMedia(filename) && UserContext.id() == null) {
            throw new BusinessException(401, "请先登录");
        }
        byte[] data = fileStorageService.read(filename);
        return ResponseEntity.ok().contentType(contentType(filename)).body(data);
    }

    private boolean isPublicAvatar(String url) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getAvatar, url));
        return count != null && count > 0;
    }

    private boolean isPublicCommentMedia(String filename) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .like(Comment::getMedia, filename));
        for (Comment comment : comments) {
            Note note = noteMapper.selectById(comment.getNoteId());
            if (note != null && Integer.valueOf(1).equals(note.getVisibility())
                    && Integer.valueOf(1).equals(note.getAuditStatus())) {
                return true;
            }
        }
        return false;
    }

    private MediaType contentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".mp4")) return MediaType.parseMediaType("video/mp4");
        if (lower.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
        if (lower.endsWith(".mov")) return MediaType.parseMediaType("video/quicktime");
        if (lower.endsWith(".m4v")) return MediaType.parseMediaType("video/mp4");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
