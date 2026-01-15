package org.example.service;

import org.example.model.Book;

import java.io.IOException;
import java.nio.file.*;

public class FileService {

    public void organizeBook(Book book, Path baseDir) throws IOException {

        Path root = baseDir.getFileName() != null &&
                baseDir.getFileName().toString().equalsIgnoreCase("collection")
                ? baseDir
                : baseDir.resolve("collection");

        Path target = root
                .resolve(safe(book.getLanguage()))
                .resolve(safe(book.getGenre()));

        if (!"No Series".equalsIgnoreCase(book.getSeries())) {
            target = target.resolve(safe(book.getSeries()));
        }

        Files.createDirectories(target);

        Files.copy(
                book.getFilePath(),
                target.resolve(book.getFilePath().getFileName()),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "Unknown"
                : s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
