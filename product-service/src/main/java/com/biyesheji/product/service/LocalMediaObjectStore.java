package com.biyesheji.product.service;

import com.biyesheji.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "media.storage-type", havingValue = "local", matchIfMissing = true)
class LocalMediaObjectStore implements MediaObjectStore {
    private final Path root;

    LocalMediaObjectStore(@Value("${media.storage-path:/data/media}") String storagePath) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建媒体存储目录", e);
        }
    }

    @Override
    public void put(String filename, byte[] content, String contentType) {
        try {
            Files.write(resolve(filename), content);
        } catch (IOException e) {
            throw new IllegalStateException("图片保存失败", e);
        }
    }

    @Override
    public Resource get(String filename) {
        Path file = resolve(filename);
        if (!Files.isRegularFile(file)) throw new BizException(404, "图片不存在");
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String filename) {
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("图片删除失败", e);
        }
    }

    private Path resolve(String filename) {
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root)) throw new BizException(400, "图片标识无效");
        return file;
    }
}
