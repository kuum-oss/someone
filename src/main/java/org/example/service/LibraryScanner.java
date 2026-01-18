package org.example.service;

import org.example.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class LibraryScanner extends SwingWorker<Void, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryScanner.class);
    private final List<File> files;
    private final List<Book> currentBooks;
    private final MetadataService metadataService;
    private final Consumer<Integer> onProgress;
    private final Runnable onDone;
    private long startTime;

    public LibraryScanner(List<File> files, List<Book> currentBooks, MetadataService metadataService, 
                          Consumer<Integer> onProgress, Runnable onDone) {
        this.files = files;
        this.currentBooks = currentBooks;
        this.metadataService = metadataService;
        this.onProgress = onProgress;
        this.onDone = onDone;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    @Override
    protected Void doInBackground() {
        try {
            scan(files);
        } catch (Exception e) {
            LOGGER.error("Critical error during library scanning", e);
        }
        return null;
    }

    private void scan(List<File> list) {
        for (File f : list) {
            if (isCancelled()) return;
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    scan(Arrays.asList(children));
                }
            } else if (isBookFile(f)) {
                try {
                    currentBooks.add(metadataService.extractMetadata(f.toPath()));
                    publish(currentBooks.size());
                } catch (Exception e) {
                    LOGGER.warn("Failed to extract metadata from file: {}", f.getAbsolutePath(), e);
                }
            }
        }
    }

    private boolean isBookFile(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".pdf") || n.endsWith(".epub") || n.endsWith(".fb2") || n.endsWith(".mobi");
    }

    @Override
    protected void process(List<Integer> chunks) {
        onProgress.accept(chunks.get(chunks.size() - 1));
    }

    @Override
    protected void done() {
        onDone.run();
    }
}
