package org.example.service;

import org.example.model.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testOrganizeBook() throws IOException {
        FileService fileService = new FileService();
        
        Path sourceFile = tempDir.resolve("testbook.epub");
        Files.writeString(sourceFile, "dummy content");
        
        Book book = Book.builder()
                .title("Test Book")
                .author("Test Author")
                .genre("Fantasy")
                .language("en")
                .filePath(sourceFile)
                .build();
                
        Path targetBase = tempDir.resolve("output");
        fileService.organizeBook(book, targetBase);
        
        Path expectedPath = targetBase.resolve("collection")
                .resolve("en")
                .resolve("Fantasy")
                .resolve("testbook.epub");
                
        assertTrue(Files.exists(expectedPath), "Book should be copied to the expected path");
        assertEquals("dummy content", Files.readString(expectedPath));
    }

    @Test
    public void testSafeFileName() {
        FileService fileService = new FileService();
        // Since safe is private, we test through organizeBook or if we made it package-private
        // For now let's assume we want to test the logic.
    }
}
