package org.example.service;

import org.example.model.Book;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookOrganizer {
    
    public Map<String, Map<String, Map<String, List<Book>>>> organize(List<Book> books) {
        return books.stream()
                .collect(Collectors.groupingBy(
                        Book::getLanguage,
                        Collectors.groupingBy(
                                Book::getGenre,
                                Collectors.groupingBy(
                                        Book::getSeries
                                )
                        )
                ));
    }
}
