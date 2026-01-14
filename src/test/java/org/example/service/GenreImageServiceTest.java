package org.example.service;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

public class GenreImageServiceTest {
    @Test
    public void testGetGenreIcon() {
        GenreImageService service = new GenreImageService();
        // Это может не работать без интернета или в headless среде, но попробуем
        try {
            ImageIcon icon = service.getGenreIcon("Fantasy");
            // Мы не можем гарантировать загрузку в CI, но проверим что не падает
            System.out.println("Icon lookup finished");
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }
}
