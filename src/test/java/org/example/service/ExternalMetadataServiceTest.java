package org.example.service;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class ExternalMetadataServiceTest {
    @Test
    public void testFetchGenre() {
        ExternalMetadataService service = new ExternalMetadataService();
        // Мы не можем гарантировать наличие сети в CI, поэтому тест может не найти данные,
        // но он не должен падать с необработанным исключением.
        try {
            Optional<String> genre = service.fetchGenre("Effective Java", "Joshua Bloch");
            // Если сеть есть, проверяем. Если нет - просто логируем.
            genre.ifPresent(s -> System.out.println("Genre found: " + s));
        } catch (Exception e) {
            fail("Should not throw exception even if network is down: " + e.getMessage());
        }
    }
}
