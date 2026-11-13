package ifsuldeminas.edu.br.ifsystem;

import static org.junit.jupiter.api.Assertions.*;

import ifsuldeminas.edu.br.ifsystem.room.provider.SpreadsheetRoomProvider;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.*;
import java.time.Duration;

class SpreadsheetRoomProviderTest {
    @TempDir Path directory;

    private SpreadsheetRoomProvider provider(String content, String idColumn) throws Exception {
        Path path = directory.resolve("rooms.csv");
        Files.writeString(path, content);
        return new SpreadsheetRoomProvider(
                path.toUri().toString(),
                idColumn,
                "Sala / Local",
                Duration.ZERO,
                new DefaultResourceLoader());
    }

    @Test
    void listsAndDeduplicatesTimetableRows() throws Exception {
        var source = provider("Sala / Local\nSala 114\nSala 114\nSala 115\n", "");
        assertEquals(2, source.findAll().size());
        assertEquals("Sala 114", source.findById("sala-114").orElseThrow().name());
        assertTrue(source.findById("missing").isEmpty());
    }

    @Test
    void rejectsMissingColumns() throws Exception {
        assertEquals(
                503,
                assertThrows(
                                ResponseStatusException.class,
                                () -> provider("Wrong\nvalue\n", "").findAll())
                        .getStatusCode()
                        .value());
    }

    @Test
    void rejectsConflictingDuplicateIdentifiers() throws Exception {
        assertThrows(
                ResponseStatusException.class,
                () -> provider("ID,Sala / Local\na,Sala 1\na,Sala 2\n", "ID").findAll());
    }

    @Test
    void acceptsRepeatedIdenticalExplicitIdentifiers() throws Exception {
        assertEquals(1, provider("ID,Sala / Local\na,Sala 1\na,Sala 1\n", "ID").findAll().size());
    }

    @Test
    void rejectsMalformedCsv() throws Exception {
        assertThrows(
                ResponseStatusException.class,
                () -> provider("Sala / Local\n\"unclosed\n", "").findAll());
    }

    @Test
    void rejectsMissingFile() {
        var source =
                new SpreadsheetRoomProvider(
                        directory.resolve("absent.csv").toUri().toString(),
                        "",
                        "Sala / Local",
                        Duration.ZERO,
                        new DefaultResourceLoader());
        assertThrows(ResponseStatusException.class, source::findAll);
    }

    @Test
    void rejectsEmptySource() throws Exception {
        assertThrows(ResponseStatusException.class, () -> provider("Sala / Local\n", "").findAll());
    }

    @Test
    void readsQuotedFieldsAndKeepsIdsStableAcrossOrdering() throws Exception {
        var source = provider("Sala / Local\n\"Sala, 1\"\nSala 2\n", "");
        var first = source.findAll();
        Files.writeString(directory.resolve("rooms.csv"), "Sala / Local\nSala 2\n\"Sala, 1\"\n");
        assertEquals(first, source.findAll());
    }
}
