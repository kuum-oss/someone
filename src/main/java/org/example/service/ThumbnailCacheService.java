package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

public class ThumbnailCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailCacheService.class);
    private final Path cacheDir;

    public ThumbnailCacheService() {
        this.cacheDir = Paths.get(System.getProperty("user.home"), ".someone", "cache", "covers");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create cache directory", e);
        }
    }

    public byte[] get(String key) {
        Path path = cacheDir.resolve(hash(key));
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                LOGGER.warn("Failed to read cached thumbnail for {}", key);
            }
        }
        return null;
    }

    public void put(String key, byte[] data) {
        Path path = cacheDir.resolve(hash(key));
        try {
            Files.write(path, data);
        } catch (IOException e) {
            LOGGER.warn("Failed to cache thumbnail for {}", key);
        }
    }

    private String hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(key.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return String.valueOf(key.hashCode());
        }
    }
}
