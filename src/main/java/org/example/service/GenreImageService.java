package org.example.service;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GenreImageService {
    private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private static final String DEFAULT_GIF_URL = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7bu3XilJ5BOiSGic/giphy.gif"; // Пример заглушки

    // Маппинг жанров на GIF-ки (используем проверенные ссылки или ключевые слова для поиска)
    private static final Map<String, String> GENRE_GIFS = new HashMap<>();

    static {
        // Ссылки на анимированные стикеры/гифки
        GENRE_GIFS.put("Fiction", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7bu3XilJ5BOiSGic/giphy.gif");
        GENRE_GIFS.put("Fantasy", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/l41lTfO7K9Lp3FmCs/giphy.gif");
        GENRE_GIFS.put("Science Fiction", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7TKSjP6S7J7n9J7y/giphy.gif");
        GENRE_GIFS.put("Mystery", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7TKURi8fJ5j2J9zG/giphy.gif");
        GENRE_GIFS.put("Horror", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7TKVUn7iM8FMEU24/giphy.gif");
        GENRE_GIFS.put("Romance", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/l41lTfO7K9Lp3FmCs/giphy.gif");
        GENRE_GIFS.put("History", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7TKSjP6S7J7n9J7y/giphy.gif");
        GENRE_GIFS.put("Programming", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7TKSjP6S7J7n9J7y/giphy.gif");
        GENRE_GIFS.put("Computers", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJqZ3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1z/3o7TKSjP6S7J7n9J7y/giphy.gif");
    }

    public ImageIcon getGenreIcon(String genre) {
        if (genre == null || GraphicsEnvironment.isHeadless()) return null;

        return cache.computeIfAbsent(genre, g -> {
            // Fallback icon from resources
            ImageIcon fallback = null;
            try {
                URL localUrl = getClass().getResource("/icons/book.png");
                if (localUrl != null) {
                    fallback = new ImageIcon(localUrl);
                }
            } catch (Exception ignored) {}

            try {
                String urlString = findGifUrl(g);
                URL url = new URL(urlString);
                ImageIcon icon = new ImageIcon(url);

                if (icon.getIconWidth() > 1) {
                    if (urlString.endsWith(".gif")) {
                        return icon;
                    }
                    Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                }
            } catch (Exception e) {
                // Ignore and use fallback
            }
            return fallback;
        });
    }

    private String findGifUrl(String genre) {
        for (Map.Entry<String, String> entry : GENRE_GIFS.entrySet()) {
            if (genre.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return "https://img.icons8.com/color/48/book.png"; // Default
    }
}
