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
    public void singleWaypointRoundTrip()
    {
        UUID catId = UUID.randomUUID();
        Category c = new Category(catId, "Bossing", 0, false, null, false);
        Waypoint w = new Waypoint(UUID.randomUUID(), "Vorkath", 42, catId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0);

        String code = codec.encodeSingle(w, c);
        assertTrue(code.startsWith("WP1:"));

        WaypointShareCodec.SingleResult result = codec.decodeSingle(code);
        assertEquals("Vorkath", result.waypoint.getName());
        assertEquals(42, result.waypoint.getPackedWorldPoint());
        assertEquals("Bossing", result.category.getName());
    }

    @Test
    public void libraryRoundTrip()
    {
        Library lib = new Library();
        UUID c = UUID.randomUUID();
        lib.getCategories().add(new Category(c, "Banks", 0, false, null, false));
        lib.getWaypoints().add(new Waypoint(UUID.randomUUID(), "GE", 99, c, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0));

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
            Instant.parse("2026-05-02T00:00:00Z"), 0));

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
    public void singleWaypointRoundTripPreservesIconNotesAndCreatedAt()
    {
        UUID catId = UUID.randomUUID();
        Category c = new Category(catId, "Skilling", 0, false, 99, false);
        Instant ts = Instant.parse("2026-05-02T12:34:56Z");
        Waypoint w = new Waypoint(UUID.randomUUID(), "Yew tree", 100, catId, 42,
            "100k/hr at 90 wc", ts, 3);

        WaypointShareCodec.SingleResult r = codec.decodeSingle(codec.encodeSingle(w, c));
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
                "", Instant.parse("2026-05-02T00:00:00Z"), i));
        }
        String code = codec.encodeLibrary(lib);
        codec.decodeLibrary(code);  // should throw on size cap
    }
}
