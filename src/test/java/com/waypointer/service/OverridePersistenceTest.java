package com.waypointer.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OverridePersistenceTest
{
    private Path tmpDir;
    private OverridePersistence persistence;

    @Before
    public void setUp() throws IOException
    {
        tmpDir = Files.createTempDirectory("waypointer-override-test");
        persistence = new OverridePersistence(tmpDir, "test-overrides.json");
    }

    @After
    public void tearDown() throws IOException
    {
        Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder())
            .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
    }

    @Test
    public void loadFromMissingFileReturnsEmptyString()
    {
        assertEquals("", persistence.loadOrEmpty());
    }

    @Test
    public void roundTripWritesPrimaryAndBackup() throws IOException
    {
        assertTrue(persistence.writeBlocking("{\"hello\":1}"));
        assertEquals("{\"hello\":1}", persistence.loadOrEmpty());
        assertTrue(Files.exists(tmpDir.resolve("test-overrides.json.bak")));
    }

    @Test
    public void corruptPrimaryFallsBackToBackup() throws IOException
    {
        persistence.writeBlocking("{\"v\":1}");
        // Simulate primary unreadable by deleting it; backup should still be there.
        Files.delete(tmpDir.resolve("test-overrides.json"));
        assertEquals("{\"v\":1}", persistence.loadOrEmpty());
    }
}
