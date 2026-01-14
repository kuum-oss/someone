package org.example.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.example.model.Book;
import org.example.service.BookOrganizer;
import org.example.service.FileService;
import org.example.service.GenreImageService;
import org.example.service.MetadataService;

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
    private JButton organizeButton;
    private JButton cancelButton;

    private SwingWorker<Void, Integer> currentWorker;

    private ResourceBundle messages;
    private Locale currentLocale;

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
        tree.setCellRenderer(new BookTreeCellRenderer());

        add(new JScrollPane(tree), BorderLayout.CENTER);

        statusLabel = new JLabel(messages.getString("status.drag.drop"));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new LineBorder(Color.BLUE, 1, true));

        organizeButton = new JButton(messages.getString("button.organize"));
        organizeButton.setEnabled(false);
        organizeButton.addActionListener(e -> startOrganizing());

        cancelButton = new JButton(messages.getString("button.cancel"));
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> {
            if (currentWorker != null) {
                currentWorker.cancel(true);
            }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(statusLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(organizeButton);

        top.add(buttons, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
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
        JMenuItem auto = new JMenuItem(messages.getString("theme.system"));
        auto.addActionListener(e -> changeTheme(null));
        JMenuItem light = new JMenuItem(messages.getString("theme.light"));
        light.addActionListener(e -> changeTheme(false));
        JMenuItem dark = new JMenuItem(messages.getString("theme.dark"));
        dark.addActionListener(e -> changeTheme(true));

        theme.add(auto);
        theme.add(light);
        theme.add(dark);

        settings.add(lang);
        settings.add(theme);

        bar.add(settings);
        setJMenuBar(bar);
    }

    /* ===================== THEME / LANG ===================== */

    private void changeTheme(Boolean dark) {
        FlatAnimatedLafChange.showSnapshot();
        try {
            if (dark == null) {
                // "System" — оставляем текущую тему без изменений
                // (FlatLaf не имеет авто-переключения)
                return;
            }

            if (dark) {
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
        this.messages = ResourceBundle.getBundle("messages", locale);
    }

    private void changeLanguage(Locale locale) {
        initLocale(locale);
        setTitle(messages.getString("app.title"));
        statusLabel.setText(messages.getString("status.drag.drop"));
        organizeButton.setText(messages.getString("button.organize"));
        cancelButton.setText(messages.getString("button.cancel"));
        root.setUserObject(messages.getString("tree.root"));
        treeModel.reload();
    }

    /* ===================== DRAG & DROP ===================== */

    private void setupDragAndDrop() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) e.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    processFiles(files);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /* ===================== SCAN FILES ===================== */

    private void processFiles(List<File> files) {
        currentBooks.clear();
        organizeButton.setEnabled(false);
        cancelButton.setEnabled(true);
        statusLabel.setText(messages.getString("status.preparing"));

        currentWorker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() {
                scan(files);
                return null;
            }

            private void scan(List<File> list) {
                for (File f : list) {
                    if (isCancelled()) return;
                    if (f.isDirectory()) {
                        scan(Arrays.asList(Objects.requireNonNull(f.listFiles())));
                    } else if (isBookFile(f)) {
                        currentBooks.add(metadataService.extractMetadata(f.toPath()));
                        publish(currentBooks.size());
                    }
                }
            }

            @Override
            protected void process(List<Integer> chunks) {
                statusLabel.setText(
                        MessageFormat.format(
                                messages.getString("status.processed"),
                                chunks.get(chunks.size() - 1)
                        )
                );
            }

            @Override
            protected void done() {
                cancelButton.setEnabled(false);
                updateTree(currentBooks);
                organizeButton.setEnabled(!currentBooks.isEmpty());
                statusLabel.setText(
                        MessageFormat.format(messages.getString("status.found"), currentBooks.size())
                );
            }
        };

        currentWorker.execute();
    }

    /* ===================== ORGANIZE ===================== */

    private void startOrganizing() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(messages.getString("chooser.title"));

        Path defaultDir = Paths.get(System.getProperty("user.home"), "Desktop", "collection");
        try {
            java.nio.file.Files.createDirectories(defaultDir);
            chooser.setSelectedFile(defaultDir.toFile());
        } catch (IOException ignored) {}

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path targetDir = chooser.getSelectedFile().toPath();
        cancelButton.setEnabled(true);
        organizeButton.setEnabled(false);

        currentWorker = new SwingWorker<Void, Integer>() {

            @Override
            protected Void doInBackground() {
                int i = 0;
                for (Book book : currentBooks) {
                    if (isCancelled()) break;
                    try {
                        fileService.organizeBook(book, targetDir);
                        publish(++i);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                statusLabel.setText(
                        MessageFormat.format(
                                messages.getString("status.copying"),
                                chunks.get(chunks.size() - 1),
                                currentBooks.size()
                        )
                );
            }

            @Override
            protected void done() {
                cancelButton.setEnabled(false);
                organizeButton.setEnabled(true);
                statusLabel.setText(messages.getString("status.done"));
            }
        };

        currentWorker.execute();
    }

    /* ===================== TREE ===================== */

    private void updateTree(List<Book> books) {
        root.removeAllChildren();

        bookOrganizer.organize(books).forEach((lang, genres) -> {
            DefaultMutableTreeNode langNode = new DefaultMutableTreeNode(lang);
            root.add(langNode);

            genres.forEach((genre, seriesMap) -> {
                DefaultMutableTreeNode genreNode = new DefaultMutableTreeNode(genre);
                langNode.add(genreNode);

                seriesMap.forEach((series, list) -> {
                    DefaultMutableTreeNode seriesNode = new DefaultMutableTreeNode(series);
                    genreNode.add(seriesNode);

                    list.forEach(book ->
                            seriesNode.add(new DefaultMutableTreeNode(book))
                    );
                });
            });
        });

        treeModel.reload();
    }

    private boolean isBookFile(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".pdf") || n.endsWith(".epub") || n.endsWith(".fb2") || n.endsWith(".mobi");
    }

    /* ===================== RENDERER ===================== */

    private class BookTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean sel, boolean exp,
                boolean leaf, int row, boolean focus) {

            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, focus);

            if (value instanceof DefaultMutableTreeNode node &&
                    node.getUserObject() instanceof Book book) {
                setText(book.getTitle());
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BookLibraryGui().setVisible(true));
    }
}
