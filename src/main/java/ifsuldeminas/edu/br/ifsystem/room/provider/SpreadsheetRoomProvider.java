package ifsuldeminas.edu.br.ifsystem.room.provider;

import ifsuldeminas.edu.br.ifsystem.room.domain.Room;

import org.apache.commons.csv.CSVFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.*;
import java.util.*;

@Component
public class SpreadsheetRoomProvider implements RoomDataProvider {
    private final String location;
    private final String idColumn;
    private final String nameColumn;
    private final ResourceLoader loader;
    private final Duration cacheTtl;
    private final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
    private List<Room> cached;
    private Instant expiresAt = Instant.MIN;

    public SpreadsheetRoomProvider(
            @Value("${app.rooms.location}") String location,
            @Value("${app.rooms.id-column:}") String idColumn,
            @Value("${app.rooms.name-column:Sala / Local}") String nameColumn,
            @Value("${app.rooms.cache-ttl:PT5M}") Duration cacheTtl,
            ResourceLoader loader) {
        this.location = location;
        this.idColumn = idColumn;
        this.nameColumn = nameColumn;
        this.cacheTtl = cacheTtl;
        this.loader = loader;
    }

    @Override
    public synchronized List<Room> findAll() {
        if (cached != null && Instant.now().isBefore(expiresAt)) return cached;
        try (Reader reader =
                        new InputStreamReader(
                                new ByteArrayInputStream(readSource()), StandardCharsets.UTF_8);
                var parser =
                        CSVFormat.DEFAULT
                                .builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreEmptyLines(true)
                                .setTrim(true)
                                .get()
                                .parse(reader)) {
            if (!parser.getHeaderMap().containsKey(nameColumn)
                    || (!idColumn.isBlank() && !parser.getHeaderMap().containsKey(idColumn)))
                throw new IOException("Missing room columns");
            Map<String, Room> rooms = new TreeMap<>();
            for (var row : parser) {
                if (!row.isConsistent()) throw new IOException("Invalid CSV row");
                String name = row.get(nameColumn).strip();
                // The timetable contains classes without an assigned physical room.
                if (name.isBlank()) continue;
                String id = idColumn.isBlank() ? stableId(name) : row.get(idColumn).strip();
                if (id.isBlank() || id.length() > 255 || name.length() > 255)
                    throw new IOException("Invalid room identifier or name");
                Room room = new Room(id, name, null, null, null, null);
                Room previous = rooms.putIfAbsent(id, room);
                // Repeated timetable rows are expected; different names sharing an ID are not.
                if (previous != null && !previous.equals(room))
                    throw new IOException("Duplicate room identifier");
            }
            if (rooms.isEmpty()) throw new IOException("No rooms in spreadsheet");
            cached = List.copyOf(rooms.values());
            expiresAt = Instant.now().plus(cacheTtl);
            return cached;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            throw unavailable(e);
        }
    }

    private byte[] readSource() throws IOException, InterruptedException {
        if (location.startsWith("https://")) {
            var response =
                    client.send(
                            HttpRequest.newBuilder(URI.create(location))
                                    .timeout(Duration.ofSeconds(30))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream input = response.body()) {
                if (response.statusCode() != 200)
                    throw new IOException("Spreadsheet HTTP " + response.statusCode());
                return limited(input);
            }
        }
        if (location.startsWith("http:")) throw new IOException("HTTPS required");
        try (InputStream input = loader.getResource(location).getInputStream()) {
            return limited(input);
        }
    }

    private byte[] limited(InputStream input) throws IOException {
        byte[] data = input.readNBytes(5_000_001);
        if (data.length > 5_000_000) throw new IOException("Spreadsheet exceeds 5 MB");
        return data;
    }

    public static String stableId(String name) {
        return Normalizer.normalize(name.strip(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-");
    }

    private ResponseStatusException unavailable(Exception cause) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Room source unavailable or invalid", cause);
    }
}
