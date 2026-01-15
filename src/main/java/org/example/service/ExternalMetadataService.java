package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ExternalMetadataService {

    private static final String API = "https://www.googleapis.com/books/v1/volumes?q=";
    private final HttpClient client = HttpClient.newHttpClient();
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
                    .GET().build();

            HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) {
                JsonNode items = mapper.readTree(r.body()).path("items");
                if (items.isArray() && !items.isEmpty()) {
                    return Optional.of(items.get(0).path("volumeInfo"));
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Optional<byte[]> downloadImage(String url) {
        if (url == null) return Optional.empty();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url.replace("http://", "https://")))
                    .GET().build();
            HttpResponse<byte[]> r = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            return r.statusCode() == 200 ? Optional.of(r.body()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
