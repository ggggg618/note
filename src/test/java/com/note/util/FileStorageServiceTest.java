package com.note.util;

import com.note.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsRealPng() throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] png = out.toByteArray();
        String url = service.store(new MockMultipartFile("file", "a.png", "image/png", png));
        assertNotNull(url);
        assertNotNull(service.read(url.substring("/uploads/".length())));
    }

    @Test
    void rejectsFakeImage() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile fake = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());
        assertThrows(BusinessException.class, () -> service.store(fake));
    }

    @Test
    void rejectsVideoAsAvatarImage() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        byte[] ftypVideo = new byte[]{0, 0, 0, 0, 'f', 't', 'y', 'p'};
        MockMultipartFile video = new MockMultipartFile("file", "avatar.mp4", "video/mp4", ftypVideo);
        assertThrows(BusinessException.class, () -> service.storeImage(video));
    }
}
