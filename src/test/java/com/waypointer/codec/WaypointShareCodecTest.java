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

    private String wpl1(String innerJson) throws java.io.IOException
    {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(baos))
        {
            gz.write(innerJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return "WPL1:" + java.util.Base64.getEncoder().withoutPadding()
            .encodeToString(baos.toByteArray());
    }

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
        codec = new WaypointShareCodec(gson, new LibraryJsonCodec(gson));
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

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsLibraryCodeFromNewerSchema() throws Exception
    {
        // Inner library schemaVersion newer than supported must be rejected, like the file-load path.
        String json = "{\"schemaVersion\":" + (Library.CURRENT_SCHEMA_VERSION + 1)
            + ",\"categories\":[],\"waypoints\":[]}";
        codec.decodeLibrary(wpl1(json));
    }

    @Test
    public void legacyLibraryCodeWithoutSchemaDecodesViaMigrator() throws Exception
    {
        // A pre-versioned inner payload (no schemaVersion) must run through the migrator and be
        // normalised to CURRENT, proving the share path now shares the file-load decode contract.
        String json = "{\"categories\":[],\"waypoints\":[]}";
        Library back = codec.decodeLibrary(wpl1(json));
        assertNotNull(back);
        assertEquals(Library.CURRENT_SCHEMA_VERSION, back.getSchemaVersion());
    }

    private static String wp1(String innerJson) throws java.io.IOException
    {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(baos))
        {
            gz.write(innerJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return "WP1:" + java.util.Base64.getEncoder().withoutPadding()
            .encodeToString(baos.toByteArray());
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsSingleWithEmptyWaypointAndCategory() throws Exception
    {
        codec.decodeSingle(wp1("{\"waypoint\":{},\"category\":{}}"));
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsSingleWithZeroCoordinate() throws Exception
    {
        UUID catId = UUID.randomUUID();
        String json = "{\"waypoint\":{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"X\","
            + "\"packedWorldPoint\":0,\"categoryId\":\"" + catId + "\"},"
            + "\"category\":{\"id\":\"" + catId + "\",\"name\":\"Bossing\","
            + "\"sortOrder\":0,\"uncategorized\":false,\"bundled\":false}}";
        codec.decodeSingle(wp1(json));
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsSingleWithNamelessCategory() throws Exception
    {
        UUID catId = UUID.randomUUID();
        String json = "{\"waypoint\":{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Vorkath\","
            + "\"packedWorldPoint\":42,\"categoryId\":\"" + catId + "\"},"
            + "\"category\":{\"id\":\"" + catId + "\",\"sortOrder\":0,"
            + "\"uncategorized\":false,\"bundled\":false}}";
        codec.decodeSingle(wp1(json));
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsLibraryWithNullSchemaVersion() throws Exception
    {
        codec.decodeLibrary(wpl1("{\"schemaVersion\":null,\"categories\":[],\"waypoints\":[]}"));
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsLibraryWithStringSchemaVersion() throws Exception
    {
        codec.decodeLibrary(wpl1("{\"schemaVersion\":\"x\",\"categories\":[],\"waypoints\":[]}"));
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsLibraryWithInvalidBase64()
    {
        codec.decodeLibrary("WPL1:!!!notbase64!!!");
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsLibraryWithArrayTopLevel() throws Exception
    {
        codec.decodeLibrary(wpl1("[]"));
    }

    @Test(expected = WaypointShareCodec.MalformedCodeException.class)
    public void rejectsLibraryWithStringTopLevel() throws Exception
    {
        codec.decodeLibrary(wpl1("\"not an object\""));
    }

    @Test
    public void decodesLibraryWithNullArrays() throws Exception
    {
        // Null categories/waypoints must decode to an empty library, not crash. Asserts the
        // LibraryJsonCodec null-array defense reached through the share path.
        Library back = codec.decodeLibrary(wpl1("{\"categories\":null,\"waypoints\":null}"));
        assertNotNull(back);
        assertEquals(0, back.getCategories().size());
        assertEquals(0, back.getWaypoints().size());
    }

    @Test
    public void decodeLibraryWithReportCountsDrops() throws Exception
    {
        UUID catId = UUID.randomUUID();
        String json = "{\"schemaVersion\":2,"
            + "\"categories\":[{\"id\":\"" + catId + "\",\"name\":\"Keep\",\"sortOrder\":0,"
            + "\"uncategorized\":false,\"bundled\":false}],"
            + "\"waypoints\":["
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Good\",\"packedWorldPoint\":42,\"categoryId\":\"" + catId + "\"},"
            + "{\"name\":\"NoId\",\"packedWorldPoint\":42}"
            + "]}";

        DecodeReport r = codec.decodeLibraryWithReport(wpl1(json));

        assertEquals(1, r.library.getWaypoints().size());
        assertEquals(1, r.droppedWaypoints);
        assertEquals(0, r.droppedCategories);
    }

    @Test
    public void decodeLibraryWithReportForSingleHasZeroDrops() throws Exception
    {
        UUID catId = UUID.randomUUID();
        Category c = new Category(catId, "Bossing", 0, false, null, false);
        Waypoint w = new Waypoint(UUID.randomUUID(), "Vorkath", 42, catId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);

        DecodeReport r = codec.decodeLibraryWithReport(legacyWp1(w, c));

        assertEquals(1, r.library.getWaypoints().size());
        assertEquals(0, r.droppedWaypoints);
        assertEquals(0, r.droppedCategories);
    }
}
