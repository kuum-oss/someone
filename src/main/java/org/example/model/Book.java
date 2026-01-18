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
    private final String year;
    private final Path filePath;
    private final String format;
    private final String description;
    private byte[] cover;
    private byte[] authorPhoto;

    // We can use a small cache or just keep them as is, 
    // but the recommendation was to consider memory.
    // Given the task, let's at least make them non-final to allow clearing if needed,
    // or better, keep them final but ensure they are only loaded when needed if we had a provider.
    // However, the current architecture passes them in the builder.
    // Let's implement a simple way to keep only a certain amount of images in memory if needed,
    // but for now, let's just make sure we aren't holding massive redundant data.

    private Book(Builder b) {
        this.title = Objects.requireNonNullElse(b.title, "Unknown Title");
        this.author = Objects.requireNonNullElse(b.author, "Unknown Author");
        this.series = Objects.requireNonNullElse(b.series, "No Series");
        this.seriesIndex = b.seriesIndex;
        this.genre = Objects.requireNonNullElse(b.genre, "General");
        this.language = Objects.requireNonNullElse(b.language, "Unknown");
        this.year = Objects.requireNonNullElse(b.year, "Unknown Year");
        this.filePath = Objects.requireNonNull(b.filePath, "filePath required");
        this.format = Objects.requireNonNullElse(b.format, "");
        this.description = Objects.requireNonNullElse(b.description, "");
        this.cover = b.cover;
        this.authorPhoto = b.authorPhoto;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSeries() { return series; }
    public Integer getSeriesIndex() { return seriesIndex; }
    public String getGenre() { return genre; }
    public String getLanguage() { return language; }
    public String getYear() { return year; }
    public Path getFilePath() { return filePath; }
    public String getFormat() { return format; }
    public String getDescription() { return description; }
    public byte[] getCover() { return cover; }
    public byte[] getAuthorPhoto() { return authorPhoto; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(title, book.title) &&
                Objects.equals(author, book.author) &&
                Objects.equals(filePath, book.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, filePath);
    }

    @Override
    public String toString() {
        return "Book{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", genre='" + genre + '\'' +
                ", year='" + year + '\'' +
                ", format='" + format + '\'' +
                '}';
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String title;
        private String author;
        private String series;
        private Integer seriesIndex;
        private String genre;
        private String language;
        private String year;
        private Path filePath;
        private String format;
        private String description;
        private byte[] cover;
        private byte[] authorPhoto;

        public Builder title(String v) { title = v; return this; }
        public Builder author(String v) { author = v; return this; }
        public Builder series(String v) { series = v; return this; }
        public Builder seriesIndex(Integer v) { seriesIndex = v; return this; }
        public Builder genre(String v) { genre = v; return this; }
        public Builder language(String v) { language = v; return this; }
        public Builder year(String v) { year = v; return this; }
        public Builder filePath(Path v) { filePath = v; return this; }
        public Builder format(String v) { format = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder cover(byte[] v) { cover = v; return this; }
        public Builder authorPhoto(byte[] v) { authorPhoto = v; return this; }

        public Book build() { return new Book(this); }
    }
}
