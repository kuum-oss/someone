package org.example.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.example.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetadataService {
    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);
    private final AutoDetectParser parser = new AutoDetectParser();
    private final ExternalMetadataService externalMetadataService = new ExternalMetadataService();

    public Book extractMetadata(Path path) {
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(-1);
        ParseContext context = new ParseContext();

        try (InputStream stream = Files.newInputStream(path)) {
            parser.parse(stream, handler, metadata, context);
            
            String title = metadata.get("dc:title");
            if (title == null) title = path.getFileName().toString();
            
            String author = metadata.get("dc:creator");
            if (author == null) author = "Unknown Author";

            String language = metadata.get("dc:language");
            if (language == null) language = "Unknown";

            // Series and Genre are harder to get via standard Tika metadata for all formats
            // Some formats use specific keys.
            String series = metadata.get("fb2:series-name"); // Example for FB2 if Tika maps it
            String genre = metadata.get("fb2:genre");

            // EPUB specific (if available)
            if (series == null) series = metadata.get("series");
            
            // Если серия всё еще null, пробуем угадать из названия файла
            if (series == null || series.equalsIgnoreCase("No Series") || series.equalsIgnoreCase("Без серии")) {
                series = guessSeriesFromFileName(path.getFileName().toString());
            }

            // Проверка на "числовые" названия
            if (title.matches("\\d+")) {
                title = title + " (Проверьте название)";
            }

            // Если жанр не найден в метаданных, ищем в сети
            if (genre == null || genre.equalsIgnoreCase("General") || genre.equalsIgnoreCase("Общий")) {
                genre = externalMetadataService.fetchGenre(title, author).orElse("General");
            }

            // Ищем обложку (сначала в файле, потом в сети)
            byte[] cover = extractCoverFromFile(path);
            if (cover == null) {
                cover = externalMetadataService.fetchCover(title, author).orElse(null);
            }

            return Book.builder()
                    .title(title)
                    .author(author)
                    .language(language)
                    .series(series != null ? series : "No Series")
                    .genre(genre != null ? genre : "General")
                    .filePath(path)
                    .format(getFileExtension(path))
                    .cover(cover)
                    .build();

        } catch (Exception e) {
            logger.error("Error extracting metadata from {}", path, e);
            return Book.builder()
                    .title(path.getFileName().toString())
                    .author("Unknown")
                    .language("Unknown")
                    .series("No Series")
                    .genre("General")
                    .filePath(path)
                    .format(getFileExtension(path))
                    .build();
        }
    }

    private String guessSeriesFromFileName(String fileName) {
        // Убираем расширение
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // Паттерн: Название [число] или Название vol [число]
        // Например: Jujutsu Kaisen 01, Naruto vol. 5
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.*?)\\s*(?:vol\\.?\\s*)?\\d+.*$", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(nameWithoutExt);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private byte[] extractCoverFromFile(Path path) {
        String ext = getFileExtension(path);
        if ("epub".equalsIgnoreCase(ext)) {
            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(path.toFile())) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    String name = entry.getName().toLowerCase();
                    if (name.contains("cover") && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            return is.readAllBytes();
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to extract cover from EPUB: {}", path);
            }
        } else if ("fb2".equalsIgnoreCase(ext)) {
            try {
                String content = Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
                // Simple regex for base64 binary in FB2
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("<binary[^>]*id=\"cover.[^>]*\">([^<]+)</binary>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(content);
                if (matcher.find()) {
                    return java.util.Base64.getMimeDecoder().decode(matcher.group(1).trim());
                }
            } catch (Exception e) {
                logger.warn("Failed to extract cover from FB2: {}", path);
            }
        }
        return null;
    }

    private String getFileExtension(Path path) {
        String name = path.getFileName().toString();
        int lastIdx = name.lastIndexOf('.');
        return (lastIdx == -1) ? "" : name.substring(lastIdx + 1).toLowerCase();
    }
}
