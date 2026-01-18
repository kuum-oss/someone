package org.example.service;

import org.example.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;

public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    public void organizeBook(Book book, Path baseDir) throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.info("Organization interrupted before processing book: {}", book.getTitle());
            return;
        }
        Path root = baseDir.getFileName() != null &&
                baseDir.getFileName().toString().equalsIgnoreCase("collection")
                ? baseDir
                : baseDir.resolve("collection");

        Path target = root
                .resolve(safe(normalizeLanguage(book.getLanguage())))
                .resolve(safe(book.getGenre()));

        if (!"No Series".equalsIgnoreCase(book.getSeries())) {
            target = target.resolve(safe(book.getSeries()));
        }

        try {
            Files.createDirectories(target);

            if (Thread.currentThread().isInterrupted()) {
                LOGGER.info("Organization interrupted after creating directory: {}", target);
                return;
            }

            Path sourcePath = book.getFilePath();
            Path targetPath = target.resolve(sourcePath.getFileName());

            // Check free space
            long fileSize = Files.size(sourcePath);
            long freeSpace = Files.getFileStore(target.getRoot() != null ? target.getRoot() : baseDir).getUsableSpace();
            if (freeSpace < fileSize) {
                throw new IOException("Not enough free space on disk. Required: " + fileSize + ", Available: " + freeSpace);
            }

            // Используем copy + REPLACE_EXISTING, но можно рассмотреть move, если файлы на том же диске.
            // Для безопасности оставляем copy.
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Successfully copied book '{}' to '{}'", book.getTitle(), targetPath);

        } catch (IOException e) {
            LOGGER.error("Failed to organize book '{}': {}", book.getTitle(), e.getMessage(), e);
            throw e;
        }
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "Unknown"
                : s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) return "Unknown";
        String l = lang.toLowerCase().trim();
        if (l.contains("-")) {
            l = l.split("-")[0];
        } else if (l.contains("_")) {
            l = l.split("_")[0];
        }
        return l;
    }
}
