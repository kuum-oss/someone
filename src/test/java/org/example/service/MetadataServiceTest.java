package org.example.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.mime.MediaType;
import org.example.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataServiceTest {

    private Parser parser;
    private StubExternalMetadataService external;
    private MetadataService metadataService;

    private static class StubExternalMetadataService extends ExternalMetadataService {
        boolean fetchGenreCalled = false;

        public StubExternalMetadataService() {
            super(null, null, null);
        }

        @Override
        public Optional<String> fetchGenre(String title, String author) {
            fetchGenreCalled = true;
            return Optional.of("Science Fiction");
        }

        @Override
        public Optional<String> fetchYear(String title, String author) {
            return Optional.of("2024");
        }

        @Override
        public Optional<String> fetchDescription(String title, String author) {
            return Optional.of("Test Description");
        }

        @Override
        public Optional<byte[]> fetchCover(String title, String author) {
            return Optional.empty();
        }

        @Override
        public Optional<byte[]> fetchAuthorPhoto(String author) {
            return Optional.empty();
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new Parser() {
            @Override
            public Set<MediaType> getSupportedTypes(ParseContext context) {
                return Collections.emptySet();
            }

            @Override
            public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) {
                // Do nothing
            }
        };
        external = new StubExternalMetadataService();
        metadataService = new MetadataService(parser, external);
    }

    @Test
    public void testExtractMetadataWithExternalFallback() {
        Path path = Paths.get("test.epub");
        Book book = metadataService.extractMetadata(path);

        assertEquals("Science Fiction", book.getGenre());
        assertEquals("2024", book.getYear());
        assertEquals("Test Description", book.getDescription());
        assertTrue(external.fetchGenreCalled);
    }

    @Test
    public void testNormalizeTitle() {
        assertEquals("Effective Java", metadataService.normalizeTitle("effective-java"));
        assertEquals("The Lord of the Rings", metadataService.normalizeTitle("the_lord_of_the_rings"));
        assertEquals("Clean Code", metadataService.normalizeTitle("CLEAN CODE"));
    }
}
