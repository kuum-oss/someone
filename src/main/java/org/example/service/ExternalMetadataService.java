package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExternalMetadataService {

    private static final Logger LOGGER = Logger.getLogger(ExternalMetadataService.class.getName());
    private static final String API = "https://www.googleapis.com/books/v1/volumes?q=";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<String> fetchGenre(String title, String author) {
        return fetchInfo(title, author)
                .map(v -> v.path("categories").isArray()
                        ? v.path("categories").get(0).asText()
                        : null);
    }

    public Optional<String> fetchDescription(String title, String author) {
        return fetchInfo(title, author)
                .map(v -> v.path("description").asText(null));
    }

    public Optional<String> fetchYear(String title, String author) {
        return fetchInfo(title, author)
                .map(v -> v.path("publishedDate").asText(null))
                .map(d -> d != null && d.length() >= 4 ? d.substring(0, 4) : null);
    }

    public Optional<byte[]> fetchAuthorPhoto(String author) {
        if (author == null || author.isBlank() || author.equalsIgnoreCase("Unknown Author")) {
            return Optional.empty();
        }
        try {
            // Пытаемся найти именно автора через Open Library или аналогичный сервис, 
            // либо через поиск в Google Books с фокусом на автора.
            String q = URLEncoder.encode(author, StandardCharsets.UTF_8);
            
            // 1. Пробуем Open Library (там часто есть фото авторов по имени)
            HttpRequest olReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://openlibrary.org/search/authors.json?q=" + q))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> olRes = client.send(olReq, HttpResponse.BodyHandlers.ofString());
            if (olRes.statusCode() == 200) {
                JsonNode docs = mapper.readTree(olRes.body()).path("docs");
                if (docs.isArray() && !docs.isEmpty()) {
                    String key = docs.get(0).path("key").asText();
                    if (key != null && !key.isEmpty()) {
                        // Формат ссылки на фото в Open Library: https://covers.openlibrary.org/a/olid/ID-M.jpg
                        String photoUrl = "https://covers.openlibrary.org/a/olid/" + key + "-M.jpg";
                        Optional<byte[]> photo = downloadImage(photoUrl);
                        if (photo.isPresent() && photo.get().length > 1000) { // Проверка, что это не заглушка (маленький файл)
                            return photo;
                        }
                    }
                }
            }

            // 2. Если не вышло, пробуем Google Books (хотя там редко именно фото автора, чаще обложки)
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/books/v1/volumes?q=inauthor:" + q + "&maxResults=1"))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();

            HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) {
                JsonNode items = mapper.readTree(r.body()).path("items");
                if (items.isArray() && !items.isEmpty()) {
                    // Мы не берем thumbnail из volumeInfo, так как это обложка книги.
                    // К сожалению, Google Books API редко предоставляет прямую ссылку на фото автора.
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching author photo for: " + author, e);
        }
        return Optional.empty();
    }

    public Optional<byte[]> fetchCover(String title, String author) {
        if (title == null || title.isBlank() || title.equalsIgnoreCase("Unknown Title")) {
            return Optional.empty();
        }
        return fetchInfo(title, author)
                .map(v -> {
                    String url = v.path("imageLinks").path("thumbnail").asText(null);
                    if (url == null) {
                        url = v.path("imageLinks").path("smallThumbnail").asText(null);
                    }
                    return url;
                })
                .flatMap(this::downloadImage);
    }

    private Optional<JsonNode> fetchInfo(String title, String author) {
        try {
            String q = "intitle:" + title + "+inauthor:" + author;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + URLEncoder.encode(q, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();

            HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) {
                JsonNode items = mapper.readTree(r.body()).path("items");
                if (items.isArray() && !items.isEmpty()) {
                    return Optional.of(items.get(0).path("volumeInfo"));
                }
            } else {
                LOGGER.warning("Google Books API returned status code: " + r.statusCode());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching book info from Google Books API", e);
        }
        return Optional.empty();
    }

    private Optional<byte[]> downloadImage(String url) {
        if (url == null) return Optional.empty();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url.replace("http://", "https://")))
                    .timeout(Duration.ofSeconds(15))
                    .GET().build();
            HttpResponse<byte[]> r = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() == 200) {
                return Optional.of(r.body());
            } else {
                LOGGER.warning("Error downloading image, status code: " + r.statusCode());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading cover image: " + url, e);
        }
        return Optional.empty();
    }
}
