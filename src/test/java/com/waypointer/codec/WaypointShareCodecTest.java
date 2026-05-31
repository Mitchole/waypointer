package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaypointShareCodecTest
{
    private WaypointShareCodec codec;

    private final Gson fixtureGson = new GsonBuilder()
        .registerTypeAdapter(Instant.class,
            (com.google.gson.JsonSerializer<Instant>) (src, t, c) ->
                new com.google.gson.JsonPrimitive(src.toString()))
        .registerTypeAdapter(Instant.class,
            (com.google.gson.JsonDeserializer<Instant>) (e, t, c) ->
                Instant.parse(e.getAsString()))
        .create();

    private String legacyWp1(Waypoint w, Category c) throws java.io.IOException
    {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.add("waypoint", fixtureGson.toJsonTree(w));
        obj.add("category", fixtureGson.toJsonTree(c));
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(baos))
        {
            gz.write(obj.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        // Unpadded base64 matches what the deleted encodeSingle produced; the codec's
        // ungzipBase64 uses a plain Base64.getDecoder(), which accepts unpadded input. Do NOT
        // switch this to a padded / URL / MIME encoder or the legacy decode contract drifts.
        return "WP1:" + java.util.Base64.getEncoder().withoutPadding()
            .encodeToString(baos.toByteArray());
    }

    @Before
    public void setUp()
    {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonSerializer<Instant>) (src, t, c) ->
                    new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonDeserializer<Instant>) (e, t, c) ->
                    Instant.parse(e.getAsString()))
            .create();
        codec = new WaypointShareCodec(gson);
    }

    @Test
    public void legacySingleCodeStillDecodes() throws Exception
    {
        UUID catId = UUID.randomUUID();
        Category c = new Category(catId, "Bossing", 0, false, null, false);
        Waypoint w = new Waypoint(UUID.randomUUID(), "Vorkath", 42, catId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);

        String code = legacyWp1(w, c);
        assertTrue(code.startsWith("WP1:"));

        WaypointShareCodec.SingleResult result = codec.decodeSingle(code);
        assertEquals("Vorkath", result.waypoint.getName());
        assertEquals(42, result.waypoint.getPackedWorldPoint());
        assertEquals("Bossing", result.category.getName());
    }

    @Test
    public void legacySingleCodeDecodesAsOneWaypointLibrary() throws Exception
    {
        UUID catId = UUID.randomUUID();
        Category c = new Category(catId, "Bossing", 0, false, null, false);
        Waypoint w = new Waypoint(UUID.randomUUID(), "Vorkath", 42, catId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);

        Library lib = codec.decodeLibrary(legacyWp1(w, c));
        assertEquals(1, lib.getWaypoints().size());
        assertEquals(1, lib.getCategories().size());
        assertEquals("Vorkath", lib.getWaypoints().get(0).getName());
    }

    @Test
    public void libraryRoundTrip()
    {
        Library lib = new Library();
        UUID c = UUID.randomUUID();
        lib.getCategories().add(new Category(c, "Banks", 0, false, null, false));
        lib.getWaypoints().add(new Waypoint(UUID.randomUUID(), "GE", 99, c, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false));

        String code = codec.encodeLibrary(lib);
        assertTrue(code.startsWith("WPL1:"));

        Library back = codec.decodeLibrary(code);
        assertEquals(1, back.getCategories().size());
        assertEquals(1, back.getWaypoints().size());
    }

    @Test
    public void whitespaceTrimmedFromInput()
    {
        Library lib = new Library();
        String code = codec.encodeLibrary(lib);
        Library back = codec.decodeLibrary("  \n  " + code + "  \r\n  ");
        assertNotNull(back);
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsBadMagic()
    {
        codec.decodeLibrary("XYZ:1:abcd");
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsUnknownVersion()
    {
        codec.decodeLibrary("WPL99:abcd");
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsGarbage()
    {
        codec.decodeLibrary("not a code at all");
    }

    @Test
    public void libraryRoundTripPreservesIconAndBundledFlag()
    {
        Library lib = new Library();
        UUID c = UUID.randomUUID();
        lib.getCategories().add(new Category(c, "Bundled bosses", 0, false, 1234, true));
        lib.getWaypoints().add(new Waypoint(UUID.randomUUID(), "Vorkath", 7, c, 5678, "notes",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false));

        Library back = codec.decodeLibrary(codec.encodeLibrary(lib));

        Category restoredCat = back.getCategories().get(0);
        assertEquals("Bundled bosses", restoredCat.getName());
        assertEquals(Integer.valueOf(1234), restoredCat.getIconId());
        assertTrue("bundled flag must round-trip", restoredCat.isBundled());

        Waypoint restoredWp = back.getWaypoints().get(0);
        assertEquals(Integer.valueOf(5678), restoredWp.getIconId());
        assertEquals("notes", restoredWp.getNotes());
    }

    @Test
    public void libraryRoundTripPreservesSchemaVersion()
    {
        Library lib = new Library();
        // Library defaults schemaVersion to CURRENT; verify it survives round-trip.
        Library back = codec.decodeLibrary(codec.encodeLibrary(lib));
        assertEquals(Library.CURRENT_SCHEMA_VERSION, back.getSchemaVersion());
    }

    @Test
    public void legacySingleCodePreservesIconNotesAndCreatedAt() throws Exception
    {
        UUID catId = UUID.randomUUID();
        Category c = new Category(catId, "Skilling", 0, false, 99, false);
        Instant ts = Instant.parse("2026-05-02T12:34:56Z");
        Waypoint w = new Waypoint(UUID.randomUUID(), "Yew tree", 100, catId, 42,
            "100k/hr at 90 wc", ts, 3, false, null, false);

        WaypointShareCodec.SingleResult r = codec.decodeSingle(legacyWp1(w, c));
        assertEquals(Integer.valueOf(42), r.waypoint.getIconId());
        assertEquals("100k/hr at 90 wc", r.waypoint.getNotes());
        assertEquals(ts, r.waypoint.getCreatedAt());
        assertEquals(Integer.valueOf(99), r.category.getIconId());
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsOversizedPayload() throws Exception
    {
        // Build a library big enough that the inflated JSON exceeds the 1 MiB cap.
        Library lib = new Library();
        UUID c = UUID.randomUUID();
        lib.getCategories().add(new Category(c, "X", 0, false, null, false));
        StringBuilder bigName = new StringBuilder();
        for (int i = 0; i < 200_000; i++) bigName.append('X');
        for (int i = 0; i < 10; i++)
        {
            lib.getWaypoints().add(new Waypoint(UUID.randomUUID(), bigName.toString(), 1, c, null,
                "", Instant.parse("2026-05-02T00:00:00Z"), i, false, null, false));
        }
        String code = codec.encodeLibrary(lib);
        codec.decodeLibrary(code);  // should throw on size cap
    }
}
