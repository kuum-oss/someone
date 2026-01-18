package org.example.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.example.model.Book;
import org.example.service.BookOrganizer;
import org.example.service.FileService;
import org.example.service.GenreImageService;
import org.example.service.LibraryScanner;
import org.example.service.MetadataService;
import org.example.ui.components.BookDetailsPanel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class BookLibraryGui extends JFrame {

    private final MetadataService metadataService = new MetadataService();
    private final BookOrganizer bookOrganizer = new BookOrganizer();
    private final FileService fileService = new FileService();
    private final GenreImageService genreImageService = new GenreImageService();

    private final List<Book> currentBooks = new ArrayList<>();

    private DefaultMutableTreeNode root;
    private DefaultTreeModel treeModel;
    private JTree tree;

    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton organizeButton;
    private JButton cancelButton;
    private JButton exitButton;

    private BookDetailsPanel detailsPanel;
    private JTextField searchField;
    private JComboBox<String> groupModeCombo;

    private SwingWorker<?, ?> currentWorker;
    private ResourceBundle messages;
    private Locale currentLocale;

    // Для подсчета времени сканирования
    private long startTime;

    public BookLibraryGui() {
        initLocale(Locale.ENGLISH);
        initLookAndFeel();
        initUI();
        initMenuBar();
        setupDragAndDrop();
    }

    /* ===================== INIT ===================== */

    private void initLookAndFeel() {
        try {
            if (!(UIManager.getLookAndFeel() instanceof FlatLaf)) {
                UIManager.setLookAndFeel(new FlatMacLightLaf());
            }
        } catch (Exception ignored) {}
    }

    private void initUI() {
        setTitle(messages.getString("app.title"));
        setSize(1000, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        root = new DefaultMutableTreeNode(messages.getString("tree.root"));
        treeModel = new DefaultTreeModel(root);

        tree = new JTree(treeModel);
        tree.setRowHeight(32);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new BookTreeCellRenderer());

        add(new JScrollPane(tree), BorderLayout.CENTER);

        detailsPanel = new BookDetailsPanel(messages);
        add(detailsPanel, BorderLayout.EAST);

        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof Book book) {
                detailsPanel.updateDetails(book);
            }
        });

        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (node != null && node.getUserObject() instanceof Book book) {
                        openBook(book);
                    }
                }
            }
        });

        statusLabel = new JLabel(messages.getString("status.drag.drop"));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new LineBorder(Color.BLUE, 1, true));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        organizeButton = new JButton(messages.getString("button.organize"));
        organizeButton.setEnabled(false);
        organizeButton.addActionListener(e -> startOrganizing());

        cancelButton = new JButton(messages.getString("button.cancel"));
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> {
            if (currentWorker != null) currentWorker.cancel(true);
        });

        exitButton = new JButton(messages.getString("button.exit"));
        exitButton.addActionListener(e -> exitApplication());

        JPanel top = new JPanel(new BorderLayout());
        top.add(statusLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(exitButton);
        buttons.add(cancelButton);
        buttons.add(organizeButton);

        top.add(buttons, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", messages.getString("search.placeholder"));
        searchField.addActionListener(e -> filterBooks(searchField.getText()));
        
        JButton searchButton = new JButton(messages.getString("button.search"));
        searchButton.addActionListener(e -> filterBooks(searchField.getText()));

        JButton clearButton = new JButton("X");
        clearButton.setToolTipText(messages.getString("button.clear"));
        clearButton.addActionListener(e -> {
            searchField.setText("");
            filterBooks("");
        });

        String[] modes = {
                messages.getString("filter.genre"),
                messages.getString("filter.author"),
                messages.getString("filter.year")
        };
        groupModeCombo = new JComboBox<>(modes);
        groupModeCombo.addActionListener(e -> updateTree(currentBooks));

        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(clearButton, BorderLayout.WEST);
        searchBar.add(searchButton, BorderLayout.EAST);

        searchPanel.add(new JLabel(messages.getString("search.label") + " "), BorderLayout.WEST);
        searchPanel.add(searchBar, BorderLayout.CENTER);
        searchPanel.add(groupModeCombo, BorderLayout.EAST);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(tree), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    /* ===================== MENU ===================== */

    private void initMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu settings = new JMenu(messages.getString("menu.settings"));

        JMenu lang = new JMenu(messages.getString("menu.language"));
        JMenuItem en = new JMenuItem(messages.getString("lang.en"));
        en.addActionListener(e -> changeLanguage(Locale.ENGLISH));
        JMenuItem ru = new JMenuItem(messages.getString("lang.ru"));
        ru.addActionListener(e -> changeLanguage(new Locale("ru")));
        lang.add(en);
        lang.add(ru);

        JMenu theme = new JMenu(messages.getString("menu.theme"));
        JMenuItem light = new JMenuItem(messages.getString("theme.light"));
        light.addActionListener(e -> changeTheme(false));
        JMenuItem dark = new JMenuItem(messages.getString("theme.dark"));
        dark.addActionListener(e -> changeTheme(true));

        theme.add(light);
        theme.add(dark);

        settings.add(lang);
        settings.add(theme);

        JMenu tools = new JMenu(messages.getString("menu.tools"));
        JMenuItem stats = new JMenuItem(messages.getString("menu.stats"));
        stats.addActionListener(e -> showStatistics());
        JMenuItem dups = new JMenuItem(messages.getString("menu.duplicates"));
        dups.addActionListener(e -> findDuplicates());
        
        tools.add(stats);
        tools.add(dups);

        bar.add(settings);
        bar.add(tools);
        setJMenuBar(bar);
    }

    private void showStatistics() {
        if (currentBooks.isEmpty()) return;

        long genresCount = currentBooks.stream().map(Book::getGenre).distinct().count();
        long authorsCount = currentBooks.stream().map(Book::getAuthor).distinct().count();
        Map<String, Long> formats = currentBooks.stream()
                .collect(java.util.stream.Collectors.groupingBy(Book::getFormat, java.util.stream.Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append(MessageFormat.format(messages.getString("stats.total_books"), currentBooks.size())).append("\n");
        sb.append(MessageFormat.format(messages.getString("stats.genres"), genresCount)).append("\n");
        sb.append(MessageFormat.format(messages.getString("stats.authors"), authorsCount)).append("\n\n");
        sb.append(messages.getString("stats.formats")).append("\n");
        formats.forEach((f, c) -> sb.append(f.isEmpty() ? "Unknown" : f).append(": ").append(c).append("\n"));

        JOptionPane.showMessageDialog(this, sb.toString(), messages.getString("stats.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void findDuplicates() {
        if (currentBooks.isEmpty()) return;

        Map<String, List<Book>> map = currentBooks.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> (b.getTitle() + "|" + b.getAuthor()).toLowerCase()));

        List<String> duplicates = map.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> e.getValue().get(0).getTitle() + " (" + e.getValue().get(0).getAuthor() + ") x" + e.getValue().size())
                .toList();

        if (duplicates.isEmpty()) {
            JOptionPane.showMessageDialog(this, messages.getString("duplicates.none"), messages.getString("duplicates.title"), JOptionPane.INFORMATION_MESSAGE);
        } else {
            String msg = MessageFormat.format(messages.getString("duplicates.found"), duplicates.size()) + "\n\n" + String.join("\n", duplicates);
            JOptionPane.showMessageDialog(this, msg, messages.getString("duplicates.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    /* ===================== THEME / LANG ===================== */

    private void changeTheme(Boolean dark) {
        FlatAnimatedLafChange.showSnapshot();
        try {
            if (dark != null && dark) {
                UIManager.setLookAndFeel(new FlatMacDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatMacLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }

    private void initLocale(Locale locale) {
        this.currentLocale = locale;
        this.messages = ResourceBundle.getBundle("messages", locale, new ResourceBundle.Control() {
            @Override
            public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                    throws IllegalAccessException, InstantiationException, IOException {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, "properties");
                try (java.io.InputStream stream = loader.getResourceAsStream(resourceName)) {
                    if (stream != null) {
                        return new PropertyResourceBundle(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
                    }
                }
                return super.newBundle(baseName, locale, format, loader, reload);
            }
        });
    }

    private void changeLanguage(Locale locale) {
        initLocale(locale);
        setTitle(messages.getString("app.title"));
        statusLabel.setText(messages.getString("status.drag.drop"));
        organizeButton.setText(messages.getString("button.organize"));
        cancelButton.setText(messages.getString("button.cancel"));
        exitButton.setText(messages.getString("button.exit"));
        root.setUserObject(messages.getString("tree.root"));
        detailsPanel.setMessages(messages);
        if (searchField != null) {
            searchField.putClientProperty("JTextField.placeholderText", messages.getString("search.placeholder"));
        }
        initMenuBar(); // Re-initialize menu bar to update labels
        
        // Update groupModeCombo items
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(new String[]{
                messages.getString("filter.genre"),
                messages.getString("filter.author"),
                messages.getString("filter.year")
        });
        int selectedIndex = groupModeCombo.getSelectedIndex();
        groupModeCombo.setModel(model);
        if (selectedIndex >= 0 && selectedIndex < model.getSize()) {
            groupModeCombo.setSelectedIndex(selectedIndex);
        }

        treeModel.reload();
    }

    private void filterBooks(String query) {
        if (query == null || query.isBlank()) {
            updateTree(currentBooks);
            return;
        }
        String q = query.toLowerCase();
        List<Book> filtered = currentBooks.stream()
                .filter(b -> b.getTitle().toLowerCase().contains(q) ||
                        b.getAuthor().toLowerCase().contains(q) ||
                        b.getGenre().toLowerCase().contains(q))
                .toList();
        updateTree(filtered);
    }

    private void openBook(Book book) {
        try {
            Desktop.getDesktop().open(book.getFilePath().toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    messages.getString("error.open_file") + "\n" + e.getMessage(),
                    messages.getString("error.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ===================== DRAG & DROP ===================== */

    private void setupDragAndDrop() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    if (e.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        e.acceptDrop(DnDConstants.ACTION_COPY);
                        List<File> files = (List<File>) e.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        processFiles(files);
                    } else {
                        e.rejectDrop();
                        JOptionPane.showMessageDialog(BookLibraryGui.this,
                                messages.getString("error.unsupported_flavor"),
                                messages.getString("error.title"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    e.rejectDrop();
                }
            }
        });
    }

    /* ===================== SCAN FILES ===================== */

    private void processFiles(List<File> files) {
        currentBooks.clear();
        organizeButton.setEnabled(false);
        cancelButton.setEnabled(true);
        startTime = System.currentTimeMillis();
        statusLabel.setText(messages.getString("status.preparing"));
        
        int approxTotal = estimateFileCount(files);
        progressBar.setIndeterminate(approxTotal == 0);
        progressBar.setMaximum(approxTotal > 0 ? approxTotal : 100);
        progressBar.setValue(0);
        progressBar.setVisible(true);

        LibraryScanner scanner = new LibraryScanner(files, currentBooks, metadataService, 
            processed -> {
                int total = currentBooks.size();
                int remaining = total - processed;
                long elapsed = System.currentTimeMillis() - startTime;

                if (!progressBar.isIndeterminate()) {
                    progressBar.setValue(processed);
                }

                statusLabel.setText(
                        MessageFormat.format(
                                messages.getString("status.processed"),
                                processed,
                                total,
                                remaining,
                                formatTime(elapsed)
                        )
                );
            },
            () -> {
                cancelButton.setEnabled(false);
                updateTree(currentBooks);
                organizeButton.setEnabled(!currentBooks.isEmpty());
                statusLabel.setText(
                        MessageFormat.format(messages.getString("status.found"), currentBooks.size())
                );
                progressBar.setVisible(false);
            }
        );
        scanner.setStartTime(startTime);
        currentWorker = scanner;
        scanner.execute();
    }

    private int estimateFileCount(List<File> files) {
        int count = 0;
        for (File f : files) {
            if (f.isFile() && isBookFile(f)) count++;
            else if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) count += estimateFileCount(Arrays.asList(children));
            }
        }
        return count;
    }

    private String formatTime(long millis) {
        long sec = millis / 1000;
        long min = sec / 60;
        sec %= 60;

        if (min > 0) {
            return MessageFormat.format(messages.getString("status.time_format"), min, sec);
        } else {
            return MessageFormat.format(messages.getString("status.time_format_sec"), sec);
        }
    }

    /* ===================== ORGANIZE ===================== */

    private void startOrganizing() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(messages.getString("chooser.title"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        Path defaultDir = Paths.get(System.getProperty("user.home"), "Desktop", "collection");
        try { java.nio.file.Files.createDirectories(defaultDir); } catch (IOException ignored) {}

        chooser.setSelectedFile(defaultDir.toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path targetDir = chooser.getSelectedFile().toPath();
        cancelButton.setEnabled(true);
        organizeButton.setEnabled(false);

        progressBar.setIndeterminate(false);
        progressBar.setMaximum(currentBooks.size());
        progressBar.setValue(0);
        progressBar.setVisible(true);

        currentWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                int i = 0;
                for (Book book : currentBooks) {
                    if (isCancelled()) break;
                    try {
                        fileService.organizeBook(book, targetDir);
                        publish(++i);
                    } catch (IOException e) { e.printStackTrace(); }
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int processed = chunks.get(chunks.size() - 1);
                progressBar.setValue(processed);
                statusLabel.setText(
                        MessageFormat.format(
                                messages.getString("status.copying"),
                                processed,
                                currentBooks.size()
                        )
                );
            }

            @Override
            protected void done() {
                cancelButton.setEnabled(false);
                organizeButton.setEnabled(true);
                progressBar.setVisible(false);
                statusLabel.setText(
                        MessageFormat.format(messages.getString("status.done"), targetDir.toString())
                );
                JOptionPane.showMessageDialog(BookLibraryGui.this, messages.getString("dialog.finished"));
            }
        };

        currentWorker.execute();
    }

    /* ===================== TREE ===================== */

    private void updateTree(List<Book> books) {
        root.removeAllChildren();
        if (books != null) {
            int mode = groupModeCombo.getSelectedIndex(); // 0-Genre, 1-Author, 2-Year
            Map<String, List<Book>> grouped = new TreeMap<>();

            for (Book b : books) {
                String key;
                if (mode == 1) key = b.getAuthor();
                else if (mode == 2) key = b.getYear();
                else key = b.getGenre();

                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(b);
            }

            for (Map.Entry<String, List<Book>> entry : grouped.entrySet()) {
                DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(entry.getKey());
                for (Book b : entry.getValue()) {
                    groupNode.add(new DefaultMutableTreeNode(b));
                }
                root.add(groupNode);
            }
        }
        treeModel.reload();
    }

    private boolean isBookFile(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".pdf") || n.endsWith(".epub") || n.endsWith(".fb2") || n.endsWith(".mobi");
    }

    /* ===================== RENDERER ===================== */

    private class BookTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Map<Book, ImageIcon> coverCache = new WeakHashMap<>();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean exp, boolean leaf, int row, boolean focus) {

            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, focus);

            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof Book book) {
                    setText(book.getTitle());
                    ImageIcon icon = null;
                    if (book.getCover() != null && book.getCover().length > 0) {
                        icon = coverCache.computeIfAbsent(book, b -> {
                            try {
                                ImageIcon original = new ImageIcon(b.getCover());
                                if (original.getIconWidth() > 0) {
                                    Image img = original.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                                    return new ImageIcon(img);
                                }
                            } catch (Exception ignored) {}
                            return null;
                        });
                    }
                    
                    if (icon == null) {
                        icon = genreImageService.getDefaultBookIcon();
                    }
                    setIcon(icon);
                } else if (userObject instanceof String groupName) {
                    setText(groupName);
                    setIcon(genreImageService.getGenreIcon(groupName));
                }
            }
            return this;
        }
    }

    /* ===================== EXIT ===================== */

    private void exitApplication() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BookLibraryGui().setVisible(true));
    }
}
