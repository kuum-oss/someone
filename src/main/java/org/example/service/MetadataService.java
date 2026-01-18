package org.example.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.example.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MetadataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataService.class);
    private final Parser parser;
    private final ExternalMetadataService external;

    public MetadataService() {
        this(new AutoDetectParser(), new ExternalMetadataService());
    }

    public MetadataService(Parser parser, ExternalMetadataService external) {
        this.parser = parser;
        this.external = external;
    }

    public Book extractMetadata(Path path) {
        Metadata md = new Metadata();

        try (InputStream in = Files.newInputStream(path)) {
            parser.parse(in, new BodyContentHandler(-1), md, new ParseContext());
        } catch (Exception e) {
            LOGGER.error("Error parsing metadata for file: {}", path, e);
        }

        String title = normalizeTitle(defaultIfBlank(md.get("dc:title"), stripExtension(path.getFileName().toString())));
        String author = defaultIfBlank(md.get("dc:creator"), "Unknown Author");
        String language = normalizeLanguage(defaultIfBlank(md.get("dc:language"), "Unknown"));
        String series = defaultIfBlank(md.get("fb2:series-name"), "No Series");
        String genre = defaultIfBlank(md.get("fb2:genre"), null);
        String year = md.get("dc:date");
        if (year == null) {
            year = md.get("fb2:date");
        }
        String description = defaultIfBlank(md.get("dc:description"), null);
        if (description == null) {
            description = md.get("fb2:annotation");
        }

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

        byte[] cover = extractCoverFromTika(md);
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

    private byte[] extractCoverFromTika(Metadata md) {
        String base64 = md.get("fb2:cover");
        if (base64 != null && !base64.isBlank()) {
            try {
                return java.util.Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
            } catch (Exception e) {
                LOGGER.warn("Failed to decode base64 cover", e);
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
        
        // Remove multiple dots or underscores
        String normalized = title.replaceAll("[-_.]", " ");
        
        // Split by whitespace
        String[] words = normalized.split("\\s+");
        StringBuilder sb = new StringBuilder();
        
        // Small words that should usually be lowercase
        java.util.Set<String> smallWords = java.util.Set.of("a", "an", "the", "and", "or", "in", "on", "at", "to", "for", "of", "with", "и", "или", "в", "на", "с");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (!word.isEmpty()) {
                if (i > 0 && smallWords.contains(word) && i < words.length - 1) {
                    sb.append(word);
                } else {
                    sb.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1));
                }
                sb.append(" ");
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
