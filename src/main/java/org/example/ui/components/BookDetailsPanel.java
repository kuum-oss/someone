package org.example.ui.components;

import org.example.model.Book;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class BookDetailsPanel extends JPanel {
    private final JLabel coverLabel;
    private final JLabel authorPhotoLabel;
    private final JTextArea infoArea;
    private final JButton descriptionButton;
    private final JButton youtubeButton;
    private ResourceBundle messages;
    private Book currentBook;

    public BookDetailsPanel(ResourceBundle messages) {
        this.messages = messages;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 0));
        updateBorder();

        coverLabel = new JLabel();
        coverLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coverLabel.setPreferredSize(new Dimension(280, 200));

        authorPhotoLabel = new JLabel();
        authorPhotoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        authorPhotoLabel.setPreferredSize(new Dimension(280, 150));
        authorPhotoLabel.setBorder(BorderFactory.createTitledBorder(messages.getString("details.author_photo")));

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(new Color(0, 0, 0, 0));
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

        descriptionButton = new JButton(messages.getString("button.description"));
        descriptionButton.addActionListener(e -> showDescription());
        descriptionButton.setVisible(false);

        youtubeButton = new JButton(messages.getString("button.youtube"));
        youtubeButton.addActionListener(e -> watchReview());
        youtubeButton.setVisible(false);
        try {
            java.net.URL iconUrl = getClass().getResource("/icons/youtube.png");
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                youtubeButton.setIcon(new ImageIcon(img));
            }
        } catch (Exception ignored) {}

        JPanel photos = new JPanel(new GridLayout(2, 1));
        photos.add(coverLabel);
        photos.add(authorPhotoLabel);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        buttonPanel.add(descriptionButton);
        buttonPanel.add(youtubeButton);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(photos, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void updateDetails(Book book) {
        this.currentBook = book;
        if (book.getCover() != null) {
            ImageIcon icon = new ImageIcon(book.getCover());
            if (icon.getIconWidth() > 0) {
                Image img = icon.getImage().getScaledInstance(200, 280, Image.SCALE_SMOOTH);
                coverLabel.setIcon(new ImageIcon(img));
                coverLabel.setText(null);
            } else {
                showNoCover();
            }
        } else {
            showNoCover();
        }

        if (book.getAuthorPhoto() != null) {
            ImageIcon icon = new ImageIcon(book.getAuthorPhoto());
            if (icon.getIconWidth() > 0) {
                Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                authorPhotoLabel.setIcon(new ImageIcon(img));
                authorPhotoLabel.setText(null);
                authorPhotoLabel.setVisible(true);
            } else {
                authorPhotoLabel.setVisible(false);
            }
        } else {
            authorPhotoLabel.setVisible(false);
        }

        String info = java.text.MessageFormat.format(
                "{0}: {1}\n{2}: {3}\n{4}: {5}\n{6}: {7}\n{8}: {9}\n{10}: {11}\n{12}: {13}",
                messages.getString("details.title"), book.getTitle(),
                messages.getString("details.author"), book.getAuthor(),
                messages.getString("details.genre"), book.getGenre(),
                messages.getString("details.year"), book.getYear(),
                messages.getString("details.series"), book.getSeries(),
                messages.getString("details.language"), book.getLanguage(),
                messages.getString("details.path"), book.getFilePath().toString()
        );
        infoArea.setText(info);
        descriptionButton.setVisible(book.getDescription() != null && !book.getDescription().isBlank());
        
        boolean hasTitleAndAuthor = book.getTitle() != null && !book.getTitle().equalsIgnoreCase("Unknown Title") &&
                book.getAuthor() != null && !book.getAuthor().equalsIgnoreCase("Unknown Author");
        youtubeButton.setVisible(hasTitleAndAuthor);
    }

    private void showNoCover() {
        coverLabel.setIcon(null);
        coverLabel.setText(messages.getString("details.no_cover"));
    }

    private void showDescription() {
        if (currentBook == null) return;
        String desc = currentBook.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = messages.getString("dialog.description.none");
        }

        JTextArea textArea = new JTextArea(desc);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(this, scrollPane, 
                messages.getString("dialog.description.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void watchReview() {
        if (currentBook == null) return;
        try {
            String query = currentBook.getTitle() + " " + currentBook.getAuthor() + " Review";
            String url = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    messages.getString("error.open_file") + ": " + e.getMessage(), 
                    messages.getString("error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setMessages(ResourceBundle messages) {
        this.messages = messages;
        updateBorder();
        authorPhotoLabel.setBorder(BorderFactory.createTitledBorder(messages.getString("details.author_photo")));
        descriptionButton.setText(messages.getString("button.description"));
        youtubeButton.setText(messages.getString("button.youtube"));
        if (currentBook != null) {
            updateDetails(currentBook);
        }
    }

    private void updateBorder() {
        setBorder(BorderFactory.createTitledBorder(messages.getString("panel.details")));
    }
}
