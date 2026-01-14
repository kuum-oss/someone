package org.example.model;

import java.nio.file.Path;

public class Book {
    private String title;
    private String author;
    private String series;
    private Integer seriesIndex;
    private String genre;
    private String language;
    private Path filePath;
    private String format;
    private byte[] cover;

    public Book() {}

    public Book(String title, String author, String series, Integer seriesIndex, String genre, String language, Path filePath, String format, byte[] cover) {
        this.title = title;
        this.author = author;
        this.series = series;
        this.seriesIndex = seriesIndex;
        this.genre = genre;
        this.language = language;
        this.filePath = filePath;
        this.format = format;
        this.cover = cover;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public Integer getSeriesIndex() { return seriesIndex; }
    public void setSeriesIndex(Integer seriesIndex) { this.seriesIndex = seriesIndex; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Path getFilePath() { return filePath; }
    public void setFilePath(Path filePath) { this.filePath = filePath; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public byte[] getCover() { return cover; }
    public void setCover(byte[] cover) { this.cover = cover; }

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder title(String title) { this.title = title; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder series(String series) { this.series = series; return this; }
        public Builder seriesIndex(Integer seriesIndex) { this.seriesIndex = seriesIndex; return this; }
        public Builder genre(String genre) { this.genre = genre; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder filePath(Path filePath) { this.filePath = filePath; return this; }
        public Builder format(String format) { this.format = format; return this; }
        public Builder cover(byte[] cover) { this.cover = cover; return this; }

        public Book build() {
            return new Book(title, author, series, seriesIndex, genre, language, filePath, format, cover);
        }
    }
}
