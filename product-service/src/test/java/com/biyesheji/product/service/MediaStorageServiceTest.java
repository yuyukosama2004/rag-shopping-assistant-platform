package com.biyesheji.product.service;

import com.biyesheji.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaStorageServiceTest {
    @TempDir Path tempDir;

    @Test
    void storesVerifiedPngUsingGeneratedName() throws Exception {
        MediaStorageService storage = new MediaStorageService(tempDir.toString(), 1024);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        String url = storage.save(new MockMultipartFile("file", "phone.png", "image/png", output.toByteArray()));

        assertDoesNotThrow(() -> storage.load(url.substring(url.lastIndexOf('/') + 1)));
    }

    @Test
    void rejectsNonImageContent() {
        MediaStorageService storage = new MediaStorageService(tempDir.toString(), 1024);

        assertThrows(BizException.class, () -> storage.save(new MockMultipartFile("file", "bad.jpg", "image/jpeg", "not an image".getBytes())));
    }
}
