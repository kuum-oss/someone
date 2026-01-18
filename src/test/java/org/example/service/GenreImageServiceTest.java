package org.example.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import static org.junit.jupiter.api.Assertions.*;

public class GenreImageServiceTest {
    @Test
    public void testGetGenreIcon() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping GUI test in headless environment");
        
        GenreImageService service = new GenreImageService();
        ImageIcon fantasyIcon = service.getGenreIcon("Fantasy");
        assertNotNull(fantasyIcon, "Icon for Fantasy should not be null");
        
        ImageIcon thrillerIcon = service.getGenreIcon("Thriller");
        assertNotNull(thrillerIcon, "Icon for Thriller should not be null");

        ImageIcon horrorIcon = service.getGenreIcon("Horror");
        assertNotNull(horrorIcon, "Icon for Horror should not be null");

        ImageIcon unknownIcon = service.getGenreIcon("UnknownGenre123");
        assertNotNull(unknownIcon, "Icon for unknown genre should be generated");
        
        // Test Group Headers
        ImageIcon artistsIcon = service.getGenreIcon("Artists");
        assertNotNull(artistsIcon, "Icon for Artists group should not be null");
    }

    @Test
    public void testDefaultBookIcon() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping GUI test in headless environment");
        
        GenreImageService service = new GenreImageService();
        ImageIcon defaultIcon = service.getDefaultBookIcon();
        assertNotNull(defaultIcon, "Default book icon should not be null");
        assertEquals(24, defaultIcon.getIconWidth());
        assertEquals(24, defaultIcon.getIconHeight());
    }
}
