package org.example.service;

import org.example.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public void organizeBook(Book book, Path targetBaseDir) throws IOException {
        Path collectionDir = targetBaseDir;
        if (targetBaseDir.getFileName() == null || !targetBaseDir.getFileName().toString().equals("collection")) {
            collectionDir = targetBaseDir.resolve("collection");
        }
        
        // Construct target path: base/collection/Language/Genre/Series/Title.ext
        String cleanLanguage = sanitize(getLocalizedLanguage(book.getLanguage()));
        String cleanGenre = sanitize(getLocalizedGenre(book.getGenre()));
        String cleanSeries = sanitize(book.getSeries());
        String fileName = book.getFilePath().getFileName().toString();

        Path targetDir;
        if ("No Series".equalsIgnoreCase(book.getSeries()) || "Без серии".equalsIgnoreCase(book.getSeries())) {
            // Пропускаем папку серии, если её нет
            targetDir = collectionDir.resolve(cleanLanguage).resolve(cleanGenre);
        } else {
            targetDir = collectionDir.resolve(cleanLanguage).resolve(cleanGenre).resolve(cleanSeries);
        }
        
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(fileName);
        
        logger.info("Moving {} to {}", book.getFilePath(), targetPath);
        Files.copy(book.getFilePath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private String getLocalizedLanguage(String lang) {
        if (lang == null || "Unknown".equalsIgnoreCase(lang)) return "Unknown";
        String lower = lang.toLowerCase();
        if (lower.contains("ru") || lower.contains("rus")) return "Русский";
        if (lower.contains("en") || lower.contains("eng")) return "English";
        return lang;
    }

    private String getLocalizedGenre(String genre) {
        if (genre == null || "General".equalsIgnoreCase(genre) || "Общий".equalsIgnoreCase(genre)) return "General";
        return genre;
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "Unknown";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
