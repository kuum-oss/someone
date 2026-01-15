package org.example.service;

import org.example.model.Book;

import java.util.*;
import java.util.stream.Collectors;

public class BookOrganizer {

    public Map<String, Map<String, Map<String, List<Book>>>> organize(List<Book> books) {
        return books.stream().collect(
                Collectors.groupingBy(
                        b -> Objects.requireNonNullElse(b.getLanguage(), "Unknown"),
                        TreeMap::new,
                        Collectors.groupingBy(
                                b -> Objects.requireNonNullElse(b.getGenre(), "General"),
                                TreeMap::new,
                                Collectors.groupingBy(
                                        b -> Objects.requireNonNullElse(b.getSeries(), "No Series"),
                                        TreeMap::new,
                                        Collectors.toList()
                                )
                        )
                )
        );
    }
}
