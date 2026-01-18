package org.example.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.example.model.Book;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
        String language = defaultIfBlank(md.get("dc:language"), "Unknown");
        String series = defaultIfBlank(md.get("fb2:series-name"), "No Series");
        String genre = defaultIfBlank(md.get("fb2:genre"), null);

        if (genre == null) {
            genre = external.fetchGenre(title, author).orElse("General");
        }

        byte[] cover = external.fetchCover(title, author).orElse(null);

        return Book.builder()
                .title(title)
                .author(author)
                .language(language)
                .series(series)
                .genre(genre)
                .filePath(path)
                .format(ext(path))
                .cover(cover)
                .build();
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
