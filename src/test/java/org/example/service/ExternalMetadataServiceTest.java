package org.example.service;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class ExternalMetadataServiceTest {
    @Test
    public void testFetchGenre() {
        ExternalMetadataService service = new ExternalMetadataService();
        // Тестируем на известной книге
        Optional<String> genre = service.fetchGenre("The Hobbit", "J.R.R. Tolkien");
        assertTrue(genre.isPresent());
        System.out.println("Genre found: " + genre.get());
    }
}
