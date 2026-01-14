package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ExternalMetadataService {
    private static final Logger logger = LoggerFactory.getLogger(ExternalMetadataService.class);
    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes?q=";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ExternalMetadataService() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Optional<String> fetchGenre(String title, String author) {
        return fetchVolumeInfo(title, author).map(vi -> {
            JsonNode categories = vi.get("categories");
            if (categories != null && categories.isArray() && !categories.isEmpty()) {
                return categories.get(0).asText();
            }
            return null;
        });
    }

    public Optional<byte[]> fetchCover(String title, String author) {
        return fetchVolumeInfo(title, author).flatMap(vi -> {
            JsonNode imageLinks = vi.get("imageLinks");
            if (imageLinks != null) {
                String thumbnailUrl = imageLinks.has("thumbnail") ? imageLinks.get("thumbnail").asText() : 
                                     imageLinks.has("smallThumbnail") ? imageLinks.get("smallThumbnail").asText() : null;
                if (thumbnailUrl != null) {
                    return downloadImage(thumbnailUrl);
                }
            }
            return Optional.empty();
        });
    }

    private Optional<JsonNode> fetchVolumeInfo(String title, String author) {
        if (title == null || title.isBlank() || "Unknown".equalsIgnoreCase(title)) {
            return Optional.empty();
        }

        try {
            String query = "intitle:" + title;
            if (author != null && !author.isBlank() && !"Unknown Author".equalsIgnoreCase(author)) {
                query += "+inauthor:" + author;
            }

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_BOOKS_API + encodedQuery))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode items = root.get("items");
                if (items != null && items.isArray() && !items.isEmpty()) {
                    return Optional.of(items.get(0).get("volumeInfo"));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching volume info for title: {} author: {}", title, author, e);
        }
        return Optional.empty();
    }

    private Optional<byte[]> downloadImage(String urlString) {
        try {
            // Google Books API thumbnail URLs often use http, try to force https if possible or just use as is
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString.replace("http://", "https://")))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return Optional.of(response.body());
            }
        } catch (Exception e) {
            logger.error("Error downloading image from {}", urlString, e);
        }
        return Optional.empty();
    }
}
