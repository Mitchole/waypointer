package com.waypointer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaypointStorePersistenceTest
{
    private Path tmpDir;
    private LibraryJsonCodec codec;
    private WaypointStorePersistence persistence;

    @Before
    public void setUp() throws IOException
    {
        tmpDir = Files.createTempDirectory("waypointer-persist-test");
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonSerializer<Instant>) (src, t, c) ->
                    new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonDeserializer<Instant>) (e, t, c) ->
                    Instant.parse(e.getAsString()))
            .create();
        codec = new LibraryJsonCodec(gson);
        persistence = new WaypointStorePersistence(tmpDir, codec);
    }

    @After
    public void tearDown() throws IOException
    {
        if (Files.isDirectory(tmpDir))
        {
            Files.walk(tmpDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }
    }

    @Test
    public void loadFromMissingFileReturnsFreshLibrary()
    {
        Library lib = persistence.loadOrEmpty();
        assertEquals(Library.CURRENT_SCHEMA_VERSION, lib.getSchemaVersion());
        assertTrue(lib.getCategories().isEmpty());
    }

    @Test
    public void saveAndLoadRoundTrip() throws IOException
    {
        Library lib = new Library();
        UUID c = UUID.randomUUID();
        lib.getCategories().add(new Category(c, "Banks", 0, false, null, false));
        lib.getWaypoints().add(new Waypoint(
            UUID.randomUUID(), "GE", 42, c, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0));

        persistence.saveBlocking(lib);
        Library back = persistence.loadOrEmpty();

        assertEquals(1, back.getCategories().size());
        assertEquals("GE", back.getWaypoints().get(0).getName());
        // Backup should also exist after the save
        assertTrue(Files.exists(tmpDir.resolve("library.json.bak")));
    }

    @Test
    public void corruptPrimaryFallsBackToBackup() throws IOException
    {
        Library lib = new Library();
        UUID c = UUID.randomUUID();
        lib.getCategories().add(new Category(c, "Banks", 0, false, null, false));
        persistence.saveBlocking(lib);

        // Corrupt primary; backup should still be intact
        Files.writeString(tmpDir.resolve("library.json"), "garbage", StandardCharsets.UTF_8);

        Library back = persistence.loadOrEmpty();
        assertEquals(1, back.getCategories().size());
        assertEquals("Banks", back.getCategories().get(0).getName());
    }

    @Test
    public void bothCorruptYieldsEmptyAndDoesNotOverwriteFiles() throws IOException
    {
        Files.writeString(tmpDir.resolve("library.json"), "garbage", StandardCharsets.UTF_8);
        Files.writeString(tmpDir.resolve("library.json.bak"), "also garbage", StandardCharsets.UTF_8);

        Library back = persistence.loadOrEmpty();
        assertTrue(back.getCategories().isEmpty());
        // The bad files MUST remain intact for later manual recovery.
        assertEquals("garbage", Files.readString(tmpDir.resolve("library.json")));
        assertEquals("also garbage", Files.readString(tmpDir.resolve("library.json.bak")));
    }
}
