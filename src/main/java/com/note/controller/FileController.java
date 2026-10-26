package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.common.BusinessException;
import com.note.common.Result;
import com.note.common.UserContext;
import com.note.note.entity.Note;
import com.note.note.entity.NoteImage;
import com.note.note.mapper.NoteImageMapper;
import com.note.note.mapper.NoteMapper;
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

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final NoteImageMapper noteImageMapper;
    private final NoteMapper noteMapper;

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
        }
        byte[] data = fileStorageService.read(filename);
        return ResponseEntity.ok().contentType(contentType(filename)).body(data);
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
