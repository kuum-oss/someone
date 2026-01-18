package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GenreImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenreImageService.class);
    private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private static final String DEFAULT_GIF_URL = "https://media.giphy.com/media/3o7bu3XilJ5BOiSGic/giphy.gif";

    // Маппинг жанров на GIF-ки (используем проверенные ссылки или ключевые слова для поиска)
    private static final Map<String, String> GENRE_ICONS = new HashMap<>();

    static {
        // Mapping genres and group headers to local icon names in src/main/resources/icons/
        GENRE_ICONS.put("Science Fiction", "scifi.png");
        GENRE_ICONS.put("Sci-Fi", "scifi.png");
        GENRE_ICONS.put("Fantasy", "fantasy.png");
        GENRE_ICONS.put("Horror", "horror.png");
        GENRE_ICONS.put("Thriller", "thriller.png");
        GENRE_ICONS.put("Romance", "romance.png");
        GENRE_ICONS.put("Detective", "detective.png");
        GENRE_ICONS.put("Mystery", "mystery.png");
        GENRE_ICONS.put("History", "history.png");
        GENRE_ICONS.put("Programming", "programming.png");
        GENRE_ICONS.put("Computers", "computers.png");
        GENRE_ICONS.put("Psychology", "psychology.png");
        GENRE_ICONS.put("Fiction", "book.png");
        GENRE_ICONS.put("Classic", "book.png");
        GENRE_ICONS.put("Action", "action.png");
        GENRE_ICONS.put("Adventure", "adventure.png");
        GENRE_ICONS.put("Dystopia", "scifi.png");
        GENRE_ICONS.put("Crime", "detective.png");
        
        // Group Headers Mapping
        GENRE_ICONS.put("Artists", "history.png"); // Using history/art related
        GENRE_ICONS.put("General", "book.png");
        GENRE_ICONS.put("Unknown", "book.png");
    }

    public ImageIcon getDefaultBookIcon() {
        return getIconFromResource("book.png");
    }

    private ImageIcon getIconFromResource(String iconName) {
        try {
            URL iconUrl = getClass().getResource("/icons/" + iconName);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                if (icon.getIconWidth() > 0) {
                    Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                }
            } else {
                LOGGER.warn("Icon not found in resources: /icons/{}", iconName);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading icon from resource: {}", iconName, e);
        }
        return null;
    }

    public ImageIcon getGenreIcon(String genre) {
        if (genre == null || GraphicsEnvironment.isHeadless()) return null;

        return cache.computeIfAbsent(genre, g -> {
            // 1. Try to find local icon from resources
            String iconName = findIconName(g);
            if (iconName != null) {
                ImageIcon icon = getIconFromResource(iconName);
                if (icon != null) return icon;
            }

            // 2. Try remote URLs as fallback (keeping some for backward compatibility or if local missing)
            String remoteUrl = findRemoteUrl(g);
            if (remoteUrl != null) {
                try {
                    ImageIcon icon = new ImageIcon(new URL(remoteUrl));
                    if (icon.getIconWidth() > 0) {
                        Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                        return new ImageIcon(img);
                    }
                } catch (Exception ignored) {}
            }

            // 3. Last resort: generate pixel icon
            return createPixelIcon(g);
        });
    }

    private String findIconName(String genre) {
        String lowerGenre = genre.toLowerCase();
        for (Map.Entry<String, String> entry : GENRE_ICONS.entrySet()) {
            if (lowerGenre.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String findRemoteUrl(String genre) {
        // Keeping the old mapping as a fallback
        Map<String, String> remoteMapping = new HashMap<>();
        remoteMapping.put("Science Fiction", "https://cdn-icons-png.flaticon.com/512/6119/6119533.png");
        remoteMapping.put("Fantasy", "https://cdn-icons-png.flaticon.com/512/1065/1065051.png");
        remoteMapping.put("Horror", "https://cdn-icons-png.flaticon.com/512/1065/1065056.png");
        remoteMapping.put("Romance", "https://cdn-icons-png.flaticon.com/512/833/833472.png");
        remoteMapping.put("Detective", "https://cdn-icons-png.flaticon.com/512/3504/3504445.png");
        remoteMapping.put("Mystery", "https://cdn-icons-png.flaticon.com/512/3504/3504445.png");
        remoteMapping.put("History", "https://cdn-icons-png.flaticon.com/512/2618/2618239.png");
        remoteMapping.put("Programming", "https://cdn-icons-png.flaticon.com/512/1149/1149168.png");
        remoteMapping.put("Computers", "https://cdn-icons-png.flaticon.com/512/3062/3062310.png");
        remoteMapping.put("Psychology", "https://cdn-icons-png.flaticon.com/512/2040/2040432.png");
        remoteMapping.put("Fiction", "https://cdn-icons-png.flaticon.com/512/3389/3389032.png");
        
        String lowerGenre = genre.toLowerCase();
        for (Map.Entry<String, String> entry : remoteMapping.entrySet()) {
            if (lowerGenre.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private ImageIcon createPixelIcon(String genre) {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // Детерминированный цвет на основе названия жанра
        int hash = genre.hashCode();
        Color baseColor = new Color(
                (hash & 0xFF0000) >> 16,
                (hash & 0x00FF00) >> 8,
                (hash & 0x0000FF)
        );

        g2.setColor(baseColor);
        // Рисуем "пиксельный" паттерн
        int pixelSize = 4;
        for (int x = 0; x < size; x += pixelSize) {
            for (int y = 0; y < size; y += pixelSize) {
                if (((hash >> (x + y)) & 1) == 1) {
                    g2.fillRect(x, y, pixelSize, pixelSize);
                }
            }
        }

        // Добавляем рамку
        g2.setColor(baseColor.darker());
        g2.drawRect(0, 0, size - 1, size - 1);

        g2.dispose();
        return new ImageIcon(img);
    }
}
