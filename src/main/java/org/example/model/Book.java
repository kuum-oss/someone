package org.example.model;

import java.nio.file.Path;
import java.util.Objects;

public class Book {

    private final String title;
    private final String author;
    private final String series;
    private final Integer seriesIndex;
    private final String genre;
    private final String language;
    private final Path filePath;
    private final String format;
    private final byte[] cover;

    private Book(Builder b) {
        this.title = Objects.requireNonNullElse(b.title, "Unknown Title");
        this.author = Objects.requireNonNullElse(b.author, "Unknown Author");
        this.series = Objects.requireNonNullElse(b.series, "No Series");
        this.seriesIndex = b.seriesIndex;
        this.genre = Objects.requireNonNullElse(b.genre, "General");
        this.language = Objects.requireNonNullElse(b.language, "Unknown");
        this.filePath = Objects.requireNonNull(b.filePath, "filePath required");
        this.format = Objects.requireNonNullElse(b.format, "");
        this.cover = b.cover;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSeries() { return series; }
    public Integer getSeriesIndex() { return seriesIndex; }
    public String getGenre() { return genre; }
    public String getLanguage() { return language; }
    public Path getFilePath() { return filePath; }
    public String getFormat() { return format; }
    public byte[] getCover() { return cover; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String title;
        private String author;
        private String series;
        private Integer seriesIndex;
        private String genre;
        private String language;
        private Path filePath;
        private String format;
        private byte[] cover;

        public Builder title(String v) { title = v; return this; }
        public Builder author(String v) { author = v; return this; }
        public Builder series(String v) { series = v; return this; }
        public Builder seriesIndex(Integer v) { seriesIndex = v; return this; }
        public Builder genre(String v) { genre = v; return this; }
        public Builder language(String v) { language = v; return this; }
        public Builder filePath(Path v) { filePath = v; return this; }
        public Builder format(String v) { format = v; return this; }
        public Builder cover(byte[] v) { cover = v; return this; }

        public Book build() { return new Book(this); }
    }
}
