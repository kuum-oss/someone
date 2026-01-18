package org.example.service;

import org.example.model.Book;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

public class TitleNormalizationTest {

    @Test
    public void testTitleNormalization() {
        MetadataService service = new MetadataService();
        
        // Нам нужно сделать метод normalizeTitle доступным для теста или тестировать через extractMetadata
        // Для простоты проверим через рефлексию или сделаем метод package-private
        // Но лучше просто протестировать саму логику, если она вынесена.
        
        // Проверим имя файла
        // К сожалению, extractMetadata требует реального пути к файлу.
        // Поэтому добавим вспомогательный тест в MetadataService или протестируем саму логику здесь.
    }

    @Test
    public void testNormalizeTitleLogic() {
        // Мы можем протестировать логику нормализации, если скопируем её сюда 
        // или если сделаем метод в MetadataService доступным.
        // Сделаем normalizeTitle package-private в MetadataService.
        MetadataService service = new MetadataService();
        assertEquals("Ubivstvo Za Etiketom", service.normalizeTitle("ubivstvo-za-etiketom"));
        assertEquals("War And Peace", service.normalizeTitle("war_and_peace"));
        assertEquals("Example Title", service.normalizeTitle("  example---title  "));
    }
}
