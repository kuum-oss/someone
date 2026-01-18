package org.example.service;

import org.example.model.Book;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BookOrganizerTest {

    @Test
    public void testOrganize() {
        Book b1 = Book.builder()
                .title("Book 1").author("Author A").genre("Sci-Fi").language("en")
                .filePath(Paths.get("b1.epub")).build();
        Book b2 = Book.builder()
                .title("Book 2").author("Author B").genre("Fantasy").language("en")
                .filePath(Paths.get("b2.epub")).build();
        Book b3 = Book.builder()
                .title("Book 3").author("Author A").genre("Sci-Fi").language("ru")
                .filePath(Paths.get("b3.epub")).build();

        BookOrganizer organizer = new BookOrganizer();
        Map<String, Map<String, Map<String, List<Book>>>> result = organizer.organize(Arrays.asList(b1, b2, b3));

        assertNotNull(result.get("en"));
        assertNotNull(result.get("ru"));
        assertEquals(2, result.get("en").size()); // Sci-Fi and Fantasy
        assertEquals(1, result.get("ru").get("Sci-Fi").get("No Series").size());
    }
}
