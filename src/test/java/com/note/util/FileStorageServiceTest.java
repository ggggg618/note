package com.note.util;

import com.note.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsRealPng() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        String url = service.store(new MockMultipartFile("file", "a.png", "image/png", png));
        assertNotNull(url);
    }

    @Test
    void rejectsFakeImage() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile fake = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());
        assertThrows(BusinessException.class, () -> service.store(fake));
    }
}
