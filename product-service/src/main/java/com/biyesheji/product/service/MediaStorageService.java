package com.biyesheji.product.service;

import com.biyesheji.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

@Service
public class MediaStorageService {
    private final MediaObjectStore objectStore;
    private final long maxBytes;

    @Autowired
    public MediaStorageService(MediaObjectStore objectStore,
                               @Value("${media.max-bytes:5242880}") long maxBytes) {
        this.objectStore = objectStore;
        this.maxBytes = maxBytes;
    }

    MediaStorageService(String storagePath, long maxBytes) {
        this(new LocalMediaObjectStore(storagePath), maxBytes);
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes) {
            throw new BizException(400, "图片不能为空且不能超过配置的大小限制");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BizException(400, "图片读取失败");
        }
        String format;
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(content));
            if (image == null) throw new BizException(400, "仅支持JPEG或PNG图片");
            format = detectFormat(content);
        } catch (IOException e) {
            throw new BizException(400, "图片格式无效");
        }
        String filename = UUID.randomUUID() + "." + format;
        objectStore.put(filename, content, "png".equals(format) ? "image/png" : "image/jpeg");
        return "/api/media/" + filename;
    }

    public Resource load(String filename) {
        validateFilename(filename, 404);
        return objectStore.get(filename);
    }

    public void delete(String filename) {
        validateFilename(filename, 400);
        objectStore.delete(filename);
    }

    private void validateFilename(String filename, int code) {
        if (filename == null || !filename.matches("[0-9a-f-]{36}\\.(jpg|png)")) {
            throw new BizException(code, code == 404 ? "图片不存在" : "图片标识无效");
        }
    }

    private String detectFormat(byte[] content) {
        if (content.length >= 3 && content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF) return "jpg";
        if (content.length >= 8 && content[0] == (byte) 0x89 && content[1] == 0x50 && content[2] == 0x4E && content[3] == 0x47) return "png";
        throw new BizException(400, "仅支持JPEG或PNG图片");
    }
}
