package com.note.util;

import com.note.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mov", "webm", "m4v");

    private final Path root;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory", e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        boolean image = IMAGE_EXTENSIONS.contains(extension);
        boolean video = VIDEO_EXTENSIONS.contains(extension);
        if (!image && !video) {
            throw new BusinessException("Unsupported file type");
        }
        long maxSize = video ? 50L * 1024 * 1024 : 10L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException("File is too large");
        }
        validateMagic(file, extension, video);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.copy(file.getInputStream(), root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            if (image) {
                compressImage(filename);
            }
        } catch (IOException e) {
            log.error("文件保存失败: {}", filename, e);
            throw new BusinessException("Failed to store file");
        }
        log.info("文件保存成功: {}", filename);
        return "/uploads/" + filename;
    }

    private void compressImage(String filename) {
        Path original = root.resolve(filename);
        Path temp = root.resolve(filename + ".thumb");
        try {
            Thumbnails.of(original.toFile())
                    .size(1600, 1600)
                    .keepAspectRatio(true)
                    .toFile(temp.toFile());
            Files.move(temp, original, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("图片压缩失败，保留原图: {}", filename, e);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
        }
    }

    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        try {
            Path file = root.resolve(url.substring("/uploads/".length())).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("文件删除失败: {}", url, e);
        }
    }

    private void validateMagic(MultipartFile file, String extension, boolean video) {
        try {
            byte[] head = file.getInputStream().readNBytes(12);
            boolean valid;
            if (video) {
                if ("webm".equals(extension)) {
                    valid = head.length >= 4
                            && (head[0] & 0xff) == 0x1A
                            && (head[1] & 0xff) == 0x45
                            && (head[2] & 0xff) == 0xDF
                            && (head[3] & 0xff) == 0xA3;
                } else {
                    valid = head.length >= 8 && "ftyp".equals(new String(head, 4, 4));
                }
            } else {
                valid = switch (extension) {
                    case "jpg", "jpeg" -> head.length >= 3 && (head[0] & 0xff) == 0xFF && (head[1] & 0xff) == 0xD8 && (head[2] & 0xff) == 0xFF;
                    case "png" -> head.length >= 8 && (head[0] & 0xff) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47;
                    case "gif" -> head.length >= 4 && head[0] == 0x47 && head[1] == 0x49 && head[2] == 0x46;
                    case "webp" -> head.length >= 12 && head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46
                            && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50;
                    default -> false;
                };
            }
            if (!valid) {
                throw new BusinessException("文件内容与格式不符");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
        }
    }
}
