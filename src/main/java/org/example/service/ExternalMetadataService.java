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

    public Optional<byte[]> fetchCover(String title, String author) {
        return fetchInfo(title, author)
                .map(v -> v.path("imageLinks").path("thumbnail").asText(null))
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
