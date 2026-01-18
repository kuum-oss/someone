package org.example.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.example.model.Book;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class MetadataService {

    private final AutoDetectParser parser = new AutoDetectParser();
    private final ExternalMetadataService external = new ExternalMetadataService();

    public Book extractMetadata(Path path) {

        Metadata md = new Metadata();

        try (InputStream in = Files.newInputStream(path)) {
            parser.parse(in, new BodyContentHandler(-1), md, new ParseContext());
        } catch (Exception ignored) {}

        String title = normalizeTitle(defaultIfBlank(md.get("dc:title"), stripExtension(path.getFileName().toString())));
        String author = defaultIfBlank(md.get("dc:creator"), "Unknown Author");
        String language = normalizeLanguage(defaultIfBlank(md.get("dc:language"), "Unknown"));
        String series = defaultIfBlank(md.get("fb2:series-name"), "No Series");
        String genre = defaultIfBlank(md.get("fb2:genre"), null);
        String year = md.get("dc:date");
        String description = defaultIfBlank(md.get("dc:description"), null);

        if (genre == null || year == null || description == null) {
            if (genre == null) {
                genre = external.fetchGenre(title, author).orElse("General");
            }
            if (year == null) {
                year = external.fetchYear(title, author).orElse("Unknown Year");
            }
            if (description == null) {
                description = external.fetchDescription(title, author).orElse("");
            }
        }

        byte[] cover = extractCoverLocally(path);
        if (cover == null) {
            cover = external.fetchCover(title, author).orElse(null);
        }
        byte[] authorPhoto = external.fetchAuthorPhoto(author).orElse(null);

        return Book.builder()
                .title(title)
                .author(author)
                .language(language)
                .series(series)
                .genre(genre)
                .year(year)
                .description(description)
                .filePath(path)
                .format(ext(path))
                .cover(cover)
                .authorPhoto(authorPhoto)
                .build();
    }

    private byte[] extractCoverLocally(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            Metadata md = new Metadata();
            parser.parse(in, new BodyContentHandler(-1), md, new ParseContext());
            return extractCoverFromTika(md);
        } catch (Exception ignored) {}
        return null;
    }

    private byte[] extractCoverFromTika(Metadata md) {
        // Some parsers (like FB2 or EPUB via Tika) might put cover in metadata
        // although usually it's more complex. For now, we check common fields.
        String[] coverFields = {"resource-name", "Content-Location", "thumbnail"};
        for (String field : coverFields) {
            String value = md.get(field);
            if (value != null && (value.toLowerCase().endsWith(".jpg") || value.toLowerCase().endsWith(".png"))) {
                // This is just a path, not the actual bytes. 
                // Tika usually doesn't give raw bytes for images in Metadata object easily 
                // without custom handlers.
            }
        }
        return null; 
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

    String normalizeTitle(String title) {
        if (title == null || title.isBlank()) return title;
        
        // Заменяем дефисы и подчеркивания на пробелы
        String normalized = title.replaceAll("[-_]", " ");
        
        // Разделяем по пробелам и делаем каждое слово с большой буквы
        String[] words = normalized.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? fileName : fileName.substring(0, lastDot);
    }

    private String defaultIfBlank(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private String ext(Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i == -1 ? "" : n.substring(i + 1);
    }
}
