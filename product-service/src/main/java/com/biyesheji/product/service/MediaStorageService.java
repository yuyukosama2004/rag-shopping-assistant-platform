package com.biyesheji.product.service;

import com.biyesheji.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class MediaStorageService {
    private final Path root;
    private final long maxBytes;

    public MediaStorageService(@Value("${media.storage-path:/data/media}") String storagePath,
                               @Value("${media.max-bytes:5242880}") long maxBytes) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建媒体存储目录", e);
        }
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes) {
            throw new BizException(400, "图片不能为空且不能超过5MiB");
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
        try {
            Files.write(root.resolve(filename), content);
        } catch (IOException e) {
            throw new IllegalStateException("图片保存失败", e);
        }
        return "/api/media/" + filename;
    }

    public Resource load(String filename) {
        if (!filename.matches("[0-9a-f-]{36}\\.(jpg|png)")) throw new BizException(404, "图片不存在");
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) throw new BizException(404, "图片不存在");
        return new FileSystemResource(file);
    }

    public void delete(String filename) {
        if (!filename.matches("[0-9a-f-]{36}\\.(jpg|png)")) throw new BizException(400, "图片标识无效");
        try {
            Files.deleteIfExists(root.resolve(filename).normalize());
        } catch (IOException e) {
            throw new IllegalStateException("图片删除失败", e);
        }
    }

    private String detectFormat(byte[] content) {
        if (content.length >= 3 && content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF) return "jpg";
        if (content.length >= 8 && content[0] == (byte) 0x89 && content[1] == 0x50 && content[2] == 0x4E && content[3] == 0x47) return "png";
        throw new BizException(400, "仅支持JPEG或PNG图片");
    }
}
